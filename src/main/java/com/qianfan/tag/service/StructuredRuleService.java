package com.qianfan.tag.service;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.common.Ids;
import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.RuleEvaluationBatch;
import com.qianfan.tag.domain.TagRuleCondition;
import com.qianfan.tag.domain.TagRuleSet;
import com.qianfan.tag.dto.RuleSetRequests;
import com.qianfan.tag.mapper.IndicatorMapper;
import com.qianfan.tag.mapper.PersonMapper;
import com.qianfan.tag.mapper.StructuredRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;

@Service
public class StructuredRuleService {
    private static final int PAGE_SIZE = 200;
    private final StructuredRuleMapper mapper;
    private final IndicatorMapper indicatorMapper;
    private final IndicatorService indicatorService;
    private final TagService tagService;
    private final PersonMapper personMapper;
    private final RuleEvaluationPageProcessor pageProcessor;
    private final RuleApplicationService applicationService;

    public StructuredRuleService(StructuredRuleMapper mapper, IndicatorMapper indicatorMapper,
                                 IndicatorService indicatorService, TagService tagService,
                                 PersonMapper personMapper, RuleEvaluationPageProcessor pageProcessor,
                                 RuleApplicationService applicationService) {
        this.mapper = mapper;
        this.indicatorMapper = indicatorMapper;
        this.indicatorService = indicatorService;
        this.tagService = tagService;
        this.personMapper = personMapper;
        this.pageProcessor = pageProcessor;
        this.applicationService = applicationService;
    }

    @Transactional
    public TagRuleSet createDraft(RuleSetRequests.CreateDraft request) {
        tagService.requireTag(request.getTagId());
        String mode = request.getMatchMode().toUpperCase(Locale.ROOT);
        if (!"ALL".equals(mode) && !"ANY".equals(mode)) {
            throw new BusinessException("INVALID_MATCH_MODE", "条件关系只能是 ALL 或 ANY");
        }
        Date now = new Date();
        TagRuleSet ruleSet = new TagRuleSet();
        ruleSet.setId(Ids.uuid());
        ruleSet.setTagId(request.getTagId());
        Integer max = mapper.findMaxVersion(request.getTagId());
        ruleSet.setVersion(max == null ? 1 : max + 1);
        ruleSet.setMatchMode(mode);
        ruleSet.setStatus("DRAFT");
        ruleSet.setCreatedAt(now);
        ruleSet.setUpdatedAt(now);
        mapper.insertRuleSet(ruleSet);

        Set<String> signatures = new HashSet<String>();
        int sort = 0;
        for (RuleSetRequests.Condition input : request.getConditions()) {
            IndicatorDefinition definition = indicatorMapper.findDefinitionById(input.getIndicatorId());
            if (definition == null || definition.getStatus() == null || definition.getStatus() != 1) {
                throw new BusinessException("RULE_INDICATOR_UNAVAILABLE", "规则引用的指标不存在或已停用");
            }
            String operator = input.getOperator().toUpperCase(Locale.ROOT);
            String valuesJson = indicatorService.validateAndSerializeExpected(definition, operator, input.getValues());
            String signature = definition.getId() + "|" + operator + "|" + valuesJson;
            if (!signatures.add(signature)) {
                throw new BusinessException("DUPLICATE_RULE_CONDITION", "规则中存在完全重复的条件");
            }
            TagRuleCondition condition = new TagRuleCondition();
            condition.setId(Ids.uuid());
            condition.setRuleSetId(ruleSet.getId());
            condition.setIndicatorId(definition.getId());
            condition.setOperator(operator);
            condition.setExpectedValues(valuesJson);
            condition.setSortNo(sort++);
            condition.setCreatedAt(now);
            mapper.insertCondition(condition);
            ruleSet.getConditions().add(condition);
        }
        return ruleSet;
    }

    public List<TagRuleSet> list() {
        List<TagRuleSet> result = mapper.findRuleSets();
        for (TagRuleSet item : result) {
            item.setConditions(mapper.findConditions(item.getId()));
        }
        return result;
    }

    public TagRuleSet require(String id) {
        TagRuleSet ruleSet = mapper.findRuleSet(id);
        if (ruleSet == null) {
            throw new BusinessException("RULE_SET_NOT_FOUND", "结构化规则集不存在");
        }
        ruleSet.setConditions(mapper.findConditions(id));
        return ruleSet;
    }

    @Transactional
    public TagRuleSet publish(String id) {
        TagRuleSet ruleSet = require(id);
        if (!"DRAFT".equals(ruleSet.getStatus())) {
            throw new BusinessException("RULE_SET_NOT_DRAFT", "只有草稿规则可以发布");
        }
        Date now = new Date();
        mapper.disablePublishedByTag(ruleSet.getTagId(), now);
        if (mapper.publishRuleSet(id, now) == 0) {
            throw new BusinessException("RULE_SET_PUBLISH_FAILED", "规则发布失败");
        }
        return require(id);
    }

    public synchronized RuleEvaluationBatch recalculate(String ruleSetId, String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty() || batchNo.trim().length() > 64) {
            throw new BusinessException("RULE_BATCH_NO_INVALID", "规则重算批次号不能为空且不能超过64个字符");
        }
        batchNo = batchNo.trim();
        RuleEvaluationBatch existing = mapper.findBatchByNo(batchNo);
        if (existing != null && "SUCCESS".equals(existing.getStatus())) return existing;
        if (existing != null && "RUNNING".equals(existing.getStatus())) {
            throw new BusinessException("RULE_BATCH_RUNNING", "该规则重算批次正在执行");
        }
        TagRuleSet ruleSet = require(ruleSetId);
        if (!"PUBLISHED".equals(ruleSet.getStatus())) {
            throw new BusinessException("RULE_SET_NOT_PUBLISHED", "只有已发布规则可以重算");
        }
        Date now = new Date();
        if (existing == null) {
            existing = new RuleEvaluationBatch();
            existing.setId(Ids.uuid());
            existing.setBatchNo(batchNo);
            existing.setRuleSetId(ruleSetId);
            existing.setStatus("RUNNING");
            existing.setScannedCount(0);
            existing.setMatchedCount(0);
            existing.setExpiredCount(0);
            existing.setStartedAt(now);
            mapper.insertBatch(existing);
        } else {
            mapper.restartBatch(batchNo, now);
        }
        try {
            int offset = 0;
            while (true) {
                List<PersonRecord> people = personMapper.findActivePage(offset, offset + PAGE_SIZE);
                if (people.isEmpty()) break;
                pageProcessor.process(batchNo, ruleSet, people);
                offset += people.size();
                if (people.size() < PAGE_SIZE) break;
            }
            Date finished = new Date();
            mapper.expireOtherRuleVersions(ruleSet.getTagId(), ruleSet.getId(), finished);
            mapper.expireOrphanRuleBindings(ruleSet.getTagId(), finished);
            mapper.finishBatch(batchNo, "SUCCESS", null, finished);
        } catch (RuntimeException ex) {
            mapper.finishBatch(batchNo, "FAILED", abbreviate(ex.getMessage()), new Date());
            throw ex;
        }
        return mapper.findBatchByNo(batchNo);
    }

    public List<RuleEvaluationBatch> recalculatePublished(String batchNoPrefix) {
        if (batchNoPrefix == null || batchNoPrefix.trim().isEmpty()) {
            throw new BusinessException("RULE_BATCH_NO_REQUIRED", "规则重算批次号不能为空");
        }
        List<RuleEvaluationBatch> result = new ArrayList<RuleEvaluationBatch>();
        int sequence = 1;
        for (TagRuleSet ruleSet : mapper.findPublishedRuleSets()) {
            String suffix = "_" + sequence++;
            String prefix = batchNoPrefix.trim();
            if (prefix.length() + suffix.length() > 64) prefix = prefix.substring(0, 64 - suffix.length());
            result.add(recalculate(ruleSet.getId(), prefix + suffix));
        }
        return result;
    }

    public void evaluatePublishedForPerson(PersonRecord person, String batchNo) {
        for (TagRuleSet ruleSet : mapper.findPublishedRuleSets()) {
            ruleSet.setConditions(mapper.findConditions(ruleSet.getId()));
            applicationService.apply(person, ruleSet, batchNo);
        }
    }

    private String abbreviate(String value) {
        if (value == null) return "未知错误";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}

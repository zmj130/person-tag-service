package com.qianfan.tag.service;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.common.Ids;
import com.qianfan.tag.domain.TagDefinition;
import com.qianfan.tag.domain.TagRule;
import com.qianfan.tag.dto.TagRequests;
import com.qianfan.tag.mapper.TagMapper;
import com.qianfan.tag.mapper.PersonTagMapper;
import com.qianfan.tag.trie.RuleChangedEvent;
import com.qianfan.tag.trie.TextNormalizer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/** 标签定义和关键词规则管理。 */
@Service
public class TagService {
    private final TagMapper tagMapper;
    private final TextNormalizer normalizer;
    private final ApplicationEventPublisher eventPublisher;
    private final PersonTagMapper personTagMapper;

    public TagService(TagMapper tagMapper, TextNormalizer normalizer,
                      ApplicationEventPublisher eventPublisher, PersonTagMapper personTagMapper) {
        this.tagMapper = tagMapper;
        this.normalizer = normalizer;
        this.eventPublisher = eventPublisher;
        this.personTagMapper = personTagMapper;
    }

    @Transactional
    public TagDefinition create(TagRequests.CreateTag request) {
        if (tagMapper.findTagByCode(request.getCode()) != null) {
            throw new BusinessException("TAG_CODE_EXISTS", "标签编码已经存在");
        }
        Date now = new Date();
        TagDefinition tag = new TagDefinition();
        tag.setId(Ids.uuid());
        tag.setCode(request.getCode().trim());
        tag.setName(request.getName().trim());
        tag.setCategory(request.getCategory().trim());
        tag.setDescription(request.getDescription());
        tag.setStatus(1);
        tag.setAutoApprove(Boolean.TRUE.equals(request.getAutoApprove()) ? 1 : 0);
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);
        tagMapper.insertTag(tag);
        return tag;
    }

    public List<TagDefinition> list() {
        return tagMapper.findAllTags();
    }

    @Transactional
    public TagDefinition update(String tagId, TagRequests.UpdateTag request) {
        List<String> affectedPersonIds = personTagMapper.findPersonIdsByTag(tagId);
        TagDefinition tag = requireTag(tagId);
        tag.setName(request.getName().trim());
        tag.setCategory(request.getCategory().trim());
        tag.setDescription(request.getDescription());
        tag.setAutoApprove(Boolean.TRUE.equals(request.getAutoApprove()) ? 1 : 0);
        tag.setUpdatedAt(new Date());
        tagMapper.updateTag(tag);
        eventPublisher.publishEvent(new RuleChangedEvent());
        publishProfileRefresh(affectedPersonIds);
        return tag;
    }

    @Transactional
    public void changeStatus(String tagId, boolean enabled) {
        List<String> affectedPersonIds = personTagMapper.findPersonIdsByTag(tagId);
        requireTag(tagId);
        tagMapper.updateTagStatus(tagId, enabled ? 1 : 0, new Date());
        eventPublisher.publishEvent(new RuleChangedEvent());
        publishProfileRefresh(affectedPersonIds);
    }

    @Transactional
    public void delete(String tagId) {
        requireTag(tagId);
        if (tagMapper.countTagReferences(tagId) > 0) {
            throw new BusinessException("TAG_IN_USE", "标签已有规则、人员关系或历史记录，请改为停用");
        }
        if (tagMapper.deleteTag(tagId) == 0) {
            throw new BusinessException("TAG_DELETE_FAILED", "标签删除失败");
        }
    }

    @Transactional
    public TagRule addRule(String tagId, TagRequests.CreateRule request) {
        requireTag(tagId);
        String normalized = normalizer.normalize(request.getKeyword());
        if (normalized.isEmpty()) {
            throw new BusinessException("EMPTY_RULE", "规则归一化后不能为空");
        }
        if (tagMapper.findRule(tagId, normalized) != null) {
            throw new BusinessException("RULE_EXISTS", "该标签下已经存在等价关键词规则");
        }
        Date now = new Date();
        TagRule rule = new TagRule();
        rule.setId(Ids.uuid());
        rule.setTagId(tagId);
        rule.setKeyword(request.getKeyword().trim());
        rule.setNormalizedKeyword(normalized);
        rule.setStatus(1);
        rule.setVersion(1);
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        tagMapper.insertRule(rule);
        // 监听器在事务提交后重建 Trie；若事务回滚，内存索引不会变化。
        eventPublisher.publishEvent(new RuleChangedEvent());
        return rule;
    }

    public List<TagRule> listRules(String tagId) {
        requireTag(tagId);
        return tagMapper.findRulesByTagId(tagId);
    }

    @Transactional
    public void changeRuleStatus(String ruleId, boolean enabled) {
        if (tagMapper.findRuleById(ruleId) == null) {
            throw new BusinessException("RULE_NOT_FOUND", "标签规则不存在");
        }
        tagMapper.updateRuleStatus(ruleId, enabled ? 1 : 0, new Date());
        eventPublisher.publishEvent(new RuleChangedEvent());
    }

    @Transactional
    public void deleteRule(String ruleId) {
        if (tagMapper.findRuleById(ruleId) == null) {
            throw new BusinessException("RULE_NOT_FOUND", "标签规则不存在");
        }
        List<String> affectedPersonIds = personTagMapper.findPersonIdsByRule(ruleId);
        personTagMapper.deleteKeywordRuleBindings(ruleId);
        tagMapper.deleteRule(ruleId);
        eventPublisher.publishEvent(new RuleChangedEvent());
        publishProfileRefresh(affectedPersonIds);
    }

    public TagDefinition requireTag(String tagId) {
        TagDefinition tag = tagMapper.findTagById(tagId);
        if (tag == null) {
            throw new BusinessException("TAG_NOT_FOUND", "标签不存在");
        }
        return tag;
    }

    private void publishProfileRefresh(List<String> personIds) {
        for (String personId : personIds) {
            eventPublisher.publishEvent(new ProfileRefreshEvent(personId));
        }
    }
}

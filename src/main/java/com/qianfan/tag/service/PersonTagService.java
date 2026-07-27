package com.qianfan.tag.service;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.common.Ids;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.PersonTag;
import com.qianfan.tag.domain.TagDefinition;
import com.qianfan.tag.mapper.PersonTagMapper;
import com.qianfan.tag.mapper.TagMapper;
import com.qianfan.tag.trie.RuleMatch;
import com.qianfan.tag.trie.TrieManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 人工、远程和规则三种标签来源的统一绑定入口。 */
@Service
public class PersonTagService {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private final PersonTagMapper personTagMapper;
    private final TagMapper tagMapper;
    private final TrieManager trieManager;

    public PersonTagService(PersonTagMapper personTagMapper, TagMapper tagMapper, TrieManager trieManager) {
        this.personTagMapper = personTagMapper;
        this.tagMapper = tagMapper;
        this.trieManager = trieManager;
    }

    @Transactional
    public PersonTag bindManual(String personId, String tagId, String operator) {
        TagDefinition tag = requireEnabledTagById(tagId);
        return saveBinding(personId, tag.getId(), "MANUAL", APPROVED, null, null, null, operator);
    }

    @Transactional
    public int bindManualBatch(List<String> personIds, String tagId, String operator) {
        if (personIds == null || personIds.isEmpty() || personIds.size() > 500) {
            throw new BusinessException("INVALID_BATCH_SIZE", "批量绑定人员数量范围为 1 到 500");
        }
        TagDefinition tag = requireEnabledTagById(tagId);
        for (String personId : personIds) {
            saveBinding(personId, tag.getId(), "MANUAL", APPROVED, null, null, null, operator);
        }
        return personIds.size();
    }

    public void bindRemoteTags(PersonRecord person, List<String> tagCodes, String batchNo) {
        if (tagCodes == null) {
            return;
        }
        for (String tagCode : tagCodes) {
            TagDefinition tag = tagMapper.findTagByCode(tagCode);
            if (tag == null || tag.getStatus() == null || tag.getStatus() != 1) {
                throw new BusinessException("REMOTE_TAG_NOT_FOUND", "远程标签编码未配置或已停用：" + tagCode);
            }
            saveBinding(person.getId(), tag.getId(), "REMOTE", APPROVED, null, batchNo, null, "REMOTE_SYNC");
        }
    }

    public void removeRemoteTags(PersonRecord person, List<String> removedTagCodes) {
        if (removedTagCodes == null) {
            return;
        }
        for (String tagCode : removedTagCodes) {
            TagDefinition tag = tagMapper.findTagByCode(tagCode);
            if (tag != null) {
                // SQL 限定来源为 REMOTE，人工确认的同名标签不会被上游撤销事件删除。
                personTagMapper.deleteRemote(person.getId(), tag.getId());
            }
        }
    }

    public void applyRules(PersonRecord person, String batchNo) {
        String searchableText = join(person.getOrganization(), person.getOccupation(),
                person.getAddress(), person.getRemark());
        List<RuleMatch> matches = trieManager.match(searchableText);
        Set<String> matchedTagIds = new LinkedHashSet<String>();
        for (RuleMatch match : matches) {
            matchedTagIds.add(match.getTagId());
        }
        // 删除资料变更后已经不再命中的规则标签；REJECTED 记录保留，用于防止反复生成候选。
        personTagMapper.deleteStaleRuleBindings(
                person.getId(), new java.util.ArrayList<String>(matchedTagIds));
        for (RuleMatch match : matches) {
            saveBinding(person.getId(), match.getTagId(), "RULE",
                    match.isAutoApprove() ? APPROVED : PENDING,
                    match.getRuleId(), batchNo, match.getKeyword(), null);
        }
    }

    @Transactional
    public void review(String bindingId, String status, String reviewer) {
        if (!APPROVED.equals(status) && !REJECTED.equals(status)) {
            throw new BusinessException("INVALID_REVIEW_STATUS", "审核状态只能是 APPROVED 或 REJECTED");
        }
        if (personTagMapper.review(bindingId, status, reviewer, new Date()) == 0) {
            throw new BusinessException("BINDING_NOT_FOUND", "人员标签关系不存在");
        }
    }

    public List<PersonTag> listByPerson(String personId) {
        return personTagMapper.findByPersonId(personId);
    }

    @Transactional
    public void unbindManual(String personId, String tagId) {
        if (personTagMapper.deleteManual(personId, tagId) == 0) {
            throw new BusinessException("MANUAL_BINDING_NOT_FOUND", "不存在可解绑的人工标签");
        }
    }

    private PersonTag saveBinding(String personId, String tagId, String source, String status,
                                  String ruleId, String batchNo, String keyword, String reviewer) {
        Date now = new Date();
        PersonTag existing = personTagMapper.find(personId, tagId);
        if (existing != null) {
            // 人工拒绝过的规则候选不应在下一轮同步中自动复活。
            if ("RULE".equals(source) && REJECTED.equals(existing.getStatus())) {
                return existing;
            }
            // 已确认的人工/远程标签可信度高于规则命中，规则不能覆盖其来源。
            if ("RULE".equals(source) && APPROVED.equals(existing.getStatus())
                    && !"RULE".equals(existing.getSource())) {
                return existing;
            }
            existing.setSource(source);
            existing.setStatus(status);
            existing.setRuleId(ruleId);
            existing.setBatchNo(batchNo);
            existing.setMatchedKeyword(keyword);
            existing.setReviewedBy(reviewer);
            existing.setReviewedAt(reviewer == null ? existing.getReviewedAt() : now);
            existing.setUpdatedAt(now);
            personTagMapper.updateBinding(existing);
            return existing;
        }

        PersonTag binding = new PersonTag();
        binding.setId(Ids.uuid());
        binding.setPersonId(personId);
        binding.setTagId(tagId);
        binding.setSource(source);
        binding.setStatus(status);
        binding.setRuleId(ruleId);
        binding.setBatchNo(batchNo);
        binding.setMatchedKeyword(keyword);
        binding.setReviewedBy(reviewer);
        binding.setReviewedAt(reviewer == null ? null : now);
        binding.setCreatedAt(now);
        binding.setUpdatedAt(now);
        personTagMapper.insert(binding);
        return binding;
    }

    private TagDefinition requireEnabledTagById(String tagId) {
        TagDefinition tag = tagMapper.findTagById(tagId);
        if (tag == null || tag.getStatus() == null || tag.getStatus() != 1) {
            throw new BusinessException("TAG_NOT_AVAILABLE", "标签不存在或已停用");
        }
        return tag;
    }

    private String join(String... fields) {
        StringBuilder builder = new StringBuilder();
        for (String field : fields) {
            if (field != null && !field.trim().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append('|');
                }
                builder.append(field);
            }
        }
        return builder.toString();
    }
}

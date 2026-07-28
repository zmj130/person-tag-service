package com.qianfan.tag.service;

import com.qianfan.tag.common.Ids;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.PersonTag;
import com.qianfan.tag.domain.PersonTagEvidence;
import com.qianfan.tag.domain.TagRuleSet;
import com.qianfan.tag.mapper.PersonTagMapper;
import com.qianfan.tag.mapper.StructuredRuleMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class RuleApplicationService {
    private final RuleConditionEvaluator evaluator;
    private final StructuredRuleMapper ruleMapper;
    private final PersonTagMapper personTagMapper;

    public RuleApplicationService(RuleConditionEvaluator evaluator, StructuredRuleMapper ruleMapper,
                                  PersonTagMapper personTagMapper) {
        this.evaluator = evaluator;
        this.ruleMapper = ruleMapper;
        this.personTagMapper = personTagMapper;
    }

    public boolean apply(PersonRecord person, TagRuleSet ruleSet, String batchNo) {
        RuleEvaluationResult result = evaluator.evaluate(person, ruleSet);
        if (result.isMatched()) {
            activateEvidenceAndBinding(person, ruleSet, batchNo, result.getDetail());
            return true;
        }
        Date now = new Date();
        ruleMapper.expireActiveEvidence(person.getId(), ruleSet.getTagId(), ruleSet.getId(), now);
        if (ruleMapper.countActiveRuleEvidence(person.getId(), ruleSet.getTagId()) == 0) {
            personTagMapper.expireRuleBinding(person.getId(), ruleSet.getTagId(), now);
        }
        return false;
    }

    private void activateEvidenceAndBinding(PersonRecord person, TagRuleSet ruleSet,
                                            String batchNo, String detail) {
        Date now = new Date();
        PersonTagEvidence evidence = ruleMapper.findEvidence(
                person.getId(), ruleSet.getTagId(), ruleSet.getId(), ruleSet.getVersion());
        if (evidence == null) {
            evidence = new PersonTagEvidence();
            evidence.setId(Ids.uuid());
            evidence.setPersonId(person.getId());
            evidence.setTagId(ruleSet.getTagId());
            evidence.setSourceType("RULE");
            evidence.setRuleSetId(ruleSet.getId());
            evidence.setRuleVersion(ruleSet.getVersion());
            evidence.setReviewStatus("PENDING");
            evidence.setCreatedAt(now);
        }
        evidence.setBatchNo(batchNo);
        evidence.setMatchDetail(detail);
        evidence.setEvidenceStatus("ACTIVE");
        evidence.setUpdatedAt(now);

        PersonTag binding = personTagMapper.find(person.getId(), ruleSet.getTagId());
        if (binding != null && !"RULE".equals(binding.getSource()) && "APPROVED".equals(binding.getStatus())) {
            evidence.setReviewStatus("APPROVED");
            evidence.setReviewedBy("TRUSTED_SOURCE");
            evidence.setReviewedAt(now);
        }
        if (ruleMapper.findEvidence(person.getId(), ruleSet.getTagId(), ruleSet.getId(), ruleSet.getVersion()) == null) {
            ruleMapper.insertEvidence(evidence);
        } else {
            ruleMapper.updateEvidence(evidence);
        }
        if ("REJECTED".equals(evidence.getReviewStatus())) {
            return;
        }
        if (binding == null) {
            binding = new PersonTag();
            binding.setId(Ids.uuid());
            binding.setPersonId(person.getId());
            binding.setTagId(ruleSet.getTagId());
            binding.setSource("RULE");
            binding.setStatus(evidence.getReviewStatus());
            binding.setRuleId(ruleSet.getId());
            binding.setBatchNo(batchNo);
            binding.setCreatedAt(now);
            binding.setUpdatedAt(now);
            personTagMapper.insert(binding);
        } else if ("RULE".equals(binding.getSource())) {
            binding.setStatus(evidence.getReviewStatus());
            binding.setRuleId(ruleSet.getId());
            binding.setBatchNo(batchNo);
            binding.setMatchedKeyword(null);
            binding.setReviewedBy(evidence.getReviewedBy());
            binding.setReviewedAt(evidence.getReviewedAt());
            binding.setUpdatedAt(now);
            personTagMapper.updateBinding(binding);
        }
    }
}

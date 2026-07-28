package com.qianfan.tag.service;

import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.TagRuleSet;
import com.qianfan.tag.mapper.StructuredRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RuleEvaluationPageProcessor {
    private final RuleApplicationService applicationService;
    private final StructuredRuleMapper ruleMapper;

    public RuleEvaluationPageProcessor(RuleApplicationService applicationService, StructuredRuleMapper ruleMapper) {
        this.applicationService = applicationService;
        this.ruleMapper = ruleMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int process(String batchNo, TagRuleSet ruleSet, List<PersonRecord> people) {
        int matched = 0;
        for (PersonRecord person : people) {
            if (applicationService.apply(person, ruleSet, batchNo)) {
                matched++;
            }
        }
        ruleMapper.updateBatchProgress(batchNo, people.size(), matched, people.size() - matched);
        return matched;
    }
}

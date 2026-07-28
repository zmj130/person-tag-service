package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.domain.RuleEvaluationBatch;
import com.qianfan.tag.domain.TagRuleSet;
import com.qianfan.tag.dto.RuleSetRequests;
import com.qianfan.tag.service.StructuredRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/rule-sets")
public class RuleSetController {
    private final StructuredRuleService service;

    public RuleSetController(StructuredRuleService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<TagRuleSet>> list() { return ApiResponse.success(service.list()); }

    @PostMapping
    public ApiResponse<TagRuleSet> createDraft(@Valid @RequestBody RuleSetRequests.CreateDraft request) {
        return ApiResponse.success(service.createDraft(request));
    }

    @PostMapping("/{ruleSetId}/publish")
    public ApiResponse<TagRuleSet> publish(@PathVariable String ruleSetId) {
        return ApiResponse.success(service.publish(ruleSetId));
    }

    @PostMapping("/{ruleSetId}/recalculate")
    public ApiResponse<RuleEvaluationBatch> recalculate(@PathVariable String ruleSetId,
                                                         @Valid @RequestBody RuleSetRequests.Recalculate request) {
        return ApiResponse.success(service.recalculate(ruleSetId, request.getBatchNo()));
    }
}

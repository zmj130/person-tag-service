package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.domain.SyncBatch;
import com.qianfan.tag.domain.RuleEvaluationBatch;
import com.qianfan.tag.dto.ProfileIndexStatus;
import com.qianfan.tag.dto.SyncRequest;
import com.qianfan.tag.service.PersonSyncService;
import com.qianfan.tag.service.SchedulerTokenVerifier;
import com.qianfan.tag.service.StructuredRuleService;
import com.qianfan.tag.service.ProfileSearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** 供 DolphinScheduler 调用的内部任务接口。 */
@RestController
@RequestMapping("/internal/sync")
public class InternalSyncController {
    private final PersonSyncService personSyncService;
    private final SchedulerTokenVerifier tokenVerifier;
    private final StructuredRuleService structuredRuleService;
    private final ProfileSearchService profileSearchService;

    public InternalSyncController(PersonSyncService personSyncService, SchedulerTokenVerifier tokenVerifier,
                                  StructuredRuleService structuredRuleService,
                                  ProfileSearchService profileSearchService) {
        this.personSyncService = personSyncService;
        this.tokenVerifier = tokenVerifier;
        this.structuredRuleService = structuredRuleService;
        this.profileSearchService = profileSearchService;
    }

    @PostMapping("/persons/incremental")
    public ApiResponse<SyncBatch> synchronize(
            @RequestHeader(value = "X-Scheduler-Token", required = false) String token,
            @Valid @RequestBody SyncRequest request) {
        tokenVerifier.verify(token);
        return ApiResponse.success(personSyncService.synchronize(request));
    }

    @PostMapping("/rules/recalculate")
    public ApiResponse<List<RuleEvaluationBatch>> recalculateRules(
            @RequestHeader(value = "X-Scheduler-Token", required = false) String token,
            @Valid @RequestBody SyncRequest request) {
        tokenVerifier.verify(token);
        return ApiResponse.success(structuredRuleService.recalculatePublished(request.getBatchNo()));
    }

    @PostMapping("/profiles/rebuild")
    public ApiResponse<ProfileIndexStatus> rebuildProfiles(
            @RequestHeader(value = "X-Scheduler-Token", required = false) String token) {
        tokenVerifier.verify(token);
        return ApiResponse.success(profileSearchService.rebuild());
    }
}

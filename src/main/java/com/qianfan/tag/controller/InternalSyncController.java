package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.domain.SyncBatch;
import com.qianfan.tag.dto.SyncRequest;
import com.qianfan.tag.service.PersonSyncService;
import com.qianfan.tag.service.SchedulerTokenVerifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** 供 DolphinScheduler 调用的内部任务接口。 */
@RestController
@RequestMapping("/internal/sync")
public class InternalSyncController {
    private final PersonSyncService personSyncService;
    private final SchedulerTokenVerifier tokenVerifier;

    public InternalSyncController(PersonSyncService personSyncService, SchedulerTokenVerifier tokenVerifier) {
        this.personSyncService = personSyncService;
        this.tokenVerifier = tokenVerifier;
    }

    @PostMapping("/persons/incremental")
    public ApiResponse<SyncBatch> synchronize(
            @RequestHeader(value = "X-Scheduler-Token", required = false) String token,
            @Valid @RequestBody SyncRequest request) {
        tokenVerifier.verify(token);
        return ApiResponse.success(personSyncService.synchronize(request));
    }
}

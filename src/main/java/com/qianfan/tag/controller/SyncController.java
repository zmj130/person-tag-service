package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.domain.SyncBatch;
import com.qianfan.tag.dto.PageResult;
import com.qianfan.tag.service.PersonSyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端查看同步执行记录的只读接口。 */
@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final PersonSyncService personSyncService;

    public SyncController(PersonSyncService personSyncService) {
        this.personSyncService = personSyncService;
    }

    @GetMapping("/batches")
    public ApiResponse<PageResult<SyncBatch>> listBatches(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        String normalizedStatus = status == null || status.trim().isEmpty() ? null : status;
        return ApiResponse.success(personSyncService.listBatches(normalizedStatus, pageNo, pageSize));
    }
}

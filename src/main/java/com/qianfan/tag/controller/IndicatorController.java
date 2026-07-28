package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.IndicatorOption;
import com.qianfan.tag.dto.IndicatorDetail;
import com.qianfan.tag.dto.IndicatorRequests;
import com.qianfan.tag.service.IndicatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/indicators")
public class IndicatorController {
    private final IndicatorService service;

    public IndicatorController(IndicatorService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<IndicatorDetail>> list(
            @RequestParam(value = "enabledOnly", defaultValue = "false") boolean enabledOnly) {
        return ApiResponse.success(service.list(enabledOnly));
    }

    @PostMapping
    public ApiResponse<IndicatorDefinition> create(@Valid @RequestBody IndicatorRequests.Create request) {
        return ApiResponse.success(service.create(request));
    }

    @PostMapping("/{indicatorId}/options")
    public ApiResponse<IndicatorOption> addOption(@PathVariable String indicatorId,
                                                   @Valid @RequestBody IndicatorRequests.AddOption request) {
        return ApiResponse.success(service.addOption(indicatorId, request));
    }

    @PutMapping("/{indicatorId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable String indicatorId,
                                          @Valid @RequestBody IndicatorRequests.ChangeStatus request) {
        service.changeStatus(indicatorId, request.getEnabled());
        return ApiResponse.success(null);
    }
}

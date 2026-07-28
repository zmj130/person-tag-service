package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.dto.ProfileIndexStatus;
import com.qianfan.tag.dto.ProfileRequests;
import com.qianfan.tag.dto.ProfileSearchResult;
import com.qianfan.tag.service.ProfileSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final ProfileSearchService service;

    public ProfileController(ProfileSearchService service) { this.service = service; }

    @GetMapping("/status")
    public ApiResponse<ProfileIndexStatus> status() {
        return ApiResponse.success(service.status());
    }

    @PostMapping("/rebuild")
    public ApiResponse<ProfileIndexStatus> rebuild() {
        return ApiResponse.success(service.rebuild());
    }

    @PostMapping("/search")
    public ApiResponse<ProfileSearchResult> search(@Valid @RequestBody ProfileRequests.Search request) {
        return ApiResponse.success(service.search(request));
    }
}

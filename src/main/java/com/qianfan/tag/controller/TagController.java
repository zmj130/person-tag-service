package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.domain.TagDefinition;
import com.qianfan.tag.domain.TagRule;
import com.qianfan.tag.dto.TagRequests;
import com.qianfan.tag.service.TagService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** 标签定义和规则维护接口。 */
@Validated
@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    public ApiResponse<TagDefinition> create(@Valid @RequestBody TagRequests.CreateTag request) {
        return ApiResponse.success(tagService.create(request));
    }

    @GetMapping
    public ApiResponse<List<TagDefinition>> list() {
        return ApiResponse.success(tagService.list());
    }

    @PutMapping("/{tagId}")
    public ApiResponse<TagDefinition> update(@PathVariable String tagId,
                                             @Valid @RequestBody TagRequests.UpdateTag request) {
        return ApiResponse.success(tagService.update(tagId, request));
    }

    @PutMapping("/{tagId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable String tagId,
                                          @Valid @RequestBody TagRequests.ChangeStatus request) {
        tagService.changeStatus(tagId, request.getEnabled());
        return ApiResponse.success(null);
    }

    @PostMapping("/{tagId}/rules")
    public ApiResponse<TagRule> addRule(@PathVariable String tagId,
                                        @Valid @RequestBody TagRequests.CreateRule request) {
        return ApiResponse.success(tagService.addRule(tagId, request));
    }

    @GetMapping("/{tagId}/rules")
    public ApiResponse<List<TagRule>> listRules(@PathVariable String tagId) {
        return ApiResponse.success(tagService.listRules(tagId));
    }

    @PutMapping("/rules/{ruleId}/status")
    public ApiResponse<Void> changeRuleStatus(@PathVariable String ruleId,
                                              @Valid @RequestBody TagRequests.ChangeStatus request) {
        tagService.changeRuleStatus(ruleId, request.getEnabled());
        return ApiResponse.success(null);
    }
}

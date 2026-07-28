package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.PersonTag;
import com.qianfan.tag.dto.PageResult;
import com.qianfan.tag.dto.PersonRequests;
import com.qianfan.tag.dto.ReviewItem;
import com.qianfan.tag.service.PersonService;
import com.qianfan.tag.service.PersonTagService;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** 人员维护、人工标签、候选审核和标签组合查询接口。 */
@RestController
@RequestMapping("/api/persons")
public class PersonController {
    private final PersonService personService;
    private final PersonTagService personTagService;

    public PersonController(PersonService personService, PersonTagService personTagService) {
        this.personService = personService;
        this.personTagService = personTagService;
    }

    @PostMapping
    public ApiResponse<PersonRecord> upsert(@Valid @RequestBody PersonRequests.UpsertPerson request) {
        return ApiResponse.success(personService.upsert(request));
    }

    @PostMapping("/search")
    public ApiResponse<PageResult<PersonRecord>> search(@Valid @RequestBody PersonRequests.Search request) {
        return ApiResponse.success(personService.search(request));
    }

    @GetMapping("/{personId}/tags")
    public ApiResponse<List<PersonTag>> listTags(@PathVariable String personId) {
        personService.requirePerson(personId);
        return ApiResponse.success(personTagService.listByPerson(personId));
    }

    @PostMapping("/{personId}/tags")
    public ApiResponse<PersonTag> bind(@PathVariable String personId,
                                       @Valid @RequestBody PersonRequests.BindTag request) {
        personService.requirePerson(personId);
        return ApiResponse.success(personTagService.bindManual(
                personId, request.getTagId(), request.getOperator()));
    }

    @PostMapping("/tags/batch")
    public ApiResponse<Integer> bindBatch(@Valid @RequestBody PersonRequests.BatchBindTag request) {
        for (String personId : request.getPersonIds()) {
            personService.requirePerson(personId);
        }
        return ApiResponse.success(personTagService.bindManualBatch(
                request.getPersonIds(), request.getTagId(), request.getOperator()));
    }

    @DeleteMapping("/{personId}/tags/{tagId}")
    public ApiResponse<Void> unbind(@PathVariable String personId, @PathVariable String tagId) {
        personTagService.unbindManual(personId, tagId);
        return ApiResponse.success(null);
    }

    @PutMapping("/tag-bindings/{bindingId}/review")
    public ApiResponse<Void> review(@PathVariable String bindingId,
                                    @Valid @RequestBody PersonRequests.ReviewTag request) {
        personTagService.review(bindingId, request.getStatus(), request.getReviewer());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/tag-bindings/{bindingId}")
    public ApiResponse<Void> deleteRuleResult(@PathVariable String bindingId) {
        personTagService.deleteRuleResult(bindingId);
        return ApiResponse.success(null);
    }

    @GetMapping("/tag-bindings/reviews")
    public ApiResponse<PageResult<ReviewItem>> listReviews(
            @RequestParam(value = "status", required = false, defaultValue = "PENDING") String status,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.success(personTagService.listReviews(emptyToNull(status), pageNo, pageSize));
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }
}

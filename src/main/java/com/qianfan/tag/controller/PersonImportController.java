package com.qianfan.tag.controller;

import com.qianfan.tag.common.ApiResponse;
import com.qianfan.tag.domain.ImportBatch;
import com.qianfan.tag.service.PersonExcelImportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/imports/persons")
public class PersonImportController {
    private final PersonExcelImportService service;

    public PersonImportController(PersonExcelImportService service) { this.service = service; }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("person-import-template.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(service.createTemplate());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportBatch> upload(@RequestParam("batchNo") String batchNo,
                                           @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(service.importFile(batchNo, file));
    }

    @GetMapping("/batches")
    public ApiResponse<List<ImportBatch>> batches() { return ApiResponse.success(service.recent()); }
}

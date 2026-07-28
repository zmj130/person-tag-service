package com.qianfan.tag;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.PersonTag;
import com.qianfan.tag.domain.RuleEvaluationBatch;
import com.qianfan.tag.domain.TagRuleSet;
import com.qianfan.tag.dto.IndicatorRequests;
import com.qianfan.tag.dto.PersonRequests;
import com.qianfan.tag.dto.RuleSetRequests;
import com.qianfan.tag.mapper.PersonMapper;
import com.qianfan.tag.mapper.PersonTagMapper;
import com.qianfan.tag.mapper.StructuredRuleMapper;
import com.qianfan.tag.service.IndicatorService;
import com.qianfan.tag.service.PersonExcelImportService;
import com.qianfan.tag.service.PersonService;
import com.qianfan.tag.service.PersonTagService;
import com.qianfan.tag.service.ProfileSearchService;
import com.qianfan.tag.service.StructuredRuleService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PlatformFlowIntegrationTest {
    private static final String TARGET_TAG = "10000000000000000000000000000001";

    @Autowired private IndicatorService indicatorService;
    @Autowired private PersonService personService;
    @Autowired private PersonTagService personTagService;
    @Autowired private StructuredRuleService ruleService;
    @Autowired private PersonTagMapper personTagMapper;
    @Autowired private StructuredRuleMapper structuredRuleMapper;
    @Autowired private PersonMapper personMapper;
    @Autowired private PersonExcelImportService importService;
    @Autowired private ProfileSearchService profileSearchService;

    @Test
    void structuredRuleMatchesThenExpiresWhenIndicatorChanges() {
        IndicatorDefinition flow = createNumberIndicator("TEST_ANNUAL_FLOW", "测试年度流水");
        PersonRecord person = createPerson("RULE-FLOW-001", "规则测试人员");
        indicatorService.saveImportedValue(person.getId(), flow, "12000000", "TEST_VALUE_1", new Date());

        RuleSetRequests.Condition condition = new RuleSetRequests.Condition();
        condition.setIndicatorId(flow.getId());
        condition.setOperator("GE");
        condition.setValues(Collections.singletonList("10000000"));
        RuleSetRequests.CreateDraft draft = new RuleSetRequests.CreateDraft();
        draft.setTagId(TARGET_TAG);
        draft.setMatchMode("ALL");
        draft.setConditions(Collections.singletonList(condition));

        TagRuleSet ruleSet = ruleService.publish(ruleService.createDraft(draft).getId());
        RuleEvaluationBatch matched = ruleService.recalculate(ruleSet.getId(), "RULE_FLOW_MATCH_001");
        assertEquals("SUCCESS", matched.getStatus());
        assertTrue(matched.getMatchedCount() >= 1);
        PersonTag binding = personTagMapper.find(person.getId(), TARGET_TAG);
        assertNotNull(binding);
        assertEquals("RULE", binding.getSource());
        assertEquals("PENDING", binding.getStatus());
        personTagService.applyRules(person, "LEGACY_RECHECK");
        assertNotNull(personTagMapper.findById(binding.getId()));
        assertTrue(personTagService.listReviews("PENDING", 1, 200).getRecords().stream()
                .anyMatch(item -> binding.getId().equals(item.getId())
                        && item.getMatchDetail() != null
                        && item.getMatchDetail().contains("TEST_ANNUAL_FLOW")));

        indicatorService.saveImportedValue(person.getId(), flow, "500", "TEST_VALUE_2", new Date());
        RuleEvaluationBatch expired = ruleService.recalculate(ruleSet.getId(), "RULE_FLOW_EXPIRE_001");
        assertEquals("SUCCESS", expired.getStatus());
        assertTrue(expired.getExpiredCount() >= 1);
        assertEquals("EXPIRED", personTagMapper.find(person.getId(), TARGET_TAG).getStatus());
        personTagService.deleteRuleResult(binding.getId());
        assertEquals(null, personTagMapper.findById(binding.getId()));
        assertEquals(null, structuredRuleMapper.findEvidence(
                person.getId(), TARGET_TAG, ruleSet.getId(), ruleSet.getVersion()));
    }

    @Test
    void templateIsDynamicAndInvalidWorkbookWritesNoPerson() throws Exception {
        createNumberIndicator("TEST_CREDIT_AMOUNT", "测试授信金额");
        byte[] template = importService.createTemplate();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(template))) {
            boolean found = false;
            org.apache.poi.ss.usermodel.Row header = workbook.getSheetAt(0).getRow(0);
            for (int column = 0; column < header.getLastCellNum(); column++) {
                found = found || "TEST_CREDIT_AMOUNT".equals(header.getCell(column).getStringCellValue());
            }
            assertTrue(found);
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("枚举选项")));
        }

        byte[] invalid = duplicatePersonWorkbook();
        MockMultipartFile file = new MockMultipartFile("file", "invalid.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", invalid);
        BusinessException error = assertThrows(BusinessException.class,
                () -> importService.importFile("IMPORT_INVALID_001", file));
        assertEquals("IMPORT_VALIDATION_FAILED", error.getCode());
        assertEquals(null, personMapper.findByExternalId("DUPLICATE-IMPORT-001"));
    }

    @Test
    void profileSearchIsDisabledWithoutTouchingElasticsearch() {
        assertFalse(profileSearchService.status().isEnabled());
        BusinessException error = assertThrows(BusinessException.class,
                () -> profileSearchService.rebuild());
        assertEquals("ES_DISABLED", error.getCode());
    }

    private IndicatorDefinition createNumberIndicator(String code, String name) {
        IndicatorRequests.Create request = new IndicatorRequests.Create();
        request.setCode(code);
        request.setName(name);
        request.setDataType("NUMBER");
        request.setSourceType("IMPORT");
        request.setUnit("元");
        return indicatorService.create(request);
    }

    private PersonRecord createPerson(String externalId, String name) {
        PersonRequests.UpsertPerson request = new PersonRequests.UpsertPerson();
        request.setExternalId(externalId);
        request.setName(name);
        request.setGender("男");
        request.setOccupation("学生");
        return personService.upsert(request);
    }

    private byte[] duplicatePersonWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("人员导入");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("external_id");
            header.createCell(1).setCellValue("name");
            for (int rowNo = 1; rowNo <= 2; rowNo++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNo);
                row.createCell(0).setCellValue("DUPLICATE-IMPORT-001");
                row.createCell(1).setCellValue("重复人员" + rowNo);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}

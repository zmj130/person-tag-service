package com.qianfan.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.PersonTag;
import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.dto.IndicatorRequests;
import com.qianfan.tag.dto.PersonRequests;
import com.qianfan.tag.mapper.PersonMapper;
import com.qianfan.tag.mapper.PersonTagMapper;
import com.qianfan.tag.mapper.TagMapper;
import com.qianfan.tag.service.PersonService;
import com.qianfan.tag.service.PersonTagService;
import com.qianfan.tag.service.IndicatorService;
import com.qianfan.tag.trie.TrieManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class PersonSyncIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PersonMapper personMapper;
    @Autowired private PersonTagMapper personTagMapper;
    @Autowired private PersonService personService;
    @Autowired private PersonTagService personTagService;
    @Autowired private TrieManager trieManager;
    @Autowired private TagMapper tagMapper;
    @Autowired private IndicatorService indicatorService;

    @Test
    void shouldExplainUnsupportedMethodInsteadOfReturningSystemError() throws Exception {
        mockMvc.perform(patch("/api/tags/10000000000000000000000000000001"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("当前后端不支持该操作，请确认服务已升级并重启"));
    }

    @Test
    void shouldExposeFormattedDynamicIndicatorsInPersonDetail() throws Exception {
        PersonRequests.UpsertPerson personRequest = new PersonRequests.UpsertPerson();
        personRequest.setExternalId("DETAIL-INDICATOR-001");
        personRequest.setName("指标详情测试人员");
        PersonRecord person = personService.upsert(personRequest);

        IndicatorDefinition active = createIndicator("TEST_DETAIL_ACTIVE", "是否活跃", "BOOLEAN", null);
        IndicatorDefinition amount = createIndicator("TEST_DETAIL_AMOUNT", "年度流水", "NUMBER", "元");
        indicatorService.saveImportedValue(person.getId(), active, "true", "DETAIL_TEST", new Date());
        indicatorService.saveImportedValue(person.getId(), amount, "1200.500", "DETAIL_TEST", new Date());

        mockMvc.perform(get("/api/persons/{personId}/indicators", person.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("TEST_DETAIL_ACTIVE"))
                .andExpect(jsonPath("$.data[0].name").value("是否活跃"))
                .andExpect(jsonPath("$.data[0].value").value("是"))
                .andExpect(jsonPath("$.data[1].code").value("TEST_DETAIL_AMOUNT"))
                .andExpect(jsonPath("$.data[1].value").value("1200.5 元"));
    }

    @Test
    void shouldSyncRemoteTagsAndCreateRuleCandidatesIdempotently() throws Exception {
        assertThat(tagMapper.findEnabledRuleMatches()).singleElement().satisfies(rule -> {
            assertThat(rule.getRuleId()).isEqualTo("20000000000000000000000000000001");
            assertThat(rule.getTagId()).isEqualTo("10000000000000000000000000000001");
            assertThat(rule.getKeyword()).isEqualTo("货运");
        });
        assertThat(trieManager.match("长途货运")).hasSize(1);
        String body = "{\"batchNo\":\"TEST-BATCH-001\"}";
        mockMvc.perform(post("/internal/sync/persons/incremental")
                        .header("X-Scheduler-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.successCount").value(2));

        PersonRecord person = personMapper.findByExternalId("DEMO-001");
        List<PersonTag> bindings = personTagMapper.findByPersonId(person.getId());
        assertThat(bindings).hasSize(2);
        assertThat(bindings).filteredOn(item -> "REMOTE".equals(item.getSource()))
                .extracting(PersonTag::getStatus).containsExactly("APPROVED");
        assertThat(bindings).filteredOn(item -> "RULE".equals(item.getSource()))
                .extracting(PersonTag::getStatus).containsExactly("PENDING");

        // DolphinScheduler 重试相同批次号时直接返回原结果，不重复写人员或标签。
        mockMvc.perform(post("/internal/sync/persons/incremental")
                        .header("X-Scheduler-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(2));
        assertThat(personTagMapper.findByPersonId(person.getId())).hasSize(2);

        mockMvc.perform(get("/api/persons/tag-bindings/reviews")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].personName").value("示例甲"))
                .andExpect(jsonPath("$.data.records[0].tagName").value("运输从业人员"));

        mockMvc.perform(get("/api/sync/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].batchNo").value("TEST-BATCH-001"))
                .andExpect(jsonPath("$.data.records[0].status").value("SUCCESS"));

        PersonRequests.Search search = new PersonRequests.Search();
        search.setTagIds(Arrays.asList("10000000000000000000000000000002"));
        assertThat(personService.search(search).getTotal()).isEqualTo(1);

        PersonRequests.UpsertPerson changed = new PersonRequests.UpsertPerson();
        changed.setExternalId("DEMO-001");
        changed.setName("示例甲");
        changed.setOrganization("某物流企业");
        changed.setOccupation("行政人员");
        changed.setRemark("负责内部档案整理");
        changed.setSourceUpdatedAt(new Date(System.currentTimeMillis() + 1000));
        personService.upsert(changed);
        assertThat(personTagMapper.findByPersonId(person.getId()))
                .noneMatch(item -> "RULE".equals(item.getSource()));
    }

    @Test
    void shouldUpdateGenderWhenBrowserReturnsAnOlderSourceTimestamp() throws Exception {
        PersonRequests.UpsertPerson initial = new PersonRequests.UpsertPerson();
        initial.setExternalId("MANUAL-GENDER-001");
        initial.setName("性别修改测试人员");
        initial.setGender("男");
        personService.upsert(initial);

        String body = "{\"externalId\":\"MANUAL-GENDER-001\","
                + "\"name\":\"性别修改测试人员\",\"gender\":\"女\","
                + "\"sourceUpdatedAt\":\"2000-01-01 00:00:00\"}";
        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("女"));

        assertThat(personMapper.findByExternalId("MANUAL-GENDER-001").getGender()).isEqualTo("女");
    }

    @Test
    void shouldKeepManualBindingWhenRemoteSourceIsRemoved() {
        PersonRequests.UpsertPerson request = new PersonRequests.UpsertPerson();
        request.setExternalId("MULTI-SOURCE-001");
        request.setName("多来源测试人员");
        PersonRecord person = personService.upsert(request);

        String tagId = "10000000000000000000000000000001";
        personTagService.bindManual(person.getId(), tagId, "TEST_OPERATOR");
        personTagService.bindRemoteTags(person, Collections.singletonList("TRANSPORT_WORKER"), "REMOTE-001");

        assertThat(personTagMapper.findByPersonId(person.getId()))
                .filteredOn(item -> tagId.equals(item.getTagId()))
                .extracting(PersonTag::getSource)
                .containsExactlyInAnyOrder("MANUAL", "REMOTE");

        personTagService.removeRemoteTags(person, Collections.singletonList("TRANSPORT_WORKER"));

        assertThat(personTagMapper.findByPersonId(person.getId()))
                .filteredOn(item -> tagId.equals(item.getTagId()))
                .singleElement()
                .extracting(PersonTag::getSource)
                .isEqualTo("MANUAL");
    }

    @Test
    void shouldSoftDeleteAndRestorePerson() {
        PersonRequests.UpsertPerson request = new PersonRequests.UpsertPerson();
        request.setExternalId("DELETE-RESTORE-001");
        request.setName("删除恢复测试人员");
        PersonRecord person = personService.upsert(request);

        personService.changeDeleted(person.getId(), true);
        PersonRequests.Search search = new PersonRequests.Search();
        search.setKeyword("DELETE-RESTORE-001");
        assertThat(personService.search(search).getTotal()).isZero();

        search.setIncludeDeleted(true);
        assertThat(personService.search(search).getTotal()).isEqualTo(1);
        personService.changeDeleted(person.getId(), false);
        search.setIncludeDeleted(false);
        assertThat(personService.search(search).getTotal()).isEqualTo(1);
    }

    private IndicatorDefinition createIndicator(String code, String name, String dataType, String unit) {
        IndicatorRequests.Create request = new IndicatorRequests.Create();
        request.setCode(code);
        request.setName(name);
        request.setDataType(dataType);
        request.setSourceType("IMPORT");
        request.setUnit(unit);
        return indicatorService.create(request);
    }
}

package com.qianfan.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.PersonTag;
import com.qianfan.tag.dto.PersonRequests;
import com.qianfan.tag.mapper.PersonMapper;
import com.qianfan.tag.mapper.PersonTagMapper;
import com.qianfan.tag.mapper.TagMapper;
import com.qianfan.tag.service.PersonService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @Autowired private TrieManager trieManager;
    @Autowired private TagMapper tagMapper;

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
}

package com.qianfan.tag;

import com.qianfan.tag.domain.RuleEvaluationBatch;
import com.qianfan.tag.service.StructuredRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo_data_test;MODE=PostgreSQL;DATABASE_TO_UPPER=true;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoDataIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StructuredRuleService ruleService;

    @Test
    void demoSqlIsIdempotentAndProducesExpectedRuleMatches() {
        execute("db/dm/demo/full_flow_demo_data.sql");
        execute("db/dm/demo/full_flow_demo_data.sql");

        assertEquals(6, count("SELECT COUNT(*) FROM PT_PERSON WHERE EXTERNAL_ID LIKE 'DEMO-P%'"));
        assertEquals(5, count("SELECT COUNT(*) FROM PT_INDICATOR_DEFINITION WHERE CODE LIKE 'DEMO_%'"));
        assertEquals(7, count("SELECT COUNT(*) FROM PT_TAG_DEFINITION WHERE CODE LIKE 'DEMO_%'"));
        assertEquals(30, count("SELECT COUNT(*) FROM PT_PERSON_INDICATOR WHERE IMPORT_BATCH_NO='DEMO_SEED_001'"));

        assertMatched("45000000000000000000000000000001", "DEMO_TEST_RULE_1", 2);
        assertMatched("45000000000000000000000000000002", "DEMO_TEST_RULE_2", 4);
        assertMatched("45000000000000000000000000000003", "DEMO_TEST_RULE_3", 3);
        assertMatched("45000000000000000000000000000004", "DEMO_TEST_RULE_4", 4);
        assertEquals(13, count("SELECT COUNT(*) FROM PT_PERSON_TAG PT JOIN PT_PERSON P ON P.ID=PT.PERSON_ID "
                + "WHERE P.EXTERNAL_ID LIKE 'DEMO-P%' AND PT.SOURCE_TYPE='RULE' AND PT.STATUS='PENDING'"));

        execute("db/dm/demo/cleanup_demo_data.sql");
        assertEquals(0, count("SELECT COUNT(*) FROM PT_PERSON WHERE EXTERNAL_ID LIKE 'DEMO-P%'"));
        assertEquals(0, count("SELECT COUNT(*) FROM PT_TAG_DEFINITION WHERE CODE LIKE 'DEMO_%'"));
        assertEquals(0, count("SELECT COUNT(*) FROM PT_INDICATOR_DEFINITION WHERE CODE LIKE 'DEMO_%'"));
    }

    private void assertMatched(String ruleSetId, String batchNo, int expected) {
        RuleEvaluationBatch batch = ruleService.recalculate(ruleSetId, batchNo);
        assertEquals("SUCCESS", batch.getStatus());
        assertEquals(expected, batch.getMatchedCount().intValue());
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private void execute(String classpathLocation) {
        new ResourceDatabasePopulator(new ClassPathResource(classpathLocation)).execute(dataSource);
    }
}

package com.qianfan.tag.trie;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrieIndexTest {
    @Test
    void shouldMatchChineseRulesAndDeduplicateRepeatedHits() {
        RuleMatch shortRule = new RuleMatch("r1", "t1", "货运", false);
        RuleMatch longRule = new RuleMatch("r2", "t2", "长途货运", true);
        TextNormalizer normalizer = new TextNormalizer();
        TrieIndex trie = TrieIndex.build(Arrays.asList(shortRule, longRule), normalizer);

        List<RuleMatch> matches = trie.match(normalizer.normalize("长期从事长途货运，兼营货运"));

        assertThat(matches).extracting(RuleMatch::getRuleId)
                .containsExactlyInAnyOrder("r1", "r2");
    }
}

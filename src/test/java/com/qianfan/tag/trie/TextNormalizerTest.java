package com.qianfan.tag.trie;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {
    private final TextNormalizer normalizer = new TextNormalizer();

    @Test
    void shouldNormalizeFullWidthCaseAndSeparators() {
        assertThat(normalizer.normalize("Ａ-货 运-a"))
                .isEqualTo("a货运a");
    }
}


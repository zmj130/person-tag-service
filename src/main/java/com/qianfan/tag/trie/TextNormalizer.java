package com.qianfan.tag.trie;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 匹配前统一全半角、大小写和分隔符。
 * 这是字符归一化，不具备同义词、暗语或上下文语义理解能力。
 */
@Component
public class TextNormalizer {

    public String normalize(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                result.append(current);
            }
        }
        return result.toString();
    }
}


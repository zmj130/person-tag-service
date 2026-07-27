package com.qianfan.tag.trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读 Trie 快照。构建阶段使用可变节点，发布后不再修改，因此请求线程无需加锁。
 */
public final class TrieIndex {
    private final Node root;

    private TrieIndex(Node root) {
        this.root = root;
    }

    public static TrieIndex empty() {
        return new TrieIndex(new Node());
    }

    public static TrieIndex build(List<RuleMatch> rules, TextNormalizer normalizer) {
        Node root = new Node();
        for (RuleMatch rule : rules) {
            String keyword = normalizer.normalize(rule.getKeyword());
            if (keyword.isEmpty()) {
                continue;
            }
            Node current = root;
            for (int i = 0; i < keyword.length(); i++) {
                Character character = keyword.charAt(i);
                Node child = current.children.get(character);
                if (child == null) {
                    child = new Node();
                    current.children.put(character, child);
                }
                current = child;
            }
            current.outputs.add(rule);
        }
        return new TrieIndex(root);
    }

    public List<RuleMatch> match(String normalizedText) {
        if (normalizedText == null || normalizedText.isEmpty()) {
            return Collections.emptyList();
        }
        // 相同规则在文本中出现多次时只返回一次，避免重复写标签关系。
        Map<String, RuleMatch> matches = new LinkedHashMap<String, RuleMatch>();
        for (int start = 0; start < normalizedText.length(); start++) {
            Node current = root;
            for (int cursor = start; cursor < normalizedText.length(); cursor++) {
                current = current.children.get(normalizedText.charAt(cursor));
                if (current == null) {
                    break;
                }
                for (RuleMatch output : current.outputs) {
                    matches.put(output.getRuleId(), output);
                }
            }
        }
        return new ArrayList<RuleMatch>(matches.values());
    }

    private static final class Node {
        private final Map<Character, Node> children = new LinkedHashMap<Character, Node>();
        private final List<RuleMatch> outputs = new ArrayList<RuleMatch>();
    }
}


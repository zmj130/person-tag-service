package com.qianfan.tag.trie;

import com.qianfan.tag.mapper.TagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 管理当前 Trie 快照。新树完整构建后一次性替换，查询线程不会看到半棵树。
 */
@Component
public class TrieManager implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrieManager.class);
    private final AtomicReference<TrieIndex> current = new AtomicReference<TrieIndex>(TrieIndex.empty());
    private final TagMapper tagMapper;
    private final TextNormalizer normalizer;

    public TrieManager(TagMapper tagMapper, TextNormalizer normalizer) {
        this.tagMapper = tagMapper;
        this.normalizer = normalizer;
    }

    @Override
    public void run(ApplicationArguments args) {
        rebuild();
    }

    public void rebuild() {
        List<RuleMatch> rules = tagMapper.findEnabledRuleMatches();
        current.set(TrieIndex.build(rules, normalizer));
        LOGGER.info("Trie 规则索引已重建，启用规则数={}", rules.size());
    }

    public List<RuleMatch> match(String text) {
        return current.get().match(normalizer.normalize(text));
    }
}


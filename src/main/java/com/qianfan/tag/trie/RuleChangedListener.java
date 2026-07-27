package com.qianfan.tag.trie;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 只有数据库事务提交成功才发布新 Trie，避免内存规则领先于数据库。
 */
@Component
public class RuleChangedListener {
    private final TrieManager trieManager;

    public RuleChangedListener(TrieManager trieManager) {
        this.trieManager = trieManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRuleChanged(RuleChangedEvent event) {
        trieManager.rebuild();
    }
}


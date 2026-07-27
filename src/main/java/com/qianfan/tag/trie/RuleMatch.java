package com.qianfan.tag.trie;

/** Trie 命中的规则快照。 */
public class RuleMatch {
    private String ruleId;
    private String tagId;
    private String keyword;
    private boolean autoApprove;

    public RuleMatch() {
    }

    public RuleMatch(String ruleId, String tagId, String keyword, boolean autoApprove) {
        this.ruleId = ruleId;
        this.tagId = tagId;
        this.keyword = keyword;
        this.autoApprove = autoApprove;
    }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public boolean isAutoApprove() { return autoApprove; }
    public void setAutoApprove(boolean autoApprove) { this.autoApprove = autoApprove; }
}


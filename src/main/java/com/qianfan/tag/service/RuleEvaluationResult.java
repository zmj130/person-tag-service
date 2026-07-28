package com.qianfan.tag.service;

public class RuleEvaluationResult {
    private final boolean matched;
    private final String detail;

    public RuleEvaluationResult(boolean matched, String detail) {
        this.matched = matched;
        this.detail = detail;
    }

    public boolean isMatched() { return matched; }
    public String getDetail() { return detail; }
}

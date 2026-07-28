package com.qianfan.tag.domain;

import java.util.Date;

public class TagRuleCondition {
    private String id;
    private String ruleSetId;
    private String indicatorId;
    private String operator;
    private String expectedValues;
    private Integer sortNo;
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRuleSetId() { return ruleSetId; }
    public void setRuleSetId(String ruleSetId) { this.ruleSetId = ruleSetId; }
    public String getIndicatorId() { return indicatorId; }
    public void setIndicatorId(String indicatorId) { this.indicatorId = indicatorId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getExpectedValues() { return expectedValues; }
    public void setExpectedValues(String expectedValues) { this.expectedValues = expectedValues; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}

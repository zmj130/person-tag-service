package com.qianfan.tag.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public final class RuleSetRequests {
    private RuleSetRequests() { }

    public static class CreateDraft {
        @NotBlank private String tagId;
        @NotBlank private String matchMode;
        @Valid @NotEmpty private List<Condition> conditions = new ArrayList<Condition>();
        public String getTagId() { return tagId; }
        public void setTagId(String tagId) { this.tagId = tagId; }
        public String getMatchMode() { return matchMode; }
        public void setMatchMode(String matchMode) { this.matchMode = matchMode; }
        public List<Condition> getConditions() { return conditions; }
        public void setConditions(List<Condition> conditions) { this.conditions = conditions; }
    }

    public static class Condition {
        @NotBlank private String indicatorId;
        @NotBlank private String operator;
        private List<String> values = new ArrayList<String>();
        public String getIndicatorId() { return indicatorId; }
        public void setIndicatorId(String indicatorId) { this.indicatorId = indicatorId; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public List<String> getValues() { return values; }
        public void setValues(List<String> values) { this.values = values; }
    }

    public static class Recalculate {
        @NotBlank private String batchNo;
        public String getBatchNo() { return batchNo; }
        public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    }
}

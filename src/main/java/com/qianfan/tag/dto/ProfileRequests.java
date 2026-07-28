package com.qianfan.tag.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public final class ProfileRequests {
    private ProfileRequests() { }

    public static class Search {
        private String keyword;
        private List<String> tagIds = new ArrayList<String>();
        private String tagOperator = "AND";
        @Valid private List<IndicatorFilter> indicators = new ArrayList<IndicatorFilter>();
        @NotNull private Integer pageNo = 1;
        @NotNull private Integer pageSize = 20;
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public List<String> getTagIds() { return tagIds; }
        public void setTagIds(List<String> tagIds) { this.tagIds = tagIds; }
        public String getTagOperator() { return tagOperator; }
        public void setTagOperator(String tagOperator) { this.tagOperator = tagOperator; }
        public List<IndicatorFilter> getIndicators() { return indicators; }
        public void setIndicators(List<IndicatorFilter> indicators) { this.indicators = indicators; }
        public Integer getPageNo() { return pageNo; }
        public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }

    public static class IndicatorFilter {
        private String indicatorCode;
        private String operator;
        private List<String> values = new ArrayList<String>();
        public String getIndicatorCode() { return indicatorCode; }
        public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public List<String> getValues() { return values; }
        public void setValues(List<String> values) { this.values = values; }
    }
}

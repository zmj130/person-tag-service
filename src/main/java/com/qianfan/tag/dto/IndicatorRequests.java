package com.qianfan.tag.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public final class IndicatorRequests {
    private IndicatorRequests() { }

    public static class Create {
        @NotBlank private String code;
        @NotBlank private String name;
        @NotBlank private String dataType;
        @NotBlank private String sourceType;
        private String unit;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public static class AddOption {
        @NotBlank private String code;
        @NotBlank private String label;
        private Integer sortNo = 0;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getSortNo() { return sortNo; }
        public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    }

    public static class ChangeStatus {
        @NotNull private Boolean enabled;
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
}

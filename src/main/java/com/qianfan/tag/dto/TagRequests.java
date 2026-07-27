package com.qianfan.tag.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 标签相关接口请求对象。 */
public final class TagRequests {
    private TagRequests() { }

    public static class CreateTag {
        @NotBlank private String code;
        @NotBlank private String name;
        @NotBlank private String category;
        private String description;
        @NotNull private Boolean autoApprove;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getAutoApprove() { return autoApprove; }
        public void setAutoApprove(Boolean autoApprove) { this.autoApprove = autoApprove; }
    }

    public static class CreateRule {
        @NotBlank private String keyword;
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
    }

    public static class UpdateTag {
        @NotBlank private String name;
        @NotBlank private String category;
        private String description;
        @NotNull private Boolean autoApprove;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getAutoApprove() { return autoApprove; }
        public void setAutoApprove(Boolean autoApprove) { this.autoApprove = autoApprove; }
    }

    public static class ChangeStatus {
        @NotNull private Boolean enabled;
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
}

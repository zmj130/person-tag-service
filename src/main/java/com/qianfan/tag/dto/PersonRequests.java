package com.qianfan.tag.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 人员维护、标签绑定和组合检索请求。 */
public final class PersonRequests {
    private PersonRequests() { }

    public static class UpsertPerson {
        @NotBlank private String externalId;
        @NotBlank private String name;
        private String gender;
        private String organization;
        private String occupation;
        private String address;
        private String remark;
        private Date sourceUpdatedAt;
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getOrganization() { return organization; }
        public void setOrganization(String organization) { this.organization = organization; }
        public String getOccupation() { return occupation; }
        public void setOccupation(String occupation) { this.occupation = occupation; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public Date getSourceUpdatedAt() { return sourceUpdatedAt; }
        public void setSourceUpdatedAt(Date sourceUpdatedAt) { this.sourceUpdatedAt = sourceUpdatedAt; }
    }

    public static class BindTag {
        @NotBlank private String tagId;
        @NotBlank private String operator;
        public String getTagId() { return tagId; }
        public void setTagId(String tagId) { this.tagId = tagId; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
    }

    public static class BatchBindTag {
        @NotNull private List<String> personIds;
        @NotBlank private String tagId;
        @NotBlank private String operator;
        public List<String> getPersonIds() { return personIds; }
        public void setPersonIds(List<String> personIds) { this.personIds = personIds; }
        public String getTagId() { return tagId; }
        public void setTagId(String tagId) { this.tagId = tagId; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
    }

    public static class ReviewTag {
        @NotBlank private String status;
        @NotBlank private String reviewer;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReviewer() { return reviewer; }
        public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    }

    public static class Search {
        private String keyword;
        private List<String> tagIds = new ArrayList<String>();
        private String tagOperator = "AND";
        private Boolean includeDeleted = false;
        @NotNull private Integer pageNo = 1;
        @NotNull private Integer pageSize = 20;
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public List<String> getTagIds() { return tagIds; }
        public void setTagIds(List<String> tagIds) { this.tagIds = tagIds; }
        public String getTagOperator() { return tagOperator; }
        public void setTagOperator(String tagOperator) { this.tagOperator = tagOperator; }
        public Boolean getIncludeDeleted() { return includeDeleted; }
        public void setIncludeDeleted(Boolean includeDeleted) { this.includeDeleted = includeDeleted; }
        public Integer getPageNo() { return pageNo; }
        public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }
}

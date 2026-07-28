package com.qianfan.tag.domain;

import java.util.Date;

public class PersonTagEvidence {
    private String id;
    private String personId;
    private String tagId;
    private String sourceType;
    private String ruleSetId;
    private Integer ruleVersion;
    private String batchNo;
    private String matchDetail;
    private String evidenceStatus;
    private String reviewStatus;
    private String reviewedBy;
    private Date reviewedAt;
    private Date createdAt;
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }
    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getRuleSetId() { return ruleSetId; }
    public void setRuleSetId(String ruleSetId) { this.ruleSetId = ruleSetId; }
    public Integer getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Integer ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getMatchDetail() { return matchDetail; }
    public void setMatchDetail(String matchDetail) { this.matchDetail = matchDetail; }
    public String getEvidenceStatus() { return evidenceStatus; }
    public void setEvidenceStatus(String evidenceStatus) { this.evidenceStatus = evidenceStatus; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Date getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Date reviewedAt) { this.reviewedAt = reviewedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

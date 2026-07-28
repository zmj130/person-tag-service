package com.qianfan.tag.dto;

import java.util.Date;

/** 待审核列表展示项，包含人员和标签名称，避免前端逐条补查。 */
public class ReviewItem {
    private String id;
    private String personId;
    private String personName;
    private String externalId;
    private String tagId;
    private String tagName;
    private String tagCategory;
    private String source;
    private String status;
    private String matchedKeyword;
    private String matchDetail;
    private String batchNo;
    private String reviewedBy;
    private Date reviewedAt;
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public String getTagCategory() { return tagCategory; }
    public void setTagCategory(String tagCategory) { this.tagCategory = tagCategory; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMatchedKeyword() { return matchedKeyword; }
    public void setMatchedKeyword(String matchedKeyword) { this.matchedKeyword = matchedKeyword; }
    public String getMatchDetail() { return matchDetail; }
    public void setMatchDetail(String matchDetail) { this.matchDetail = matchDetail; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Date getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Date reviewedAt) { this.reviewedAt = reviewedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}

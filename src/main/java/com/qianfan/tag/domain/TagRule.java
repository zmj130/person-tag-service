package com.qianfan.tag.domain;

import java.util.Date;

/** 一个关键词规则只指向一个标签，多个规则可以共享前缀。 */
public class TagRule {
    private String id;
    private String tagId;
    private String keyword;
    private String normalizedKeyword;
    private Integer status;
    private Integer version;
    private Date createdAt;
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getNormalizedKeyword() { return normalizedKeyword; }
    public void setNormalizedKeyword(String normalizedKeyword) { this.normalizedKeyword = normalizedKeyword; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}


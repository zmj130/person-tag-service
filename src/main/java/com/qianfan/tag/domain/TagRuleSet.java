package com.qianfan.tag.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TagRuleSet {
    private String id;
    private String tagId;
    private Integer version;
    private String matchMode;
    private String status;
    private Date publishedAt;
    private Date createdAt;
    private Date updatedAt;
    private List<TagRuleCondition> conditions = new ArrayList<TagRuleCondition>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getMatchMode() { return matchMode; }
    public void setMatchMode(String matchMode) { this.matchMode = matchMode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public List<TagRuleCondition> getConditions() { return conditions; }
    public void setConditions(List<TagRuleCondition> conditions) { this.conditions = conditions; }
}

package com.qianfan.tag.domain;

import java.util.Date;

/** 标签定义。标签编码用于对接外部系统，创建后不应随意修改。 */
public class TagDefinition {
    private String id;
    private String code;
    private String name;
    private String category;
    private String description;
    private Integer status;
    private Integer autoApprove;
    private Date createdAt;
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getAutoApprove() { return autoApprove; }
    public void setAutoApprove(Integer autoApprove) { this.autoApprove = autoApprove; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}


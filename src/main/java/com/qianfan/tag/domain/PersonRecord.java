package com.qianfan.tag.domain;

import java.util.Date;

/** 人员基础资料。示例只保留自动打标所需的通用字段，不保存证件号码等敏感信息。 */
public class PersonRecord {
    private String id;
    private String externalId;
    private String name;
    private String gender;
    private String organization;
    private String occupation;
    private String address;
    private String remark;
    private Date sourceUpdatedAt;
    private Integer deleted;
    private Date createdAt;
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}


package com.qianfan.tag.remote;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 远程系统返回的人员变更记录。 */
public class RemotePerson {
    private String externalId;
    private String name;
    private String gender;
    private String organization;
    private String occupation;
    private String address;
    private String remark;
    private Date updatedAt;
    private Boolean deleted = false;
    private List<String> tagCodes = new ArrayList<String>();
    private List<String> removedTagCodes = new ArrayList<String>();

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
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public List<String> getTagCodes() { return tagCodes; }
    public void setTagCodes(List<String> tagCodes) { this.tagCodes = tagCodes; }
    public List<String> getRemovedTagCodes() { return removedTagCodes; }
    public void setRemovedTagCodes(List<String> removedTagCodes) { this.removedTagCodes = removedTagCodes; }
}

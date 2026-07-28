package com.qianfan.tag.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class PersonImportRow {
    private int rowNo;
    private String externalId;
    private String name;
    private String gender;
    private String organization;
    private String occupation;
    private String address;
    private String remark;
    private boolean deleted;
    private Map<String, String> indicators = new LinkedHashMap<String, String>();

    public int getRowNo() { return rowNo; }
    public void setRowNo(int rowNo) { this.rowNo = rowNo; }
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
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Map<String, String> getIndicators() { return indicators; }
}

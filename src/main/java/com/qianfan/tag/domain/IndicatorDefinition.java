package com.qianfan.tag.domain;

import java.util.Date;

public class IndicatorDefinition {
    private String id;
    private String code;
    private String name;
    private String dataType;
    private String sourceType;
    private String personField;
    private String unit;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getPersonField() { return personField; }
    public void setPersonField(String personField) { this.personField = personField; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

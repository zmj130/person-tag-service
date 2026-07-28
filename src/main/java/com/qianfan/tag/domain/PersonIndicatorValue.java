package com.qianfan.tag.domain;

import java.math.BigDecimal;
import java.util.Date;

public class PersonIndicatorValue {
    private String id;
    private String personId;
    private String indicatorId;
    private String stringValue;
    private BigDecimal numberValue;
    private Date dateValue;
    private Integer booleanValue;
    private String optionCode;
    private Date periodStart;
    private Date periodEnd;
    private String sourceType;
    private String importBatchNo;
    private Date sourceUpdatedAt;
    private Date createdAt;
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }
    public String getIndicatorId() { return indicatorId; }
    public void setIndicatorId(String indicatorId) { this.indicatorId = indicatorId; }
    public String getStringValue() { return stringValue; }
    public void setStringValue(String stringValue) { this.stringValue = stringValue; }
    public BigDecimal getNumberValue() { return numberValue; }
    public void setNumberValue(BigDecimal numberValue) { this.numberValue = numberValue; }
    public Date getDateValue() { return dateValue; }
    public void setDateValue(Date dateValue) { this.dateValue = dateValue; }
    public Integer getBooleanValue() { return booleanValue; }
    public void setBooleanValue(Integer booleanValue) { this.booleanValue = booleanValue; }
    public String getOptionCode() { return optionCode; }
    public void setOptionCode(String optionCode) { this.optionCode = optionCode; }
    public Date getPeriodStart() { return periodStart; }
    public void setPeriodStart(Date periodStart) { this.periodStart = periodStart; }
    public Date getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Date periodEnd) { this.periodEnd = periodEnd; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getImportBatchNo() { return importBatchNo; }
    public void setImportBatchNo(String importBatchNo) { this.importBatchNo = importBatchNo; }
    public Date getSourceUpdatedAt() { return sourceUpdatedAt; }
    public void setSourceUpdatedAt(Date sourceUpdatedAt) { this.sourceUpdatedAt = sourceUpdatedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

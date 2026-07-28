package com.qianfan.tag.domain;

import java.util.Date;

public class RuleEvaluationBatch {
    private String id;
    private String batchNo;
    private String ruleSetId;
    private String status;
    private Integer scannedCount;
    private Integer matchedCount;
    private Integer expiredCount;
    private String errorMessage;
    private Date startedAt;
    private Date finishedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getRuleSetId() { return ruleSetId; }
    public void setRuleSetId(String ruleSetId) { this.ruleSetId = ruleSetId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getScannedCount() { return scannedCount; }
    public void setScannedCount(Integer scannedCount) { this.scannedCount = scannedCount; }
    public Integer getMatchedCount() { return matchedCount; }
    public void setMatchedCount(Integer matchedCount) { this.matchedCount = matchedCount; }
    public Integer getExpiredCount() { return expiredCount; }
    public void setExpiredCount(Integer expiredCount) { this.expiredCount = expiredCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
}

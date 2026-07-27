package com.qianfan.tag.domain;

import java.util.Date;

/** DolphinScheduler 每次调用对应一条同步批次记录。 */
public class SyncBatch {
    private String id;
    private String batchNo;
    private String syncType;
    private String status;
    private String cursorBefore;
    private String cursorAfter;
    private Integer receivedCount;
    private Integer successCount;
    private Integer failureCount;
    private String errorMessage;
    private Date startedAt;
    private Date finishedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCursorBefore() { return cursorBefore; }
    public void setCursorBefore(String cursorBefore) { this.cursorBefore = cursorBefore; }
    public String getCursorAfter() { return cursorAfter; }
    public void setCursorAfter(String cursorAfter) { this.cursorAfter = cursorAfter; }
    public Integer getReceivedCount() { return receivedCount; }
    public void setReceivedCount(Integer receivedCount) { this.receivedCount = receivedCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailureCount() { return failureCount; }
    public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
}


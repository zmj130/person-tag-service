package com.qianfan.tag.domain;

import java.util.Date;

public class ImportBatch {
    private String id;
    private String batchNo;
    private String fileName;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private String errorMessage;
    private Date startedAt;
    private Date finishedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
}

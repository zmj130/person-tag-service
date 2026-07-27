package com.qianfan.tag.dto;

import javax.validation.constraints.NotBlank;

/** 调度平台触发同步时传入的稳定批次号。 */
public class SyncRequest {
    @NotBlank private String batchNo;
    private String startCursor;
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getStartCursor() { return startCursor; }
    public void setStartCursor(String startCursor) { this.startCursor = startCursor; }
}


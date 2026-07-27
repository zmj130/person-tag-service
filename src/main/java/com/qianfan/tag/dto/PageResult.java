package com.qianfan.tag.dto;

import java.util.List;

/** 通用分页结果。 */
public class PageResult<T> {
    private final long total;
    private final int pageNo;
    private final int pageSize;
    private final List<T> records;
    public PageResult(long total, int pageNo, int pageSize, List<T> records) {
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.records = records;
    }
    public long getTotal() { return total; }
    public int getPageNo() { return pageNo; }
    public int getPageSize() { return pageSize; }
    public List<T> getRecords() { return records; }
}

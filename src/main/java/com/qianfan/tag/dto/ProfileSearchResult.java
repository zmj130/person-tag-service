package com.qianfan.tag.dto;

import java.util.List;
import java.util.Map;

public class ProfileSearchResult {
    private long total;
    private int pageNo;
    private int pageSize;
    private List<Map<String, Object>> records;
    private Map<String, List<Bucket>> aggregations;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public List<Map<String, Object>> getRecords() { return records; }
    public void setRecords(List<Map<String, Object>> records) { this.records = records; }
    public Map<String, List<Bucket>> getAggregations() { return aggregations; }
    public void setAggregations(Map<String, List<Bucket>> aggregations) { this.aggregations = aggregations; }

    public static class Bucket {
        private String key;
        private long count;
        public Bucket(String key, long count) { this.key = key; this.count = count; }
        public String getKey() { return key; }
        public long getCount() { return count; }
    }
}

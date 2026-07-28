package com.qianfan.tag.dto;

public class ProfileIndexStatus {
    private boolean enabled;
    private boolean exists;
    private String index;
    private long documentCount;

    public ProfileIndexStatus(boolean enabled, boolean exists, String index, long documentCount) {
        this.enabled = enabled;
        this.exists = exists;
        this.index = index;
        this.documentCount = documentCount;
    }
    public boolean isEnabled() { return enabled; }
    public boolean isExists() { return exists; }
    public String getIndex() { return index; }
    public long getDocumentCount() { return documentCount; }
}

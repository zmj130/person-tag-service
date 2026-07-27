package com.qianfan.tag.remote;

import java.util.ArrayList;
import java.util.List;

/** 基于游标的远程分页结果。 */
public class RemotePersonPage {
    private List<RemotePerson> records = new ArrayList<RemotePerson>();
    private String nextCursor;
    private boolean hasMore;

    public List<RemotePerson> getRecords() { return records; }
    public void setRecords(List<RemotePerson> records) { this.records = records; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
}


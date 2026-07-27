package com.qianfan.tag.remote;

/** 远程人员系统适配口，生产 HTTP 实现和本地 Mock 实现可以互换。 */
public interface RemotePersonClient {
    RemotePersonPage fetchChanges(String cursor, int pageSize);
}


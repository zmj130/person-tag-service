package com.qianfan.tag.common;

import java.util.UUID;

/** 使用应用生成 UUID，避免依赖达梦自增列或序列方言。 */
public final class Ids {
    private Ids() { }
    public static String uuid() { return UUID.randomUUID().toString().replace("-", ""); }
}

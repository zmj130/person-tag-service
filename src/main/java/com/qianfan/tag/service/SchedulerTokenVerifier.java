package com.qianfan.tag.service;

import com.qianfan.tag.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 校验调度内部接口令牌，避免同步接口暴露后被任意触发。 */
@Component
public class SchedulerTokenVerifier {
    private final byte[] expected;

    public SchedulerTokenVerifier(@Value("${scheduler.token}") String token) {
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String actual) {
        byte[] actualBytes = actual == null ? new byte[0] : actual.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actualBytes)) {
            throw new BusinessException("INVALID_SCHEDULER_TOKEN", "调度令牌无效");
        }
    }
}


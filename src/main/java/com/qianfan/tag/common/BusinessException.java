package com.qianfan.tag.common;

/** 可预期的业务异常，异常信息可以直接返回调用方。 */
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}


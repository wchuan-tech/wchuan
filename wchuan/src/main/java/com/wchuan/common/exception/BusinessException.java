package com.wchuan.common.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * 自定义业务异常类
 */
@Getter // 🔑 加上 Lombok 的 @Getter 注解，会自动生成 getCode() 方法！
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误状态码（默认 500） */
    private Integer code = 500;

    /**
     * 常用构造：仅传错误提示（默认 code 为 500）
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 常用构造：传自定义错误码 + 错误提示（如 401, 403, 1001 等）
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 完整构造：支持传入异常链
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
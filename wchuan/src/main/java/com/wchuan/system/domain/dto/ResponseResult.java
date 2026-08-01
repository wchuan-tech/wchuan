package com.wchuan.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果封装类
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态码（200:成功, 500:失败, 401:未登录, 403:无权限） */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    // ==================== 快速构造函数 ====================

    public ResponseResult(Integer code, String msg) {
        this(code, msg, null);
    }

    public ResponseResult(Integer code, T data) {
        this(code, "操作成功", data);
    }

    // ==================== 静态工厂方法（推荐在 Controller 中直接调用） ====================

    /** 成功响应 - 无数据 */
    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>(200, "操作成功", null);
    }

    /** 成功响应 - 带数据 */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(200, "操作成功", data);
    }

    /** 成功响应 - 带提示信息和数据 */
    public static <T> ResponseResult<T> success(String msg, T data) {
        return new ResponseResult<>(200, msg, data);
    }

    /** 失败响应 - 默认 500 错误码 */
    public static <T> ResponseResult<T> fail(String msg) {
        return new ResponseResult<>(500, msg, null);
    }

    /** 失败响应 - 自定义状态码和提示信息 */
    public static <T> ResponseResult<T> fail(Integer code, String msg) {
        return new ResponseResult<>(code, msg, null);
    }

    /** 失败响应 - 自定义状态码、提示信息和数据 */
    public static <T> ResponseResult<T> fail(Integer code, String msg, T data) {
        return new ResponseResult<>(code, msg, data);
    }
}
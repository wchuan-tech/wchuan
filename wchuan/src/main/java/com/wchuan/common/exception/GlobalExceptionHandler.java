package com.wchuan.common.exception;

import com.wchuan.system.domain.dto.ResponseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获自定义业务异常 BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException e) {
        // 自动转换成 code=500 的失败响应返回给前端
        return ResponseResult.fail(e.getMessage());
    }
}
package com.wchuan.common.exception;

import com.wchuan.system.domain.dto.ResponseResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 拦截自定义业务异常 BusinessException（如“用户名已存在”、“没有权限访问该用户数据”）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("请求地址 '{}', 发生业务异常: {}", request.getRequestURI(), e.getMessage());
        return ResponseResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 2. 拦截 Spring Security 权限不足异常 AccessDeniedException（403 拒绝访问）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseResult<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.error("请求地址 '{}', 权限校验不通过: {}", request.getRequestURI(), e.getMessage());
        return ResponseResult.fail(403, "没有该接口的操作权限，请联系管理员分配权限！");
    }

    /**
     * 3. 拦截请求方式不支持异常 HttpRequestMethodNotSupportedException（如 POST 接口用 GET 调）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseResult<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                    HttpServletRequest request) {
        log.error("请求地址 '{}', 不支持 '{}' 请求方式", request.getRequestURI(), e.getMethod());
        return ResponseResult.fail(405, String.format("不支持 '%s' 请求方式", e.getMethod()));
    }

    /**
     * 4. 拦截 @Valid / @Validated JSON 入参校验失败异常（如“关联的角色ID不能为空”）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        log.error("JSON 参数校验失败: {}", message);
        return ResponseResult.fail(400, message);
    }

    /**
     * 5. 拦截 URL 表单/Query 参数校验失败异常 BindException
     */
    @ExceptionHandler(BindException.class)
    public ResponseResult<Void> handleBindException(BindException e) {
        String message = Objects.requireNonNull(e.getFieldError()).getDefaultMessage();
        log.error("URL 参数校验失败: {}", message);
        return ResponseResult.fail(400, message);
    }

    /** 拦截 Controller 路径参数和普通方法参数校验失败异常。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("请求参数不合法");
        log.error("请求参数校验失败: {}", message);
        return ResponseResult.fail(400, message);
    }

    /**
     * 6. 拦截数据库 SQL 语法错 BadSqlGrammarException（如列名不存在等）
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public ResponseResult<Void> handleBadSqlGrammarException(BadSqlGrammarException e, HttpServletRequest request) {
        log.error("请求地址 '{}', 数据库 SQL 语法异常: ", request.getRequestURI(), e);
        return ResponseResult.fail(500, "数据库 SQL 异常，请检查字段是否存在！");
    }

    /**
     * 7. 兜底拦截：捕获所有未知的运行时系统异常 Exception
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("请求地址 '{}', 发生未知系统异常: ", request.getRequestURI(), e);
        return ResponseResult.fail(500, "系统发生未知错误，请联系管理员：" + e.getMessage());
    }
}

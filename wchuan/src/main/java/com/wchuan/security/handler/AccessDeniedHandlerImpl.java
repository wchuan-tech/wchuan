package com.wchuan.security.handler;

import com.alibaba.fastjson.JSON;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.common.utils.WebUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        int status = HttpStatus.FORBIDDEN.value();
        ResponseResult result = new ResponseResult(status,"权限不足");
        String json = JSON.toJSONString(result);
        // 处理异常
        WebUtils.renderString(response,status,json);
    }
}
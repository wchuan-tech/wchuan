package com.wchuan.security.handler;

import com.alibaba.fastjson.JSON;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.common.utils.WebUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        int status = HttpStatus.UNAUTHORIZED.value();
        ResponseResult result = new ResponseResult(status,"认证失败");
        String json = JSON.toJSONString(result);
        // 处理异常
        WebUtils.renderString(response,status,json);

    }
}

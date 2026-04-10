package com.wchuan.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

public class WebUtils {
    /**
     * 将字符串渲染到客户端
     *
     * @param response 响应对象
     * @param status   HTTP状态码 (例如 200, 401, 403)
     * @param string   待渲染的字符串内容
     */
    public static void renderString(HttpServletResponse response, int status, String string) {
        try {
            response.setStatus(status); // 核心修改：使用传入的状态码
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getClientIp() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr == null) {
            return "unknown"; // 非Web线程环境下返回未知
        }
        HttpServletRequest request = attr.getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

}
package com.wchuan.common.aspect;

import com.wchuan.common.annotation.Log;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.domain.entity.SysOperLog;
import com.wchuan.system.service.AsyncLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor // 自动为 final 字段生成构造器
public class LogAspect {

    private final AsyncLogService asyncLogService; // 1. 注入异步服务类

    @Around("@annotation(controllerLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, Log controllerLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 执行原始方法
        Object result;
        Exception exception = null;

        System.out.println("StartTime:" + startTime);
        try {
            result = joinPoint.proceed(); // joinPoint 表示有 @Log 标识的方法 result 表示上述方法的返回值
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            // 记录日志入库
            handleLog(joinPoint, controllerLog, exception, costTime);
        }

    }

    private void handleLog(JoinPoint joinPoint, Log controllerLog, Exception e, long costTime) {
        try {
            SysOperLog sysOperLog = new SysOperLog();

            // 1. 设置时间与耗时
            sysOperLog.setOperTime(new Date());
            sysOperLog.setCostTime(costTime);

            // 2. 获取请求信息 (URL, IP)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                sysOperLog.setOperUrl(request.getRequestURI()); // 获取请求路径
                sysOperLog.setOperIp(request.getRemoteAddr()); // 获取请求地址
                sysOperLog.setRequestMethod(request.getMethod()); // 获取请求方法
            }

            // 3. 获取当前登录人 (从 SecurityContext 获取)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
                sysOperLog.setUserId(loginUser.getUser().getId());
                sysOperLog.setOperName(loginUser.getUsername());

            } else {
                sysOperLog.setOperName("游客/未登录");
            }

            // 4. 设置注解中的信息
            sysOperLog.setTitle(controllerLog.title());
            sysOperLog.setBusinessType(controllerLog.businessType());

            // 5. 设置执行的方法名  joinPoint.getTarget() 获取被拦截的类对象
            String methodName = joinPoint.getSignature().getName();
            sysOperLog.setMethod(methodName);

            // 6. 设置状态 (判断是否有异常)
            if (e != null) {
                sysOperLog.setStatus(1); // 异常
                sysOperLog.setErrorMsg(e.getMessage());
            } else {
                sysOperLog.setStatus(0); // 正常
            }

            // 7. 插入数据库
            asyncLogService.saveLog(sysOperLog);
        } catch (Exception exp) {
            log.error("日志记录异常: {}", exp.getMessage());
        }
    }
}
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

import java.time.LocalDateTime;

/**
 * 操作日志切面。
 *
 * <p>通过 {@code @Around} 拦截标注了 {@link Log} 的方法，采集操作人、请求信息、耗时、异常等，
 * 并交由 {@link AsyncLogService} 异步落库，避免阻塞主业务线程。
 *
 * @author WChuan
 * @see Log
 * @see AsyncLogService
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor // 自动为 final 字段生成构造器
public class LogAspect {

    private final AsyncLogService asyncLogService; // 1. 注入异步服务类

    /**
     * 环绕通知：包裹目标方法执行，统计耗时并记录操作日志。
     *
     * <p><b>为什么用 {@code @Around}：</b>需要同时拿到“返回值、异常、耗时”，
     * 只有环绕通知能完整包裹方法执行前后；{@code @Before}/{@code @After} 做不到。
     *
     * @param joinPoint     {@link ProceedingJoinPoint}：{@link JoinPoint} 的子接口，
     *                      在被拦截方法执行前/后可获取上下文（方法签名、参数、目标对象），
     *                      并通过 {@link ProceedingJoinPoint#proceed()} 执行目标方法。
     * @param controllerLog 被拦截方法上的 {@link Log} 注解实例，提供 title / businessType。
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常，原样向上抛出（不吞异常）
     */
    @Around("@annotation(controllerLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, Log controllerLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        Object result;
        Exception exception = null;

        System.out.println("StartTime:" + startTime);
        try {
            // 执行目标方法（即被 @Log 标识的方法）
            // joinPoint.proceed() 触发业务逻辑执行
            // result 接收该业务方法执行后的返回值（如 ResponseResult 对象）
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            // 记录日志入库（finally 保证成功/异常都会记录）
            handleLog(joinPoint, controllerLog, exception, costTime);
        }
    }

    /**
     * 组装并异步保存操作日志。
     *
     * @param joinPoint     {@link JoinPoint}：被拦截方法的上下文；
     *                      此处通过 {@code getSignature().getName()} 获取方法名。
     *                      注意：{@code @Around} 传入的 {@link ProceedingJoinPoint} 是其子接口，可直接传入。
     * @param controllerLog {@link Log} 注解实例
     * @param e             目标方法抛出的异常（正常执行时为 {@code null}）
     * @param costTime      目标方法耗时（毫秒）
     */
    private void handleLog(JoinPoint joinPoint, Log controllerLog, Exception e, long costTime) {
        try {
            SysOperLog sysOperLog = new SysOperLog();

            // 1. 设置时间与耗时
            sysOperLog.setOperTime(LocalDateTime.now());
            sysOperLog.setCostTime(costTime);

            // 2. 获取请求信息 (URL, IP)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                sysOperLog.setOperIp(request.getRemoteAddr()); // 获取请求地址
                sysOperLog.setOperUrl(request.getRequestURI()); // 获取请求路径
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

            // 5. 通过 joinPoint.getSignature() 获取被拦截方法的签名，取出方法名
            String methodName = joinPoint.getSignature().getName();
            sysOperLog.setMethod(methodName);

            // 6. 设置状态 (判断是否有异常)
            if (e != null) {
                sysOperLog.setStatus(1); // 异常
                sysOperLog.setErrorMsg(e.getMessage());
            } else {
                sysOperLog.setStatus(0); // 正常
            }

            // 7. 异步插入数据库（@Async → 线程池 → TaskDecorator 传递 SecurityContext）
            asyncLogService.saveLog(sysOperLog);
        } catch (Exception exp) {
            log.error("日志记录异常: {}", exp.getMessage());
        }
    }
}

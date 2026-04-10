package com.wchuan.common.config;

import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. 获取主线程的上下文
        SecurityContext context = SecurityContextHolder.getContext();

        return () -> {
            try {
                // 2. 将上下文塞进子线程
                SecurityContextHolder.setContext(context);
                // 3. 执行子线程任务
                runnable.run();
            } finally {
                // 4. 关键：任务执行完必须清理，防止子线程被线程池回收后造成数据污染
                SecurityContextHolder.clearContext();
            }
        };
    }
}
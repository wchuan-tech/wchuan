package com.wchuan.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.wchuan.system.domain.vo.LoginUser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 添加多租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            /**
             * 获取租户 ID 值表达式，只支持常量（如：LongValue, StringValue）
             */
            @Override
            public Expression getTenantId() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                // 如果是匿名用户（正在登录），不强制加 tenant_id = 0
                if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                    return new LongValue(loginUser.getUser().getTenantId());
                }

                // 关键点：如果无法获取当前租户，返回 null 或者一个特殊值
                // 但更好的做法是在登录接口的 SQL 上忽略租户插件
                return new LongValue(0L);
            }

            /**
             * 获取租户字段名（数据库中的列名）
             */
            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            /**
             * 过滤不需要执行多租户隔离的表
             */
            @Override
            public boolean ignoreTable(String tableName) {
                // 这里列出不需要加 tenant_id 过滤条件的表
                List<String> ignoreTables = Arrays.asList(
                        "sys_tenant",
                        "sys_menu",
                        "sys_user_role",
                        "sys_role_menu"
                        );
                return ignoreTables.contains(tableName);
            }
        }));

        // 2. 添加分页插件 (多租户插件必须放在分页插件之前)
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());

        return interceptor;
    }
}
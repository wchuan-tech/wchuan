package com.wchuan.security.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.wchuan.system.domain.vo.LoginUser;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /** 默认租户ID（用于无登录态如注册、定时任务等场景的兜底） */
    private static final Long DEFAULT_TENANT_ID = 1L;

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = null;
        Long tenantId = DEFAULT_TENANT_ID;

        // 只要实体类里有 tenantId 字段，且当前用户已登录，就自动填充
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            userId = loginUser.getUser().getId();
            if (loginUser.getUser().getTenantId() != null) {
                tenantId = loginUser.getUser().getTenantId();
            }
        }

        // 自动填充租户 ID
        this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);

        // 3. 将 Date.class 替换为 LocalDateTime.class
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, userId);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时通常不需要修改租户ID
    }
}

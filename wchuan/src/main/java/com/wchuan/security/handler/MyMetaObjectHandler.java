package com.wchuan.security.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.wchuan.system.domain.vo.LoginUser;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 只要实体类里有 tenantId 字段，且当前用户已登录，就自动填充
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            System.out.println("MyMetaObject loginUser.getUserName:" + loginUser.getUsername());
            this.strictInsertFill(metaObject,
                    "tenantId",
                    Long.class,
                    loginUser.getUser().getTenantId());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时通常不需要修改租户ID
    }
}

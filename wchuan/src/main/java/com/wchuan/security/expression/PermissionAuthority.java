package com.wchuan.security.expression;


import com.wchuan.common.utils.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("ss")
public class PermissionAuthority {

    public boolean hasPerm(String permission){
        System.out.println("当前校验权限：" + permission);
        System.out.println("当前用户权限：" + SecurityContextHolder.getContext().getAuthentication().getAuthorities());

        if(SecurityUtils.isAdmin())
            return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(permission));
    }
}

package com.wchuan.common.utils;

import com.wchuan.system.domain.vo.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

public class SecurityUtils {

    /**
     * 判断当前用户是否为管理员
     */
    public static boolean isAdmin(){
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        for(GrantedAuthority authoritiy : authorities){
            if("*".equals(authoritiy.getAuthority())){
                return true;
            }
        }
        return false;
    }

    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUser().getId();
        }
        throw new RuntimeException("登录状态已过期");
    }

}

package com.wchuan.security.expression;


import com.wchuan.common.utils.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("ss")
public class PermissionAuthority {

    public boolean hasPerm(String permission){
        log.debug("校验权限: {}", permission);

        if(SecurityUtils.isAdmin()) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals(permission));
    }
}

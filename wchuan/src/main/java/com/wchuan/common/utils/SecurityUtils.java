package com.wchuan.common.utils;

import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.domain.vo.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * 1. 判断【当前登录用户】是否为超级管理员
     */
    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            SysUser user = loginUser.getUser();
            // 超管身份只信任不可由菜单编辑接口修改的用户主键。
            return user != null && isAdmin(user.getId());
        }
        return false;
    }

    /**
     * 2. 判断【指定用户 ID】是否为超级管理员（主键为 1 默认为超管）
     */
    public static boolean isAdmin(Long userId) {
        return Long.valueOf(1L).equals(userId);
    }

    /**
     * 获取当前登录用户 ID
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUser().getId();
        }
        throw new RuntimeException("登录状态已过期或未登录");
    }

    /**
     * 获取当前登录用户对象
     */
    public static SysUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUser();
        }
        throw new RuntimeException("登录状态已过期或未登录");
    }
}

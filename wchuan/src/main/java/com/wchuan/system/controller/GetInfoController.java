package com.wchuan.system.controller;

import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.Tenant;
import com.wchuan.system.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 获取用户信息返回给前端
 */

@RestController
@RequiredArgsConstructor
public class GetInfoController {

    private final TenantMapper tenantMapper;

    @GetMapping("/user/getInfo")
    public ResponseResult<?> getInfo() {
        // 1. 从 SecurityContextHolder 拿到当前用户
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<String> perms = loginUser.getPermissions(); // 获取用户权限
        // 2. 封装成 Map 或 DTO 返回
        Map<String, Object> map = new HashMap<>();
        map.put("id",loginUser.getUser().getId());
        map.put("userName", loginUser.getUsername());
        // 如果 permissions 为空，给一个空集合而不是 null
        map.put("permissions", perms != null ? perms : new ArrayList<>());

        Long tenantId = loginUser.getUser().getTenantId();
        map.put("tenantId", loginUser.getUser().getTenantId());

        Tenant tenant = tenantMapper.selectById(tenantId);
        map.put("tenantName", tenant.getTenantName());

        return new ResponseResult<>(200, "操作成功", map);
    }
}

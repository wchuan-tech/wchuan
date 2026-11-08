package com.wchuan.system.controller;

import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.dto.UserCreateDTO;
import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户信息 Controller 控制层
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Resource
    private IUserService userService;

    /**
     * 1. 查询用户列表（🔑 自动触发 @DataScope 数据权限过滤）
     */
    @PreAuthorize("@ss.hasPerm('system:user:list')")
    @GetMapping("/list")
    public ResponseResult<List<SysUser>> list(SysUser user) {
        List<SysUser> list = userService.selectUserList(user);
        return ResponseResult.success(list);
    }

    /**
     * 2. 获取用户详细信息（🔑 带数据权限越权访问校验）
     */
    @PreAuthorize("@ss.hasPerm('system:user:query')")
    @GetMapping("/{userId}")
    public ResponseResult<SysUser> getInfo(@PathVariable Long userId) {
        SysUser user = userService.selectUserById(userId);
        return ResponseResult.success(user);
    }

    /**
     * 3. 新增用户
     */
    @PreAuthorize("@ss.hasPerm('system:user:add')")
    @PostMapping
    public ResponseResult<String> add(@Validated @RequestBody UserCreateDTO dto) {
        userService.insertUserRole(dto);
        return ResponseResult.success("新增用户成功");
    }

    /**
     * 4. 修改用户（🔑 带数据权限越权修改校验）
     */
    @PreAuthorize("@ss.hasPerm('system:user:edit')")
    @PutMapping
    public ResponseResult<String> edit(@RequestBody SysUser user) {
        userService.updateUser(user);
        return ResponseResult.success("修改用户成功");
    }

    /**
     * 5. 删除用户（🔑 带防删超管、防删自己及越权删除保护）
     */
    @PreAuthorize("@ss.hasPerm('system:user:remove')")
    @DeleteMapping("/{userId}")
    public ResponseResult<String> remove(@PathVariable Long userId) {
        userService.deleteUserById(userId);
        return ResponseResult.success("删除用户成功");
    }

    /**
     * 6. 重置密码（🔑 带越权校验）
     */
    @PreAuthorize("@ss.hasPerm('system:user:resetPwd')")
    @PutMapping("/resetPwd")
    public ResponseResult<String> resetPwd(@RequestBody SysUser user) {
        userService.resetUserPwd(user.getId(), user.getPassword());
        return ResponseResult.success("重置密码成功");
    }

    /**
     * 7. 修改状态（解冻/账号停用，🔑 带越权校验）
     */
    @PreAuthorize("@ss.hasPerm('system:user:edit')")
    @PutMapping("/changeStatus")
    public ResponseResult<String> changeStatus(@RequestBody SysUser user) {
        userService.updateUserStatus(user);
        return ResponseResult.success("修改用户状态成功");
    }

}
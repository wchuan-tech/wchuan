package com.wchuan.system.controller;

import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.dto.RoleMenuAuthDTO;
import com.wchuan.system.domain.entity.SysRole;
import com.wchuan.system.service.ISysRoleService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理 Controller 控制层
 */
@RestController
@RequestMapping("/system/role") // 🔑 对应前端请求路径 /system/role
public class SysRoleController {

    @Resource
    private ISysRoleService roleService;

    /**
     * 1. 获取角色列表（对应前端 GET /system/role/list）
     */
    @PreAuthorize("@ss.hasPerm('system:role:list')")
    @GetMapping("/list")
    public ResponseResult<List<SysRole>> list(SysRole role) {
        List<SysRole> list = roleService.selectRoleList(role);
        return ResponseResult.success(list);
    }

    /**
     * 2. 根据角色 ID 获取详细信息（对应前端 GET /system/role/{roleId}）
     */
    @PreAuthorize("@ss.hasPerm('system:role:query')")
    @GetMapping("/{roleId}")
    public ResponseResult<SysRole> getInfo(@PathVariable Long roleId) {
        return ResponseResult.success(roleService.selectRoleById(roleId));
    }

    /**
     * 3. 新增角色（对应前端 POST /system/role）
     */
    @PreAuthorize("@ss.hasPerm('system:role:add')")
    @PostMapping
    public ResponseResult<String> add(@Validated @RequestBody SysRole role) {
        roleService.insertRole(role);
        return ResponseResult.success("新增角色成功");
    }

    /**
     * 4. 修改角色基本信息（对应前端 PUT /system/role）
     */
    @PreAuthorize("@ss.hasPerm('system:role:edit')")
    @PutMapping
    public ResponseResult<String> edit(@Validated @RequestBody SysRole role) {
        roleService.updateRole(role);
        return ResponseResult.success("修改角色成功");
    }

    /**
     * 🔑 5. 修改角色数据权限（对应前端 PUT /system/role/dataScope，核心方法！）
     */
    @PreAuthorize("@ss.hasPerm('system:role:edit')")
    @PutMapping("/dataScope")
    public ResponseResult<String> dataScope(@RequestBody SysRole role) {
        roleService.authDataScope(role);
        return ResponseResult.success("分配数据权限成功");
    }

    /** 保存角色菜单及按钮权限。 */
    @PreAuthorize("@ss.hasPerm('system:role:edit')")
    @PutMapping("/menu")
    public ResponseResult<String> authRoleMenu(@Validated @RequestBody RoleMenuAuthDTO authDTO) {
        roleService.authRoleMenu(authDTO);
        return ResponseResult.success("菜单权限分配成功", null);
    }

    /**
     * 6. 删除角色（对应前端 DELETE /system/role/{roleId}）
     */
    @PreAuthorize("@ss.hasPerm('system:role:remove')")
    @DeleteMapping("/{roleId}")
    public ResponseResult<String> remove(@PathVariable Long roleId) {
        roleService.deleteRoleById(roleId);
        return ResponseResult.success("删除角色成功");
    }
}

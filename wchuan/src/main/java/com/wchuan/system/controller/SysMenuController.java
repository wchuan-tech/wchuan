package com.wchuan.system.controller;

import com.wchuan.common.annotation.Log;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.domain.dto.MenuTreeNodeDTO;
import com.wchuan.system.domain.dto.RoleMenuTreeDTO;
import com.wchuan.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.wchuan.common.enums.BusinessTypeEnum.*;

@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final MenuService menuService;

    /**
     * 获取完整菜单树
     */
    @GetMapping("/tree")
    public ResponseResult<List<SysMenu>> tree() {
        return ResponseResult.success(menuService.selectMenuTree());
    }

    /** 菜单管理列表，包含目录、菜单和按钮。 */
    @PreAuthorize("@ss.hasPerm('system:menu:list')")
    @GetMapping("/list")
    public ResponseResult<List<SysMenu>> list() {
        return ResponseResult.success(menuService.selectMenuList());
    }

    /** 查询菜单详情。 */
    @PreAuthorize("@ss.hasPerm('system:menu:query')")
    @GetMapping("/{id:\\d+}")
    public ResponseResult<SysMenu> getInfo(@PathVariable Long id) {
        return ResponseResult.success(menuService.getById(id));
    }

    /** 获取用于角色授权的完整菜单与按钮权限树。 */
    @PreAuthorize("@ss.hasPerm('system:role:query')")
    @GetMapping("/treeselect")
    public ResponseResult<List<MenuTreeNodeDTO>> treeSelect() {
        return ResponseResult.success(menuService.selectMenuTreeSelect());
    }

    /** 获取完整权限树及指定角色已勾选节点。 */
    @PreAuthorize("@ss.hasPerm('system:role:query')")
    @GetMapping("/roleMenuTreeselect/{roleId:\\d+}")
    public ResponseResult<RoleMenuTreeDTO> roleMenuTreeSelect(@PathVariable Long roleId) {
        return ResponseResult.success(menuService.selectRoleMenuTree(roleId));
    }

    /**
     * 新增菜单
     */
    @Log(title = "菜单管理", businessType = INSERT)
    @PreAuthorize("@ss.hasPerm('system:menu:add')")
    @PostMapping
    public ResponseResult<Void> add(@Validated @RequestBody SysMenu menu) {
        menuService.saveMenu(menu);
        return ResponseResult.success("新增成功", null);
    }

    /**
     * 修改菜单
     */
    @Log(title = "菜单管理", businessType = UPDATE)
    @PreAuthorize("@ss.hasPerm('system:menu:edit')")
    @PutMapping
    public ResponseResult<Void> edit(@Validated @RequestBody SysMenu menu) {
        menuService.updateMenu(menu);
        return ResponseResult.success("修改成功", null);
    }

    /**
     * 删除菜单
     */
    @Log(title = "菜单管理", businessType = DELETE)
    @PreAuthorize("@ss.hasPerm('system:menu:remove')")
    @DeleteMapping("/{id:\\d+}")
    public ResponseResult<Void> remove(@PathVariable Long id) {
        menuService.deleteMenuById(id);
        return ResponseResult.success("删除成功", null);
    }

    /**
     * 权限拓扑自愈接口 (运维专用)
     */
    @Log(title = "系统自愈", businessType = UPDATE)
    @PreAuthorize("@ss.hasPerm('system:menu:repair')") // 只有高权账号可调
    @PostMapping("/repair")
    public ResponseResult<Void> repair() {
        menuService.selfHealing();
        return ResponseResult.success("系统权限拓扑自愈成功", null);
    }

}

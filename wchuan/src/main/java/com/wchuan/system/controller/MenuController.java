package com.wchuan.system.controller;

import com.wchuan.common.annotation.Log;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.wchuan.common.enums.BusinessTypeEnum.*;

@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 获取完整菜单树
     */
    @GetMapping("/treeselect")
    public ResponseResult<List<SysMenu>> treeselect() {
        return new ResponseResult<>(200, "查询成功", menuService.selectMenuTree());
    }

    /**
     * 新增菜单
     */
    @Log(title = "菜单管理", businessType = INSERT)
    @PreAuthorize("@ss.hasPerm('system:menu:add')")
    @PostMapping
    public ResponseResult<?> add(@RequestBody SysMenu menu) {
        menuService.validateMenu(menu);
        menuService.save(menu);
        return new ResponseResult<>(200, "新增成功");
    }

    /**
     * 修改菜单
     */
    @Log(title = "菜单管理", businessType = UPDATE)
    @PreAuthorize("@ss.hasPerm('system:menu:edit')")
    @PutMapping
    public ResponseResult<?> edit(@RequestBody SysMenu menu) {
        menuService.updateMenu(menu);
        return new ResponseResult<>(200, "修改成功");
    }

    /**
     * 删除菜单
     */
    @Log(title = "菜单管理", businessType = DELETE)
    @PreAuthorize("@ss.hasPerm('system:menu:remove')")
    @DeleteMapping("/{id}")
    public ResponseResult<?> remove(@PathVariable Long id) {
        // 简单逻辑：如果有子菜单则不允许删除
        if (menuService.lambdaQuery().eq(SysMenu::getParentId, id).count() > 0) {
            return new ResponseResult<>(500, "存在子菜单，不允许删除");
        }
        menuService.removeById(id);
        return new ResponseResult<>(200, "删除成功");
    }

    /**
     * 权限拓扑自愈接口 (运维专用)
     */
    @Log(title = "系统自愈", businessType = UPDATE)
    @PreAuthorize("@ss.hasPerm('system:menu:repair')") // 只有高权账号可调
    @PostMapping("/repair")
    public ResponseResult<?> repair() {
        menuService.selfHealing();
        return new ResponseResult<>(200, "系统权限拓扑自愈成功");
    }
}
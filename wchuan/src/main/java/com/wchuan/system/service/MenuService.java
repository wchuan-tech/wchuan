package com.wchuan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wchuan.system.domain.entity.SysMenu;

import java.util.List;

public interface MenuService extends IService<SysMenu> {
    /**
     * 查询完整菜单树
     */
    List<SysMenu> selectMenuTree();

    /**
     * 递归构建树结构
     */
    List<SysMenu> buildTree(List<SysMenu> list, Long parentId);

    /**
     * 保存菜单（含自动归位与校验）
     */
    boolean saveMenu(SysMenu menu);

    /**
     * 更新菜单（含自动归位与校验）
     */
    boolean updateMenu(SysMenu menu);

    /**
     * 统一业务校验逻辑（调用验证器）
     */
    void validateMenu(SysMenu menu);

    void selfHealing();
}

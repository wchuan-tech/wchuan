package com.wchuan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wchuan.system.domain.entity.SysMenu;

import java.util.List;

public interface MenuService extends IService<SysMenu> {
    /** 查询完整菜单树 */
    List<SysMenu> selectMenuTree();

    /** 新增菜单（含逻辑处理） */
    void saveMenu(SysMenu menu);

    /** 修改菜单（含级联更新） */
    void updateMenu(SysMenu menu);

    /** 校验菜单合法性 */
    void validateMenu(SysMenu menu);

    /** 拓扑结构自动修复 */
    void selfHealing();
}

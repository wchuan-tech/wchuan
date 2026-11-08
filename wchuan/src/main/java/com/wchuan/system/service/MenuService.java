package com.wchuan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.domain.dto.MenuTreeNodeDTO;
import com.wchuan.system.domain.dto.RoleMenuTreeDTO;

import java.util.List;

public interface MenuService extends IService<SysMenu> {
    /** 查询完整菜单树 */
    List<SysMenu> selectMenuTree();

    /** 查询菜单管理使用的完整目录、菜单和按钮树。 */
    List<SysMenu> selectMenuList();

    /** 查询包含目录、菜单和按钮的完整权限树。 */
    List<MenuTreeNodeDTO> selectMenuTreeSelect();

    /** 查询完整权限树及指定角色已直接关联的菜单节点。 */
    RoleMenuTreeDTO selectRoleMenuTree(Long roleId);

    /** 新增菜单（含逻辑处理） */
    void saveMenu(SysMenu menu);

    /** 修改菜单（含级联更新） */
    void updateMenu(SysMenu menu);

    /** 删除菜单，包含子节点、角色关联和核心菜单保护。 */
    void deleteMenuById(Long id);

    /** 校验菜单合法性 */
    void validateMenu(SysMenu menu);

    /** 拓扑结构自动修复 */
    void selfHealing();
}

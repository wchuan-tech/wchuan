package com.wchuan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wchuan.system.domain.entity.SysRole;
import com.wchuan.system.domain.dto.RoleMenuAuthDTO;

import java.util.List;

/**
 * 角色管理 业务层接口
 */
public interface ISysRoleService extends IService<SysRole> {

    /**
     * 查询角色列表
     */
    List<SysRole> selectRoleList(SysRole role);

    /**
     * 根据 ID 查询角色详情
     */
    SysRole selectRoleById(Long roleId);

    /**
     * 新增角色
     */
    boolean insertRole(SysRole role);

    /**
     * 修改角色
     */
    boolean updateRole(SysRole role);

    /**
     * 🔑 授权修改角色数据权限（更新 data_scope 并写入 sys_role_dept）
     */
    boolean authDataScope(SysRole role);

    /** 保存角色菜单及按钮权限。 */
    boolean authRoleMenu(RoleMenuAuthDTO authDTO);

    /**
     * 删除角色
     */
    boolean deleteRoleById(Long roleId);

    /**
     * 校验角色名称唯一性
     */
    boolean checkRoleNameUnique(SysRole role);

    /**
     * 校验角色字符唯一性
     */
    boolean checkRoleKeyUnique(SysRole role);
}

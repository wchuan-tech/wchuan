package com.wchuan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wchuan.system.domain.entity.SysDept;
import com.wchuan.system.domain.dto.RoleDeptTreeDTO;

import java.util.List;

/**
 * 部门管理 业务层接口
 */
public interface ISysDeptService extends IService<SysDept> {

    /**
     * 查询部门树形列表（带筛选）
     */
    List<SysDept> selectDeptList(SysDept dept);

    /** 查询部门树，排除指定部门及其所有子孙部门。 */
    List<SysDept> selectDeptListExcludeChild(Long deptId);

    /** 查询部门树及指定角色的自定义数据权限节点。 */
    RoleDeptTreeDTO selectRoleDeptTree(Long roleId);

    /** 查询指定角色在 sys_role_dept 中绑定的部门 ID。 */
    List<Long> selectDeptListByRoleId(Long roleId);

    /** 构建供前端树组件使用的部门层级结构。 */
    List<SysDept> buildDeptTreeSelect(List<SysDept> depts);

    /**
     * 构建前端树形结构数据
     */
    List<SysDept> buildDeptTree(List<SysDept> depts);

    /**
     * 根据部门 ID 查询部门详情
     */
    SysDept selectDeptById(Long deptId);

    /**
     * 新增部门（自动计算 ancestors 祖级链）
     */
    boolean insertDept(SysDept dept);

    /**
     * 修改部门（联动更新子部门 ancestors 祖级链）
     */
    boolean updateDept(SysDept dept);

    /**
     * 校验部门名称同级唯一性
     */
    boolean checkDeptNameUnique(SysDept dept);

    /**
     * 检查部门下是否存在关联用户
     */
    boolean checkDeptExistUser(Long deptId);

    /**
     * 检查是否存在下级子部门
     */
    boolean hasChildByDeptId(Long deptId);

    /**
     * 删除部门（带安全防护）
     */
    boolean deleteDeptById(Long deptId);
}

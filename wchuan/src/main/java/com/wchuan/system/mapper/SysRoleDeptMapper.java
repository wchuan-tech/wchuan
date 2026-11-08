package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysRoleDept;

import java.util.List;

public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {
    int deleteRoleDeptByRoleId(Long roleId);

    int batchRoleDept(List<SysRoleDept> roleDeptList);

    int deleteRoleDept(Long[] roleIds);
}

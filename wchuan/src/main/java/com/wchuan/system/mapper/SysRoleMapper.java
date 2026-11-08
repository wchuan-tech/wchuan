package com.wchuan.system.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
    List<SysRole> selectRoleList(SysRole sysRole);

    int insertRole(SysRole role);

    int updateRole(SysRole role);
}

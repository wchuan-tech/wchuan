package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色和菜单关联表 数据层 Mapper
 */
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    int insertBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}

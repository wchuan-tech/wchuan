package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {
    /**
     * 根据条件分页查询用户列表（带数据权限过滤）
     *
     * @param sysUser 用户查询条件（继承 BaseEntity，包含数据权限参数）
     * @return 用户信息集合
     */
    List<SysUser> selectUserList(SysUser sysUser);


    SysUser selectUserById(@Param("userId") Long userId);

    @Select("""
            SELECT *
            FROM sys_user
            WHERE id = #{userId}
              AND del_flag = 0
              AND status = '0'
            """)
    SysUser selectActiveUser(@Param("userId") Long userId);

    @Select("""
            SELECT *
            FROM sys_user
            WHERE id = #{userId}
              AND del_flag = 0
              AND status = '0'
            FOR UPDATE
            """)
    SysUser selectActiveUserForUpdate(@Param("userId") Long userId);
}

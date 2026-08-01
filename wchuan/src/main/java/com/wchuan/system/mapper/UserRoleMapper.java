package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysUserRole; // 如果你没创建 SysUserRole 实体类，看下面注脚
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<SysUserRole> {
}
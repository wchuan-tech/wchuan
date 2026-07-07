package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysMenu;

import java.util.List;

public interface MenuMapper extends BaseMapper<SysMenu> {

    List<String> selectMenuNameByUserId(Long userId);

    @InterceptorIgnore(tenantLine = "true") // 菜单通常是全局的，忽略租户隔离
    List<SysMenu> selectMenuTreeByUserId(Long userId);
}

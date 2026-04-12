package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysMenu;

import java.util.List;

public interface MenuMapper extends BaseMapper<SysMenu> {
    List<String> selectMenuNameByUserId(Long userId);
}

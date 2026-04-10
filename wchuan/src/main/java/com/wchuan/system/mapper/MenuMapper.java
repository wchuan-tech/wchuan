package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.Menu;

import java.util.List;

public interface MenuMapper extends BaseMapper<Menu> {
    List<String> selectMenuNameByUserId(Long userId);
}

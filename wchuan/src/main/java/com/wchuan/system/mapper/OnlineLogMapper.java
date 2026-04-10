package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysUserOnlineLog;

@InterceptorIgnore(tenantLine = "true")
public interface OnlineLogMapper extends BaseMapper<SysUserOnlineLog> {
}
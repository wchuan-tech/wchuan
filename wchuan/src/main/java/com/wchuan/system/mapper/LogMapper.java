package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysOperLog;

@InterceptorIgnore(tenantLine = "true")
public interface LogMapper extends BaseMapper<SysOperLog> {
}

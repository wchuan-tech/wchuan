package com.wchuan.system.service;

import com.wchuan.system.domain.entity.SysOperLog;

public interface AsyncLogService {
    void saveLog(SysOperLog operLog);
}

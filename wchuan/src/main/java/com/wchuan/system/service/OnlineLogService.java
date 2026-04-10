package com.wchuan.system.service;

import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.domain.entity.SysUserOnlineLog;

public interface OnlineLogService {
    void settleOnlineLog(SysUserOnlineLog onlineLog, int exitType);
    void createOnlineLog(LoginUser loginUser);
    void logoutSettle(LoginUser loginUser);
    void refreshOnlineUserTime(LoginUser loginUser);
}

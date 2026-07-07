package com.wchuan.system.service.impl;

import com.wchuan.common.utils.RedisCache;
import com.wchuan.common.utils.WebUtils;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.domain.entity.SysUserOnlineLog;
import com.wchuan.system.mapper.OnlineLogMapper;
import com.wchuan.system.service.OnlineLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.wchuan.common.constants.Constants.ONLINE_LOG_REDIS_TTL;
import static com.wchuan.common.enums.RedisKeyEnum.ONLINE_USER_LOG_KEY;


@Service
@RequiredArgsConstructor
public class OnlineLogServiceImpl implements OnlineLogService {

    private final OnlineLogMapper onlineLogMapper;

    private final RedisCache redisCache;

    /**
     * 登录时创建在线日志存入 Redis
     */
    @Override
    public void createOnlineLog(LoginUser loginUser) {
        String ip = WebUtils.getClientIp();
        String userId = loginUser.getUser().getId().toString();

        SysUserOnlineLog onlineLog = new SysUserOnlineLog();
        onlineLog.setUserId(loginUser.getUser().getId());
        onlineLog.setUserName(loginUser.getUsername());
        onlineLog.setIpAddr(ip);
        onlineLog.setLoginTime(new Date());
        onlineLog.setLastActivityTime(new Date());

        // 在 Web 线程（有上下文）时，抓取租户 ID 存入 Redis 对象
        onlineLogMapper.insert(onlineLog);

        // 存入 Redis 在线有效期初始化为 41 分钟
        redisCache.setCacheObject(ONLINE_USER_LOG_KEY + userId, onlineLog, ONLINE_LOG_REDIS_TTL, TimeUnit.MINUTES);
    }

    /**
     * 登出时结算在线日志
     */
    @Override
    public void logoutSettle(LoginUser loginUser) {
        String userId = loginUser.getUser().getId().toString();
        String onlineKey = ONLINE_USER_LOG_KEY + userId;

        SysUserOnlineLog onlineLog = redisCache.getCacheObject(onlineKey);
        if (onlineLog != null) {
            onlineLog.setLastActivityTime(new Date());
            settleOnlineLog(onlineLog, 0); // 0-正常退出
            redisCache.deleteObject(onlineKey);
        }

    }

    @Override
    public void refreshOnlineUserTime(LoginUser loginUser) {
        String userId = loginUser.getUser().getId().toString();
        String onlineKey = ONLINE_USER_LOG_KEY + userId;

        SysUserOnlineLog onlineLog = redisCache.getCacheObject(onlineKey);
        if (onlineLog != null) {
            onlineLog.setLastActivityTime(new Date());
            // 每次刷新为 41 分钟
            redisCache.setCacheObject(onlineKey, onlineLog, ONLINE_LOG_REDIS_TTL, TimeUnit.MINUTES);
        }

    }

    /**
     * 通用结算方法
     * @param onlineLog Redis中的在线信息
     * @param exitType 退出类型
     */
    public void settleOnlineLog(SysUserOnlineLog onlineLog, int exitType) {

        if (onlineLog == null) return;

        // 计算时长
        long duration = (onlineLog.getLastActivityTime().getTime() - onlineLog.getLoginTime().getTime()) / 1000;
        onlineLog.setDuration(Math.max(0, duration));
        onlineLog.setExitType(exitType);

        // 此时 onlineLog 里的 id 是 null（因为它是从 Redis 来的，还没进过库）
        // 而数据库 sys_user_online_log 的 id 是自增的。
        // 直接 insert 这个对象即可，不需要重新 new 之后一个一个 set
        onlineLogMapper.updateById(onlineLog);
    }
}
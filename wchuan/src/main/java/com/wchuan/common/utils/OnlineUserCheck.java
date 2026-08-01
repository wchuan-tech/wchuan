package com.wchuan.common.utils;

import com.wchuan.common.constants.Constants;
import com.wchuan.system.domain.entity.SysUserOnlineLog;
import com.wchuan.system.service.OnlineLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

@Component
@Slf4j
@RequiredArgsConstructor // 自动为 final 字段生成构造器
public class OnlineUserCheck {

    private final RedisCache redisCache;

    private final OnlineLogService onlineLogService;

    // 每 10 分钟检查一次

    /**
     * 秒 分 时 日 月 周
     * 0  0/10 * * * ?
     * 0/10：每 10 分钟
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void autoSettleOnlineUsers() {
        System.out.println("------ 正在扫描在线用户 ------");
        Collection<String> keys = redisCache.keys("online_log:*");
        LocalDateTime now = LocalDateTime.now();

        for (String key : keys) {
            // 从 Redis 中获取在线用户
            SysUserOnlineLog online = redisCache.getCacheObject(key);

            if (online == null) {
                continue;
            }

            System.out.println("OnlineCheck userName Now:" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ":" + online.getUserName());
            // 如果最后一次活跃距离现在超过 30 分钟（认为已离线）
            long inactiveMs = Duration.between(online.getLastActivityTime(), now).toMillis();

            if (inactiveMs > Constants.OFFLINE_MS) {
                onlineLogService.settleOnlineLog(online, 1); // 1-超时自动结算
                redisCache.deleteObject(key);
                log.info(">>>> 用户 {} 超过 30 分钟未操作，会话已自动结算入库。", online.getUserName());
            }
        }
    }

}
package com.wchuan.common.constants;

public class Constants {
    /** JWT 有效期（分钟） */
    public static final int JWT_TTL = 120;

    /** Redis 会话有效期（分钟） */
    public static final int SESSION_TTL = 30;

    /** 过期时间：30 分钟  */
    public static final int EXPIRE_TIME = 30;

    /** 令牌自动续期阈值 10 分钟） */
    public static final int REFRESH_THRESHOLD = 20 * 30; // 600s

    /** 在线日志 Redis 缓存时间（分钟）- 必须大于 SESSION_TTL + 扫描周期 */
    public static final int ONLINE_LOG_REDIS_TTL = 60;

    /** 判定为离线的无操作时长（毫秒） */
    public static final long OFFLINE_MS = 30 * 60 * 1000L;
}
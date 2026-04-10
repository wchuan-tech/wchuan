package com.wchuan.system.service.impl;

import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.service.TokenService;
import com.wchuan.common.utils.JwtUtil;
import com.wchuan.common.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.wchuan.common.constants.Constants.*;
import static com.wchuan.common.enums.RedisKeyEnum.LOGIN_TOKEN_KEY;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisCache redisCache;


    // 将数据库查到的信息存到 Redis 中时才创建 jwt  jwt 要到第二次验证时才使用
    @Override
    public String createToken(LoginUser loginUser) {
        String userId = loginUser.getUser().getId().toString();
        // 1. 刷新 Redis 缓存时间
        String redisKey = LOGIN_TOKEN_KEY + loginUser.getUser().getId();
        redisCache.setCacheObject(redisKey, loginUser, JWT_TTL, TimeUnit.MINUTES);
        // 2. 生成 JWT 字符串
        return JwtUtil.createJWT(userId);
    }

    @Override
    public void renewTokenIfNecessary(LoginUser loginUser) {
        String redisKey = LOGIN_TOKEN_KEY + loginUser.getUser().getId();

        // 获取 Redis 中该 key 的剩余时间（秒）
        Long expireTime = redisCache.getExpireTime(redisKey);

        if (expireTime != null && expireTime < REFRESH_THRESHOLD ) {
            // 如果剩余时间低于 10 分钟，执行续期
            refreshToken(loginUser);
        }
    }

    @Override
    public void delToken(String userId) {
        redisCache.deleteObject(LOGIN_TOKEN_KEY + userId);
    }

    /**
     * 私有辅助方法：执行真正的 Redis 过期时间重置
     */
    private void refreshToken(LoginUser loginUser) {
        String redisKey = LOGIN_TOKEN_KEY + loginUser.getUser().getId();
        // 重置为 30 分钟
        redisCache.setCacheObject(redisKey, loginUser, EXPIRE_TIME, TimeUnit.MINUTES);
    }
}
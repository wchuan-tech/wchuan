package com.wchuan.common.utils;

import com.wchuan.system.domain.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CaptchaUtils {

    private final RedisCache redisCache;
    /**
     * 验证码校验（封装成私有方法）
     */
    public void checkCaptcha(LoginRequest loginRequest) {
        String verifyKey = "captcha_codes:" + loginRequest.getUuid();
        String serverCode = redisCache.getCacheObject(verifyKey);

        if (serverCode == null) {
            throw new RuntimeException("验证码已过期");
        }
        if (!serverCode.equalsIgnoreCase(loginRequest.getCode())) {
            throw new RuntimeException("验证码错误");
        }
        redisCache.deleteObject(verifyKey);
    }
}

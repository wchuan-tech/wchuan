package com.wchuan.system.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.common.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class CaptchaController {

    private final RedisCache redisCache; // 假设你有之前定义的 RedisCache 工具类

    @GetMapping("/captchaImage")
    public ResponseResult<?> getCode() {
        // 1. 生成验证码（此处以线段干扰的图形验证码为例）
        // 参数：宽、高、字符数、干扰线数
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 10);

        // 2. 获取验证码文本内容（如：a1b2）
        String code = lineCaptcha.getCode();

        // 3. 生成唯一标识 UUID
        String uuid = IdUtil.simpleUUID();
        String verifyKey = "captcha_codes:" + uuid;

        // 4. 存入 Redis，设置有效期（比如 2 分钟）
        redisCache.setCacheObject(verifyKey, code, 2, TimeUnit.MINUTES);

        // 5. 返回给前端 Base64 编码的图片和 UUID
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("img", lineCaptcha.getImageBase64Data()); // 会带有 data:image/png;base64, 前缀

        return new ResponseResult<>(200, "操作成功", map);
    }
}
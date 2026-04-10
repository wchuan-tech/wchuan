package com.wchuan.system.controller;


import com.wchuan.common.annotation.Log;
import com.wchuan.system.domain.dto.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HelloController {

    // 建议在配置类中统一注册Bean，这里直接new也可
    private final RestTemplate restTemplate = new RestTemplate();

    @Log(title = "测试接口") // 只需要加这一行！
    @GetMapping("/hello")
    @PreAuthorize("@ss.hasPerm('system:test:list')")
    public ResponseResult<?> hello(){
        try {
            // 国内可访问的英文名言API
            String url = "https://zenquotes.io/api/random";
            List<Map<String, String>> quotes = restTemplate.getForObject(url, List.class);

            if (quotes != null && !quotes.isEmpty()) {
                Map<String, String> quote = quotes.get(0);
                String content = quote.get("q"); // 名言内容
                String author = quote.get("a"); // 作者
                String result = content + " — " + author;
                return new ResponseResult<>(200, "操作成功", result);
            }
            return new ResponseResult<>(500, "获取名言失败", null);
        } catch (Exception e) {
            return new ResponseResult<>(500, "服务异常：" + e.getMessage(), null);
        }
    }

}

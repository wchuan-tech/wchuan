package com.wchuan.system.controller;


import com.wchuan.common.annotation.Log;
import com.wchuan.system.domain.dto.LoginRequest;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @Log(title = "登录") // 只需要加这一行！
    @PostMapping("/user/login")
    public ResponseResult<?> login(@RequestBody LoginRequest loginRequest){
        return loginService.login(loginRequest);
    }

    @Log(title = "登出")
    @RequestMapping("/user/logout")
    public ResponseResult<?> logout(){
        return loginService.logout();
    }
}

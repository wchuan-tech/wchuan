package com.wchuan.system.service.impl;

import com.wchuan.common.utils.CaptchaUtils;
import com.wchuan.system.domain.dto.LoginRequest;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.service.LoginService;
import com.wchuan.system.service.OnlineLogService;
import com.wchuan.system.service.TokenService;
import com.wchuan.common.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.wchuan.common.constants.Constants.SESSION_TTL;
import static com.wchuan.common.enums.RedisKeyEnum.LOGIN_TOKEN_KEY;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final AuthenticationManager authenticationManager;

    private final RedisCache redisCache;

    private final TokenService tokenService;

    private final OnlineLogService onlineLogService; // 注入结算服务

    private final CaptchaUtils captchaUtils;

    @Override
    public ResponseResult<?> login(LoginRequest loginRequest) {
        // 1. 验证码校验（建议封装成工具类）
        captchaUtils.checkCaptcha(loginRequest);

        // 登陆校验
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.getUserName(),loginRequest.getPassword());
        Authentication authentication;

        try{
            authentication = authenticationManager.authenticate(authenticationToken);
        }catch (Exception e){
            throw new RuntimeException("LoginImpl:用户名或密码错误");
        }
        // 认证成功 将用户信息存入 LoginUser 中
        SecurityContextHolder.getContext().setAuthentication(authentication);
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        String userId = loginUser.getUser().getId().toString();

        System.out.println("loginUserAuthorities:" + loginUser.getAuthorities());
        // 设置用户登陆信息时间
        redisCache.setCacheObject(LOGIN_TOKEN_KEY + userId, loginUser,SESSION_TTL, TimeUnit.MINUTES);

        // 创建用户在线时间日志
        onlineLogService.createOnlineLog(loginUser);

        String token = tokenService.createToken(loginUser);
        Map<String,String> map = new HashMap<>();
        map.put("token",token);

        return new ResponseResult<>(200,"登录成功",map);
    }

    @Override
    public ResponseResult<?> logout() {
        System.out.println("------- Service 的 logout 方法 ------*");
        UsernamePasswordAuthenticationToken authentication =  (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String userId = loginUser.getUser().getId().toString();

        onlineLogService.logoutSettle(loginUser);

        tokenService.delToken(userId);
        return new ResponseResult<>(200,"注销成功");
    }


}

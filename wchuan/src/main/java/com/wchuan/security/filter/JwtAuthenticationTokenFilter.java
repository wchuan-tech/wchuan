package com.wchuan.security.filter;

import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.service.OnlineLogService;
import com.wchuan.system.service.TokenService;
import com.wchuan.common.utils.JwtUtil;
import com.wchuan.common.utils.RedisCache;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

import static com.wchuan.common.enums.RedisKeyEnum.LOGIN_TOKEN_KEY;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final RedisCache redisCache;

    private final TokenService tokenService;

    private final OnlineLogService  onlineLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("token");
        if(!StringUtils.hasText(token)){
            filterChain.doFilter(request,response);
            return;
        }
        String subject;
        try{
            Claims claims = JwtUtil.parseJWT(token);
            subject = claims.getSubject();
        }catch (Exception e){
            throw new RuntimeException("token非法");
        }

        String redisKey = LOGIN_TOKEN_KEY + subject;
        LoginUser loginUser = redisCache.getCacheObject(redisKey);
        if(Objects.isNull(loginUser)){
            throw new RuntimeException("用户未登录或登录已过期");
        }

        // 验证 Token 令牌
        tokenService.renewTokenIfNecessary(loginUser);

        // 存入 SecurityContext
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser,null,loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // 刷新在线时间
        onlineLogService.refreshOnlineUserTime(loginUser);

        filterChain.doFilter(request,response);

    }
}

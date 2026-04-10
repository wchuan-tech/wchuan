package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.domain.entity.User;
import com.wchuan.system.mapper.MenuMapper;
import com.wchuan.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    private final MenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;
        List<String> permissions;

        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
        try {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUserName, username);
            user = userMapper.selectOne(queryWrapper);

            if (Objects.isNull(user)) {
                throw new UsernameNotFoundException("UserDetails:用户名或密码错误");
            }
            permissions = menuMapper.selectMenuNameByUserId(user.getId());
            if (user.getId() == 1) {
                // 给管理员加通配符 = 拥有所有权限
                permissions.add("*");
            }
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
        return new LoginUser(user, permissions);
    }
}

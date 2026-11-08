package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wchuan.system.domain.entity.SysRole;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.mapper.SysMenuMapper;
import com.wchuan.system.mapper.SysRoleMapper;
import com.wchuan.system.mapper.SysUserMapper;
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

    private final SysUserMapper userMapper;

    private final SysMenuMapper menuMapper;

    private final SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user;
        List<String> permissions;

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysUser::getUserName, username);

        user = userMapper.selectOne(queryWrapper);

        if (Objects.isNull(user)) {
            throw new UsernameNotFoundException("UserDetails:用户名或密码错误");
        }

        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getId());
        user.setRoles(roles);

        permissions = menuMapper.selectMenuNameByUserId(user.getId());
        if (user.getId() == 1) {
            // 给管理员加通配符 = 拥有所有权限
            permissions.add("*");
        }
        return new LoginUser(user, permissions);
    }

}

package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wchuan.common.exception.BusinessException;
import com.wchuan.system.domain.dto.UserCreateDTO;
import com.wchuan.system.domain.entity.SysUserRole;
import com.wchuan.system.domain.entity.User;
import com.wchuan.system.mapper.UserMapper;
import com.wchuan.system.mapper.UserRoleMapper;
import com.wchuan.system.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private PasswordEncoder passwordEncoder; // Spring Security 的密码加密器

    @Resource
    private UserRoleMapper userRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateDTO dto) {
        // 1. 唯一性校验（防止用户名重复）
        if (checkUserNameExists(dto.getUserName())) {
            throw new BusinessException("用户名已存在！");
        }

        // 2. 将 DTO 转为 Entity 属性（推荐使用 Spring 的 BeanUtils 或 MapStruct）
        User user = new User();
        BeanUtils.copyProperties(dto, user);

        // 3. 密码必须通过 BCrypt 加密（切记不能存明文！）
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 4. 设置一些初始化的默认状态（如果数据库没有设默认值的话）
        user.setStatus("0"); // 0-正常
        user.setDelFlag(0);  // 0-未删除

        // 5. 保存用户到 sys_user 表
        // 此时 MyMetaObjectHandler 拦截器会自动把 tenantId, createBy, createTime 填充进去！
        boolean success = this.save(user);

        // 6. 绑定用户与角色的关系（写入 sys_user_role 表）
        if (success && dto.getRoleId() != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId()); // 上一步保存完后，user.getId() 已经被自动赋值主键了
            userRole.setRoleId(dto.getRoleId());
            userRoleMapper.insert(userRole);
        }

    }

    /**
     * 检查用户名是否已存在
     * @param userName 用户名
     * @return true-已存在，false-不存在
     */
    private boolean checkUserNameExists(String userName) {
        // 利用 MyBatis-Plus 的 lambdaQuery 快速查询 count
        Long count = this.lambdaQuery()
                .eq(User::getUserName, userName)
                .count();
        return count != null && count > 0;
    }
}

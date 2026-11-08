package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wchuan.common.annotation.DataScope;
import com.wchuan.common.exception.BusinessException;
import com.wchuan.common.utils.SecurityUtils;
import com.wchuan.system.domain.dto.UserCreateDTO;
import com.wchuan.system.domain.entity.SysDept;
import com.wchuan.system.domain.entity.SysRole;
import com.wchuan.system.domain.entity.SysUserRole;
import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.mapper.SysDeptMapper;
import com.wchuan.system.mapper.SysUserMapper;
import com.wchuan.system.mapper.SysUserRoleMapper;
import com.wchuan.system.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements IUserService {

    @Resource
    private PasswordEncoder passwordEncoder; // Spring Security 的密码加密器

    @Resource
    private SysUserRoleMapper userRoleMapper;

    @Resource
    private SysUserMapper userMapper;

    // 🔑 注入部门 Mapper
    @Resource
    private SysDeptMapper sysDeptMapper;
    /**
     * 1. 分页查询用户列表（带 @DataScope 数据权限过滤）
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUserList(SysUser user) {
        return userMapper.selectUserList(user);
    }

    /**
     * 2. 根据用户 ID 查询详细信息（带数据权限校验）
     */
    @Override
    public SysUser selectUserById(Long userId) {
        // 校验越权访问
        checkUserDataScope(userId);
        return userMapper.selectUserById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserRole(UserCreateDTO dto) {
        // 1. 唯一性校验（防止用户名重复）
        if (checkUserNameExists(dto.getUserName())) {
            throw new BusinessException("用户名已存在！");
        }

        // 2. 将 DTO 转为 Entity 属性（推荐使用 Spring 的 BeanUtils 或 MapStruct）
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);

        // 3. 密码必须通过 BCrypt 加密（切记不能存明文！）
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 4. 设置一些初始化的默认状态（如果数据库没有设默认值的话）
        user.setStatus("0"); // 0-正常
        user.setDelFlag(0);  // 0-未删除

        // 5. 保存用户到 sys_user 表
        // MyMetaObjectHandler 会自动填充 createBy、createTime 等审计字段。
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
     * 4. 修改保存用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user) {
        // 1. 越权修改校验
        checkUserDataScope(user.getId());

        // 2. 更新用户信息
        boolean success = this.updateById(user);

        // 3. 重新绑定角色关系（先清空旧角色，再插入新角色）
        if (success && user.getRoles() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
            insertUserRoles(user.getId(), user.getRoles());
        }

        return success;
    }

    /**
     * 5. 单条删除用户（带安全防护）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserById(Long userId) {
        // 安全防护一：不能删除超级管理员账号 (ID = 1)
        if (SecurityUtils.isAdmin(userId)) {
            throw new BusinessException("不允许删除超级管理员账号！");
        }
        // 安全防护二：不能删除当前正在登录的自己
        if (userId.equals(SecurityUtils.getUserId())) {
            throw new BusinessException("不能删除当前正在登录的账号！");
        }
        // 安全防护三：越权删除校验
        checkUserDataScope(userId);

        // 逻辑删除（设 del_flag = 2）
        SysUser user = new SysUser();
        user.setId(userId);
        user.setDelFlag(2);
        boolean success = this.updateById(user);

        // 清理绑定的角色关联
        if (success) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        }
        return success;
    }

    /**
     * 6. 重置用户密码
     */
    @Override
    public boolean resetUserPwd(Long userId, String newPassword) {
        checkUserDataScope(userId);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        return this.updateById(user);
    }

    /**
     * 7. 修改用户状态（开启/停用账号）
     */
    @Override
    public boolean updateUserStatus(SysUser user) {
        checkUserDataScope(user.getId());
        return this.updateById(user);
    }

    /**
     * 🛡️ 8. 数据权限防越权校验（核心安全拦截！）
     * 如果当前登录人没有目标用户的查看/修改权限，直接抛出异常拦截！
     */
    /**
     * 🛡️ 数据权限防越权校验（重构版，彻底避免 AOP 嵌套调用失效问题）
     */
    @Override
    public void checkUserDataScope(Long targetUserId) {
        if (targetUserId == null || SecurityUtils.isAdmin()) {
            return; // 超级管理员直接放行
        }

        // 1. 查出目标用户的数据
        SysUser targetUser = this.getById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在！");
        }

        // 2. 获取当前登录用户及其角色列表
        SysUser currentUser = SecurityUtils.getLoginUser();
        if (currentUser == null || CollectionUtils.isEmpty(currentUser.getRoles())) {
            throw new BusinessException("没有权限访问或修改该用户数据！");
        }

        // 3. 逐个角色校验目标用户是否在当前登录人的数据权限范围内
        boolean hasPermission = false;
        Long currentDeptId = currentUser.getDeptId();
        Long targetDeptId = targetUser.getDeptId();

        for (SysRole role : currentUser.getRoles()) {
            String dataScope = role.getDataScope();

            // 规则 1：全部数据权限
            if ("1".equals(dataScope)) {
                hasPermission = true;
                break;
            }
            // 规则 3：本部门数据权限（部门ID必须相同）
            else if ("3".equals(dataScope)) {
                if (currentDeptId != null && currentDeptId.equals(targetDeptId)) {
                    hasPermission = true;
                    break;
                }
            }
            // 规则 4：本部门及以下数据权限
            else if ("4".equals(dataScope)) {
                if (currentDeptId != null && (currentDeptId.equals(targetDeptId) || checkDeptIsChild(currentDeptId, targetDeptId))) {
                    hasPermission = true;
                    break;
                }
            }
            // 规则 5：仅本人数据权限（目标用户必须就是我自己）
            else if ("5".equals(dataScope)) {
                if (currentUser.getId().equals(targetUserId)) {
                    hasPermission = true;
                    break;
                }
            }
        }

        // 4. 如果所有角色都不匹配，抛出越权拦截异常！
        if (!hasPermission) {
            throw new BusinessException("没有权限访问或修改该用户数据！");
        }

    }

    /**
     * 辅助方法：检查 targetDeptId 是否属于 currentDeptId 的下属部门
     */
    private boolean checkDeptIsChild(Long currentDeptId, Long targetDeptId) {
        if (targetDeptId == null) {
            return false;
        }
        // 去 sys_dept 表查 targetDeptId 的 ancestors 字段是否包含 currentDeptId
        SysDept targetDept = sysDeptMapper.selectById(targetDeptId);
        if (targetDept != null && StringUtils.isNotBlank(targetDept.getAncestors())) {
            // ancestors 格式为 "0,100,101"，判断是否包含当前部门 ID
            String[] parentIds = targetDept.getAncestors().split(",");
            for (String pid : parentIds) {
                if (String.valueOf(currentDeptId).equals(pid.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查用户名是否已存在
     * @param userName 用户名
     * @return true-已存在，false-不存在
     */
    private boolean checkUserNameExists(String userName) {
        // 利用 MyBatis-Plus 的 lambdaQuery 快速查询 count
        Long count = this.lambdaQuery()
                .eq(SysUser::getUserName, userName)
                .count();
        return count != null && count > 0;
    }


    /**
     * 批量插入用户角色关联关系
     */
    private void insertUserRoles(Long userId, List<com.wchuan.system.domain.entity.SysRole> roles) {
        if (!CollectionUtils.isEmpty(roles)) {
            for (com.wchuan.system.domain.entity.SysRole role : roles) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(role.getId());
                userRoleMapper.insert(ur);
            }
        }
    }

}

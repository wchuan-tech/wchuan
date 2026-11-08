package com.wchuan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wchuan.system.domain.dto.UserCreateDTO;
import com.wchuan.system.domain.entity.SysUser;

import java.util.List;

public interface IUserService extends IService<SysUser> {

    /** 查询用户列表（带数据权限） */
    List<SysUser> selectUserList(SysUser user);

    /** 根据 ID 查询用户详情 */
    SysUser selectUserById(Long userId);

    /** 创建用户 */
    void insertUserRole(UserCreateDTO dto);

    /** 修改用户 */
    boolean updateUser(SysUser user);

    /** 单条删除用户 */
    boolean deleteUserById(Long userId);

    /** 重置密码 */
    boolean resetUserPwd(Long userId, String newPassword);

    /** 修改用户状态 */
    boolean updateUserStatus(SysUser user);

    /** 校验数据权限越权 */
    void checkUserDataScope(Long targetUserId);
}
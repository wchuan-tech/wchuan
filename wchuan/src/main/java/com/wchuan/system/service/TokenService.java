package com.wchuan.system.service;

import com.wchuan.system.domain.vo.LoginUser;

/**
 * 令牌处理中心接口
 */
public interface TokenService {

    /**
     * 创建令牌并存入 Redis
     */
    String createToken(LoginUser loginUser);

    /**
     * 验证并刷新令牌有效期（滑动窗口续期）
     */
    void renewTokenIfNecessary(LoginUser loginUser);

    /**
     * 删除令牌信息（用于注销）
     */
    void delToken(String userId);
}
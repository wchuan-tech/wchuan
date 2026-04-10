package com.wchuan.system.service;

import com.wchuan.system.domain.dto.LoginRequest;
import com.wchuan.system.domain.dto.ResponseResult;

public interface LoginService {
    ResponseResult<?> login(LoginRequest loginRequest);

    ResponseResult<?> logout();
}

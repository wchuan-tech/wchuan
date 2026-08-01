package com.wchuan.system.controller;

import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.dto.UserCreateDTO;
import com.wchuan.system.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/user")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 新增用户接口
     */
    @PostMapping
    public ResponseResult<String> add(@Validated @RequestBody UserCreateDTO dto) {
        // 执行创建逻辑
        userService.createUser(dto);

        // 返回成功提示（此时 ResponseResult<Void> 就完美匹配了）
        return ResponseResult.success("新增用户成功");
    }
}
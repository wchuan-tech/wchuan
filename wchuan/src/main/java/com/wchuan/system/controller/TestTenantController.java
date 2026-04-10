package com.wchuan.system.controller;


import com.wchuan.common.annotation.Log;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.User;
import com.wchuan.system.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class TestTenantController {

    private final UserMapper userMapper;

    public TestTenantController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Log(title = "查询相同租户的用户")
    @GetMapping("/list")
    public ResponseResult<?> list() {
        List<User> list = userMapper.selectList(null);
        System.out.println(list.toString());
        return new ResponseResult<>(200,"success",list);
    }
}

package com.wchuan.system.controller;


import com.wchuan.common.annotation.Log;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class TestTenantController {

    private final SysUserMapper userMapper;

    public TestTenantController(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Log(title = "查询相同租户的用户")
    @GetMapping("/list")
    public ResponseResult<?> list() {
        List<SysUser> list = userMapper.selectList(null);
        System.out.println(list.toString());
        return new ResponseResult<>(200,"success",list);
    }
}

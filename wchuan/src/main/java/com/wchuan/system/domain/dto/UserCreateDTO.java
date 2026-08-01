package com.wchuan.system.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateDTO {

    @NotNull(message = "所属部门不能为空")
    private Long deptId;

    @NotBlank(message = "用户账号不能为空")
    private String userName;

    @NotBlank(message = "用户昵称不能为空")
    private String nickName;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phoneNumber;

    private String sex; // '0' 男, '1' 女

    @NotNull(message = "关联的角色ID不能为空")
    private Long roleId; // 顺便接收分配的角色ID
}
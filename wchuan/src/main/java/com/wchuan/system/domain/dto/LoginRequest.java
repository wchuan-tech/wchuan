package com.wchuan.system.domain.dto;


import lombok.Data;

@Data
public class LoginRequest {
    String userName;
    String password;
    String uuid;
    String code;
}

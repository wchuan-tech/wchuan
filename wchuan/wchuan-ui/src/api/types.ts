// 与后端的 Result<T> 类对应
export interface Result<T = any> {
    code: number;
    msg: string;
    data: T;
}

// 登录成功后的 Data 结构
export interface LoginResponse {
    token: string;
    // 如果后端还返回了用户信息，也可以写在这里
}

export interface CaptchaResponse {
    uuid: string;
    img: string;
}

export interface UserInfo {
    id: number;
    userName: string;
    nickName: string;
    roles: string[];      // 角色列表
    permissions: string[]; // 权限标识列表
    tenantId: number;     // 新增
    tenantName?: string;  // 新增（可选）
}

// src/api/types.ts

export interface SysNotice {
    id: number;
    title: string;
    content: string;
    type: string;        // '0' 为租户公告，'1' 为系统公告
    createTime: string;
    createBy?: string;
}
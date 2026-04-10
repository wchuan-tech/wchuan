package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_user_online_log")
public class SysUserOnlineLog extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 用户 id
    private String userName; // 用户名
    private String ipAddr; // ip地址
    private Date loginTime; // 登录时间
    private Date lastActivityTime; // 上次活跃时间
    private Long duration;   // 时长（秒）
    private Integer exitType; // 0-正常注销, 1-超时结算
}
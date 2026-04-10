package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;          // 模块标题
    private Integer businessType;  // 业务类型
    private String method;         // 方法名称
    private String requestMethod;  // 请求方法
    private String operName;       // 操作人员
    private String operUrl;        // 请求URL
    private String operIp;         // 主机地址
    private Integer status;        // 操作状态 (0正常 1异常)
    private String errorMsg;       // 错误消息
    private Date operTime;         // 操作时间
    private Long costTime;         // 消耗时间
}

package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_tenant")
public class Tenant {

    @TableId(type =  IdType.AUTO)
    private Long id;

    private String tenantName;   // 租户名称

    private String contactUser;  // 联系人

    private String contactPhone; // 联系电话

    private String status;       // 状态 (0正常 1停用)

    private Date expireTime;

    private Date createTime;

    private Date updateTime;

}

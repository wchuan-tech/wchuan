package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_notice")
public class SysNotice extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String type;
    private Long tenantId;
    private String tenantName;
    private Date createTime;
    private String createBy;
}

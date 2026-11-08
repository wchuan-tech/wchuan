package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleName;

    private String roleKey;

    /** 🔑 数据范围（1全部 2自定义 3本部门 4本部门及以下 5仅本人） */
    private String dataScope;

    private String status;

    private String delFlag;

    private String remark;

    private Integer roleSort;

    @TableField(exist = false)
    private Long[] menuIds;

    @TableField(exist = false)
    private Long[] deptIds;

}

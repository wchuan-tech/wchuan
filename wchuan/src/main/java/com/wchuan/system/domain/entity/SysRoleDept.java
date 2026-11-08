package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_dept")
public class SysRoleDept {

    private Long roleId;

    private Long deptId;
}
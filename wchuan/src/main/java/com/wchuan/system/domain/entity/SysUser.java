package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user")
public class SysUser extends BaseEntity {
    @Serial
    private static final long serialVersionUID = -40356785423868312L;

    @TableId(type = IdType.AUTO) // 明确指定主键策略与数据库一致
    private Long id;

    /** 部门ID（🔑 关联 sys_dept 的关键字段） */
    private Long deptId;

    private String userName;

    private String nickName;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String status;

    private String email;

    private String phoneNumber;

    private String sex;

    private String avatar;

    private String userType;

    private Integer delFlag;

    /**
     * 🔑 关联的部门对象（非数据库列，用于接收 XML 联查出来的部门信息）
     * 必须加上 @TableField(exist = false) ！
     */
    @TableField(exist = false)
    private SysDept dept;
    /**
     * 🔑 角色对象列表（数据权限切面 DataScopeAspect 核心依靠属性！）
     * 注意：必须加上 @TableField(exist = false)，告诉 MyBatis-Plus 这个不是 sys_user 表中的真实列！
     */
    @TableField(exist = false)
    private List<SysRole> roles;

    /**
     * 判断是否是超级管理员（主键 ID 为 1 的默认为超管）
     */
    public boolean isAdmin() {
        return id != null && 1L == id;
    }

}
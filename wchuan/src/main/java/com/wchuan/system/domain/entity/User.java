package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user")
public class User extends BaseEntity {
    @Serial
    private static final long serialVersionUID = -40356785423868312L;

    @TableId(type = IdType.AUTO) // 明确指定主键策略与数据库一致
    private Long id;

    /** 部门ID（🔑 关联 sys_dept 的关键字段） */
    private Long deptId;

    private String userName;

    private String nickName;

    private String password;

    private String status;

    private String email;

    private String phoneNumber;

    private String sex;

    private String avatar;

    private String userType;

    private Integer delFlag;
}
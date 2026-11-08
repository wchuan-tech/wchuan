package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long deptId;

    /** 前端树组件统一使用 id 作为节点键。 */
    @JsonProperty("id")
    public Long getId() {
        return deptId;
    }

    private Long parentId;

    /** 祖级列表（格式: 0,100,101，切面本部门及以下的关键字段） */
    private String ancestors;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    @NotNull(message = "显示排序不能为空")
    private Integer orderNum;

    private String leader;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$|^[0-9-]{6,20}$", message = "联系电话格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "0|1", message = "部门状态只能是0或1")
    private String status;

    private String delFlag;

    /**
     * 🔑 核心补全：子部门列表集合（非数据库列，用于树形结构组装）
     * 注意：必须加 @TableField(exist = false)，避免 MyBatis-Plus 去数据库查 children 列！
     */
    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}

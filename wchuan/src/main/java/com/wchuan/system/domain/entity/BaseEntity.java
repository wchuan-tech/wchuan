package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT) // 插入时自动填充
    private Long tenantId;
}
package com.wchuan.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新者 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 👈 确保这里也是 LocalDateTime

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 👈 确保这里也是 LocalDateTime

    /**
     * 🔑 核心扩展参数：存放数据权限切面生成的 SQL 片段 params.get("dataScope")
     * 注意：必须加 exist = false，告诉 MyBatis-Plus 这个不是数据库字段！
     */
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }
}
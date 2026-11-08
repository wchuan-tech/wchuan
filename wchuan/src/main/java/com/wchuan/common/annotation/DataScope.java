package com.wchuan.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门表的别名
     */
    String deptAlias() default "";

    /** 部门主键列名，sys_dept 使用 id。 */
    String deptIdColumn() default "id";

    /**
     * 用户表的别名
     */
    String userAlias() default "";

    /**
     * 权限字符（可选）
     */
    String permission() default "";
}

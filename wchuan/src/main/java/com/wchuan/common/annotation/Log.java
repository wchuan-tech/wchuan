package com.wchuan.common.annotation;

import com.wchuan.common.enums.BusinessTypeEnum;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({PARAMETER,METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    String title() default "";

    int businessType() default BusinessTypeEnum.OTHER;
}

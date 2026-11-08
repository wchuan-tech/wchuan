package com.wchuan.crm.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerPageQuery {

    @Min(value = 1, message = "页码必须大于0")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private long pageSize = 10;

    @Size(max = 100, message = "客户名称长度不能超过100个字符")
    private String customerName;

    @Size(max = 20, message = "手机号码长度不能超过20个字符")
    private String phone;

    @Size(max = 16, message = "客户阶段长度不能超过16个字符")
    private String customerStage;
}

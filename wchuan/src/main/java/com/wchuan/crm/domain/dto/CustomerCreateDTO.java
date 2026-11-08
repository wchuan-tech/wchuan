package com.wchuan.crm.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerCreateDTO {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称长度不能超过100个字符")
    private String customerName;

    @Size(max = 20, message = "手机号码长度不能超过20个字符")
    private String phone;

    @Size(max = 64, message = "微信号长度不能超过64个字符")
    private String wechat;

    @Size(max = 200, message = "公司名称长度不能超过200个字符")
    private String companyName;

    @Pattern(regexp = "线索|初访|意向|报价|签约", message = "客户阶段不合法")
    private String customerStage = "线索";
}

package com.wchuan.crm.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CustomerAssignDTO {

    @NotNull(message = "客户ID不能为空")
    @Positive(message = "客户ID必须大于0")
    private Long customerId;

    @NotNull(message = "目标销售ID不能为空")
    @Positive(message = "目标销售ID必须大于0")
    private Long targetUserId;
}

package com.wchuan.crm.domain.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowupCreateDTO {

    @NotNull(message = "客户ID不能为空")
    @Positive(message = "客户ID必须大于0")
    private Long customerId;

    @NotBlank(message = "跟进方式不能为空")
    @Size(max = 16, message = "跟进方式长度不能超过16个字符")
    private String followupType;

    @NotBlank(message = "跟进内容不能为空")
    private String content;

    @FutureOrPresent(message = "下次跟进时间不能早于当前时间")
    private LocalDateTime nextFollowupTime;

    private String customerStage;
}

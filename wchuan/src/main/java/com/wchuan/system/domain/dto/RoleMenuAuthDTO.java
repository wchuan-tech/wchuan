package com.wchuan.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleMenuAuthDTO {

    @NotNull(message = "角色ID不能为空")
    @Positive(message = "角色ID必须大于0")
    private Long roleId;

    private List<Long> checkedKeys = new ArrayList<>();

    private List<Long> halfCheckedKeys = new ArrayList<>();
}

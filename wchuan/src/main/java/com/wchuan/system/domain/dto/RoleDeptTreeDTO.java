package com.wchuan.system.domain.dto;

import com.wchuan.system.domain.entity.SysDept;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoleDeptTreeDTO {

    private List<Long> checkedKeys;
    private List<SysDept> depts;
}

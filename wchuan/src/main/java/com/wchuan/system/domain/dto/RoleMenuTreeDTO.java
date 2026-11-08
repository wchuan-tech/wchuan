package com.wchuan.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoleMenuTreeDTO {

    private List<Long> checkedKeys;
    private List<Long> halfCheckedKeys;
    private List<MenuTreeNodeDTO> menus;
}

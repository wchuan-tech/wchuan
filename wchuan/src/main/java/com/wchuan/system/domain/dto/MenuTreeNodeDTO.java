package com.wchuan.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MenuTreeNodeDTO {

    private Long id;
    private String label;
    private String menuName;
    private List<MenuTreeNodeDTO> children;
}

package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.common.annotation.DataScope;
import com.wchuan.system.domain.entity.SysDept;

import java.util.List;

/**
 * @author WChuan
 */
public interface SysDeptMapper extends BaseMapper<SysDept> {
    @DataScope(deptAlias = "d")
    List<SysDept> selectDeptList(SysDept dept);

    List<Long> selectDeptListByRoleId(Long roleId);
}


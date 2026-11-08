package com.wchuan.system.controller;

import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysDept;
import com.wchuan.system.service.ISysDeptService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** 部门管理 Controller。 */
@Validated
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    @Resource
    private ISysDeptService deptService;

    /** 查询当前用户数据范围内的部门树。 */
    @PreAuthorize("@ss.hasPerm('system:dept:list')")
    @GetMapping("/list")
    public ResponseResult<List<SysDept>> list(SysDept dept) {
        return ResponseResult.success(deptService.selectDeptList(dept));
    }

    /** 排除当前部门及其子孙，用于选择新的上级部门。 */
    @PreAuthorize("@ss.hasPerm('system:dept:list')")
    @GetMapping("/list/exclude/{deptId:\\d+}")
    public ResponseResult<List<SysDept>> excludeChild(
            @PathVariable @Positive(message = "部门ID必须大于0") Long deptId) {
        return ResponseResult.success(deptService.selectDeptListExcludeChild(deptId));
    }

    /** 根据 ID 查询部门详情。 */
    @PreAuthorize("@ss.hasPerm('system:dept:query')")
    @GetMapping("/{deptId:\\d+}")
    public ResponseResult<SysDept> getInfo(
            @PathVariable @Positive(message = "部门ID必须大于0") Long deptId) {
        return ResponseResult.success(deptService.selectDeptById(deptId));
    }

    /** 查询完整部门树及角色已授权的自定义数据范围。 */
    @PreAuthorize("@ss.hasPerm('system:role:query')")
    @GetMapping("/treeselect")
    public ResponseResult<List<SysDept>> treeSelect() {
        return ResponseResult.success(deptService.buildDeptTreeSelect(
                deptService.selectDeptList(new SysDept())));
    }

    /** 查询完整部门树及角色已授权的自定义数据范围。 */
    @PreAuthorize("@ss.hasPerm('system:role:query')")
    @GetMapping("/roleDeptTreeselect/{roleId:\\d+}")
    public ResponseResult<Map<String, Object>> roleDeptTreeselect(
            @PathVariable("roleId") @Positive(message = "角色ID必须大于0") Long roleId) {
        List<SysDept> depts = deptService.selectDeptList(new SysDept());
        Map<String, Object> result = new HashMap<>();
        result.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
        result.put("depts", deptService.buildDeptTreeSelect(depts));
        return ResponseResult.success(result);
    }

    /** 新增部门。 */
    @PreAuthorize("@ss.hasPerm('system:dept:add')")
    @PostMapping
    public ResponseResult<Void> add(@Validated @RequestBody SysDept dept) {
        deptService.insertDept(dept);
        return ResponseResult.success("新增部门成功", null);
    }

    /** 修改部门及其组织层级。 */
    @PreAuthorize("@ss.hasPerm('system:dept:edit')")
    @PutMapping
    public ResponseResult<Void> edit(@Validated @RequestBody SysDept dept) {
        deptService.updateDept(dept);
        return ResponseResult.success("修改部门成功", null);
    }

    /** 删除无子部门、无在职员工的部门。 */
    @PreAuthorize("@ss.hasPerm('system:dept:remove')")
    @DeleteMapping("/{deptId:\\d+}")
    public ResponseResult<Void> remove(
            @PathVariable @Positive(message = "部门ID必须大于0") Long deptId) {
        deptService.deleteDeptById(deptId);
        return ResponseResult.success("删除部门成功", null);
    }
}

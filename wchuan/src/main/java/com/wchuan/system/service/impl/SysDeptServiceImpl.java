package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wchuan.common.exception.BusinessException;
import com.wchuan.system.domain.entity.SysDept;
import com.wchuan.system.domain.dto.RoleDeptTreeDTO;
import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.mapper.SysDeptMapper;
import com.wchuan.system.mapper.SysUserMapper;
import com.wchuan.system.service.ISysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

    private static final String NORMAL = "0";
    private static final String DISABLED = "1";
    private static final String NOT_DELETED = "0";
    private static final String DELETED = "2";

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        SysDept criteria = dept == null ? new SysDept() : dept;
        return buildDeptTree(deptMapper.selectDeptList(criteria));
    }

    @Override
    public List<SysDept> selectDeptListExcludeChild(Long deptId) {
        List<SysDept> departments = deptMapper.selectDeptList(new SysDept());
        if (deptId == null) {
            return buildDeptTree(departments);
        }
        String targetId = String.valueOf(deptId);
        List<SysDept> filtered = departments.stream()
                .filter(dept -> !Objects.equals(dept.getDeptId(), deptId))
                .filter(dept -> !containsAncestor(dept.getAncestors(), targetId))
                .toList();
        return buildDeptTree(new ArrayList<>(filtered));
    }

    @Override
    public RoleDeptTreeDTO selectRoleDeptTree(Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw new BusinessException(400, "角色ID不合法");
        }
        List<SysDept> departments = deptMapper.selectDeptList(new SysDept());
        return new RoleDeptTreeDTO(selectDeptListByRoleId(roleId), buildDeptTreeSelect(departments));
    }

    @Override
    public List<Long> selectDeptListByRoleId(Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw new BusinessException(400, "角色ID不合法");
        }
        return deptMapper.selectDeptListByRoleId(roleId);
    }

    @Override
    public List<SysDept> buildDeptTreeSelect(List<SysDept> depts) {
        if (depts == null || depts.isEmpty()) {
            return new ArrayList<>();
        }
        // selectDeptList 当前返回树结构，先展平后重建，避免重复构建时丢失子节点。
        List<SysDept> flattened = new ArrayList<>();
        flattenDeptTree(depts, flattened);
        return buildDeptTree(flattened);
    }

    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        if (depts == null || depts.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<SysDept>> grouped = depts.stream()
                .peek(dept -> dept.setChildren(new ArrayList<>()))
                .collect(Collectors.groupingBy(dept -> dept.getParentId() == null ? 0L : dept.getParentId()));
        Set<Long> ids = depts.stream().map(SysDept::getDeptId).collect(Collectors.toSet());
        List<SysDept> roots = depts.stream()
                .filter(dept -> dept.getParentId() == null
                        || dept.getParentId() == 0L
                        || !ids.contains(dept.getParentId()))
                .sorted(deptComparator())
                .toList();
        roots.forEach(root -> attachChildren(root, grouped));
        return new ArrayList<>(roots);
    }

    @Override
    public SysDept selectDeptById(Long deptId) {
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null || DELETED.equals(dept.getDelFlag())) {
            throw new BusinessException(404, "部门不存在");
        }
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertDept(SysDept dept) {
        // 新增接口不接受客户端主键，避免编辑表单残留 ID 被误作当前部门。
        dept.setDeptId(null);
        normalizeDept(dept);
        if (!checkDeptNameUnique(dept)) {
            throw new BusinessException(409, "新增部门'" + dept.getDeptName() + "'失败，同级部门名称已存在");
        }

        SysDept parent = getParent(dept.getParentId());
        if (parent != null && DISABLED.equals(parent.getStatus())) {
            throw new BusinessException(409, "上级部门已停用，不能新增下级部门");
        }
        dept.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getDeptId());
        dept.setDelFlag(NOT_DELETED);
        return deptMapper.insert(dept) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDept(SysDept dept) {
        if (dept.getDeptId() == null) {
            throw new BusinessException(400, "部门ID不能为空");
        }
        normalizeDept(dept);
        SysDept oldDept = selectDeptById(dept.getDeptId());
        assertParentIsValid(dept);
        if (!checkDeptNameUnique(dept)) {
            throw new BusinessException(409, "修改部门'" + dept.getDeptName() + "'失败，同级部门名称已存在");
        }

        SysDept newParent = getParent(dept.getParentId());
        if (NORMAL.equals(dept.getStatus()) && newParent != null && DISABLED.equals(newParent.getStatus())) {
            throw new BusinessException(409, "上级部门已停用，当前部门不能启用");
        }

        String newAncestors = newParent == null ? "0" : newParent.getAncestors() + "," + newParent.getDeptId();
        dept.setAncestors(newAncestors);
        if (!Objects.equals(oldDept.getAncestors(), newAncestors)) {
            updateDescendantAncestors(oldDept, newAncestors);
        }
        if (DISABLED.equals(dept.getStatus())) {
            disableDescendants(dept.getDeptId());
        }
        return deptMapper.updateById(dept) == 1;
    }

    @Override
    public boolean checkDeptNameUnique(SysDept dept) {
        Long currentId = dept.getDeptId() == null ? -1L : dept.getDeptId();
        SysDept existing = deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, dept.getParentId())
                .eq(SysDept::getDeptName, dept.getDeptName())
                .eq(SysDept::getDelFlag, NOT_DELETED)
                .last("LIMIT 1"));
        return existing == null || Objects.equals(existing.getDeptId(), currentId);
    }

    @Override
    public boolean checkDeptExistUser(Long deptId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getDelFlag, 0));
        return count != null && count > 0;
    }

    @Override
    public boolean hasChildByDeptId(Long deptId) {
        Long count = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, deptId)
                .eq(SysDept::getDelFlag, NOT_DELETED));
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDeptById(Long deptId) {
        selectDeptById(deptId);
        if (hasChildByDeptId(deptId)) {
            throw new BusinessException(409, "该部门下存在未删除的子部门，不允许删除");
        }
        if (checkDeptExistUser(deptId)) {
            throw new BusinessException(409, "该部门下存在在职员工，不允许删除");
        }
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setDelFlag(DELETED);
        return deptMapper.updateById(dept) == 1;
    }

    private void assertParentIsValid(SysDept dept) {
        Long deptId = dept.getDeptId();
        Long parentId = dept.getParentId();
        if (Objects.equals(deptId, parentId)) {
            throw new BusinessException(400, "上级部门不能选择当前部门自身");
        }
        if (parentId == null || parentId == 0L) {
            return;
        }
        SysDept parent = selectDeptById(parentId);
        if (containsAncestor(parent.getAncestors(), String.valueOf(deptId))) {
            throw new BusinessException(400, "上级部门不能选择当前部门的子孙部门");
        }
    }

    private void updateDescendantAncestors(SysDept oldDept, String newAncestors) {
        String oldPrefix = oldDept.getAncestors() + "," + oldDept.getDeptId();
        String newPrefix = newAncestors + "," + oldDept.getDeptId();
        List<SysDept> descendants = selectDescendants(oldDept.getDeptId());
        for (SysDept child : descendants) {
            String ancestors = child.getAncestors();
            if (ancestors.equals(oldPrefix) || ancestors.startsWith(oldPrefix + ",")) {
                child.setAncestors(newPrefix + ancestors.substring(oldPrefix.length()));
            }
        }
        if (!descendants.isEmpty()) {
            updateBatchById(descendants);
        }
    }

    private void disableDescendants(Long deptId) {
        List<SysDept> descendants = selectDescendants(deptId);
        descendants.forEach(child -> child.setStatus(DISABLED));
        if (!descendants.isEmpty()) {
            updateBatchById(descendants);
        }
    }

    private List<SysDept> selectDescendants(Long deptId) {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDelFlag, NOT_DELETED)
                .apply("FIND_IN_SET({0}, ancestors)", deptId)
                .orderByAsc(SysDept::getAncestors, SysDept::getOrderNum));
    }

    private SysDept getParent(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return null;
        }
        return selectDeptById(parentId);
    }

    private void normalizeDept(SysDept dept) {
        dept.setParentId(dept.getParentId() == null ? 0L : dept.getParentId());
        if (!StringUtils.hasText(dept.getDeptName())) {
            throw new BusinessException(400, "部门名称不能为空");
        }
        dept.setDeptName(dept.getDeptName().trim());
        dept.setOrderNum(dept.getOrderNum() == null ? 0 : dept.getOrderNum());
        dept.setStatus(StringUtils.hasText(dept.getStatus()) ? dept.getStatus() : NORMAL);
    }

    private void attachChildren(SysDept parent, Map<Long, List<SysDept>> grouped) {
        List<SysDept> children = grouped.getOrDefault(parent.getDeptId(), List.of()).stream()
                .sorted(deptComparator())
                .toList();
        parent.setChildren(new ArrayList<>(children));
        children.forEach(child -> attachChildren(child, grouped));
    }

    private void flattenDeptTree(List<SysDept> source, List<SysDept> target) {
        for (SysDept dept : source) {
            List<SysDept> children = dept.getChildren() == null
                    ? List.of() : new ArrayList<>(dept.getChildren());
            dept.setChildren(new ArrayList<>());
            target.add(dept);
            flattenDeptTree(children, target);
        }
    }

    private Comparator<SysDept> deptComparator() {
        return Comparator.comparing(
                        (SysDept dept) -> dept.getOrderNum() == null ? 0 : dept.getOrderNum())
                .thenComparing(
                        dept -> dept.getId() == null ? 0L : dept.getId());
    }

    private boolean containsAncestor(String ancestors, String deptId) {
        if (!StringUtils.hasText(ancestors)) {
            return false;
        }
        for (String ancestor : ancestors.split(",")) {
            if (deptId.equals(ancestor.trim())) {
                return true;
            }
        }
        return false;
    }
}

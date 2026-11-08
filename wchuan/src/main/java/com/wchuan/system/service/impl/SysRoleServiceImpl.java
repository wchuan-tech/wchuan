package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wchuan.common.annotation.DataScope;
import com.wchuan.common.exception.BusinessException;
import com.wchuan.common.utils.RedisCache;
import com.wchuan.system.domain.dto.RoleMenuAuthDTO;
import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.domain.entity.SysDept;
import com.wchuan.system.domain.entity.SysRole;
import com.wchuan.system.domain.entity.SysRoleDept;
import com.wchuan.system.domain.entity.SysRoleMenu;
import com.wchuan.system.domain.entity.SysUserRole;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.mapper.SysRoleDeptMapper;
import com.wchuan.system.mapper.SysRoleMapper;
import com.wchuan.system.mapper.SysRoleMenuMapper;
import com.wchuan.system.mapper.SysMenuMapper;
import com.wchuan.system.mapper.SysDeptMapper;
import com.wchuan.system.mapper.SysUserRoleMapper;
import com.wchuan.system.service.ISysRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import static com.wchuan.common.enums.RedisKeyEnum.LOGIN_TOKEN_KEY;

/**
 * 角色管理 业务层实现
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Resource
    private SysRoleMapper roleMapper;

    @Resource
    private SysRoleDeptMapper roleDeptMapper;

    @Resource
    private SysUserRoleMapper userRoleMapper;

    @Resource
    private SysRoleMenuMapper roleMenuMapper;

    @Resource
    private SysMenuMapper menuMapper;

    @Resource
    private RedisCache redisCache;

    @Resource
    private SysDeptMapper deptMapper;

    /**
     * 1. 查询角色列表
     */
    @Override
    @DataScope(deptAlias = "d")
    public List<SysRole> selectRoleList(SysRole role) {
        return roleMapper.selectRoleList(role);
    }

    /**
     * 2. 查询角色详情
     */
    @Override
    public SysRole selectRoleById(Long roleId) {
        return roleMapper.selectById(roleId);
    }

    /**
     * 3. 新增角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertRole(SysRole role) {
        if (!checkRoleNameUnique(role)) {
            throw new BusinessException("新增角色'" + role.getRoleName() + "'失败，角色名称已存在！");
        }
        if (!checkRoleKeyUnique(role)) {
            throw new BusinessException("新增角色'" + role.getRoleName() + "'失败，权限字符已存在！");
        }
        role.setDelFlag("0");
        role.setRoleSort(role.getRoleSort() == null ? 0 : role.getRoleSort());
        role.setStatus(StringUtils.isBlank(role.getStatus()) ? "0" : role.getStatus());
        if (StringUtils.isBlank(role.getDataScope())) {
            role.setDataScope("5");
        }
        validateDataScope(role.getDataScope());
        if (roleMapper.insertRole(role) != 1) {
            return false;
        }
        saveRoleMenuRelations(role.getId(), role.getMenuIds());
        saveRoleDeptRelations(role.getId(), role.getDataScope(), role.getDeptIds());
        return true;
    }

    /**
     * 4. 修改角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRole role) {
        if (!checkRoleNameUnique(role)) {
            throw new BusinessException("修改角色'" + role.getRoleName() + "'失败，角色名称已存在！");
        }
        if (!checkRoleKeyUnique(role)) {
            throw new BusinessException("修改角色'" + role.getRoleName() + "'失败，权限字符已存在！");
        }
        SysRole existing = assertRoleCanBeModified(role.getId());
        role.setRoleSort(role.getRoleSort() == null ? existing.getRoleSort() : role.getRoleSort());
        role.setDataScope(StringUtils.isBlank(role.getDataScope()) ? existing.getDataScope() : role.getDataScope());
        role.setStatus(StringUtils.isBlank(role.getStatus()) ? existing.getStatus() : role.getStatus());
        validateDataScope(role.getDataScope());
        if (roleMapper.updateRole(role) != 1) {
            return false;
        }
        saveRoleMenuRelations(role.getId(), role.getMenuIds());
        saveRoleDeptRelations(role.getId(), role.getDataScope(), role.getDeptIds());
        refreshRoleUserCache(role.getId());
        return true;
    }

    /**
     * 🔑 5. 核心授权方法：修改角色的数据权限范围（同步写入 sys_role_dept 表）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean authDataScope(SysRole role) {
        SysRole existing = assertRoleCanBeModified(role.getId());
        validateDataScope(role.getDataScope());
        if (!update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, existing.getId())
                .set(SysRole::getDataScope, role.getDataScope()))) {
            throw new BusinessException("更新角色数据范围失败");
        }

        saveRoleDeptRelations(existing.getId(), role.getDataScope(), role.getDeptIds());
        refreshRoleUserCache(existing.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean authRoleMenu(RoleMenuAuthDTO authDTO) {
        SysRole role = assertRoleCanBeModified(authDTO.getRoleId());
        Set<Long> menuIds = new LinkedHashSet<>();
        if (authDTO.getCheckedKeys() != null) {
            menuIds.addAll(authDTO.getCheckedKeys());
        }
        if (authDTO.getHalfCheckedKeys() != null) {
            menuIds.addAll(authDTO.getHalfCheckedKeys());
        }
        menuIds.remove(null);

        if (!menuIds.isEmpty()) {
            List<SysMenu> validMenus = menuMapper.selectBatchIds(menuIds);
            Set<Long> validIds = validMenus.stream().map(SysMenu::getId).collect(Collectors.toSet());
            if (validIds.size() != menuIds.size()) {
                throw new BusinessException(400, "菜单权限中包含不存在的节点");
            }
        }

        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, role.getId()));
        if (!menuIds.isEmpty()) {
            roleMenuMapper.insertBatch(role.getId(), List.copyOf(menuIds));
        }
        refreshRoleUserCache(role.getId());
        return true;
    }

    private SysRole assertRoleCanBeModified(Long roleId) {
        if (roleId == null) {
            throw new BusinessException(400, "角色ID不能为空");
        }
        SysRole role = roleMapper.selectById(roleId);
        if (role == null || "2".equals(role.getDelFlag())) {
            throw new BusinessException(404, "角色不存在");
        }
        if (Long.valueOf(1L).equals(roleId) || "admin".equals(role.getRoleKey())) {
            throw new BusinessException(403, "超级管理员核心权限禁止修改");
        }
        return role;
    }

    private void validateDataScope(String dataScope) {
        if (!Set.of("1", "2", "3", "4", "5").contains(dataScope)) {
            throw new BusinessException(400, "数据范围必须为1到5");
        }
    }

    private void saveRoleMenuRelations(Long roleId, Long[] menuIds) {
        if (menuIds == null) {
            return;
        }
        Set<Long> uniqueMenuIds = java.util.Arrays.stream(menuIds)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!uniqueMenuIds.isEmpty()) {
            List<SysMenu> validMenus = menuMapper.selectBatchIds(uniqueMenuIds);
            if (validMenus.size() != uniqueMenuIds.size()) {
                throw new BusinessException(400, "菜单权限中包含不存在的节点");
            }
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (!uniqueMenuIds.isEmpty()) {
            roleMenuMapper.insertBatch(roleId, List.copyOf(uniqueMenuIds));
        }
    }

    private void saveRoleDeptRelations(Long roleId, String dataScope, Long[] deptIds) {
        roleDeptMapper.deleteRoleDeptByRoleId(roleId);
        if (!"2".equals(dataScope) || deptIds == null || deptIds.length == 0) {
            return;
        }
        Set<Long> uniqueDeptIds = java.util.Arrays.stream(deptIds)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueDeptIds.isEmpty()) {
            return;
        }
        List<SysDept> validDepartments = deptMapper.selectBatchIds(uniqueDeptIds);
        if (validDepartments.size() != uniqueDeptIds.size()) {
            throw new BusinessException(400, "自定义数据权限中包含不存在的部门");
        }
        List<SysRoleDept> relations = uniqueDeptIds.stream()
                .map(deptId -> {
                    SysRoleDept relation = new SysRoleDept();
                    relation.setRoleId(roleId);
                    relation.setDeptId(deptId);
                    return relation;
                })
                .toList();
        roleDeptMapper.batchRoleDept(relations);
    }

    private void refreshRoleUserCache(Long roleId) {
        List<SysUserRole> relations = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        for (SysUserRole relation : relations) {
            String key = LOGIN_TOKEN_KEY + relation.getUserId();
            LoginUser loginUser = redisCache.getCacheObject(key);
            if (loginUser == null) {
                continue;
            }
            List<String> permissions = menuMapper.selectMenuNameByUserId(relation.getUserId());
            if (Long.valueOf(1L).equals(relation.getUserId()) && !permissions.contains("*")) {
                permissions.add("*");
            }
            loginUser.setPermissions(permissions);
            loginUser.getUser().setRoles(roleMapper.selectRolesByUserId(relation.getUserId()));
            loginUser.setAuthorities(null);
            Long ttl = redisCache.getExpireTime(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0 && ttl <= Integer.MAX_VALUE) {
                redisCache.setCacheObject(key, loginUser, ttl.intValue(), TimeUnit.SECONDS);
            } else {
                redisCache.setCacheObject(key, loginUser);
            }
        }
    }

    /**
     * 6. 删除角色（带安全防护：有用户绑定时不允许删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleById(Long roleId) {
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        if (count != null && count > 0) {
            throw new BusinessException("该角色已分配给用户，不允许删除！");
        }

        // 逻辑删除
        SysRole role = new SysRole();
        role.setId(roleId);
        role.setDelFlag("2");
        roleDeptMapper.deleteRoleDeptByRoleId(roleId);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        return this.updateById(role);
    }

    /**
     * 校验角色名称唯一
     */
    @Override
    public boolean checkRoleNameUnique(SysRole role) {
        Long roleId = role.getId() == null ? -1L : role.getId();
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleName, role.getRoleName()).eq(SysRole::getDelFlag, "0");
        SysRole info = roleMapper.selectOne(wrapper);
        return info == null || info.getId().equals(roleId);
    }

    /**
     * 校验角色权限字符唯一
     */
    @Override
    public boolean checkRoleKeyUnique(SysRole role) {
        Long roleId = role.getId() == null ? -1L : role.getId();
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleKey, role.getRoleKey()).eq(SysRole::getDelFlag, "0");
        SysRole info = roleMapper.selectOne(wrapper);
        return info == null || info.getId().equals(roleId);
    }

}

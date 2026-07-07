package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wchuan.common.utils.SecurityUtils;
import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.mapper.MenuMapper;
import com.wchuan.system.service.MenuService;
import com.wchuan.system.service.helper.MenuValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, SysMenu> implements MenuService {

    private final MenuMapper menuMapper;
    private final MenuValidator menuValidator;

    // ======================== 查询逻辑 ========================

    @Override
    public List<SysMenu> selectMenuTree() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus;

        if (userId == 1L) {
            // 如果是超级管理员，查询所有正常的 M(目录) 和 C(菜单)，跳过按钮 F
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, "0")
                    .in(SysMenu::getMenuType, "M", "C")
                    .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum));
        } else {
            // 如果是普通用户，调用 Mapper 中的自定义 SQL 进行多表关联查询
            menus = menuMapper.selectMenuTreeByUserId(userId);
        }

        if (menus.isEmpty()) return new ArrayList<>();

        Map<Long,List<SysMenu>> groupByPid = menus.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        return recursiveBuildTree(0L,groupByPid);
    }

    private List<SysMenu> recursiveBuildTree(Long parentId, Map<Long, List<SysMenu>> nodesMap) {
        List<SysMenu> children = nodesMap.getOrDefault(parentId,new ArrayList<>());

        return children.stream()
                .sorted(Comparator.comparing(SysMenu::getOrderNum))
                .peek(menu -> menu.setChildren(recursiveBuildTree(menu.getId(),nodesMap)))
                .collect(Collectors.toList());

    }

    // ======================== 写入逻辑 ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMenu(SysMenu menu) {
        // 1. 核心预处理：通过父ID判断权限并计算祖先路径
        prepareMenuHierarchy(menu);
        // 2. 业务规范校验
        menuValidator.validate(menu);
        // 3. 将数据存入数据库
        this.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(SysMenu menu) {
        // 获取旧菜单的数据
        SysMenu oldMenu = this.getById(menu.getId());
        if (oldMenu == null)
            throw new RuntimeException("菜单不存在");

        // 当旧菜单的pid 与现在菜单的pid 不相同时更新
        if (!oldMenu.getParentId().equals(menu.getParentId())) {
            // 1. 处理当前节点的层级属性（判断权限是否正确，对齐pPerms）
            prepareMenuHierarchy(menu);
            // 2. 级联修正所有子孙节点的数据
            applyCascadeUpdate(oldMenu, menu);
        }

        menuValidator.validate(menu);
        this.updateById(menu);
    }

    /**
     * 级联更新逻辑：利用 Materialized Path (ancestors) 批量修正子孙
     */
    private void applyCascadeUpdate(SysMenu oldMenu, SysMenu newMenu) {
        // 判断当前节点祖先路径
        String oldSubtreePrefix = oldMenu.getAncestors() + "," + oldMenu.getId();
        // 一次性查出所有子孙节点
        List<SysMenu> descendants = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().and(w -> w
                        .eq(SysMenu::getAncestors, oldSubtreePrefix)
                        .or()
                        .likeRight(SysMenu::getAncestors, oldSubtreePrefix + ",")
                )
        );

        if (!descendants.isEmpty()) {
            // 权限正则
            String oldPermsRegex = "^" + Pattern.quote(oldMenu.getPerms()) + "(?=:|$)";
            // 路径正则
            String oldPathRegex = "^" + Pattern.quote(oldMenu.getAncestors()) + "(?=,|$)";
            for (SysMenu child : descendants) {
                // 修正权限标识前缀：system:user -> dev:user
                child.setPerms(
                        child.getPerms().replaceFirst(
                                oldPermsRegex, newMenu.getPerms()
                        )
                );
                // 修正祖先路径：0,1 -> 0,2
                child.setAncestors(
                        child.getAncestors().replaceFirst(
                                oldPathRegex, newMenu.getAncestors()
                        )
                );
            }
            this.updateBatchById(descendants);
        }

    }

    // ======================== 自愈引擎 ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selfHealing() {
        // 仅查询必要字段，减少内存压力 (Query Projection)
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                SysMenu::getId,
                SysMenu::getParentId,
                SysMenu::getPerms,
                SysMenu::getAncestors);
        List<SysMenu> all = this.list(queryWrapper);

        // 根据父节点分类
        Map<Long, List<SysMenu>> nodesMap = all.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        // 创建损坏的节点集合
        List<SysMenu> corruptedNodes = new ArrayList<>();

        recursiveAudit(0L, "0", "", nodesMap, corruptedNodes);

        // 如果存在损坏节点则通过 Id 修正
        if (!corruptedNodes.isEmpty()) {
            this.updateBatchById(corruptedNodes);
        }
    }

    // 基于 pid 的权限修正机制
    private void recursiveAudit(Long pId, String expectedPath, String pPerms,
                                Map<Long, List<SysMenu>> map, List<SysMenu> results) {
        // 获取子节点
        List<SysMenu> children = map.getOrDefault(pId, Collections.emptyList());
        // 通过 pid 找到子节点，子节点保存自己的权限  通过父权限拼接
        for (SysMenu child : children) {
            // 拿到节点自身的后缀（如从 sys:user:add 提取出 add）
            String nodeKey = extractLeafName(child.getPerms());
            // 获取父节点的权限与子节点的权限拼接
            String expectedPerms = pPerms.isEmpty() ? nodeKey : pPerms + ":" + nodeKey;

            // 对比并标记异常数据 放入 results 结果集中
            // 当前祖先id与通过pid得到的祖先id不一致 或者 当前节点的权限与通过pid得到的权限不一致
            if (!expectedPerms.equals(child.getPerms()) ||
                    !expectedPath.equals(child.getAncestors())
            ) {
                // 将祖先重新设置为预期的祖先路径
                child.setAncestors(expectedPath);
                // 将权限设置为期待的权限
                child.setPerms(expectedPerms);
                // 将该节点放入结果集中 通过 Mybatis-Plus 更新数据库数据
                results.add(child);
            }
            // 递归审计子树
            recursiveAudit(child.getId(), expectedPath + "," + child.getId(), expectedPerms, map, results);
        }
    }

    // ======================== 工具方法 ========================
    private void prepareMenuHierarchy(SysMenu menu) {
        // A. 自动对齐 Perms
        syncPermsFromPid(menu);
        // B. 计算 Ancestors
        menu.setAncestors(calculateAncestors(menu.getParentId()));
    }

    // 通过pid判断获取pPerms判断pPrems是否和leafPerms前缀相同 不相同则将leafPerms权限修正
    private void syncPermsFromPid(SysMenu menu) {
        // 当前权限为空
        if (!StringUtils.hasText(menu.getPerms())) return;
        // 获取pid
        Long pid = menu.getParentId();
        // 如果是一级菜单 不用强制对齐
        if (pid == null || pid == 0L) return;

        // 通过 pid 获取 pPerms
        SysMenu pMenu = menuMapper.selectById(pid);
        if (pMenu == null || !StringUtils.hasText(pMenu.getPerms())) {
            // 父节点不存在或者父节点权限为空直接返回
            return;
        }
        // 父节点权限
        String pPerms = pMenu.getPerms();
        // 叶子节点权限
        String leafPerms = extractLeafName(menu.getPerms());
        String expectedPerms = pPerms + ":" + leafPerms;
        // 比较 pPerms 是否与最后一个 ":" 之前的权限（父权限相等）
        if(expectedPerms.equals(menu.getPerms())) return;

        menu.setPerms(expectedPerms);
    }

    // 获取叶子的权限信息
    private String extractLeafName(String perms) {
        if (!StringUtils.hasText(perms)) return "";
        return perms.contains(":") ? perms.substring(perms.lastIndexOf(":") + 1) : perms;
    }

    private String calculateAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) return "0";
        SysMenu parent = this.getById(parentId);
        return (parent != null) ? parent.getAncestors() + "," + parent.getId() : "0";
    }

    @Override
    public void validateMenu(SysMenu menu) {
        menuValidator.validate(menu);
    }

}
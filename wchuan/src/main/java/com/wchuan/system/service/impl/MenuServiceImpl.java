package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.mapper.MenuMapper;
import com.wchuan.system.service.MenuService;
import com.wchuan.system.service.helper.MenuValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, SysMenu> implements MenuService {

    private final MenuMapper menuMapper;
    private final MenuValidator menuValidator;

    /**
     * 1. 获取完整菜单树
     */
    @Override
    public List<SysMenu> selectMenuTree() {
        List<SysMenu> menus = menuMapper.selectList(null);
        return buildTree(menus, 0L);
    }

    /**
     * 2. 递归构建逻辑
     */
    @Override
    public List<SysMenu> buildTree(List<SysMenu> list, Long parentId) {
        return list.stream()
                .filter(menu -> menu.getParentId().equals(parentId))
                .sorted(Comparator.comparing(SysMenu::getOrderNum))
                .peek(menu -> menu.setChildren(buildTree(list, menu.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 3. 新增菜单 (含自动归位、祖先路径计算、校验)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveMenu(SysMenu menu) {
        // 自动计算 ParentId (基于 perms 前缀)
        this.autoAlignParentByPerms(menu);

        // 计算当前节点的 ancestors
        menu.setAncestors(calculateNewAncestors(menu));

        // 业务规则校验
        menuValidator.validate(menu);

        return this.save(menu);
    }

    /**
     * 4. 修改菜单 (含批量级联更新算法)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(SysMenu menu) {
        SysMenu oldMenu = this.getById(menu.getId());
        if (oldMenu == null) throw new RuntimeException("菜单不存在");

        String oldPermsPrefix = oldMenu.getPerms();
        String oldAncestorsPrefix = oldMenu.getAncestors() + "," + oldMenu.getId();

        // 只有当 ParentId 发生变化时才触发级联更新
        if (!oldMenu.getParentId().equals(menu.getParentId())) {
            // 自动归位并重算当前节点 perms
            this.autoAlignParentByPerms(menu);
            // 重算当前节点 ancestors
            menu.setAncestors(calculateNewAncestors(menu));

            // 级联更新所有子孙节点
            String newPermsPrefix = menu.getPerms();
            String newAncestorsPrefix = menu.getAncestors() + "," + menu.getId();

            // 一次性查出所有子孙 (利用 ancestors 前缀匹配)
            List<SysMenu> children = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .likeRight(SysMenu::getAncestors, oldAncestorsPrefix));

            if (!children.isEmpty()) {
                for (SysMenu child : children) {
                    // 同步修正子孙的 perms 命名空间
                    child.setPerms(child.getPerms().replaceFirst(oldPermsPrefix, newPermsPrefix));
                    // 同步修正子孙的 ancestors 路径
                    child.setAncestors(child.getAncestors().replaceFirst(oldMenu.getAncestors(), menu.getAncestors()));
                }
                this.updateBatchById(children);
            }
        }

        menuValidator.validate(menu);
        return this.updateById(menu);
    }

    /**
     * 5. 数据自愈引擎 (全量扫描并修正)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selfHealing() {
        List<SysMenu> all = this.list();
        Map<Long, SysMenu> map = all.stream().collect(Collectors.toMap(SysMenu::getId, m -> m));
        List<SysMenu> needUpdate = new ArrayList<>();

        // 递归从根节点开始校验每条数据的 perms 和 ancestors
        recursiveCheck(0L, "0", "", all, map, needUpdate);

        if (!needUpdate.isEmpty()) {
            this.updateBatchById(needUpdate);
        }
    }

    // --- 辅助私有方法 ---

    private String calculateNewAncestors(SysMenu menu) {
        if (menu.getParentId() == 0L) return "0";
        SysMenu parent = this.getById(menu.getParentId());
        return parent.getAncestors() + "," + parent.getId();
    }

    private void autoAlignParentByPerms(SysMenu menu) {
        String perms = menu.getPerms();
        if (!StringUtils.hasText(perms)) return;

        if (perms.contains(":")) {
            String parentPerms = perms.substring(0, perms.lastIndexOf(":"));
            SysMenu parentMenu = this.getOne(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getPerms, parentPerms).last("LIMIT 1"));
            if (parentMenu != null) {
                menu.setParentId(parentMenu.getId());
            } else {
                throw new RuntimeException("归位失败：找不到前缀标识为 [" + parentPerms + "] 的上级");
            }
        } else {
            menu.setParentId(0L);
        }
    }

    private void recursiveCheck(Long parentId, String path, String pPerms,
                                List<SysMenu> all, Map<Long, SysMenu> map, List<SysMenu> results) {
        List<SysMenu> children = all.stream().filter(m -> m.getParentId().equals(parentId)).toList();
        for (SysMenu child : children) {
            String leaf = child.getPerms().contains(":") ?
                    child.getPerms().substring(child.getPerms().lastIndexOf(":") + 1) : child.getPerms();
            String expectedPerms = pPerms.isEmpty() ? leaf : pPerms + ":" + leaf;

            if (!path.equals(child.getAncestors()) || !expectedPerms.equals(child.getPerms())) {
                child.setAncestors(path);
                child.setPerms(expectedPerms);
                results.add(child);
            }
            recursiveCheck(child.getId(), path + "," + child.getId(), child.getPerms(), all, map, results);
        }
    }

    @Override
    public void validateMenu(SysMenu menu) {
        menuValidator.validate(menu);
    }
}
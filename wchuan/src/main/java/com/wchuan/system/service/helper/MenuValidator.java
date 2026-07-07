package com.wchuan.system.service.helper;

import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.wchuan.common.constants.MenuConstants.*;

@Component
@RequiredArgsConstructor
public class MenuValidator {

    // 直接注入 Mapper，彻底切断对 Service 的依赖，消除循环引用
    private final MenuMapper menuMapper;

    /**
     * 统一校验入口
     */
    public void validate(SysMenu menu) {

        Long parentId = menu.getParentId();

        // 获取父节点数据（统一查询，减少 DB 压力）
        SysMenu parent =
                (parentId != null && parentId != 0L) ? menuMapper.selectById(parentId) : null;

        // 执行各项检验
        checkSelfAndCircular(menu, parent); // 检查是否自循环
        checkTypeHierarchy(menu, parent); // 检查类层级
        checkPermsNamespace(menu, parent); // 检查权限空间
    }

    /** 1. 校验逻辑：防止自选和死循环 */
    private void checkSelfAndCircular(SysMenu menu,SysMenu parent) {
        Long menuId = menu.getId();
        Long parentId = menu.getParentId();

        // 检验是否为空即可
        if (menuId == null || parentId == null) return;

        // 不能选自己作为上级
        if (menuId.equals(parentId)) {
            throw new RuntimeException("修改失败，上级菜单不能选择自己");
        }

        if (parent != null && isIdInPath(menuId.toString(), parent.getAncestors())) {
            throw new RuntimeException("修改失败，不能将父节点移至子节点下");
        }

    }

    /** 2. 校验逻辑：M-C-F 结构完整性 */
    private void checkTypeHierarchy(SysMenu menu,SysMenu parent) {

        // 获取当前权限类型
        String type = menu.getMenuType();

        // 顶级节点校验
        if (menu.getParentId() == 0L) {
            if (TYPE_BUTTON.equals(type)) {
                // 自身权限类型为按钮时
                throw new RuntimeException("操作失败：按钮(F)不能作为顶级节点");
            }
            return;
        }

        if (parent == null) {
            throw new RuntimeException("操作失败：上级菜单不存在");
        }

        String pType = parent.getMenuType();

        if (TYPE_BUTTON.equals(pType)) {
            // 父权限类型为按钮时
            throw new RuntimeException("操作失败：按钮下方不允许添加子项");
        }

        if ((TYPE_DIR.equals(type) || TYPE_MENU.equals(type)) && !TYPE_DIR.equals(pType)) {
            // 自身权限类型为菜单时(菜单不能挂载在菜单下面)  或自身权限类型为菜单时但父权限类型不为目录时
            throw new RuntimeException("操作失败：目录或菜单只能挂载在“目录”下");
        }

        if (TYPE_BUTTON.equals(type) && !TYPE_MENU.equals(pType)) {
            // 自身菜单类型为按钮且父权限类型不为菜单时
            throw new RuntimeException("操作失败：按钮必须挂载在“菜单”页面下");
        }

    }

    /** 3. 校验逻辑：权限标识命名空间前缀 */
    private void checkPermsNamespace(SysMenu menu,SysMenu parent) {
        String perms = menu.getPerms();
        String type = menu.getMenuType();

        // A. 根节点不参与命名空间校验
        if (parent == null ||
                !StringUtils.hasText(parent.getPerms()) ||
                !StringUtils.hasText(perms))
            return;

        // B. 菜单和按钮强制要求有权限标识
        if ((TYPE_MENU.equals(type) || TYPE_BUTTON.equals(type)) && !StringUtils.hasText(perms)) {
            throw new RuntimeException("操作失败：菜单或功能必须配置权限标识");
        }

        // C. 前缀对齐校验
        String prefix = parent.getPerms() + ":";
        // 如果父级有标识，子级必须以前缀开头（system -> system:user）
        if (!perms.startsWith(prefix))
                throw new RuntimeException("权限冲突：标识必须以父级前缀 [" + prefix + "] 开头");

        // D. 深度校验（冒号数量）
        if (countColons(perms) != countColons(parent.getPerms()) + 1)
            throw new RuntimeException("规范错误：权限标识层级不符合 M:C:F 结构深度");

    }

    private boolean isIdInPath(String menuId,String ancestors){
        if (!StringUtils.hasText(ancestors)) return false;
        return Arrays.asList(ancestors.split(",")).contains(menuId);
    }

    /**
     * 计算字符串中冒号的数量
     */
    private long countColons(String str) {
        if (str == null) return 0;
        return str.chars().filter(ch -> ch == ':').count();
    }


//    /**
//     * 判断 parentId 是否是 menuId 的子孙节点
//     */
//    private boolean isChild(Long menuId, Long parentId) {
//        // 查出全量菜单数据进行递归比对
//        List<SysMenu> menuList = menuMapper.selectList(null);
//        List<Long> childIds = new ArrayList<>();
//
//        // 递归填充 childIds 集合
//        fillChildIds(menuList, menuId, childIds);
//
//        return childIds.contains(parentId);
//    }
//
//    /**
//     * 递归辅助：获取某个菜单下的所有子孙ID
//     */
//    private void fillChildIds(List<SysMenu> list, Long menuId, List<Long> childIds) {
//        for (SysMenu menu : list) {
//            // 如果某条记录的父ID等于我们要查的那个ID，说明它是子项
//            if (Objects.equals(menu.getParentId(), menuId)) {
//                childIds.add(menu.getId());
//                // 继续向下递归寻找它的后代
//                fillChildIds(list, menu.getId(), childIds);
//            }
//        }
//    }

}
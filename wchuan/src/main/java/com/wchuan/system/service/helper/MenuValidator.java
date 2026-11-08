package com.wchuan.system.service.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wchuan.common.exception.BusinessException;
import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.wchuan.common.constants.MenuConstants.TYPE_BUTTON;
import static com.wchuan.common.constants.MenuConstants.TYPE_DIR;
import static com.wchuan.common.constants.MenuConstants.TYPE_MENU;

@Component
@RequiredArgsConstructor
public class MenuValidator {

    private static final Set<String> MENU_TYPES = Set.of(TYPE_DIR, TYPE_MENU, TYPE_BUTTON);
    private static final String PERMISSION_PATTERN =
            "^[A-Za-z][A-Za-z0-9_-]*(?::[A-Za-z][A-Za-z0-9_-]*)*$";
    private static final String COMPONENT_PATTERN = "^(?!/)(?!.*\\.\\.)([A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+$";

    private final SysMenuMapper menuMapper;

    public void validate(SysMenu menu) {
        normalizeDefaults(menu);
        validateRequiredFields(menu);
        SysMenu parent = resolveParent(menu.getParentId());
        checkCircularReference(menu.getId(), menu.getParentId());
        checkTypeHierarchy(menu, parent);
        checkPermission(menu);
        checkComponent(menu);
        checkUnique(menu);
    }

    private void normalizeDefaults(SysMenu menu) {
        menu.setParentId(menu.getParentId() == null ? 0L : menu.getParentId());
        menu.setOrderNum(menu.getOrderNum() == null ? 0 : menu.getOrderNum());
        menu.setStatus(StringUtils.hasText(menu.getStatus()) ? menu.getStatus() : "0");
        menu.setVisible(StringUtils.hasText(menu.getVisible()) ? menu.getVisible() : "0");
    }

    private void validateRequiredFields(SysMenu menu) {
        if (!StringUtils.hasText(menu.getMenuName())) {
            throw new BusinessException(400, "菜单名称不能为空");
        }
        menu.setMenuName(menu.getMenuName().trim());
        if (!MENU_TYPES.contains(menu.getMenuType())) {
            throw new BusinessException(400, "菜单类型必须为M、C或F");
        }
        if (!Set.of("0", "1").contains(menu.getStatus())
                || !Set.of("0", "1").contains(menu.getVisible())) {
            throw new BusinessException(400, "菜单状态或显示状态不合法");
        }
    }

    private SysMenu resolveParent(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return null;
        }
        SysMenu parent = menuMapper.selectById(parentId);
        if (parent == null || "2".equals(parent.getDelFlag())) {
            throw new BusinessException(404, "上级菜单不存在");
        }
        return parent;
    }

    private void checkCircularReference(Long menuId, Long parentId) {
        if (menuId == null || parentId == null || parentId == 0L) {
            return;
        }
        Set<Long> visited = new HashSet<>();
        Long currentId = parentId;
        while (currentId != null && currentId != 0L) {
            if (!visited.add(currentId)) {
                throw new BusinessException(409, "菜单树中已存在循环引用");
            }
            if (Objects.equals(menuId, currentId)) {
                throw new BusinessException(400, "上级菜单不能选择自身或自身的子孙菜单");
            }
            SysMenu current = menuMapper.selectById(currentId);
            if (current == null) {
                throw new BusinessException(404, "上级菜单不存在");
            }
            currentId = current.getParentId();
        }
    }

    private void checkTypeHierarchy(SysMenu menu, SysMenu parent) {
        String type = menu.getMenuType();
        if (parent == null) {
            if (TYPE_BUTTON.equals(type)) {
                throw new BusinessException(400, "按钮不能作为顶级节点");
            }
            return;
        }
        if (TYPE_BUTTON.equals(parent.getMenuType())) {
            throw new BusinessException(400, "按钮下不允许添加子节点");
        }
        if ((TYPE_DIR.equals(type) || TYPE_MENU.equals(type)) && !TYPE_DIR.equals(parent.getMenuType())) {
            throw new BusinessException(400, "目录或菜单只能挂载在目录下");
        }
        if (TYPE_BUTTON.equals(type) && !TYPE_MENU.equals(parent.getMenuType())) {
            throw new BusinessException(400, "按钮必须挂载在页面菜单下");
        }
    }

    private void checkPermission(SysMenu menu) {
        String perms = StringUtils.hasText(menu.getPerms()) ? menu.getPerms().trim() : null;
        menu.setPerms(perms);
        if ((TYPE_MENU.equals(menu.getMenuType()) || TYPE_BUTTON.equals(menu.getMenuType()))
                && !StringUtils.hasText(perms)) {
            throw new BusinessException(400, "菜单或按钮必须配置权限标识");
        }
        if (!StringUtils.hasText(perms)) {
            return;
        }
        if (perms.contains("*")) {
            throw new BusinessException(403, "菜单权限标识禁止使用通配符");
        }
        if (!perms.matches(PERMISSION_PATTERN)) {
            throw new BusinessException(400, "权限标识格式不合法");
        }
    }

    private void checkComponent(SysMenu menu) {
        if (TYPE_DIR.equals(menu.getMenuType())) {
            menu.setComponent(
                    StringUtils.hasText(menu.getComponent())
                            ? menu.getComponent().trim()
                            : null
            );
            menu.setPath(
                    StringUtils.hasText(menu.getPath())
                            ? menu.getPath().trim()
                            : ""
            );
            return;
        }
        if (TYPE_BUTTON.equals(menu.getMenuType())) {
            menu.setPath("#");
            menu.setComponent(null);
            return;
        }
        if (!StringUtils.hasText(menu.getPath())) {
            throw new BusinessException(400, "菜单路由地址不能为空");
        }
        if (!StringUtils.hasText(menu.getComponent())) {
            throw new BusinessException(400, "菜单组件路径不能为空");
        }
        menu.setPath(menu.getPath().trim());
        menu.setComponent(menu.getComponent().trim());
        if (!menu.getComponent().matches(COMPONENT_PATTERN)) {
            throw new BusinessException(400, "菜单组件路径格式不合法");
        }
    }

    private void checkUnique(SysMenu menu) {
        Long id = menu.getId() == null ? -1L : menu.getId();
        Long nameCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menu.getParentId())
                .eq(SysMenu::getMenuName, menu.getMenuName())
                .ne(SysMenu::getId, id)
                .and(wrapper -> wrapper.isNull(SysMenu::getDelFlag).or().ne(SysMenu::getDelFlag, "2")));
        if (nameCount != null && nameCount > 0) {
            throw new BusinessException(409, "同级菜单名称已存在");
        }

        if (!TYPE_BUTTON.equals(menu.getMenuType()) && StringUtils.hasText(menu.getPath())) {
            Long pathCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getParentId, menu.getParentId())
                    .eq(SysMenu::getPath, menu.getPath())
                    .ne(SysMenu::getId, id)
                    .and(wrapper -> wrapper.isNull(SysMenu::getDelFlag).or().ne(SysMenu::getDelFlag, "2")));
            if (pathCount != null && pathCount > 0) {
                throw new BusinessException(409, "同级路由地址已存在");
            }
        }
    }
}

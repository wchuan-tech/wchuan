package com.wchuan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wchuan.common.exception.BusinessException;
import com.wchuan.common.utils.SecurityUtils;
import com.wchuan.system.domain.entity.SysMenu;
import com.wchuan.system.domain.entity.SysRoleMenu;
import com.wchuan.system.domain.dto.MenuTreeNodeDTO;
import com.wchuan.system.domain.dto.RoleMenuTreeDTO;
import com.wchuan.system.mapper.SysMenuMapper;
import com.wchuan.system.mapper.SysRoleMenuMapper;
import com.wchuan.system.service.MenuService;
import com.wchuan.system.service.helper.MenuValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 菜单管理服务实现。
 *
 * <p><b>职责范围：</b>
 * <ul>
 *     <li>菜单树查询：当前用户菜单树、管理端全量菜单、角色授权树；</li>
 *     <li>菜单写入：新增、修改（含级联调整子孙路径）、删除（多重保护）；</li>
 *     <li>菜单拓扑自愈：修复因历史脏数据导致的 {@code ancestors} 错乱。</li>
 * </ul>
 *
 * <p><b>核心设计（树形结构双存储）：</b>
 * <ul>
 *     <li><b>邻接表</b>（{@code parent_id}）：描述“直接父节点”，用于<b>组装树</b>；</li>
 *     <li><b>物化路径</b>（{@code ancestors}，形如 {@code "0,1,3"}）：描述“从根到父的完整路径”，
 *         用于<b>一次查询整棵子树</b>、判断父子关系、以及移动节点时的<b>级联更新</b>。</li>
 * </ul>
 * 二者分工明确：{@code parent_id} 管“建树”，{@code ancestors} 管“查子孙”，互补而非替代。
 *
 * <p><b>关键约束：</b>
 * <ul>
 *     <li>系统核心菜单（{@code system:user}、{@code system:role}、{@code system:menu}）
 *         仅超级管理员可修改/删除/停用；</li>
 *     <li>已分配给角色的菜单，禁止修改权限标识（{@code perms}）与状态（{@code status}），
 *         需先解除角色授权，避免角色权限静默失效。</li>
 * </ul>
 *
 * @author WChuan
 * @see SysMenu
 * @see MenuValidator
 * @see com.wchuan.system.service.MenuService
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements MenuService {

    /**
     * 系统核心权限前缀集合。
     *
     * <p>命中这些前缀的菜单被视为“核心菜单”，普通管理员不可修改、删除或停用，
     * 防止误操作导致系统管理入口被锁死。
     */
    private static final Set<String> CORE_PERMISSION_PREFIXES = Set.of(
            "system:user", "system:role", "system:menu");

    /** 菜单 Mapper（含自定义树查询 SQL）。 */
    private final SysMenuMapper menuMapper;

    /** 菜单参数校验器，负责必填、类型层级、循环引用、权限标识、唯一性等校验。 */
    private final MenuValidator menuValidator;

    /** 角色-菜单关联 Mapper，用于角色授权与删除前的授权检查。 */
    private final SysRoleMenuMapper roleMenuMapper;

    // ======================== 查询逻辑 ========================

    /**
     * 查询<b>当前登录用户</b>的菜单树（用于前端侧边栏渲染）。
     *
     * <p><b>数据范围：</b>
     * <ul>
     *     <li>超级管理员（userId = 1）：查询全部正常状态的目录（M）与菜单（C）；</li>
     *     <li>普通用户：通过 {@code sys_user_role → sys_role → sys_role_menu → sys_menu}
     *         多表关联，仅返回“用户有权限”的目录与菜单。</li>
     * </ul>
     *
     * <p><b>过滤规则：</b>仅 {@code status = '0'}（正常）；不包含按钮（F），
     * 因为侧边栏只展示“入口”，按钮权限由后端 {@code @PreAuthorize} 校验。
     *
     * @return 菜单树（根节点为 {@code parentId = 0}），无数据时返回空列表
     */
    @Override
    public List<SysMenu> selectMenuTree() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus;

        if (userId == 1L) {
            // 超管：无角色关联，直接查询全部 M/C，避免多表 JOIN
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, "0")
                    .in(SysMenu::getMenuType, "M", "C")
                    .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum));
        } else {
            // 普通用户：调用 Mapper 自定义 SQL，按“用户-角色-菜单”链路过滤
            menus = menuMapper.selectMenuTreeByUserId(userId);
        }

        if (menus.isEmpty()) {
            return new ArrayList<>();
        }

        // 一次查全量 + 内存分组 + 递归组装，避免逐层查库（N+1）
        Map<Long, List<SysMenu>> groupByPid = groupByParentId(menus);

        // visiting 集合用于检测脏数据导致的循环引用，防止递归栈溢出
        return recursiveBuildTree(0L, groupByPid, new HashSet<>());
    }

    /**
     * 查询<b>管理端全量菜单树</b>（用于菜单管理页）。
     *
     * <p>与 {@link #selectMenuTree()} 的区别：
     * <ul>
     *     <li>不做权限过滤，返回全部菜单；</li>
     *     <li>包含按钮（F），因为管理页需要维护按钮级权限标识；</li>
     *     <li>不过滤 {@code status}，停用菜单也需要展示以便重新启用；</li>
     *     <li>仅排除逻辑删除（{@code del_flag = '2'}）的数据。</li>
     * </ul>
     *
     * @return 全量菜单树
     */
    @Override
    public List<SysMenu> selectMenuList() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .and(wrapper ->
                        wrapper.isNull(SysMenu::getDelFlag)
                                .or()
                                .ne(SysMenu::getDelFlag, "2"))
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum, SysMenu::getId));
        return recursiveBuildTree(0L, groupByParentId(menus), new HashSet<>());
    }

    /**
     * 查询<b>角色授权用的完整菜单与按钮权限树</b>。
     *
     * <p>用于“给角色分配菜单/按钮权限”的场景：
     * <ul>
     *     <li>包含目录（M）、菜单（C）和按钮（F），因为按钮才是细粒度权限的载体；</li>
     *     <li>仅返回 {@code status = '0'}（正常）的菜单，停用菜单不参与授权；</li>
     *     <li>返回 {@link MenuTreeNodeDTO}（{@code id/label/children}），
     *         以适配前端 Tree 组件的数据结构。</li>
     * </ul>
     *
     * @return 菜单权限树（DTO 结构）
     */
    @Override
    public List<MenuTreeNodeDTO> selectMenuTreeSelect() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, "0")
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum));
        Map<Long, List<SysMenu>> grouped = groupByParentId(menus);
        return buildMenuTreeNodes(0L, grouped);
    }

    /**
     * 查询<b>指定角色已授权的菜单树</b>，并回显勾选状态。
     *
     * <p>在 {@link #selectMenuTreeSelect()} 的完整权限树基础上：
     * <ul>
     *     <li>查出该角色已分配的菜单 ID 集合；</li>
     *     <li>递归计算每个节点的勾选态：全选（checked）、半选（halfChecked）、未选。</li>
     * </ul>
     *
     * @param roleId 角色 ID
     * @return 角色授权树 + 已勾选节点（{@code checkedKeys}）+ 半选节点（{@code halfCheckedKeys}）
     */
    @Override
    public RoleMenuTreeDTO selectRoleMenuTree(Long roleId) {
        List<MenuTreeNodeDTO> menus = selectMenuTreeSelect();
        Set<Long> assignedIds = new HashSet<>(roleMenuMapper.selectMenuIdsByRoleId(roleId));
        List<Long> checkedKeys = new ArrayList<>();
        List<Long> halfCheckedKeys = new ArrayList<>();
        for (MenuTreeNodeDTO node : menus) {
            collectTreeSelection(node, assignedIds, checkedKeys, halfCheckedKeys);
        }
        return new RoleMenuTreeDTO(checkedKeys, halfCheckedKeys, menus);
    }

    /**
     * 递归计算节点的勾选状态（父子联动）。
     *
     * <p><b>返回值语义：</b>
     * <ul>
     *     <li>{@code 2}：全选——叶子节点已分配，或非叶子节点自身已分配且所有子节点均全选；</li>
     *     <li>{@code 1}：半选——自身已分配但子节点未全选，或存在已选中子节点；</li>
     *     <li>{@code 0}：未选。</li>
     * </ul>
     *
     * @param node            当前节点
     * @param assignedIds     该角色已分配的菜单 ID 集合
     * @param checkedKeys     全选节点收集器
     * @param halfCheckedKeys 半选节点收集器
     * @return 当前节点的勾选状态（0/1/2）
     */
    private int collectTreeSelection(MenuTreeNodeDTO node, Set<Long> assignedIds,
                                     List<Long> checkedKeys, List<Long> halfCheckedKeys) {
        // 叶子节点：直接看是否被分配
        if (node.getChildren().isEmpty()) {
            if (assignedIds.contains(node.getId())) {
                checkedKeys.add(node.getId());
                return 2;
            }
            return 0;
        }

        boolean hasSelectedChild = false;
        boolean allChildrenSelected = true;
        for (MenuTreeNodeDTO child : node.getChildren()) {
            int childState = collectTreeSelection(child, assignedIds, checkedKeys, halfCheckedKeys);
            hasSelectedChild |= childState != 0;
            // 注意：这里只认“全选(2)”，半选(1)不算全选，因此父节点最多只能半选
            allChildrenSelected &= childState == 2;
        }
        if (assignedIds.contains(node.getId()) && allChildrenSelected) {
            checkedKeys.add(node.getId());
            return 2;
        }
        if (assignedIds.contains(node.getId()) || hasSelectedChild) {
            halfCheckedKeys.add(node.getId());
            return 1;
        }
        return 0;
    }

    /**
     * 将实体树转换为前端 Tree 组件所需的 DTO 树。
     *
     * @param parentId 父节点 ID（根传 0）
     * @param grouped  按 parentId 分组后的菜单集合
     * @return DTO 树
     */
    private List<MenuTreeNodeDTO> buildMenuTreeNodes(Long parentId, Map<Long, List<SysMenu>> grouped) {
        return grouped.getOrDefault(parentId, Collections.emptyList()).stream()
                .sorted(menuComparator())
                .map(menu -> new MenuTreeNodeDTO(
                        menu.getId(),
                        menu.getMenuName(),
                        menu.getMenuName(),
                        buildMenuTreeNodes(menu.getId(), grouped)))
                .toList();
    }

    /**
     * 递归组装菜单树，并检测循环引用。
     *
     * <p><b>为什么要检测循环引用：</b>如果数据库中存在脏数据（如 A 的父是 B、B 的父又是 A），
     * 递归会无限下去并导致 {@link StackOverflowError}。这里用“当前递归路径”集合 {@code visiting} 检测：
     * 一旦节点在路径中重复出现，立即抛业务异常。
     *
     * @param parentId 当前父节点 ID（根传 0）
     * @param nodesMap 按 parentId 分组后的菜单集合
     * @param visiting 当前递归路径上的节点 ID 集合（用于环检测）
     * @return 以 {@code parentId} 为根的子树
     */
    private List<SysMenu> recursiveBuildTree(Long parentId, Map<Long, List<SysMenu>> nodesMap,
                                             Set<Long> visiting) {
        if (!visiting.add(parentId)) {
            throw new BusinessException(500, "菜单树存在循环引用，节点ID=" + parentId);
        }
        try {
            return nodesMap.getOrDefault(parentId, Collections.emptyList()).stream()
                    .sorted(menuComparator())
                    .map(menu -> {
                        menu.setChildren(recursiveBuildTree(menu.getId(), nodesMap, visiting));
                        return menu;
                    })
                    .toList();
        } finally {
            // 回溯：离开当前分支时移除，保证 visiting 只表示“当前递归路径”
            visiting.remove(parentId);
        }
    }

    // ======================== 写入逻辑 ========================

    /**
     * 新增菜单。
     *
     * <p>流程：参数校验 → 权限保护校验 → 计算物化路径 → 落库。
     *
     * <p><b>注意：</b>新增时强制将 {@code id} 置空，防止前端传入 ID 造成覆盖写入。
     *
     * @param menu 待新增菜单
     * @throws BusinessException 校验失败、无权限或落库失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMenu(SysMenu menu) {
        // 防御：新增不允许携带主键，避免误覆盖同 ID 数据
        menu.setId(null);
        menuValidator.validate(menu);
        checkPermissionOperation(null, menu);
        menu.setAncestors(calculateAncestors(menu.getParentId()));
        menu.setDelFlag("0");
        if (!this.save(menu)) {
            throw new BusinessException("新增菜单失败");
        }
    }

    /**
     * 修改菜单。
     *
     * <p>若父节点发生变化，则 {@code ancestors} 随之变化，此时需要<b>级联更新所有子孙节点</b>，
     * 保证整棵子树的物化路径一致。
     *
     * @param menu 待修改菜单（需包含 id）
     * @throws BusinessException 菜单不存在、校验失败、无权限或更新失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(SysMenu menu) {
        // 获取旧数据：用于对比父节点是否变化、判断是否核心菜单
        SysMenu oldMenu = this.getById(menu.getId());
        if (oldMenu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        menuValidator.validate(menu);
        checkPermissionOperation(oldMenu, menu);
        String newAncestors = calculateAncestors(menu.getParentId());
        menu.setAncestors(newAncestors);
        // 仅当路径变化（即父节点变化）时才需要级联，避免无谓的批量更新
        if (!Objects.equals(oldMenu.getAncestors(), newAncestors)) {
            applyCascadeUpdate(oldMenu, menu);
        }
        if (!this.updateById(menu)) {
            throw new BusinessException("修改菜单失败");
        }
    }

    /**
     * 级联更新子孙节点的物化路径。
     *
     * <p><b>物化路径的核心操作：</b>父节点移动后，其所有子孙的 {@code ancestors} 都要把
     * “旧前缀”替换为“新前缀”。
     *
     * <p>例如旧前缀为 {@code "0,1,3"}、新前缀为 {@code "0,2,3"}，则
     * {@code "0,1,3,7"} → {@code "0,2,3,7"}。
     *
     * <p><b>为什么用 {@link Pattern#quote(String)}：</b>前缀会被当作正则表达式使用，
     * 虽然当前格式（数字+逗号）没有特殊字符，但 quote 可防止未来格式变化引入正则注入/错误。
     *
     * @param oldMenu 修改前的菜单（提供旧 ancestors）
     * @param newMenu 修改后的菜单（提供新 ancestors）
     */
    private void applyCascadeUpdate(SysMenu oldMenu, SysMenu newMenu) {
        String oldSubtreePrefix = oldMenu.getAncestors() + "," + oldMenu.getId();
        String newSubtreePrefix = newMenu.getAncestors() + "," + newMenu.getId();
        // 一次性查出所有子孙节点：
        //   · ancestors 等于旧前缀 → 直接子节点
        //   · ancestors 以“旧前缀 + ,”开头 → 更深层子孙
        // 加逗号是为了避免 “3” 误匹配 “30”。
        List<SysMenu> descendants = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().and(w -> w
                        .eq(SysMenu::getAncestors, oldSubtreePrefix)
                        .or()
                        .likeRight(SysMenu::getAncestors, oldSubtreePrefix + ",")
                )
        );

        if (!descendants.isEmpty()) {
            for (SysMenu child : descendants) {
                if (StringUtils.hasText(child.getAncestors())) {
                    // 前缀替换：仅替换路径开头的旧前缀，保留后续相对路径
                    child.setAncestors(child.getAncestors().replaceFirst(
                            "^" + Pattern.quote(oldSubtreePrefix),
                            newSubtreePrefix));
                }
            }
            // 批量更新，减少数据库交互次数
            this.updateBatchById(descendants);
        }
    }

    /**
     * 删除菜单。
     *
     * <p><b>三重保护：</b>
     * <ol>
     *     <li>核心系统菜单禁止删除；</li>
     *     <li>存在子菜单禁止删除（防止产生孤儿节点）；</li>
     *     <li>已分配给角色禁止删除（防止角色权限悬空，需先解除授权）。</li>
     * </ol>
     *
     * @param id 菜单 ID
     * @throws BusinessException 菜单不存在或命中保护规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenuById(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        if (isCoreMenu(menu)) {
            throw new BusinessException(403, "核心系统菜单禁止删除");
        }
        // 子菜单检查：排除逻辑删除的数据
        Long childCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id)
                .and(wrapper -> wrapper.isNull(SysMenu::getDelFlag).or().ne(SysMenu::getDelFlag, "2")));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(409, "存在子菜单，不允许删除");
        }
        // 角色授权检查：已授权菜单必须先解除授权
        Long roleCount = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, id));
        if (roleCount != null && roleCount > 0) {
            throw new BusinessException(409, "菜单已分配给角色，请先解除角色授权");
        }
        if (!removeById(id)) {
            throw new BusinessException("删除菜单失败");
        }
    }

    // ======================== 自愈引擎 ========================

    /**
     * 菜单拓扑自愈（运维专用）。
     *
     * <p>用于修复因历史 Bug、手工改库等原因导致的 {@code ancestors} 与 {@code parent_id}
     * 不一致的问题：递归遍历整棵树，重新计算每个节点“应有的 ancestors”，
     * 将不一致的节点批量修正。
     *
     * <p><b>限制：</b>
     * <ul>
     *     <li>仅超级管理员可执行；</li>
     *     <li>只能修复 {@code ancestors} 错误，无法修复 {@code parent_id} 成环（遇到环会抛异常）。</li>
     * </ul>
     *
     * @throws BusinessException 非超管调用或数据成环时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selfHealing() {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "仅超级管理员可执行菜单拓扑修复");
        }
        // 仅查询必要字段，减少内存与网络开销（查询投影）
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                SysMenu::getId,
                SysMenu::getParentId,
                SysMenu::getPerms,
                SysMenu::getAncestors);
        List<SysMenu> all = this.list(queryWrapper);

        // 按父节点分组，便于递归审计
        Map<Long, List<SysMenu>> nodesMap = groupByParentId(all);
        List<SysMenu> corruptedNodes = new ArrayList<>();

        // 从根节点开始，期望路径为 "0"
        recursiveAudit(0L, "0", nodesMap, corruptedNodes, new HashSet<>());

        // 存在损坏节点才批量修正
        if (!corruptedNodes.isEmpty()) {
            this.updateBatchById(corruptedNodes);
        }
    }

    /**
     * 递归审计菜单树，收集 {@code ancestors} 与实际层级不一致的节点。
     *
     * <p><b>期望路径的推导：</b>根节点的子节点期望为 {@code "0"}；
     * 某节点（ID = X）的子节点期望为 {@code "父期望路径,X"}。
     *
     * @param pId          当前父节点 ID
     * @param expectedPath 子节点应有的 ancestors 值
     * @param map          按 parentId 分组的菜单集合
     * @param results      收集需要修正的节点
     * @param visiting     当前递归路径集合（环检测）
     */
    private void recursiveAudit(Long pId, String expectedPath,
                                Map<Long, List<SysMenu>> map, List<SysMenu> results,
                                Set<Long> visiting) {
        if (!visiting.add(pId)) {
            throw new BusinessException(500, "菜单树存在循环引用，节点ID=" + pId);
        }
        List<SysMenu> children = map.getOrDefault(pId, Collections.emptyList());
        try {
            for (SysMenu child : children) {
                if (!expectedPath.equals(child.getAncestors())) {
                    child.setAncestors(expectedPath);
                    results.add(child);
                }
                recursiveAudit(child.getId(), expectedPath + "," + child.getId(),
                        map, results, visiting);
            }
        } finally {
            visiting.remove(pId);
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 计算新节点的物化路径。
     *
     * <p>规则：根节点的子节点为 {@code "0"}；其余为“父节点 ancestors + ',' + 父节点 ID”。
     *
     * @param parentId 父节点 ID
     * @return 计算后的 ancestors 字符串
     */
    private String calculateAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysMenu parent = this.getById(parentId);
        return (parent != null) ? parent.getAncestors() + "," + parent.getId() : "0";
    }

    /**
     * 对外暴露的校验入口（供 Controller 独立校验使用）。
     *
     * @param menu 待校验菜单
     */
    @Override
    public void validateMenu(SysMenu menu) {
        menuValidator.validate(menu);
    }

    /**
     * 按父节点 ID 分组（{@code parentId} 为空视为根 0）。
     *
     * @param menus 菜单列表
     * @return key = parentId，value = 该父节点下的子节点列表
     */
    private Map<Long, List<SysMenu>> groupByParentId(List<SysMenu> menus) {
        return menus.stream().collect(Collectors.groupingBy(
                menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
    }

    /**
     * 菜单排序器：先按 {@code orderNum} 升序，再按 {@code id} 升序，保证输出稳定。
     *
     * @return 比较器
     */
    private Comparator<SysMenu> menuComparator() {
        return Comparator.comparing(
                        (SysMenu menu) -> menu.getOrderNum() == null ? 0 : menu.getOrderNum())
                .thenComparing(menu -> menu.getId() == null ? 0L : menu.getId());
    }

    /**
     * 权限保护校验：防止越权修改核心菜单或已授权菜单。
     *
     * <p><b>规则：</b>
     * <ol>
     *     <li>核心菜单：仅超管可修改，且禁止停用；</li>
     *     <li>非超管：禁止修改权限标识、禁止创建核心权限；</li>
     *     <li>已分配给角色的菜单：禁止修改 {@code perms} 或 {@code status}，需先解除授权。</li>
     * </ol>
     *
     * @param oldMenu 修改前的菜单（新增时为 null）
     * @param newMenu 修改后的菜单
     */
    private void checkPermissionOperation(SysMenu oldMenu, SysMenu newMenu) {
        if (oldMenu != null && isCoreMenu(oldMenu)) {
            if (!SecurityUtils.isAdmin()) {
                throw new BusinessException(403, "核心系统菜单仅允许超级管理员修改");
            }
            if ("1".equals(newMenu.getStatus())) {
                throw new BusinessException(409, "核心系统菜单不允许停用");
            }
        }
        if (!SecurityUtils.isAdmin()) {
            // 非超管不可修改已有菜单的权限标识
            if (oldMenu != null && !Objects.equals(oldMenu.getPerms(), newMenu.getPerms())) {
                throw new BusinessException(403, "非超级管理员禁止修改权限标识");
            }
            // 非超管不可创建核心系统权限
            if (oldMenu == null && isCorePermission(newMenu.getPerms())) {
                throw new BusinessException(403, "非超级管理员禁止创建核心系统权限");
            }
        }
        // 已授权菜单的 perms/status 变更会导致角色权限静默失效，必须先解绑
        if (oldMenu != null && (!Objects.equals(oldMenu.getPerms(), newMenu.getPerms())
                || !Objects.equals(oldMenu.getStatus(), newMenu.getStatus()))) {
            Long roleCount = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getMenuId, oldMenu.getId()));
            if (roleCount != null && roleCount > 0) {
                throw new BusinessException(409, "菜单已分配给角色，请先解除授权后再修改权限标识或状态");
            }
        }
    }

    /**
     * 判断是否为系统核心菜单。
     *
     * @param menu 菜单
     * @return 命中核心权限前缀返回 true
     */
    private boolean isCoreMenu(SysMenu menu) {
        return isCorePermission(menu.getPerms());
    }

    /**
     * 判断权限标识是否属于系统核心权限。
     *
     * <p>命中规则：等于 {@code "system"}，或等于/以 {@code "system:user"}、
     * {@code "system:role"}、{@code "system:menu"} 开头。
     *
     * @param permission 权限标识
     * @return 是否核心权限
     */
    private boolean isCorePermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        return "system".equals(permission) || CORE_PERMISSION_PREFIXES.stream().anyMatch(prefix ->
                permission.equals(prefix) || permission.startsWith(prefix + ":"));
    }

}

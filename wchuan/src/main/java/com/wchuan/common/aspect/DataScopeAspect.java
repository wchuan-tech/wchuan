package com.wchuan.common.aspect;

import com.wchuan.common.annotation.DataScope;
import com.wchuan.system.domain.entity.BaseEntity;
import com.wchuan.system.domain.entity.SysRole;
import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.domain.vo.LoginUser;
import org.springframework.util.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限切面处理类。
 *
 * <p><b>核心作用：</b>在标注了 {@link DataScope} 的方法执行前，根据当前登录用户的角色
 * （{@code sys_role.data_scope}）动态生成 SQL 过滤片段，并写入方法第一个参数
 * （{@link BaseEntity} 子类）的 {@code params.dataScope} 中，供 Mapper XML 通过
 * {@code ${params.dataScope}} 注入到查询 SQL。
 *
 * <p><b>五种数据范围：</b>
 * <ol>
 *     <li>{@code 1} - 全部数据：不附加条件；</li>
 *     <li>{@code 2} - 自定义：按角色-部门关联表 {@code sys_role_dept}；</li>
 *     <li>{@code 3} - 本部门：当前用户所在部门；</li>
 *     <li>{@code 4} - 本部门及以下：部门树（{@code find_in_set}）；</li>
 *     <li>{@code 5} - 仅本人：当前用户 ID。</li>
 * </ol>
 *
 * <p><b>拼装规则：</b>多角色条件以 {@code OR} 取并集；命中“全部数据”时清空并提前结束；
 * 无任何角色/范围时默认 {@code AND (1 = 0)} 拒绝，避免越权。
 *
 * @author WChuan
 * @see DataScope
 * @see BaseEntity
 */
@Aspect
@Component
public class DataScopeAspect {

    /** 全部数据权限 */
    public static final String DATA_SCOPE_ALL = "1";
    /** 自定义数据权限 */
    public static final String DATA_SCOPE_CUSTOM = "2";
    /** 本部门数据权限 */
    public static final String DATA_SCOPE_DEPT = "3";
    /** 本部门及以下数据权限 */
    public static final String DATA_SCOPE_DEPT_AND_CHILD = "4";
    /** 仅本人数据权限 */
    public static final String DATA_SCOPE_SELF = "5";

    /** 数据权限过滤关键字（存入 BaseEntity.params 的 key） */
    public static final String DATA_SCOPE = "dataScope";

    /**
     * 前置通知：在标注 {@link DataScope} 的方法执行前，注入数据权限 SQL 片段。
     *
     * <p><b>切点：</b>{@code @annotation(controllerDataScope)} —— 拦截任意标了
     * {@link DataScope} 的方法（不限定 Controller / Service / Mapper 层），
     * 并把方法上的注解实例绑定到参数 {@code controllerDataScope}。
     *
     * @param point               {@link JoinPoint}：被拦截方法调用的上下文对象，封装了
     *                            <ul>
     *                                <li>{@code getArgs()}：方法参数数组；</li>
     *                                <li>{@code getTarget()}：被代理的目标对象；</li>
     *                                <li>{@code getSignature()}：方法签名（方法名、声明类等）。</li>
     *                            </ul>
     *                            本切面通过 {@code point.getArgs()[0]} 取第一个参数，
     *                            该参数必须是 {@link BaseEntity} 子类，才能写入 {@code params}。
     * @param controllerDataScope 被拦截方法上的 {@link DataScope} 注解实例，
     *                            提供 {@code deptAlias}/{@code deptIdColumn}/{@code userAlias}，
     *                            用于拼装具体列名（如 {@code d.id}）。
     */
    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable {
        // 1. 先清空参数对象上可能残留的 dataScope，防止分页/二次查询时条件污染
        clearDataScope(point);
        // 2. 根据当前用户角色拼 SQL 片段，并写入参数对象的 params
        handleDataScope(point, controllerDataScope);
    }

    /**
     * 根据当前登录用户的角色，拼装数据权限 SQL 片段并写入参数对象。
     *
     * <p><b>处理流程：</b>
     * <ol>
     *     <li>从 {@code SecurityContext} 获取当前用户；未登录直接返回（不附加条件）；</li>
     *     <li>超级管理员（id=1 或 roleKey=admin）直接返回（不过滤）；</li>
     *     <li>遍历角色，按 {@code dataScope} 生成对应的 {@code OR} 片段；</li>
     *     <li>组装成 {@code " AND (...)"} 或默认拒绝 {@code " AND (1=0)"}；</li>
     *     <li>写入方法第一个参数的 {@code params.dataScope}。</li>
     * </ol>
     *
     * @param joinPoint           {@link JoinPoint}：被拦截方法的上下文；
     *                            通过 {@code getArgs()[0]} 取查询条件对象（{@link BaseEntity} 子类）。
     * @param controllerDataScope {@link DataScope} 注解实例，提供表别名等元数据。
     */
    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope) {
        // 1. 从 Security 上下文获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            return;
        }

        SysUser currentUser = loginUser.getUser();
        // 超级管理员不附加数据过滤条件。
        if (currentUser != null && (Long.valueOf(1L).equals(currentUser.getId())
                || currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "admin".equals(role.getRoleKey())))) {
            return;
        }

        if (currentUser == null) {
            return;
        }

        // 2. 根据角色的 dataScope 动态拼接 SQL 过滤语句
        StringBuilder sqlString = new StringBuilder();
        List<String> conditions = new ArrayList<>();

        List<SysRole> roles = currentUser.getRoles() == null ? List.of() : currentUser.getRoles();
        for (SysRole role : roles) {
            String dataScope = role.getDataScope();

            // 规则 1：全部数据权限 —— 清空已拼内容并提前结束（最宽松优先）
            if (DATA_SCOPE_ALL.equals(dataScope)) {
                sqlString = new StringBuilder();
                conditions.add(dataScope);
                break;
            }
            // 规则 2：自定义数据权限（按角色-部门关联表 sys_role_dept）
            else if (DATA_SCOPE_CUSTOM.equals(dataScope)) {
                sqlString.append(String.format(
                        " OR %s.%s IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = %d ) ",
                        controllerDataScope.deptAlias(), controllerDataScope.deptIdColumn(), role.getId()));
            }
            // 规则 3：本部门数据权限
            else if (DATA_SCOPE_DEPT.equals(dataScope)) {
                sqlString.append(String.format(
                        " OR %s.%s = %d ",
                        controllerDataScope.deptAlias(), controllerDataScope.deptIdColumn(), currentUser.getDeptId()));
            }
            // 规则 4：本部门及以下数据权限（部门树：自身 + 所有后代）
            else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
                sqlString.append(String.format(
                        " OR %s.%s IN ( SELECT id FROM sys_dept WHERE id = %d OR find_in_set(%d, ancestors) ) ",
                        controllerDataScope.deptAlias(), controllerDataScope.deptIdColumn(),
                        currentUser.getDeptId(), currentUser.getDeptId()));
            }
            // 规则 5：仅本人数据权限
            else if (DATA_SCOPE_SELF.equals(dataScope)) {
                if (StringUtils.hasText(controllerDataScope.userAlias())) {
                    sqlString.append(String.format(" OR %s.id = %d ", controllerDataScope.userAlias(), currentUser.getId()));
                } else {
                    sqlString.append(String.format(" OR %s.%s = 0 ",
                            controllerDataScope.deptAlias(), controllerDataScope.deptIdColumn()));
                }
            }

            conditions.add(dataScope);
        }

        // 3. 组装最终片段：
        //    · 有内容 → " AND (" + 去掉首个 " OR " 后的片段 + ")"
        //    · 空 + 命中“全部” → 空串（不过滤）
        //    · 空 + 无“全部” → " AND (1 = 0)"（默认拒绝）
        // 注意：未配置数据范围时默认拒绝，避免无角色账号意外获得全部数据权限。
        String dataScopeSql = StringUtils.hasText(sqlString)
                ? " AND (" + sqlString.substring(4) + ")"
                : conditions.contains(DATA_SCOPE_ALL) ? "" : " AND (1 = 0)";

        // 4. 取被拦截方法的第一个参数（查询条件对象），把 SQL 片段写入其 params
        Object params = joinPoint.getArgs()[0];
        if (params instanceof BaseEntity baseEntity) {
            baseEntity.getParams().put(DATA_SCOPE, dataScopeSql);
        }


    }

    /**
     * 清理参数对象上残留的 {@code params.dataScope}，防止二次查询/分页时数据污染。
     *
     * @param joinPoint {@link JoinPoint}：被拦截方法的上下文；
     *                  取第一个参数（{@link BaseEntity} 子类），将其 {@code params.dataScope} 置空。
     */
    private void clearDataScope(final JoinPoint joinPoint) {
        Object params = joinPoint.getArgs()[0];
        if (params != null && params instanceof BaseEntity baseEntity) {
            baseEntity.getParams().put(DATA_SCOPE, "");
        }
    }

}

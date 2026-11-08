# wchuan · 企业客户资源流转平台

> 基于 **Spring Boot 3 + Spring Security 6 + JWT + Redis + MyBatis-Plus + Vue 3** 的前后端分离企业客户协同管理系统。
>
> 业务是「客户公海 / 私海流转」，技术内核是「**RBAC 功能权限 + 数据权限**」的分层隔离设计。

---

## 一、项目简介

企业客户资源流转平台面向**多部门销售团队**，解决两个核心问题：

1. **客户资源流转**：客户从公海领取、跟进、超期自动回收，形成完整的生命周期闭环；
2. **数据权限隔离**：不同部门只能查看本部门（及下级部门）的客户，避免跨部门越权。

项目采用**前后端分离**架构：后端提供 REST API，前端 Vue 3 + Element Plus 负责页面交互与权限展示。

> 一句话定位：**业务是客户流转，技术内核是权限体系**——用客户流转业务来验证「功能权限 + 数据权限」的分层设计。

---

## 二、功能特性

### 2.1 客户管理（CRM）

- **公海客户池**：所有销售可见，可「认领 / 抢单」；
- **私海客户**：归属销售私有，支持跟进、移交、退回公海；
- **超期自动回收**：定时任务每天凌晨 2 点，把「超过 7 天未跟进且未锁定」的私海客户回收至公海；
- **并发控制**：条件更新（乐观 / 状态机 CAS）+ 行锁（悲观）混合方案，防止「一客多主」与「超领」。

### 2.2 权限体系

- **认证（Authentication）**：Spring Security + JWT + Redis，支持主动踢人下线、滑动续期；
- **功能权限（接口级）**：`@PreAuthorize("@ss.hasPerm('...')")` + SpEL，细粒度到接口；
- **数据权限（数据级）**：`@DataScope` + Spring AOP 动态注入 SQL，支持 5 种数据范围。

### 2.3 系统管理

- 用户、角色、部门、菜单的 RBAC 管理；
- 菜单树：邻接表（`parent_id`）+ 物化路径（`ancestors`）双存储；
- 菜单拓扑自愈：修复历史脏数据导致的 `ancestors` 错乱；
- 操作日志：`@Log` + AOP + `@Async` 异步落库，`TaskDecorator` 传递安全上下文。

---

## 三、技术栈

| 分类 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3、Spring Security 6 |
| 认证鉴权 | JWT（jjwt）、Redis、SpEL |
| 持久层 | MyBatis-Plus、MySQL |
| 切面 | Spring AOP（自定义注解 `@DataScope`、`@Log`） |
| 异步 | `@EnableAsync` + `ThreadPoolTaskExecutor` + `TaskDecorator` |
| 前端 | Vue 3、TypeScript、Element Plus、Vue Router、Axios、Vite |

---

## 四、系统架构

```text
┌──────────────────────────────────────────────┐
│  前端 Vue 3 + Element Plus                    │
│  菜单权限 / 按钮权限 / 路由守卫（体验层）        │
└───────────────────┬──────────────────────────┘
                    │ HTTP + JWT
┌───────────────────▼──────────────────────────┐
│  Spring Security 过滤器链                     │
│  JwtAuthenticationTokenFilter（认证）         │
├──────────────────────────────────────────────┤
│  Controller                                  │
│  @PreAuthorize + SpEL（功能权限 / 接口级）     │
├──────────────────────────────────────────────┤
│  Service                                     │
│  @DataScope + AOP（数据权限 / 数据级）         │
│  并发控制（条件更新 + 行锁）                   │
├──────────────────────────────────────────────┤
│  Mapper（MyBatis-Plus）                       │
│  ${params.dataScope} 动态 SQL 注入             │
├──────────────────────────────────────────────┤
│  MySQL：sys_user / sys_role / sys_dept /      │
│         sys_menu / crm_customer / sys_oper_log│
└──────────────────────────────────────────────┘
                    │
┌───────────────────▼──────────────────────────┐
│  Redis：login:{userId} → LoginUser（会话）     │
└──────────────────────────────────────────────┘
```

---

## 五、核心设计

### 5.1 认证（JWT + Redis 双存储）

| 存储 | 内容 | 作用 |
|---|---|---|
| **JWT** | `userId`（subject）+ 过期时间，HS256 签名 | **防篡改**（签名不是加密，payload 是明文） |
| **Redis** | `login:{userId}` → `LoginUser`（用户 + 权限） | **真正的会话中心**，支持踢人、权限刷新、滑动续期 |

**登录流程**：
`账号密码 → AuthenticationManager → DaoAuthenticationProvider → UserDetailsService.loadUserByUsername → LoginUser → 存 Redis + 签发 JWT`

**请求流程**：
`JWT → JwtAuthenticationTokenFilter → 解析验签 → 查 Redis 恢复 LoginUser → 塞 SecurityContext`

### 5.2 功能权限（接口级）

```java
@PreAuthorize("@ss.hasPerm('crm:customer:add')")
public ResponseResult<?> add(...) { ... }
```

- `@ss` 是自定义 Bean（`PermissionAuthority`），通过 SpEL 的 `BeanResolver` 解析；
- 返回 `false` 抛 `AccessDeniedException` → `AccessDeniedHandler` → **403**；
- 未登录 → `AuthenticationEntryPoint` → **401**。

### 5.3 数据权限（数据级）

```java
@DataScope(deptAlias = "d", userAlias = "u")
IPage<Customer> selectPrivateCustomerPage(...);
```

**五种数据范围**：

| 值 | 含义 | 过滤依据 |
|---|---|---|
| 1 | 全部数据 | 不过滤 |
| 2 | 自定义 | `sys_role_dept` 关联部门 |
| 3 | 本部门 | 当前用户部门 |
| 4 | 本部门及以下 | 部门树（`find_in_set(?, ancestors)`） |
| 5 | 仅本人 | 当前用户 ID |

**实现链路**：
`@DataScope 注解 → DataScopeAspect @Before 拦截 → 拼 SQL 片段 → 塞入 BaseEntity.params → Mapper XML ${params.dataScope} 注入`

### 5.4 客户流转与并发控制

| 场景 | 方案 |
|---|---|
| 不同销售抢同一个客户 | **条件更新**（`WHERE pool_type=0 AND owner_user_id IS NULL`），更新 0 行即失败 |
| 同一销售并发领取导致超限 | **`SELECT ... FOR UPDATE` 锁用户行**，串行化「统计-判断-领取」 |
| 超期自动回收 | 定时任务 + `FOR UPDATE` 锁定候选行 + 条件更新，保证幂等 |

### 5.5 异步日志与上下文传递

- `@Async` 日志落库在异步线程，`SecurityContext` 基于 `ThreadLocal`，异步线程取不到；
- 使用 `TaskDecorator`：**提交时**捕获上下文 → **子线程执行前**恢复 → **`finally` 清理**；
- 解决 `createBy` / `updateBy` 自动填充被静默写成空串的问题。

---

## 六、权限标识（部分）

| 模块 | 权限标识 |
|---|---|
| 用户 | `system:user:list` / `system:user:add` / `system:user:edit` / `system:user:remove` |
| 角色 | `system:role:list` / `system:role:add` / `system:role:edit` / `system:role:remove` |
| 部门 | `system:dept:list` / `system:dept:add` / `system:dept:edit` / `system:dept:remove` |
| 菜单 | `system:menu:list` / `system:menu:add` / `system:menu:edit` / `system:menu:remove` |
| 客户 | `crm:customer:list` / `crm:customer:add` / `crm:customer:assign` / `crm:customer:transfer` / `crm:customer:return` / `crm:customer:followup:add` |

---

## 七、目录结构

```text
wchuan
├── src/main/java/com/wchuan
│   ├── common
│   │   ├── annotation        # @Log、@DataScope
│   │   ├── aspect            # LogAspect、DataScopeAspect
│   │   ├── config            # SecurityConfig、AsyncConfig、SecurityContextTaskDecoratorConfig
│   │   ├── constants         # Constants、MenuConstants
│   │   ├── enums             # RedisKeyEnum、BusinessTypeEnum
│   │   ├── exception         # BusinessException、GlobalExceptionHandler
│   │   └── utils             # JwtUtil、RedisCache、SecurityUtils
│   ├── security
│   │   ├── expression        # PermissionAuthority（@ss）
│   │   ├── filter            # JwtAuthenticationTokenFilter
│   │   └── handler           # MyMetaObjectHandler、404/401 处理器
│   ├── system
│   │   ├── controller        # SysUser / SysRole / SysDept / SysMenu / Login ...
│   │   ├── service/impl      # TokenServiceImpl、LoginServiceImpl、MenuServiceImpl ...
│   │   └── domain            # LoginUser、SysUser、SysRole、SysMenu、BaseEntity ...
│   └── crm
│       ├── controller        # CrmCustomerController
│       ├── service/impl      # CrmCustomerServiceImpl
│       ├── mapper            # CustomerMapper
│       └── task              # CrmCustomerAutoPoolTask（超期回收）
├── src/main/resources
│   ├── application.yml
│   └── mapper                # MyBatis XML
├── sql                       # 建表 / 初始化脚本
└── wchuan-ui                 # Vue 3 前端
    └── src
        ├── router/index.ts   # 路由守卫
        ├── components        # SidebarItem（菜单权限）
        └── views             # 登录、CRM、系统管理
```

---

## 八、快速开始

### 8.1 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+（前端）

### 8.2 初始化数据库

```bash
# 创建数据库
CREATE DATABASE sg_security DEFAULT CHARACTER SET utf8mb4;

# 执行初始化脚本（按需）
# wchuan/sql/*.sql
```

> 数据库连接配置见 `wchuan/src/main/resources/application.yml`：
> `jdbc:mysql://localhost:3306/sg_security`，账号 `root/root`。

### 8.3 启动后端

```bash
cd wchuan
mvn spring-boot:run
```

默认端口 `8080`（未配置 `server.port` 时）。

### 8.4 启动前端

```bash
cd wchuan/wchuan-ui
npm install
npm run dev
```

默认地址 `http://localhost:5173`（CORS 已放行该地址）。

---

## 九、已知边界与改进方向

| 项 | 现状 | 改进方向 |
|---|---|---|
| JWT 与 Redis 生命周期 | JWT 固定 24h，Redis 30min 滑动，二者未统一 | 引入 refreshToken，或 JWT 只做身份凭证、会话全交给 Redis |
| Redis Key 粒度 | `login:{userId}`，仅支持单端登录 | 改为 `login:{uuid}`，支持多端会话与按设备踢人 |
| JWT 密钥 | 硬编码在 `JwtUtil` | 放环境变量 / 配置中心 / 密钥管理服务 |
| 超管判定 | `SecurityUtils.isAdmin()` 只判 `id==1` | 统一为 `id==1 或 roleKey=admin` |
| 数据权限兜底 | 依赖 `@DataScope` 注解，漏标即越权 | 增加「未标注解」的静态检查或默认拒绝策略 |
| 前端路由守卫 | 只校验 token，不校验权限 | 动态路由 `addRoute` + `meta.permission` |
| 权限缓存 | 权限存 Redis，变更后旧会话不刷新 | 权限变更时主动清理 Redis 会话 |

---

## 十、相关文档

- [项目知识文档](./项目知识文档.md) —— 架构、认证、授权、并发、踩坑记录与面试背题本

---

## 十一、License

本项目仅用于学习与个人实践。

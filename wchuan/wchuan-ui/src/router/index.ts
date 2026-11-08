import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Login from '../views/Login.vue'
import UserProfile from '../views/UserProfile.vue'
import Hello from '../views/Hello.vue'
import Index from '../views/Index.vue'


const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        redirect: '/index' ,
        children:[
            {
            path: 'system/notice',
            name: 'SystemNotice',
            // 🔑 重点：指向新创建的 system/notice/index.vue 路径！
            component: () => import('@/views/system/notice/index.vue'),
            meta: { title: '通知公告' }
            },
            // 兼容短路径 /notice 跳转
            {
                path: 'notice',
                redirect: '/system/notice'
            },
            {
                path: 'system/menu',
                name: 'SystemMenu',
                component: () => import('@/views/system/menu/index.vue'), // 👈 指向菜单管理
                meta: { title: '菜单管理' }
            },
            {
                path: 'menu',
                redirect: '/system/menu'
            },
            {
                path: 'system/dept',
                name: 'SystemDept',
                component: () => import('@/views/system/dept/index.vue'), // 👈 指向菜单管理
                meta: { title: '部门管理' }
            },
            {
                path: 'dept',
                redirect: '/system/dept'
            },
            {
                path: 'system/user',
                name: 'SystemUser',
                component: () => import('@/views/system/user/index.vue'), // 👈 指向菜单管理
                meta: { title: '部门管理' }
            },
            {
                path: 'user',
                redirect: '/system/user'
            },
            {
                path: 'system/role',
                name: 'SystemRole',
                component: () => import('@/views/system/role/index.vue'), // 👈 指向菜单管理
                meta: { title: '部门管理' }
            },
            {
                path: 'role',
                redirect: '/system/role'
            },
            {
                path: 'crm/public-pool',
                name: 'PublicPool',
                component: () => import('@/views/crm/public-pool/index.vue'),
                meta: { title: '公海客户池' }
            },
            {
                path: 'public-pool',
                redirect: '/crm/public-pool'
            },
            {
                path: 'crm/private-pool',
                name: 'PrivatePool',
                component: () => import('@/views/crm/private-pool/index.vue'),
                meta: { title: '私海客户' }
            },
            {
                path: 'private-pool',
                redirect: '/crm/private-pool'
            },
            ]
    },
    { path: '/login', component: Login },
    { path: '/index', component: Index },
    { path: '/profile', component: UserProfile },
    { path: '/hello', component: Hello },
    { path: '/notice', component: Index },
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

/**
 * 全局前置守卫逻辑重构
 * to: 即将要进入的目标 路由对象
 * _from: 当前导航正要离开的路由 (加下划线消除未使用报错)
 * next: 调用该方法来 resolve 这个钩子
 */
router.beforeEach((to, _from, next) => {
    const token = localStorage.getItem('token');

    // 1. 处理访问登录页的情况
    if (to.path === '/login') {
        if (token) {
            // 如果已登录还想去登录页，直接拦截并送回首页
            next('/index');
        } else {
            // 没登录，正常进登录页
            next();
        }
        return;
    }

    // 2. 处理访问受保护页面（非登录页）的情况
    if (!token) {
        // 核心亮点：如果在简历里，可以说这是“强制身份认证拦截”
        // 也可以不加 alert，因为 axios 拦截器通常会处理 401 报错
        next('/login');
    } else {
        // 已登录，且访问的是其他页面，放行
        next();
    }
});

export default router;

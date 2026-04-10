<template>
  <el-container class="main-layout">
    <!-- 侧边栏保持不变 -->
    <el-aside width="240px" class="aside-menu">
      <div class="logo-box">
        <el-icon color="#4f46e5" :size="24"><Platform /></el-icon>
        <span>WCHUAN</span>
      </div>
      <el-menu default-active="/index" background-color="#0f172a" text-color="#94a3b8" active-text-color="#ffffff" router>
        <el-menu-item index="/index"><el-icon><Grid /></el-icon><span>控制台概览</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><User /></el-icon><span>个人中心</span></el-menu-item>
        <el-menu-item index="/hello"><el-icon><Compass /></el-icon><span>权限测试中心</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="system-time-wrapper">
            <!-- 自定义动态时钟 -->
            <div class="dynamic-clock">
              <div class="hour-hand" :style="hourRotate"></div>
              <div class="minute-hand" :style="minuteRotate"></div>
            </div>
            <span class="system-time">{{ currentTime }}</span>
          </div>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-dropdown">
              <el-avatar :size="28" src="https://api.dicebear.com/9.x/shapes/svg?seed=Felix,shape2=ellipse" />
              <!-- 顶部也同步显示真实用户名 -->
              <span class="username-text">{{ nickname || 'WCHUAN' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">基本资料</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout" style="color: #f43f5e">退出系统</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <!-- 欢迎区域：现在使用动态用户名 -->
        <div class="welcome-hero">
          <div class="hero-main-content">
            <!-- 文字区 -->
            <div class="hero-text" v-loading="loading">
              <h1 class="greeting-title">
                <span class="greeting-word">{{ greeting }}</span>
                <span class="comma">,</span>
                <span class="gradient-text">{{ nickname + '...' || 'Loading...' }}</span>
              </h1>
              <p class="hero-subtext">
                欢迎回到系统管理后台，当前您的权限已通过 <span class="status-link">JWT 无状态认证</span> 校验。
              </p>
            </div>

            <!-- 标签区：移到文字下方，增加层次感 -->
            <div class="tech-stack-tags">
              <el-tag class="tech-tag" round effect="plain">Spring Boot 3</el-tag>
              <el-tag class="tech-tag" round effect="plain">SpringSecurity</el-tag>
              <el-tag class="tech-tag" round effect="plain">Vue 3</el-tag>
              <el-tag class="tech-tag" round effect="plain">TS</el-tag>
            </div>
          </div>
        </div>

        <el-row :gutter="24">
          <!-- 系统日志流：占据更多空间 -->
          <el-col :span="18">
            <el-card class="log-card">
              <template #header>
                <div style="display: flex; align-items: center; gap: 8px">
                  <el-icon color="#4f46e5"><Operation /></el-icon>
                  <span style="font-weight: 700">System Activity Stream</span>
                </div>
              </template>
              <div class="terminal-box">
                <div v-for="(log, index) in systemLogs" :key="index" class="log-line">
                  <span class="log-timestamp">[{{ log.time }}]</span>
                  <span class="log-level" :class="log.type">{{ log.level }}</span>
                  <span class="log-content">{{ log.content }}</span>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 右侧仅保留服务健康状况，让布局更清爽 -->
          <el-col :span="6">
            <el-card>
              <template #header><span style="font-size: 14px; font-weight: 700">服务节点状态</span></template>
              <div class="status-item">
                <span>Auth-Service</span>
                <el-progress :percentage="100" status="success" stroke-width="8" />
              </div>
              <div class="status-item" style="margin-top: 20px">
                <span>Redis-Node</span>
                <el-progress :percentage="92" stroke-width="8" />
              </div>
              <div class="status-item" style="margin-top: 20px">
                <span>Database-Pool</span>
                <el-progress :percentage="100" status="success" stroke-width="8" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Platform, Grid, User, Compass, ArrowDown, Operation} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { type Result, type UserInfo } from '../api/types'

const router = useRouter()
const nickname = ref('')
const loading = ref(false)

// 1. 获取真实的用户信息
const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await request.get<any, Result<UserInfo>>('/user/getInfo')
    if (res.code === 200) {
      nickname.value = res.data.nickName || res.data.userName
    }
  } catch (err) {
    console.error('获取用户信息失败', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})

// 时钟与欢迎语逻辑
const currentTime = ref(new Date().toLocaleTimeString())
setInterval(() => {
  currentTime.value = new Date().toLocaleTimeString()
}, 1000)

const greeting = computed(() => {
  const hour = new Date().getHours()

  if (hour >= 0 && hour < 6) {
    return 'Good Dawn'        // 凌晨 (0:00 - 5:59)
  } else if (hour >= 6 && hour < 12) {
    return 'Good Morning'     // 早上 (6:00 - 11:59)
  } else if (hour >= 12 && hour < 14) {
    return 'Good Noon'        // 中午 (12:00 - 13:59)
  } else if (hour >= 14 && hour < 18) {
    return 'Good Afternoon'   // 下午 (14:00 - 17:59)
  } else {
    return 'Good Evening'     // 晚上 (18:00 - 23:59)
  }
})

// 1. 获取当前时间的数值
const hour = computed(() => new Date().getHours())
const minute = computed(() => new Date().getMinutes())

// 2. 计算时针旋转角度：每小时 30度 (360/12)，加上分钟带来的微小偏移
const hourRotate = computed(() => {
  const deg = (hour.value % 12) * 30 + minute.value * 0.5
  return { transform: `rotate(${deg}deg)` }
})

// 3. 计算分针旋转角度：每分钟 6度 (360/60)
const minuteRotate = computed(() => {
  const deg = minute.value * 6
  return { transform: `rotate(${deg}deg)` }
})

// 模拟日志
const systemLogs = ref([
  { time: '17:10:02', level: 'INFO', type: 'info', content: 'Spring Security Filter Chain initialized.' },
  { time: '17:11:45', level: 'SUCCESS', type: 'success', content: 'JWT Token parsed successfully.' },
  { time: '17:15:30', level: 'DB', type: 'info', content: 'Database connection pool optimized (HikariCP).' },
  { time: '17:20:10', level: 'AUTH', type: 'warning', content: 'CORS policy applied: Allow origin http://localhost:5173' },
  { time: '17:25:05', level: 'INFO', type: 'info', content: 'Dynamic RBAC role mapping completed.' },
])

const handleLogout = async () => {
  // 1. 先向后端发起登出请求（趁着 localStorage 里还有 token）
  // 注意：必须先发请求，再删 token，否则请求头里就没有 token 了
  try {
    // 关键点：加上 <any, Result<any>>
    // 第一个参数是后端方法的请求参数类型（登出通常为any或null），第二个是后端返回的 Result 结构
    const res = await request.post<any, Result>('/user/logout')

    // 现在 res.code 就能被正确识别了
    if (res.code === 200) {
      localStorage.removeItem('token')
      ElMessage.success('已安全退出')
      router.replace('/login')
    }
  } catch (err) {
    // 如果请求失败（比如Token已失效），为了用户体验，也执行前端退出
    localStorage.removeItem('token')
    router.replace('/login')
  }
}
</script>

<style scoped>
@import '@/styles/index.css';
</style>
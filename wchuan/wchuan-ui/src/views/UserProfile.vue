<template>
  <div class="index-container" v-loading="loading">
    <el-card v-if="userInfo" class="index-card glass-container" shadow="always">
      <!-- 顶部 Mac 风格装饰 -->
      <div class="header-decoration">
        <div class="dot red"></div>
        <div class="dot yellow"></div>
        <div class="dot green"></div>
      </div>

      <div class="profile-content">
        <!-- 顶部：头像与名称 -->
        <div class="user-header">
          <el-avatar :size="80" src="https://api.dicebear.com/9.x/shapes/svg?seed=Felix,shape2=ellipse" class="user-avatar-main" />
          <h2 class="user-name">{{ userInfo.nickName || userInfo.userName }}</h2>
          <div class="status-tags">
            <el-tag type="success" effect="light" round size="small">● 在线</el-tag>
            <el-tag type="warning" effect="plain" round size="small" style="margin-left: 8px">认证用户</el-tag>
          </div>
        </div>

        <el-divider>
          <span class="divider-text">系统认证信息</span>
        </el-divider>

        <!-- 中部：滚动信息列表 -->
        <div class="info-scroll-wrapper">
          <el-scrollbar max-height="300px">

            <el-descriptions :column="1" border class="custom-descriptions">
              <el-descriptions-item>
                <template #label>
                  <div class="cell-item"><el-icon><User /></el-icon> 账号身份</div>
                </template>
                <el-tag v-if="isAdmin" type="danger" effect="dark" size="small">超级管理员</el-tag>
                <el-tag v-else size="small">普通用户</el-tag>
              </el-descriptions-item>

              <el-descriptions-item>
                <template #label>
                  <div class="cell-item"><el-icon><Lock /></el-icon> 认证方式</div>
                </template>
                <span :class="isAdmin ? 'admin-auth-text' : 'auth-text'">
                  {{ isAdmin ? '全权限 ROOT 授权' : 'JWT 无状态加密' }}
                </span>
              </el-descriptions-item>

              <el-descriptions-item>
                <template #label>
                  <div class="cell-item"><el-icon><Connection /></el-icon> 权限标识</div>
                </template>
                <div class="perm-tags">
                  <template v-if="isAdmin">
                    <el-tag type="warning" size="small">ALL_PRIVILEGES</el-tag>
                  </template>
                  <template v-else>
                    <code v-for="perm in userInfo?.permissions" :key="perm">{{ perm }}</code>
                  </template>
                </div>
              </el-descriptions-item>

              <el-descriptions-item>
                <template #label>
                  <div class="cell-item"><el-icon><OfficeBuilding /></el-icon> 所属租户</div>
                </template>
                <el-tag type="info" effect="plain">{{ userInfo.tenantName || '默认租户' }}</el-tag>
              </el-descriptions-item>

            </el-descriptions>
          </el-scrollbar>
        </div>

        <!-- 底部：固定操作按钮 -->
        <div class="footer-actions">
          <el-button type="primary" size="large" @click="goToHello" class="main-btn">
            <el-icon><Monitor /></el-icon> 进入权限测试中心
          </el-button>

          <el-button size="large" @click="router.push('/index')" class="back-btn">
            <el-icon><House /></el-icon> 返回工作台
          </el-button>

          <el-button type="danger" link @click="handleLogout" class="logout-link">
            安全退出系统
          </el-button>

        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Connection, Monitor, House, OfficeBuilding } from '@element-plus/icons-vue'
import request from '../utils/request'
import { type Result, type UserInfo } from '../api/types'

const router = useRouter()
const userInfo = ref<UserInfo | null>(null)
const loading = ref(true)

const isAdmin = computed(() => {
  if (!userInfo.value) return false
  return userInfo.value.id === 1 || userInfo.value.permissions?.includes('*')
})

const getUserData = async () => {
  try {
    loading.value = true
    const res = await request.get<any, Result<UserInfo>>('/user/getInfo')
    if (res.code === 200)
      userInfo.value = res.data
  } catch (err) {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => getUserData())
const goToHello = () => router.push('/hello')

const handleLogout = async () => {
  try {
    const res = await request.post<any, Result>('/user/logout')
    if (res.code === 200) {
      localStorage.removeItem('token')
      ElMessage.success('已安全退出')
      router.replace('/login')
    }
  } catch (err) {
    localStorage.removeItem('token')
    router.replace('/login')
  }
}
</script>

<style scoped>
@import '@/styles/userProfile.css';
</style>
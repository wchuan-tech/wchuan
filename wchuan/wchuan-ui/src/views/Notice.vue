<template>
  <div class="notice-container" v-loading="pageLoading">
    <el-card class="notice-main-card glass-container">
      <div class="header-decoration">
        <div class="dot red"></div><div class="dot yellow"></div><div class="dot green"></div>
      </div>

      <div class="notice-layout">
        <!-- 左侧公告列表 -->
        <div class="notice-list-side">

          <div class="side-header">
            <h2><el-icon><Notification /></el-icon> 公告中心</h2>
            <div class="header-btns">
              <!-- 删除按钮：有权限才显示 -->
              <el-button
                  v-if="canRemove"
                  type="danger"
                  size="small"
                  @click="enterDeleteMode"
              >
                <el-icon><Delete /></el-icon> 删除
              </el-button>

              <el-button @click="fetchNotices" circle>
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>

          <el-scrollbar max-height="600px">
            <div
                v-for="item in noticeList"
                :key="item.id"
                class="notice-item-box"
                :class="{ selectable: isDeleteMode, selected: selectedIds.includes(item.id) }"
                @click="isDeleteMode && toggleSelect(item.id)"
            >
              <!-- 选择模式才显示勾选圆圈 -->
              <div class="check-circle" v-if="isDeleteMode">
                <div class="inner" v-if="selectedIds.includes(item.id)"></div>
              </div>

              <div class="item-top">
                <el-tag :type="item.type === '1' ? 'danger' : 'success'" size="small" effect="dark">
                  {{ item.type === '1' ? '全平台' : '租户内' }}
                </el-tag>
                <span class="notice-time">{{ item.createTime }}</span>
              </div>

              <h3 class="notice-title">{{ item.title }}</h3>
              <p class="notice-content">{{ item.content }}</p>

              <div class="item-footer">
                <span>发布人: {{ item.createBy }}</span>
                <span class="tenant-tag" v-if="item.type === '0'">
                  <el-icon><OfficeBuilding /></el-icon>
                  所属租户：{{ item.tenantName }}
                </span>
              </div>
            </div>

            <el-empty v-if="noticeList.length === 0" description="暂无相关公告" />
          </el-scrollbar>

          <!-- 删除模式底部工具栏 -->
          <div class="delete-bar" v-if="isDeleteMode">
            <span>已选中 {{ selectedIds.length }} 条</span>
            <div>
              <el-button size="small" @click="cancelDeleteMode">取消</el-button>
              <el-button type="primary" size="small" @click="handleBatchDelete" :disabled="selectedIds.length === 0">
                确认删除
              </el-button>
            </div>
          </div>
        </div>

        <!-- 右侧发布 -->
        <div class="notice-manage-side" v-if="canManage">
          <div class="manage-header">
            <h3><el-icon><EditPen /></el-icon> 发布新公告</h3>
          </div>

          <el-form :model="noticeForm" label-position="top" class="custom-form">
            <el-form-item label="公告标题">
              <el-input v-model="noticeForm.title" placeholder="输入标题..." />
            </el-form-item>

            <el-form-item label="公告类型">
              <el-radio-group v-model="noticeForm.type">
                <el-radio label="0">租户内部</el-radio>
                <el-radio label="1" v-if="isSuperAdmin">全平台</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="详细内容">
              <el-input v-model="noticeForm.content" type="textarea" :rows="6" placeholder="输入正文内容..." />
            </el-form-item>

            <el-button type="primary" class="publish-btn" @click="handlePublish" :loading="publishLoading">
              立即发布
            </el-button>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Notification, Refresh, OfficeBuilding, EditPen, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { type Result, type SysNotice, type UserInfo } from '../api/types'

const noticeList = ref<SysNotice[]>([])
const pageLoading = ref(false)
const publishLoading = ref(false)
const userInfo = ref<UserInfo | null>(null)

// 删除模式
const isDeleteMode = ref(false)
const selectedIds = ref<number[]>([])

const noticeForm = ref({ title: '', content: '', type: '0' })

// ====================== 权限判断 ======================
const isSuperAdmin = computed(() => userInfo.value?.id === 1)
const canManage = computed(() => isSuperAdmin.value || userInfo.value?.permissions.includes('system:notice:add'))
const canRemove = computed(() => isSuperAdmin.value || userInfo.value?.permissions.includes('system:notice:remove'))
// ======================================================

// 进入删除模式
const enterDeleteMode = () => {
  if (!canRemove.value) {
    ElMessage.warning('你没有删除权限')
    return
  }
  isDeleteMode.value = true
  selectedIds.value = []
}

// 取消删除模式
const cancelDeleteMode = () => {
  isDeleteMode.value = false
  selectedIds.value = []
}

// 切换选择
const toggleSelect = (id: number) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

// 获取列表
const fetchNotices = async () => {
  pageLoading.value = true
  try {
    const res = await request.get<any, Result<SysNotice[]>>('/dev/notice/list')
    if (res.code === 200) noticeList.value = res.data
  } finally {
    pageLoading.value = false
  }
}

// 获取用户信息
const fetchUserInfo = async () => {
  const res = await request.get<any, Result<UserInfo>>('/user/getInfo')
  if (res.code === 200) userInfo.value = res.data
}

// 发布
const handlePublish = async () => {
  if (!noticeForm.value.title || !noticeForm.value.content) {
    return ElMessage.warning('请填写完整信息')
  }
  publishLoading.value = true
  try {
    const res = await request.post<any, Result>('/dev/notice/add', noticeForm.value)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      noticeForm.value = { title: '', content: '', type: '0' }
      await fetchNotices()
    }
  } finally {
    publishLoading.value = false
  }
}

// 删除
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return ElMessage.warning('请选择公告')

  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).catch(() => {
    throw new Error('cancel')
  })

  try {
    const res = await request.delete<any, Result>('/dev/notice/batch', {
      data: { ids: selectedIds.value }
    })
    if (res.code === 200) {
      ElMessage.success('删除成功')
      cancelDeleteMode()
      await fetchNotices()
    }
  } catch (e: any) {
    if (e.message !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  fetchUserInfo()
  fetchNotices()
})
</script>

<style scoped>
@import '@/styles/notice.css';
</style>
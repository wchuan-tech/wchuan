<template>
  <div class="hello-wrapper">
    <div class="glass-container">
      <!-- 顶部科技感装饰 -->
      <div class="header-decoration">
        <div class="dot red"></div>
        <div class="dot yellow"></div>
        <div class="dot green"></div>
      </div>

      <div class="hello-content">
        <!-- 动态图标区 -->
        <div class="status-icon-box">
          <el-icon class="shield-icon" :size="60"><Checked /></el-icon>
          <div class="pulse-ring"></div>
        </div>

        <h1 class="page-title">ACCESS_AUTHENTICATION</h1>

        <!-- 结果展示区 -->
        <div class="result-card" v-loading="loading">
          <div class="response-text">
            <transition name="fade-slide" mode="out-in">
              <div :key="quoteContent" class="quote-wrapper">
                <div class="quote-main">{{ quoteContent }}</div>
                <div v-if="quoteAuthor" class="quote-author">
                  <span class="dash">—</span> {{ quoteAuthor }}
                </div>
              </div>
            </transition>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-group">
          <el-button type="primary" class="elegant-btn" @click="fetchHello">
            <el-icon><Refresh /></el-icon> 发起安全通信
          </el-button>
          <el-button link class="back-link" @click="router.push('/index')">
            返回系统控制台
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { type Result } from '../api/types'
import { Checked, Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const quoteContent = ref('') // 名言内容
const quoteAuthor = ref('')  // 作者名
const loading = ref(false)

const fetchHello = async () => {
  loading.value = true
  try {
    const res = await request.get<any, Result<string>>('/hello')
    if (res.code === 200) {
      const fullText = res.data
      // 逻辑处理：尝试通过 "—" 或 "-" 拆分作者
      // 如果后端传的是 "句子 — 作者"，我们把它切开
      const parts = fullText.split(/[—\-]/)
      if (parts.length > 1) {
        quoteContent.value = parts[0].trim()
        quoteAuthor.value = parts[parts.length - 1].trim()
      } else {
        quoteContent.value = fullText
        quoteAuthor.value = "" // 没有作者则为空
      }
    }
  } catch (err) {
    quoteContent.value = "Access Denied"
    quoteAuthor.value = "System"
  } finally {
    setTimeout(() => { loading.value = false }, 600)
  }
}

onMounted(() => fetchHello())
</script>

<style scoped>
@import '@/styles/hello.css';
</style>
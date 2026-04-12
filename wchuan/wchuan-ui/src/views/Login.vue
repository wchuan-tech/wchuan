<template>
  <div class="login-page">
    <div class="login-container">
      <h2 class="login-title">WCHUAN_SYSTEM</h2>

      <!-- 修正：rules 绑定加冒号，加上 class  -->
      <el-form
          :model="loginForm"
          ref="formRef"
          :rules="formRules"
          label-width="70px"
          class="login-form"
          :validate-on-rule-change="false"
          :validate-on-blur="false"
          :validate-on-input="false"
      >

        <!-- 用户名 -->
        <el-form-item label="用户名" prop="userName" class="login-form-item">
          <el-input
              v-model="loginForm.userName"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item label="密码" prop="password" class="login-form-item">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
          />
        </el-form-item>

        <!-- 验证码 -->
        <el-form-item label="验证码" prop="code" class="login-form-item">
          <div class="captcha-row">
            <el-input
                v-model="loginForm.code"
                placeholder="请输入验证码"
                :prefix-icon="Picture"
                clearable
                class="captcha-input"
            />
            <!-- 验证码图片：加 alt 属性提升健壮性 -->
            <img
                v-if="captchaImg"
                :src="captchaImg"
                @click="getCaptcha"
                class="captcha-img"
                title="点击刷新"
                alt="验证码"
            />
            <div v-else class="captcha-placeholder" @click="getCaptcha">
              加载中...
            </div>
          </div>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-button
            type="primary"
            class="login-btn"
            @click="handleLogin"
            :loading="loading"
        >
          登 录
        </el-button>

      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">

import {ref, reactive, onMounted, nextTick} from 'vue'
import request from '../utils/request'
import { useRouter } from 'vue-router'
import {ElMessage, type FormInstance} from 'element-plus'
import { User, Lock, Picture } from '@element-plus/icons-vue'
import type {Result, LoginResponse, CaptchaResponse, UserInfo} from '../api/types'

const router = useRouter()
// 3. 强类型定义 formRef，修复 TS 警告
const formRef = ref<FormInstance>()
const loading = ref(false)

// 表单数据
const loginForm = reactive({
  userName: '',
  password: '',
  uuid: '',
  code: ''
})

// 记住账号
const rememberMe = ref(false)

// 表单校验规则
const formRules = {
  userName: [{ required: true, message: '请输入用户名', trigger: ['change'] }],
  password: [{ required: true, message: '请输入密码', trigger: ['change'] }],
  code: [{ required: true, message: '请输入验证码', trigger: ['change'] }]
}

// 验证码
const captchaImg = ref('')

// 读取缓存
onMounted(() => {
  const remember = localStorage.getItem('rememberMe')
  const account = localStorage.getItem('userAccount')
  if (remember === 'true' && account) {
    loginForm.userName = account
    rememberMe.value = true
  }
  getCaptcha()

  nextTick(() => {
    formRef.value?.clearValidate()
  })
})

// 获取验证码
const getCaptcha = async () => {
  try {
    const res = await request.get<any, Result<CaptchaResponse>>('/captchaImage')
    let img = res.data.img
    // 严格处理图片格式
    if (!img || !img.startsWith('data:image')) {
      img = 'data:image/png;base64,' + (img || '')
    }
    captchaImg.value = img
    loginForm.uuid = res.data.uuid
  } catch (error) {
    ElMessage.error('验证码获取失败，请刷新页面')
    captchaImg.value = ''
  }
}

// 登录
const handleLogin = async () => {

  loading.value = true
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    const res = await request
        .post<any, Result<LoginResponse>>('/user/login', loginForm)

    if (res.code === 200) {
      ElMessage.success('登录成功')
      localStorage.setItem('token', res.data.token)
      // 立即调用获取用户信息接口
      const infoRes = await request.get<any, Result<UserInfo>>('/user/getInfo')

      if (infoRes.code === 200) {
        // 4. 将 UserInfo 里的数据存入 localStorage，供 hasPerm 使用
        localStorage.setItem('userId', infoRes.data.id.toString())
        localStorage.setItem('permissions', JSON.stringify(infoRes.data.permissions || []))

        localStorage.setItem('rememberMe', 'true')
        localStorage.setItem('userAccount', loginForm.userName)
      } else {
        localStorage.removeItem('rememberMe')
        localStorage.removeItem('userAccount')
      }
      await router.push('/index')
    } else {
      ElMessage.error(res.msg || '登录失败')
      loginForm.code = ''
      await getCaptcha()
      await nextTick(() => {
        formRef.value?.clearValidate()
      })
    }
  } catch (err) {
    ElMessage.error('用户名或密码错误,请重试')
    await getCaptcha()
    await nextTick(() => {
      formRef.value?.clearValidate()
    })
  } finally {
    loading.value = false
  }
}


</script>

<style>
/* 确保路径正确 */
@import '@/styles/login.css';
</style>
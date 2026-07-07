<template>
  <div class="index-container" v-loading="loading">
    <el-card class="menu-main-card glass-container">
      <div class="header-decoration">
        <div class="dot red"></div><div class="dot yellow"></div><div class="dot green"></div>
      </div>

      <div class="profile-content">
        <div class="table-header">
          <h2 class="user-name"><el-icon><Fold /></el-icon> 菜单权限管理</h2>
          <div class="header-actions">
            <el-button v-if="hasPerm('system:menu:repair')" type="warning" plain :icon="MagicStick" @click="handleRepair">系统自愈</el-button>
            <el-button v-if="hasPerm('system:menu:add')" type="primary" :icon="Plus" @click="handleAdd(0)">新增根菜单</el-button>
            <el-button :icon="Refresh" circle @click="getMenuList"></el-button>
          </div>
        </div>

        <div class="search-bar">
          <el-input v-model="filterText" placeholder="搜索菜单名称..." prefix-icon="Search" clearable />
        </div>

        <!-- 树形表格 -->
        <el-table :data="filteredMenuList" row-key="id" border class="custom-table">
          <el-table-column prop="menuName" label="菜单名称" min-width="180" :show-overflow-tooltip="true">
            <template #default="{ row }">
              <el-icon v-if="row.menuType === 'M'" color="#E6A23C"><Folder /></el-icon>
              <el-icon v-else-if="row.menuType === 'C'" color="#409EFF"><Document /></el-icon>
              <el-icon v-else color="#909399"><Pointer /></el-icon>
              <span style="margin-left: 8px">{{ row.menuName }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="menuType" label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.menuType === 'M' ? 'warning' : row.menuType === 'C' ? '' : 'success'" size="small">
                {{ row.menuType === 'M' ? '目录' : row.menuType === 'C' ? '菜单' : '功能' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="perms" label="权限标识" min-width="150" :show-overflow-tooltip="true" />
          <el-table-column prop="path" label="路由地址" min-width="120" :show-overflow-tooltip="true" />
          <el-table-column prop="component" label="组件路径" min-width="150" :show-overflow-tooltip="true" />

          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                {{ row.status === '0' ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.menuType !== 'F'" link type="primary" :icon="Plus" @click="handleAdd(row.id)">新增</el-button>
              <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">修改</el-button>
              <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 增改弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body>
      <el-form ref="menuFormRef" :model="menuForm" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <el-tree-select
                  v-model="menuForm.parentId"
                  :data="menuOptions"
                  :props="{ label: 'menuName', value: 'id', children: 'children' }"
                  value-key="id"
                  placeholder="选择上级菜单"
                  check-strictly
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="menuForm.menuType">
                <el-radio label="M">目录</el-radio>
                <el-radio label="C">菜单</el-radio>
                <el-radio label="F">按钮</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="menuForm.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="menuForm.orderNum" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>

          <!-- 路由与组件：按钮类型隐藏 -->
          <el-col :span="12" v-if="menuForm.menuType !== 'F'">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="menuForm.path" placeholder="例如: user" />
            </el-form-item>
          </el-col>

          <el-col :span="12" v-if="menuForm.menuType === 'C'">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="menuForm.component" placeholder="例如: system/user/index" />
            </el-form-item>
          </el-col>

          <!-- 权限标识：目录不填 -->
          <el-col :span="24" v-if="menuForm.menuType !== 'M'">
            <el-form-item label="权限标识" prop="perms">
              <el-input v-model="menuForm.perms" placeholder="例如: add">
                <template #prepend v-if="parentPermsPrefix">{{ parentPermsPrefix }}:</template>
              </el-input>
              <div style="font-size: 12px; color: #909399; margin-top: 4px" v-if="parentPermsPrefix">
                最终标识：{{ parentPermsPrefix }}:{{ menuForm.perms }}
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="菜单状态">
              <el-radio-group v-model="menuForm.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="显示状态">
              <el-radio-group v-model="menuForm.visible">
                <el-radio label="0">显示</el-radio>
                <el-radio label="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, nextTick } from 'vue'
import { Plus, Edit, Delete, Refresh, Fold, Folder, Document, Pointer, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import request from '../utils/request'
import { type Result, type SysMenu } from '../api/types'

// ====================== 1. 变量定义 ======================
const menuFormRef = ref<FormInstance>()
const loading = ref(false)
const repairLoading = ref(false)
const menuList = ref<SysMenu[]>([])
const filterText = ref('')
const dialogOpen = ref(false)
const dialogTitle = ref('')

// 表单初始值
const initialForm: SysMenu = {
  id: undefined,
  menuName: '',
  parentId: 0,
  orderNum: 1,
  path: '',
  component: '',
  menuType: 'M',
  perms: '',
  status: '0',
  visible: '0'
}

const menuForm = ref<SysMenu>({ ...initialForm })

// ====================== 2. 权限与状态判断 ======================
const isAdmin = computed(() => {
  const userId = localStorage.getItem('userId')
  const rawData = localStorage.getItem('permissions')
  const userPermissions: string[] = rawData ? JSON.parse(rawData) : []
  return userId === '1' || userPermissions.includes('*')
})

const hasPerm = (permission: string) => {
  const userId = localStorage.getItem('userId')
  const rawData = localStorage.getItem('permissions')
  const userPermissions: string[] = rawData ? JSON.parse(rawData) : []
  return userId === '1' || userPermissions.includes('*') || userPermissions.includes(permission)
}

// ====================== 3. 计算属性与过滤逻辑 ======================

// 计算父级权限前缀
const parentPermsPrefix = computed(() => {
  if (!menuForm.value.parentId || menuForm.value.parentId === 0) return ''
  const parent = findNodeById(menuList.value, menuForm.value.parentId)
  return parent ? parent.perms : ''
})

// 搜索过滤
const filteredMenuList = computed(() => {
  if (!filterText.value) return menuList.value
  return searchTree(menuList.value, filterText.value)
})

// 树选择器选项
const menuOptions = computed(() => {
  const rawData = JSON.parse(JSON.stringify(menuList.value))
  const currentId = menuForm.value.id
  let filteredData = isAdmin.value ? filterOnlySelf(rawData, currentId) : filterStrict(rawData, menuForm.value.menuType, currentId)
  return [{ id: 0, menuName: '主类目', children: filteredData }]
})

// ====================== 4. 核心功能函数 ======================

const getMenuList = async () => {
  loading.value = true
  try {
    const res = await request.get<any, Result<SysMenu[]>>('/system/menu/tree')
    if (res.code === 200) menuList.value = res.data
  } finally {
    loading.value = false
  }
}

const handleAdd = (parentId: number) => {
  dialogTitle.value = parentId === 0 ? "新增根菜单" : "新增子项"
  menuForm.value = {
    ...initialForm,
    parentId,
    menuType: parentId === 0 ? 'M' : 'C',
    component: parentId === 0 ? 'Layout' : ''
  }
  dialogOpen.value = true
  nextTick(() => menuFormRef.value?.clearValidate())
}

const handleEdit = (row: SysMenu) => {
  dialogTitle.value = "修改菜单项"
  const data = { ...row }
  // 回显时截掉父级前缀，方便用户修改最后一段
  if (data.perms && data.perms.includes(':')) {
    data.perms = data.perms.substring(data.perms.lastIndexOf(':') + 1)
  }
  menuForm.value = data
  dialogOpen.value = true
}

const submitForm = async () => {
  if (!menuFormRef.value) return

  await menuFormRef.value.validate(async (valid) => {
    if (!valid) return

    // 处理权限自动拼接
    const postData = { ...menuForm.value }
    if (parentPermsPrefix.value && postData.perms) {
      const leaf = postData.perms.includes(':')
          ? postData.perms.substring(postData.perms.lastIndexOf(':') + 1)
          : postData.perms
      postData.perms = `${parentPermsPrefix.value}:${leaf}`
    }

    const isEdit = !!postData.id
    const method = isEdit ? 'put' : 'post';

    try {
      // 3. 【核心修改】使用 try-catch 包裹请求
      // 注意：这里发送的是处理后的 postData，而不是 menuForm.value
      const res = await request[method]<any, Result>('/system/menu', postData);

      if (res.code === 200) {
        ElMessage.success(isEdit ? "修改成功" : "新增成功");
        dialogOpen.value = false;
        await getMenuList();
      } else {
        // 如果后端返回了业务错误（非200），但拦截器没跳登录，这里处理提示
        ElMessage.error(res.msg || "操作失败");
      }
    } catch (error: any) {
      // 4. 【核心修复】捕获异常
      // 如果拦截器已经 reject 了（比如 500 错误），代码会运行到这里
      // 我们只需要在这里打印日志，弹窗不要关闭，让用户修改后重试
      console.error("提交菜单失败:", error);

      // 注意：如果跳转登录还在发生，说明错误码是 401，必须去改后端或拦截器
    }
  })
}

const handleDelete = (row: SysMenu) => {
  ElMessageBox.confirm(`确定删除菜单 "${row.menuName}" 吗?`, '警告', { type: 'warning' }).then(async () => {
    const res = await request.delete<any, Result>(`/system/menu/${row.id}`)
    if (res.code === 200) {
      ElMessage.success("删除成功")
      await getMenuList()
    }
  })
}

const handleRepair = async () => {
  try {
    await ElMessageBox.confirm('确定要执行系统拓扑自愈吗？', '自愈提醒', { type: 'warning' })
    repairLoading.value = true
    const res = await request.post<any, Result>('/system/menu/repair')
    if (res.code === 200) {
      ElMessage.success("自愈成功")
      await getMenuList()
    }
  } finally {
    repairLoading.value = false
  }
}

// ====================== 5. 辅助工具函数 ======================

const rules = reactive({
  menuName: [{ required: true, message: "名称不能为空", trigger: "blur" }],
  orderNum: [{ required: true, message: "排序不能为空", trigger: "blur" }],
  path: [{ validator: (_rule: any, value: any, callback: any) => {
      if (menuForm.value.menuType !== 'F' && !value) callback(new Error('路由地址不能为空'))
      else callback()
    }, trigger: "blur" }]
})

function findNodeById(tree: SysMenu[], id: number): SysMenu | null {
  for (const node of tree) {
    if (node.id === id) return node
    if (node.children) {
      const res = findNodeById(node.children, id)
      if (res) return res
    }
  }
  return null
}

function searchTree(tree: SysMenu[], keyword: string): SysMenu[] {
  return tree.filter(node => {
    const match = node.menuName.includes(keyword)
    if (node.children) {
      node.children = searchTree(node.children, keyword)
      return match || node.children.length > 0
    }
    return match
  })
}

function filterOnlySelf(tree: SysMenu[], currentId?: number): SysMenu[] {
  return tree.filter(n => n.id !== currentId).map(n => {
    const newNode = { ...n }
    if (newNode.children) newNode.children = filterOnlySelf(newNode.children, currentId)
    return newNode
  })
}

function filterStrict(tree: SysMenu[], type: string, currentId?: number): SysMenu[] {
  return tree.filter(n => {
    if (currentId && n.id === currentId) return false
    // 目录或菜单只能建在目录下，按钮只能建在菜单下
    return (type === 'M' || type === 'C') ? n.menuType === 'M' : n.menuType === 'C'
  }).map(n => {
    const newNode = { ...n }
    if (newNode.children) newNode.children = filterStrict(newNode.children, type, currentId)
    return newNode
  })
}

onMounted(() => getMenuList())
</script>

<style scoped>
@import '@/styles/userProfile.css';
@import '@/styles/menu.css';
</style>
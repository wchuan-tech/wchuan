<template>
  <div class="index-container" v-loading="loading">
    <el-card class="menu-main-card glass-container">
      <div class="header-decoration">
        <div class="dot red"></div>
        <div class="dot yellow"></div>
        <div class="dot green"></div>
      </div>

      <div class="profile-content">
        <div class="table-header">
          <h2 class="user-name"><el-icon><Fold /></el-icon> 菜单权限管理</h2>
          <div class="header-actions">
            <!-- 优化：增加 :loading 绑定 -->
            <el-button
                v-if="hasPerm('system:menu:repair')"
                type="warning"
                plain
                :icon="MagicStick"
                :loading="repairLoading"
                @click="handleRepair"
            >
              系统自愈
            </el-button>
            <el-button
                v-if="hasPerm('system:menu:add')"
                type="primary"
                :icon="Plus"
                @click="handleAdd(0)"
            >
              新增根菜单
            </el-button>
            <el-button :icon="Refresh" circle @click="getMenuList" />
          </div>
        </div>

        <div class="search-bar">
          <el-input v-model="filterText" placeholder="搜索菜单名称..." prefix-icon="Search" clearable />
        </div>

        <!-- 树形表格：搜索时默认展开节点 -->
        <el-table
            :data="filteredMenuList"
            row-key="id"
            border
            class="custom-table"
            :default-expand-all="!!filterText"
        >
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
              <el-button v-if="row.menuType !== 'F' && hasPerm('system:menu:add')" link type="primary" :icon="Plus" @click="handleAdd(row.id)">新增</el-button>
              <el-button v-if="hasPerm('system:menu:edit')" link type="primary" :icon="Edit" @click="handleEdit(row)">修改</el-button>
              <el-button v-if="hasPerm('system:menu:remove')" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 增改弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body @closed="resetForm">
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
                <el-radio value="M">目录</el-radio>
                <el-radio value="C">菜单</el-radio>
                <el-radio value="F">按钮</el-radio>
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
              <el-input v-model="menuForm.perms" placeholder="例如: system:user:add">
              </el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="菜单状态">
              <el-radio-group v-model="menuForm.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="显示状态">
              <el-radio-group v-model="menuForm.visible">
                <el-radio value="0">显示</el-radio>
                <el-radio value="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, nextTick } from 'vue'
import { Plus, Edit, Delete, Refresh, Fold, Folder, Document, Pointer, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { type SysMenu } from '@/api/types'
import { addMenu, delMenu, listMenu, repairMenu, updateMenu } from '@/api/system/menu'

// ====================== 1. 变量定义 ======================
const menuFormRef = ref<FormInstance>()
const loading = ref(false)
const repairLoading = ref(false)
const submitLoading = ref(false)
const menuList = ref<SysMenu[]>([])
const filterText = ref('')
const dialogOpen = ref(false)
const dialogTitle = ref('')

// 权限信息（一次性从缓存加载，提升性能）
const currentUserId = ref<string>('')
const userPermissions = ref<string[]>([])

// 表单初始值常量
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

// ====================== 2. 权限与状态判断（性能优化） ======================
const initUserPermissions = () => {
  currentUserId.value = localStorage.getItem('userId') || ''
  const rawData = localStorage.getItem('permissions')
  userPermissions.value = rawData ? JSON.parse(rawData) : []
}

const isAdmin = computed(() => {
  return currentUserId.value === '1' || userPermissions.value.includes('*')
})

const hasPerm = (permission: string) => {
  return isAdmin.value || userPermissions.value.includes(permission)
}

// ====================== 3. 计算属性与过滤逻辑 ======================

// 搜索过滤（修复：使用深拷贝避免修改源数据）
const filteredMenuList = computed(() => {
  if (!filterText.value) return menuList.value
  const clonedList = JSON.parse(JSON.stringify(menuList.value))
  return searchTree(clonedList, filterText.value)
})

// 树选择器选项
const menuOptions = computed(() => {
  const rawData: SysMenu[] = JSON.parse(JSON.stringify(menuList.value))
  const currentId = menuForm.value.id
  const current = currentId ? findNodeById(rawData, currentId) : null
  const excludedIds = current ? collectNodeIds(current) : new Set<number>()
  const filteredData = isAdmin.value
      ? filterExcluded(rawData, excludedIds)
      : filterStrict(rawData, menuForm.value.menuType, excludedIds)
  return [{ id: 0, menuName: '主类目', children: filteredData }]
})

// ====================== 4. 核心功能函数 ======================

const getMenuList = async () => {
  loading.value = true
  try {
    const res = await listMenu()
    menuList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleAdd = (parentId: number) => {
  const parent = parentId === 0 ? null : findNodeById(menuList.value, parentId)
  const menuType: SysMenu['menuType'] = parentId === 0
      ? 'M'
      : parent?.menuType === 'C' ? 'F' : 'C'
  dialogTitle.value = parentId === 0 ? '新增根菜单' : '新增子项'
  menuForm.value = {
    ...initialForm,
    parentId,
    menuType,
    path: menuType === 'F' ? '#' : '',
    component: ''
  }
  dialogOpen.value = true
  nextTick(() => menuFormRef.value?.clearValidate())
}

const handleEdit = (row: SysMenu) => {
  dialogTitle.value = '修改菜单项'
  const data = JSON.parse(JSON.stringify(row))
  menuForm.value = data
  dialogOpen.value = true
  nextTick(() => menuFormRef.value?.clearValidate())
}

const submitForm = async () => {
  if (!menuFormRef.value) return

  await menuFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    const postData = { ...menuForm.value }
    if (postData.menuType === 'M') {
      postData.perms = postData.perms?.trim() || ''
    } else if (postData.menuType === 'F') {
      postData.path = '#'
      postData.component = ''
    }

    const isEdit = !!postData.id
    try {
      if (isEdit) await updateMenu(postData)
      else await addMenu(postData)
      ElMessage.success(isEdit ? '修改成功' : '新增成功')
      dialogOpen.value = false
      await getMenuList()
    } catch (error) {
      console.error('提交菜单失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = (row: SysMenu) => {
  ElMessageBox.confirm(`确定删除菜单 "${row.menuName}" 吗?`, '警告', { type: 'warning' }).then(async () => {
    if (row.id == null) return
    await delMenu(row.id)
    ElMessage.success('删除成功')
    await getMenuList()
  })
}

const handleRepair = async () => {
  try {
    await ElMessageBox.confirm('确定要执行系统拓扑自愈吗？', '自愈提醒', { type: 'warning' })
    repairLoading.value = true
    await repairMenu()
    ElMessage.success('自愈成功')
    await getMenuList()
  } catch (error) {
    console.error('自愈失败:', error)
  } finally {
    repairLoading.value = false
  }
}

const resetForm = () => {
  menuForm.value = { ...initialForm }
  menuFormRef.value?.resetFields()
}

// ====================== 5. 辅助工具函数 ======================

const rules = reactive({
  menuName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  orderNum: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
  path: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (menuForm.value.menuType !== 'F' && !value) {
          callback(new Error('路由地址不能为空'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  component: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (menuForm.value.menuType === 'C' && !value) {
          callback(new Error('组件路径不能为空'))
        } else if (value && (!/^[A-Za-z0-9_/-]+$/.test(value) || value.includes('..'))) {
          callback(new Error('组件路径格式不合法'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  perms: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (['C', 'F'].includes(menuForm.value.menuType) && !value) {
          callback(new Error('权限标识不能为空'))
        } else if (value?.includes('*')) {
          callback(new Error('权限标识禁止包含通配符'))
        } else if (value && !/^[A-Za-z][A-Za-z0-9_-]*(?::[A-Za-z][A-Za-z0-9_-]*)*$/.test(value)) {
          callback(new Error('权限标识格式不合法'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

function findNodeById(tree: SysMenu[], id: number): SysMenu | null {
  for (const node of tree) {
    if (node.id === id) return node
    if (node.children && node.children.length > 0) {
      const res = findNodeById(node.children, id)
      if (res) return res
    }
  }
  return null
}

function searchTree(tree: SysMenu[], keyword: string): SysMenu[] {
  return tree.filter((node) => {
    const match = node.menuName.toLowerCase().includes(keyword.toLowerCase())
    if (node.children && node.children.length > 0) {
      node.children = searchTree(node.children, keyword)
      return match || node.children.length > 0
    }
    return match
  })
}

function collectNodeIds(node: SysMenu, result = new Set<number>()): Set<number> {
  if (node.id != null) result.add(node.id)
  node.children?.forEach(child => collectNodeIds(child, result))
  return result
}

function filterExcluded(tree: SysMenu[], excludedIds: Set<number>): SysMenu[] {
  return tree
      .filter((n) => n.id == null || !excludedIds.has(n.id))
      .map((n) => {
        const newNode = { ...n }
        if (newNode.children) newNode.children = filterExcluded(newNode.children, excludedIds)
        return newNode
      })
}

function filterStrict(tree: SysMenu[], type: string, excludedIds: Set<number>): SysMenu[] {
  return tree
      .filter((n) => {
        if (n.id != null && excludedIds.has(n.id)) return false
        return type === 'M' || type === 'C' ? n.menuType === 'M' : n.menuType === 'C'
      })
      .map((n) => {
        const newNode = { ...n }
        if (newNode.children) newNode.children = filterStrict(newNode.children, type, excludedIds)
        return newNode
      })
}

onMounted(() => {
  initUserPermissions()
  getMenuList()
})
</script>

<style scoped>
@import '@/styles/userProfile.css';
@import '@/styles/menu.css';
</style>

<template>
  <div class="index-container" v-loading="loading">

    <el-card class="menu-main-card glass-container">
      <!-- 头部装饰与标题 -->
      <div class="header-decoration">
        <div class="dot red"></div><div class="dot yellow"></div><div class="dot green"></div>
      </div>

      <div class="profile-content">
        <div class="table-header">
          <h2 class="user-name"><el-icon><Fold /></el-icon> 菜单权限管理</h2>

          <el-button
              v-if="hasPerm('system:menu:repair')"
              type="warning"
              plain
              :icon="MagicStick"
              :loading="repairLoading"
              @click="handleRepair"
              class="repair-btn"
          >系统自愈</el-button>

          <!-- 1. 新增根菜单按钮 -->
          <div class="header-actions">
            <el-button
                v-if="hasPerm('system:menu:add')"
                type="primary"
                :icon="Plus"
                @click="handleAdd(0)"
            >新增根菜单</el-button>

            <el-button :icon="Refresh" circle @click="getMenuList"></el-button>
          </div>
        </div>

        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-input v-model="filterText" placeholder="搜索菜单名称..." prefix-icon="Search" clearable />
        </div>

        <!-- 树形表格 -->
        <el-table
            :data="filteredMenuList"
            row-key="id"
            border
            class="custom-table"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        >
          <!-- 菜单名称列：增加图标区分 -->
          <el-table-column prop="menuName" label="菜单名称" min-width="180">
            <template #default="{ row }">
              <!-- 根据类型展示不同图标 -->
              <el-icon v-if="row.menuType === 'M'" style="margin-right: 5px; color: #E6A23C"><Folder /></el-icon>
              <el-icon v-if="row.menuType === 'C'" style="margin-right: 5px; color: #409EFF"><Document /></el-icon>
              <el-icon v-if="row.menuType === 'F'" style="margin-right: 5px; color: #909399"><Pointer /></el-icon>
              <span>{{ row.menuName }}</span>
            </template>
          </el-table-column>

          <!-- 类型列：增加彩色标签 -->
          <el-table-column prop="menuType" label="类型标识" width="100" align="center">
            <template #default="{ row }">
              <!-- M-目录(黄), C-菜单(蓝), F-按钮(绿) -->
              <el-tag
                  :type="row.menuType === 'M' ? 'warning' : row.menuType === 'C' ? '' : 'success'"
                  effect="dark"
                  size="small"
                  class="type-tag"
              >
                {{ row.menuType === 'M' ? '目录' : row.menuType === 'C' ? '菜单' : '功能' }}
              </el-tag>
            </template>
          </el-table-column>

          <!-- 3. 权限标识列（之前漏掉的列，现已补回） -->
          <el-table-column prop="perms" label="权限标识" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <code v-if="row.perms" class="perm-code">{{ row.perms }}</code>
              <span v-else style="color: #ccc">-</span>
            </template>
          </el-table-column>

          <el-table-column prop="path" label="路由地址" min-width="120" />

          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row }">
              <!-- 核心修改：只有类型不是 'F' (功能/按钮) 的时候，才允许在其下方“新增”子项 -->
              <el-button
                  v-if="row.menuType !== 'F'"
                  link
                  type="primary"
                  :icon="Plus"
                  @click="handleAdd(row.id)"
              >新增</el-button>

              <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">修改</el-button>
              <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>

        </el-table>
      </div>
    </el-card>

    <!-- 新增/修改弹窗 -->
    <!-- 修改后的弹窗表单部分 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="580px" append-to-body class="custom-dialog">
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
                <el-radio :disabled="!!menuForm.id" label="M">目录</el-radio>
                <el-radio :disabled="!!menuForm.id" label="C">菜单</el-radio>
                <el-radio :disabled="!!menuForm.id" label="F">按钮</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="menuForm.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>

          <el-col :span="12" v-if="menuForm.menuType !== 'F'">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="menuForm.path" placeholder="请输入路由地址" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="menuForm.orderNum" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>

          <!-- ✅ 修正这里：使用 el-form-item + el-input 而不是 el-table-column -->
          <el-col :span="24" v-if="menuForm.menuType !== 'M'">
            <el-form-item label="权限标识" prop="perms">
              <el-input v-model="menuForm.perms" placeholder="system:user:list" />
            </el-form-item>
          </el-col>

        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogOpen = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, computed, reactive} from 'vue'
import {Plus, Edit, Delete, Refresh, Fold, Folder, Document, Pointer, MagicStick} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { type Result, type SysMenu } from '../api/types'

const loading = ref(false)
const menuList = ref<SysMenu[]>([])
const filterText = ref('')
const dialogOpen = ref(false)
const dialogTitle = ref('')

// 表单对象
const menuForm = ref<SysMenu>({
  menuName: '',
  parentId: 0,
  orderNum: 1,
  path: '',
  menuType: 'C',
  perms: ''
})

// 校验规则
const rules = reactive({
  menuName: [{ required: true, message: "名称不能为空", trigger: "blur" }],
  perms: [
    { required: true, message: "权限标识不能为空", trigger: "blur" },
    { pattern: /^[a-z]+(:[a-z]+)*$/, message: "标识格式不正确(小写字母及冒号)", trigger: "blur" }
  ]
})

/**
 * 权限判断函数 (复刻 UserProfile 逻辑)
 * @param permission 按钮要求的权限标识
 */
const hasPerm = (permission: string) => {
  // 1. 从本地获取数据
  const userId = localStorage.getItem('userId')
  const rawData = localStorage.getItem('permissions')
  const userPermissions: string[] = rawData ? JSON.parse(rawData) : []

  // 2. 核心逻辑判断：是否为超级管理员
  // 逻辑：ID 为 1 或者 权限数组包含 '*'
  const isAdmin = userId === '1' || userPermissions.includes('*')

  if (isAdmin) {
    return true // 管理员直接放行所有按钮
  }

  // 3. 普通用户校验具体标识
  return userPermissions.includes(permission)
}

// 获取列表
const getMenuList = async () => {
  loading.value = true
  const res = await request.get<any, Result<SysMenu[]>>('/system/menu/treeselect')
  if (res.code === 200) menuList.value = res.data
  loading.value = false
}

// 搜索过滤
const filteredMenuList = computed(() => {
  if (!filterText.value) return menuList.value
  return menuList.value.filter(item => item.menuName.includes(filterText.value))
})


/**
 * 树选择器的数据源
 * 逻辑：根据当前选中的“菜单类型”，动态筛选合法的父节点
 */
const menuOptions = computed(() => {
  const rawData = JSON.parse(JSON.stringify(menuList.value));
  const currentType = menuForm.value.menuType;
  const currentId = menuForm.value.id;

  // 调用增强版过滤函数
  const filteredData = filterMenuTreeStrict(rawData, currentType, currentId);

  return [{ id: 0, menuName: '主类目', children: filteredData }];
});

/**
 * 严谨版过滤算法
 */
function filterMenuTreeStrict(tree: SysMenu[], currentType: string, currentId?: number): SysMenu[] {
  if (!tree) return [];

  return tree
      .filter((node: SysMenu) => {
        // 1. 基础排除：不显示自己
        if (currentId && node.id === currentId) return false;

        // 2. 层级约束过滤：
        // 如果当前想建的是“目录(M)”或“菜单(C)”，那么父节点只能是“目录(M)”
        if (currentType === 'M' || currentType === 'C') {
          return node.menuType === 'M';
        }

        // 如果当前想建的是“按钮(F)”，那么父节点只能是“菜单(C)”
        if (currentType === 'F') {
          return node.menuType === 'C';
        }

        return true;
      })
      .map((node: SysMenu) => {
        // 3. 递归处理子节点
        const newNode = { ...node };
        if (newNode.children) {
          newNode.children = filterMenuTreeStrict(newNode.children, currentType, currentId);
        }
        return newNode;
      });
}

// 操作：新增
const handleAdd = (parentId: number) => {
  menuForm.value = {
    menuName: '',
    parentId,
    orderNum: 1,
    path: '',
    menuType: 'C',
    perms: '' }
  dialogTitle.value = "发布新功能"
  dialogOpen.value = true
}

// 操作：修改
const handleEdit = (row: SysMenu) => {
  menuForm.value = { ...row } // 浅拷贝
  dialogTitle.value = "编辑权限项"
  dialogOpen.value = true
}

// 操作：删除
const handleDelete = (row: SysMenu) => {
  ElMessageBox.confirm(`确定删除菜单 "${row.menuName}" 吗?`, '警告', {
    type: 'warning'
  }).then(async () => {
    const res = await request.delete<any, Result>(`/system/menu/${row.id}`)
    if (res.code === 200) {
      ElMessage.success("删除成功")
      await getMenuList()
    }
  })
}

// 提交
const submitForm = async () => {
  const isEdit = !!menuForm.value.id
  const method = isEdit ? 'put' : 'post'
  const res = await request[method]<any, Result>('/system/menu', menuForm.value)
  if (res.code === 200) {
    ElMessage.success(isEdit ? "修改成功" : "新增成功")
    dialogOpen.value = false
    await getMenuList()
  }
}

const repairLoading = ref(false)

/**
 * 执行系统自愈逻辑
 */
const handleRepair = async () => {
  try {
    // 1. 二次确认，防止误点
    await ElMessageBox.confirm(
        '确定要执行系统拓扑自愈吗？该操作将全量扫描并自动修正手动改库导致的权限标识错误。',
        '高级系统自愈',
        {
          confirmButtonText: '立即执行',
          cancelButtonText: '取消',
          type: 'warning',
          // 雅致化弹窗样式
          customClass: 'custom-confirm-box'
        }
    )

    // 2. 开启 Loading
    repairLoading.value = true

    // 3. 发起请求 (注意：这是 POST 请求)
    const res = await request.post<any, Result>('/system/menu/repair')

    if (res.code === 200) {
      ElMessage.success("自愈成功，权限命名空间已对齐")
      // 4. 刷新列表看到新数据
      await getMenuList()
    }
  } catch (error) {
    // 用户取消或接口报错
    console.log("取消自愈操作")
  } finally {
    repairLoading.value = false
  }
}

onMounted(() => getMenuList())
</script>

<style scoped>
@import '@/styles/userProfile.css';
@import '@/styles/menu.css';
</style>
<template>
  <div class="app-container" v-loading="loading">
    <el-card class="glass-card">
      <!-- 1. 搜索栏 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="queryParams.roleName" placeholder="角色名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="queryParams.roleKey" placeholder="权限字符" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="角色状态" clearable style="width: 120px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮栏 -->
      <div class="mb-4">
        <el-button v-if="hasPerm('system:role:add')" type="primary" icon="Plus" @click="handleAdd">新增角色</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="roleList" border style="width: 100%">
        <el-table-column prop="id" label="角色ID" width="80" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" align="center" />
        <el-table-column prop="roleKey" label="权限字符" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ row.roleKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleSort" label="显示顺序" width="100" align="center" />
        <el-table-column prop="dataScope" label="数据范围" width="145" align="center">
          <template #default="{ row }">
            <el-tag :type="dataScopeTagType(row.dataScope)">{{ dataScopeLabel(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="380" align="center" fixed="right">
          <template #default="{ row }">
            <!-- 超级管理员 admin 角色防护：非超管账号禁止修改与删除 -->
            <template v-if="row.roleKey === 'admin' && currentUserId !== '1'">
              <el-tooltip content="系统内置超级管理员，禁止修改与删除" placement="top">
                <el-tag type="info" size="small" effect="plain">系统内置</el-tag>
              </el-tooltip>
            </template>

            <!-- 普通角色操作 -->
            <template v-else>
              <el-button
                  v-if="hasPerm('system:role:edit')"
                  link type="primary"
                  icon="Edit"
                  @click="handleEdit(row)">
                修改
              </el-button>

              <el-button
                  v-if="hasPerm('system:role:edit')"
                  link type="warning"
                  icon="Key"
                  @click="handleAuthMenu(row)">
                分配权限
              </el-button>

              <el-button
                  v-if="hasPerm('system:role:edit')"
                  link type="success"
                  icon="OfficeBuilding"
                  @click="handleDataScope(row)">
                数据权限
              </el-button>

              <el-button
                  v-if="hasPerm('system:role:remove')"
                  link type="danger"
                  icon="Delete"
                  @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 2. 新增/修改角色弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="760px" append-to-body @closed="resetForm">
      <el-form ref="roleFormRef" :model="roleForm" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="例如：销售主管" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="roleForm.roleKey" placeholder="例如：manager" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="roleSort">
          <el-input-number v-model="roleForm.roleSort" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="roleForm.dataScope" placeholder="请选择数据范围" style="width: 100%">
            <el-option label="1 - 全部数据权限" value="1" />
            <el-option label="2 - 自定义数据权限" value="2" />
            <el-option label="3 - 本部门数据权限" value="3" />
            <el-option label="4 - 本部门及以下数据权限" value="4" />
            <el-option label="5 - 仅本人数据权限" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="roleForm.dataScope === '2'" label="部门权限">
          <div class="form-tree-panel" v-loading="formTreeLoading">
            <el-tree
                ref="formDeptTreeRef"
                :data="formDeptOptions"
                :props="deptTreeProps"
                node-key="id"
                show-checkbox
                check-strictly
                default-expand-all
                empty-text="加载中，请稍候" />
          </div>
        </el-form-item>
        <el-form-item label="菜单权限">
          <div class="form-tree-panel" v-loading="formTreeLoading">
            <el-tree
                ref="formMenuTreeRef"
                :data="formMenuOptions"
                :props="treeProps"
                node-key="id"
                show-checkbox
                default-expand-all
                empty-text="加载中，请稍候" />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="roleForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roleForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 数据权限 -->
    <el-dialog v-model="dataScopeDialogOpen" :title="`分配数据权限 · ${selectedRole?.roleName || ''}`" width="680px" append-to-body>
      <el-alert
          v-if="selectedRole?.id === 1 || selectedRole?.roleKey === 'admin'"
          title="超级管理员数据权限不可修改"
          type="warning"
          :closable="false"
          class="menu-alert" />
      <el-form label-width="100px">
        <el-form-item label="数据范围">
          <el-select v-model="dataScopeForm.dataScope" style="width: 100%">
            <el-option v-for="item in dataScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <template v-if="dataScopeForm.dataScope === '2'">
          <div class="tree-toolbar">
            <el-checkbox v-model="deptExpandAll" @change="handleDeptExpandAll">展开全部</el-checkbox>
            <el-checkbox v-model="deptCheckAll" :indeterminate="deptCheckIndeterminate" @change="handleDeptCheckAll">全选/全不选</el-checkbox>
            <el-checkbox v-model="deptParentChildLinked">父子联动</el-checkbox>
          </div>
          <div class="menu-tree-panel" v-loading="dataScopeLoading">
            <el-tree
                ref="deptTreeRef"
                :data="deptOptions"
                :props="deptTreeProps"
                show-checkbox
                node-key="id"
                :check-strictly="!deptParentChildLinked"
                empty-text="加载中，请稍候"
                default-expand-all
                @check="syncDeptCheckState" />
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dataScopeDialogOpen = false">取 消</el-button>
        <el-button
            type="primary"
            :loading="dataScopeSubmitting"
            :disabled="selectedRole?.id === 1 || selectedRole?.roleKey === 'admin'"
            @click="submitDataScope">
          保存数据权限
        </el-button>
      </template>
    </el-dialog>

    <!-- 3. 菜单与按钮权限树 -->
    <el-dialog
        v-model="menuDialogOpen"
        :title="`分配菜单权限 · ${selectedRole?.roleName || ''}`"
        width="680px"
        append-to-body
        destroy-on-close>
      <el-alert
          v-if="selectedRole?.id === 1 || selectedRole?.roleKey === 'admin'"
          title="超级管理员核心权限不可修改"
          type="warning"
          :closable="false"
          class="menu-alert" />

      <div class="tree-toolbar">
        <el-checkbox v-model="expandAll" @change="handleExpandAll">展开全部</el-checkbox>
        <el-checkbox v-model="checkAll" :indeterminate="checkAllIndeterminate" @change="handleCheckAll">全选/全不选</el-checkbox>
        <el-checkbox v-model="parentChildLinked">父子联动</el-checkbox>
      </div>

      <div class="menu-tree-panel" v-loading="menuLoading">
        <el-tree
            ref="treeRef"
            :data="menuOptions"
            :props="treeProps"
            show-checkbox
            node-key="id"
            :check-strictly="!parentChildLinked"
            empty-text="加载中，请稍候"
            default-expand-all
            @check="syncCheckAllState" />
      </div>

      <template #footer>
        <el-button @click="menuDialogOpen = false">取 消</el-button>
        <el-button
            type="primary"
            :loading="menuSubmitting"
            :disabled="selectedRole?.id === 1 || selectedRole?.roleKey === 'admin'"
            @click="submitMenuAuth">
          保存权限
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type ElTree, type TagProps } from 'element-plus'
import request from '../../../utils/request'
import { addRole, delRole, getDeptTree, getMenuTree, getRole, getRoleDeptTree, getRoleMenuTree, listRole, updateRole, updateRoleDataScope, type DataScope, type MenuTreeNode, type Role, type RoleForm, type RoleQuery } from '@/api/system/role'
import type { Dept } from '@/api/system/dept'

// ====================== 1. 变量与数据定义 ======================
const loading = ref(false)
const dialogOpen = ref(false)
const dialogTitle = ref('')
const roleList = ref<Role[]>([])
const roleFormRef = ref<FormInstance>()
const treeRef = ref<InstanceType<typeof ElTree>>()
const deptTreeRef = ref<InstanceType<typeof ElTree>>()
const formMenuTreeRef = ref<InstanceType<typeof ElTree>>()
const formDeptTreeRef = ref<InstanceType<typeof ElTree>>()
const formTreeLoading = ref(false)
const formMenuOptions = ref<MenuTreeNode[]>([])
const formDeptOptions = ref<Dept[]>([])

const currentUserId = ref<string>('')
const userPermissions = ref<string[]>([])
const menuDialogOpen = ref(false)
const menuLoading = ref(false)
const menuSubmitting = ref(false)
const menuOptions = ref<MenuTreeNode[]>([])
const selectedRole = ref<RoleRecord>()
const expandAll = ref(true)
const checkAll = ref(false)
const checkAllIndeterminate = ref(false)
const parentChildLinked = ref(true)
const treeProps = { children: 'children', label: 'label' }
const dataScopeDialogOpen = ref(false)
const dataScopeLoading = ref(false)
const dataScopeSubmitting = ref(false)
const deptOptions = ref<Dept[]>([])
const deptExpandAll = ref(true)
const deptCheckAll = ref(false)
const deptCheckIndeterminate = ref(false)
const deptParentChildLinked = ref(true)
const deptTreeProps = { children: 'children', label: 'deptName' }
const dataScopeForm = reactive<{ roleId?: number; dataScope: DataScope }>({ dataScope: '1' })
const dataScopeOptions: Array<{ value: DataScope; label: string }> = [
  { value: '1', label: '全部数据权限' },
  { value: '2', label: '自定义数据权限' },
  { value: '3', label: '本部门数据权限' },
  { value: '4', label: '本部门及以下数据权限' },
  { value: '5', label: '仅本人数据权限' }
]

interface RoleRecord {
  id: number
  roleName: string
  roleKey: string
  status: string
  roleSort: number
  dataScope: DataScope
  remark?: string
}

interface RoleMenuTreeResponse {
  checkedKeys: number[]
  halfCheckedKeys: number[]
  menus: MenuTreeNode[]
}

const queryParams = reactive<RoleQuery>({
  roleName: '',
  roleKey: '',
  status: ''
})

const roleForm = ref<RoleForm>({
  id: undefined,
  roleName: '',
  roleKey: '',
  roleSort: 0,
  dataScope: '5',
  status: '0',
  remark: '',
  menuIds: [],
  deptIds: []
})

const rules = reactive({
  roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
  roleKey: [{ required: true, message: '权限字符不能为空', trigger: 'blur' }],
  roleSort: [{ required: true, message: '显示顺序不能为空', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }]
})

const dataScopeLabel = (scope: DataScope) => dataScopeOptions.find(item => item.value === scope)?.label || '未知范围'
const dataScopeTagType = (scope: DataScope): TagProps['type'] => ({ '1': 'danger', '2': 'warning', '3': 'primary', '4': 'success', '5': 'info' })[scope] as TagProps['type']

// ====================== 2. 基础权限判断 ======================

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

// ====================== 3. 业务 API 函数 (CRUD) ======================

// 功能 1：获取与查询角色列表
const getRoleList = async () => {
  loading.value = true
  try {
    const res = await listRole(queryParams)
    roleList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleQuery = () => getRoleList()

const resetQuery = () => {
  queryParams.roleName = ''
  queryParams.roleKey = ''
  queryParams.status = ''
  handleQuery()
}

// 功能 2：新增与修改角色
const handleAdd = async () => {
  resetForm()
  dialogTitle.value = '新增角色'
  dialogOpen.value = true
  formTreeLoading.value = true
  try {
    const [menuRes, deptRes] = await Promise.all([getMenuTree(), getDeptTree()])
    formMenuOptions.value = menuRes.data || []
    formDeptOptions.value = deptRes.data || []
    await nextTick()
    formMenuTreeRef.value?.setCheckedKeys([], false)
    formDeptTreeRef.value?.setCheckedKeys([], false)
  } finally {
    formTreeLoading.value = false
  }
}

const handleEdit = async (row: Role) => {
  dialogTitle.value = '修改角色'
  dialogOpen.value = true
  formTreeLoading.value = true
  try {
    const [roleRes, menuRes, deptRes] = await Promise.all([
      getRole(row.id),
      getRoleMenuTree(row.id),
      getRoleDeptTree(row.id)
    ])
    roleForm.value = {
      ...roleRes.data,
      menuIds: menuRes.data.checkedKeys || [],
      deptIds: deptRes.data.checkedKeys || []
    }
    formMenuOptions.value = menuRes.data.menus || []
    formDeptOptions.value = deptRes.data.depts || []
    await nextTick()
    formMenuTreeRef.value?.setCheckedKeys(menuRes.data.checkedKeys || [], false)
    formDeptTreeRef.value?.setCheckedKeys(deptRes.data.checkedKeys || [], false)
  } finally {
    formTreeLoading.value = false
  }
}

const submitForm = async () => {
  await roleFormRef.value?.validate(async (valid) => {
    if (!valid) return
    const isEdit = !!roleForm.value.id
    const menuIds = [
      ...(formMenuTreeRef.value?.getCheckedKeys(false) || []),
      ...(formMenuTreeRef.value?.getHalfCheckedKeys() || [])
    ].map(Number)
    const deptIds = roleForm.value.dataScope === '2'
      ? [
          ...(formDeptTreeRef.value?.getCheckedKeys(false) || []),
          ...(formDeptTreeRef.value?.getHalfCheckedKeys() || [])
        ].map(Number)
      : []
    const payload: RoleForm = {
      ...roleForm.value,
      menuIds: Array.from(new Set(menuIds)),
      deptIds: Array.from(new Set(deptIds))
    }
    if (isEdit) await updateRole(payload)
    else await addRole(payload)
    ElMessage.success(isEdit ? '修改成功' : '新增成功')
    dialogOpen.value = false
    getRoleList()
  })
}

// 功能 3：删除角色
const handleDelete = (row: Role) => {
  ElMessageBox.confirm(`确定删除角色 "${row.roleName}" 吗?`, '警告', { type: 'warning' }).then(async () => {
    await delRole(row.id)
    ElMessage.success('删除成功')
    getRoleList()
  })
}

const getAllMenuIds = (nodes: MenuTreeNode[]): number[] =>
  nodes.flatMap(node => [node.id, ...getAllMenuIds(node.children || [])])

const handleAuthMenu = async (row: RoleRecord) => {
  selectedRole.value = row
  menuDialogOpen.value = true
  menuLoading.value = true
  expandAll.value = true
  parentChildLinked.value = true
  try {
    const res = await request.get<any, { code: number; data: RoleMenuTreeResponse }>(
      `/system/menu/roleMenuTreeselect/${row.id}`
    )
    menuOptions.value = res.data.menus || []
    await nextTick()
    treeRef.value?.setCheckedKeys(res.data.checkedKeys || [], false)
    syncCheckAllState()
  } finally {
    menuLoading.value = false
  }
}

const handleExpandAll = (expanded: boolean | string | number) => {
  getAllMenuIds(menuOptions.value).forEach(id => {
    const node = treeRef.value?.getNode(id)
    if (node) node.expanded = Boolean(expanded)
  })
}

const handleCheckAll = (checked: boolean | string | number) => {
  treeRef.value?.setCheckedKeys(checked ? getAllMenuIds(menuOptions.value) : [], false)
  syncCheckAllState()
}

const syncCheckAllState = () => {
  const totalCount = getAllMenuIds(menuOptions.value).length
  const checkedCount = (treeRef.value?.getCheckedKeys(false) || []).length
  checkAll.value = totalCount > 0 && checkedCount === totalCount
  checkAllIndeterminate.value = checkedCount > 0 && checkedCount < totalCount
}

const submitMenuAuth = async () => {
  if (!selectedRole.value) return
  if (selectedRole.value.id === 1 || selectedRole.value.roleKey === 'admin') {
    ElMessage.warning('超级管理员核心权限禁止修改')
    return
  }
  const checkedKeys = (treeRef.value?.getCheckedKeys(false) || []).map(Number)
  const halfCheckedKeys = (treeRef.value?.getHalfCheckedKeys() || []).map(Number)
  menuSubmitting.value = true
  try {
    await request.put('/system/role/menu', {
      roleId: selectedRole.value.id,
      checkedKeys,
      halfCheckedKeys
    })
    const infoRes: any = await request.get('/user/getInfo')
    localStorage.setItem('permissions', JSON.stringify(infoRes.data?.permissions || []))
    userPermissions.value = infoRes.data?.permissions || []
    ElMessage.success('菜单权限分配成功，在线权限缓存已刷新')
    menuDialogOpen.value = false
    await getRoleList()
  } finally {
    menuSubmitting.value = false
  }
}

const getAllDeptIds = (nodes: Dept[]): number[] =>
  nodes.flatMap(node => [node.id, ...getAllDeptIds(node.children || [])])

const handleDataScope = async (row: Role) => {
  selectedRole.value = row
  dataScopeForm.roleId = row.id
  dataScopeForm.dataScope = row.dataScope || '1'
  dataScopeDialogOpen.value = true
  dataScopeLoading.value = true
  deptExpandAll.value = true
  deptParentChildLinked.value = true
  try {
    const res = await getRoleDeptTree(row.id)
    deptOptions.value = res.data.depts || []
    await nextTick()
    deptTreeRef.value?.setCheckedKeys(res.data.checkedKeys || [], false)
    syncDeptCheckState()
  } finally {
    dataScopeLoading.value = false
  }
}

const handleDeptExpandAll = (expanded: boolean | string | number) => {
  getAllDeptIds(deptOptions.value).forEach(id => {
    const node = deptTreeRef.value?.getNode(id)
    if (node) node.expanded = Boolean(expanded)
  })
}

const handleDeptCheckAll = (checked: boolean | string | number) => {
  deptTreeRef.value?.setCheckedKeys(checked ? getAllDeptIds(deptOptions.value) : [], false)
  syncDeptCheckState()
}

const syncDeptCheckState = () => {
  const totalCount = getAllDeptIds(deptOptions.value).length
  const checkedCount = (deptTreeRef.value?.getCheckedKeys(false) || []).length
  deptCheckAll.value = totalCount > 0 && checkedCount === totalCount
  deptCheckIndeterminate.value = checkedCount > 0 && checkedCount < totalCount
}

const submitDataScope = async () => {
  if (!dataScopeForm.roleId) return
  if (selectedRole.value?.id === 1 || selectedRole.value?.roleKey === 'admin') {
    ElMessage.warning('超级管理员数据权限禁止修改')
    return
  }
  const checkedKeys = dataScopeForm.dataScope === '2'
    ? (deptTreeRef.value?.getCheckedKeys(false) || []).map(Number)
    : []
  const halfCheckedKeys = dataScopeForm.dataScope === '2'
    ? (deptTreeRef.value?.getHalfCheckedKeys() || []).map(Number)
    : []
  dataScopeSubmitting.value = true
  try {
    await updateRoleDataScope({
      id: dataScopeForm.roleId,
      dataScope: dataScopeForm.dataScope,
      deptIds: Array.from(new Set([...checkedKeys, ...halfCheckedKeys]))
    })
    ElMessage.success('数据权限分配成功')
    dataScopeDialogOpen.value = false
    await getRoleList()
  } finally {
    dataScopeSubmitting.value = false
  }
}

const resetForm = () => {
  roleForm.value = {
    id: undefined,
    roleName: '',
    roleKey: '',
    roleSort: 0,
    dataScope: '5',
    status: '0',
    remark: '',
    menuIds: [],
    deptIds: []
  }
  formMenuOptions.value = []
  formDeptOptions.value = []
  roleFormRef.value?.resetFields()
}

onMounted(() => {
  initUserPermissions()
  getRoleList()
})
</script>

<style scoped>
.app-container { padding: 20px; }
.mb-4 { margin-bottom: 16px; }
.menu-alert { margin-bottom: 14px; }
.tree-toolbar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  background: #f8fafc;
}
.menu-tree-panel {
  min-height: 260px;
  max-height: 460px;
  overflow: auto;
  padding: 14px 12px;
  border: 1px solid #ebeef5;
  border-radius: 0 0 8px 8px;
}
.form-tree-panel {
  width: 100%;
  max-height: 280px;
  overflow: auto;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}
@media (max-width: 720px) {
  .tree-toolbar { align-items: flex-start; flex-direction: column; gap: 8px; }
}
</style>

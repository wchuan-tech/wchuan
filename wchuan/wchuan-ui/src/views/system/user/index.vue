<template>
  <div class="app-container" v-loading="loading">
    <el-card class="glass-card">
      <!-- 搜索栏 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true">
        <el-form-item label="用户账号" prop="userName">
          <el-input v-model="queryParams.userName" placeholder="请输入用户账号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="用户昵称" prop="nickName">
          <el-input v-model="queryParams.nickName" placeholder="请输入用户昵称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="账号状态" clearable style="width: 120px">
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
        <el-button v-if="hasPerm('system:user:add')" type="primary" icon="Plus" @click="handleAdd">新增用户</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="userList" border style="width: 100%">
        <el-table-column prop="id" label="用户ID" width="80" align="center" />
        <el-table-column prop="userName" label="用户账号" min-width="120" align="center" />
        <el-table-column prop="nickName" label="用户昵称" min-width="120" align="center" />
        <el-table-column prop="dept.deptName" label="所属部门" min-width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.dept" type="info">{{ row.dept.deptName }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="phoneNumber" label="手机号码" min-width="130" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" align="center" />

        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPerm('system:user:edit')" link type="primary" icon="Edit" @click="handleEdit(row)">修改</el-button>
            <el-button v-if="hasPerm('system:user:resetPwd')" link type="warning" icon="Key" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button v-if="hasPerm('system:user:remove') && row.id !== 1" link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 1. 新增/修改用户弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="600px" append-to-body @closed="resetForm">
      <el-form ref="userFormRef" :model="userForm" :rules="rules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <el-tree-select
                  v-model="userForm.deptId"
                  :data="deptOptions"
                  :props="{ label: 'deptName', value: 'deptId', children: 'children' }"
                  value-key="deptId"
                  placeholder="选择归属部门"
                  check-strictly
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联角色" prop="roleId">
              <el-select v-model="userForm.roleId" placeholder="选择角色" style="width: 100%">
                <el-option
                    v-for="item in roleOptions"
                    :key="item.id"
                    :label="item.roleName"
                    :value="item.id"
                    :disabled="!hasPerm('system:user:edit') || userForm.id === 1"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户账号" prop="userName">
              <el-input v-model="userForm.userName" placeholder="请输入用户账号" :disabled="!!userForm.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="userForm.nickName" placeholder="请输入用户昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="!userForm.id">
            <el-form-item label="登录密码" prop="password">
              <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phoneNumber">
              <el-input v-model="userForm.phoneNumber" placeholder="手机号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" placeholder="邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="userForm.sex" placeholder="选择性别" style="width: 100%">
                <el-option label="男" value="0" />
                <el-option label="女" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-radio-group v-model="userForm.status" :disabled="userForm.id === 1">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
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

    <!-- 🔑 2. 重置密码专属弹窗 -->
    <el-dialog title="重置密码" v-model="resetPwdOpen" width="400px" append-to-body>
      <el-form :model="resetPwdForm" label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="resetPwdForm.userName" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitResetPwd">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import request from '../../../utils/request'

const loading = ref(false)
const dialogOpen = ref(false)
const resetPwdOpen = ref(false)
const dialogTitle = ref('')
const userList = ref<any[]>([])
const deptOptions = ref<any[]>([])
const roleOptions = ref<any[]>([])
const userFormRef = ref<FormInstance>()

const queryParams = reactive({ userName: '', nickName: '', status: '' })

const userForm = ref<any>({
  id: undefined,
  deptId: undefined,
  roleId: undefined,
  userName: '',
  nickName: '',
  password: '',
  phoneNumber: '',
  email: '',
  sex: '0',
  status: '0'
})

const resetPwdForm = ref<any>({
  id: undefined,
  userName: '',
  newPassword: ''
})

const rules = reactive({
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  roleId: [{ required: true, message: '请选择关联角色', trigger: 'change' }],
  userName: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
  nickName: [{ required: true, message: '昵称不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
})

const hasPerm = (perm: string): boolean => {
  const permissions = JSON.parse(localStorage.getItem('permissions') || '[]')
  // 如果是超级管理员通配符 '*:*:*'，直接放行；否则匹配特定权限标识
  if (permissions.includes('*:*:*')) return true
  return permissions.includes(perm)
}

const getUserList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/system/user/list', { params: queryParams })
    if (res.code === 200) userList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const getDeptTree = async () => {
  const res: any = await request.get('/system/dept/list')
  if (res.code === 200) deptOptions.value = res.data || []
}

const getRoleOptions = async () => {
  const res: any = await request.get('/system/role/list')
  if (res.code === 200) roleOptions.value = res.data || []
}

const handleQuery = () => getUserList()
const resetQuery = () => { queryParams.userName = ''; queryParams.nickName = ''; queryParams.status = ''; handleQuery() }

const handleAdd = async () => {
  dialogTitle.value = '新增用户'
  await getDeptTree()
  await getRoleOptions()
  dialogOpen.value = true
}

const handleEdit = async (row: any) => {
  dialogTitle.value = '修改用户'
  await getDeptTree()
  await getRoleOptions()
  userForm.value = { ...row }
  dialogOpen.value = true
}

const handleResetPwd = (row: any) => {
  resetPwdForm.value = { id: row.id, userName: row.userName, newPassword: '' }
  resetPwdOpen.value = true
}

const submitForm = async () => {
  await userFormRef.value?.validate(async (valid) => {
    if (!valid) return
    const isEdit = !!userForm.value.id
    const method = isEdit ? 'put' : 'post'
    const res: any = await request[method]('/system/user', userForm.value)
    if (res.code === 200) {
      ElMessage.success(isEdit ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getUserList()
    }
  })
}

const submitResetPwd = async () => {
  if (!resetPwdForm.value.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  const res: any = await request.put('/system/user/resetPwd', {
    id: resetPwdForm.value.id,
    password: resetPwdForm.value.newPassword
  })
  if (res.code === 200) {
    ElMessage.success('密码重置成功')
    resetPwdOpen.value = false
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除用户 "${row.userName}" 吗?`, '警告', { type: 'warning' }).then(async () => {
    const res: any = await request.delete(`/system/user/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      getUserList()
    }
  })
}

const resetForm = () => {
  userForm.value = { id: undefined, deptId: undefined, roleId: undefined, userName: '', nickName: '', password: '', phoneNumber: '', email: '', sex: '0', status: '0' }
  userFormRef.value?.resetFields()
}

onMounted(() => getUserList())
</script>

<style scoped>
.app-container { padding: 20px; }
.mb-4 { margin-bottom: 16px; }
</style>
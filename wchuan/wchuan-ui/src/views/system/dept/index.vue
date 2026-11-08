<template>
  <div class="app-container" v-loading="loading">
    <el-card class="glass-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true">
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="queryParams.deptName" placeholder="请输入部门名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="部门状态" clearable style="width: 150px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-if="hasPerm('system:dept:add')" type="primary" icon="Plus" @click="handleAdd()">新增部门</el-button>
        <el-button icon="Sort" @click="toggleExpandAll">{{ isExpandAll ? '折叠全部' : '展开全部' }}</el-button>
      </div>

      <el-table
          v-if="refreshTable"
          :data="deptList"
          row-key="id"
          :default-expand-all="isExpandAll"
          :tree-props="{ children: 'children' }"
          border
          stripe>
        <el-table-column prop="deptName" label="部门名称" min-width="240" />
        <el-table-column prop="orderNum" label="排序" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPerm('system:dept:edit')" link type="primary" icon="Edit" @click="handleEdit(row)">修改</el-button>
            <el-button v-if="hasPerm('system:dept:add')" link type="primary" icon="Plus" @click="handleAdd(row)">新增子部门</el-button>
            <el-button v-if="hasPerm('system:dept:remove')" link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogOpen" :title="dialogTitle" width="640px" append-to-body @closed="resetForm">
      <el-form ref="deptFormRef" :model="deptForm" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="上级部门" prop="parentId">
              <el-tree-select
                  v-model="deptForm.parentId"
                  :data="deptOptions"
                  :props="{ label: 'deptName', value: 'id', children: 'children' }"
                  node-key="id"
                  check-strictly
                  default-expand-all
                  placeholder="请选择上级部门"
                  style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="deptForm.deptName" placeholder="请输入部门名称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="deptForm.orderNum" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="负责人" prop="leader"><el-input v-model="deptForm.leader" placeholder="请输入负责人" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="联系电话" prop="phone"><el-input v-model="deptForm.phone" placeholder="请输入联系电话" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="邮箱" prop="email"><el-input v-model="deptForm.email" placeholder="请输入邮箱" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="部门状态" prop="status">
              <el-radio-group v-model="deptForm.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { addDept, delDept, getDept, listDept, listDeptExcludeChild, updateDept, type Dept, type DeptForm, type DeptQuery } from '@/api/system/dept'

const loading = ref(false)
const submitting = ref(false)
const refreshTable = ref(true)
const isExpandAll = ref(true)
const dialogOpen = ref(false)
const dialogTitle = ref('')
const deptList = ref<Dept[]>([])
const deptOptions = ref<Dept[]>([])
const queryRef = ref<FormInstance>()
const deptFormRef = ref<FormInstance>()
const queryParams = reactive<DeptQuery>({ deptName: '', status: '' })
const deptForm = reactive<DeptForm>(emptyForm())

const phonePattern = /^(1[3-9]\d{9}|[0-9-]{6,20})$/
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const rules: FormRules = {
  parentId: [{ required: true, message: '请选择上级部门', trigger: 'change' }],
  deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
  orderNum: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
  phone: [{ validator: (_rule, value, callback) => !value || phonePattern.test(value) ? callback() : callback(new Error('联系电话格式不正确')), trigger: 'blur' }],
  email: [{ validator: (_rule, value, callback) => !value || emailPattern.test(value) ? callback() : callback(new Error('邮箱格式不正确')), trigger: 'blur' }]
}

function emptyForm(): DeptForm {
  return {
    id: undefined,
    deptId: undefined,
    parentId: 0,
    deptName: '',
    orderNum: 0,
    leader: '',
    phone: '',
    email: '',
    status: '0'
  }
}

const hasPerm = (permission: string) => {
  const permissions: string[] = JSON.parse(localStorage.getItem('permissions') || '[]')
  return localStorage.getItem('userId') === '1' || permissions.includes('*:*:*') || permissions.includes(permission)
}

const rootOption = (): Dept => ({ id: 0, deptId: 0, parentId: -1, deptName: '顶级部门', orderNum: 0, status: '0', children: [] })

const getDeptList = async () => {
  loading.value = true
  try {
    const res = await listDept(queryParams)
    deptList.value = res.data || []
  } finally { loading.value = false }
}

const handleQuery = () => void getDeptList()
const resetQuery = () => { queryRef.value?.resetFields(); void getDeptList() }
const toggleExpandAll = async () => { refreshTable.value = false; isExpandAll.value = !isExpandAll.value; await nextTick(); refreshTable.value = true }

const loadParentOptions = async (excludeId?: number) => {
  const res = excludeId ? await listDeptExcludeChild(excludeId) : await listDept()
  deptOptions.value = [rootOption(), ...(res.data || [])]
}

const handleAdd = async (parent?: Dept) => {
  Object.assign(deptForm, emptyForm(), {
    id: undefined,
    deptId: undefined,
    parentId: parent?.id ?? 0
  })
  dialogTitle.value = parent ? `新增 ${parent.deptName} 的子部门` : '新增部门'
  await loadParentOptions()
  dialogOpen.value = true
}

const handleEdit = async (row: Dept) => {
  const [detail] = await Promise.all([getDept(row.id), loadParentOptions(row.id)])
  Object.assign(deptForm, detail.data, { id: detail.data.id, deptId: detail.data.deptId })
  dialogTitle.value = '修改部门'
  dialogOpen.value = true
}

const submitForm = async () => {
  await deptFormRef.value?.validate()
  const isEdit = Boolean(deptForm.id || deptForm.deptId)
  submitting.value = true
  try {
    if (isEdit) await updateDept(deptForm)
    else await addDept(deptForm)
    ElMessage.success(isEdit ? '修改成功' : '新增成功')
    dialogOpen.value = false
    await getDeptList()
  } finally { submitting.value = false }
}

const handleDelete = async (row: Dept) => {
  await ElMessageBox.confirm(`确定删除部门“${row.deptName}”吗？`, '删除确认', { type: 'warning' })
  await delDept(row.id)
  ElMessage.success('删除成功')
  await getDeptList()
}

const resetForm = () => { Object.assign(deptForm, emptyForm()); deptFormRef.value?.clearValidate() }

onMounted(getDeptList)
</script>

<style scoped>
.app-container { padding: 20px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
@media (max-width: 768px) { .app-container { padding: 10px; } }
</style>

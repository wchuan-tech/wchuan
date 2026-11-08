<template>
  <div class="app-container" v-loading="loading">
    <el-card class="glass-card">
      <!-- 搜索栏 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true">
        <el-form-item label="公告标题" prop="noticeTitle">
          <el-input v-model="queryParams.noticeTitle" placeholder="请输入公告标题" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="公告类型" prop="noticeType">
          <el-select v-model="queryParams.noticeType" placeholder="公告类型" clearable style="width: 150px">
            <el-option label="通知" value="0" />
            <el-option label="公告" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮栏 -->
      <div class="mb-4">
        <el-button v-if="hasPerm('system:notice:add')" type="primary" icon="Plus" @click="handleAdd">新增公告</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="noticeList" border style="width: 100%">
        <el-table-column prop="id" label="序号" width="80" align="center" />
        <el-table-column prop="noticeTitle" label="公告标题" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column prop="noticeType" label="公告类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.noticeType === '0' ? 'primary' : 'warning'">
              {{ row.noticeType === '0' ? '通知' : '公告' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">
              {{ row.status === '0' ? '正常' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="创建者" width="120" align="center" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" align="center" />

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPerm('system:notice:edit')" link type="primary" icon="Edit" @click="handleEdit(row)">修改</el-button>
            <el-button v-if="hasPerm('system:notice:remove')" link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 增改弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body @closed="resetForm">
      <el-form ref="noticeFormRef" :model="noticeForm" :rules="rules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="公告标题" prop="noticeTitle">
              <el-input v-model="noticeForm.noticeTitle" placeholder="请输入公告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公告类型" prop="noticeType">
              <el-select v-model="noticeForm.noticeType" placeholder="请选择" style="width: 100%">
                <el-option label="通知" value="0" />
                <el-option label="公告" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="noticeForm.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">关闭</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="公告内容" prop="noticeContent">
              <el-input v-model="noticeForm.noticeContent" type="textarea" :rows="5" placeholder="请输入公告内容..." />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
// 🔑 重点：文件在 src/views/system/notice/index.vue，退3层才能到达 utils/request
import request from '../../../utils/request'

const loading = ref(false)
const dialogOpen = ref(false)
const dialogTitle = ref('')
const noticeList = ref<any[]>([])
const noticeFormRef = ref<FormInstance>()

const queryParams = reactive({ noticeTitle: '', noticeType: '' })

const noticeForm = ref<any>({
  id: undefined,
  noticeTitle: '',
  noticeType: '0',
  noticeContent: '',
  status: '0'
})

const rules = reactive({
  noticeTitle: [{ required: true, message: '公告标题不能为空', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }]
})

const hasPerm = (_perm: string) => true

const getNoticeList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/system/notice/list', { params: queryParams })
    if (res.code === 200) noticeList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleQuery = () => getNoticeList()
const resetQuery = () => { queryParams.noticeTitle = ''; queryParams.noticeType = ''; handleQuery() }

const handleAdd = () => {
  dialogTitle.value = '新增公告'
  dialogOpen.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '修改公告'
  noticeForm.value = { ...row }
  dialogOpen.value = true
}

const submitForm = async () => {
  await noticeFormRef.value?.validate(async (valid) => {
    if (!valid) return
    const isEdit = !!noticeForm.value.id
    const method = isEdit ? 'put' : 'post'
    const res: any = await request[method]('/system/notice', noticeForm.value)
    if (res.code === 200) {
      ElMessage.success(isEdit ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getNoticeList()
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除公告 "${row.noticeTitle}" 吗?`, '警告', { type: 'warning' }).then(async () => {
    const res: any = await request.delete(`/system/notice/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      getNoticeList()
    }
  })
}

const resetForm = () => {
  noticeForm.value = { id: undefined, noticeTitle: '', noticeType: '0', noticeContent: '', status: '0' }
  noticeFormRef.value?.resetFields()
}

onMounted(() => getNoticeList())
</script>

<style scoped>
.app-container { padding: 20px; }
.mb-4 { margin-bottom: 16px; }
</style>

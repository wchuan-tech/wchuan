<template>
  <div class="app-container" v-loading="loading">
    <el-card class="glass-card">
      <template #header>
        <div class="card-header">
          <div>
            <strong>公海客户池</strong>
            <span class="subtitle">发现机会，快速认领</span>
          </div>
          <el-tag type="warning" effect="plain">{{ total }} 条待认领</el-tag>
        </div>
      </template>

      <el-form ref="queryRef" :model="queryParams" :inline="true">
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="queryParams.customerName" placeholder="请输入客户名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="客户阶段" prop="customerStage">
          <el-select v-model="queryParams.customerStage" placeholder="全部阶段" clearable style="width: 130px">
            <el-option v-for="stage in stages" :key="stage" :label="stage" :value="stage" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="customerList" border stripe style="width: 100%">
        <el-table-column prop="customerName" label="客户名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="companyName" label="公司名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="135" align="center" />
        <el-table-column prop="wechat" label="微信" min-width="120" align="center" />
        <el-table-column prop="customerStage" label="阶段" width="95" align="center">
          <template #default="{ row }"><el-tag :type="stageType(row.customerStage)">{{ row.customerStage }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="lastFollowupTime" label="最后跟进" width="165" align="center">
          <template #default="{ row }">{{ row.lastFollowupTime || '暂无跟进' }}</template>
        </el-table-column>
        <el-table-column prop="lastFollowupRecord" label="跟进摘要" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="Aim" @click="handleClaim(row)">认领/抢单</el-button>
            <el-button v-if="hasPerm('crm:customer:assign')" link type="warning" icon="UserFilled" @click="openAssign(row)">分配</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList" @current-change="getList" />
      </div>
    </el-card>

    <el-dialog v-model="assignOpen" title="分配客户" width="460px" append-to-body @closed="assignFormRef?.resetFields()">
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="90px">
        <el-form-item label="客户"><el-input :model-value="selectedCustomer?.customerName" disabled /></el-form-item>
        <el-form-item label="目标销售" prop="targetUserId">
          <el-select v-model="assignForm.targetUserId" placeholder="请选择销售" filterable style="width: 100%">
            <el-option v-for="user in salesOptions" :key="user.id" :label="user.nickName || user.userName" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TagProps } from 'element-plus'
import { assignCustomer, claimCustomer, getPublicPoolList, getSalesOptions, type Customer, type CustomerQuery, type CustomerStage, type SalesOption } from '@/api/crm/customer'

const stages: CustomerStage[] = ['线索', '初访', '意向', '报价', '签约']
const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const customerList = ref<Customer[]>([])
const salesOptions = ref<SalesOption[]>([])
const queryRef = ref<FormInstance>()
const assignFormRef = ref<FormInstance>()
const assignOpen = ref(false)
const selectedCustomer = ref<Customer>()
const queryParams = reactive<CustomerQuery>({ pageNum: 1, pageSize: 10, customerName: '', phone: '', customerStage: '' })
const assignForm = reactive<{ targetUserId?: number }>({ targetUserId: undefined })
const assignRules: FormRules = { targetUserId: [{ required: true, message: '请选择目标销售', trigger: 'change' }] }

const hasPerm = (permission: string) => {
  const permissions: string[] = JSON.parse(localStorage.getItem('permissions') || '[]')
  return localStorage.getItem('userId') === '1' || permissions.includes('*:*:*') || permissions.includes(permission)
}

const stageType = (stage: CustomerStage): TagProps['type'] => ({ 线索: 'info', 初访: '', 意向: 'warning', 报价: 'primary', 签约: 'success' })[stage] as TagProps['type']

const getList = async () => {
  loading.value = true
  try {
    const res = await getPublicPoolList(queryParams)
    customerList.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

const handleQuery = () => { queryParams.pageNum = 1; void getList() }
const resetQuery = () => { queryRef.value?.resetFields(); queryParams.pageNum = 1; void getList() }

const handleClaim = async (row: Customer) => {
  await ElMessageBox.confirm(`确认认领客户“${row.customerName}”到我的私海吗？`, '认领确认', { type: 'warning' })
  await claimCustomer(row.id)
  ElMessage.success('认领成功')
  await getList()
}

const openAssign = async (row: Customer) => {
  selectedCustomer.value = row
  assignForm.targetUserId = undefined
  const res = await getSalesOptions()
  salesOptions.value = res.data || []
  assignOpen.value = true
}

const submitAssign = async () => {
  await assignFormRef.value?.validate()
  if (!selectedCustomer.value || !assignForm.targetUserId) return
  submitting.value = true
  try {
    await assignCustomer({ customerId: selectedCustomer.value.id, targetUserId: assignForm.targetUserId })
    ElMessage.success('分配成功')
    assignOpen.value = false
    await getList()
  } finally { submitting.value = false }
}

onMounted(getList)
</script>

<style scoped>
.app-container { padding: 20px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.subtitle { margin-left: 12px; color: #909399; font-size: 13px; font-weight: 400; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 18px; }
</style>

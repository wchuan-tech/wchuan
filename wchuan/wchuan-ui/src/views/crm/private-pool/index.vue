<template>
  <div class="app-container" v-loading="loading">
    <el-card class="glass-card">
      <template #header>
        <div class="card-header">
          <div><strong>私海客户</strong><span class="subtitle">聚焦跟进，推动客户转化</span></div>
          <el-button v-if="hasPerm('crm:customer:add')" type="primary" icon="Plus" @click="openCreate">新增客户</el-button>
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
        <el-table-column prop="companyName" label="公司名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" align="center" />
        <el-table-column prop="ownerUserName" label="归属销售" width="110" align="center" />
        <el-table-column prop="customerStage" label="阶段" width="90" align="center">
          <template #default="{ row }"><el-tag :type="stageType(row.customerStage)">{{ row.customerStage }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="lockStatus" label="防掉状态" width="105" align="center">
          <template #default="{ row }"><el-tag :type="row.lockStatus === 1 ? 'success' : 'info'">{{ row.lockStatus === 1 ? '已锁定' : '未锁定' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="lastFollowupTime" label="最后跟进" width="165" align="center">
          <template #default="{ row }">{{ row.lastFollowupTime || '暂无跟进' }}</template>
        </el-table-column>
        <el-table-column prop="lastFollowupRecord" label="跟进摘要" min-width="190" show-overflow-tooltip />
        <el-table-column label="操作" width="320" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="EditPen" @click="openFollowup(row)">写跟进</el-button>
            <el-button link type="success" icon="Clock" @click="openTimeline(row)">查看轨迹</el-button>
            <el-button v-if="hasPerm('crm:customer:transfer')" link type="warning" icon="Switch" @click="openTransfer(row)">移交</el-button>
            <el-button link type="danger" icon="Back" @click="handleReturn(row)">退回公海</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
                       :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper"
                       @size-change="getList" @current-change="getList" />
      </div>
    </el-card>

    <!-- 1. 新增客户弹窗 -->
    <el-dialog v-model="createOpen" title="新增客户" width="620px" append-to-body @closed="createFormRef?.resetFields()">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-row :gutter="18">
          <el-col :xs="24" :sm="12"><el-form-item label="客户名称" prop="customerName"><el-input v-model="createForm.customerName" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12"><el-form-item label="手机号" prop="phone"><el-input v-model="createForm.phone" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12"><el-form-item label="微信号" prop="wechat"><el-input v-model="createForm.wechat" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12"><el-form-item label="客户阶段" prop="customerStage"><el-select v-model="createForm.customerStage" style="width: 100%"><el-option v-for="stage in stages" :key="stage" :label="stage" :value="stage" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="公司名称" prop="companyName"><el-input v-model="createForm.companyName" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="createOpen = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitCreate">确认新增</el-button></template>
    </el-dialog>

    <!-- 2. 记录客户跟进弹窗 -->
    <el-dialog v-model="followupOpen" title="记录客户跟进" width="600px" append-to-body @closed="followupFormRef?.resetFields()">
      <el-alert :title="`当前客户：${selectedCustomer?.customerName || ''}`" type="info" :closable="false" class="dialog-alert" />
      <el-form ref="followupFormRef" :model="followupForm" :rules="followupRules" label-width="100px">
        <el-form-item label="跟进方式" prop="followupType">
          <el-radio-group v-model="followupForm.followupType">
            <el-radio-button value="电话">电话</el-radio-button>
            <el-radio-button value="拜访">拜访</el-radio-button>
            <el-radio-button value="微信">微信</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 🔑 改动点 1：增加更新客户阶段单选组 -->
        <!-- 限制：只能选择当前阶段或更高阶的阶段，禁止向后倒退 -->
        <el-form-item label="更新阶段" prop="customerStage">
          <el-radio-group v-model="followupForm.customerStage" size="small">
            <el-radio-button
                v-for="stage in stages"
                :key="stage"
                :value="stage"
                :disabled="isStageDisabled(stage, selectedCustomer?.customerStage)"
            >
              {{ stage }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="跟进内容" prop="content">
          <el-input v-model="followupForm.content" type="textarea" :rows="5" maxlength="2000" show-word-limit placeholder="请输入详细的沟通内容..." />
        </el-form-item>
        <el-form-item label="下次跟进" prop="nextFollowupTime">
          <el-date-picker v-model="followupForm.nextFollowupTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择下次跟进时间" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="followupOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitFollowup">保存跟进</el-button>
      </template>

    </el-dialog>

    <!-- 3. 移交客户弹窗 -->
    <el-dialog v-model="transferOpen" title="移交客户" width="460px" append-to-body @closed="transferFormRef?.resetFields()">
      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="90px">
        <el-form-item label="客户"><el-input :model-value="selectedCustomer?.customerName" disabled /></el-form-item>
        <el-form-item label="目标成员" prop="targetUserId">
          <el-select v-model="transferForm.targetUserId" placeholder="请选择团队成员" filterable style="width: 100%">
            <el-option v-for="user in availableSales" :key="user.id" :label="user.nickName || user.userName" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="transferOpen = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitTransfer">确认移交</el-button></template>
    </el-dialog>

    <!-- 4. 查看跟进轨迹抽屉 -->
    <el-drawer v-model="timelineOpen" :title="`${selectedCustomer?.customerName || ''} · 跟进轨迹`" size="min(520px, 92vw)">
      <div v-loading="timelineLoading" class="timeline-content">
        <el-empty v-if="!timelineLoading && followupList.length === 0" description="暂无跟进记录" />
        <el-timeline v-else>
          <el-timeline-item v-for="item in followupList" :key="item.id" :timestamp="item.createTime" placement="top" :type="followupTypeColor(item.followupType)">
            <el-card shadow="hover" class="timeline-card">
              <div class="timeline-title">
                <el-tag size="small">{{ item.followupType }}</el-tag>
                <!-- 🔑 改动点 4：时间轴上展示当时跟进后的阶段 -->
                <el-tag v-if="item.customerStage" size="small" :type="stageType(item.customerStage)" effect="plain">
                  {{ item.customerStage }}
                </el-tag>
                <span>{{ item.createBy || '系统' }}</span>
              </div>
              <p>{{ item.content }}</p>
              <small v-if="item.nextFollowupTime">下次跟进：{{ item.nextFollowupTime }}</small>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TagProps, type TimelineItemProps } from 'element-plus'
import { addFollowup, createCustomer, getFollowupList, getPrivatePoolList, getSalesOptions, returnCustomerToPool, transferCustomer, type Customer, type CustomerCreatePayload, type CustomerQuery, type CustomerStage, type FollowupCreatePayload, type FollowupRecord, type FollowupType, type SalesOption } from '@/api/crm/customer'

const stages: CustomerStage[] = ['线索', '初访', '意向', '报价', '签约', '输单']
const currentUserId = Number(localStorage.getItem('userId'))
const loading = ref(false)
const submitting = ref(false)
const timelineLoading = ref(false)
const total = ref(0)
const customerList = ref<Customer[]>([])
const followupList = ref<FollowupRecord[]>([])
const salesOptions = ref<SalesOption[]>([])
const selectedCustomer = ref<Customer>()
const queryRef = ref<FormInstance>()
const createFormRef = ref<FormInstance>()
const followupFormRef = ref<FormInstance>()
const transferFormRef = ref<FormInstance>()
const createOpen = ref(false)
const followupOpen = ref(false)
const transferOpen = ref(false)
const timelineOpen = ref(false)

const queryParams = reactive<CustomerQuery>({ pageNum: 1, pageSize: 10, customerName: '', phone: '', customerStage: '' })
const createForm = reactive<CustomerCreatePayload>({ customerName: '', phone: '', wechat: '', companyName: '', customerStage: '线索' })

// 🔑 确保类型与初始化完整
const followupForm = reactive<Omit<FollowupCreatePayload, 'customerId'>>({
  followupType: '电话',
  customerStage: '线索',
  content: '',
  nextFollowupTime: ''
})

const transferForm = reactive<{ targetUserId?: number }>({ targetUserId: undefined })
const createRules: FormRules = { customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }], customerStage: [{ required: true, message: '请选择客户阶段', trigger: 'change' }] }

// 🔑 改动点 3：增加 customerStage 必填校验
const followupRules: FormRules = {
  followupType: [{ required: true, message: '请选择跟进方式', trigger: 'change' }],
  customerStage: [{ required: true, message: '请选择更新后的客户阶段', trigger: 'change' }],
  content: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }]
}

const transferRules: FormRules = { targetUserId: [{ required: true, message: '请选择目标成员', trigger: 'change' }] }
const availableSales = computed(() => salesOptions.value.filter(user => user.id !== currentUserId))

const hasPerm = (permission: string) => { const permissions: string[] = JSON.parse(localStorage.getItem('permissions') || '[]'); return currentUserId === 1 || permissions.includes('*:*:*') || permissions.includes(permission) }


const stageType = (stage: CustomerStage): TagProps['type'] => ({
  线索: 'info',
  初访: '',
  意向: 'warning',
  报价: 'primary',
  签约: 'success',
  输单: 'danger' // 👈 🔑 输单显示为红色
})[stage] as TagProps['type']// 🔑 核心补全：获取当前阶段对应的索引下标（用于限制阶段不可向后倒退）

// 🔑 阶段限制：只能维持当前阶段、推进到紧挨着的下一个阶段，或者直接输单！
const isStageDisabled = (targetStage: CustomerStage, currentStage?: CustomerStage): boolean => {
  if (!currentStage) return false

  // 1. “输单”作为流失终态，随时都可以点击
  if (targetStage === '输单') return false

  // 2. 如果当前已经是终态（已签约 或 已输单），全部禁止再点击
  if (currentStage === '签约' || currentStage === '输单') return true

  const currentIndex = stages.indexOf(currentStage)
  const targetIndex = stages.indexOf(targetStage)

  // 3. 核心限制：
  // 只能选【当前阶段】(targetIndex === currentIndex)
  // 或者选【紧邻的下一个阶段】(targetIndex === currentIndex + 1)
  // 既禁止向后倒退，也禁止向前跨级跳跃！
  return targetIndex < currentIndex || targetIndex > currentIndex + 1
}


const followupTypeColor = (type: FollowupType): TimelineItemProps['type'] => type === '系统' ? 'info' : type === '拜访' ? 'success' : type === '微信' ? 'primary' : 'warning'

const getList = async () => { loading.value = true; try { const res = await getPrivatePoolList(queryParams); customerList.value = res.data.records || []; total.value = res.data.total || 0 } finally { loading.value = false } }
const handleQuery = () => { queryParams.pageNum = 1; void getList() }
const resetQuery = () => { queryRef.value?.resetFields(); queryParams.pageNum = 1; void getList() }

const openCreate = () => { Object.assign(createForm, { customerName: '', phone: '', wechat: '', companyName: '', customerStage: '线索' }); createOpen.value = true }
const submitCreate = async () => { await createFormRef.value?.validate(); submitting.value = true; try { await createCustomer(createForm); ElMessage.success('客户新增成功'); createOpen.value = false; await getList() } finally { submitting.value = false } }

// 🔑 改动点 2：打开跟进弹窗时，将客户现有阶段自动回显选中
const openFollowup = (row: Customer) => {
  selectedCustomer.value = row
  Object.assign(followupForm, {
    followupType: '电话',
    customerStage: row.customerStage || '线索',
    content: '',
    nextFollowupTime: ''
  })
  followupOpen.value = true
}

const submitFollowup = async () => {
  await followupFormRef.value?.validate()
  if (!selectedCustomer.value) return
  submitting.value = true
  try {
    await addFollowup({ customerId: selectedCustomer.value.id, ...followupForm })
    ElMessage.success('跟进记录已保存')
    followupOpen.value = false
    await getList()
  } finally {
    submitting.value = false
  }
}

const openTimeline = async (row: Customer) => { selectedCustomer.value = row; timelineOpen.value = true; timelineLoading.value = true; try { const res = await getFollowupList(row.id); followupList.value = res.data || [] } finally { timelineLoading.value = false } }

const handleReturn = async (row: Customer) => { await ElMessageBox.confirm(`确认将客户“${row.customerName}”退回公海吗？`, '退回确认', { type: 'warning' }); await returnCustomerToPool(row.id); ElMessage.success('客户已退回公海'); await getList() }

const openTransfer = async (row: Customer) => { selectedCustomer.value = row; transferForm.targetUserId = undefined; const res = await getSalesOptions(); salesOptions.value = res.data || []; transferOpen.value = true }
const submitTransfer = async () => { await transferFormRef.value?.validate(); if (!selectedCustomer.value || !transferForm.targetUserId) return; submitting.value = true; try { await transferCustomer({ customerId: selectedCustomer.value.id, targetUserId: transferForm.targetUserId }); ElMessage.success('客户移交成功'); transferOpen.value = false; await getList() } finally { submitting.value = false } }

onMounted(getList)
</script>

<style scoped>
.app-container { padding: 20px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.subtitle { margin-left: 12px; color: #909399; font-size: 13px; font-weight: 400; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 18px; overflow-x: auto; }
.dialog-alert { margin-bottom: 18px; }
.timeline-content { min-height: 180px; padding: 8px 8px 8px 0; }
.timeline-card p { line-height: 1.7; color: #303133; white-space: pre-wrap; }
.timeline-card small { color: #909399; }
.timeline-title { display: flex; align-items: center; gap: 10px; color: #606266; }
@media (max-width: 768px) { .app-container { padding: 10px; } .subtitle { display: none; } }
</style>
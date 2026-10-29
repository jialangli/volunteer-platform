<template>
  <div>
    <el-card shadow="never" class="bar">
      <div class="bar-inner">
        <el-button @click="router.back()">返回</el-button>
        <span class="act-title">{{ actTitle }}</span>
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="load(1)">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </div>
    </el-card>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="报名时间" min-width="150">
        <template #default="{ row }">{{ fmt(row.applyTime) }}</template>
      </el-table-column>
      <el-table-column prop="auditReason" label="审核意见" min-width="120" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="audit(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="audit(row, false)">拒绝</el-button>
          </template>
          <span v-else class="done">已处理</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRegistrations, auditRegistration, getActivity } from '../../api'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const status = ref('')
const query = ref({ page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const actTitle = ref('')

const statusMap = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }
function statusText(s) { return statusMap[s] || s }
function statusType(s) { return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[s] || 'info' }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function load(page) {
  query.value.page = page
  loading.value = true
  try {
    const data = await listRegistrations(id, { ...query.value, status: status.value || undefined })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function audit(row, approve) {
  const action = approve ? '通过' : '拒绝'
  let reason = ''
  if (!approve) {
    const r = await ElMessageBox.prompt('请输入拒绝原因', '拒绝报名', { inputPattern: /.+/, inputErrorMessage: '请填写原因' })
    reason = r.value
  }
  try {
    await auditRegistration(row.id, { approve, reason })
    ElMessage.success(`已${action} ${row.realName} 的报名`)
    load(query.value.page)
  } catch (e) { /* 已统一提示 */ }
}

onMounted(async () => {
  try {
    const a = await getActivity(id)
    actTitle.value = a.title
  } catch (e) { /* 忽略 */ }
  load(1)
})
</script>

<style scoped>
.bar { margin-bottom: 16px; }
.bar-inner { display: flex; gap: 12px; align-items: center; }
.act-title { font-weight: 600; color: #1f2d3d; }
.done { color: #909399; font-size: 13px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>

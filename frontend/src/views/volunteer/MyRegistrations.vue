<template>
  <el-card shadow="never">
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="activityTitle" label="活动" min-width="180" />
      <el-table-column label="时间" min-width="200">
        <template #default="{ row }">{{ fmt(row.activityStartTime) }} ~ {{ fmt(row.activityEndTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="applyTime" label="报名时间" min-width="150">
        <template #default="{ row }">{{ fmt(row.applyTime) }}</template>
      </el-table-column>
      <el-table-column label="审核意见" min-width="120">
        <template #default="{ row }">{{ row.auditReason || (row.auditTime ? '—' : '待审核') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button v-if="['PENDING', 'APPROVED'].includes(row.status)" size="small" type="danger" plain @click="cancel(row)">取消报名</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myRegistrations, cancelEnroll } from '../../api'

const query = reactive({ page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const statusMap = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }

function statusText(s) { return statusMap[s] || s }
function statusType(s) { return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[s] || 'info' }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function load(page) {
  query.page = page
  loading.value = true
  try {
    const data = await myRegistrations(query)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function cancel(row) {
  await ElMessageBox.confirm(`确认取消报名「${row.activityTitle}」？`, '取消报名', { type: 'warning' })
  try {
    await cancelEnroll(row.activityId)
    ElMessage.success('已取消报名')
    load(query.page)
  } catch (e) { /* 已统一提示 */ }
}

onMounted(() => load(1))
</script>

<style scoped>
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>

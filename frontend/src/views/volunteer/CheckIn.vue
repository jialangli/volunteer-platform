<template>
  <div>
    <el-card shadow="never">
      <template #header>已通过报名的活动（可签到/签退）</template>
      <el-table :data="approved" v-loading="loading" stripe>
        <el-table-column prop="activityTitle" label="活动" min-width="160" />
        <el-table-column label="签到窗口" min-width="200">
          <template #default="{ row }">{{ fmt(row.signinStart) }} ~ {{ fmt(row.signinEnd) }}</template>
        </el-table-column>
        <el-table-column label="签退截止" min-width="150">
          <template #default="{ row }">{{ fmt(row.signoutEnd) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :disabled="row.checkedIn" @click="doSignin(row)">签到</el-button>
            <el-button size="small" type="warning" :disabled="!row.checkedIn || row.checkedOut" @click="doSignout(row)">签退</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt">
      <template #header>我的签到记录与工时</template>
      <el-table :data="records" v-loading="loading2" stripe>
        <el-table-column prop="activityTitle" label="活动" min-width="160" />
        <el-table-column label="签到时间" min-width="150">
          <template #default="{ row }">{{ fmt(row.signinTime) }}</template>
        </el-table-column>
        <el-table-column label="签退时间" min-width="150">
          <template #default="{ row }">{{ row.signoutTime ? fmt(row.signoutTime) : '未签退' }}</template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="100" />
        <el-table-column label="核算工时" width="100">
          <template #default="{ row }">{{ row.hours != null ? row.hours + 'h' : '—' }}</template>
        </el-table-column>
        <el-table-column label="签到方式" width="100">
          <template #default="{ row }">{{ { QR: '扫码', GEO: '定位', MANUAL: '代签' }[row.signinMethod] || row.signinMethod }}</template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="prev, pager, next, total" :total="total2" :page-size="q2.size" :current-page="q2.page" @current-change="loadRecords" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { myRegistrations, signin, signout, myAttendance } from '../../api'

const loading = ref(false)
const loading2 = ref(false)
const regs = ref([])
const records = ref([])
const q2 = reactive({ page: 1, size: 10 })
const total2 = ref(0)
const signinTime = ref({}) // activityId -> 记录，避免重复点击

function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

const approved = computed(() =>
  regs.value.filter((r) => r.status === 'APPROVED').map((r) => ({
    ...r,
    checkedIn: !!signinTime.value[r.activityId],
    checkedOut: signinTime.value[r.activityId]?.signoutTime != null
  }))
)

async function loadRegs() {
  loading.value = true
  try {
    const data = await myRegistrations({ page: 1, size: 50 })
    regs.value = data.list
    const att = await myAttendance({ page: 1, size: 50 })
    const map = {}
    att.list.forEach((a) => { map[a.activityId] = a })
    signinTime.value = map
  } finally {
    loading.value = false
  }
}

async function doSignin(row) {
  try {
    await signin(row.activityId, { method: 'QR' })
    ElMessage.success('签到成功')
    loadRegs()
    loadRecords(1)
  } catch (e) { /* 已统一提示 */ }
}

async function doSignout(row) {
  try {
    await signout(row.activityId)
    ElMessage.success('签退成功，工时已核算')
    loadRegs()
    loadRecords(1)
  } catch (e) { /* 已统一提示 */ }
}

async function loadRecords(page) {
  q2.page = page
  loading2.value = true
  try {
    const data = await myAttendance(q2)
    records.value = data.list
    total2.value = data.total
  } finally {
    loading2.value = false
  }
}

onMounted(() => { loadRegs(); loadRecords(1) })
</script>

<style scoped>
.mt { margin-top: 16px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>

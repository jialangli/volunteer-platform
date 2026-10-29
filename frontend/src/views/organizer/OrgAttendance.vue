<template>
  <div>
    <el-card shadow="never" class="bar">
      <div class="bar-inner">
        <el-button @click="router.back()">返回</el-button>
        <span class="act-title">{{ actTitle }}</span>
        <el-button type="success" plain @click="showQr">签到二维码</el-button>
        <span class="tip">支持对已通过报名且未签到的志愿者代签（手动签到）；志愿者也可扫码签到</span>
      </div>
    </el-card>

    <el-dialog v-model="qrVisible" title="扫码签到" width="380px">
      <div class="qr-box">
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="签到二维码" />
        <p class="qr-tip">志愿者使用浏览器扫码（或直接访问下方链接）完成签到，签到方式记为「扫码」。</p>
        <p class="qr-url">{{ qrUrl }}</p>
      </div>
    </el-dialog>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column label="签到状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.signinTime ? 'success' : 'info'" size="small">{{ row.signinTime ? '已签到' : '未签到' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="签到时间" min-width="150">
        <template #default="{ row }">{{ fmt(row.signinTime) }}</template>
      </el-table-column>
      <el-table-column label="签退时间" min-width="150">
        <template #default="{ row }">{{ fmt(row.signoutTime) }}</template>
      </el-table-column>
      <el-table-column label="时长(分)" width="100">
        <template #default="{ row }">{{ row.durationMinutes ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="核算工时" width="100">
        <template #default="{ row }">{{ row.hours != null ? row.hours + 'h' : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.checkedIn" size="small" type="primary" plain @click="manual(row)">代签</el-button>
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
import QRCode from 'qrcode'
import { activityAttendance, signin, getActivity } from '../../api'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const query = ref({ page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const actTitle = ref('')
const qrVisible = ref(false)
const qrDataUrl = ref('')
const qrUrl = ref('')

function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function load(page) {
  query.value.page = page
  loading.value = true
  try {
    const data = await activityAttendance(id, query.value)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function manual(row) {
  await ElMessageBox.confirm(`确认为 ${row.realName}（学号 ${row.studentNo}）手动签到？`, '手动签到', { type: 'warning' })
  try {
    await signin(id, { userId: row.userId, method: 'MANUAL' })
    ElMessage.success('代签成功')
    load(query.value.page)
  } catch (e) { /* 已统一提示 */ }
}

async function showQr() {
  qrUrl.value = `${location.origin}/scan/${id}`
  qrDataUrl.value = await QRCode.toDataURL(qrUrl.value, { width: 260, margin: 2 })
  qrVisible.value = true
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
.bar-inner { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.act-title { font-weight: 600; color: #1f2d3d; }
.tip { color: #909399; font-size: 12px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
.qr-box { text-align: center; }
.qr-box img { width: 260px; height: 260px; }
.qr-tip { color: #909399; font-size: 12px; margin-top: 8px; }
.qr-url { color: #606266; font-size: 12px; margin-top: 4px; word-break: break-all; }
</style>

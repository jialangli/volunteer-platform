<template>
  <div>
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-select v-model="query.typeId" placeholder="活动类型" clearable style="width: 160px" @change="load(1)">
          <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="活动状态" clearable style="width: 160px" @change="load(1)">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索活动标题" clearable style="width: 220px" @keyup.enter="load(1)" @clear="load(1)" />
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="a in list" :key="a.id" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="act-card">
          <div class="act-cover">
            <img v-if="a.coverUrl" :src="a.coverUrl" alt="活动封面" />
            <div v-else class="cover-placeholder">
              <el-icon><Picture /></el-icon><span>暂无封面</span>
            </div>
          </div>
          <div class="act-body">
            <div class="act-head">
              <span class="act-title">{{ a.title }}</span>
              <el-tag :type="statusType(a.status)" size="small">{{ statusText(a.status) }}</el-tag>
            </div>
            <p class="act-meta">
              <el-icon><Location /></el-icon>{{ a.location || '未指定地点' }}
            </p>
            <p class="act-meta">
              <el-icon><Clock /></el-icon>{{ fmt(a.startTime) }} ~ {{ fmt(a.endTime) }}
            </p>
            <p class="act-meta">
              <el-icon><Collection /></el-icon>{{ a.typeName || '未分类' }} · 已报名 {{ a.registeredCount }}{{ a.capacity > 0 ? ' / ' + a.capacity : '' }} 人
            </p>
            <div class="act-foot">
              <span class="act-org">{{ a.organizerName || '平台' }}</span>
              <el-button size="small" type="primary" :disabled="!canEnroll(a)" @click="doEnroll(a)">
                {{ enrollBtnText(a) }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && list.length === 0" description="暂无活动" />
    <div class="pager">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Clock, Collection, Picture } from '@element-plus/icons-vue'
import { listActivities, enroll, adminTypes } from '../../api'

const query = reactive({ page: 1, size: 9, typeId: null, status: '', keyword: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const types = ref([])

const statusOptions = [
  { value: 'RECRUITING', label: '招募中' },
  { value: 'ONGOING', label: '进行中' },
  { value: 'FINISHED', label: '已结束' }
]
const statusMap = { DRAFT: '草稿', RECRUITING: '招募中', ONGOING: '进行中', FINISHED: '已结束', ARCHIVED: '已归档', CANCELLED: '已取消' }

function statusText(s) { return statusMap[s] || s }
function statusType(s) {
  return { DRAFT: 'info', RECRUITING: 'success', ONGOING: 'primary', FINISHED: 'warning', CANCELLED: 'danger', ARCHIVED: 'info' }[s] || 'info'
}
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }
function canEnroll(a) {
  return a.status === 'RECRUITING' && !a.myRegistrationStatus && (a.capacity === 0 || a.capacityRemaining > 0)
}
function enrollBtnText(a) {
  if (a.myRegistrationStatus) return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }[a.myRegistrationStatus] || '已报名'
  if (a.status !== 'RECRUITING') return '不可报名'
  if (a.capacity > 0 && a.capacityRemaining <= 0) return '已满员'
  return '我要报名'
}

async function load(page) {
  query.page = page
  loading.value = true
  try {
    const data = await listActivities(query)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function doEnroll(a) {
  await ElMessageBox.confirm(`确认报名「${a.title}」？`, '报名确认', { type: 'warning' })
  try {
    await enroll(a.id)
    ElMessage.success('报名成功，请等待组织者审核')
    load(query.page)
  } catch (e) { /* 错误已统一提示 */ }
}

onMounted(async () => {
  try { types.value = await adminTypes() } catch (e) { /* 类型非必需 */ }
  load(1)
})
</script>

<style scoped>
.filter-card { margin-bottom: 16px; }
.filter-bar { display: flex; gap: 12px; flex-wrap: wrap; }
.act-card { margin-bottom: 16px; padding: 0; }
.act-cover { width: 100%; height: 140px; overflow: hidden; border-radius: 4px 4px 0 0; }
.act-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.cover-placeholder { width: 100%; height: 100%; background: linear-gradient(135deg, #f0f4f8, #e2e8f0); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; color: #94a3b8; font-size: 13px; }
.act-body { padding: 12px 16px 14px; }
.act-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; }
.act-title { font-size: 16px; font-weight: 600; color: #1f2d3d; }
.act-meta { margin-top: 8px; color: #64748b; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.act-foot { margin-top: 12px; display: flex; justify-content: space-between; align-items: center; }
.act-org { color: #909399; font-size: 13px; }
.pager { display: flex; justify-content: flex-end; margin-top: 8px; }
</style>

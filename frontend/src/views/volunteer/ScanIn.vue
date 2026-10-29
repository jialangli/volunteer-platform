<template>
  <div class="scan-page">
    <div class="scan-card" v-loading="loading">
      <template v-if="activity">
        <div class="icon-circle" :class="done ? 'done' : ''">
          <el-icon :size="36"><component :is="done ? 'SuccessFilled' : 'Checked'" /></el-icon>
        </div>
        <h2 class="title">{{ done ? '签到成功' : '扫码签到' }}</h2>
        <p class="sub">{{ done ? '您已完成该活动签到' : '请确认活动信息后完成签到' }}</p>

        <div class="act-info">
          <p class="act-name">{{ activity.title }}</p>
          <p class="act-meta"><el-icon><Clock /></el-icon>{{ fmt(activity.startTime) }} ~ {{ fmt(activity.endTime) }}</p>
          <p class="act-meta"><el-icon><Location /></el-icon>{{ activity.location || '未指定地点' }}</p>
          <p class="act-meta"><el-icon><Collection /></el-icon>{{ activity.typeName || '未分类' }}</p>
        </div>

        <el-button v-if="!done" type="primary" size="large" class="sign-btn" :loading="signing" @click="doSignin">
          确认签到（扫码方式）
        </el-button>
        <el-button v-else type="success" size="large" class="sign-btn" @click="router.push('/my/checkin')">
          查看我的签到记录
        </el-button>

        <p v-if="!done" class="tip">签到方式将记录为「扫码」；请确保已报名且通过审核。</p>
      </template>
      <el-empty v-else-if="!loading" description="活动不存在或已删除" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, Location, Collection, Checked, SuccessFilled } from '@element-plus/icons-vue'
import { getActivity, signin } from '../../api'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const activity = ref(null)
const loading = ref(true)
const signing = ref(false)
const done = ref(false)

function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function doSignin() {
  signing.value = true
  try {
    await signin(id, { method: 'QR' })
    done.value = true
    ElMessage.success('签到成功')
  } catch (e) {
    // 已签到视为成功
    const msg = e?.response?.data?.message || ''
    if (msg.includes('已签到') || msg.includes('已完成签到')) {
      done.value = true
      ElMessage.success('您已完成签到')
    }
  } finally {
    signing.value = false
  }
}

onMounted(async () => {
  try {
    activity.value = await getActivity(id)
  } catch (e) { /* 活动不存在 */ } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.scan-page { min-height: 100vh; background: linear-gradient(135deg, #eef4ff, #f8fafc); display: flex; align-items: center; justify-content: center; padding: 20px; }
.scan-card { background: #fff; border-radius: 16px; box-shadow: 0 8px 32px rgba(30, 60, 114, 0.12); width: 400px; max-width: 100%; padding: 36px 28px; text-align: center; }
.icon-circle { width: 72px; height: 72px; margin: 0 auto 16px; border-radius: 50%; background: #e8f3ff; color: #409eff; display: flex; align-items: center; justify-content: center; }
.icon-circle.done { background: #ecfdf5; color: #67c23a; }
.title { font-size: 20px; color: #1f2d3d; margin: 0; }
.sub { color: #909399; font-size: 13px; margin: 8px 0 20px; }
.act-info { background: #f8fafc; border: 1px solid #eef2f7; border-radius: 10px; padding: 16px; margin-bottom: 20px; }
.act-name { font-size: 16px; font-weight: 600; color: #1f2d3d; margin: 0 0 10px; }
.act-meta { color: #64748b; font-size: 13px; margin: 6px 0; display: flex; align-items: center; justify-content: center; gap: 4px; }
.sign-btn { width: 100%; margin-top: 4px; }
.tip { color: #a0aec0; font-size: 12px; margin-top: 12px; }
</style>

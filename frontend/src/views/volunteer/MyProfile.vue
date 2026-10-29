<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <el-card shadow="never" v-loading="loading">
          <template #header>我的档案</template>
          <div class="profile-center" v-if="profile">
            <el-avatar :size="72">{{ (profile.realName || profile.username || '?').slice(0, 1) }}</el-avatar>
            <h2>{{ profile.realName || profile.username }}</h2>
            <p class="sub">{{ roleText }}</p>
            <div class="level-tag">
              <el-tag type="warning" effect="dark">{{ profile.levelName || '—' }}</el-tag>
            </div>
            <el-descriptions :column="1" class="mt">
              <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
              <el-descriptions-item label="学号">{{ profile.studentNo || '—' }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ profile.phone || '—' }}</el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ profile.createdAt ? profile.createdAt.slice(0, 10) : '—' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="16">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card shadow="never" class="stat-card">
              <div class="stat-num">{{ profile?.totalHours ?? '—' }}</div>
              <div class="stat-label">累计服务时长(h)</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="stat-card">
              <div class="stat-num">{{ profile?.serviceCount ?? '—' }}</div>
              <div class="stat-label">有效服务次数</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="stat-card">
              <div class="stat-num">{{ profile?.activityCount ?? '—' }}</div>
              <div class="stat-label">参与活动数</div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never" class="mt">
          <template #header>编辑资料</template>
          <el-form :model="form" label-width="80px" style="max-width: 420px">
            <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
            <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="save">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, updateMyProfile } from '../../api'

const loading = ref(false)
const profile = ref(null)
const form = reactive({ realName: '', studentNo: '', phone: '' })

const roleText = computed(() => {
  const map = { VOLUNTEER: '志愿者', ORGANIZER: '组织者', ADMIN: '管理员' }
  return (profile.value?.roles || []).map((r) => map[r] || r).join(' / ')
})

async function load() {
  loading.value = true
  try {
    profile.value = await getMyProfile()
    form.realName = profile.value.realName || ''
    form.studentNo = profile.value.studentNo || ''
    form.phone = profile.value.phone || ''
  } finally {
    loading.value = false
  }
}

async function save() {
  try {
    await updateMyProfile({ ...form })
    ElMessage.success('资料已更新')
    load()
  } catch (e) { /* 已统一提示 */ }
}

onMounted(load)
</script>

<style scoped>
.profile-center { text-align: center; }
.profile-center h2 { margin-top: 8px; font-size: 20px; color: #1f2d3d; }
.profile-center .sub { color: #909399; margin-top: 4px; }
.level-tag { margin-top: 10px; }
.stat-card { text-align: center; }
.stat-num { font-size: 28px; font-weight: 700; color: #409eff; }
.stat-label { color: #64748b; font-size: 13px; margin-top: 4px; }
.mt { margin-top: 16px; }
</style>

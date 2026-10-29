<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>签发周期证明</template>
          <el-form :model="form" label-width="90px">
            <el-form-item label="志愿者ID" required>
              <el-input v-model="form.userId" placeholder="志愿者用户 ID（见志愿者档案）" />
            </el-form-item>
            <el-form-item label="证明周期" required>
              <el-input v-model="form.periodLabel" placeholder="如：2025-2026学年" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="issue">签发证明</el-button>
              <span class="tip">系统自动汇总该周期已确认工时，并同步生成 PDF</span>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>
            <div class="head-row">
              已签发证明
              <el-input v-model="searchId" placeholder="志愿者 ID" style="width: 140px" size="small" @keyup.enter="loadCerts(1)" />
              <el-button size="small" type="primary" @click="loadCerts(1)">查询</el-button>
            </div>
          </template>
          <el-table :data="certs" v-loading="loading" size="small">
            <el-table-column prop="serialNo" label="编号" min-width="180" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="periodLabel" label="周期" min-width="120" />
            <el-table-column label="工时" width="80">
              <template #default="{ row }">{{ row.totalHours }}h</template>
            </el-table-column>
            <el-table-column label="签发时间" min-width="140">
              <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T', ' ').slice(0, 16) : '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.pdfUrl" size="small" type="success" link @click="downloadPdf(row)">下载 PDF</el-button>
                <span v-else class="tip">无 PDF</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { issueCert, volunteerCerts, downloadCertPdf } from '../../api'

const form = reactive({ userId: '', periodLabel: '' })
const saving = ref(false)
const certs = ref([])
const loading = ref(false)
const searchId = ref('')

async function issue() {
  if (!form.userId || !form.periodLabel) {
    ElMessage.warning('请填写志愿者 ID 与证明周期')
    return
  }
  saving.value = true
  try {
    await issueCert({ userId: Number(form.userId), periodLabel: form.periodLabel.trim() })
    ElMessage.success('证明已签发')
    form.periodLabel = ''
    loadCerts(1)
  } finally {
    saving.value = false
  }
}

async function loadCerts(page) {
  if (!searchId.value) return
  loading.value = true
  try {
    const data = await volunteerCerts(searchId.value, { page, size: 10 })
    certs.value = data.list
  } finally {
    loading.value = false
  }
}

async function downloadPdf(row) {
  try {
    const blob = await downloadCertPdf(row.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${row.serialNo || 'certificate'}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('证明 PDF 已下载')
  } catch (e) { /* 错误已统一提示 */ }
}

onMounted(() => { /* 等待输入志愿者 ID 后查询 */ })
</script>

<style scoped>
.tip { color: #909399; font-size: 12px; margin-left: 8px; }
.head-row { display: flex; align-items: center; gap: 10px; }
</style>

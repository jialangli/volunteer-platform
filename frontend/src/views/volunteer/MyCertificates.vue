<template>
  <el-card shadow="never">
    <template #header>我的志愿服务证明</template>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="serialNo" label="证明编号" min-width="200" />
      <el-table-column prop="periodLabel" label="证明周期" min-width="140" />
      <el-table-column label="周期累计工时" width="130">
        <template #default="{ row }">{{ row.totalHours }} 小时</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'GENERATED' ? 'success' : 'info'" size="small">{{ row.status === 'GENERATED' ? '已签发' : row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="签发时间" min-width="150">
        <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T', ' ').slice(0, 16) : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="showDetail(row)">查看</el-button>
          <el-button v-if="row.pdfUrl" size="small" type="success" link @click="downloadPdf(row)">下载 PDF</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />

    <el-dialog v-model="detailVisible" title="服务证明" width="600px">
      <div class="cert" v-if="current">
        <div class="cert-head">
          <h2>志愿服务证明</h2>
          <p class="cert-no">编号：{{ current.serialNo }}</p>
        </div>
        <div class="cert-body">
          <p>兹证明 <b>{{ current.realName }}</b>（学号：{{ current.studentNo || '—' }}）</p>
          <p>在 <b>{{ current.periodLabel }}</b> 期间参与志愿服务活动，</p>
          <p>累计服务时长 <b class="highlight">{{ current.totalHours }}</b> 小时。</p>
          <p>特此证明，以供评奖评优及档案留存使用。</p>
        </div>
        <div class="cert-foot">
          <p>签发日期：{{ current.createdAt ? current.createdAt.slice(0, 10) : '—' }}</p>
          <p>志愿服务管理平台</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current && current.pdfUrl" type="primary" @click="downloadPdf(current)">下载 PDF</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { myCerts, downloadCertPdf } from '../../api'

const query = reactive({ page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const current = ref(null)

async function load(page) {
  query.page = page
  loading.value = true
  try {
    const data = await myCerts(query)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function showDetail(row) {
  current.value = row
  detailVisible.value = true
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

onMounted(() => load(1))
</script>

<style scoped>
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
.cert { border: 2px solid #d4a64a; border-radius: 8px; padding: 28px; background: #fffef5; text-align: center; }
.cert-head h2 { font-size: 24px; color: #8c6d1f; letter-spacing: 6px; }
.cert-no { color: #a08c5c; font-size: 12px; margin-top: 6px; }
.cert-body { margin: 24px 0; line-height: 2; color: #4a3f28; }
.cert-body .highlight { color: #c0392b; font-size: 18px; }
.cert-foot { border-top: 1px dashed #d4a64a; padding-top: 12px; color: #8c6d1f; font-size: 13px; }
</style>

<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="6" v-for="s in cards" :key="s.label">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt">
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>活动类型分布</template>
          <div ref="pieRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>近 6 个月活动与工时趋势</template>
          <div ref="trendRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>热门活动 TOP5（按报名数）</template>
          <div ref="actRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>志愿者工时 TOP5</template>
          <div ref="volRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { statsOverview, statsTypeDist, statsMonthly, statsTopActivities, statsTopVolunteers } from '../../api'

const loading = ref(false)
const cards = ref([
  { label: '活动总数', value: 0, color: '#409eff' },
  { label: '志愿者人数', value: 0, color: '#67c23a' },
  { label: '服务人次', value: 0, color: '#e6a23c' },
  { label: '累计工时(h)', value: 0, color: '#f56c6c' }
])

const pieRef = ref()
const trendRef = ref()
const actRef = ref()
const volRef = ref()
const charts = []

function mkChart(el) {
  const c = echarts.init(el)
  charts.push(c)
  return c
}

async function load() {
  loading.value = true
  try {
    const ov = await statsOverview()
    cards.value = [
      { label: '活动总数', value: ov.activityCount ?? 0, color: '#409eff' },
      { label: '志愿者人数', value: ov.volunteerCount ?? 0, color: '#67c23a' },
      { label: '服务人次', value: ov.totalServiceCount ?? 0, color: '#e6a23c' },
      { label: '累计工时(h)', value: ov.totalHours ?? 0, color: '#f56c6c' }
    ]

    const dist = await statsTypeDist()
    mkChart(pieRef.value).setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, type: 'scroll' },
      series: [{
        type: 'pie', radius: ['35%', '65%'],
        label: { formatter: '{b}: {c}' },
        data: (dist || []).map((d) => ({ name: d.typeName || '未分类', value: d.registrationCount ?? 0 }))
      }]
    })

    const trend = await statsMonthly(6)
    const months = (trend || []).map((d) => d.month)
    mkChart(trendRef.value).setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['活动数', '工时(h)'] },
      grid: { left: 40, right: 40, bottom: 30, top: 30 },
      xAxis: { type: 'category', data: months },
      yAxis: [
        { type: 'value', name: '活动数' },
        { type: 'value', name: '工时' }
      ],
      series: [
        { name: '活动数', type: 'bar', data: (trend || []).map((d) => d.activityCount), itemStyle: { color: '#409eff' } },
        { name: '工时(h)', type: 'line', smooth: true, yAxisIndex: 1, data: (trend || []).map((d) => d.totalHours), itemStyle: { color: '#f56c6c' } }
      ]
    })

    const acts = await statsTopActivities(5)
    mkChart(actRef.value).setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, bottom: 60, top: 20 },
      xAxis: { type: 'category', data: (acts || []).map((d) => truncate(d.title)), axisLabel: { interval: 0, fontSize: 11 } },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', barWidth: 26, data: (acts || []).map((d) => d.regCount), itemStyle: { color: '#67c23a' } }]
    })

    const vols = await statsTopVolunteers(5)
    mkChart(volRef.value).setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, bottom: 30, top: 20 },
      xAxis: { type: 'category', data: (vols || []).map((d) => d.realName || d.userId) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', barWidth: 26, data: (vols || []).map((d) => d.totalHours), itemStyle: { color: '#e6a23c' } }]
    })
  } finally {
    loading.value = false
  }
}

function truncate(s) { return s && s.length > 8 ? s.slice(0, 8) + '…' : (s || '') }

onMounted(async () => {
  await nextTick()
  await load()
  window.addEventListener('resize', resizeAll)
})
function resizeAll() { charts.forEach((c) => c.resize()) }
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-num { font-size: 30px; font-weight: 700; }
.stat-label { color: #64748b; font-size: 13px; margin-top: 4px; }
.chart { height: 320px; }
.mt { margin-top: 16px; }
</style>

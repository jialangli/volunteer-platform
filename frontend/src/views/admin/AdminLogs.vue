<template>
  <el-card shadow="never">
    <div class="bar">
      <el-input v-model="userId" placeholder="操作用户 ID" clearable style="width: 160px" @keyup.enter="load(1)" @clear="load(1)" />
      <el-select v-model="module" placeholder="模块" clearable style="width: 140px" @change="load(1)">
        <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userId" label="操作用户" width="90" />
      <el-table-column prop="module" label="模块" width="110" />
      <el-table-column prop="action" label="操作" width="150" />
      <el-table-column prop="targetType" label="对象类型" width="110" />
      <el-table-column prop="targetId" label="对象ID" width="90" />
      <el-table-column label="内容" min-width="200">
        <template #default="{ row }">{{ row.note }}</template>
      </el-table-column>
      <el-table-column label="时间" min-width="150">
        <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T', ' ').slice(0, 19) : '—' }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="130" />
    </el-table>
    <el-pagination class="pager" background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminLogs } from '../../api'

const userId = ref('')
const module = ref('')
const query = ref({ page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const modules = ['auth', 'activity', 'registration', 'attendance', 'hours', 'profile', 'certification', 'admin']

async function load(page) {
  query.value.page = page
  loading.value = true
  try {
    const data = await adminLogs({
      ...query.value,
      userId: userId.value || undefined,
      module: module.value || undefined
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.bar { display: flex; gap: 12px; margin-bottom: 12px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>

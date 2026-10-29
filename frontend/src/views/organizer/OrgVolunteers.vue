<template>
  <div>
    <el-card shadow="never" class="bar">
      <div class="bar-inner">
        <el-input v-model="keyword" placeholder="按姓名/学号/用户名搜索志愿者" clearable style="width: 260px" @keyup.enter="load(1)" @clear="load(1)" />
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </el-card>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="studentNo" label="学号" width="130" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="累计工时" width="110">
        <template #default="{ row }">{{ row.totalHours ?? 0 }} 小时</template>
      </el-table-column>
      <el-table-column label="服务次数" width="100">
        <template #default="{ row }">{{ row.serviceCount ?? 0 }} 次</template>
      </el-table-column>
      <el-table-column label="星级等级" width="110">
        <template #default="{ row }">
          <el-tag type="warning" effect="dark" size="small">{{ row.levelName || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" min-width="120">
        <template #default="{ row }">{{ row.createdAt ? row.createdAt.slice(0, 10) : '—' }}</template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listVolunteers } from '../../api'

const keyword = ref('')
const query = ref({ page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function load(page) {
  query.value.page = page
  loading.value = true
  try {
    const data = await listVolunteers({ ...query.value, keyword: keyword.value || undefined })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.bar { margin-bottom: 16px; }
.bar-inner { display: flex; gap: 12px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>

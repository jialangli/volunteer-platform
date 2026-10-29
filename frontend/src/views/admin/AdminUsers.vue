<template>
  <el-card shadow="never">
    <div class="bar">
      <el-input v-model="keyword" placeholder="搜索用户名/姓名/学号" clearable style="width: 240px" @keyup.enter="load(1)" @clear="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="角色" min-width="140">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles || []" :key="r" size="small" class="role-tag">{{ roleName(r) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '已禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" min-width="120">
        <template #default="{ row }">{{ row.createdAt ? row.createdAt.slice(0, 10) : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openRoles(row)">角色</el-button>
          <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" plain @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />

    <el-dialog v-model="rolesVisible" title="分配角色" width="420px">
      <el-select v-model="selectedRoles" multiple style="width: 100%" placeholder="选择角色">
        <el-option v-for="r in roleOptions" :key="r.id" :label="`${r.name}（${r.code}）`" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="rolesVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUsers, adminUserStatus, adminUserRoles, adminRoles } from '../../api'

const keyword = ref('')
const query = ref({ page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const roleOptions = ref([])
const rolesVisible = ref(false)
const selectedRoles = ref([])
const currentUser = ref(null)
const saving = ref(false)

const roleNameMap = { VOLUNTEER: '志愿者', ORGANIZER: '组织者', ADMIN: '管理员' }
function roleName(c) { return roleNameMap[c] || c }

async function load(page) {
  query.value.page = page
  loading.value = true
  try {
    const data = await adminUsers({ ...query.value, keyword: keyword.value || undefined })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${action}用户 ${row.username}？`, `${action}用户`, { type: 'warning' })
  try {
    await adminUserStatus(row.id, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success(`已${action}`)
    load(query.value.page)
  } catch (e) { /* 已统一提示 */ }
}

async function openRoles(row) {
  currentUser.value = row
  selectedRoles.value = (row.roleIds || []).slice()
  if (roleOptions.value.length === 0) {
    roleOptions.value = await adminRoles()
  }
  rolesVisible.value = true
}

async function saveRoles() {
  saving.value = true
  try {
    await adminUserRoles(currentUser.value.id, { roleIds: selectedRoles.value })
    ElMessage.success('角色已更新')
    rolesVisible.value = false
    load(query.value.page)
  } finally {
    saving.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.bar { display: flex; gap: 12px; margin-bottom: 12px; }
.role-tag { margin-right: 4px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>

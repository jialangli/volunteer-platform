<template>
  <div>
    <el-card shadow="never" class="bar">
      <div class="bar-inner">
        <el-input v-model="query.keyword" placeholder="搜索我的活动" clearable style="width: 240px" @keyup.enter="load(1)" @clear="load(1)" />
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button type="success" @click="openCreate">发布新活动</el-button>
      </div>
    </el-card>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="封面" width="96">
        <template #default="{ row }">
          <img v-if="row.coverUrl" :src="row.coverUrl" class="cover-thumb" alt="封面" />
          <span v-else class="no-cover">无封面</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="活动" min-width="180" />
      <el-table-column prop="typeName" label="类型" width="100" />
      <el-table-column label="时间" min-width="180">
        <template #default="{ row }">{{ fmt(row.startTime) }} ~ {{ fmt(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="报名" width="110">
        <template #default="{ row }">{{ row.registeredCount }}{{ row.capacity > 0 ? ' / ' + row.capacity : '' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="router.push(`/org/registrations/${row.id}`)">报名审核</el-button>
          <el-button size="small" @click="router.push(`/org/attendance/${row.id}`)">签到管理</el-button>
          <el-button v-if="row.status === 'DRAFT'" size="small" type="primary" @click="changeStatus(row, 'publish')">发布</el-button>
          <el-button v-if="row.status === 'RECRUITING'" size="small" type="warning" @click="changeStatus(row, 'finish')">结束</el-button>
          <el-button v-if="row.status === 'DRAFT'" size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />

    <el-dialog v-model="createVisible" title="发布新活动" width="640px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="活动标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="活动封面">
          <div class="cover-upload">
            <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-preview" alt="封面预览" />
            <el-upload :show-file-list="false" :http-request="doUpload" accept="image/*" :disabled="uploading">
              <el-button :loading="uploading">上传封面</el-button>
            </el-upload>
            <el-button v-if="form.coverUrl" size="small" type="danger" plain @click="form.coverUrl = ''">移除</el-button>
            <span class="tip">支持 jpg/png/webp/gif，≤5MB</span>
          </div>
        </el-form-item>
        <el-form-item label="活动类型" prop="typeId">
          <el-select v-model="form.typeId" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="活动开始" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="活动结束" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="签到开放" prop="signinStart">
          <el-date-picker v-model="form.signinStart" type="datetime" placeholder="签到开始" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="签到截止" prop="signinEnd">
          <el-date-picker v-model="form.signinEnd" type="datetime" placeholder="签到结束" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="签退截止" prop="signoutEnd">
          <el-date-picker v-model="form.signoutEnd" type="datetime" placeholder="签退结束" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="名额上限">
          <el-input-number v-model="form.capacity" :min="0" placeholder="0 表示不限名额" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listActivities, createActivity, changeActivityStatus, deleteActivity, adminTypes, uploadFile } from '../../api'

const router = useRouter()
const query = reactive({ page: 1, size: 10, onlyMine: true, keyword: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const types = ref([])
const createVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const formRef = ref()

const form = reactive({
  title: '', typeId: null, location: '', description: '',
  startTime: '', endTime: '', signinStart: '', signinEnd: '', signoutEnd: '', capacity: 0, coverUrl: ''
})
const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  typeId: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  signinStart: [{ required: true, message: '请选择签到开放时间', trigger: 'change' }],
  signinEnd: [{ required: true, message: '请选择签到截止时间', trigger: 'change' }],
  signoutEnd: [{ required: true, message: '请选择签退截止时间', trigger: 'change' }]
}
const statusMap = { DRAFT: '草稿', RECRUITING: '招募中', ONGOING: '进行中', FINISHED: '已结束', ARCHIVED: '已归档', CANCELLED: '已取消' }
function statusText(s) { return statusMap[s] || s }
function statusType(s) { return { DRAFT: 'info', RECRUITING: 'success', ONGOING: 'primary', FINISHED: 'warning', CANCELLED: 'danger', ARCHIVED: 'info' }[s] || 'info' }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

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

function openCreate() {
  Object.assign(form, { title: '', typeId: null, location: '', description: '', startTime: '', endTime: '', signinStart: '', signinEnd: '', signoutEnd: '', capacity: 0, coverUrl: '' })
  createVisible.value = true
}

async function doUpload({ file }) {
  uploading.value = true
  try {
    const data = await uploadFile(file)
    form.coverUrl = data.url
    ElMessage.success('封面已上传')
  } catch (e) { /* 错误已统一提示 */ } finally {
    uploading.value = false
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await createActivity({ ...form })
    ElMessage.success('活动已创建（草稿），可立即发布')
    createVisible.value = false
    load(1)
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, action) {
  const label = { publish: '发布', finish: '结束' }[action]
  await ElMessageBox.confirm(`确认${label}「${row.title}」？`, '确认', { type: 'warning' })
  try {
    await changeActivityStatus(row.id, action)
    ElMessage.success(`已${label}`)
    load(query.page)
  } catch (e) { /* 已统一提示 */ }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除草稿「${row.title}」？`, '删除确认', { type: 'error' })
  try {
    await deleteActivity(row.id)
    ElMessage.success('已删除')
    load(1)
  } catch (e) { /* 已统一提示 */ }
}

onMounted(async () => {
  try { types.value = await adminTypes() } catch (e) { /* 忽略 */ }
  load(1)
})
</script>

<style scoped>
.bar { margin-bottom: 16px; }
.bar-inner { display: flex; gap: 12px; align-items: center; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
.cover-thumb { width: 80px; height: 48px; object-fit: cover; border-radius: 4px; display: block; }
.no-cover { color: #c0c4cc; font-size: 12px; }
.cover-upload { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.cover-preview { width: 96px; height: 60px; object-fit: cover; border-radius: 4px; }
.tip { color: #909399; font-size: 12px; }
</style>

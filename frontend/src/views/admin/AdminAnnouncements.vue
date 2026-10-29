<template>
  <el-card shadow="never">
    <div class="bar">
      <el-button type="primary" @click="openCreate">发布公告</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '已发布' : '草稿/下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置顶" width="80">
        <template #default="{ row }">{{ row.isTop ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="发布时间" min-width="150">
        <template #default="{ row }">{{ row.publishTime ? row.publishTime.replace('T', ' ').slice(0, 16) : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 0" size="small" type="primary" plain @click="setStatus(row, 'publish')">发布</el-button>
          <el-button v-if="row.status === 1" size="small" type="warning" plain @click="setStatus(row, 'unpublish')">下架</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容" required><el-input v-model="form.content" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAnnouncements, adminCreateAnnouncement, adminUpdateAnnouncement, adminPublishAnnouncement, adminUnpublishAnnouncement, adminDeleteAnnouncement } from '../../api'

const query = ref({ page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, title: '', content: '', isTop: 0 })

async function load(page) {
  query.value.page = page
  loading.value = true
  try {
    const data = await adminAnnouncements(query.value)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, title: '', content: '', isTop: 0 })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { id: row.id, title: row.title, content: row.content, isTop: row.isTop })
  dialogVisible.value = true
}

async function save() {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题与内容')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await adminUpdateAnnouncement(form.id, { title: form.title, content: form.content, isTop: form.isTop })
      ElMessage.success('公告已更新')
    } else {
      await adminCreateAnnouncement({ title: form.title, content: form.content, isTop: form.isTop })
      ElMessage.success('公告已创建')
    }
    dialogVisible.value = false
    load(query.value.page)
  } finally {
    saving.value = false
  }
}

async function setStatus(row, action) {
  const label = action === 'publish' ? '发布' : '下架'
  await ElMessageBox.confirm(`确认${label}「${row.title}」？`, '确认', { type: 'warning' })
  try {
    if (action === 'publish') await adminPublishAnnouncement(row.id)
    else await adminUnpublishAnnouncement(row.id)
    ElMessage.success(`已${label}`)
    load(query.value.page)
  } catch (e) { /* 已统一提示 */ }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除公告「${row.title}」？`, '删除确认', { type: 'error' })
  try {
    await adminDeleteAnnouncement(row.id)
    ElMessage.success('已删除')
    load(1)
  } catch (e) { /* 已统一提示 */ }
}

onMounted(() => load(1))
</script>

<style scoped>
.bar { margin-bottom: 12px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>

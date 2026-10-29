<template>
  <el-card shadow="never">
    <div class="bar">
      <el-button type="primary" @click="openCreate">新增类型</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="类型名称" min-width="160" />
      <el-table-column prop="description" label="说明" min-width="220" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑类型' : '新增类型'" width="440px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" /></el-form-item>
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
import { adminTypes, adminCreateType, adminUpdateType, adminDeleteType } from '../../api'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, name: '', description: '' })

async function load() {
  loading.value = true
  try {
    list.value = await adminTypes()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { id: row.id, name: row.name, description: row.description || '' })
  dialogVisible.value = true
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请输入类型名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await adminUpdateType(form.id, { name: form.name, description: form.description })
      ElMessage.success('类型已更新')
    } else {
      await adminCreateType({ name: form.name, description: form.description })
      ElMessage.success('类型已新增')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除类型「${row.name}」？被活动引用的类型无法删除。`, '删除确认', { type: 'error' })
  try {
    await adminDeleteType(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 已统一提示 */ }
}

onMounted(load)
</script>

<style scoped>
.bar { margin-bottom: 12px; }
</style>

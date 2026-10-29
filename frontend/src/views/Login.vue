<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="brand-badge">志</div>
        <h1>志愿服务管理平台</h1>
        <p>活动发布 · 在线报名 · 签到签退 · 工时核算 · 服务证明</p>
      </div>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginRef" label-width="0" size="large">
            <el-form-item prop="username"><el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" /></el-form-item>
            <el-form-item prop="password"><el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="doLogin" /></el-form-item>
            <el-button type="primary" class="submit" size="large" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm" :rules="regRules" ref="regRef" label-width="0" size="large">
            <el-form-item prop="username"><el-input v-model="regForm.username" placeholder="用户名（登录使用）" :prefix-icon="User" /></el-form-item>
            <el-form-item prop="password"><el-input v-model="regForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password /></el-form-item>
            <el-form-item prop="realName"><el-input v-model="regForm.realName" placeholder="真实姓名" /></el-form-item>
            <el-form-item prop="studentNo"><el-input v-model="regForm.studentNo" placeholder="学号" /></el-form-item>
            <el-button type="primary" class="submit" size="large" :loading="loading" @click="doRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <el-alert type="info" :closable="false" class="demo-tip" title="演示账号" description="管理员 admin/admin123 · 组织者 organizer/organizer123 · 志愿者请注册新账号" />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const tab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', realName: '', studentNo: '' })
const loginRef = ref()
const regRef = ref()

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const regRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }]
}

async function doLogin() {
  await loginRef.value.validate()
  loading.value = true
  try {
    const user = await auth.login(loginForm.username, loginForm.password)
    ElMessage.success(`欢迎回来，${user.realName || user.username}`)
    router.push(route.query.redirect || '/activities')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  await regRef.value.validate()
  loading.value = true
  try {
    const user = await auth.register(regForm)
    ElMessage.success(`注册成功，欢迎 ${user.realName || user.username}`)
    router.push('/activities')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1d39c4 0%, #409eff 100%); }
.login-card { width: 420px; background: #fff; border-radius: 12px; padding: 32px 36px; box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2); }
.brand { text-align: center; margin-bottom: 20px; }
.brand-badge { width: 48px; height: 48px; border-radius: 12px; background: #409eff; color: #fff; font-size: 24px; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 8px; }
.brand h1 { font-size: 20px; color: #1f2d3d; }
.brand p { margin-top: 6px; font-size: 13px; color: #64748b; }
.submit { width: 100%; }
.demo-tip { margin-top: 16px; }
</style>

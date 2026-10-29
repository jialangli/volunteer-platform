<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span class="logo-badge">志</span>
        <span class="logo-text">志愿服务平台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#001529" text-color="#c8ccd4" active-text-color="#fff">
        <el-menu-item index="/activities">
          <el-icon><Promotion /></el-icon><span>活动广场</span>
        </el-menu-item>
        <template v-if="isVolunteer">
          <el-menu-item index="/my/registrations"><el-icon><Document /></el-icon><span>我的报名</span></el-menu-item>
          <el-menu-item index="/my/checkin"><el-icon><Position /></el-icon><span>签到签退</span></el-menu-item>
          <el-menu-item index="/my/profile"><el-icon><User /></el-icon><span>我的档案</span></el-menu-item>
          <el-menu-item index="/my/certificates"><el-icon><Tickets /></el-icon><span>我的证明</span></el-menu-item>
        </template>
        <template v-if="isOrg || isAdmin">
          <div class="menu-group">组织管理</div>
          <el-menu-item index="/org/activities"><el-icon><Calendar /></el-icon><span>活动管理</span></el-menu-item>
          <el-menu-item index="/org/volunteers"><el-icon><UserFilled /></el-icon><span>志愿者档案</span></el-menu-item>
          <el-menu-item index="/org/certificates"><el-icon><Stamp /></el-icon><span>证明签发</span></el-menu-item>
          <el-menu-item index="/admin/stats"><el-icon><DataAnalysis /></el-icon><span>统计可视化</span></el-menu-item>
        </template>
        <template v-if="isAdmin">
          <div class="menu-group">系统管理</div>
          <el-menu-item index="/admin/users"><el-icon><Setting /></el-icon><span>用户管理</span></el-menu-item>
          <el-menu-item index="/admin/announcements"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
          <el-menu-item index="/admin/types"><el-icon><Grid /></el-icon><span>类型管理</span></el-menu-item>
          <el-menu-item index="/admin/logs"><el-icon><Memo /></el-icon><span>操作日志</span></el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title || '' }}</span>
        <div class="header-right">
          <el-dropdown @command="onCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>角色：{{ roleText }}</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { Promotion, Document, Position, User, Tickets, Calendar, UserFilled, Stamp, DataAnalysis, Setting, Bell, Grid, Memo, ArrowDown } from '@element-plus/icons-vue'

const auth = useAuthStore()
const router = useRouter()

const isVolunteer = computed(() => auth.roles.includes('VOLUNTEER'))
const isOrg = computed(() => auth.roles.includes('ORGANIZER'))
const isAdmin = computed(() => auth.roles.includes('ADMIN'))
const userName = computed(() => auth.user?.realName || auth.user?.username || '用户')
const roleText = computed(() => {
  const map = { VOLUNTEER: '志愿者', ORGANIZER: '组织者', ADMIN: '管理员' }
  return auth.roles.map((r) => map[r] || r).join(' / ')
})

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #001529; overflow-y: auto; }
.aside::-webkit-scrollbar { width: 4px; }
.logo { display: flex; align-items: center; gap: 8px; padding: 18px 16px; color: #fff; font-size: 16px; font-weight: 600; }
.logo-badge { width: 30px; height: 30px; border-radius: 6px; background: #409eff; display: inline-flex; align-items: center; justify-content: center; color: #fff; }
.menu-group { padding: 12px 16px 4px; font-size: 12px; color: #6b7280; }
.aside :deep(.el-menu) { border-right: none; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e5e7eb; }
.page-title { font-size: 16px; font-weight: 600; color: #1f2d3d; }
.user-info { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; color: #334155; outline: none; }
.main { background: #f5f7fa; padding: 16px; overflow-y: auto; }
</style>

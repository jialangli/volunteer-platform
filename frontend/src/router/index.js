import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import Layout from '../layout/Layout.vue'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: Layout,
    redirect: '/activities',
    children: [
      { path: 'activities', name: 'activities', component: () => import('../views/volunteer/ActivityList.vue'), meta: { title: '活动广场' } },
      { path: 'my/registrations', name: 'myRegistrations', component: () => import('../views/volunteer/MyRegistrations.vue'), meta: { title: '我的报名', roles: ['VOLUNTEER'] } },
      { path: 'my/checkin', name: 'myCheckin', component: () => import('../views/volunteer/CheckIn.vue'), meta: { title: '签到签退', roles: ['VOLUNTEER'] } },
      { path: 'my/profile', name: 'myProfile', component: () => import('../views/volunteer/MyProfile.vue'), meta: { title: '我的档案', roles: ['VOLUNTEER'] } },
      { path: 'my/certificates', name: 'myCertificates', component: () => import('../views/volunteer/MyCertificates.vue'), meta: { title: '我的证明', roles: ['VOLUNTEER'] } },

      { path: 'org/activities', name: 'orgActivities', component: () => import('../views/organizer/OrgActivities.vue'), meta: { title: '活动管理', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'org/registrations/:id', name: 'orgRegistrations', component: () => import('../views/organizer/OrgRegistrations.vue'), meta: { title: '报名审核', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'org/attendance/:id', name: 'orgAttendance', component: () => import('../views/organizer/OrgAttendance.vue'), meta: { title: '签到管理', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'org/volunteers', name: 'orgVolunteers', component: () => import('../views/organizer/OrgVolunteers.vue'), meta: { title: '志愿者档案', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'org/certificates', name: 'orgCertificates', component: () => import('../views/organizer/OrgCertificates.vue'), meta: { title: '证明签发', roles: ['ORGANIZER', 'ADMIN'] } },

      { path: 'admin/stats', name: 'adminStats', component: () => import('../views/admin/StatsDashboard.vue'), meta: { title: '统计可视化', roles: ['ORGANIZER', 'ADMIN'] } },
      { path: 'admin/users', name: 'adminUsers', component: () => import('../views/admin/AdminUsers.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'admin/announcements', name: 'adminAnnouncements', component: () => import('../views/admin/AdminAnnouncements.vue'), meta: { title: '公告管理', roles: ['ADMIN'] } },
      { path: 'admin/types', name: 'adminTypes', component: () => import('../views/admin/AdminTypes.vue'), meta: { title: '类型管理', roles: ['ADMIN'] } },
      { path: 'admin/logs', name: 'adminLogs', component: () => import('../views/admin/AdminLogs.vue'), meta: { title: '操作日志', roles: ['ADMIN'] } }
    ]
  },
  // 扫码签到页（组织者展示二维码 → 志愿者扫码进入此页完成签到）
  { path: '/scan/:id', name: 'scanIn', component: () => import('../views/volunteer/ScanIn.vue'), meta: { title: '扫码签到' } },
  { path: '/:pathMatch(.*)*', redirect: '/activities' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return true
  }
  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 刷新页面后 store 从 localStorage 恢复，但仍需拿到最新用户信息（角色用于菜单与鉴权）
  if (!auth.user && auth.isLoggedIn) {
    try {
      await auth.fetchMe()
    } catch (e) {
      auth.logout()
      return { path: '/login' }
    }
  }
  const allowed = to.meta.roles
  if (allowed && !allowed.some((r) => auth.roles.includes(r))) {
    return { path: '/activities' }
  }
  return true
})

export default router

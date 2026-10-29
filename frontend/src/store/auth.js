import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister, getMe, logout as apiLogout } from '../api'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    roles: (s) => s.user?.roles || [],
    isLoggedIn: (s) => !!s.token,
    hasRole: (s) => (role) => s.user?.roles?.includes(role) || false
  },
  actions: {
    async login(username, password) {
      const data = await apiLogin({ username, password })
      this.token = data.accessToken
      localStorage.setItem(TOKEN_KEY, data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken || '')
      await this.fetchMe()
      return this.user
    },
    async register(form) {
      await apiRegister(form)
      return this.login(form.username, form.password)
    },
    async fetchMe() {
      const me = await getMe()
      this.user = me
      localStorage.setItem(USER_KEY, JSON.stringify(me))
      return me
    },
    async logout() {
      try {
        await apiLogout({ refreshToken: localStorage.getItem('refreshToken') || '' })
      } catch (e) { /* 忽略登出失败 */ }
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem('refreshToken')
    }
  }
})

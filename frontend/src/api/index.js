import request from './request'

// ---------- 认证 ----------
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const logout = (data) => request.post('/auth/logout', data)
export const getMe = () => request.get('/auth/me')

// ---------- 活动 ----------
export const listActivities = (params) => request.get('/activities', { params })
export const getActivity = (id) => request.get(`/activities/${id}`)
export const createActivity = (data) => request.post('/activities', data)
export const updateActivity = (id, data) => request.put(`/activities/${id}`, data)
export const changeActivityStatus = (id, action) => request.post(`/activities/${id}/status`, null, { params: { action } })
export const deleteActivity = (id) => request.delete(`/activities/${id}`)

// ---------- 报名 ----------
export const enroll = (activityId) => request.post('/registrations', { activityId })
export const cancelEnroll = (activityId) => request.delete(`/registrations/${activityId}`)
export const myRegistrations = (params) => request.get('/registrations/mine', { params })
export const listRegistrations = (activityId, params) => request.get(`/activities/${activityId}/registrations`, { params })
export const auditRegistration = (id, data) => request.post(`/registrations/${id}/audit`, data)

// ---------- 签到签退与工时 ----------
export const signin = (activityId, data) => request.post(`/attendance/${activityId}/signin`, data || {})
export const signout = (activityId) => request.post(`/attendance/${activityId}/signout`, {})
export const myAttendance = (params) => request.get('/attendance/mine', { params })
export const activityAttendance = (activityId, params) => request.get(`/activities/${activityId}/attendance`, { params })
export const myHours = (params) => request.get('/hours/mine', { params })

// ---------- 档案与证明 ----------
export const getMyProfile = () => request.get('/profile/mine')
export const updateMyProfile = (data) => request.put('/profile/mine', data)
export const listVolunteers = (params) => request.get('/profile/volunteers', { params })
export const issueCert = (data) => request.post('/certifications', data)
export const myCerts = (params) => request.get('/certifications/mine', { params })
export const volunteerCerts = (userId, params) => request.get(`/certifications/volunteer/${userId}`, { params })

// ---------- 统计 ----------
export const statsOverview = () => request.get('/stats/overview')
export const statsTypeDist = () => request.get('/stats/type-distribution')
export const statsMonthly = (months) => request.get('/stats/monthly-trend', { params: { months } })
export const statsTopActivities = (limit) => request.get('/stats/top-activities', { params: { limit } })
export const statsTopVolunteers = (limit) => request.get('/stats/top-volunteers', { params: { limit } })

// ---------- 系统管理 ----------
export const adminUsers = (params) => request.get('/admin/users', { params })
export const adminUserStatus = (id, data) => request.put(`/admin/users/${id}/status`, data)
export const adminUserRoles = (id, data) => request.put(`/admin/users/${id}/roles`, data)
export const adminRoles = () => request.get('/admin/roles')
export const adminAnnouncements = (params) => request.get('/admin/announcements', { params })
export const adminCreateAnnouncement = (data) => request.post('/admin/announcements', data)
export const adminUpdateAnnouncement = (id, data) => request.put(`/admin/announcements/${id}`, data)
export const adminPublishAnnouncement = (id) => request.post(`/admin/announcements/${id}/publish`)
export const adminUnpublishAnnouncement = (id) => request.post(`/admin/announcements/${id}/unpublish`)
export const adminDeleteAnnouncement = (id) => request.delete(`/admin/announcements/${id}`)
export const adminTypes = () => request.get('/admin/types')
export const adminCreateType = (data) => request.post('/admin/types', data)
export const adminUpdateType = (id, data) => request.put(`/admin/types/${id}`, data)
export const adminDeleteType = (id) => request.delete(`/admin/types/${id}`)
export const adminLogs = (params) => request.get('/admin/logs', { params })

// ---------- 文件上传 / 证明 PDF ----------
export const uploadFile = (file, dir = 'covers') => {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('dir', dir)
  return request.post('/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const downloadCertPdf = (id) => request.get(`/certifications/${id}/pdf`, { responseType: 'blob' })

// ---------- 公开公告 ----------
export const publicAnnouncements = () => request.get('/announcements')

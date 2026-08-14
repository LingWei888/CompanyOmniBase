import axios from 'axios'

export const http = axios.create({
  baseURL: '/',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const url = config.url || ''
  const isAdminApi = url.includes('/api/admin/')
  const adminToken = localStorage.getItem('admin_access_token')
  const userToken = localStorage.getItem('user_access_token')

  if (isAdminApi && adminToken) {
    config.headers.Authorization = `Bearer ${adminToken}`
  } else if (!isAdminApi && userToken) {
    config.headers.Authorization = `Bearer ${userToken}`
  } else if (adminToken) {
    config.headers.Authorization = `Bearer ${adminToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = String(error.config?.url || '')
      const onAdmin = window.location.pathname.startsWith('/admin')
      if (url.includes('/api/admin/') || onAdmin) {
        localStorage.removeItem('admin_access_token')
        localStorage.removeItem('admin_refresh_token')
        localStorage.removeItem('admin_user')
        if (onAdmin && window.location.pathname !== '/admin/login') {
          window.location.href = `/admin/login?redirect=${encodeURIComponent(window.location.pathname)}`
        }
      } else if (url.includes('/api/auth/')) {
        localStorage.removeItem('user_access_token')
        localStorage.removeItem('user_refresh_token')
        localStorage.removeItem('user_user')
      }
    }
    return Promise.reject(error)
  },
)

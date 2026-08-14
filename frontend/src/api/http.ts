import axios from 'axios'

export const http = axios.create({
  baseURL: '/',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_access_token')
      localStorage.removeItem('admin_refresh_token')
      localStorage.removeItem('admin_user')
      if (window.location.pathname.startsWith('/admin') && window.location.pathname !== '/admin/login') {
        window.location.href = `/admin/login?redirect=${encodeURIComponent(window.location.pathname)}`
      }
    }
    return Promise.reject(error)
  },
)

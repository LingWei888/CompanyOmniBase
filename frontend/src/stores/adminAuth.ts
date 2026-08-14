import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { adminLogin, adminLogout, type AdminUser } from '@/api/adminAuth'

const USER_KEY = 'admin_user'
const ACCESS_KEY = 'admin_access_token'
const REFRESH_KEY = 'admin_refresh_token'

function readUser(): AdminUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AdminUser
  } catch {
    return null
  }
}

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_KEY))
  const refreshToken = ref(localStorage.getItem(REFRESH_KEY))
  const user = ref<AdminUser | null>(readUser())

  const isLoggedIn = computed(() => Boolean(accessToken.value))

  async function login(username: string, password: string) {
    const result = await adminLogin(username, password)
    accessToken.value = result.accessToken
    refreshToken.value = result.refreshToken
    user.value = result.user
    localStorage.setItem(ACCESS_KEY, result.accessToken)
    localStorage.setItem(REFRESH_KEY, result.refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  }

  async function logout() {
    try {
      await adminLogout()
    } catch {
      // ignore network errors on logout
    }
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    accessToken,
    refreshToken,
    user,
    isLoggedIn,
    login,
    logout,
  }
})

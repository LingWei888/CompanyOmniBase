import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  changeUserPassword,
  fetchUserMe,
  updateUserProfile,
  uploadUserAvatar,
  userLogin,
  userLogout,
  userRegister,
  type AppUser,
} from '@/api/userAuth'

const USER_KEY = 'user_user'
const ACCESS_KEY = 'user_access_token'
const REFRESH_KEY = 'user_refresh_token'

function readUser(): AppUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AppUser
  } catch {
    return null
  }
}

function persist(accessToken: string, refreshToken: string, user: AppUser) {
  localStorage.setItem(ACCESS_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

function clearPersist() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

export const useUserAuthStore = defineStore('userAuth', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_KEY))
  const refreshToken = ref(localStorage.getItem(REFRESH_KEY))
  const user = ref<AppUser | null>(readUser())

  const isLoggedIn = computed(() => Boolean(accessToken.value && user.value))

  function applyAuth(result: { accessToken: string; refreshToken: string; user: AppUser }) {
    accessToken.value = result.accessToken
    refreshToken.value = result.refreshToken
    user.value = result.user
    persist(result.accessToken, result.refreshToken, result.user)
  }

  function setUser(profile: AppUser) {
    user.value = profile
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
  }

  async function login(username: string, password: string) {
    const result = await userLogin(username, password)
    applyAuth(result)
  }

  async function register(username: string, password: string, nickname?: string) {
    const result = await userRegister({ username, password, nickname })
    applyAuth(result)
  }

  async function refreshProfile() {
    if (!accessToken.value) return
    const profile = await fetchUserMe()
    setUser(profile)
  }

  async function updateProfile(payload: { nickname?: string; avatarUrl?: string }) {
    const profile = await updateUserProfile(payload)
    setUser(profile)
  }

  async function uploadAvatar(file: File) {
    const profile = await uploadUserAvatar(file)
    setUser(profile)
    return profile
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    await changeUserPassword({ oldPassword, newPassword })
  }

  async function logout() {
    try {
      await userLogout()
    } catch {
      // ignore
    }
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    clearPersist()
  }

  function clearLocal() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    clearPersist()
  }

  return {
    accessToken,
    refreshToken,
    user,
    isLoggedIn,
    login,
    register,
    logout,
    refreshProfile,
    updateProfile,
    uploadAvatar,
    changePassword,
    clearLocal,
  }
})

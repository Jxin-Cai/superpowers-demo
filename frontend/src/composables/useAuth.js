import { ref, computed } from 'vue'

const AUTH_KEY = 'cms_auth'

// 创建全局单例状态（所有组件共享同一个ref）
const authData = ref(JSON.parse(localStorage.getItem(AUTH_KEY) || 'null'))

const isLoggedIn = computed(() => !!authData.value)
const username = computed(() => authData.value?.username || '')
const userRole = computed(() => authData.value?.role || '')
const isAdmin = computed(() => authData.value?.role === 'ADMIN')

function login(username, password, role = 'USER') {
  const credentials = btoa(`${username}:${password}`)
  authData.value = { username, credentials, role }
  localStorage.setItem(AUTH_KEY, JSON.stringify(authData.value))
}

function logout() {
  authData.value = null
  localStorage.removeItem(AUTH_KEY)
  window.location.href = '/login'
}

function getAuthHeader() {
  return authData.value?.credentials
    ? { Authorization: `Basic ${authData.value.credentials}` }
    : {}
}

// 导出单例（所有地方使用同一个状态）
export function useAuth() {
  return {
    isLoggedIn,
    username,
    userRole,
    isAdmin,
    login,
    logout,
    getAuthHeader
  }
}

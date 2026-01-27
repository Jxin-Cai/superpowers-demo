import { ref, computed } from 'vue'

const AUTH_KEY = 'cms_auth'

export function useAuth() {
  const authData = ref(JSON.parse(localStorage.getItem(AUTH_KEY) || 'null'))

  const isLoggedIn = computed(() => !!authData.value)
  const username = computed(() => authData.value?.username || '')

  function login(username, password) {
    const credentials = btoa(`${username}:${password}`)
    authData.value = { username, credentials }
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

  return {
    isLoggedIn,
    username,
    login,
    logout,
    getAuthHeader
  }
}

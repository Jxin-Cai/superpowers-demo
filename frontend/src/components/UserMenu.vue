<template>
  <div class="user-menu">
    <template v-if="isLoggedIn">
      <span class="username">{{ username }}</span>
      <el-button type="text" @click="handleLogout">退出</el-button>
    </template>
    <template v-else>
      <el-button type="text" @click="goToLogin">登录</el-button>
      <el-button type="text" @click="goToRegister">注册</el-button>
    </template>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { isLoggedIn, username, logout } = useAuth()

function handleLogout() {
  logout()
}

function goToLogin() {
  router.push('/login')
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  color: #333;
}
</style>

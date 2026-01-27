<template>
  <div class="user-menu">
    <template v-if="isLoggedIn">
      <el-dropdown trigger="hover" @command="handleCommand">
        <span class="user-info">
          <el-icon><Avatar /></el-icon>
          <span class="username">{{ username }}</span>
          <el-icon class="arrow"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              <span style="color: #909399;">{{ username }}</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
    <template v-else>
      <el-button type="text" @click="goToLogin">登录</el-button>
      <el-button type="text" @click="goToRegister">注册</el-button>
    </template>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Avatar, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { isLoggedIn, username, logout } = useAuth()

function handleCommand(command) {
  if (command === 'logout') {
    logout()
  }
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

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.username {
  color: #333;
  font-size: 14px;
}

.arrow {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
}

.user-info:hover .arrow {
  transform: rotate(180deg);
}
</style>

<template>
  <nav class="top-nav">
    <div class="nav-container">
      <div class="nav-logo" @click="router.push('/')">
        <span>CMS</span>
      </div>
      <div class="nav-menu">
        <div
          v-for="category in rootCategories"
          :key="category.id"
          class="nav-item"
          @mouseenter="showDropdown(category.id)"
          @mouseleave="hideDropdown"
        >
          <span @click="goToCategory(category.id)">{{ category.name }}</span>
          <div v-if="category.children?.length" class="dropdown" :class="{ show: activeDropdown === category.id }">
            <div
              v-for="child in category.children"
              :key="child.id"
              class="dropdown-item"
              @click="goToCategory(child.id)"
            >
              {{ child.name }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '@/api'

const router = useRouter()
const treeData = ref([])
const activeDropdown = ref(null)

const rootCategories = computed(() => {
  return treeData.value
})

const loadTree = async () => {
  try {
    const res = await publicApi.getCategoryTree()
    treeData.value = res.data.tree || []
  } catch (e) {
    console.error('加载导航失败', e)
  }
}

const showDropdown = (id) => {
  activeDropdown.value = id
}

const hideDropdown = () => {
  activeDropdown.value = null
}

const goToCategory = (id) => {
  router.push({ name: 'CategoryArticleList', params: { id } })
  activeDropdown.value = null
}

onMounted(loadTree)
</script>

<style scoped>
.top-nav {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 20px;
}

.nav-logo {
  font-size: 24px;
  font-weight: bold;
  margin-right: 40px;
  cursor: pointer;
}

.nav-menu {
  display: flex;
  gap: 8px;
}

.nav-item {
  position: relative;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.nav-item:hover {
  background: #f5f7fa;
}

.dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 150px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-8px);
  transition: all 0.2s;
}

.dropdown.show {
  opacity: 1;
  visibility: visible;
  transform: translateY(4px);
}

.dropdown-item {
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.dropdown-item:hover {
  background: #f5f7fa;
}
</style>

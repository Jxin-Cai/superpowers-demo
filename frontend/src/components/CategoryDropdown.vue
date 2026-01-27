<template>
  <el-dropdown trigger="hover" placement="bottom-start" @command="handleCommand">
    <span class="category-dropdown-trigger">
      分类导航
      <el-icon><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <template v-for="category in categoryTree" :key="category.id">
          <template v-if="category.children && category.children.length > 0">
            <el-sub-menu :index="String(category.id)">
              <template #title>
                <router-link :to="`/category/${category.id}`" @click.stop>
                  {{ category.name }}
                </router-link>
              </template>
              <CategoryDropdownItem
                :categories="category.children"
                :level="1"
                @navigate="handleNavigate"
              />
            </el-sub-menu>
          </template>
          <template v-else>
            <el-dropdown-item :command="`/category/${category.id}`">
              {{ category.name }}
            </el-dropdown-item>
          </template>
        </template>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { categoryApi } from '@/api/category'
import CategoryDropdownItem from './CategoryDropdownItem.vue'

const router = useRouter()
const categoryTree = ref([])

const loadCategoryTree = async () => {
  try {
    const res = await categoryApi.getTree()
    categoryTree.value = res.data
  } catch (error) {
    console.error('Failed to load category tree:', error)
  }
}

const handleCommand = (path) => {
  router.push(path)
}

const handleNavigate = (path) => {
  router.push(path)
}

onMounted(() => {
  loadCategoryTree()
})
</script>

<style scoped>
.category-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.category-dropdown-trigger:hover {
  background-color: #f0f0f0;
}
</style>

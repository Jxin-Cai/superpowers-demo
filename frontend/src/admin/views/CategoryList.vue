<template>
  <div class="category-list">
    <div class="header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="handleCreate">
        新建分类
      </el-button>
    </div>

    <CategoryTree ref="treeRef" @refresh="handleRefresh" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import CategoryTree from '@/admin/components/CategoryTree.vue'

const treeRef = ref()

const handleCreate = () => {
  treeRef.value?.create()
}

const handleRefresh = () => {
  // 刷新后处理，如需要可以添加额外逻辑
}

onMounted(() => {
  treeRef.value?.load()
})
</script>

<style scoped>
.category-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
}
</style>

<template>
  <div class="category-sidebar">
    <div class="sidebar-title">分类目录</div>
    <el-tree
      :data="treeData"
      :props="treeProps"
      node-key="id"
      :default-expand-all="false"
      @node-click="handleNodeClick"
      class="category-tree"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <span class="node-label">{{ data.name }}</span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '@/api'

const router = useRouter()
const treeData = ref([])

const treeProps = {
  children: 'children',
  label: 'name'
}

const loadTree = async () => {
  try {
    const res = await publicApi.getCategoryTree()
    // axios 拦截器已返回 response.data，所以 res 直接是 { tree: [...] }
    treeData.value = res.tree || []
  } catch (e) {
    console.error('加载分类树失败', e)
  }
}

const handleNodeClick = (data) => {
  router.push({ name: 'CategoryArticleList', params: { id: data.id } })
}

onMounted(loadTree)
</script>

<style scoped>
.category-sidebar {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.category-tree {
  background: transparent;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}

.node-label {
  flex: 1;
}

:deep(.el-tree-node__content) {
  height: 36px;
}

:deep(.el-tree-node__content:hover) {
  background-color: #f5f7fa;
}
</style>

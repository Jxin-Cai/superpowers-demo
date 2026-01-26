<template>
  <div>
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between">
      <h2>分类管理</h2>
      <el-button type="primary" @click="$router.push('/admin/categories/new')">
        新建分类
      </el-button>
    </div>

    <el-table :data="categories" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="edit(row.id)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'

const router = useRouter()
const categories = ref([])

const load = async () => {
  try {
    const res = await categoryApi.adminGetAll()
    categories.value = res.data
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const edit = (id) => {
  router.push(`/admin/categories/${id}/edit`)
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该分类吗？', '确认', { type: 'warning' })
    await categoryApi.delete(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

onMounted(load)
</script>

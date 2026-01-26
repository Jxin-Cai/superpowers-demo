<template>
  <div>
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between">
      <h2>文章管理</h2>
      <el-button type="primary" @click="$router.push('/admin/articles/new')">
        新建文章
      </el-button>
    </div>

    <el-table :data="articles" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" width="200" />
      <el-table-column prop="categoryName" label="分类" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
            {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" @click="edit(row.id)">编辑</el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            size="small"
            type="success"
            @click="publish(row.id)"
          >
            发布
          </el-button>
          <el-button
            v-if="row.status === 'PUBLISHED'"
            size="small"
            type="warning"
            @click="unpublish(row.id)"
          >
            取消发布
          </el-button>
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
import { articleApi } from '@/api/article'

const router = useRouter()
const articles = ref([])

const load = async () => {
  try {
    const res = await articleApi.adminGetAll()
    articles.value = res.data
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const edit = (id) => {
  router.push(`/admin/articles/${id}/edit`)
}

const publish = async (id) => {
  try {
    await articleApi.publish(id)
    ElMessage.success('发布成功')
    load()
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

const unpublish = async (id) => {
  try {
    await articleApi.unpublish(id)
    ElMessage.success('已取消发布')
    load()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该文章吗？', '确认', { type: 'warning' })
    await articleApi.delete(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(load)
</script>

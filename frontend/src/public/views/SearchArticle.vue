<template>
  <div class="search-page">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索文章标题或关键词"
        clearable
        @keyup.enter="handleSearch"
        style="width: 400px"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      <el-select v-model="categoryId" placeholder="选择分类" clearable style="width: 200px; margin-left: 10px">
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
    </div>

    <div class="article-list">
      <el-card v-for="article in articles" :key="article.id" class="article-card" @click="goToDetail(article.id)">
        <h3>{{ article.title }}</h3>
        <p class="summary">{{ article.content?.substring(0, 100) }}...</p>
        <div class="meta">
          <el-tag size="small">{{ article.categoryName }}</el-tag>
          <span class="time">{{ formatDate(article.publishedAt) }}</span>
        </div>
      </el-card>

      <el-empty v-if="articles.length === 0 && !loading" description="没有找到相关文章" />
    </div>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      @current-change="handleSearch"
      layout="prev, pager, next"
      style="margin-top: 20px; text-align: center"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const keyword = ref('')
const categoryId = ref(null)
const articles = ref([])
const categories = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function fetchCategories() {
  try {
    const response = await api.get('/public/categories')
    categories.value = response.data.data
  } catch (error) {
    ElMessage.error('获取分类失败')
  }
}

async function handleSearch() {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      page: page.value - 1,
      size: size.value
    }
    const response = await api.get('/public/articles/search', { params })
    articles.value = response.data.data.content || []
    total.value = response.data.data.totalElements || 0
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

function goToDetail(id) {
  router.push(`/article/${id}`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

onMounted(() => {
  fetchCategories()
  handleSearch()
})
</script>

<style scoped>
.search-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.search-bar {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.article-list {
  min-height: 300px;
}

.article-card {
  margin-bottom: 15px;
  cursor: pointer;
  transition: box-shadow 0.3s;
}

.article-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.article-card h3 {
  margin: 0 0 10px 0;
}

.summary {
  color: #666;
  margin: 10px 0;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time {
  color: #999;
  font-size: 14px;
}
</style>

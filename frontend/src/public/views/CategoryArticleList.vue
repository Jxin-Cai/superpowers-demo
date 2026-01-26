<template>
  <div class="category-page">
    <header class="header">
      <div class="header-content">
        <h1 class="title">CMS 系统</h1>
        <nav class="nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link v-for="cat in categories" :key="cat.id" :to="`/category/${cat.id}`" class="nav-link">
            {{ cat.name }}
          </router-link>
          <router-link to="/admin" class="nav-link admin">后台管理</router-link>
        </nav>
      </div>
    </header>

    <main class="main">
      <h2 class="category-title">{{ category?.name || '加载中...' }}</h2>
      <div class="article-list">
        <div v-for="article in articles" :key="article.id" class="article-card" @click="viewArticle(article.id)">
          <h3 class="article-title">{{ article.title }}</h3>
          <div class="article-meta">
            <span class="time">{{ formatDate(article.publishedAt) }}</span>
          </div>
        </div>
        <div v-if="articles.length === 0" class="empty">该分类下暂无文章</div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const categoryId = computed(() => route.params.id)
const articles = ref([])
const categories = ref([])
const category = ref(null)

const loadArticles = async () => {
  try {
    const res = await articleApi.getPublished(categoryId.value)
    articles.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAll()
    categories.value = res.data
    category.value = res.data.find(c => c.id === Number(categoryId.value))
  } catch (e) {
    console.error(e)
  }
}

const viewArticle = (id) => {
  router.push(`/article/${id}`)
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString()
}

onMounted(() => {
  loadCategories()
  loadArticles()
})
</script>

<style scoped>
.category-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.header-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  margin: 0;
  font-size: 24px;
}

.nav {
  display: flex;
  gap: 20px;
}

.nav-link {
  text-decoration: none;
  color: #333;
  padding: 8px 16px;
  border-radius: 4px;
}

.nav-link:hover,
.nav-link.router-link-active {
  background-color: #f0f0f0;
}

.nav-link.admin {
  color: #409eff;
}

.main {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.category-title {
  margin-bottom: 24px;
}

.article-card {
  background-color: #fff;
  padding: 20px;
  margin-bottom: 12px;
  border-radius: 8px;
  cursor: pointer;
}

.article-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.article-title {
  margin: 0 0 8px;
  font-size: 18px;
}

.article-meta {
  color: #999;
  font-size: 14px;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>

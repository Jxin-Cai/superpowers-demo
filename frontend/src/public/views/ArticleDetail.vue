<template>
  <div class="article-page">
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
      <div v-if="article" class="article">
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-meta">
          <span class="category">{{ article.categoryName }}</span>
          <span class="time">{{ formatDate(article.publishedAt) }}</span>
        </div>
        <div class="article-content" v-html="article.renderedContent"></div>
      </div>
      <div v-else class="loading">加载中...</div>
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

const articleId = computed(() => route.params.id)
const article = ref(null)
const categories = ref([])

const loadArticle = async () => {
  try {
    const res = await articleApi.getById(articleId.value)
    article.value = res.data
  } catch (e) {
    router.push('/')
  }
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAll()
    categories.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadCategories()
  loadArticle()
})
</script>

<style scoped>
.article-page {
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

.article {
  background-color: #fff;
  padding: 40px;
  border-radius: 8px;
}

.article-title {
  margin: 0 0 16px;
  font-size: 32px;
}

.article-meta {
  display: flex;
  gap: 16px;
  color: #999;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.article-content {
  line-height: 1.8;
  color: #333;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  margin-top: 24px;
  margin-bottom: 16px;
}

.article-content :deep(p) {
  margin-bottom: 16px;
}

.article-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}

.article-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin-bottom: 16px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>

<template>
  <div class="home">
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
      <div class="article-list">
        <div v-for="article in articles" :key="article.id" class="article-card" @click="viewArticle(article.id)">
          <h3 class="article-title">{{ article.title }}</h3>
          <div class="article-meta">
            <span class="category">{{ article.categoryName }}</span>
            <span class="time">{{ formatDate(article.publishedAt) }}</span>
          </div>
          <div class="article-preview">{{ article.renderedContent?.slice(0, 200) }}...</div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const router = useRouter()
const articles = ref([])
const categories = ref([])

const loadArticles = async () => {
  try {
    const res = await articleApi.getPublished()
    articles.value = res.data
  } catch (e) {
    console.error(e)
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

const viewArticle = (id) => {
  router.push(`/article/${id}`)
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString()
}

onMounted(() => {
  loadArticles()
  loadCategories()
})
</script>

<style scoped>
.home {
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
  transition: background-color 0.3s;
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

.article-card {
  background-color: #fff;
  padding: 24px;
  margin-bottom: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.3s;
}

.article-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.article-title {
  margin: 0 0 12px;
  font-size: 20px;
}

.article-meta {
  display: flex;
  gap: 16px;
  color: #999;
  font-size: 14px;
  margin-bottom: 12px;
}

.article-preview {
  color: #666;
  line-height: 1.6;
}
</style>

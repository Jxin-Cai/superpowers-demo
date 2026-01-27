<template>
  <div class="article-page">
    <TopCategoryNav />

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
import TopCategoryNav from '@/public/components/TopCategoryNav.vue'

const route = useRoute()
const router = useRouter()

const articleId = computed(() => route.params.id)
const article = ref(null)

const loadArticle = async () => {
  try {
    const res = await articleApi.getById(articleId.value)
    article.value = res
  } catch (e) {
    router.push('/')
  }
}

const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped>
.article-page {
  min-height: 100vh;
  background-color: #f5f5f5;
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

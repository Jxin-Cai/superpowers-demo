<template>
  <div class="category-page">
    <TopCategoryNav />

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
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'
import TopCategoryNav from '@/public/components/TopCategoryNav.vue'

const route = useRoute()
const router = useRouter()

const categoryId = computed(() => route.params.id)
const articles = ref([])
const category = ref(null)

const loadArticles = async () => {
  try {
    const res = await articleApi.getPublished(categoryId.value)
    articles.value = res
  } catch (e) {
    console.error(e)
  }
}

const loadCategory = async () => {
  try {
    const res = await categoryApi.getAll()
    category.value = res.find(c => c.id === Number(categoryId.value))
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

// 监听路由参数变化，重新加载数据
watch(categoryId, () => {
  loadCategory()
  loadArticles()
})

onMounted(() => {
  loadCategory()
  loadArticles()
})
</script>

<style scoped>
.category-page {
  min-height: 100vh;
  background-color: #f5f5f5;
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

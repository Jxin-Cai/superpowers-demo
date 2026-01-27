<template>
  <div class="home-page">
    <TopCategoryNav />
    <div class="content-container">
      <CategorySidebar />
      <div class="main-content">
        <h1>最新文章</h1>
        <div class="article-list">
          <div
            v-for="article in articles"
            :key="article.id"
            class="article-item"
            @click="viewArticle(article.id)"
          >
            <h3>{{ article.title }}</h3>
            <p class="article-meta">
              <span>{{ getCategoryName(article.categoryId) }}</span>
              <span>{{ formatDate(article.publishedAt) }}</span>
            </p>
            <p class="article-excerpt">{{ getExcerpt(article.renderedContent) }}</p>
          </div>
        </div>
        <el-empty v-if="articles.length === 0" description="暂无文章" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopCategoryNav from '@/public/components/TopCategoryNav.vue'
import CategorySidebar from '@/public/components/CategorySidebar.vue'
import { publicApi } from '@/api'

const router = useRouter()
const articles = ref([])
const categories = ref([])

const loadArticles = async () => {
  try {
    const res = await publicApi.getPublishedArticles()
    articles.value = res.data
  } catch (e) {
    console.error('加载文章失败', e)
  }
}

const loadCategories = async () => {
  try {
    const res = await publicApi.getCategories()
    categories.value = res.data
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

const getCategoryName = (categoryId) => {
  const cat = categories.value.find(c => c.id === categoryId)
  return cat?.name || '未知'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const getExcerpt = (html) => {
  if (!html) return ''
  const text = html.replace(/<[^>]+>/g, '')
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

const viewArticle = (id) => {
  router.push({ name: 'ArticleDetail', params: { id } })
}

onMounted(() => {
  loadArticles()
  loadCategories()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.content-container {
  max-width: 1200px;
  margin: 20px auto;
  display: flex;
  gap: 20px;
  padding: 0 20px;
}

.main-content {
  flex: 1;
  background: #fff;
  border-radius: 4px;
  padding: 20px;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-item {
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.article-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.article-item h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.article-meta {
  color: #999;
  font-size: 14px;
  display: flex;
  gap: 16px;
}

.article-excerpt {
  color: #666;
  margin: 8px 0 0;
}
</style>

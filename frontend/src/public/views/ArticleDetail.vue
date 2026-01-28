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

        <!-- 点赞功能 -->
        <div class="like-section">
          <el-button
            :type="isLiked ? 'danger' : 'default'"
            @click="toggleLike"
            :disabled="!isLoggedIn"
            :loading="likeLoading"
          >
            {{ isLiked ? "已点赞" : "点赞" }}
          </el-button>
          <span class="like-count">{{ likeCount }} 人点赞</span>
          <span v-if="!isLoggedIn" class="login-tip">请先登录后点赞</span>
        </div>
      </div>
      <div v-else class="loading">加载中...</div>

      <!-- 评论区 -->
      <div v-if="article" class="comments-section">
        <h3>评论 ({{ comments.length }})</h3>

        <!-- 评论表单 -->
        <div v-if="isLoggedIn" class="comment-form">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <el-button
            type="primary"
            @click="submitComment"
            :loading="submitting"
            :disabled="!newComment.trim()"
            class="submit-btn"
          >
            发表评论
          </el-button>
        </div>
        <div v-else class="login-prompt">
          <el-alert title="请先登录后发表评论" type="info" show-icon />
        </div>

        <!-- 评论列表 -->
        <div v-if="comments.length > 0" class="comments-list">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <div class="comment-header">
              <span class="comment-author">{{ comment.author }}</span>
              <span class="comment-time">{{
                formatDate(comment.createdAt)
              }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
          </div>
        </div>
        <div v-else-if="!loading" class="no-comments">
          暂无评论，快来发表第一条评论吧！
        </div>
        <div v-if="loading" class="loading-comments">
          <el-skeleton :rows="3" animated />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { articleApi } from "@/api/article";
import commentApi from "@/api/comment";
import { useAuth } from "@/composables/useAuth";
import TopCategoryNav from "@/public/components/TopCategoryNav.vue";

const route = useRoute();
const router = useRouter();
const { isLoggedIn } = useAuth();

const articleId = computed(() => route.params.id);
const article = ref(null);
const comments = ref([]);
const newComment = ref("");
const isLiked = ref(false);
const likeCount = ref(0);
const loading = ref(false);
const submitting = ref(false);
const likeLoading = ref(false);

const loadArticle = async () => {
  try {
    const res = await articleApi.getById(articleId.value);
    article.value = res;
  } catch (e) {
    router.push("/");
  }
};

const loadComments = async () => {
  if (!articleId.value) return;

  loading.value = true;
  try {
    const res = await commentApi.getCommentsByArticle(articleId.value);
    comments.value = res;
  } catch (error) {
    ElMessage.error("加载评论失败");
  } finally {
    loading.value = false;
  }
};

const loadLikeStatus = async () => {
  if (!articleId.value) return;

  try {
    const [statusRes, countRes] = await Promise.all([
      articleApi.getLikeStatus(articleId.value),
      articleApi.getLikeCount(articleId.value),
    ]);
    isLiked.value = statusRes.liked;
    likeCount.value = countRes;
  } catch (error) {
    console.error("加载点赞状态失败:", error);
  }
};

const submitComment = async () => {
  if (!newComment.value.trim()) return;

  submitting.value = true;
  try {
    await commentApi.createComment({
      articleId: articleId.value,
      content: newComment.value.trim(),
    });
    newComment.value = "";
    ElMessage.success("评论发表成功");
    await loadComments(); // 刷新评论列表
  } catch (error) {
    ElMessage.error("评论发表失败");
  } finally {
    submitting.value = false;
  }
};

const toggleLike = async () => {
  if (!isLoggedIn.value) {
    ElMessage.info("请先登录后点赞");
    return;
  }

  likeLoading.value = true;
  try {
    if (isLiked.value) {
      await articleApi.unlikeArticle(articleId.value);
      ElMessage.success("取消点赞");
    } else {
      await articleApi.likeArticle(articleId.value);
      ElMessage.success("点赞成功");
    }
    await loadLikeStatus(); // 刷新点赞状态
  } catch (error) {
    ElMessage.error("操作失败");
  } finally {
    likeLoading.value = false;
  }
};

const formatDate = (date) => {
  return new Date(date).toLocaleString();
};

onMounted(async () => {
  await loadArticle();
  if (article.value) {
    await Promise.all([loadComments(), loadLikeStatus()]);
  }
});
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
  margin-bottom: 20px;
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

/* 点赞区域 */
.like-section {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 12px;
}

.like-count {
  color: #666;
  font-size: 14px;
}

.login-tip {
  color: #999;
  font-size: 12px;
}

/* 评论区 */
.comments-section {
  background-color: #fff;
  padding: 30px;
  border-radius: 8px;
}

.comments-section h3 {
  margin: 0 0 20px;
  font-size: 20px;
  color: #333;
}

.comment-form {
  margin-bottom: 30px;
}

.submit-btn {
  margin-top: 12px;
}

.login-prompt {
  margin-bottom: 30px;
}

.comments-list {
  margin-top: 20px;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 500;
  color: #333;
}

.comment-time {
  color: #999;
  font-size: 12px;
}

.comment-content {
  color: #666;
  line-height: 1.6;
  white-space: pre-wrap;
}

.no-comments {
  text-align: center;
  padding: 40px;
  color: #999;
}

.loading-comments {
  margin-top: 20px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>

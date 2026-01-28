import api from './index'

export const articleApi = {
  // 前台
  getPublished: (categoryId) => api.get('/public/articles', { params: { categoryId } }),
  getById: (id) => api.get(`/public/articles/${id}`),

  // 后台
  adminGetAll: () => api.get('/admin/articles'),
  adminGetById: (id) => api.get(`/admin/articles/${id}`),
  create: (data) => api.post('/admin/articles', data),
  update: (id, data) => api.put(`/admin/articles/${id}`, data),
  delete: (id) => api.delete(`/admin/articles/${id}`),
  publish: (id) => api.post(`/admin/articles/${id}/publish`),
  unpublish: (id) => api.post(`/admin/articles/${id}/unpublish`),

  // 点赞相关
  likeArticle: (articleId) => api.post(`/article-likes/${articleId}`),
  unlikeArticle: (articleId) => api.delete(`/article-likes/${articleId}`),
  getLikeStatus: (articleId) => api.get(`/article-likes/${articleId}/status`),
  getLikeCount: (articleId) => api.get(`/article-likes/${articleId}/count`)
}

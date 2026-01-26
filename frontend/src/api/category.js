import api from './index'

export const categoryApi = {
  // 前台
  getAll: () => api.get('/public/categories'),

  // 后台
  adminGetAll: () => api.get('/admin/categories'),
  create: (data) => api.post('/admin/categories', data),
  update: (id, data) => api.put(`/admin/categories/${id}`, data),
  delete: (id) => api.delete(`/admin/categories/${id}`)
}

import api from './index'

export const categoryApi = {
  // 前台
  getAll: () => api.get('/public/categories'),

  // 后台
  getTree: () => api.get('/admin/categories/tree'),
  adminGetAll: () => api.get('/admin/categories'),
  create: (data) => api.post('/admin/categories', data),
  update: (id, data) => api.put(`/admin/categories/${id}`, data),
  delete: (id, cascade = false) => api.delete(`/admin/categories/${id}`, { params: { cascade } }),
  moveToCategory: (id, newParentId) => api.put(`/admin/categories/${id}/move`, { newParentId }),
  reorder: (data) => api.put('/admin/categories/reorder', data)
}

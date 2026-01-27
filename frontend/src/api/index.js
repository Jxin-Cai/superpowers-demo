import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.response.use(
  response => response.data.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export const publicApi = {
  getCategoryTree: () => api.get('/public/categories/tree'),
  getPublishedArticles: () => api.get('/public/articles'),
  getCategories: () => api.get('/public/categories')
}

export default api

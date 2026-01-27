import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use(
  (config) => {
    const authData = JSON.parse(localStorage.getItem('cms_auth') || 'null')
    if (authData?.credentials) {
      config.headers.Authorization = `Basic ${authData.credentials}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  response => response.data.data,
  error => {
    if (error.response?.status === 401) {
      window.location.href = '/login'
    }
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

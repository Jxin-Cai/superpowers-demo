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
  response => {
    // 检查响应是否是JSON格式
    if (!response.data || typeof response.data !== 'object') {
      return response.data
    }
    
    const { code, message, data } = response.data
    
    // 如果没有code字段，说明不是标准ApiResponse格式，直接返回
    if (code === undefined) {
      return response.data
    }
    
    // 检查业务状态码
    if (code !== 200) {
      const error = new Error(message || '请求失败')
      error.code = code
      return Promise.reject(error)
    }
    return data
  },
  error => {
    if (error.response?.status === 401) {
      // 避免在登录页和注册页重复重定向
      if (!window.location.pathname.startsWith('/login') && 
          !window.location.pathname.startsWith('/register')) {
        localStorage.removeItem('cms_auth')
        window.location.href = '/login'
      }
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

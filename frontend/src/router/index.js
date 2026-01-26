import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('../public/views/Home.vue')
    },
    {
      path: '/category/:id',
      component: () => import('../public/views/CategoryArticleList.vue')
    },
    {
      path: '/article/:id',
      component: () => import('../public/views/ArticleDetail.vue')
    },
    {
      path: '/search',
      component: () => import('../public/views/SearchArticle.vue')
    },
    {
      path: '/login',
      component: () => import('../auth/Login.vue')
    },
    {
      path: '/register',
      component: () => import('../auth/Register.vue')
    },
    {
      path: '/admin',
      component: () => import('../admin/views/AdminLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/admin/articles'
        },
        {
          path: 'categories',
          component: () => import('../admin/views/CategoryList.vue')
        },
        {
          path: 'categories/new',
          component: () => import('../admin/views/CategoryForm.vue')
        },
        {
          path: 'categories/:id/edit',
          component: () => import('../admin/views/CategoryForm.vue')
        },
        {
          path: 'articles',
          component: () => import('../admin/views/ArticleList.vue')
        },
        {
          path: 'articles/new',
          component: () => import('../admin/views/ArticleForm.vue')
        },
        {
          path: 'articles/:id/edit',
          component: () => import('../admin/views/ArticleForm.vue')
        },
        {
          path: 'users',
          component: () => import('../admin/views/UserList.vue')
        }
      ]
    }
  ]
})

export default router

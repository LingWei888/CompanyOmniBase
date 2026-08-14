import { createRouter, createWebHistory } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'chat',
      component: () => import('@/views/chat/ChatView.vue'),
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard',
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/AdminDashboardView.vue'),
        },
        {
          path: 'knowledge',
          name: 'admin-knowledge',
          component: () => import('@/views/admin/AdminPlaceholderView.vue'),
          props: { title: '知识库管理', tip: 'Day3 实现知识库 CRUD' },
        },
        {
          path: 'documents',
          name: 'admin-documents',
          component: () => import('@/views/admin/AdminPlaceholderView.vue'),
          props: { title: '文档管理', tip: 'Day3/Day11 实现文档上传与状态管理' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAdminAuthStore()
  if (to.meta.requiresAdmin && !auth.isLoggedIn) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { name: 'admin-dashboard' }
  }
  return true
})

export default router

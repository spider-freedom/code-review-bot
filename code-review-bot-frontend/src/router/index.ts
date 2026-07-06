import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        {
          path: '',
          name: 'review',
          component: () => import('@/views/ReviewView.vue'),
        },
        {
          path: 'history',
          name: 'history',
          component: () => import('@/views/HistoryView.vue'),
        },
        {
          path: 'review/:id',
          name: 'review-detail',
          component: () => import('@/views/ReviewDetail.vue'),
        },
      ],
    },
  ],
})

export default router

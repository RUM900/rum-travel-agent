import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: 'RUM', transition: 'fade' } },
  { path: '/travel-planner', name: 'TravelPlanner', component: () => import('../views/TravelPlanner.vue'), meta: { title: '旅行规划 — RUM', transition: 'slide' } },
  { path: '/super-agent', name: 'SuperAgent', component: () => import('../views/SuperAgent.vue'), meta: { title: '通用助手 — RUM', transition: 'slide' } }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to, from, next) => { if (to.meta.title) document.title = to.meta.title; next() })

export default router

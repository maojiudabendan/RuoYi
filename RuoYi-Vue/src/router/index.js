import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Welcome from '../views/Welcome.vue'

// 用 hash 模式（#/login），这样即使直接打开构建产物或刷新深链接也不会 404，
// 不需要后端做特殊路由回退配置。
const routes = [
  { path: '/', name: 'welcome', component: Welcome },
  { path: '/login', name: 'login', component: Login },
  { path: '/register', name: 'register', component: Register },
  // 任意未知路径都回到首页（首页会做登录态校验，未登录会跳登录页）
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router

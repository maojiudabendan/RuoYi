// 应用入口：创建 Vue 应用、挂载路由、全局样式、文件协议环境提示
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { mountEnvBanner } from './api/env'
import './assets/style.css'

// 如果是直接双击打开本地文件（file://），弹一条环境提示
mountEnvBanner()

createApp(App).use(router).mount('#app')

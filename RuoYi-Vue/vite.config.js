import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发服务器配置：
// - 端口 5173，浏览器访问 http://localhost:5173
// - 把 /api 请求代理到后端 LoginServer（端口 8080），
//   这样前端用相对路径 /api/* 就能同源访问后端，Cookie 会话也正常。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})

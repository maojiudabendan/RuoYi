<template>
  <section id="login-view" class="login-view">
    <div class="login-card">
      <!-- 品牌区 -->
      <div class="brand">
        <div class="logo">R</div>
        <h1>RouYi</h1>
        <p class="subtitle">个人工作站</p>
      </div>

      <!-- 登录表单：@submit.prevent 阻止默认刷新，改由 onSubmit 处理 -->
      <form id="login-form" class="login-form" @submit.prevent="onSubmit">
        <div class="field">
          <input id="username" v-model.trim="username" type="text" required placeholder=" " />
          <label for="username">用户名</label>
          <span class="line"></span>
        </div>
        <div class="field">
          <input id="password" v-model="password" type="password" required placeholder=" " />
          <label for="password">密码</label>
          <span class="line"></span>
        </div>
        <label class="remember">
          <span class="checkbox"><input type="checkbox" id="remember" v-model="remember" /><i></i></span>
          记住我（7 天内免登录）
        </label>
        <div id="login-error" class="login-error" ref="errRef">{{ error }}</div>
        <button id="login-btn" type="submit" class="btn-primary" :disabled="loading">
          <span>{{ loading ? '登录中…' : '登 录' }}</span>
        </button>
        <p class="hint">演示账号：admin / admin123 &nbsp;·&nbsp; demo / demo123</p>
        <p class="switch-link">还没有账号？<a @click.prevent="goRegister">立即注册</a></p>
      </form>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const remember = ref(false)
const loading = ref(false)
const error = ref('')
const errRef = ref(null)

// 写入错误并重新播放抖动动画
function showError(msg) {
  error.value = msg
  const el = errRef.value
  if (el) {
    el.style.animation = 'none'
    void el.offsetWidth // 强制重排，让动画能重新播放
    el.style.animation = ''
  }
}

function goRegister() {
  router.push('/register')
}

async function onSubmit() {
  if (!username.value || !password.value) {
    showError('请输入用户名和密码')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const d = await login(username.value, password.value, remember.value)
    if (d.success) {
      router.push('/')
    } else {
      showError(d.message || '登录失败')
      loading.value = false
    }
  } catch (err) {
    showError(err.message || '网络错误，请确认服务已启动')
    loading.value = false
  }
}
</script>

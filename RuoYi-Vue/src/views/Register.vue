<template>
  <section id="register-view" class="login-view">
    <div class="login-card">
      <!-- 品牌区 -->
      <div class="brand">
        <div class="logo">R</div>
        <h1>RouYi</h1>
        <p class="subtitle">创建你的工作台账号</p>
      </div>

      <!-- 注册表单 -->
      <form id="register-form" class="auth-form" @submit.prevent="onSubmit">
        <div class="field">
          <input id="reg-username" v-model.trim="username" type="text" required placeholder=" " />
          <label for="reg-username">用户名</label>
          <span class="line"></span>
        </div>
        <div class="field">
          <input id="reg-password" v-model="password" type="password" required placeholder=" " />
          <label for="reg-password">密码（至少 6 位）</label>
          <span class="line"></span>
        </div>
        <div class="field">
          <input id="reg-confirm" v-model="confirm" type="password" required placeholder=" " />
          <label for="reg-confirm">确认密码</label>
          <span class="line"></span>
        </div>
        <div id="register-error" class="login-error" ref="errRef">{{ error }}</div>
        <button id="register-btn" type="submit" class="btn-primary" :disabled="loading">
          <span>{{ loading ? '注册中…' : '注 册' }}</span>
        </button>
        <p class="switch-link">已有账号？<a @click.prevent="goLogin">去登录</a></p>
      </form>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const confirm = ref('')
const loading = ref(false)
const error = ref('')
const errRef = ref(null)

function regError(m) {
  error.value = m
  const el = errRef.value
  if (el) {
    el.style.animation = 'none'
    void el.offsetWidth
    el.style.animation = ''
  }
}

function goLogin() {
  router.push('/login')
}

async function onSubmit() {
  if (!username.value || !password.value) {
    regError('请填写用户名和密码')
    return
  }
  if (password.value !== confirm.value) {
    regError('两次输入的密码不一致')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const d = await register(username.value, password.value)
    if (d.success) {
      router.push('/')
    } else {
      regError(d.message || '注册失败')
      loading.value = false
    }
  } catch (err) {
    regError(err.message || '网络错误，请确认服务已启动')
    loading.value = false
  }
}
</script>

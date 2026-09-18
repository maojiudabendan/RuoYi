<template>
  <!-- 受保护页：未登录会被 checkSession 跳走；ready 前不渲染，避免闪烁 -->
  <section id="app-view" class="welcome-shell" :class="{ show: shown }" v-if="ready">
    <div class="welcome-card">
      <div class="brand">
        <div class="logo">R</div>
        <h1>RouYi</h1>
      </div>
      <div class="avatar big" id="welcome-avatar" :style="avatarStyle">{{ initial }}</div>
      <h2 id="welcome-greet" class="welcome-greet">{{ greet }}</h2>
      <p class="welcome-sub">欢迎回来，{{ username }} 👋</p>
      <button id="logout-btn" class="btn-primary" @click="onLogout">退 出 登 录</button>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { me, logout } from '../api'

const router = useRouter()
const ready = ref(false)
const shown = ref(false)
const username = ref('')
const initial = ref('U')
const greet = ref('你好')
const avatarStyle = ref({})

// 简单字符串哈希，用来给头像生成稳定颜色
function hashStr(s) {
  let h = 0
  for (let i = 0; i < (s || '').length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return h
}

function applyIdentity(name) {
  initial.value = (name || 'U').charAt(0).toUpperCase()
  const hue = Math.abs(hashStr(name)) % 360
  const grad = `linear-gradient(135deg, hsl(${hue} 70% 65%), hsl(${(hue + 55) % 360} 70% 58%))`
  avatarStyle.value = { background: grad, color: '#0e1117' }
  username.value = name
  const h = new Date().getHours()
  const word = h < 6 ? '夜深了' : h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
  greet.value = word + '，' + name
}

async function checkSession() {
  const res = await me()
  if (res.ok) {
    applyIdentity(res.data.username)
    ready.value = true
    await nextTick()
    // 加一点延迟再触发淡入动画，过渡更自然
    setTimeout(() => { shown.value = true }, 60)
  } else {
    router.replace('/login')
  }
}

async function onLogout() {
  await logout()
  router.replace('/login')
}

onMounted(checkSession)
</script>

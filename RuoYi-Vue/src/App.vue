<template>
  <!-- 动态背景（纯装饰，所有页面共用） -->
  <div class="bg-aurora"></div>
  <div class="bg-blobs">
    <span class="blob b1"></span>
    <span class="blob b2"></span>
    <span class="blob b3"></span>
  </div>

  <!-- 路由出口：登录 / 注册 / 欢迎页 在这里切换 -->
  <router-view />
</template>

<script setup>
// App 只负责全局布局（背景）和路由切换，具体逻辑在各视图组件里
</script>

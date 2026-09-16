<template>
  <div class="app-container">
    <!-- 顶部导航栏 / 模式切换切换器 -->
    <header class="app-header">
      <div class="brand">伴游行 (TripCraft)</div>
        <nav class="app-nav">
        <router-link to="/" class="nav-link">🗺️ 高德探索打卡</router-link>
        <router-link to="/footprint" class="nav-link">🇨🇳 全国足迹大盘</router-link>
        <router-link to="/planner" class="nav-link">🧭 行程规划</router-link>
        </nav>
    </header>

    <!-- 视图切换：keep-alive 保证切换时不重复销毁重绘 -->
    <main class="app-main">
        <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
</script>

<style>
.app-nav { display: flex; gap: 10px; }
.nav-link {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; border-radius: 20px;
  color: #0f172a; text-decoration: none; font-size: 13px;
}
.router-link-active.nav-link { background: #409eff; color: #fff; }

html, body, #app, .app-container {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
.app-header {
  position: absolute;
  top: 15px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 20px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 16px;
  border-radius: 30px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(6px);
}
.app-header .brand {
  font-weight: bold;
  font-size: 14px;
  color: #0f172a;
}
.app-main {
  width: 100%;
  height: 100%;
}
</style>

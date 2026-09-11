<template>
  <div class="app-container">
    <!-- 顶部导航栏 / 模式切换切换器 -->
    <header class="app-header">
      <div class="brand">伴游行 (TripCraft)</div>
      <el-radio-group v-model="currentView" size="small">
        <el-radio-button value="map">🗺️ 高德探索打卡</el-radio-button>
        <el-radio-button value="footprint">🇨🇳 全国足迹大盘</el-radio-button>
      </el-radio-group>
    </header>

    <!-- 视图切换：keep-alive 保证切换时不重复销毁重绘 -->
    <main class="app-main">
      <MapContainer v-show="currentView === 'map'" />
      <FootprintBoard v-if="currentView === 'footprint'" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import MapContainer from './components/MapContainer.vue'
import FootprintBoard from './components/FootprintBoard.vue'

const currentView = ref<'map' | 'footprint'>('map')
</script>

<style>
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

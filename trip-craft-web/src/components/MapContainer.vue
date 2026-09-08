<template>
  <div class="map-wrapper">
    <!-- 地图挂载容器，必须指定明确的高宽 -->
    <div id="amap-container"></div>
    <!-- 顶部浮层展示标题 -->
    <div class="map-card">
      <h3>伴游行 (TripCraft)</h3>
      <p>全国智能行程与空间底座已就绪</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, shallowRef } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

// ⚠️ 考点：地图实例必须用 shallowRef，绝不能用 ref！
const mapInstance = shallowRef<any>(null)

// ⚠️ 替换为你刚刚在高德申请的真实 Key 和安全密钥
const AMAP_KEY = import.meta.env.VITE_AMAP_KEY
const AMAP_SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE

onMounted(() => {
  // 1. 配置高德安全密钥（必须在 AMapLoader.load 之前注入）
  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: AMAP_SECURITY_CODE
  }

  // 2. 异步加载高德 JS API 2.0
  AMapLoader.load({
    key: AMAP_KEY,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.ControlBar'] // 常用控件插件
  })
    .then((AMap) => {
      // 3. 实例化地图
      mapInstance.value = new AMap.Map('amap-container', {
        viewMode: '3D', // 开启 3D 视角
        zoom: 11, // 初始缩放级别
        center: [116.397428, 39.90923], // 初始中心点（北京天安门：经度，纬度）
        mapStyle: 'amap://styles/normal' // 标准地图样式
      })

      // 4. 添加基础控件
      mapInstance.value.addControl(new AMap.Scale()) // 比例尺
      mapInstance.value.addControl(new AMap.ToolBar()) // 缩放工具条
      mapInstance.value.addControl(new AMap.ControlBar()) // 3D 罗盘与俯仰角
    })
    .catch((e) => {
      console.error('高德地图加载失败：', e)
    })
})

onUnmounted(() => {
  // 组件销毁时注销地图，释放 WebGL 上下文内存
  mapInstance.value?.destroy()
})
</script>

<style scoped>
.map-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

#amap-container {
  width: 100%;
  height: 100%;
}

.map-card {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.map-card h3 {
  margin: 0 0 8px 0;
  color: #1f2937;
}

.map-card p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}
</style>

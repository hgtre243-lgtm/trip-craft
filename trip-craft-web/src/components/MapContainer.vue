<template>
  <div class="map-wrapper">
    <div id="amap-container"></div>
    <div class="map-card">
      <h3>伴游行 (TripCraft)</h3>
      <p>💡 提示：在地图上任意位置点击，即可添加打卡标记</p>
      <p>当前打卡总数：{{ markerList.length }} 个</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, shallowRef, ref } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import axios from 'axios'

const mapInstance = shallowRef<any>(null)
const markerList = ref<any[]>([])

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY
const AMAP_SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE

// 1. 从后端加载所有已保存的标记，并渲染到地图上
const loadMarkersFromBackend = async (AMap: any) => {
  try {
    const res = await axios.get('/api/markers')
    markerList.value = res.data

    markerList.value.forEach((item) => {
      renderSingleMarker(AMap, item)
    })
  } catch (err) {
    console.error('获取打卡标记失败：', err)
  }
}

// 2. 在地图上渲染一个 Marker 标记
const renderSingleMarker = (AMap: any, data: any) => {
  const marker = new AMap.Marker({
    position: [data.longitude, data.latitude],
    title: data.title,
    label: {
      content: `<div style="background:#fff;padding:2px 6px;border-radius:4px;box-shadow:0 1px 4px rgba(0,0,0,0.2);font-size:12px;">${data.title}</div>`,
      direction: 'top'
    }
  })
  mapInstance.value.add(marker)
}

onMounted(() => {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }

  AMapLoader.load({
    key: AMAP_KEY,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar']
  }).then((AMap) => {
    mapInstance.value = new AMap.Map('amap-container', {
      viewMode: '3D',
      zoom: 10,
      center: [120.153576, 30.287459] // 默认定位到杭州
    })

    // 页面初次加载时，先从数据库拉取已有打卡点
    loadMarkersFromBackend(AMap)

    // 3. 核心交互：监听地图点击事件
    mapInstance.value.on('click', async (e: any) => {
      const lng = e.lnglat.getLng()
      const lat = e.lnglat.getLat()

      // 简单弹窗输入打卡名称
      const title = window.prompt(`检测到点击坐标：[${lng.toFixed(4)}, ${lat.toFixed(4)}]\n请输入此处的打卡名称：`)
      if (!title || title.trim() === '') return

      try {
        // 请求后端保存到 MySQL
        const postData = {
          title: title.trim(),
          longitude: lng,
          latitude: lat,
          notes: '用户在前端地图点选打卡'
        }
        await axios.post('/api/markers', postData)

        // 保存成功后，立即在前端地图插旗
        renderSingleMarker(AMap, postData)
        markerList.value.push(postData)
      } catch (error) {
        alert('保存标记失败，请检查后端服务是否启动')
        console.error(error)
      }
    })
  })
})

onUnmounted(() => {
  mapInstance.value?.destroy()
})
</script>

<style scoped>
.map-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
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
  background: rgba(255, 255, 255, 0.95);
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.map-card h3 {
  margin: 0 0 6px 0;
  color: #111827;
}
.map-card p {
  margin: 4px 0;
  color: #4b5563;
  font-size: 13px;
}
</style>

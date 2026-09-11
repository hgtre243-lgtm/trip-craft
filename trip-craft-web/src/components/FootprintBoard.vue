<template>
  <div class="footprint-wrapper">
    <!-- ECharts 渲染容器 -->
    <div id="echarts-china-map" class="chart-container" v-loading="loading"></div>

    <!-- 悬浮数据统计看板 -->
    <div class="stats-panel">
      <h3>🇨🇳 我的中国足迹大盘</h3>
      <div class="stat-item">
        <span class="label">已点亮省份</span>
        <span class="value">{{ visitedProvinces.length }} <small>/ 34</small></span>
      </div>
      <div class="stat-item">
        <span class="label">全国省份覆盖率</span>
        <span class="value">{{ ((visitedProvinces.length / 34) * 100).toFixed(1) }}%</span>
      </div>
      <div class="stat-item">
        <span class="label">累计打卡标记</span>
        <span class="value">{{ markers.length }} 个</span>
      </div>

      <div class="visited-tags">
        <p style="margin: 8px 0 4px; font-size: 12px; color: #666">已踏足：</p>
        <el-tag
          v-for="prov in visitedProvinces"
          :key="prov"
          type="warning"
          size="small"
          effect="dark"
          style="margin: 2px"
        >
          {{ prov }}
        </el-tag>
        <span v-if="visitedProvinces.length === 0" style="font-size: 12px; color: #999">暂无打卡，快去地图点亮吧！</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, shallowRef, ref, computed } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const chartInstance = shallowRef<echarts.ECharts | null>(null)
const loading = ref(true)
const markers = ref<any[]>([])

// 统计去过的省份列表（去重）
const visitedProvinces = computed(() => {
  const set = new Set<string>()
  markers.value.forEach((m) => {
    if (m.province) set.add(m.province)
  })
  return Array.from(set)
})

// 初始化 ECharts 中国地图
const initChinaMap = async () => {
  loading.value = true
  try {
    // 1. 加载本地省份 GeoJSON（public/100000_full.json，源于阿里云 DataV 官方数据，规避外网直连 403）
    let geoData: any
    try {
      const geoRes = await axios.get('/100000_full.json')
      geoData = geoRes.data
    } catch (err) {
      ElMessage.error('加载中国地图 GeoJSON 失败')
      console.error('[GeoJSON]', err)
      return
    }
    echarts.registerMap('china', geoData)

    // 2. 从后端拉取所有打卡点
    try {
      const markerRes = await axios.get('/api/markers')
      if (markerRes.data.code === 200) {
        markers.value = markerRes.data.data
      }
    } catch (err) {
      ElMessage.error('拉取打卡数据失败')
      console.error('[markers]', err)
      return
    }

    // 3. 构建高亮数据（已点亮省份设高分，未点亮设 0）
    const mapData = geoData.features.map((f: any) => {
      const provinceName = f.properties.name
      // 模糊匹配省份名称（例如："浙江省" 匹配 "浙江"）
      const isVisited = visitedProvinces.value.some((p) => p.includes(provinceName) || provinceName.includes(p))
      return {
        name: provinceName,
        value: isVisited ? 100 : 0
      }
    })

    // 4. 配置地图样式
    const chartDom = document.getElementById('echarts-china-map')
    if (!chartDom) return

    chartInstance.value = echarts.init(chartDom)
    const option: echarts.EChartsOption = {
      backgroundColor: '#1e293b', // 暗夜蓝底色
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          const status = params.value > 0 ? '✨ 已点亮' : '⚪ 未踏足'
          return `<b>${params.name}</b><br/>状态：${status}`
        }
      },
      visualMap: {
        show: false,
        min: 0,
        max: 100,
        inRange: {
          color: ['#334155', '#eab308'] // 未点亮：深灰蓝；已点亮：璀璨金黄
        }
      },
      series: [
        {
          name: '中国地图',
          type: 'map',
          map: 'china',
          roam: true, // 支持鼠标拖拽与缩放
          zoom: 1.2,
          emphasis: {
            label: { show: true, color: '#fff' },
            itemStyle: { areaColor: '#f59e0b' }
          },
          select: { disabled: true },
          data: mapData
        }
      ]
    }

    chartInstance.value.setOption(option)
  } finally {
    loading.value = false
  }
}

// 监听窗口自适应
const handleResize = () => {
  chartInstance.value?.resize()
}

onMounted(() => {
  initChinaMap()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance.value?.dispose()
})
</script>

<style scoped>
.footprint-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}
.chart-container {
  width: 100%;
  height: 100%;
}
.stats-panel {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
  width: 260px;
  background: rgba(30, 41, 59, 0.9);
  color: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}
.stats-panel h3 {
  margin: 0 0 14px 0;
  font-size: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 8px;
}
.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}
.stat-item .label {
  font-size: 13px;
  color: #94a3b8;
}
.stat-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #facc15;
}
.stat-item .value small {
  font-size: 12px;
  color: #94a3b8;
  font-weight: normal;
}
</style>

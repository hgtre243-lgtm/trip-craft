<template>
  <div class="footprint-wrapper">
    <!-- ECharts 渲染容器 -->
    <div id="echarts-china-map" class="chart-container" v-loading="loading"></div>

    <!-- 悬浮数据统计看板 -->
    <div class="stats-panel" v-if="stats">
      <h3>🇨🇳 我的中国足迹大盘</h3>
      <div class="stat-item">
        <span class="label">已点亮省份</span>
        <span class="value">{{ stats.visitedProvinceCount }} <small>/ 34</small></span>
      </div>
      <div class="stat-item">
        <span class="label">全国省份覆盖率</span>
        <span class="value">{{ stats.coverageRate }}%</span>
      </div>
      <div class="stat-item">
        <span class="label">累计打卡标记</span>
        <span class="value">{{ stats.totalMarkers }} 个</span>
      </div>

      <div class="visited-tags">
        <p style="margin: 8px 0 4px; font-size: 12px; color: #94a3b8">点击已点亮省份查看足迹：</p>
        <el-tag
          v-for="item in stats.provinceList"
          :key="item.province"
          type="warning"
          size="small"
          effect="dark"
          style="margin: 2px; cursor: pointer"
          @click="openProvinceDrawer(item.province)"
        >
          {{ item.province }} ({{ item.count }})
        </el-tag>
      </div>
    </div>

    <!-- 省份详情抽屉 (点击省份下钻) -->
    <el-drawer
      v-model="drawerVisible"
      :title="`📍 ${selectedProvince} · 历史足迹清单`"
      direction="rtl"
      size="380px"
    >
      <div v-loading="drawerLoading">
        <div v-if="provinceMarkers.length === 0" style="color: #999; text-align: center; margin-top: 40px;">
          暂无该省打卡数据
        </div>
        <div
          v-for="marker in provinceMarkers"
          :key="marker.id"
          class="marker-card"
        >
          <div class="marker-title">{{ marker.title }}</div>
          <div class="marker-meta">城市：{{ marker.city || '未知城市' }}</div>
          <div class="marker-notes" v-if="marker.notes">“{{ marker.notes }}”</div>
          <div class="marker-time">{{ marker.createdAt || '未知时间' }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, shallowRef, ref } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const chartInstance = shallowRef<echarts.ECharts | null>(null)
const loading = ref(true)
const stats = ref<any>(null)

// 抽屉状态
const drawerVisible = ref(false)
const drawerLoading = ref(false)
const selectedProvince = ref('')
const provinceMarkers = ref<any[]>([])

// 打开指定省份抽屉
const openProvinceDrawer = async (provinceName: string) => {
  selectedProvince.value = provinceName
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    // 调接口只拉该省份的数据
    const res = await axios.get(`/api/markers?province=${encodeURIComponent(provinceName)}`)
    if (res.data.code === 200) {
      provinceMarkers.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('获取省份打卡点失败')
  } finally {
    drawerLoading.value = false
  }
}

// 初始化地图
const initChinaMap = async () => {
  try {
    loading.value = true

    // 1. 获取地图 GeoJSON（改为使用本地 public/100000_full.json，避免局域网/离线访问公网 CDN 失败）
    const geoRes = await axios.get('/100000_full.json')
    echarts.registerMap('china', geoRes.data)

    // 2. 调后端专属统计接口 (享受 Redis 加速)
    const statsRes = await axios.get('/api/markers/stats')
    if (statsRes.data.code === 200) {
      stats.value = statsRes.data.data
    }

    const provinceStatsMap = new Map<string, number>()
    stats.value?.provinceList?.forEach((p: any) => {
      provinceStatsMap.set(p.province, p.count)
    })

    // 3. 构建染色数据
    const mapData = geoRes.data.features.map((f: any) => {
      const geoProvinceName = f.properties.name
      // 匹配省份
      let matchedCount = 0
      let fullProvinceName = ''
      for (const [pName, cnt] of provinceStatsMap.entries()) {
        if (pName.includes(geoProvinceName) || geoProvinceName.includes(pName)) {
          matchedCount = cnt
          fullProvinceName = pName
          break
        }
      }

      return {
        name: geoProvinceName,
        value: matchedCount > 0 ? 100 : 0,
        originalProvince: fullProvinceName || geoProvinceName,
        markerCount: matchedCount
      }
    })

    const chartDom = document.getElementById('echarts-china-map')
    if (!chartDom) return

    chartInstance.value = echarts.init(chartDom)
    const option: echarts.EChartsOption = {
      backgroundColor: '#1e293b',
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          const data = params.data
          if (!data) return params.name
          if (data.markerCount > 0) {
            return `<b>${params.name}</b><br/>✨ 已点亮！累计打卡 <b>${data.markerCount}</b> 处<br/><small style="color:#fbbf24">👉 点击查看详细足迹</small>`
          }
          return `<b>${params.name}</b><br/>⚪ 未踏足`
        }
      },
      visualMap: {
        show: false,
        min: 0,
        max: 100,
        inRange: {
          color: ['#334155', '#eab308'] // 深灰蓝 -> 璀璨金黄
        }
      },
      series: [
        {
          name: '中国地图',
          type: 'map',
          map: 'china',
          roam: true,
          zoom: 1.2,
          emphasis: {
            label: { show: true, color: '#fff' },
            itemStyle: { areaColor: '#f59e0b' }
          },
          data: mapData
        }
      ]
    }

    chartInstance.value.setOption(option)

    // 4. 监听 ECharts 地图点击事件：点击已点亮省份，打开下钻抽屉！
    chartInstance.value.on('click', (params: any) => {
      if (params.data && params.data.markerCount > 0) {
        openProvinceDrawer(params.data.originalProvince)
      } else {
        ElMessage.info(`${params.name} 尚未打卡，快去探索吧！`)
      }
    })

  } catch (err) {
    ElMessage.error('加载足迹大盘失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initChinaMap()
  window.addEventListener('resize', () => chartInstance.value?.resize())
})

onUnmounted(() => {
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
  background: rgba(30, 41, 59, 0.92);
  color: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}
.stats-panel h3 {
  margin: 0 0 14px 0;
  font-size: 15px;
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
.marker-card {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 12px;
  border-left: 4px solid #eab308;
}
.marker-title {
  font-weight: bold;
  color: #1e293b;
  margin-bottom: 4px;
}
.marker-meta {
  font-size: 12px;
  color: #64748b;
}
.marker-notes {
  font-size: 13px;
  color: #475569;
  margin: 6px 0;
  font-style: italic;
}
.marker-time {
  font-size: 11px;
  color: #94a3b8;
}
</style>

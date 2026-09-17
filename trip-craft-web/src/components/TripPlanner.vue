<template>
  <div class="workbench-container">
    <!-- 1. 左栏：行程选择列表 (260px) -->
    <aside class="trip-sidebar">
      <div class="sidebar-header">
        <h3>我的行程清单</h3>
        <el-button type="primary" size="small" @click="openCreateDialog">+ 新建</el-button>
      </div>

      <div class="trip-list" v-loading="loading">
        <div v-if="tripList.length === 0" class="empty-tip">暂无行程</div>
        <div
          v-for="trip in tripList"
          :key="trip.id"
          class="trip-item"
          :class="{ active: currentTripId === trip.id }"
          @click="selectTrip(trip.id)"
        >
          <div class="trip-item-title">{{ trip.title }}</div>
          <div class="trip-item-dates">📅 {{ trip.startDate }} ({{ trip.totalDays }}天)</div>
        </div>
      </div>
    </aside>

    <!-- 2. 中栏：分天日程与拖拽编排 (380px) -->
    <section class="itinerary-panel" v-loading="detailLoading">
      <div v-if="currentTripDetail" class="itinerary-inner">
        <div class="panel-header">
          <h2>{{ currentTripDetail.plan.title }}</h2>
          <el-tag size="small" type="success">{{ currentTripDetail.plan.totalDays }} 日游排程</el-tag>
        </div>

        <!-- 分天 Tab -->
        <el-tabs v-model="activeDayId" class="day-tabs" @tab-change="handleTabChange">
          <el-tab-pane
            v-for="day in currentTripDetail.days"
            :key="day.id"
            :label="`Day ${day.dayIndex}`"
            :name="day.id"
          />
        </el-tabs>

        <!-- 节点列表操作栏 -->
        <div class="nodes-toolbar">
          <span class="toolbar-title">游玩路线清单 (可拖拽重排)</span>
          <el-button type="primary" size="small" plain @click="openAddNodeDialog">+ 搜地点添加</el-button>
        </div>

        <!-- 拖拽列表 -->
        <div class="nodes-list-wrapper" v-loading="nodesLoading">
          <div v-if="nodeList.length === 0" class="empty-node-tip">
            <p>📍 本日暂无地点</p>
            <small>点击右上角搜索并添加打卡地</small>
          </div>

          <draggable
            v-else
            v-model="nodeList"
            item-key="id"
            handle=".drag-handle"
            animation="200"
            @end="handleDragEnd"
          >
            <template #item="{ element, index }">
              <div class="node-card" @mouseenter="focusMarker(element)">
                <div class="drag-handle" title="按住拖拽">⋮⋮</div>
                <div class="node-order">{{ index + 1 }}</div>
                <div class="node-content">
                  <div class="node-name">{{ element.spotName }}</div>
                  <div class="node-coord" v-if="element.longitude">
                    {{ Number(element.longitude).toFixed(3) }}, {{ Number(element.latitude).toFixed(3) }}
                  </div>
                  <div class="node-notes" v-if="element.notes">💡 {{ element.notes }}</div>
                </div>
                <el-button
                  type="danger"
                  link
                  size="small"
                  @click="handleDeleteNode(element.id, element.spotName)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </draggable>
        </div>
      </div>
      <div v-else class="empty-detail">请选择左侧行程</div>
    </section>

    <!-- 3. 右栏：全屏高德地图轨迹大屏 (自适应宽度) -->
    <main class="map-panel">
      <div id="planner-amap" class="map-render-target"></div>
      <div class="map-floating-card" v-if="nodeList.length > 0">
        <span class="route-summary">
          🛣️ 本日路线：共 <b>{{ nodeList.length }}</b> 个打卡点，已按次序生成路网连线
        </span>
      </div>
    </main>

    <!-- 弹窗：新建行程 -->
    <el-dialog v-model="createDialogVisible" title="创建新出行计划" width="420px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="行程名称" required>
          <el-input v-model="createForm.title" placeholder="例如：杭州秋季3日游" />
        </el-form-item>
        <el-form-item label="起止日期" required>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateTrip">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：高德 POI 智能联想添加节点 -->
    <el-dialog v-model="addNodeDialogVisible" title="搜索添加游玩地点" width="460px">
      <el-form :model="nodeForm" label-width="80px">
        <el-form-item label="地点搜索" required>
          <el-autocomplete
            v-model="nodeForm.spotName"
            :fetch-suggestions="queryPoiSearch"
            placeholder="输入地名，如：西湖、灵隐寺、雷峰塔"
            clearable
            style="width: 100%"
            @select="handlePoiSelect"
          >
            <template #default="{ item }">
              <div class="poi-item-title">{{ item.value }}</div>
              <div class="poi-item-address">{{ item.address }}</div>
            </template>
          </el-autocomplete>
        </el-form-item>
        <el-form-item label="经纬坐标">
          <el-tag v-if="nodeForm.longitude" type="success" size="small">
            [{{ Number(nodeForm.longitude).toFixed(4) }}, {{ Number(nodeForm.latitude).toFixed(4) }}]
          </el-tag>
          <span v-else style="color: #94a3b8; font-size: 12px">从上方搜索列表中选择后自动解析</span>
        </el-form-item>
        <el-form-item label="游玩备注">
          <el-input v-model="nodeForm.notes" type="textarea" rows="2" placeholder="游玩建议、门票等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addNodeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addNodeLoading" @click="handleAddNode">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, shallowRef } from 'vue'
import axios from 'axios'
import draggable from 'vuedraggable'
import AMapLoader from '@amap/amap-jsapi-loader'
import { ElMessage, ElMessageBox } from 'element-plus'

// 地图与高德插件实例 (必须 shallowRef)
const mapInstance = shallowRef<any>(null)
let AMapRef: any = null
let autoCompleteInstance: any = null
const markersOnMap = shallowRef<any[]>([])
const polylineOnMap = shallowRef<any>(null)

// 环境变量配置
const AMAP_KEY = import.meta.env.VITE_AMAP_KEY
const AMAP_SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE

// 基础业务状态
const loading = ref(false)
const detailLoading = ref(false)
const nodesLoading = ref(false)
const tripList = ref<any[]>([])
const currentTripId = ref<number | null>(null)
const currentTripDetail = ref<any>(null)
const activeDayId = ref<number | null>(null)
const nodeList = ref<any[]>([])

// 弹窗状态
const createDialogVisible = ref(false)
const createLoading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const createForm = reactive({ title: '' })

const addNodeDialogVisible = ref(false)
const addNodeLoading = ref(false)
const nodeForm = reactive({
  spotName: '',
  longitude: null as number | null,
  latitude: null as number | null,
  notes: ''
})

// 1. 初始化高德地图及 AutoComplete 插件
const initMap = async () => {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }

  try {
    const AMap = await AMapLoader.load({
      key: AMAP_KEY,
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.AutoComplete']
    })
    AMapRef = AMap
    mapInstance.value = new AMap.Map('planner-amap', {
      viewMode: '3D',
      zoom: 12,
      center: [120.153576, 30.287459] // 默认杭州
    })

    // 初始化输入提示插件
    autoCompleteInstance = new AMap.AutoComplete({ city: '全国' })
  } catch (err) {
    console.error('地图加载失败', err)
  }
}

// 2. 高德 POI 联想搜索 (结合 Element Plus el-autocomplete)
const queryPoiSearch = (queryString: string, cb: (results: any[]) => void) => {
  if (!queryString || !autoCompleteInstance) {
    cb([])
    return
  }
  autoCompleteInstance.search(queryString, (status: string, result: any) => {
    if (status === 'complete' && result.tips) {
      const list = result.tips
        .filter((item: any) => item.location && item.location.lng && item.location.lat)
        .map((item: any) => ({
          value: item.name,
          address: item.district + (item.address || ''),
          lng: item.location.lng,
          lat: item.location.lat
        }))
      cb(list)
    } else {
      cb([])
    }
  })
}

// 选中 POI 联想项
const handlePoiSelect = (item: any) => {
  nodeForm.spotName = item.value
  nodeForm.longitude = item.lng
  nodeForm.latitude = item.lat
}

// 3. 核心：在地图上重新渲染带序号的 Marker 与 Polyline 轨迹折线
const renderItineraryOnMap = () => {
  if (!mapInstance.value || !AMapRef) return

  // 清除旧的覆盖物
  if (polylineOnMap.value) {
    mapInstance.value.remove(polylineOnMap.value)
    polylineOnMap.value = null
  }
  if (markersOnMap.value.length > 0) {
    mapInstance.value.remove(markersOnMap.value)
    markersOnMap.value = []
  }

  const pathCoords: any[] = []
  const newMarkers: any[] = []

  nodeList.value.forEach((node, index) => {
    if (node.longitude && node.latitude) {
      const pos = [Number(node.longitude), Number(node.latitude)]
      pathCoords.push(pos)

      // 自定义带圆形序号的 Marker 内容
      const markerContent = `
        <div style="
          background: #2563eb;
          color: #ffffff;
          width: 26px;
          height: 26px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: bold;
          border: 2px solid #ffffff;
          box-shadow: 0 2px 6px rgba(0,0,0,0.35);
        ">
          ${index + 1}
        </div>
      `

      const marker = new AMapRef.Marker({
        position: pos,
        title: `${index + 1}. ${node.spotName}`,
        offset: new AMapRef.Pixel(-13, -13),
        content: markerContent
      })

      mapInstance.value.add(marker)
      newMarkers.push(marker)
    }
  })

  markersOnMap.value = newMarkers

  // 如果节点 >= 2，绘制带箭头的高清路网折线
  if (pathCoords.length >= 2) {
    const polyline = new AMapRef.Polyline({
      path: pathCoords,
      strokeColor: '#3b82f6',
      strokeWeight: 6,
      strokeOpacity: 0.85,
      lineJoin: 'round',
      showDir: true // 折线上显示行进箭头
    })
    mapInstance.value.add(polyline)
    polylineOnMap.value = polyline
  }

  // 视口自动缩放平移以容纳所有点位
  if (newMarkers.length > 0) {
    mapInstance.value.setFitView()
  }
}

// 鼠标悬停聚焦单个节点
const focusMarker = (node: any) => {
  if (node.longitude && node.latitude && mapInstance.value) {
    mapInstance.value.panTo([Number(node.longitude), Number(node.latitude)])
  }
}

// 4. 业务数据流加载
const loadTrips = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/trips')
    if (res.data.code === 200) {
      tripList.value = res.data.data
      if (tripList.value.length > 0 && !currentTripId.value) {
        selectTrip(tripList.value[0].id)
      }
    }
  } catch (err) {
    ElMessage.error('获取行程列表失败')
  } finally {
    loading.value = false
  }
}

const selectTrip = async (id: number) => {
  currentTripId.value = id
  detailLoading.value = true
  try {
    const res = await axios.get(`/api/trips/${id}`)
    if (res.data.code === 200) {
      currentTripDetail.value = res.data.data
      if (currentTripDetail.value.days && currentTripDetail.value.days.length > 0) {
        activeDayId.value = currentTripDetail.value.days[0].id
        await loadNodes(activeDayId.value!)
      }
    }
  } catch (err) {
    ElMessage.error('获取行程详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleTabChange = (tabId: any) => {
  loadNodes(Number(tabId))
}

const loadNodes = async (dayId: number) => {
  nodesLoading.value = true
  try {
    const res = await axios.get(`/api/trips/days/${dayId}/nodes`)
    if (res.data.code === 200) {
      nodeList.value = res.data.data
      // 节点数据更新后，立即重绘地图连线与 Marker
      renderItineraryOnMap()
    }
  } catch (err) {
    ElMessage.error('加载日程节点失败')
  } finally {
    nodesLoading.value = false
  }
}

// 5. 拖拽完成同步保存并重绘连线
const handleDragEnd = async () => {
  // 拖拽结束先立即根据新次序重绘地图，给用户零延迟反馈
  renderItineraryOnMap()

  const orderedIds = nodeList.value.map((item) => item.id)
  try {
    const res = await axios.put(`/api/trips/days/${activeDayId.value}/nodes/reorder`, orderedIds)
    if (res.data.code === 200) {
      ElMessage.success('游览次序已同步更新')
    }
  } catch (err) {
    ElMessage.error('次序保存失败，请刷新')
    await loadNodes(activeDayId.value!)
  }
}

// 6. 添加与删除节点
const openAddNodeDialog = () => {
  nodeForm.spotName = ''
  nodeForm.longitude = null
  nodeForm.latitude = null
  nodeForm.notes = ''
  addNodeDialogVisible.value = true
}

const handleAddNode = async () => {
  if (!nodeForm.spotName.trim()) {
    ElMessage.warning('请输入地点名称')
    return
  }
  addNodeLoading.value = true
  try {
    const postData = {
      dayId: activeDayId.value,
      spotName: nodeForm.spotName.trim(),
      longitude: nodeForm.longitude,
      latitude: nodeForm.latitude,
      notes: nodeForm.notes.trim()
    }
    const res = await axios.post('/api/trips/nodes', postData)
    if (res.data.code === 200) {
      ElMessage.success('地点已加入排程')
      addNodeDialogVisible.value = false
      await loadNodes(activeDayId.value!)
    }
  } catch (err) {
    ElMessage.error('添加失败')
  } finally {
    addNodeLoading.value = false
  }
}

const handleDeleteNode = (id: number, name: string) => {
  ElMessageBox.confirm(`确定从排程中移除【${name}】吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '移除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      const res = await axios.delete(`/api/trips/nodes/${id}`)
      if (res.data.code === 200) {
        ElMessage.success('已移除')
        await loadNodes(activeDayId.value!)
      }
    } catch (err) {
      ElMessage.error('删除失败')
    }
  })
}

// 7. 新建行程
const openCreateDialog = () => {
  createForm.title = ''
  dateRange.value = null
  createDialogVisible.value = true
}

const handleCreateTrip = async () => {
  if (!createForm.title.trim() || !dateRange.value) {
    ElMessage.warning('请填写完整信息')
    return
  }
  createLoading.value = true
  try {
    const postData = {
      title: createForm.title.trim(),
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    }
    const res = await axios.post('/api/trips', postData)
    if (res.data.code === 200) {
      ElMessage.success('行程创建成功')
      createDialogVisible.value = false
      await loadTrips()
      selectTrip(res.data.data.id)
    }
  } catch (err) {
    ElMessage.error('创建行程失败')
  } finally {
    createLoading.value = false
  }
}

onMounted(async () => {
  await initMap()
  await loadTrips()
})

onUnmounted(() => {
  mapInstance.value?.destroy()
})
</script>

<style scoped>
.workbench-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  background: #f8fafc;
  overflow: hidden;
}

/* 1. 左栏样式 */
.trip-sidebar {
  width: 260px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 15px;
  color: #1e293b;
}
.trip-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}
.empty-tip {
  text-align: center;
  color: #94a3b8;
  margin-top: 30px;
  font-size: 13px;
}
.trip-item {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fafc;
  margin-bottom: 8px;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}
.trip-item:hover {
  border-color: #93c5fd;
}
.trip-item.active {
  background: #eff6ff;
  border-color: #3b82f6;
}
.trip-item-title {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}
.trip-item-dates {
  font-size: 11px;
  color: #64748b;
  margin-top: 4px;
}

/* 2. 中栏样式 */
.itinerary-panel {
  width: 380px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}
.itinerary-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.panel-header {
  padding: 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.panel-header h2 {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}
.day-tabs {
  padding: 0 16px;
  background: #fff;
}
.nodes-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 8px;
}
.toolbar-title {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}
.nodes-list-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px 16px;
}
.empty-node-tip {
  text-align: center;
  padding: 40px 0;
  color: #94a3b8;
  border: 2px dashed #e2e8f0;
  border-radius: 8px;
}
.empty-node-tip p {
  margin: 0 0 6px 0;
}

/* 节点拖拽卡片 */
.node-card {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  transition: all 0.2s;
}
.node-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.12);
}
.drag-handle {
  cursor: grab;
  color: #94a3b8;
  font-size: 16px;
  padding-right: 8px;
}
.node-order {
  background: #2563eb;
  color: #fff;
  font-size: 11px;
  font-weight: bold;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
}
.node-content {
  flex: 1;
  min-width: 0;
}
.node-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.node-coord {
  font-size: 11px;
  color: #94a3b8;
  font-family: monospace;
}
.node-notes {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

/* 3. 右栏地图样式 */
.map-panel {
  flex: 1;
  position: relative;
  height: 100%;
}
.map-render-target {
  width: 100%;
  height: 100%;
}
.map-floating-card {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  padding: 8px 16px;
  border-radius: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  font-size: 13px;
  color: #1e293b;
}

/* 搜索下拉项样式 */
.poi-item-title {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}
.poi-item-address {
  font-size: 11px;
  color: #64748b;
  line-height: 1.2;
}
.empty-detail {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}
</style>

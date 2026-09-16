<template>
  <div class="planner-container">
    <!-- 左侧：行程列表与创建栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>我的行程清单</h3>
        <el-button type="primary" size="small" @click="openCreateDialog">+ 新建行程</el-button>
      </div>

      <div class="trip-list" v-loading="loading">
        <div v-if="tripList.length === 0" class="empty-tip">暂无行程，点击上方创建！</div>
        <div
          v-for="trip in tripList"
          :key="trip.id"
          class="trip-card"
          :class="{ active: currentTripId === trip.id }"
          @click="selectTrip(trip.id)"
        >
          <div class="trip-card-header">
            <span class="trip-title">{{ trip.title }}</span>
            <el-tag size="small" type="info">{{ trip.totalDays }} 天</el-tag>
          </div>
          <div class="trip-dates">📅 {{ trip.startDate }} ~ {{ trip.endDate }}</div>
        </div>
      </div>
    </div>

    <!-- 右侧：分天排程与拖拽卡片 -->
    <div class="main-content" v-loading="detailLoading">
      <div v-if="currentTripDetail" class="detail-wrapper">
        <div class="detail-header">
          <h2>{{ currentTripDetail.plan.title }}</h2>
          <span class="dates-tag">
            出行排期：{{ currentTripDetail.plan.startDate }} 至 {{ currentTripDetail.plan.endDate }}（共 {{ currentTripDetail.plan.totalDays }} 天）
          </span>
        </div>

        <!-- 核心交互：分天 Tab 栏 -->
        <el-tabs v-model="activeDayId" type="border-card" class="day-tabs" @tab-change="handleTabChange">
          <el-tab-pane
            v-for="day in currentTripDetail.days"
            :key="day.id"
            :label="`Day ${day.dayIndex} (${day.planDate.substring(5)})`"
            :name="day.id"
          >
            <div class="day-content">
              <div class="day-header">
                <h4>第 {{ day.dayIndex }} 天游玩排程清单</h4>
                <el-button size="small" type="success" plain @click="openAddNodeDialog">
                  + 添加游玩地点
                </el-button>
              </div>

              <!-- 拖拽列表核心：vuedraggable -->
              <div v-loading="nodesLoading">
                <div v-if="nodeList.length === 0" class="empty-node-box">
                  <p>📍 本日暂无游玩安排</p>
                  <small style="color: #94a3b8">点击右上角按钮添加本日游玩的景点、餐厅或酒店</small>
                </div>

                <draggable
                  v-else
                  :list="nodeList"
                  item-key="id"
                  handle=".drag-handle"
                  animation="200"
                  @end="handleDragEnd"
                >
                  <template #item="{ element, index }">
                    <div class="node-card">
                      <!-- 拖拽抓手手柄 -->
                      <div class="drag-handle" title="按住拖拽重排次序">⋮⋮</div>

                      <!-- 排序序号徽标 -->
                      <div class="order-badge">{{ index + 1 }}</div>

                      <!-- 地点核心信息 -->
                      <div class="node-info">
                        <div class="node-title">{{ element.spotName }}</div>
                        <div class="node-notes" v-if="element.notes">💡 备注：{{ element.notes }}</div>
                      </div>

                      <!-- 操作栏 -->
                      <div class="node-actions">
                        <el-button
                          type="danger"
                          size="small"
                          link
                          @click="handleDeleteNode(element.id, element.spotName)"
                        >
                          删除
                        </el-button>
                      </div>
                    </div>
                  </template>
                </draggable>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-else class="empty-detail">
        <p>👈 请在左侧选择一个行程查看日程排期</p>
      </div>
    </div>

    <!-- 弹窗 1：新建行程 -->
    <el-dialog v-model="createDialogVisible" title="创建新出行计划" width="450px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="行程名称" required>
          <el-input v-model="createForm.title" placeholder="例如：杭州秋季赏枫3日游" />
        </el-form-item>
        <el-form-item label="起止日期" required>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateTrip">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗 2：向当前分天添加游玩节点 -->
    <el-dialog v-model="addNodeDialogVisible" title="添加游玩地点" width="420px">
      <el-form :model="nodeForm" label-width="80px">
        <el-form-item label="地点名称" required>
          <el-input v-model="nodeForm.spotName" placeholder="例如：西湖断桥 / 灵隐寺" />
        </el-form-item>
        <el-form-item label="游玩备注">
          <el-input v-model="nodeForm.notes" type="textarea" rows="2" placeholder="游玩建议、门票信息、耗时预估等" />
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
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import draggable from 'vuedraggable'
import { ElMessage, ElMessageBox } from 'element-plus'

// 基础状态
const loading = ref(false)
const detailLoading = ref(false)
const nodesLoading = ref(false)
const tripList = ref<any[]>([])
const currentTripId = ref<number | null>(null)
const currentTripDetail = ref<any>(null)
const activeDayId = ref<number | null>(null)
const nodeList = ref<any[]>([])

// 新建行程弹窗表单
const createDialogVisible = ref(false)
const createLoading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const createForm = reactive({ title: '' })

// 添加节点弹窗表单
const addNodeDialogVisible = ref(false)
const addNodeLoading = ref(false)
const nodeForm = reactive({
  spotName: '',
  notes: ''
})

// 1. 拉取所有行程列表
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
    ElMessage.error('拉取行程失败')
  } finally {
    loading.value = false
  }
}

// 2. 选中行程并加载分天
const selectTrip = async (id: number) => {
  currentTripId.value = id
  detailLoading.value = true
  try {
    const res = await axios.get(`/api/trips/${id}`)
    if (res.data.code === 200) {
      currentTripDetail.value = res.data.data
      if (currentTripDetail.value.days && currentTripDetail.value.days.length > 0) {
        // 默认选中第一天
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

// 3. 切换分天 Tab 时加载该天的节点列表
const handleTabChange = (tabId: any) => {
  loadNodes(Number(tabId))
}

// 4. 加载指定分天的游玩节点列表
const loadNodes = async (dayId: number) => {
  nodesLoading.value = true
  try {
    const res = await axios.get(`/api/trips/days/${dayId}/nodes`)
    if (res.data.code === 200) {
      nodeList.value = res.data.data
    }
  } catch (err) {
    ElMessage.error('获取该日日程节点失败')
  } finally {
    nodesLoading.value = false
  }
}

// 5. 打开添加节点弹窗
const openAddNodeDialog = () => {
  nodeForm.spotName = ''
  nodeForm.notes = ''
  addNodeDialogVisible.value = true
}

// 6. 提交添加节点
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
      notes: nodeForm.notes.trim()
    }
    const res = await axios.post('/api/trips/nodes', postData)
    if (res.data.code === 200) {
      ElMessage.success('添加成功！')
      addNodeDialogVisible.value = false
      await loadNodes(activeDayId.value!)
    }
  } catch (err) {
    ElMessage.error('添加失败')
  } finally {
    addNodeLoading.value = false
  }
}

// 7. 删除节点
const handleDeleteNode = (id: number, name: string) => {
  ElMessageBox.confirm(`确定删除地点【${name}】吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      const res = await axios.delete(`/api/trips/nodes/${id}`)
      if (res.data.code === 200) {
        ElMessage.success('已删除')
        await loadNodes(activeDayId.value!)
      }
    } catch (err) {
      ElMessage.error('删除失败')
    }
  })
}

// 8. 核心亮点：拖拽结束触发后端批量重排
// 8. 核心亮点：拖拽结束触发后端批量重排
const handleDragEnd = () => {
  // v-model 改为 :list 后，需要在这里把拖拽后的新顺序同步回响应式列表（否则标签顺序和实际不符）
  nodeList.value = [...nodeList.value]
  const orderedIds = nodeList.value.map((item) => item.id)
  ;(async () => {
    try {
      const res = await axios.put(`/api/trips/days/${activeDayId.value}/nodes/reorder`, orderedIds)
      if (res.data.code === 200) {
        ElMessage.success('次序已自动同步保存')
      }
    } catch (err) {
      ElMessage.error('次序保存失败，请刷新重试')
      await loadNodes(activeDayId.value!)
    }
  })()
}

// const handleDragEnd = async () => {
//   const orderedIds = nodeList.value.map((item) => item.id)
//   try {
//     // 调用昨日编写的批量更新排序接口
//     const res = await axios.put(`/api/trips/days/${activeDayId.value}/nodes/reorder`, orderedIds)
//     if (res.data.code === 200) {
//       ElMessage.success('次序已自动同步保存')
//     }
//   } catch (err) {
//     ElMessage.error('次序保存失败，请刷新重试')
//     await loadNodes(activeDayId.value!)
//   }
// }

// 行程新建弹窗控制
const openCreateDialog = () => {
  createForm.title = ''
  dateRange.value = null
  createDialogVisible.value = true
}

const handleCreateTrip = async () => {
  if (!createForm.title.trim()) {
    ElMessage.warning('请输入行程名称')
    return
  }
  if (!dateRange.value || dateRange.value.length < 2) {
    ElMessage.warning('请选择起止日期')
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
      ElMessage.success('行程创建成功！')
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

onMounted(() => {
  loadTrips()
})
</script>

<style scoped>
.planner-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  background: #f8fafc;
}
.sidebar {
  width: 300px;
  background: #fff;
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
  margin-top: 40px;
  font-size: 13px;
}
.trip-card {
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
  margin-bottom: 10px;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}
.trip-card:hover {
  border-color: #93c5fd;
  transform: translateY(-1px);
}
.trip-card.active {
  background: #eff6ff;
  border-color: #3b82f6;
}
.trip-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.trip-title {
  font-weight: bold;
  color: #0f172a;
  font-size: 14px;
}
.trip-dates {
  font-size: 12px;
  color: #64748b;
}
.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
.detail-header {
  margin-bottom: 16px;
}
.detail-header h2 {
  margin: 0 0 6px 0;
  color: #0f172a;
}
.dates-tag {
  font-size: 13px;
  color: #64748b;
}
.day-tabs {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.day-content {
  padding: 16px;
}
.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.day-header h4 {
  margin: 0;
  color: #1e293b;
}
.empty-node-box {
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
}

/* 拖拽卡片样式 */
.node-card {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}
.node-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}
.drag-handle {
  cursor: grab;
  color: #94a3b8;
  font-size: 18px;
  padding: 0 8px 0 0;
  user-select: none;
}
.drag-handle:active {
  cursor: grabbing;
}
.order-badge {
  background: #3b82f6;
  color: #fff;
  font-weight: bold;
  font-size: 12px;
  width: 22px;
  height: 22px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  margin-right: 14px;
}
.node-info {
  flex: 1;
}
.node-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}
.node-notes {
  font-size: 12px;
  color: #64748b;
  margin-top: 3px;
}
.empty-detail {
  display: flex;
  height: 100%;
  justify-content: center;
  align-items: center;
  color: #94a3b8;
}
</style>

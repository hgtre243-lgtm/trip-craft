<template>
  <div class="planner-container">
    <!-- 左侧：行程列表与创建栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>我的行程清单</h3>
        <el-button type="primary" size="small" @click="openCreateDialog">+ 新建行程</el-button>
      </div>

      <div class="trip-list" v-loading="loading">
        <div v-if="tripList.length === 0" class="empty-tip">暂无行程，点击上方创建你的第一次旅行！</div>
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

    <!-- 右侧：当前选中的行程分天编排视图 -->
    <div class="main-content" v-loading="detailLoading">
      <div v-if="currentTripDetail" class="detail-wrapper">
        <div class="detail-header">
          <h2>{{ currentTripDetail.plan.title }}</h2>
          <span class="dates-tag">出行排期：{{ currentTripDetail.plan.startDate }} 至 {{ currentTripDetail.plan.endDate }}（共 {{ currentTripDetail.plan.totalDays }} 天）</span>
        </div>

        <!-- 核心交互：分天 Tab 栏 -->
        <el-tabs v-model="activeDayTab" type="border-card" class="day-tabs">
          <el-tab-pane
            v-for="day in currentTripDetail.days"
            :key="day.id"
            :label="`Day ${day.dayIndex} (${day.planDate.substring(5)})`"
            :name="`day_${day.dayIndex}`"
          >
            <div class="day-content">
              <div class="day-header">
                <h4>第 {{ day.dayIndex }} 天日程清单</h4>
                <el-button size="small" type="success" plain>+ 添加该日游玩地点</el-button>
              </div>
              <!-- 占位预告，为明天接入拖拽排程做准备 -->
              <div class="empty-node-box">
                <p>📍 本日暂未添加游玩节点</p>
                <small style="color: #94a3b8">点击上方按钮，或在地图模式中将打卡点添加至本日排程</small>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-else class="empty-detail">
        <p>👈 请在左侧选择一个行程查看日程排期</p>
      </div>
    </div>

    <!-- 新建行程弹窗 -->
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const detailLoading = ref(false)
const tripList = ref<any[]>([])
const currentTripId = ref<number | null>(null)
const currentTripDetail = ref<any>(null)
const activeDayTab = ref('day_1')

// 新建弹窗表单
const createDialogVisible = ref(false)
const createLoading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const createForm = reactive({
  title: ''
})

// 拉取行程列表
const loadTrips = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/trip-plan')
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

// 选中某个行程
const selectTrip = async (id: number) => {
  currentTripId.value = id
  detailLoading.value = true
  try {
    const res = await axios.get(`/api/trip-plan/${id}`)
    if (res.data.code === 200) {
      currentTripDetail.value = res.data.data
      activeDayTab.value = 'day_1'
    }
  } catch (err) {
    ElMessage.error('获取行程详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 打开创建弹窗
const openCreateDialog = () => {
  createForm.title = ''
  dateRange.value = null
  createDialogVisible.value = true
}

// 提交创建
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
    const res = await axios.post('/api/trip-plan', postData)
    if (res.data.code === 200) {
      ElMessage.success('行程创建成功！自动生成每日日程')
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
  background: #f1f5f9;
}
.sidebar {
  width: 320px;
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
  font-size: 16px;
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
  margin-bottom: 20px;
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
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.day-content {
  padding: 16px;
}
.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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
.empty-detail {
  display: flex;
  height: 100%;
  justify-content: center;
  align-items: center;
  color: #94a3b8;
}
</style>

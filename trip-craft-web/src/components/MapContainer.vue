<template>
  <div class="map-wrapper">
    <!-- 地图容器 -->
    <div id="amap-container"></div>

    <!-- 浮层控制卡片 -->
    <div class="map-card">
      <h3>伴游行 (TripCraft)</h3>
      <p>💡 点击地图空白处可【添加打卡点】</p>
      <p>💡 点击已有的图钉图标可【查看/删除】</p>
      <el-tag type="success" style="margin-top: 6px">
        当前打卡总数：{{ markerList.length }} 个
      </el-tag>
    </div>

    <!-- 1. 新增打卡弹窗 -->
    <el-dialog v-model="addDialogVisible" title="新增打卡标记" width="400px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="经纬度">
          <el-input :value="`[${form.longitude.toFixed(4)}, ${form.latitude.toFixed(4)}]`" disabled />
        </el-form-item>
        <el-form-item label="地点名称" required>
          <el-input v-model="form.title" placeholder="例如：西湖雷峰塔" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="打卡备注">
          <el-input v-model="form.notes" type="textarea" placeholder="写点游玩感受或攻略..." rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSaveMarker">保存标记</el-button>
      </template>
    </el-dialog>

    <!-- 2. 查看/删除标记详情抽屉/弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="打卡点详情" width="380px">
      <div v-if="activeMarker">
        <h4 style="margin: 0 0 10px 0; font-size: 18px">{{ activeMarker.title }}</h4>
        <p style="color: #666; margin: 6px 0"><strong>备注：</strong>{{ activeMarker.notes || '暂无备注' }}</p>
        <p style="color: #999; font-size: 12px">打卡时间：{{ activeMarker.created_at || '刚刚' }}</p>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="danger" @click="handleDeleteMarker">删除此打卡</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, shallowRef, ref, reactive } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const mapInstance = shallowRef<any>(null)
const markerList = ref<any[]>([])
// 保存高德地图生成的 AMap.Marker 对象映射，方便后续根据 ID 移除地图上的图标
const markerMap = new Map<number, any>()
let AMapRef: any = null

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY
const AMAP_SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE

// 弹窗状态
const addDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const submitLoading = ref(false)
const activeMarker = ref<any>(null)

// 新增表单数据
const form = reactive({
  title: '',
  longitude: 0,
  latitude: 0,
  notes: ''
})

// 1. 初始化拉取后端数据
const loadMarkersFromBackend = async () => {
  try {
    const res = await axios.get('/api/markers')
    // 注意：因后端改为了 Result 统一返回体，数据在 res.data.data 中
    if (res.data.code === 200) {
      markerList.value = res.data.data
      markerList.value.forEach((item) => renderSingleMarker(item))
    }
  } catch (err) {
    ElMessage.error('拉取打卡标记失败')
  }
}

// 2. 渲染单个 Marker 并绑定点击事件
const renderSingleMarker = (data: any) => {
  const marker = new AMapRef.Marker({
    position: [data.longitude, data.latitude],
    title: data.title,
    label: {
      content: `<div style="background:#fff;padding:2px 8px;border-radius:4px;box-shadow:0 1px 4px rgba(0,0,0,0.2);font-size:12px;">${data.title}</div>`,
      direction: 'top'
    }
  })

  // 点击已有 Marker，弹出详情对话框
  marker.on('click', () => {
    activeMarker.value = data
    detailDialogVisible.value = true
  })

  mapInstance.value.add(marker)
  // 用数据库 ID 记录地图上的图标引用
  if (data.id) {
    markerMap.set(data.id, marker)
  }
}

// 3. 提交保存标记
const handleSaveMarker = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入地点名称')
    return
  }
  submitLoading.value = true
  try {
    const res = await axios.post('/api/markers', form)
    if (res.data.code === 200) {
      const newMarker = res.data.data
      renderSingleMarker(newMarker)
      markerList.value.push(newMarker)
      ElMessage.success('打卡成功！')
      addDialogVisible.value = false
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

// 4. 删除标记
const handleDeleteMarker = () => {
  ElMessageBox.confirm(`确定要删除打卡点【${activeMarker.value.title}】吗？`, '警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const id = activeMarker.value.id
      const res = await axios.delete(`/api/markers/${id}`)
      if (res.data.code === 200) {
        // 从地图实例中物理移除图钉
        const amapMarker = markerMap.get(id)
        if (amapMarker) {
          mapInstance.value.remove(amapMarker)
          markerMap.delete(id)
        }
        // 从本地响应式列表中移除
        markerList.value = markerList.value.filter((item) => item.id !== id)
        detailDialogVisible.value = false
        ElMessage.success('已删除打卡点')
      }
    } catch (e) {
      ElMessage.error('删除失败')
    }
  })
}

onMounted(() => {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }

  AMapLoader.load({
    key: AMAP_KEY,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar']
  }).then((AMap) => {
    AMapRef = AMap
    mapInstance.value = new AMap.Map('amap-container', {
      viewMode: '3D',
      zoom: 10,
      center: [120.153576, 30.287459]
    })

    loadMarkersFromBackend()

    // 点击空白处打开 Element Plus 弹窗
    mapInstance.value.on('click', (e: any) => {
      form.title = ''
      form.notes = ''
      form.longitude = e.lnglat.getLng()
      form.latitude = e.lnglat.getLat()
      addDialogVisible.value = true
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

<template>
  <div id="app">
    <div class="header">
      <h1>博学楼空闲教室查看系统</h1>
      <div class="time-info">
        <span>当前日期：{{ timeInfo.currentDate }}</span>
        <span>第{{ timeInfo.currentWeek }}周</span>
        <span>星期{{ dayNames[timeInfo.currentDay] }}</span>
        <span>{{ currentTimeStr }}</span>
      </div>
    </div>

    <SearchBar @search="handleSearch" />

    <RoomGrid
      :rooms="rooms"
      :loading="loading"
      @selectRoom="openRoomDetail"
    />

    <ScheduleModal
      v-if="selectedRoom"
      :room="selectedRoom"
      @close="selectedRoom = null"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getAllRooms, searchRooms, getCurrentTime } from './api/room'
import SearchBar from './components/SearchBar.vue'
import RoomGrid from './components/RoomGrid.vue'
import ScheduleModal from './components/ScheduleModal.vue'

const rooms = ref([])
const loading = ref(true)
const selectedRoom = ref(null)
const timeInfo = ref({
  currentDate: '',
  currentWeek: 1,
  currentDay: 1,
  currentPeriod: 0
})

const dayNames = {
  1: '一',
  2: '二',
  3: '三',
  4: '四',
  5: '五',
  6: '六',
  7: '日'
}

const currentTimeStr = ref('')
let refreshTimer = null
let timeTimer = null

onMounted(async () => {
  await fetchRooms()
  await fetchTimeInfo()

  // 每分钟刷新教室状态
  refreshTimer = setInterval(fetchRooms, 60000)

  // 每秒更新时间显示
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (timeTimer) clearInterval(timeTimer)
})

async function fetchRooms() {
  try {
    loading.value = true
    const res = await getAllRooms()
    rooms.value = res.data
  } catch (error) {
    console.error('获取教室列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function fetchTimeInfo() {
  try {
    const res = await getCurrentTime()
    timeInfo.value = res.data
  } catch (error) {
    console.error('获取时间信息失败:', error)
  }
}

function updateTime() {
  const now = new Date()
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTimeStr.value = `${hours}:${minutes}:${seconds}`
}

async function handleSearch(keyword) {
  if (!keyword || keyword.trim() === '') {
    await fetchRooms()
    return
  }

  try {
    loading.value = true
    const res = await searchRooms(keyword)
    rooms.value = res.data
  } catch (error) {
    console.error('搜索教室失败:', error)
  } finally {
    loading.value = false
  }
}

function openRoomDetail(room) {
  selectedRoom.value = room
}
</script>

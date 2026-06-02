<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h2>{{ room.roomName }} - 课表详情</h2>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <div style="margin-bottom: 15px; color: #8892b0;">
          座位数：{{ room.seatCount }} | 当前第{{ currentWeek }}周
        </div>

        <table class="schedule-table">
          <thead>
            <tr>
              <th>节次</th>
              <th>周一</th>
              <th>周二</th>
              <th>周三</th>
              <th>周四</th>
              <th>周五</th>
              <th>周六</th>
              <th>周日</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="period in periods" :key="period.start">
              <td>
                <div>{{ period.label }}</div>
                <div class="period-label">{{ period.start }}-{{ period.end }}节</div>
              </td>
              <td
                v-for="day in 7"
                :key="day"
                :class="{ 'has-course': hasCourse(day, period.start) }"
              >
                <div v-if="getCourse(day, period.start)" class="course-info">
                  <div class="course-name">{{ getCourse(day, period.start).courseName }}</div>
                  <div class="teacher">{{ getCourse(day, period.start).teacher }}</div>
                </div>
                <div v-else style="color: #00b894;">空闲</div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRoomDetail } from '../api/room'

const props = defineProps({
  room: {
    type: Object,
    required: true
  }
})

defineEmits(['close'])

const loading = ref(true)
const scheduleData = ref({})
const currentWeek = ref(1)

const periods = [
  { label: '上午', start: 1, end: 2 },
  { label: '上午', start: 3, end: 4 },
  { label: '下午', start: 5, end: 6 },
  { label: '下午', start: 7, end: 8 },
  { label: '晚上', start: 9, end: 10 },
  { label: '晚上', start: 11, end: 12 }
]

onMounted(async () => {
  try {
    const res = await getRoomDetail(props.room.id)
    scheduleData.value = res.data.weeklySchedule || {}
    currentWeek.value = res.data.currentWeek || 1
  } catch (error) {
    console.error('获取课表详情失败:', error)
  } finally {
    loading.value = false
  }
})

function hasCourse(day, period) {
  const key = `${day}-${period}`
  return scheduleData.value[key] && scheduleData.value[key].length > 0
}

function getCourse(day, period) {
  const key = `${day}-${period}`
  const courses = scheduleData.value[key]
  if (courses && courses.length > 0) {
    return courses[0]
  }
  return null
}
</script>

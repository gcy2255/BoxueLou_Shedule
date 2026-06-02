<template>
  <div
    class="room-card"
    :class="room.isOccupied ? 'occupied' : 'free'"
    @click="$emit('click', room)"
  >
    <div class="room-name">{{ room.roomName }}</div>
    <div class="room-info">座位：{{ room.seatCount }}</div>
    <div class="room-status">
      {{ room.isOccupied ? '有课' : '空闲' }}
    </div>
    <div v-if="room.isOccupied && room.currentCourses.length > 0" class="course-info" style="margin-top: 8px; font-size: 0.8rem;">
      {{ room.currentCourses[0] }}
    </div>
  </div>
</template>

<script setup>
defineProps({
  room: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])
</script>

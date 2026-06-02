<template>
  <div class="room-grid-container">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="groupedRooms.length === 0" class="no-data">未找到教室</div>
    <div v-else>
      <div v-for="group in groupedRooms" :key="group.floor" class="floor-section">
        <h3 class="floor-title">{{ group.label }}</h3>
        <div class="room-grid">
          <RoomCard
            v-for="room in group.rooms"
            :key="room.id"
            :room="room"
            @click="$emit('selectRoom', room)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import RoomCard from './RoomCard.vue'

const props = defineProps({
  rooms: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['selectRoom'])

const floorLabels = {
  '-1': 'B层（地下一层）',
  '0': 'A层（地下）',
  '1': '1楼',
  '2': '2楼',
  '3': '3楼',
  '4': '4楼',
  '5': '5楼',
  '6': '6楼'
}

const groupedRooms = computed(() => {
  const groups = {}

  props.rooms.forEach(room => {
    const floor = room.floorNum
    if (!groups[floor]) {
      groups[floor] = {
        floor: floor,
        label: floorLabels[floor] || `${floor}楼`,
        rooms: []
      }
    }
    groups[floor].rooms.push(room)
  })

  // 按楼层排序
  return Object.values(groups).sort((a, b) => a.floor - b.floor)
})
</script>

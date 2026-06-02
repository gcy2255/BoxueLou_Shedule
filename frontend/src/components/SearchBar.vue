<template>
  <div class="search-container">
    <input
      type="text"
      class="search-input"
      :placeholder="placeholder"
      v-model="keyword"
      @input="onInput"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['search'])

const keyword = ref('')
const placeholder = '搜索教室名称，如：博学楼102'

let debounceTimer = null

function onInput() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    emit('search', keyword.value)
  }, 300)
}
</script>

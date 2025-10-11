<template>
    <div class="p-4">
      <h2 class="text-xl font-bold mb-4">객실 현황</h2>
  
      <ul class="space-y-2">
        <li v-for="room in rooms" :key="room.id" class="border p-2 rounded">
          <div class="flex justify-between">
            <span>{{ room.name }} ({{ room.type }})</span>
            <span class="text-sm text-gray-500">{{ room.status }}</span>
          </div>
        </li>
        <li v-if="!rooms.length" class="text-center text-gray-500 p-3 border rounded">
          데이터가 없습니다.
        </li>
      </ul>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import { get } from '@/api/_http'
  
  const rooms = ref([])
  
  onMounted(async () => {
    try {
      const data = await get('/admin/rooms')
      rooms.value = Array.isArray(data) ? data : []
    } catch (e) {
      console.error(e)
      rooms.value = []
    }
  })
  </script>
  
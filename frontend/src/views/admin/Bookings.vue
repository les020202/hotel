<template>
    <div class="p-4">
      <h2 class="text-xl font-bold mb-4">예약 관리</h2>
  
      <table class="w-full text-sm border">
        <thead class="bg-gray-100">
          <tr>
            <th class="p-2">예약번호</th>
            <th class="p-2">고객명</th>
            <th class="p-2">객실</th>
            <th class="p-2">체크인</th>
            <th class="p-2">상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in bookings" :key="b.id" class="border-t">
            <td class="p-2">#{{ b.id }}</td>
            <td class="p-2">{{ b.guest }}</td>
            <td class="p-2">{{ b.roomType }}</td>
            <td class="p-2">{{ dt(b.checkIn) }}</td>
            <td class="p-2">{{ b.status }}</td>
          </tr>
          <tr v-if="!bookings.length">
            <td colspan="5" class="p-3 text-center text-gray-500">데이터가 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import { get } from '@/api/_http'
  
  const bookings = ref([])
  
  const dt = (s) => (s ? String(s).replace('T',' ').slice(0,16) : '-')
  
  onMounted(async () => {
    try {
      bookings.value = await get('/admin/bookings')
    } catch (e) {
      console.error(e)
      bookings.value = []
    }
  })
  </script>
  
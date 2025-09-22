<template>
    <div class="p-4">
      <h2 class="text-xl font-bold mb-4">정산 관리</h2>
  
      <table class="w-full text-sm border">
        <thead class="bg-gray-100">
          <tr>
            <th class="p-2">정산ID</th>
            <th class="p-2">금액</th>
            <th class="p-2">상태</th>
            <th class="p-2">정산 예정일</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in settlements" :key="s.id" class="border-t">
            <td class="p-2">#{{ s.id }}</td>
            <td class="p-2">{{ n(s.amount) }}원</td>
            <td class="p-2">{{ s.status }}</td>
            <td class="p-2">{{ s.dueDate || '-' }}</td>
          </tr>
          <tr v-if="!settlements.length">
            <td colspan="4" class="p-3 text-center text-gray-500">데이터가 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import { get } from '@/api/_http'
  
  const settlements = ref([])
  
  const n = (v) => Number(v || 0).toLocaleString()
  
  onMounted(async () => {
    try {
      const data = await get('/admin/settlements')
      settlements.value = Array.isArray(data) ? data : []
    } catch (e) {
      console.error(e)
      settlements.value = []
    }
  })
  </script>
  
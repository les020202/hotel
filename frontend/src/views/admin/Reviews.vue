<template>
    <div class="p-4">
      <h2 class="text-xl font-bold mb-4">리뷰 / 신고 관리</h2>
      <div v-for="r in reviews" :key="r.id" class="border p-3 rounded mb-2">
        <div class="flex justify-between">
          <strong>{{ r.user }}</strong>
          <span class="text-sm text-gray-400">{{ r.createdAt }}</span>
        </div>
        <p class="mt-2">{{ r.content }}</p>
        <div v-if="r.reported" class="text-red-500 text-sm mt-2">🚨 신고됨</div>
      </div>
    </div>
  </template>
  
  <script setup>
 import { ref, onMounted } from 'vue'
import { get, post } from '@/api/_http'

  const reviews = ref([])
  
 
onMounted(async () => {
  reviews.value = await get('/admin/reviews') // Authorization 헤더 자동 첨부
})
  </script>
  
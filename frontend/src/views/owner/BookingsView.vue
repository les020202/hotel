<template>
  <div class="p-4 space-y-4">
    <h2 class="text-xl font-bold">내 호텔 예약 내역</h2>

    <!-- 예약 리스트 -->
    <div class="border rounded">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="p-2 text-left">예약번호</th>
            <th class="p-2 text-left">고객</th>
            <th class="p-2 text-left">객실</th>
            <th class="p-2 text-left">체크인</th>
            <th class="p-2 text-left">박수</th>
            <th class="p-2 text-center">상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in bookings" :key="r.bookingId" class="border-b">
            <td class="p-2">#{{ r.bookingId }}</td>
            <td class="p-2">
              <div class="font-medium">{{ r.userName || '-' }}</div>
              <div class="text-xs text-gray-500">{{ r.userLoginId }}</div>
            </td>
            <td class="p-2">{{ r.roomTypeName }}</td>
            <td class="p-2">{{ fmtDate(r.checkIn) }}</td>
            <td class="p-2">{{ r.nights ?? '-' }}</td>
            <td class="p-2 text-center">
              <span :class="badgeClass(r.status)">{{ r.status }}</span>
            </td>
          </tr>
          <tr v-if="!loading && !bookings.length">
            <td colspan="6" class="p-4 text-center text-gray-500">예약이 없습니다.</td>
          </tr>
          <tr v-if="loading">
            <td colspan="6" class="p-4 text-center text-gray-400">불러오는 중…</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '@/api/_http'

const route = useRoute()
const hotelId = route.params.hotelId

const bookings = ref([])
const loading = ref(false)

function fmtDate(d) {
  return d ?? '-'
}

function badgeClass(s) {
  return [
    'inline-block px-2 py-0.5 rounded text-xs font-semibold',
    s === 'CONFIRMED' ? 'bg-green-100 text-green-700' :
    s === 'CANCELED'  ? 'bg-red-100 text-red-700' :
    s === 'PENDING'   ? 'bg-yellow-100 text-yellow-700' :
                        'bg-gray-100 text-gray-700'
  ].join(' ')
}

async function load() {
  loading.value = true
  try {
    // 백엔드: GET /api/owner/hotels/{hotelId}/bookings
    const res = await get(`/owner/hotels/${hotelId}/bookings`)
    bookings.value = Array.isArray(res) ? res : res.content ?? []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

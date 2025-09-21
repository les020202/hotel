<script setup lang="js">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const hotelId = ref(null)
const myHotels = ref([])
const currentHotel = computed(() => myHotels.value.find(h => h.id === hotelId.value))
  import { api } from '@/lib/api'
async function loadHotels() {

const res = await api('/api/owner/hotels')
  if (res.ok) {
    myHotels.value = await res.json()
  }
}

onMounted(async () => {
  hotelId.value = Number(route.params.hotelId)
  await loadHotels()
})

watch(() => route.params.hotelId, (v) => { hotelId.value = Number(v) })
</script>

<template>
  <div>
    <h1 class="text-xl font-bold">오너 대시보드</h1>
    <p class="text-gray-500 mt-1" v-if="currentHotel">
      {{ currentHotel.name }} · {{ currentHotel.region }} · 사업자 {{ currentHotel.businessNo }}
    </p>

    <!-- 요약 카드 (샘플 자리표시자) -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6">
      <div class="rounded-2xl p-4 bg-white border">
        <div class="text-xs text-gray-500">오늘 판매 가능 객실</div>
        <div class="text-2xl font-semibold mt-1">—</div>
        <div class="text-xs text-gray-400 mt-1">룸타입 선택 시 인벤토리에서 확인</div>
      </div>
      <div class="rounded-2xl p-4 bg-white border">
        <div class="text-xs text-gray-500">이번 주 예약 수</div>
        <div class="text-2xl font-semibold mt-1">—</div>
        <div class="text-xs text-gray-400 mt-1">예약 API 연동 시 표시</div>
      </div>
      <div class="rounded-2xl p-4 bg-white border">
        <div class="text-xs text-gray-500">체크인 예정(오늘)</div>
        <div class="text-2xl font-semibold mt-1">—</div>
        <div class="text-xs text-gray-400 mt-1">예약 API 연동 시 표시</div>
      </div>
    </div>

    <div class="mt-8 flex gap-2">
      <button class="px-4 py-2 rounded-xl bg-black text-white hover:opacity-90"
              @click="router.push(`/owner/hotels/${hotelId}/inventory`)">
        재고 캘린더로 이동
      </button>
      <button class="px-4 py-2 rounded-xl border hover:bg-gray-100"
              @click="router.push(`/owner/hotels/${hotelId}/bookings`)">
        예약 목록으로 이동
      </button>
    </div>
  </div>
</template>

<script setup lang="js">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'

const route = useRoute()
const router = useRouter()

const hotels = ref([])               // [{id,name,region,gradeLevel,businessNo}]
const currentHotelId = ref(null)     // number

 import { api } from '@/lib/api'
async function fetchMyHotels() {
  try {
    const res = await api('/api/owner/hotels', { method: 'GET' })
    if (!res.ok) throw new Error('Failed to load my hotels')
    hotels.value = await res.json()
    // 초기 선택
    if (!currentHotelId.value && hotels.value.length) {
      currentHotelId.value = hotels.value[0].id
      // 현재 라우트가 /owner/hotels/:hotelId... 형태면 그대로 유지/치환
      const seg = route.path.split('/')
      const isChild = seg.includes('hotels') && seg.length >= 4
      router.replace(isChild ? `/owner/hotels/${currentHotelId.value}${childSuffix()}` 
                             : `/owner/hotels/${currentHotelId.value}`)
    }
  } catch (e) {
    console.error(e)
  }
}

function childSuffix() {
  // 현재 자식 경로 유지 (dashboard | inventory | bookings)
  if (route.path.endsWith('/inventory')) return '/inventory'
  if (route.path.endsWith('/bookings')) return '/bookings'
  return ''
}

function onHotelChange() {
  if (!currentHotelId.value) return
  router.push(`/owner/hotels/${currentHotelId.value}${childSuffix()}`)
}

// 라우트 파라미터에서 초기값 세팅
onMounted(async () => {
  const hid = Number(route.params.hotelId)
  if (!isNaN(hid)) currentHotelId.value = hid
  await fetchMyHotels()
})

// 라우트 변경으로 :hotelId 바뀌면 드롭다운 동기화
watch(() => route.params.hotelId, (v) => {
  const hid = Number(v)
  if (!isNaN(hid)) currentHotelId.value = hid
})
</script>

<template>
  <div class="flex min-h-screen text-sm">
    <!-- Sidebar -->
    <aside class="w-64 border-r bg-white">
      <div class="p-4 space-y-4">
        <div>
          <div class="text-xs text-gray-500">내 호텔</div>
          <select class="w-full mt-1 border rounded-lg p-2"
                  v-model.number="currentHotelId"
                  @change="onHotelChange"
                  :disabled="!hotels.length">
            <option v-for="h in hotels" :key="h.id" :value="h.id">
              {{ h.name }} ({{ h.businessNo }})
            </option>
          </select>
        </div>

        <nav class="space-y-1">
          <RouterLink
            :to="currentHotelId ? `/owner/hotels/${currentHotelId}` : '/owner'"
            class="block px-3 py-2 rounded hover:bg-gray-100"
            :class="{'pointer-events-none opacity-50': !currentHotelId}"
          >
            대시보드
          </RouterLink>
          <RouterLink
            :to="currentHotelId ? `/owner/hotels/${currentHotelId}/inventory` : '/owner'"
            class="block px-3 py-2 rounded hover:bg-gray-100"
            :class="{'pointer-events-none opacity-50': !currentHotelId}"
          >
            재고 캘린더
          </RouterLink>
          <RouterLink
            :to="currentHotelId ? `/owner/hotels/${currentHotelId}/bookings` : '/owner'"
            class="block px-3 py-2 rounded hover:bg-gray-100"
            :class="{'pointer-events-none opacity-50': !currentHotelId}"
          >
            예약
          </RouterLink>
          <RouterLink
            :to="`/owner/hotels/${currentHotelId}/assign`"
            class="block px-3 py-2 rounded-lg hover:bg-gray-100"
            :class="{'bg-gray-200 font-semibold': $route.path.includes('/assign')}"
            >
            호실 배정
        </RouterLink>
        <RouterLink :to="`/owner/hotels/${currentHotelId}/rooms`">객실 현황</RouterLink>

        </nav>
      </div>
    </aside>

    <!-- Main -->
    <main class="flex-1 p-6 bg-gray-50">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
/* 최소한의 스타일만 사용 (Tailwind 가정) */
</style>

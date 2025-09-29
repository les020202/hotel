<script setup lang="js">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/lib/api'

const route = useRoute()
const hotelId = ref(null)

const date = ref(new Date().toISOString().slice(0,10))  // 도착일
const arrivals = ref([])  // ArrivalItemDto[]
const selected = ref(null) // 선택된 arrival (bookingItemId 등)

const rooms = ref([])      // AvailableRoomDto[]
const upgrade = ref(true)

async function loadArrivals() {
  if (!hotelId.value) return
  const q = new URLSearchParams({ date: date.value }).toString()
  const res = await api(`/api/owner/hotels/${hotelId.value}/arrivals?`+q)
  arrivals.value = res.ok ? await res.json() : []
  selected.value = null
  rooms.value = []
}

async function loadRooms() {
  if (!hotelId.value || !selected.value) return
  const it = selected.value
  const q = new URLSearchParams({
    from: it.checkIn, to: it.checkOut,
    minCapacity: String(it.guests ?? 1),
    typeCode: it.typeCode,
    upgrade: String(upgrade.value)
  }).toString()
  const res = await api(`/api/owner/hotels/${hotelId.value}/rooms/available?`+q)
  rooms.value = res.ok ? await res.json() : []
}

async function assign(roomId) {
  if (!selected.value) return
  const body = { bookingItemId: selected.value.bookingItemId, roomId }
  const res = await api(`/api/owner/hotels/${hotelId.value}/assignments`, {
    method: 'POST', body: JSON.stringify(body)
  })
  if (res.ok) {
    const data = await res.json()
    alert(`배정 완료: ${data.assignedNights}박`)
    await loadArrivals()
  } else {
    alert('배정 실패: '+ await res.text())
  }
}

onMounted(() => {
  hotelId.value = Number(route.params.hotelId)
  loadArrivals()
})
watch(() => route.params.hotelId, v => { hotelId.value = Number(v); loadArrivals() })
</script>

<template>
  <div>
    <header class="topbar">
      <div class="title"><h1 class="text-xl font-bold">호실 배정</h1></div>
    </header>

    <div class="mt-4 flex gap-3 items-end">
      <div>
        <div class="text-xs text-gray-500">도착일</div>
        <input type="date" class="border rounded-lg p-2" v-model="date" />
      </div>
      <button class="px-4 py-2 rounded-lg bg-black text-white" @click="loadArrivals">조회</button>
    </div>

    <div class="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 좌: 도착/배정 대상 -->
      <div class="bg-white border rounded-2xl overflow-hidden">
        <div class="px-4 py-3 bg-gray-100 font-medium">도착 예정</div>
        <div v-if="!arrivals.length" class="p-4 text-gray-500">해당 일자 도착 예정 없음</div>

        <ul>
          <li v-for="a in arrivals" :key="a.bookingItemId"
              class="p-4 border-t cursor-pointer"
              :class="{'bg-blue-50': selected?.bookingItemId===a.bookingItemId}"
              @click="selected=a; loadRooms()">
            <div class="flex justify-between">
              <div class="font-semibold">
                #{{ a.bookingId }} · {{ a.roomTypeName }} ({{ a.typeCode }}) · {{ a.guests }}명
              </div>
              <div class="text-xs" :class="a.assigned ? 'text-green-700' : 'text-red-600'">
                {{ a.assigned ? '배정됨' : '미배정' }}
              </div>
            </div>
            <div class="text-xs text-gray-500">
              {{ a.checkIn }} → {{ a.checkOut }} ({{ a.nights }}박)
            </div>
          </li>
        </ul>
      </div>

      <!-- 우: 가용 객실 -->
      <div class="bg-white border rounded-2xl overflow-hidden">
        <div class="px-4 py-3 bg-gray-100 flex items-center justify-between">
          <div class="font-medium">가용 객실</div>
          <label class="text-xs flex items-center gap-2 px-2">
            <input type="checkbox" v-model="upgrade" @change="loadRooms" />
            업그레이드 허용
          </label>
        </div>
        <div v-if="!selected" class="p-4 text-gray-500">좌측에서 예약을 선택하세요.</div>
        <div v-else-if="!rooms.length" class="p-4 text-gray-500">가용 객실이 없습니다.</div>

        <ul>
          <li v-for="r in rooms" :key="r.roomId" class="p-4 border-t flex items-center justify-between">
            <div>
              <div class="font-semibold">
                {{ r.roomNo }} · {{ r.typeCode }} · {{ r.capacity }}인 · {{ r.housekeeping }}
              </div>
              <div class="text-xs text-gray-500">floor {{ r.floor }}</div>
            </div>
            <button class="px-3 py-1 rounded border hover:bg-gray-100" @click="assign(r.roomId)">
              배정
            </button>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style>
/* 상단 바 */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.title { font-weight: 800; font-size: 18px; }
</style>

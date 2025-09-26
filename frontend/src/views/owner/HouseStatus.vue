<script setup lang="js">
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/lib/api'

const route = useRoute()
const hotelId = computed(() => Number(route.params.hotelId))

const date = ref(new Date().toISOString().slice(0,10))
const rooms = ref([]) // [{...RoomStatusDto}]
const floor = ref('') // 필터

const hkOptions = ['CLEAN','DIRTY','INSPECT']
const stOptions = ['ACTIVE','OUT_OF_SERVICE']

async function load() {
  const url = `/api/owner/hotels/${hotelId.value}/rooms/status?date=${date.value}`
  const res = await api(url)
  rooms.value = res.ok ? await res.json() : []
}

async function setHk(r, hk) {
  const res = await api(`/api/owner/hotels/${hotelId.value}/rooms/${r.id}/housekeeping`, {
    method:'PATCH', body: JSON.stringify({ housekeepingStatus: hk })
  })
  if (res.ok) { r.hkStatus = hk } else alert('변경 실패')
}
async function setStatus(r, st) {
  const res = await api(`/api/owner/hotels/${hotelId.value}/rooms/${r.id}/status`, {
    method:'PATCH', body: JSON.stringify({ status: st })
  })
  if (res.ok) { r.status = st } else alert('변경 실패')
}

const floors = computed(() => {
  const s = new Set(rooms.value.map(r => r.floor))
  return Array.from(s).sort((a,b)=>a-b)
})
const filtered = computed(() => {
  return rooms.value
    .filter(r => !floor.value || String(r.floor)===String(floor.value))
    .sort((a,b)=> a.floor-b.floor || a.roomNumber-b.roomNumber)
})

function badgeClass(r){
  if (r.occupied) return 'bg-red-100 text-red-700 border-red-300'
  if (r.status === 'OUT_OF_SERVICE') return 'bg-gray-100 text-gray-700 border-gray-300'
  if (r.hkStatus === 'DIRTY') return 'bg-yellow-100 text-yellow-700 border-yellow-300'
  return 'bg-green-100 text-green-700 border-green-300'
}

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center gap-3">
      <div class="text-xl font-bold">객실 현황</div>
      <input type="date" v-model="date" class="border rounded-lg px-3 py-2" @change="load" />
      <select v-model="floor" class="border rounded-lg px-3 py-2">
        <option value="">전체 층</option>
        <option v-for="f in floors" :key="f" :value="f">{{ f }}층</option>
      </select>
      <button class="px-3 py-2 rounded-xl bg-black text-white" @click="load">새로고침</button>
      <div class="ml-auto text-xs text-gray-500 flex gap-3">
        <span class="inline-flex items-center gap-1"><span class="w-3 h-3 bg-red-400 inline-block rounded-sm"></span> 점유</span>
        <span class="inline-flex items-center gap-1"><span class="w-3 h-3 bg-yellow-400 inline-block rounded-sm"></span> 청소필요</span>
        <span class="inline-flex items-center gap-1"><span class="w-3 h-3 bg-gray-400 inline-block rounded-sm"></span> 수리중</span>
        <span class="inline-flex items-center gap-1"><span class="w-3 h-3 bg-green-400 inline-block rounded-sm"></span> 가용</span>
      </div>
    </div>

    <!-- 카드 그리드 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
      <div v-for="r in filtered" :key="r.id" class="border rounded-2xl p-4 space-y-3" :class="badgeClass(r)">
        <div class="flex items-baseline justify-between">
          <div class="text-lg font-bold">{{ r.roomNumber }}</div>
          <div class="text-xs">{{ r.floor }}층 · {{ r.roomTypeCode }}</div>
        </div>
        <div class="text-sm">
          정원 {{ r.capacity ?? '-' }}명 · {{ r.roomTypeName }}
        </div>
        <div class="text-xs">
          상태: <b>{{ r.status }}</b> · 하우스키핑: <b>{{ r.hkStatus }}</b>
          <span v-if="r.occupied" class="ml-2 px-2 py-0.5 text-[11px] rounded bg-red-500 text-white">OCCUPIED</span>
        </div>

        <div class="flex gap-2 flex-wrap">
          <select class="border rounded-lg px-2 py-1" :value="r.hkStatus" @change="e=>setHk(r, e.target.value)">
            <option v-for="h in hkOptions" :key="h" :value="h">{{ h }}</option>
          </select>
          <select class="border rounded-lg px-2 py-1" :value="r.status" @change="e=>setStatus(r, e.target.value)">
            <option v-for="s in stOptions" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
      </div>
    </div>
  </div>
</template>

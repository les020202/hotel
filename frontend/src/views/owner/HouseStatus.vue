<script setup lang="js">
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/lib/api'

const route = useRoute()
const hotelId = computed(() => Number(route.params.hotelId))

const date = ref(new Date().toISOString().slice(0,10))
const rooms = ref([])
const floor = ref('')

const HK_FREE = ['CLEAN','DIRTY','INSPECTED']   // 비점유에서 선택 가능
const ST_OPTIONS = ['ACTIVE','INACTIVE']

/** 서버에서 온 원본을 그대로 보관: rawHk만 들고, 표시는 displayHk()로만 */
function normalizeRoom(r) {
  return {
    ...r,
    occupied: !!r.occupied,
    status: r.status || 'ACTIVE',
    rawHk: r.housekeeping ?? null,   // 서버 원본 (null일 수도 있음)
  }
}

async function load() {
  const url = `/api/owner/hotels/${hotelId.value}/rooms/status?date=${date.value}`
  const res = await api(url)
  const data = res.ok ? await res.json() : []
  rooms.value = data.map(normalizeRoom)
}

/** 화면표시용 HK만 계산(데이터 자체는 건드리지 않음) */
function displayHk(r) {
  if (r.occupied) return 'OCCUPIED'
  if (r.rawHk === 'CLEAN' || r.rawHk === 'DIRTY' || r.rawHk === 'INSPECTED') return r.rawHk
  // 서버가 아직 값을 못줬거나 null일 때의 안전망:
  return (r.status === 'INACTIVE') ? 'DIRTY' : 'CLEAN'
}

/** 하우스키핑 변경: CLEAN→ACTIVE, DIRTY/INSPECTED→INACTIVE */
async function setHk(r, hk) {
  if (r.occupied) return

  const res = await api(
    `/api/owner/hotels/${hotelId.value}/rooms/${r.id}/housekeeping?date=${date.value}`,
    { method:'PATCH', body: JSON.stringify({ housekeepingStatus: hk }) }
  )
  if (!res.ok) { alert('변경 실패'); return }

  // 원본에도 즉시 반영 (깜빡임 방지)
  r.rawHk = hk

  const targetStatus = (hk === 'CLEAN') ? 'ACTIVE' : 'INACTIVE'
  if (r.status !== targetStatus) {
    const res2 = await api(
      `/api/owner/hotels/${hotelId.value}/rooms/${r.id}/status?date=${date.value}`,
      { method:'PATCH', body: JSON.stringify({ status: targetStatus }) }
    )
    if (res2.ok) {
      r.status = targetStatus
      // 서버 확정 동기화
      await load()
    } else {
      console.warn('상태 동기화 실패')
    }
  }
}

/** 상태 변경: ACTIVE→CLEAN, INACTIVE→DIRTY + (점유였다면) 즉시 비점유 처리 */
async function setStatus(r, st) {
  const res = await api(
    `/api/owner/hotels/${hotelId.value}/rooms/${r.id}/status?date=${date.value}`,
    { method:'PATCH', body: JSON.stringify({ status: st }) }
  )
  if (!res.ok) { alert('변경 실패'); return }

  // 로컬 즉시 반영
  r.status = st
  // 점유 해제 즉시 반영
  r.occupied = false
  r.guestName = null
  r.guestPhone = null
  // 상태 규칙에 맞춰 HK도 즉시 반영 (서버 응답 오기 전 깜빡임 방지)
  r.rawHk = (st === 'INACTIVE') ? 'DIRTY' : 'CLEAN'

  // 서버와 최종 동기화
  await load()
}

/** 카드 클릭 → HK 순환 / Shift+클릭 → 운영 상태 토글 */
async function onCardClick(r, e) {
  if (e.shiftKey) {
    const next = r.status === 'INACTIVE' ? 'ACTIVE' : 'INACTIVE'
    await setStatus(r, next)
    return
  }
  if (r.occupied) return
  const curr = displayHk(r)
  const idx = HK_FREE.indexOf(curr)
  const next = HK_FREE[(idx + 1) % HK_FREE.length] || 'CLEAN'
  await setHk(r, next)
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

/** 배경색: 점유 빨강 > DIRTY 노랑 > INSPECTED 회색 > 나머지 초록 */
function badgeClass(r){
  const hk = displayHk(r)
  if (r.occupied) return 'bg-red-100 text-red-700 border-red-300'
  if (hk === 'DIRTY') return 'bg-yellow-100 text-yellow-700 border-yellow-300'
  if (hk === 'INSPECTED') return 'bg-gray-200 text-gray-700 border-gray-400'
  return 'bg-green-100 text-green-700 border-green-300'
}

function hkOptionsFor(r){
  return r.occupied ? ['OCCUPIED'] : HK_FREE
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

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-3 2xl:grid-cols-4 gap-3">
      <div
        v-for="r in filtered"
        :key="r.id"
        class="border rounded-2xl p-4 space-y-3 cursor-pointer transition hover:shadow"
        :class="badgeClass(r)"
        @click="(e)=>onCardClick(r, e)"
        :title="'클릭: HK 순환 / Shift+클릭: 운영상태 토글'"
      >
        <div class="flex items-baseline justify-between">
          <div class="text-lg font-bold">{{ r.roomNumber }}</div>
          <div class="text-xs">{{ r.floor }}층 · {{ r.roomTypeCode }}</div>
        </div>
        <div class="text-sm">
          정원 {{ r.capacity ?? '-' }}명 · {{ r.roomTypeName }}
        </div>

        <div v-if="r.occupied && (r.guestName || r.guestPhone)" class="text-xs">
          대표 투숙객: <b>{{ r.guestName || '-' }}</b>
          <span v-if="r.guestPhone"> · {{ r.guestPhone }}</span>
        </div>

        <!-- 표기: 하우스키핑 → 상태 (표시는 displayHk만 사용) -->
        <div class="text-xs">
          하우스키핑: <b>{{ displayHk(r) }}</b>
          · 상태: <b>{{ r.status || 'ACTIVE' }}</b>
        </div>

        <div class="flex gap-2 flex-wrap">
          <select
            class="border rounded-lg px-2 py-1"
            :value="displayHk(r)"
            :disabled="r.occupied"
            @click.stop
            @change="e=>setHk(r, e.target.value)"
          >
            <option v-for="h in hkOptionsFor(r)" :key="h" :value="h">{{ h }}</option>
          </select>

          <select
            class="border rounded-lg px-2 py-1"
            :value="r.status || 'ACTIVE'"
            @click.stop
            @change="e=>setStatus(r, e.target.value)"
          >
            <option v-for="s in ST_OPTIONS" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
      </div>
    </div>
  </div>
</template>

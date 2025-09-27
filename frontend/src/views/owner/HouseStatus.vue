<script setup lang="js">
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/lib/api'

const route = useRoute()
const hotelId = computed(() => Number(route.params.hotelId))

const date = ref(new Date().toISOString().slice(0,10))
const rooms = ref([])
const floor = ref('')

const HK_FREE = ['CLEAN','DIRTY','INSPECTED']  // 비점유 시 노출
const ST_OPTIONS = ['ACTIVE','INACTIVE']

function normalizeRoom(r) {
  const occupied = !!r.occupied
  const hkRaw = r.hkStatus ?? r.housekeeping ?? 'CLEAN'
  return {
    ...r,
    occupied,
    // 점유 중엔 화면표시는 OCCUPIED로 하되, 원래 HK값은 보존
    hkStatus: occupied ? 'OCCUPIED' : hkRaw,
    _hkSaved: hkRaw,   // (선택) 점유 해제 시 복원용
    status:   r.status || 'ACTIVE',
  }
}

async function load() {
  const url = `/api/owner/hotels/${hotelId.value}/rooms/status?date=${date.value}`
  const res = await api(url)
  const data = res.ok ? await res.json() : []
  rooms.value = data.map(normalizeRoom)
}

/** 하우스키핑 변경: HK → 상태 강제 매핑
 *  CLEAN -> ACTIVE
 *  DIRTY -> INACTIVE
 *  INSPECTED -> INACTIVE
 */
async function setHk(r, hk) {
  if (r.occupied) return // 정책: 점유 중 HK 변경 금지

  // 1) HK 저장 (날짜 포함)
  const res = await api(
    `/api/owner/hotels/${hotelId.value}/rooms/${r.id}/housekeeping?date=${date.value}`,
    { method:'PATCH', body: JSON.stringify({ housekeepingStatus: hk }) }
  )
  if (!res.ok) { alert('변경 실패'); return }

  r.hkStatus = hk

  // 2) HK → 운영상태 동기화
  let targetStatus = null
  if (hk === 'CLEAN') targetStatus = 'ACTIVE'
  else if (hk === 'DIRTY' || hk === 'INSPECTED') targetStatus = 'INACTIVE'

  if (targetStatus && r.status !== targetStatus) {
    const res2 = await api(
      `/api/owner/hotels/${hotelId.value}/rooms/${r.id}/status`,
      { method:'PATCH', body: JSON.stringify({ status: targetStatus }) }
    )
    if (res2.ok) r.status = targetStatus
    else console.warn(`상태(${targetStatus}) 동기화 실패`)
  }
}

/** 운영 상태 변경: 최소 동기화만 유지 (ACTIVE->CLEAN, INACTIVE->DIRTY) */
async function setStatus(r, st) {
  const res = await api(
    `/api/owner/hotels/${hotelId.value}/rooms/${r.id}/status`,
    { method:'PATCH', body: JSON.stringify({ status: st }) }
  )
  if (!res.ok) { alert('변경 실패'); return }

  r.status = st

  // 상태 → HK 최소 동기화 (비점유일 때만)
  if (!r.occupied) {
    if (st === 'ACTIVE' && r.hkStatus !== 'CLEAN') {
      const res2 = await api(
        `/api/owner/hotels/${hotelId.value}/rooms/${r.id}/housekeeping?date=${date.value}`,
        { method:'PATCH', body: JSON.stringify({ housekeepingStatus: 'CLEAN' }) }
      )
      if (res2.ok) r.hkStatus = 'CLEAN'
      else console.warn('하우스키핑(CLEAN) 동기화 실패')
    }
    if (st === 'INACTIVE' && r.hkStatus !== 'DIRTY' && r.hkStatus !== 'OCCUPIED') {
      const res2 = await api(
        `/api/owner/hotels/${hotelId.value}/rooms/${r.id}/housekeeping?date=${date.value}`,
        { method:'PATCH', body: JSON.stringify({ housekeepingStatus: 'DIRTY' }) }
      )
      if (res2.ok) r.hkStatus = 'DIRTY'
      else console.warn('하우스키핑(DIRTY) 동기화 실패')
    }
  }
}

/** 카드 클릭 → HK 상태 순환 / Shift+클릭 → 운영 상태 토글 */
async function onCardClick(r, e) {
  if (e.shiftKey) {
    const next = r.status === 'INACTIVE' ? 'ACTIVE' : 'INACTIVE'
    await setStatus(r, next)
    return
  }
  if (r.occupied) return
  const curr = r.hkStatus || 'CLEAN'
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

/** 배경색 규칙:
 *  점유 빨강 > DIRTY 노랑 > INSPECTED 회색 > 나머지(CLEAN 등) 초록
 *  (요청: INACTIVE 회색 처리는 제거)
 */
function badgeClass(r){
  const hk = r.hkStatus || 'CLEAN'
  if (r.occupied) return 'bg-red-100 text-red-700 border-red-300'
  if (hk === 'DIRTY') return 'bg-yellow-100 text-yellow-700 border-yellow-300'
  if (hk === 'INSPECTED') return 'bg-gray-200 text-gray-700 border-gray-400'
  // INACTIVE 회색 처리 제거됨
  return 'bg-green-100 text-green-700 border-green-300'
}

function displayHk(r){
  return r.occupied ? 'OCCUPIED' : (r.hkStatus ?? r.housekeeping ?? 'CLEAN')
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

        <!-- 대표 투숙객 (점유시에만) -->
        <div v-if="r.occupied && (r.guestName || r.guestPhone)" class="text-xs">
          대표 투숙객: <b>{{ r.guestName || '-' }}</b>
          <span v-if="r.guestPhone"> · {{ r.guestPhone }}</span>
        </div>

        <!-- ✅ 표기 순서: 하우스키핑 → 상태 -->
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

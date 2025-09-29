<script setup lang="js">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/lib/api'

const route = useRoute()
const router = useRouter()
const hotelId = ref(null)

const date = ref(new Date().toISOString().slice(0,10))  // 도착일 기준
const arrivals = ref([])          // ArrivalItemDto[]
const selected = ref(null)        // 선택된 arrival (배정 대상)

const rooms = ref([])             // AvailableRoomDto[]
const upgrade = ref(true)

/** 배정 여부로 분리 */
const assignedToday = computed(() => (arrivals.value || []).filter(a => a.assigned))
const pendingArrivals = computed(() => (arrivals.value || []).filter(a => !a.assigned))

/** 배정된 호실 정보를 담는 맵: { [bookingItemId]: { roomId, roomNo } } */
const assignedRoomMap = ref({})

/** 도착 리스트 로드 */
async function loadArrivals () {
  if (!hotelId.value) return
  const q = new URLSearchParams({ date: date.value }).toString()
  const res = await api(`/api/owner/hotels/${hotelId.value}/arrivals?` + q)
  arrivals.value = res.ok ? await res.json() : []
  selected.value = null
  rooms.value = []

  // 하단 섹션용: 배정된 항목들의 룸넘버 일괄 조회 시도 (엔드포인트 없으면 무시)
  await loadAssignedRoomsBatch()
}

/** 배정된 룸넘버를 일괄 조회(가능하면) */
async function loadAssignedRoomsBatch () {
  const itemIds = assignedToday.value.map(a => a.bookingItemId)
  if (!itemIds.length) {
    assignedRoomMap.value = {}
    return
  }
  try {
    // 가정: POST /assignments/rooms/by-items  → { [bookingItemId]: {roomId, roomNo} }
    const res = await api(`/api/owner/hotels/${hotelId.value}/assignments/rooms/by-items`, {
      method: 'POST',
      body: JSON.stringify({ itemIds })
    })
    if (res.ok) {
      const data = await res.json()
      assignedRoomMap.value = data || {}
    } else {
      // 엔드포인트가 없다면(404 등) 조용히 넘어감
      assignedRoomMap.value = {}
    }
  } catch (e) {
    assignedRoomMap.value = {}
  }
}

/** 가용 객실 로드 */
async function loadRooms () {
  if (!hotelId.value || !selected.value) return
  const it = selected.value
  const q = new URLSearchParams({
    from: it.checkIn, to: it.checkOut,
    minCapacity: String(it.guests ?? 1),
    typeCode: it.typeCode,
    upgrade: String(upgrade.value)
  }).toString()
  const res = await api(`/api/owner/hotels/${hotelId.value}/rooms/available?` + q)
  rooms.value = res.ok ? await res.json() : []
}

/** 배정 수행 */
async function assign (roomId) {
  if (!selected.value) return
  const body = { bookingItemId: selected.value.bookingItemId, roomId }
  const res = await api(`/api/owner/hotels/${hotelId.value}/assignments`, {
    method: 'POST',
    body: JSON.stringify(body)
  })
  if (res.ok) {
    const data = await res.json()
    alert(`배정 완료: ${data.assignedNights}박`)
    await loadArrivals() // 리스트 및 하단 섹션 갱신(그리고 배치 룸넘버 조회 재시도)
  } else {
    alert('배정 실패: ' + await res.text())
  }
}

/** HouseStatus(객실 현황)으로 이동 */
/** HouseStatus(객실 현황)으로 이동 */
function goRoomStatus (roomNo = '') {
  const base = `/owner/hotels/${hotelId.value}/rooms`   // ✅ 실제 라우트 경로
  const params = new URLSearchParams({ date: date.value })
  if (roomNo) params.set('roomNo', roomNo)
  router.push(`${base}?${params.toString()}`)
}


/** init */
onMounted(() => {
  hotelId.value = Number(route.params.hotelId)
  loadArrivals()
})
watch(() => route.params.hotelId, v => { hotelId.value = Number(v); loadArrivals() })
watch(date, () => loadArrivals())
</script>

<template>
  <div>
    <h1 class="text-xl font-bold">호실 배정</h1>

    <!-- 조회 필터 -->
    <div class="mt-4 flex gap-3 items-end">
      <div>
        <div class="text-xs text-gray-500">도착일</div>
        <input type="date" class="border rounded-lg p-2" v-model="date" />
      </div>
      <button class="px-4 py-2 rounded-lg bg-black text-white" @click="loadArrivals">조회</button>
    </div>

    <!-- 상단 2칼럼: (좌) 도착 예정(미배정만) / (우) 가용 객실 -->
    <div class="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 좌: 도착 예정 (미배정만) -->
      <div class="bg-white border rounded-2xl overflow-hidden">
        <div class="px-4 py-3 bg-gray-100 font-medium">
          도착 예정
          <span class="text-xs text-gray-500">({{ pendingArrivals.length }}건)</span>
        </div>
        <div v-if="!pendingArrivals.length" class="p-4 text-gray-500">
          해당 일자에 <b>미배정</b> 상태인 도착 예정이 없습니다.
        </div>

        <ul>
          <li v-for="a in pendingArrivals" :key="a.bookingItemId"
              class="p-4 border-t cursor-pointer hover:bg-blue-50/40 transition-colors"
              :class="{'bg-blue-50': selected?.bookingItemId===a.bookingItemId}"
              @click="selected=a; loadRooms()">
            <div class="flex justify-between">
              <div class="font-semibold">
                #{{ a.bookingId }} · {{ a.roomTypeName }} ({{ a.typeCode }}) · {{ a.guests }}명
              </div>
              <div class="text-xs text-red-600">미배정</div>
            </div>
            <div class="text-xs text-gray-500">
              {{ a.checkIn }} → {{ a.checkOut }} ({{ a.nights }}박)
            </div>
            <div v-if="a.leadName || a.leadPhone" class="text-xs text-gray-600 mt-1">
              대표자: <span class="font-medium">{{ a.leadName || '-' }}</span>
              <span v-if="a.leadPhone"> / {{ a.leadPhone }}</span>
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

        <div v-if="!pendingArrivals.length && !selected" class="p-4 text-gray-500">
          오늘은 미배정 예약이 없습니다.
        </div>
        <div v-else-if="!selected" class="p-4 text-gray-500">좌측에서 예약을 선택하세요.</div>
        <div v-else-if="!rooms.length" class="p-4 text-gray-500">가용 객실이 없습니다.</div>

        <ul v-else>
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

    <!-- 하단: 오늘 배정 완료 (아이템 클릭 시 객실현황으로 이동) -->
    <div class="mt-6 bg-white border rounded-2xl overflow-hidden">
      <div class="px-4 py-3 bg-gray-100 font-medium">
        오늘 배정 완료 <span class="text-xs text-gray-500">({{ assignedToday.length }}건)</span>
      </div>

      <div v-if="!assignedToday.length" class="p-4 text-gray-500">
        오늘 날짜({{ date }}) 기준 배정 완료된 예약이 없습니다.
      </div>

      <ul v-else>
        <li
          v-for="a in assignedToday"
          :key="a.bookingItemId"
          class="p-4 border-t cursor-pointer hover:bg-gray-50 transition-colors"
          @click="goRoomStatus(assignedRoomMap[a.bookingItemId]?.roomNo)"
        >
          <div class="flex items-start justify-between">
            <div>
              <div class="font-semibold">
                #{{ a.bookingId }} · {{ a.roomTypeName }} ({{ a.typeCode }}) · {{ a.guests }}명
              </div>
              <div class="text-xs text-gray-500">
                {{ a.checkIn }} → {{ a.checkOut }} ({{ a.nights }}박)
              </div>

              <!-- 대표자 -->
              <div v-if="a.leadName || a.leadPhone" class="text-xs text-gray-600 mt-1">
                대표자: <span class="font-medium">{{ a.leadName || '-' }}</span>
                <span v-if="a.leadPhone"> / {{ a.leadPhone }}</span>
              </div>

              <!-- 배정된 호실 (있으면 표시) -->
              <div v-if="assignedRoomMap[a.bookingItemId]?.roomNo"
                   class="text-xs text-emerald-700 mt-1">
                배정 호실: <span class="font-semibold">
                  {{ assignedRoomMap[a.bookingItemId].roomNo }}
                </span>
              </div>
            </div>

            <span class="text-xs px-2 py-1 rounded-full bg-green-50 text-green-700 border border-green-200">
              배정 완료
            </span>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

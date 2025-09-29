<script setup lang="js">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/lib/api' // 공통 fetch 래퍼

const route = useRoute()
const hotelId = ref(null)

// 필터 상태
const roomTypeId = ref('') // 선택된 룸타입 ID (string)
const roomTypes = ref([])  // [{id,name,typeCode}]
const range = reactive({
  // 기본: 이번달 1일 ~ 이번달 말일
  from: toISO(new Date(new Date().getFullYear(), new Date().getMonth(), 1)),
  to:   toISO(new Date(new Date().getFullYear(), new Date().getMonth()+1, 0)),
})

// 기간형 그리드(풀주 확장) 상태
const grid = reactive({
  startISO: range.from,
  endISO: range.to,
  weeks: [] // [ [ {date,inRange}, ...7 ], ... ]
})

// 일자 데이터
// [{stayDate, allotment, booked, price, status, remainingQty, sellable}]
const days = ref([])

/* =========================
 * 유틸 / 그리드 빌더
 * ========================= */
function toISO(d) {
  const dt = (d instanceof Date) ? d : new Date(d)
  const m = dt.getMonth() + 1
  const day = dt.getDate()
  return `${dt.getFullYear()}-${String(m).padStart(2,'0')}-${String(day).padStart(2,'0')}`
}

// 월요일(ISO)로 보정
function startOfWeek(date) {
  const d = new Date(date)
  const k = (d.getDay() + 6) % 7 // 0=Mon
  d.setDate(d.getDate() - k)
  return d
}
// 일요일(ISO)로 보정
function endOfWeek(date) {
  const d = new Date(date)
  const k = (d.getDay() + 6) % 7
  d.setDate(d.getDate() + (6 - k))
  return d
}

// from~to 범위를 풀주로 확장한 “기간형 그리드” 생성
function buildRangeGrid(fromIso, toIso) {
  const from = new Date(fromIso)
  const to   = new Date(toIso)

  // 입력 보정(끝이 시작보다 빠르면 시작+1일)
  if (to < from) {
    to.setTime(from.getTime())
    to.setDate(to.getDate() + 1)
  }

  const gridStart = startOfWeek(from)
  const gridEnd   = endOfWeek(to)

  const dayCells = []
  for (let d = new Date(gridStart); d <= gridEnd; d.setDate(d.getDate() + 1)) {
    const iso = toISO(d)
    dayCells.push({
      date: iso,
      inRange: (new Date(iso) >= new Date(fromIso)) && (new Date(iso) <= new Date(toIso))
    })
  }

  const weeks = []
  for (let i = 0; i < dayCells.length; i += 7) {
    weeks.push(dayCells.slice(i, i + 7))
  }

  return {
    gridStartISO: toISO(gridStart),
    gridEndISO: toISO(gridEnd),
    weeks
  }
}

function recomputeGrid() {
  const g = buildRangeGrid(range.from, range.to)
  grid.startISO = g.gridStartISO
  grid.endISO   = g.gridEndISO
  grid.weeks    = g.weeks
}

/* =========================
 * 서버 데이터 로드
 * ========================= */
async function loadRoomTypes() {
  if (!hotelId.value) return
  const res = await api(`/api/owner/hotels/${hotelId.value}/room-types`, { method: 'GET' })
  if (res.ok) {
    roomTypes.value = await res.json()
    if (!roomTypeId.value && roomTypes.value.length) {
      roomTypeId.value = String(roomTypes.value[0].id)
    }
  } else {
    roomTypes.value = []
  }
}

async function load() {
  if (!hotelId.value || !roomTypeId.value) return
  // ✅ 조회는 풀주 범위로
  const q = new URLSearchParams({
    roomTypeId: roomTypeId.value,
    from: grid.startISO,
    to: grid.endISO
  }).toString()
  const res = await api(`/api/owner/hotels/${hotelId.value}/inventory?` + q, { method: 'GET' })
  if (res.ok) {
    days.value = await res.json()
  } else {
    days.value = []
  }
}

// 특정 날짜 데이터
function dayInfo(isoDate) {
  return days.value.find(d => d.stayDate === isoDate)
}

/* =========================
 * 일괄 적용
 * ========================= */
const bulk = reactive({
  weekdays: new Set(), // 1..7 (Mon..Sun)
  allotment: '',
  price: '',
  status: 'OPEN'
})
function toggleWeekday(idx) {
  if (bulk.weekdays.has(idx)) bulk.weekdays.delete(idx)
  else bulk.weekdays.add(idx)
}
async function applyBulk() {
  if (!hotelId.value || !roomTypeId.value) return
  const body = {
    roomTypeId: Number(roomTypeId.value),
    // ✅ 일괄적용은 사용자가 선택한 원래 범위를 그대로 사용
    from: range.from,
    to: range.to,
    weekdays: Array.from(bulk.weekdays),
    allotment: bulk.allotment === '' ? null : Number(bulk.allotment),
    price: bulk.price === '' ? null : Number(bulk.price),
    status: bulk.status || null
  }
  const res = await api(`/api/owner/hotels/${hotelId.value}/inventory/bulk`, {
    method: 'PUT',
    body: JSON.stringify(body)
  })
  if (res.ok) {
    await load()
    alert('적용되었습니다.')
  } else {
    const t = await res.text()
    alert('실패: ' + t)
  }
}

/* =========================
 * 초기화 & 감시자
 * ========================= */
onMounted(async () => {
  hotelId.value = Number(route.params.hotelId)
  recomputeGrid()           // 먼저 그리드 계산
  await loadRoomTypes()
  await load()              // 풀주 범위로 조회
})

watch(() => route.params.hotelId, async (v) => {
  hotelId.value = Number(v)
  recomputeGrid()
  await loadRoomTypes()
  await load()
})

watch([roomTypeId, () => range.from, () => range.to], async () => {
  recomputeGrid()
  // 자동 조회 원하면 주석 해제
  // await load()
})
</script>

<template>
  <div>
    <header class="topbar">
      <div class="title"><h1 class="text-xl font-bold">재고 캘린더</h1></div>
    </header>


    <!-- 필터 -->
    <div class="mt-4 grid grid-cols-1 md:grid-cols-4 gap-3">
      <div>
        <div class="text-xs text-gray-500">룸타입</div>
        <select class="w-full border rounded-lg p-2" v-model="roomTypeId">
          <option v-for="rt in roomTypes" :key="rt.id" :value="String(rt.id)">
            {{ rt.name }} ({{ rt.typeCode }})
          </option>
        </select>
      </div>
      <div>
        <div class="text-xs text-gray-500">기간 시작</div>
        <input type="date" class="w-full border rounded-lg p-2" v-model="range.from" />
      </div>
      <div>
        <div class="text-xs text-gray-500">기간 종료</div>
        <input type="date" class="w-full border rounded-lg p-2" v-model="range.to" />
      </div>
      <div class="flex items-end">
        <button class="w-full px-4 py-2 rounded-xl bg-black text-white hover:opacity-90" @click="load">
          조회
        </button>
      </div>
    </div>

    <!-- 실제 표시는 풀주 범위 -->
    <div class="mt-2 text-xs text-gray-500">
      표시 범위: {{ grid.startISO }} ~ {{ grid.endISO }} (선택 범위: {{ range.from }} ~ {{ range.to }})
    </div>

    <!-- 캘린더(기간형 그리드) -->
    <div class="mt-4 bg-white border rounded-2xl overflow-hidden">
      <div class="grid grid-cols-7 text-center text-xs font-medium bg-gray-100 py-2">
        <div>Mon</div><div>Tue</div><div>Wed</div><div>Thu</div><div>Fri</div><div>Sat</div><div>Sun</div>
      </div>

      <div>
        <div v-for="(week, wi) in grid.weeks" :key="wi" class="grid grid-cols-7 border-t">
          <div
            v-for="d in week"
            :key="d.date"
            class="border-r p-2 min-h-[96px]"
            :class="{'bg-gray-50 text-gray-400': !d.inRange}"
          >
            <div class="text-xs" :class="d.inRange ? 'text-gray-500' : 'text-gray-400'">
              {{ d.date.slice(8,10) }}
            </div>

            <div v-if="dayInfo(d.date)" class="mt-1 space-y-1">
              <div class="text-sm font-semibold">
                ₩{{ dayInfo(d.date).price?.toLocaleString?.() ?? dayInfo(d.date).price }}
              </div>
              <div class="text-xs" :class="d.inRange ? 'text-gray-600' : 'text-gray-400'">
                남은 {{ dayInfo(d.date).remainingQty }} / 총 {{ dayInfo(d.date).allotment }}
              </div>
              <div
                class="text-[11px]"
                :class="{
                  'text-green-600': d.inRange && dayInfo(d.date).sellable,
                  'text-red-600':   d.inRange && !dayInfo(d.date).sellable,
                  'text-gray-400': !d.inRange
                }"
              >
                {{ dayInfo(d.date).status }} {{ dayInfo(d.date).sellable ? '(판매중)' : '(판매불가)' }}
              </div>
            </div>

            <div v-else class="mt-1 text-xs italic" :class="d.inRange ? 'text-gray-400' : 'text-gray-300'">
              데이터 없음
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 일괄 적용 -->
    <div class="mt-6 p-4 bg-white border rounded-2xl">
      <div class="text-sm font-semibold mb-3">일괄 적용</div>
      <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
        <div class="col-span-2">
          <div class="text-xs text-gray-500">요일 선택</div>
          <div class="flex flex-wrap gap-2 mt-1">
            <button
              v-for="(lbl,idx) in ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']"
              :key="idx"
              class="px-3 py-1 rounded-full border"
              :class="{'bg-black text-white': bulk.weekdays.has(idx+1)}"
              @click="toggleWeekday(idx+1)"
            >
              {{ lbl }}
            </button>
            <button class="ml-2 px-3 py-1 rounded-full border" @click="bulk.weekdays.clear()">
              전체
            </button>
          </div>
        </div>
        <div>
          <div class="text-xs text-gray-500">Allotment(총배정)</div>
          <input class="w-full border rounded-lg p-2" v-model="bulk.allotment" placeholder="비워두면 변경 없음" />
        </div>
        <div>
          <div class="text-xs text-gray-500">Price(₩)</div>
          <input class="w-full border rounded-lg p-2" v-model="bulk.price" placeholder="비워두면 변경 없음" />
        </div>
        <div>
          <div class="text-xs text-gray-500">Status</div>
          <select class="w-full border rounded-lg p-2" v-model="bulk.status">
            <option value="OPEN">OPEN</option>
            <option value="CLOSED">CLOSED</option>
            <option value="SOLD_OUT">SOLD_OUT</option>
          </select>
        </div>
      </div>
      <div class="mt-4">
        <button class="px-4 py-2 rounded-xl bg-black text-white hover:opacity-90" @click="applyBulk">
          적용
        </button>
      </div>
      <div class="text-xs text-gray-500 mt-2">
        * 비워둔 필드는 변경하지 않습니다. allotment는 booked보다 작게 설정할 수 없습니다.
      </div>
    </div>
  </div>
</template>

<style scoped>
.min-h-\[96px\]{ min-height:96px; }
</style>

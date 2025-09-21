<script setup lang="js">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/lib/api' // ← 공통 래퍼

const route = useRoute()
const hotelId = ref(null)

// 필터 상태
const roomTypeId = ref('') // 선택된 룸타입 ID (string)
const roomTypes = ref([])  // [{id,name,typeCode}]
const range = reactive({
  from: toISO(new Date(new Date().getFullYear(), new Date().getMonth(), 1)),
  to:   toISO(new Date(new Date().getFullYear(), new Date().getMonth()+1, 0)),
})

// 데이터
const days = ref([]) // [{stayDate, allotment, booked, price, status, remainingQty, sellable}]

// 주차 그리드 계산용
const calendarWeeks = computed(() => buildCalendar(range.from))

function toISO(d) {
  const dt = (d instanceof Date) ? d : new Date(d)
  const m = dt.getMonth() + 1, day = dt.getDate()
  return `${dt.getFullYear()}-${String(m).padStart(2,'0')}-${String(day).padStart(2,'0')}`
}

function buildCalendar(isoStart) {
  const first = new Date(isoStart)
  const year = first.getFullYear(), month = first.getMonth()
  const start = new Date(year, month, 1)
  const end = new Date(year, month + 1, 0)

  // 시작주 월요일 맞춤 (ISO, 월=1..일=7; JS는 일=0)
  const startDay = (start.getDay() + 6) % 7 // 0=Mon
  const gridStart = new Date(start); gridStart.setDate(start.getDate() - startDay)

  const weeks = []
  let cur = new Date(gridStart)
  while (cur <= end || weeks.length < 6) {
    const week = []
    for (let i=0;i<7;i++) {
      week.push({
        date: toISO(cur),
        inMonth: cur.getMonth() === month
      })
      cur = new Date(cur.getFullYear(), cur.getMonth(), cur.getDate() + 1)
    }
    weeks.push(week)
    if (cur > end && weeks.length >= 5) break
  }
  return weeks
}

async function loadRoomTypes() {
  if (!hotelId.value) return
  const res = await api(`/api/owner/hotels/${hotelId.value}/room-types`, { method: 'GET' })
  if (res.ok) {
    roomTypes.value = await res.json()
    // 처음 진입 시 기본 선택
    if (!roomTypeId.value && roomTypes.value.length) {
      roomTypeId.value = String(roomTypes.value[0].id)
    }
  } else {
    roomTypes.value = []
  }
}

async function load() {
  if (!hotelId.value || !roomTypeId.value) return
  const q = new URLSearchParams({
    roomTypeId: roomTypeId.value,
    from: range.from,
    to: range.to
  }).toString()
  const res = await api(`/api/owner/hotels/${hotelId.value}/inventory?` + q, { method: 'GET' })
  if (res.ok) {
    days.value = await res.json()
  } else {
    days.value = []
  }
}

// 캘린더 셀에 표시할 데이터 찾기
function dayInfo(isoDate) {
  return days.value.find(d => d.stayDate === isoDate)
}

// 일괄 적용
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

// 초기화
onMounted(async () => {
  hotelId.value = Number(route.params.hotelId)
  await loadRoomTypes()
  await load()
})

// 라우트/필터 변경 시 자동 조회
watch(() => route.params.hotelId, async (v) => {
  hotelId.value = Number(v)
  await loadRoomTypes()
  await load()
})
watch([roomTypeId, () => range.from, () => range.to], () => {
  // 사용자가 값 바꾸면 조회 버튼 없이도 자동 로드하고 싶으면 아래 호출
  // load()
})
</script>

<template>
  <div>
    <h1 class="text-xl font-bold">재고 캘린더</h1>

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

    <!-- 캘린더 -->
    <div class="mt-6 bg-white border rounded-2xl overflow-hidden">
      <div class="grid grid-cols-7 text-center text-xs font-medium bg-gray-100 py-2">
        <div>Mon</div><div>Tue</div><div>Wed</div><div>Thu</div><div>Fri</div><div>Sat</div><div>Sun</div>
      </div>

      <div class="grid grid-rows-6">
        <div v-for="(week, wi) in calendarWeeks" :key="wi" class="grid grid-cols-7 border-t">
          <div v-for="d in week" :key="d.date" class="border-r p-2 min-h-[96px]"
               :class="{'bg-gray-50': !d.inMonth}">
            <div class="text-xs text-gray-500">{{ d.date.slice(8,10) }}</div>
            <div v-if="dayInfo(d.date)" class="mt-1 space-y-1">
              <div class="text-sm font-semibold">
                ₩{{ dayInfo(d.date).price?.toLocaleString?.() ?? dayInfo(d.date).price }}
              </div>
              <div class="text-xs text-gray-600">
                남은 {{ dayInfo(d.date).remainingQty }} / 총 {{ dayInfo(d.date).allotment }}
              </div>
              <div class="text-[11px]" :class="{
                'text-green-600': dayInfo(d.date).sellable,
                'text-red-600': !dayInfo(d.date).sellable
              }">
                {{ dayInfo(d.date).status }} {{ dayInfo(d.date).sellable ? '(판매중)' : '(판매불가)' }}
              </div>
            </div>
            <div v-else class="mt-1 text-xs text-gray-400 italic">데이터 없음</div>
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
            <button v-for="(lbl,idx) in ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']"
                    :key="idx"
                    class="px-3 py-1 rounded-full border"
                    :class="{'bg-black text-white': bulk.weekdays.has(idx+1)}"
                    @click="toggleWeekday(idx+1)">
              {{ lbl }}
            </button>
            <button class="ml-2 px-3 py-1 rounded-full border"
                    @click="bulk.weekdays.clear()">
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

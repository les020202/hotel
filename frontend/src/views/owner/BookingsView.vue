<!-- src/views/owner/HotelBookings.vue (예시 파일명) -->
<template>
  <div class="p-4 space-y-4">
      <header class="topbar">
      <div class="title"><h1 class="text-xl font-bold">내 호텔 예약 내역</h1></div>
    </header>

    <!-- [ADD] 검색/필터 바 -->
    <div class="flex flex-col gap-2 md:flex-row md:items-end md:gap-3">
      <div class="flex-1">
        <label class="block text-xs text-gray-500 mb-1">검색어 (예약번호/고객/아이디/객실)</label>
        <input
          v-model.trim="q"
          type="text"
          class="w-full border rounded-lg p-2"
          placeholder="예: 1024 / 홍길동 / standard"
        />
      </div>

      <div>
        <label class="block text-xs text-gray-500 mb-1">상태</label>
        <select v-model="status" class="w-40 border rounded-lg p-2">
          <option value="">전체</option>
          <option value="CONFIRMED">확정</option>
          <option value="PENDING">대기</option>
          <option value="CANCELLED">취소</option>
        </select>
      </div>

      <div>
        <label class="block text-xs text-gray-500 mb-1">체크인(시작)</label>
        <input v-model="from" type="date" class="border rounded-lg p-2 w-40" />
      </div>

      <div>
        <label class="block text-xs text-gray-500 mb-1">체크인(종료)</label>
        <input v-model="to" type="date" class="border rounded-lg p-2 w-40" />
      </div>

      <div class="flex gap-2">
        <button class="px-3 py-2 rounded-lg border" @click="applyNow++">검색</button>
        <button class="px-3 py-2 rounded-lg border" @click="resetFilters">초기화</button>
      </div>
    </div>
    <!-- [/ADD] -->

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
            <th class="p-2 text-center w-44">관리</th>
          </tr>
        </thead>
        <tbody>
          <!-- [MOD] bookings → filteredBookings 로 변경 -->
          <tr v-for="r in filteredBookings" :key="r.bookingId" class="border-b">
            <td class="p-2">#{{ r.bookingId }}</td>
            <td class="p-2">
              <div class="font-medium">{{ r.userName || '-' }}</div>
              <div class="text-xs text-gray-500">{{ r.userLoginId }}</div>
            </td>
            <td class="p-2">{{ r.roomTypeName }}</td>
            <td class="p-2">{{ fmtDate(r.checkIn) }}</td>
            <td class="p-2">{{ r.nights ?? '-' }}</td>
            <td class="p-2 text-center">
              <span :class="badgeClass(r.status)">{{ toKStatus(r.status) }}</span>
            </td>
            <td class="p-2 text-center">
              <!-- 취소 가능 -->
              <button
                v-if="r.status !== 'CANCELLED' && canCancelOwner(r)"
                class="px-3 py-1.5 rounded bg-rose-600 text-white font-semibold"
                @click="openCancel(r)"
              >
                예약 취소
              </button>

              <!-- 취소 불가 -->
              <div
                v-else-if="r.status !== 'CANCELLED'"
                class="flex flex-col items-center"
              >
                <button class="px-3 py-1.5 rounded border font-semibold" disabled style="opacity:.5; cursor:not-allowed;">
                  취소 불가
                </button>
                <small class="mt-1 text-xs text-gray-500">
                  체크인 당일 23:59 까지 취소 가능
                </small>
              </div>

              <!-- 이미 취소 -->
              <span v-else class="text-gray-400">-</span>
            </td>
          </tr>

          <tr v-if="!loading && !filteredBookings.length">
            <td colspan="7" class="p-4 text-center text-gray-500">예약이 없습니다.</td>
          </tr>
          <tr v-if="loading">
            <td colspan="7" class="p-4 text-center text-gray-400">불러오는 중…</td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-if="errorMsg" class="text-red-600 text-sm">{{ errorMsg }}</p>

    <!-- 취소 다이얼로그 -->
    <CancelDialog
      :open="cancelOpen"
      @close="cancelOpen = false"
      @submit="doCancel"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '@/api/_http'
import { cancelBooking } from '@/api/bookings'
import CancelDialog from '@/components/common/CancelDialog.vue'

const route = useRoute()
const hotelId = route.params.hotelId

const bookings = ref([])
const loading = ref(false)
const errorMsg = ref('')

/* 취소 모달 상태 */
const cancelOpen = ref(false)
const targetBookingId = ref(null)

function openCancel(row) {
  targetBookingId.value = row?.bookingId ?? row?.id
  cancelOpen.value = true
}

async function doCancel({ reason }) {
  try {
    if (!targetBookingId.value) return
    await cancelBooking(targetBookingId.value, reason || '')
    await load()
    alert('예약이 취소되었습니다.')
  } catch (e) {
    console.error(e)
    let msg = '취소에 실패했습니다.'
    try { msg = JSON.parse(e.message)?.error || msg } catch {}
    alert(msg)
  } finally {
    cancelOpen.value = false
    targetBookingId.value = null
  }
}

/* 표시 유틸 */
function fmtDate(d) { return d ?? '-' }
function toKStatus(s) {
  if (!s) return '-'
  const x = s.toUpperCase()
  if (x === 'CONFIRMED') return '확정'
  if (x === 'CANCELLED' || x === 'CANCELED') return '취소'
  if (x === 'PENDING') return '대기'
  return x
}
function badgeClass(s) {
  const x = (s || '').toUpperCase()
  return [
    'inline-block px-2 py-0.5 rounded text-xs font-semibold',
    x === 'CONFIRMED' ? 'bg-green-100 text-green-700' :
    (x === 'CANCELLED' || x === 'CANCELED') ? 'bg-red-100 text-red-700' :
    x === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
    'bg-gray-100 text-gray-700'
  ].join(' ')
}

/* 오너 취소 가능 규칙: 체크인 당일 23:59까지 가능 → 오늘 <= 체크인 날짜 */
function canCancelOwner(row) {
  if (!row) return false
  const st = (row.status || '').toUpperCase()
  if (st === 'CANCELLED' || st === 'CANCELED') return false
  const cin = normalizeDate(row.checkIn)
  if (!cin) return false
  return todayStr() <= cin
}
function todayStr () {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}
function normalizeDate (v) {
  if (!v) return ''
  if (typeof v === 'string') return v.slice(0, 10)
  if (v instanceof Date) {
    const y = v.getFullYear()
    const m = String(v.getMonth() + 1).padStart(2, '0')
    const d = String(v.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  return String(v).slice(0, 10)
}

/* 데이터 로드 */
async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    // 백엔드: GET /api/owner/hotels/{hotelId}/bookings
    const res = await get(`/owner/hotels/${hotelId}/bookings`)
    bookings.value = Array.isArray(res) ? res : (res?.content ?? [])
  } catch (e) {
    console.error(e)
    errorMsg.value = e?.response?.data?.message || e.message || '불러오기 실패'
    bookings.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

/* ---------------------------- */
/* [ADD] 검색/필터 로직 (프론트) */
/* ---------------------------- */
const q = ref('')          // 키워드
const status = ref('')     // CONFIRMED | PENDING | CANCELLED | ''
const from = ref('')       // YYYY-MM-DD
const to = ref('')         // YYYY-MM-DD

// "검색" 버튼 누르면 즉시 재평가되도록 트리거
const applyNow = ref(0)

function resetFilters() {
  q.value = ''
  status.value = ''
  from.value = ''
  to.value = ''
  applyNow.value++ // 즉시 반영
}

// 소문자 비교용
const lc = (s) => (s ?? '').toString().toLowerCase()

const filteredBookings = computed(() => {
  // applyNow를 의존성에 추가해서 검색 버튼 클릭 시 즉시 재계산
  void applyNow.value

  const kw = lc(q.value).trim()
  const st = (status.value || '').toUpperCase()
  const fromD = from.value || ''
  const toD = to.value || ''

  return (bookings.value || []).filter(r => {
    // 상태 필터
    if (st && (r.status || '').toUpperCase() !== st) return false

    // 기간 필터 (체크인 기준)
    const cin = normalizeDate(r.checkIn)
    if (fromD && (!cin || cin < fromD)) return false
    if (toD && (!cin || cin > toD)) return false

    // 키워드 필터
    if (!kw) return true
    const hay = [
      String(r.bookingId || ''),
      r.userName || '',
      r.userLoginId || '',
      r.roomTypeName || ''
    ].map(lc).join(' ')
    return hay.includes(kw)
  })
})
</script>

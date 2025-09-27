<!-- src/views/owner/HotelBookings.vue (예시 파일명) -->
<template>
  <div class="p-4 space-y-4">
    <h2 class="text-xl font-bold">내 호텔 예약 내역</h2>

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
          <tr v-for="r in bookings" :key="r.bookingId" class="border-b">
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

          <tr v-if="!loading && !bookings.length">
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
import { ref, onMounted } from 'vue'
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
</script>

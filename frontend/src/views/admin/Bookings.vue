<!-- src/views/admin/Bookings.vue -->
<template>
  <div class="p-4 space-y-4">
    <h2 class="text-xl font-bold">예약 관리</h2>

    <!-- 필터 -->
    <form class="grid grid-cols-1 md:grid-cols-6 gap-2 items-end" @submit.prevent="reload(0)">
      <div>
        <label class="block text-xs text-gray-600 mb-1">상태</label>
        <select v-model="q.status" class="w-full border rounded px-2 py-1">
          <option :value="null">전체</option>
          <!-- ✅ 백엔드 상태 철자 통일: CANCELLED -->
          <option value="CONFIRMED">CONFIRMED</option>
          <option value="CANCELLED">CANCELLED</option>
          <option value="PENDING">PENDING</option>
        </select>
      </div>

      <!-- 호텔 이름 필터 -->
      <div class="md:col-span-2">
        <label class="block text-xs text-gray-600 mb-1">호텔 이름</label>
        <input v-model.trim="q.hotelName" class="w-full border rounded px-2 py-1" placeholder="예: 콘래드, 신라, 제주…" />
      </div>

      <div>
        <label class="block text-xs text-gray-600 mb-1">로그인ID</label>
        <input v-model.trim="q.loginId" class="w-full border rounded px-2 py-1" placeholder="user 검색" />
      </div>
      <div>
        <label class="block text-xs text-gray-600 mb-1">체크인 From</label>
        <input v-model="q.from" type="date" class="w-full border rounded px-2 py-1" />
      </div>
      <div>
        <label class="block text-xs text-gray-600 mb-1">체크인 To</label>
        <input v-model="q.to" type="date" class="w-full border rounded px-2 py-1" />
      </div>
      <div class="flex gap-2">
        <button type="submit" class="border rounded px-3 py-2 bg-black text-white">검색</button>
        <button type="button" @click="resetFilters" class="border rounded px-3 py-2">초기화</button>
      </div>
    </form>

    <!-- 표 -->
    <div class="border rounded overflow-x-auto">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="p-2 text-left">예약번호</th>
            <th class="p-2 text-left">고객</th>
            <th class="p-2 text-left">호텔</th>
            <th class="p-2 text-left">객실</th>
            <th class="p-2 text-left">체크인</th>
            <th class="p-2 text-left">박수</th>
            <th class="p-2 text-right">금액</th>
            <th class="p-2 text-center">상태</th>
            <th class="p-2 text-center">영수증</th>
            <!-- ✅ 관리(취소) 컬럼 추가 -->
            <th class="p-2 text-center">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in bookings" :key="b.bookingId" class="border-b">
            <td class="p-2">#{{ b.bookingId }}</td>
            <td class="p-2">
              <div class="font-medium">{{ b.userName || '-' }}</div>
              <div class="text-xs text-gray-500">{{ b.userLoginId }}</div>
            </td>
            <td class="p-2">{{ b.hotelName }}</td>
            <td class="p-2">{{ b.roomTypeName }}</td>
            <td class="p-2">{{ b.checkIn || '-' }}</td>
            <td class="p-2">{{ b.nights ?? '-' }}</td>
            <td class="p-2 text-right">
              {{ nfmt(b.totalAmount) }} {{ b.currency }}
            </td>
            <td class="p-2 text-center">
              <span :class="badgeClass(b.status)">{{ b.status }}</span>
            </td>
            <td class="p-2 text-center">
              <a v-if="b.receiptUrl" :href="b.receiptUrl" target="_blank" rel="noopener" class="text-blue-600 underline">보기</a>
              <span v-else class="text-gray-400">-</span>
            </td>
            <!-- ✅ 취소 버튼 -->
            <td class="p-2 text-center">
              <button
                class="px-2 py-1 border rounded text-red-600 border-red-300 hover:bg-red-50 disabled:opacity-50"
                :disabled="!canCancel(b) || loading"
                @click="openCancel(b.bookingId)"
              >
                취소
              </button>
            </td>
          </tr>
          <tr v-if="!loading && !bookings.length">
            <td colspan="10" class="p-4 text-center text-gray-500">데이터가 없습니다.</td>
          </tr>
          <tr v-if="loading">
            <td colspan="10" class="p-4 text-center text-gray-400">불러오는 중…</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="flex items-center justify-between">
      <div class="text-sm text-gray-600">
        총 {{ totalElements.toLocaleString() }}건
      </div>
      <div class="flex items-center gap-2">
        <button class="px-3 py-1 border rounded"
                :disabled="page<=0 || loading"
                @click="reload(page-1)">
          이전
        </button>
        <span class="text-sm">페이지 {{ page+1 }} / {{ totalPages }}</span>
        <button class="px-3 py-1 border rounded"
                :disabled="page>=totalPages-1 || loading"
                @click="reload(page+1)">
          다음
        </button>
        <select v-model.number="size" @change="reload(0)" class="border rounded px-2 py-1">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </div>
    </div>

    <!-- 에러 -->
    <p v-if="errorMsg" class="text-red-600 text-sm">{{ errorMsg }}</p>

    <!-- ✅ 취소 모달 -->
    <CancelDialog
      :open="cancelOpen"
      @close="cancelOpen=false"
      @submit="submitCancel"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/api/_http'
import { cancelBooking } from '@/api/bookings'            // ★ 공용 취소 API 재사용
import CancelDialog from '@/components/common/CancelDialog.vue' // ★ 모달

const bookings = ref([])
const loading = ref(false)
const errorMsg = ref('')

const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)

const q = ref({
  status: null,
  hotelName: '',
  loginId: '',
  from: '',
  to: ''
})

const nfmt = (n) => (n == null ? '-' : Number(n).toLocaleString('ko-KR'))

// ✅ 상태 배지: CANCELLED 반영
const badgeClass = (s) => [
  'inline-block px-2 py-0.5 rounded text-xs font-semibold',
  s === 'CONFIRMED' ? 'bg-green-100 text-green-700' :
  s === 'CANCELLED' ? 'bg-red-100 text-red-700' :
  s === 'PENDING'   ? 'bg-yellow-100 text-yellow-700' :
                      'bg-gray-100 text-gray-700'
].join(' ')

function buildParams(nextPage) {
  const params = new URLSearchParams()
  params.set('page', String(nextPage ?? page.value))
  params.set('size', String(size.value))
  if (q.value.status)     params.set('status', q.value.status)
  if (q.value.hotelName)  params.set('hotelName', q.value.hotelName)
  if (q.value.loginId)    params.set('loginId', q.value.loginId)
  if (q.value.from)       params.set('from', q.value.from)
  if (q.value.to)         params.set('to', q.value.to)
  return params.toString()
}

async function reload(nextPage = page.value) {
  loading.value = true
  errorMsg.value = ''
  try {
    const query = buildParams(nextPage)
    // 백엔드 라우팅: /api/admin/bookings
    const res = await get(`/admin/bookings?${query}`)
    bookings.value = Array.isArray(res?.content) ? res.content : []
    page.value = Number(res?.number ?? nextPage)
    size.value = Number(res?.size ?? size.value)
    totalPages.value = Number(res?.totalPages ?? 0)
    totalElements.value = Number(res?.totalElements ?? 0)
  } catch (e) {
    console.error(e)
    // fetch 래퍼의 에러 포맷(JSON string) 방어 처리
    let msg = '불러오기 실패'
    try {
      const j = JSON.parse(e?.message || '{}')
      if (j?.error) msg = j.error
    } catch {}
    errorMsg.value = msg
    bookings.value = []
    totalPages.value = 0
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  q.value = { status: null, hotelName: '', loginId: '', from: '', to: '' }
  reload(0)
}

/* =========================
   취소 버튼/모달
   - 어드민 컷오프: 체크인 당일 23:59까지 허용 (백엔드에서도 최종 검증)
   ========================= */
const cancelOpen = ref(false)
const cancelTargetId = ref(null)

function openCancel(id) {
  cancelTargetId.value = id
  cancelOpen.value = true
}

async function submitCancel({ reason }) {
  if (!cancelTargetId.value) return
  try {
    await cancelBooking(cancelTargetId.value, reason || '')
    await reload(page.value)
    alert('예약이 취소되었습니다.')
  } catch (e) {
    console.error(e)
    let msg = '취소에 실패했습니다.'
    try {
      const j = JSON.parse(e?.message || '{}')
      if (j?.error) msg = j.error
    } catch {}
    alert(msg)
  } finally {
    cancelOpen.value = false
    cancelTargetId.value = null
  }
}

/* UX용 프론트 가드 (백엔드가 최종 판단) */
function canCancel(b) {
  if (!b || b.status === 'CANCELLED') return false
  if (!b.checkIn) return true
  // b.checkIn은 'YYYY-MM-DD' 가정
  const today = new Date()
  const t0 = new Date(today.getFullYear(), today.getMonth(), today.getDate())     // 오늘 0시
  const ci  = new Date(b.checkIn)                                                 // 체크인 0시
  return t0 <= ci // 어드민: 체크인 당일까지 가능
}

onMounted(() => reload(0))
</script>

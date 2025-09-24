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
          <option value="CONFIRMED">CONFIRMED</option>
          <option value="CANCELED">CANCELED</option>
          <option value="PENDING">PENDING</option>
        </select>
      </div>

      <!-- ✅ 호텔 이름 필터 -->
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
    <div class="border rounded">
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
          </tr>
          <tr v-if="!loading && !bookings.length">
            <td colspan="9" class="p-4 text-center text-gray-500">데이터가 없습니다.</td>
          </tr>
          <tr v-if="loading">
            <td colspan="9" class="p-4 text-center text-gray-400">불러오는 중…</td>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/api/_http'

const bookings = ref([])
const loading = ref(false)
const errorMsg = ref('')

const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)

// ✅ hotelId 제거, hotelName 추가
const q = ref({
  status: null,
  hotelName: '',   // ← 여기!
  loginId: '',
  from: '',
  to: ''
})

const nfmt = (n) => (n == null ? '-' : Number(n).toLocaleString('ko-KR'))
const badgeClass = (s) => [
  'inline-block px-2 py-0.5 rounded text-xs font-semibold',
  s === 'CONFIRMED' ? 'bg-green-100 text-green-700' :
  s === 'CANCELED'  ? 'bg-red-100 text-red-700' :
  s === 'PENDING'   ? 'bg-yellow-100 text-yellow-700' :
                      'bg-gray-100 text-gray-700'
].join(' ')

function buildParams(nextPage) {
  const params = new URLSearchParams()
  params.set('page', String(nextPage ?? page.value))
  params.set('size', String(size.value))
  if (q.value.status)     params.set('status', q.value.status)
  if (q.value.hotelName)  params.set('hotelName', q.value.hotelName) // ★ 변경
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
    const res = await get(`/admin/bookings?${query}`)
    bookings.value = Array.isArray(res?.content) ? res.content : []
    page.value = Number(res?.number ?? nextPage)
    size.value = Number(res?.size ?? size.value)
    totalPages.value = Number(res?.totalPages ?? 0)
    totalElements.value = Number(res?.totalElements ?? 0)
  } catch (e) {
    console.error(e)
    errorMsg.value = e?.response?.data?.message || e.message || '불러오기 실패'
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

onMounted(() => reload(0))
</script>

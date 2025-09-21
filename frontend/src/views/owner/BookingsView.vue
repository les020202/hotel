<script setup lang="js">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const hotelId = ref(null)

// 필터 상태
const filt = reactive({
  from: toISO(new Date()),
  to:   toISO(new Date(Date.now() + 6*24*3600*1000)), // +6일 = 1주
  status: '',
  q: '',
  page: 0,
  size: 20
})

const rows = ref([]) // [{id, checkIn, checkOut, nights, guests, totalAmount, status, voucherNo}]
const total = ref(0)

function toISO(d) {
  const dt = (d instanceof Date) ? d : new Date(d)
  const m = dt.getMonth() + 1, day = dt.getDate()
  return `${dt.getFullYear()}-${String(m).padStart(2,'0')}-${String(day).padStart(2,'0')}`
}

async function load() {
  if (!hotelId.value) return
  const url = new URL(window.location.origin + `/api/owner/hotels/${hotelId.value}/bookings`)
  url.searchParams.set('from', filt.from)
  url.searchParams.set('to', filt.to)
  if (filt.status) url.searchParams.set('status', filt.status)
  if (filt.q) url.searchParams.set('q', filt.q)
  url.searchParams.set('page', filt.page)
  url.searchParams.set('size', filt.size)

  const res = await fetch(url, { method: 'GET' })
  if (res.ok) {
    const data = await res.json()
    // 페이지 응답 형태 예시 반영
    rows.value = data.content ?? data
    total.value = data.totalElements ?? rows.value.length
  } else {
    rows.value = []
    total.value = 0
  }
}

function prevPage() {
  if (filt.page > 0) { filt.page--; load() }
}
function nextPage() {
  if ((filt.page+1) * filt.size < total.value) { filt.page++; load() }
}

onMounted(() => {
  hotelId.value = Number(route.params.hotelId)
  load()
})
watch(() => route.params.hotelId, (v) => { hotelId.value = Number(v); load() })
</script>

<template>
  <div>
    <h1 class="text-xl font-bold">예약 목록</h1>

    <!-- 필터 -->
    <div class="mt-4 grid grid-cols-1 md:grid-cols-6 gap-3">
      <div>
        <div class="text-xs text-gray-500">기간 시작</div>
        <input type="date" class="w-full border rounded-lg p-2" v-model="filt.from" />
      </div>
      <div>
        <div class="text-xs text-gray-500">기간 종료</div>
        <input type="date" class="w-full border rounded-lg p-2" v-model="filt.to" />
      </div>
      <div>
        <div class="text-xs text-gray-500">상태</div>
        <select class="w-full border rounded-lg p-2" v-model="filt.status">
          <option value="">전체</option>
          <option value="PENDING">PENDING</option>
          <option value="CONFIRMED">CONFIRMED</option>
          <option value="CANCELLED">CANCELLED</option>
          <option value="NO_SHOW">NO_SHOW</option>
        </select>
      </div>
      <div class="md:col-span-2">
        <div class="text-xs text-gray-500">검색(이름/바우처)</div>
        <input class="w-full border rounded-lg p-2" v-model="filt.q" placeholder="예: 홍길동, VCH-001" />
      </div>
      <div class="flex items-end">
        <button class="w-full px-4 py-2 rounded-xl bg-black text-white hover:opacity-90" @click="load">
          조회
        </button>
      </div>
    </div>

    <!-- 테이블 -->
    <div class="mt-6 bg-white border rounded-2xl overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-gray-100">
          <tr>
            <th class="px-3 py-2 text-left">예약ID</th>
            <th class="px-3 py-2 text-left">체크인</th>
            <th class="px-3 py-2 text-left">체크아웃</th>
            <th class="px-3 py-2 text-left">박수</th>
            <th class="px-3 py-2 text-left">인원</th>
            <th class="px-3 py-2 text-left">총액</th>
            <th class="px-3 py-2 text-left">상태</th>
            <th class="px-3 py-2 text-left">바우처</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in rows" :key="r.id" class="border-t">
            <td class="px-3 py-2">{{ r.id }}</td>
            <td class="px-3 py-2">{{ r.checkIn }}</td>
            <td class="px-3 py-2">{{ r.checkOut }}</td>
            <td class="px-3 py-2">{{ r.nights }}</td>
            <td class="px-3 py-2">{{ r.guests }}</td>
            <td class="px-3 py-2">₩{{ (r.totalAmount ?? 0).toLocaleString() }}</td>
            <td class="px-3 py-2">
              <span class="px-2 py-1 rounded-full text-xs border"
                    :class="{
                      'bg-green-50 border-green-300 text-green-700': r.status==='CONFIRMED',
                      'bg-yellow-50 border-yellow-300 text-yellow-700': r.status==='PENDING',
                      'bg-red-50 border-red-300 text-red-700': r.status==='CANCELLED' || r.status==='NO_SHOW'
                    }">
                {{ r.status }}
              </span>
            </td>
            <td class="px-3 py-2">
              <button class="text-blue-600 underline" @click="navigator.clipboard.writeText(r.voucherNo || '')">
                {{ r.voucherNo || '-' }}
              </button>
            </td>
          </tr>
          <tr v-if="!rows.length">
            <td colspan="8" class="px-3 py-8 text-center text-gray-500">데이터가 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="mt-4 flex items-center justify-between">
      <div class="text-xs text-gray-500">총 {{ total }}건</div>
      <div class="flex gap-2">
        <button class="px-3 py-1 rounded border" @click="prevPage">이전</button>
        <button class="px-3 py-1 rounded border" @click="nextPage">다음</button>
      </div>
    </div>
  </div>
</template>

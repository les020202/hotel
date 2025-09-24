<!-- src/views/mypage/MyBookings.vue -->
<template>
  <div class="container">
    <h2>예약 내역</h2>

    <div v-if="loading" class="muted">불러오는 중…</div>

    <template v-else>
      <div v-if="rows.length === 0" class="empty">
        예약 내역이 없습니다.
      </div>

      <div v-else class="list">
        <article v-for="r in rows" :key="r.bookingId" class="card" @click="goDetail(r.bookingId)">
          <header class="card-hd">
            <div class="id">#{{ r.bookingId }}</div>
            <span class="status" :data-st="r.status">{{ toKStatus(r.status) }}</span>
          </header>

          <div class="grid">
            <div><b>호텔</b></div><div class="tr">{{ r.hotelName }}</div>
            <div><b>객실타입</b></div><div class="tr">{{ r.roomTypeName }}</div>

            <div><b>체크인</b></div><div class="tr">{{ fmtDate(r.checkIn) }}</div>
            <div><b>체크아웃</b></div>
            <div class="tr">
              {{ fmtDate(r.checkOut) }}
              <span v-if="r.nights"> ({{ r.nights }}박)</span>
            </div>

            <div><b>인원</b></div><div class="tr">{{ r.guests }}명</div>
            <div><b>총 결제금액</b></div>
            <div class="tr">{{ fmtAmount(r.totalAmount, r.currency) }}</div>

            <template v-if="r.receiptUrl">
              <div><b>영수증</b></div>
              <div class="tr">
                <a :href="r.receiptUrl" target="_blank" rel="noopener">영수증 보기</a>
              </div>
            </template>
          </div>
        </article>

        <!-- 페이지네이션 -->
        <nav class="pager">
          <button class="btn" :disabled="page<=0" @click="move(page-1)">이전</button>
          <span class="muted">Page {{ page+1 }} / {{ totalPages }}</span>
          <button class="btn" :disabled="page>=totalPages-1" @click="move(page+1)">다음</button>
        </nav>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchMyBookings } from '@/api/myBookings'

const router = useRouter()
const route  = useRoute()

const rows = ref([])
const loading = ref(false)
const page = ref(Number(route.query.page ?? 0))
const size = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)

function fmtAmount(v, c) {
  if (v == null) return ''
  return `${Number(v).toLocaleString('ko-KR')}${c === 'KRW' ? '원' : (c ? ` ${c}` : '')}`
}
function fmtDate(d) {
  // d가 '2025-09-24' 같은 ISO-LOCAL 날짜라고 가정
  return d ?? ''
}
function toKStatus(st) {
  // 필요 시 매핑
  switch (st) {
    case 'CONFIRMED': return '확정'
    case 'CANCELLED': return '취소'
    default: return st
  }
}

async function load() {
  loading.value = true
  try {
    const data = await fetchMyBookings(page.value, size.value)
    rows.value = data.content ?? []
    totalPages.value = data.totalPages ?? 1
    totalElements.value = data.totalElements ?? rows.value.length
  } finally {
    loading.value = false
  }
}

function move(p) {
  router.push({ name: 'MyBookings', query: { page: p } })
}

function goDetail(id) {
  router.push({ name: 'MyBookingDetail', params: { id } })
}

onMounted(load)

// 쿼리 페이지 변화 시 갱신
watch(() => route.query.page, (nv) => {
  page.value = Number(nv ?? 0)
  load()
})
</script>

<style scoped>
.container{max-width:900px;margin:32px auto;padding:0 12px}
.muted{color:#64748b}
.empty{color:#64748b;border:1px dashed #e5e7eb;border-radius:12px;padding:24px;text-align:center}
.list{display:grid;gap:12px}
.card{border:1px solid #e5e7eb;border-radius:12px;padding:16px;background:#fff;cursor:pointer}
.card:hover{box-shadow:0 4px 14px rgba(0,0,0,.06)}
.card-hd{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px}
.id{font-weight:700}
.status{font-size:12px;padding:2px 8px;border-radius:999px;background:#eef2ff}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:8px}
.tr{text-align:right}
.pager{display:flex;gap:12px;align-items:center;justify-content:center;margin:12px 0}
.btn{padding:8px 12px;border-radius:8px;background:#1b64da;color:#fff;border:none;cursor:pointer}
.btn:disabled{opacity:.5;cursor:not-allowed}
</style>

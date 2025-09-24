<!-- src/views/mypage/MyBookingDetail.vue -->
<template>
  <div class="container">
    <button class="btn" @click="router.back()">← 목록으로</button>

    <div v-if="loading" class="muted" style="margin-top:12px">불러오는 중…</div>
    <div v-else-if="!row" class="empty">해당 예약을 찾을 수 없습니다.</div>

    <article v-else class="card">
      <header class="card-hd">
        <h2>#{{ row.bookingId }}</h2>
        <span class="status" :data-st="row.status">{{ toKStatus(row.status) }}</span>
      </header>

      <section class="grid">
        <div><b>호텔</b></div><div class="tr">{{ row.hotelName }}</div>
        <div><b>객실타입</b></div><div class="tr">{{ row.roomTypeName }}</div>

        <div><b>체크인</b></div><div class="tr">{{ fmtDate(row.checkIn) }}</div>
        <div><b>체크아웃</b></div>
        <div class="tr">{{ fmtDate(row.checkOut) }} <span v-if="row.nights"> ({{ row.nights }}박)</span></div>

        <div><b>인원</b></div><div class="tr">{{ row.guests }}명</div>
        <div><b>총 결제금액</b></div><div class="tr">{{ fmtAmount(row.totalAmount, row.currency) }}</div>

        <template v-if="row.receiptUrl">
          <div><b>영수증</b></div>
          <div class="tr">
            <a :href="row.receiptUrl" target="_blank" rel="noopener">영수증 보기</a>
          </div>
        </template>
      </section>

      <!-- (선택) 추가 기능: 예약취소 버튼 등 -->
      <!-- <div class="btns">
        <button class="btn danger" @click="cancelBooking()">예약 취소</button>
      </div> -->
    </article>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchMyBooking } from '@/api/myBookings'

const route = useRoute()
const router = useRouter()

const row = ref(null)
const loading = ref(false)
const id = Number(route.params.id)

function fmtAmount(v, c) {
  if (v == null) return ''
  return `${Number(v).toLocaleString('ko-KR')}${c === 'KRW' ? '원' : (c ? ` ${c}` : '')}`
}
function fmtDate(d) { return d ?? '' }
function toKStatus(st) {
  switch (st) {
    case 'CONFIRMED': return '확정'
    case 'CANCELLED': return '취소'
    default: return st
  }
}

async function load() {
  loading.value = true
  try {
    row.value = await fetchMyBooking(id)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.container{max-width:900px;margin:32px auto;padding:0 12px}
.muted{color:#64748b}
.empty{color:#64748b;border:1px dashed #e5e7eb;border-radius:12px;padding:24px;text-align:center;margin-top:12px}
.card{border:1px solid #e5e7eb;border-radius:12px;padding:16px;background:#fff;margin-top:12px}
.card-hd{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px}
.status{font-size:12px;padding:2px 8px;border-radius:999px;background:#eef2ff}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:8px}
.tr{text-align:right}
.btn{padding:8px 12px;border-radius:8px;background:#1b64da;color:#fff;border:none;cursor:pointer}
.btn.danger{background:#e11d48}
</style>

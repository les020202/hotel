<!-- src/views/mypage/History.vue -->
<template>
  <div class="history page">
    <!-- ✅ 쿠폰/Account와 동일한 상단 제목 바 -->
    <div class="topbar">
      <div class="topbar-title">예약내역</div>
    </div>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="!rows.length" class="muted">예약 내역이 없습니다.</p>

    <div class="list" v-else>
      <div
        class="ticket"
        v-for="r in rows"
        :key="r.bookingId"
        @click="goDetail(r.bookingId)"
      >
        <div class="left">
          <div class="icon">🏨</div>
          <div class="dates">
            <div><b>Check-in</b> {{ fmtDate(r.checkIn) }}</div>
            <div><b>Check-out</b> {{ fmtDate(r.checkOut) }}</div>
            <div class="sub">
              {{ r.hotelName || '-' }} · {{ r.roomTypeName || '-' }}
            </div>
          </div>
        </div>

        <div class="mid">
          <div class="line">
            <span>체크인</span> <span class="time">15:00</span>
          </div>
          <div class="line">
            <span>체크아웃</span> <span class="time">11:00</span>
          </div>
          <div class="amt">
            {{ fmtAmount(r.totalAmount, r.currency) }}
          </div>
          <div class="status" :data-st="r.status">{{ toKStatus(r.status) }}</div>
        </div>

        <div class="right" @click.stop>
          <button
            class="btn"
            type="button"
            :disabled="!canOpenTicket(r)"
            @click="openTicket(r)"
            :title="canOpenTicket(r) ? '티켓 보기 / 인쇄' : '취소된 예약은 티켓을 제공하지 않습니다.'"
          >
            Download Ticket
          </button>
          <button class="btn ghost" type="button" @click="goDetail(r.bookingId)">
            상세보기
          </button>
        </div>
      </div>

      <div class="more">
        <button
          class="btn wide"
          :disabled="loading || page >= totalPages - 1"
          @click="loadMore"
        >
          {{ page < totalPages - 1 ? '더 보기' : '마지막 페이지입니다' }}
        </button>
      </div>
    </div>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/api/_http'

const router = useRouter()

const rows = ref([])
const loading = ref(false)
const errorMsg = ref('')

const page = ref(0)
const size = ref(10)
const totalPages = ref(1)
const totalElements = ref(0)

function fmtAmount(v, c) {
  if (v == null) return ''
  return `${Number(v).toLocaleString('ko-KR')}${c === 'KRW' ? '원' : (c ? ` ${c}` : '')}`
}
function fmtDate(d) {
  if (!d) return ''
  const [y, m, day] = String(d).split('-').map(Number)
  const dt = new Date(y, (m ?? 1) - 1, day ?? 1)
  return Number.isNaN(dt.getTime())
    ? d
    : dt.toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' })
}
function toKStatus(st) {
  const s = String(st || '').toUpperCase()
  if (s === 'CONFIRMED') return '확정'
  if (s === 'CANCELED' || s === 'CANCELLED') return '취소'
  if (s === 'PENDING') return '대기'
  return st
}
function isCanceled(st) {
  const s = String(st || '').toUpperCase()
  return s === 'CANCELED' || s === 'CANCELLED'
}

/** 취소 예약은 티켓 열기 비활성화 */
function canOpenTicket(row) {
  return !!(row && row.ticketAvailable && !isCanceled(row.status))
}

async function fetchPage(p, s) {
  // GET /api/my/bookings?page={p}&size={s}
  return await get(`/my/bookings?page=${p}&size=${s}`)
}

async function load(reset = false) {
  if (loading.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await fetchPage(page.value, size.value)
    const content = Array.isArray(res?.content) ? res.content : []
    totalPages.value = Number(res?.totalPages ?? 1)
    totalElements.value = Number(res?.totalElements ?? content.length)

    // 백엔드에서 r.receiptUrl 대신 "티켓 가능 여부" 플래그가 있다면 ticketAvailable로 매핑
    rows.value = (reset ? content : rows.value.concat(content)).map(x => ({
      ...x,
      ticketAvailable: x.ticketAvailable ?? !!x.receiptUrl ?? true,
    }))
  } catch (e) {
    console.error(e)
    errorMsg.value = e?.response?.data?.message || e.message || '불러오기 실패'
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (page.value >= totalPages.value - 1) return
  page.value += 1
  load(false)
}

function openTicket(row) {
  if (!canOpenTicket(row)) {
    alert('취소된 예약은 티켓을 제공하지 않습니다.')
    return
  }
  router.push({ name: 'MyBookingTicket', params: { id: row.bookingId } })
}

function goDetail(id) {
  router.push({ name: 'MyBookingDetail', params: { id } })
}

onMounted(() => load(true))
</script>

<style scoped>
/* 페이지 공통 여백 + topbar */
.page { max-width: 960px; margin: 0 auto; padding: 16px 12px; }

/* ✅ 쿠폰/Account와 동일 스타일의 상단 제목 바 */
.topbar {
  height: 60px; display: flex; align-items: center; gap: 8px;
  padding: 0 16px; border-bottom: 1px solid #f1f5f9;
  margin: -16px -12px 12px; background: #fff; justify-content: flex-start;
}
.topbar-title { font-weight: 800; font-size: 20px; text-align: left; }

.muted { color: #6b7280; }
.error { color: #dc2626; margin-top: 10px; }

.list { display: flex; flex-direction: column; gap: 10px; }

/* 카드 레이아웃 */
.ticket {
  display: grid;
  grid-template-columns: 1.5fr 1fr auto;
  align-items: center;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 12px;
  padding: 12px 14px;
  overflow: hidden; /* 전역 장식요소가 튀어나오는 것 방지 */
}
.left { display: flex; align-items: center; gap: 10px; }
.icon { width: 44px; height: 44px; border-radius: 10px; background: #f1f5f9; display: grid; place-items: center; }
.dates b { margin-right: 6px; }
.dates .sub { color: #64748b; font-size: 12px; margin-top: 2px; }

.mid { color: #374151; text-align: right; display:flex; flex-direction:column; gap:4px; }
/* 혹시 전역 스타일에서 mid 영역에 바(gradient/progress)를 뿌리는 경우 무력화 */
.mid, .ticket .mid * { background-image: none !important; }
.mid .line { display:flex; justify-content: flex-end; gap:8px; color:#6b7280; }
.mid .time { font-weight: 600; color:#111827; }
.mid .amt { font-weight: 700; }
.status {
  display:inline-block;
  margin-top:2px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef2ff;
  color:#1e3a8a;
}
.status[data-st="CONFIRMED"] { background:#dcfce7; color:#166534; }
.status[data-st="CANCELED"], .status[data-st="CANCELLED"] { background:#fee2e2; color:#991b1b; }
.status[data-st="PENDING"] { background:#fef9c3; color:#854d0e; }

.right { display: flex; flex-direction: column; gap: 6px; justify-content: flex-end; align-items: flex-end; }
.btn {
  padding: 8px 12px; border: 0; border-radius: 8px;
  background: #0a6; color: #fff; cursor: pointer;
}
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn.ghost { background: #111827; color:#fff; }

.more { display:flex; justify-content:center; margin-top:10px; }
.btn.wide { min-width: 220px; }

/* 혹시 전역 CSS에서 progress/meter 요소를 카드에 뿌린 경우 강제 제어 */
.ticket progress,
.ticket .progress,
.ticket .meter,
.ticket .bar {
  display: none !important;           /* 아예 숨기기 */
  width: auto !important;             /* 필요 시 글자폭 기준 */
  max-width: max-content !important;
}
</style>

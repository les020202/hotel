<!-- src/views/HistoryDetail.vue -->
<template>
  <div class="page">
    <!-- 상단 앱바 -->
    <header class="topbar">
      <button class="icon-btn" @click="router.back()" aria-label="목록으로">
        <svg viewBox="0 0 24 24" class="icon">
          <path d="M15.5 19a1 1 0 0 1-.7-.3l-6-6a1 1 0 0 1 0-1.4l6-6a1 1 0 1 1 1.4 1.4L10.9 12l5.3 5.3A1 1 0 0 1 15.5 19z"/>
        </svg>
      </button>
      <div class="topbar-title">예약 상세</div>

      <!-- 우측 액션 -->
      <div class="topbar-right text-right">
        <!-- 취소 가능 -->
        <button
          v-if="row && row.status !== 'CANCELLED' && canCancel"
          class="btn danger"
          @click="openCancel()"
        >
          예약 취소
        </button>

        <!-- 취소 불가(컷오프 경과) -->
        <div
          v-else-if="row && row.status !== 'CANCELLED' && !canCancel"
          class="flex flex-col items-end"
        >
          <button class="btn" disabled style="opacity:.5; cursor:not-allowed;">
            예약 취소 불가
          </button>
          <small class="mt-1 text-[12px] text-gray-300/90 block">
            {{ cannotCancelReason }}
          </small>
        </div>
      </div>
    </header>

    <main class="container">
      <!-- 로딩 스켈레톤 -->
      <div v-if="loading" class="skeleton-card" aria-hidden="true">
        <div class="skeleton-line w-60"></div>
        <div class="skeleton-line w-32 mt-2"></div>
        <div class="divider"></div>
        <div class="grid two">
          <div class="skeleton-line w-24"></div><div class="skeleton-line w-40"></div>
          <div class="skeleton-line w-24"></div><div class="skeleton-line w-40"></div>
          <div class="skeleton-line w-24"></div><div class="skeleton-line w-40"></div>
          <div class="skeleton-line w-24"></div><div class="skeleton-line w-40"></div>
        </div>
      </div>

      <div v-else-if="!row" class="empty">
        <div class="empty-emoji">🗂️</div>
        <p>해당 예약을 찾을 수 없습니다.</p>
        <button class="btn ghost mt-8" @click="router.back()">목록으로 돌아가기</button>
      </div>

      <article v-else class="card">
        <!-- 헤더: 번호 + 상태 -->
        <header class="card-head">
          <div class="id-line">
            <span class="hash">#</span><span class="bid">{{ row.bookingId }}</span>
          </div>
          <span class="status-badge" :data-st="row.status">{{ toKStatus(row.status) }}</span>
        </header>

        <!-- 호텔/객실 요약 -->
        <section class="summary">
          <div class="hotel-chip">
            <div class="logo" aria-hidden="true">🏨</div>
            <div class="texts">
              <div class="hotel-name" :title="row.hotelName">{{ row.hotelName }}</div>
              <div class="room-name" :title="row.roomTypeName">{{ row.roomTypeName }}</div>
            </div>
          </div>
          <div class="amount-box">
            <div class="caption">총 결제금액</div>
            <div class="amount">{{ fmtAmount(row.totalAmount, row.currency) }}</div>
          </div>
        </section>

        <div class="divider"></div>

        <!-- ✅ 체크인/아웃 + (N박) 한 줄로 자연스럽게 -->
        <section class="grid info-grid">
          <div class="label">
            <span class="ico" aria-hidden="true">📅</span> 체크인 · 체크아웃
          </div>
          <div class="value">
            {{ inOutText }}
          </div>

          <div class="label">
            <span class="ico" aria-hidden="true">👥</span> 인원
          </div>
          <div class="value">{{ row.guests }}명</div>

          <!-- 대표 투숙객 -->
          <div class="label">
            <span class="ico" aria-hidden="true">🧑</span> 대표 투숙객
          </div>
          <div class="value">{{ row.guestName || '-' }}</div>

          <div class="label">
            <span class="ico" aria-hidden="true">📞</span> 연락처
          </div>
          <div class="value">{{ row.guestPhone || '-' }}</div>

          <div class="label">
            <span class="ico" aria-hidden="true">🏨</span> 호텔 상세
          </div>
          <div class="value">
            <button class="link-as-btn" type="button" @click="goHotelDetail(row)">
              View Place
            </button>
          </div>
        </section>

        <!-- 취소 메타 -->
        <section v-if="row.status==='CANCELLED'" class="grid info-grid">
          <div class="label">🕒 취소일</div>
          <div class="value">{{ displayCanceledAt }}</div>

          <div class="label">🙍 취소자</div>
          <div class="value">{{ row.guestName || '-' }}</div>

          <div class="label">📝 사유</div>
          <div class="value">{{ row.cancelReason || '-' }}</div>
        </section>
      </article>
    </main>

    <!-- 예약 취소 다이얼로그 -->
    <CancelDialog
      :open="cancelOpen"
      @close="cancelOpen = false"
      @submit="doCancel"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchMyBooking } from '@/api/myBookings'
import { cancelBooking } from '@/api/bookings'
import CancelDialog from '@/components/common/CancelDialog.vue'

const route = useRoute()
const router = useRouter()

const row = ref(null)
const loading = ref(false)
const id = Number(route.params.id)

/* ---------- 방어적 매핑 ---------- */
function normalizeBooking(r = {}) {
  const n = { ...r }

  n.bookingId = r.bookingId ?? r.id ?? r.booking_id ?? null

  if (typeof r.status === 'string') n.status = r.status.trim().toUpperCase()

  n.canceledAt =
    r.canceledAt ?? r.cancelledAt ?? r.canceled_at ?? r.cancelled_at ?? null
  n.canceledBy =
    r.canceledBy ?? r.cancelledBy ?? r.canceled_by ?? r.cancelled_by ?? null
  n.cancelReason = r.cancelReason ?? r.cancel_reason ?? null

  n.hotelId = r.hotelId ?? r.hotelsId ?? r.hotels_id ?? r.hotels?.id ?? null

  n.hotelName   = r.hotelName ?? r.hotel_name ?? r.hotels?.name ?? n.hotelName
  n.roomTypeName= r.roomTypeName ?? r.room_type_name ?? n.roomTypeName

  n.totalAmount = r.totalAmount ?? r.total_amount ?? n.totalAmount
  n.currency    = r.currency ?? n.currency
  n.nights      = r.nights ?? n.nights
  n.guests      = r.guests ?? n.guests

  n.checkIn     = r.checkIn  ?? r.check_in  ?? n.checkIn
  n.checkOut    = r.checkOut ?? r.check_out ?? n.checkOut

  return n
}

/* 표시용 취소일 */
const displayCanceledAt = computed(() => {
  const s = row.value?.canceledAt || ''
  return s.includes('T') ? s.replace('T', ' ').slice(0, 16) : (s || '-')
})

/* ✅ 체크인/체크아웃 + (N박) 한 줄 표시 */
const inOutText = computed(() => {
  const ci = fmtDate(row.value?.checkIn)
  const co = fmtDate(row.value?.checkOut)
  const nights = row.value?.nights
  const range = [ci, co].filter(Boolean).join(' ~ ')
  return range + (nights ? ` (${nights}박)` : '')
})

/* ---------- 취소 가능 여부(사용자 규칙: 전날 23:59까지) ---------- */
const canCancel = computed(() => {
  if (!row.value) return false
  if (row.value.status === 'CANCELLED') return false
  const cin = normalizeDate(row.value.checkIn)
  if (!cin) return false
  const today = todayStr()
  return today < cin
})

const cannotCancelReason = computed(
  () => '체크인 전날 23:59 까지 예약취소가 가능합니다.'
)

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

/* ---------- 취소 다이얼로그 ---------- */
const cancelOpen = ref(false)
function openCancel() { cancelOpen.value = true }

async function doCancel({ reason }) {
  if (!row.value) return
  try {
    const bookingId = row.value.bookingId ?? row.value.id
    await cancelBooking(bookingId, reason || '')
    await load()
    alert('예약이 취소되었습니다.')
  } catch (e) {
    console.error(e)
    let msg = '취소에 실패했습니다.'
    try {
      const parsed = JSON.parse(e.message || '{}')
      if (parsed?.error) msg = parsed.error
    } catch {}
    alert(msg)
  } finally {
    cancelOpen.value = false
  }
}

/* ---------- 표시 유틸 ---------- */
function fmtAmount(v, c) {
  if (v == null) return ''
  return `${Number(v).toLocaleString('ko-KR')}${c === 'KRW' ? '원' : (c ? ` ${c}` : '')}`
}
function fmtDate(d) { return d ?? '' }
function toKStatus(st) {
  switch (st) {
    case 'CONFIRMED': return '확정'
    case 'CANCELLED': return '취소'
    case 'PENDING':   return '대기'
    default:          return st
  }
}

/* 호텔 상세 */
function goHotelDetail(r) {
  if (!r) return
  const hid =
    r.hotelId ?? r.hotelsId ?? r.hotels_id ?? r.hotels?.id ?? r.hotelsIdRef ?? null
  if (!hid) {
    console.warn('[예약상세] 호텔 ID 없음', r)
    return
  }
  const query = {
    ...(r.checkIn ? { checkIn: r.checkIn } : {}),
    ...(r.checkOut ? { checkOut: r.checkOut } : {}),
    ...(r.guests ? { adults: String(r.guests) } : {}),
  }
  router.push({ path: `/hotels/${hid}`, query })
}

/* 데이터 로드 */
async function load() {
  loading.value = true
  try {
    const raw = await fetchMyBooking(id)
    row.value = normalizeBooking(raw)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* ========== 레이아웃 ========== */
.page{
  min-height:100vh;
  background: linear-gradient(180deg,#0f172a 0%, #0f172a 60px, #f6f7fb 60px, #f6f7fb 100%);
}
.container{ max-width: 960px; margin: 0 auto; padding: 20px 16px 40px; }

/* ========== 앱바 ========== */
.topbar{
  position: sticky; top: 0; z-index: 20;
  display:flex; align-items:center; justify-content:space-between;
  height: 56px; padding: 0 12px;
  background:#0f172a; color:#fff;
  backdrop-filter: saturate(1.2) blur(6px);
  border-bottom: 1px solid rgba(255,255,255,.06);
}
.topbar-title{ font-weight:800; letter-spacing:.3px }
.icon-btn{
  width:36px;height:36px;border-radius:10px;border:1px solid rgba(255,255,255,.18);
  background:rgba(255,255,255,.06); color:#fff; display:grid; place-items:center; cursor:pointer;
}
.icon-btn:hover{ background:rgba(255,255,255,.12) }
.icon{ width:18px;height:18px; fill:currentColor; display:block }

/* ========== 카드 ========== */
.card{
  margin-top: 14px; border: 1px solid #e8ebf2; border-radius: 16px; background: #fff;
  box-shadow: 0 8px 28px rgba(15, 23, 42, .06); overflow: hidden;
}
.card-head{ display:flex; align-items:center; justify-content:space-between; padding: 16px 18px 8px 18px; }
.id-line{ display:flex; align-items:baseline; gap:6px }
.hash{ color:#94a3b8; font-weight:700 }
.bid{ font-size:22px; font-weight:900; letter-spacing:.2px }

/* 상태 배지 */
.status-badge{
  font-size:12px; font-weight:700; padding:5px 10px; border-radius:999px;
  background:#eef2ff; color:#3730a3; border:1px solid #e0e7ff;
}
.status-badge[data-st="CANCELLED"]{ background:#fee2e2; color:#991b1b; border-color:#fecaca }
.status-badge[data-st="CONFIRMED"]{ background:#dcfce7; color:#065f46; border-color:#bbf7d0 }

/* 요약 영역 */
.summary{
  display:flex; align-items:flex-start; justify-content:space-between;
  gap:16px; padding: 4px 18px 16px 18px; flex-wrap: wrap;
}
.hotel-chip{ display:flex; align-items:center; gap:12px; min-width:0 }
.logo{
  width:44px; height:44px; border-radius:12px; display:grid; place-items:center; font-size:22px;
  background:linear-gradient(135deg, #e0e7ff, #fff); border:1px solid #e5e7eb;
}
.texts{ min-width:0 }
.hotel-name{ font-weight:800; font-size:16px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.room-name{ margin-top:2px; color:#64748b; font-size:13px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }

.amount-box{ margin-left:auto; text-align:right; }
.amount-box .caption{ font-size:12px; color:#64748b; margin-bottom:2px; }
.amount-box .amount{ font-size:22px; font-weight:900; color:#ef4444; }

/* 구분선 */
.divider{
  height:1px; background:linear-gradient(90deg, transparent, #eef0f4, transparent);
  margin: 8px 0;
}

/* 상세 정보 그리드 */
.info-grid{ padding: 8px 18px 18px 18px; }
.grid{ display:grid; gap:10px; }
.grid.two{ grid-template-columns: 1fr 1fr; }
.info-grid{ grid-template-columns: 160px 1fr; }
@media (max-width: 560px){
  .info-grid{ grid-template-columns: 1fr; }
  .value{ text-align:left !important; }
}

.label{ color:#334155; font-weight:700; display:flex; align-items:center; gap:8px; }
.label .ico{ font-size:16px }
.value{ text-align:right; color:#0f172a; font-weight:600; }
.value .sub{ color:#6b7280; font-weight:600; margin-left:6px }

/* 내부 링크 버튼 */
.link-as-btn{
  padding:0; border:0; background:transparent; color:#2563eb;
  text-decoration:underline; text-underline-offset:2px; cursor:pointer; font: inherit;
}

/* 빈 상태 */
.empty{
  margin-top: 18px; border: 1px dashed #d7dbe6; border-radius: 16px; background: #fff;
  padding: 36px 20px; text-align:center; color:#64748b;
}
.empty-emoji{ font-size:32px; margin-bottom:8px }

/* 버튼 */
.btn{
  padding:10px 14px; border-radius:10px; border:1px solid #1b64da;
  background:#1b64da; color:#fff; font-weight:700; cursor:pointer;
  box-shadow: 0 6px 16px rgba(27,100,218,.15);
}
.btn:hover{ filter: brightness(1.02) }
.btn.ghost{ background:#fff; color:#1b64da; border:1px solid #c7d2fe; }
.btn.danger{ background:#e11d48; border-color:#e11d48; }

/* 스켈레톤 */
.skeleton-card{
  margin-top: 14px; background:#fff; border:1px solid #e8ebf2; border-radius:16px; padding:18px;
  box-shadow: 0 8px 28px rgba(15, 23, 42, .06);
}
.skeleton-line{
  height:12px; border-radius:6px;
  background:linear-gradient(90deg,#f3f4f6 25%,#eceff3 37%,#f3f4f6 63%);
  background-size:400% 100%; animation: shimmer 1.4s ease infinite;
}
.skeleton-line.w-60{ width:60% } .skeleton-line.w-40{ width:40% } .skeleton-line.w-32{ width:32% }
.mt-2{ margin-top:8px } .mt-8{ margin-top:20px }

@keyframes shimmer{ 0%{ background-position: 100% 0 } 100%{ background-position: -100% 0 } }
</style>

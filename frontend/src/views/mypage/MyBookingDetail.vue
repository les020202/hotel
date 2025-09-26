<template>
  <div class="page">
    <!-- 상단 앱바 느낌 -->
    <header class="topbar">
      <button class="icon-btn" @click="router.back()" aria-label="목록으로">
        <!-- back chevron -->
        <svg viewBox="0 0 24 24" class="icon">
          <path d="M15.5 19a1 1 0 0 1-.7-.3l-6-6a1 1 0 0 1 0-1.4l6-6a1 1 0 1 1 1.4 1.4L10.9 12l5.3 5.3A1 1 0 0 1 15.5 19z"/>
        </svg>
      </button>
      <div class="topbar-title">예약 상세</div>
      <div class="topbar-right"></div>
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
        <!-- 헤더: 번호 + 상태 배지 -->
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

        <!-- 상세 그리드 -->
        <section class="grid info-grid">
          <div class="label">
            <span class="ico" aria-hidden="true">📅</span> 체크인
          </div>
          <div class="value">{{ fmtDate(row.checkIn) }}</div>

          <div class="label">
            <span class="ico" aria-hidden="true">📆</span> 체크아웃
          </div>
          <div class="value">
            {{ fmtDate(row.checkOut) }}
            <span v-if="row.nights" class="sub">({{ row.nights }}박)</span>
          </div>

          <div class="label">
            <span class="ico" aria-hidden="true">👥</span> 인원
          </div>
          <div class="value">{{ row.guests }}명</div>

          <div class="label">
            <span class="ico" aria-hidden="true">🏨</span> 호텔 상세
          </div>
          <div class="value">
            <button
              class="link-as-btn"
              type="button"
              @click="goHotelDetail(row)"
            >
              View Place
            </button>
          </div>
        </section>
      </article>
    </main>
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

// ✅ /hotels/:id 로 이동 (checkIn/checkOut/adults 쿼리 포함)
function goHotelDetail(r) {
  if (!r) return
  // 다양한 키에서 호텔 id 시도
  const hid =
    r.hotelsId ??
    r.hotels_id ??
    r.hotels?.id ??
    r.hotelsIdRef ?? // 혹시 다른 이름을 썼을 수 있어 대비
    null

  if (!hid) {
    console.warn('[예약상세] 호텔 ID를 찾을 수 없어 상세로 이동하지 않았습니다.', r)
    return
  }

  const query = {
    ...(r.checkIn ? { checkIn: r.checkIn } : {}),
    ...(r.checkOut ? { checkOut: r.checkOut } : {}),
    ...(r.guests ? { adults: String(r.guests) } : {}),
  }

  // 라우트 name이 없다면 path 사용이 가장 안전
  router.push({ path: `/hotels/${hid}`, query })
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
/* ========== 레이아웃 ========== */
.page{
  min-height:100vh;
  background: linear-gradient(180deg,#0f172a 0%, #0f172a 60px, #f6f7fb 60px, #f6f7fb 100%);
}
.container{
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

/* ========== 앱바 ========== */
.topbar{
  position: sticky;
  top: 0;
  z-index: 20;
  display:flex; align-items:center; justify-content:space-between;
  height: 56px;
  padding: 0 12px;
  background:#0f172a; /* slate-900 */
  color:#fff;
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
  margin-top: 14px;
  border: 1px solid #e8ebf2;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 28px rgba(15, 23, 42, .06);
  overflow: hidden;
}
.card-head{
  display:flex; align-items:center; justify-content:space-between;
  padding: 16px 18px 8px 18px;
}
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
  gap:16px; padding: 4px 18px 16px 18px;
  flex-wrap: wrap;
}
.hotel-chip{ display:flex; align-items:center; gap:12px; min-width:0 }
.logo{
  width:44px; height:44px; border-radius:12px;
  display:grid; place-items:center; font-size:22px;
  background:linear-gradient(135deg, #e0e7ff, #fff);
  border:1px solid #e5e7eb;
}
.texts{ min-width:0 }
.hotel-name{
  font-weight:800; font-size:16px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;
}
.room-name{
  margin-top:2px; color:#64748b; font-size:13px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;
}

.amount-box{
  margin-left:auto; text-align:right;
}
.amount-box .caption{
  font-size:12px; color:#64748b; margin-bottom:2px;
}
.amount-box .amount{
  font-size:22px; font-weight:900; color:#ef4444;
}

/* 구분선 */
.divider{
  height:1px; background:linear-gradient(90deg, transparent, #eef0f4, transparent);
  margin: 8px 0;
}

/* 상세 정보 그리드 */
.info-grid{
  padding: 8px 18px 18px 18px;
}
.grid{
  display:grid; gap:10px;
}
.grid.two{ grid-template-columns: 1fr 1fr; }
.info-grid{ grid-template-columns: 160px 1fr; }
@media (max-width: 560px){
  .info-grid{ grid-template-columns: 1fr; }
  .value{ text-align:left !important; }
}

.label{
  color:#334155; font-weight:700; display:flex; align-items:center; gap:8px;
}
.label .ico{ font-size:16px }
.value{
  text-align:right; color:#0f172a; font-weight:600;
}
.value .sub{ color:#6b7280; font-weight:600; margin-left:6px }

/* 내부 링크 버튼 스타일 */
.link-as-btn{
  padding:0;
  border:0;
  background:transparent;
  color:#2563eb;
  text-decoration:underline;
  text-underline-offset:2px;
  cursor:pointer;
  font: inherit;
}

/* 빈 상태 */
.empty{
  margin-top: 18px;
  border: 1px dashed #d7dbe6;
  border-radius: 16px;
  background: #fff;
  padding: 36px 20px;
  text-align:center;
  color:#64748b;
}
.empty-emoji{ font-size:32px; margin-bottom:8px }

/* 버튼 */
.btn{
  padding:10px 14px; border-radius:10px; border:1px solid #1b64da;
  background:#1b64da; color:#fff; font-weight:700; cursor:pointer;
  box-shadow: 0 6px 16px rgba(27,100,218,.15);
}
.btn:hover{ filter: brightness(1.02) }
.btn.ghost{
  background:#fff; color:#1b64da; border:1px solid #c7d2fe;
}
.btn.danger{
  background:#e11d48; border-color:#e11d48;
}

/* 스켈레톤 */
.skeleton-card{
  margin-top: 14px;
  background:#fff; border:1px solid #e8ebf2; border-radius:16px; padding:18px;
  box-shadow: 0 8px 28px rgba(15, 23, 42, .06);
}
.skeleton-line{
  height:12px; border-radius:6px; background:linear-gradient(90deg,#f3f4f6 25%,#eceff3 37%,#f3f4f6 63%);
  background-size:400% 100%; animation: shimmer 1.4s ease infinite;
}
.skeleton-line.w-60{ width:60% }
.skeleton-line.w-40{ width:40% }
.skeleton-line.w-32{ width:32% }
.mt-2{ margin-top:8px }
.mt-8{ margin-top:20px }

@keyframes shimmer{
  0%{ background-position: 100% 0}
  100%{ background-position: -100% 0}
}

/* 유틸 */
.muted{ color:#64748b }
.link{ color:#2563eb; text-decoration:underline; text-underline-offset:2px }
</style>

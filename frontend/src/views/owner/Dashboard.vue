<script setup lang="js">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import DashboardGraphs from '@/components/owner_graph/DashboardGraphs.vue'

const route = useRoute()
const router = useRouter()

const hotelId = ref(null)
const myHotels = ref([])
const currentHotel = computed(() => myHotels.value.find(h => h.id === hotelId.value))
  import { api } from '@/lib/api'
async function loadHotels() {

const res = await api('/api/owner/hotels')
  if (res.ok) {
    myHotels.value = await res.json()
  }
}

const logout = () => {
    localStorage.removeItem('token')
    document.cookie = 'refreshToken=; Max-Age=0; path=/;'
    router.push('/login')
  }

/* 오늘 잔여 객실 상태 */
const todayRemaining = ref(null)
const loadingRemain = ref(false)

/*  이번 주 예약 수 상태 */
const weeklyCount = ref(null)
const loadingWeekly = ref(false)

/* 오늘 체크인 수 상태 */
const todayCheckIn = ref(null)
const loadingTodayCheckIn = ref(false)
const nowCheckIn = ref(null)
const loadingNow = ref(false)


/*  오늘 잔여 객실 로드 */
async function loadTodayRemaining () {
  if (!hotelId.value) return
  loadingRemain.value = true
  try {
    const res = await api(`/api/owner/hotels/${hotelId.value}/inventory/today`)
    if (res.ok) {
      const data = await res.json()
      todayRemaining.value =
        typeof data === 'number'
          ? data
          : (data.totalRemainingQty ?? data.total ?? data.value ?? null)
    } else {
      todayRemaining.value = null
    }
  } catch {
    todayRemaining.value = null
  } finally {
    loadingRemain.value = false
  }
}

/*  이번 주 예약 수 로더 */
async function loadWeeklyCount () {
  if (!hotelId.value) return
  loadingWeekly.value = true
  try {
    // 백엔드: GET /api/owner/hotels/{hotelId}/bookings/weekly-count → { weeklyCount: number }
    const res = await api(`/api/owner/hotels/${hotelId.value}/bookings/weekly-count`)
    if (res.ok) {
      const data = await res.json()
      weeklyCount.value =
        typeof data === 'number'
          ? data
          : (data.weeklyCount ?? data.count ?? null)
    } else {
      weeklyCount.value = null
    }
  } catch {
    weeklyCount.value = null
  } finally {
    loadingWeekly.value = false
  }
}

/* 오늘 체크인 상태 로더*/ 
async function loadTodayCheckIn () {
  if (!hotelId.value) return
  loadingTodayCheckIn.value = true
  try {
    const res = await api(`/api/owner/hotels/${hotelId.value}/bookings/today-checkin-count`)
    if (res.ok) {
      const data = await res.json()
      todayCheckIn.value =
        typeof data === 'number'
          ? data
          : (data.todayCheckInCount ?? data.count ?? null)
    } else {
      todayCheckIn.value = null
    }
  } catch {
    todayCheckIn.value = null
  } finally {
    loadingTodayCheckIn.value = false
  }
}
async function loadNowCheckIn () {
  if (!hotelId.value) return
  loadingNow.value = true
  try {
    const res = await api(`/api/owner/hotels/${hotelId.value}/checkins/now-count`)
    if (res.ok) {
      const data = await res.json()
      nowCheckIn.value =
        typeof data === 'number'
          ? data
          : (data.nowCheckInCount ?? data.count ?? null)
    } else {
      nowCheckIn.value = null
    }
  } catch {
    nowCheckIn.value = null
  } finally {
    loadingNow.value = false
  }
}

/* --------------------------- */
/* [ADD: Sales Chart] 매출 차트 */
/* --------------------------- */
const salesMode = ref('week')        // 'week' | 'month'
const salesData = ref([])            // [{ label: string, amount: number }]
const salesLoading = ref(false)
const salesErr = ref('')

/* [ADD] 주/월 이동용 앵커 날짜 (기본: 오늘) */
const salesAnchor = ref(new Date())

/* [ADD] 날짜 유틸 */
const pad2 = (n) => String(n).padStart(2, '0')
const fmtYMD = (d) => `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())}`
const mondayOf = (d) => {
  const t = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const day = t.getDay() || 7 // Sun=0 → 7
  if (day !== 1) t.setDate(t.getDate() - (day - 1))
  return t
}
const monthStart = (d) => new Date(d.getFullYear(), d.getMonth(), 1)
const isSameYMD = (a,b) => a.getFullYear()===b.getFullYear() && a.getMonth()===b.getMonth() && a.getDate()===b.getDate()

/* [ADD] 현재 주/월 여부 체크 → 앞으로(→) 버튼 비활성화 */
const canNext = computed(() => {
  if (salesMode.value === 'week') {
    const cur = mondayOf(new Date())
    const anchor = mondayOf(salesAnchor.value)
    return anchor < cur
  } else {
    const curM = monthStart(new Date())
    const anchorM = monthStart(salesAnchor.value)
    return anchorM < curM
  }
})

/* [ADD] 이전/다음 기간 이동 */
function shiftPrev () {
  const dt = new Date(salesAnchor.value)
  if (salesMode.value === 'week') {
    dt.setDate(dt.getDate() - 7)
  } else {
    dt.setMonth(dt.getMonth() - 1)
  }
  salesAnchor.value = dt
  loadSales()
}
function shiftNext () {
  if (!canNext.value) return
  const dt = new Date(salesAnchor.value)
  if (salesMode.value === 'week') {
    dt.setDate(dt.getDate() + 7)
  } else {
    dt.setMonth(dt.getMonth() + 1)
  }
  salesAnchor.value = dt
  loadSales()
}

/* [ADD] 모드 바꾸면 앵커를 현재로 리셋 */
watch(salesMode, () => {
  salesAnchor.value = new Date()
  loadSales()
})

async function loadSales () {
  if (!hotelId.value) return
  salesLoading.value = true
  salesErr.value = ''
  try {
    let url = `/api/owner/hotels/${hotelId.value}/sales?mode=${salesMode.value}`

    if (salesMode.value === 'week') {
      const start = fmtYMD(mondayOf(salesAnchor.value))
      url += `&start=${start}`
    } else {
      const start = fmtYMD(monthStart(salesAnchor.value))
      // end를 같은 달의 1일로 넣으면(서비스에서 +1개월 처리) 해당 월만 반환
      url += `&start=${start}&end=${start}`
    }

    const res = await api(url)
    if (!res.ok) throw new Error(await res.text())
    const raw = await res.json()
    const array = Array.isArray(raw) ? raw : (raw?.items ?? [])
    salesData.value = array.map(it => ({
      label: it.label ?? it.date ?? it.key ?? '',
      amount: Number(it.amount ?? it.total ?? it.value ?? 0)
    }))
  } catch (e) {
    salesErr.value = e?.message || '불러오기 실패'
    salesData.value = []
  } finally {
    salesLoading.value = false
  }
}

// 간단 SVG 라인차트 계산 유틸
const chartViewport = { w: 640, h: 220, pad: 24 }
const salesMax = computed(() => {
  const max = Math.max(0, ...salesData.value.map(d => d.amount))
  return max > 0 ? Math.ceil(max * 1.2) : 1
})
const salesPath = computed(() => {
  const { w, h, pad } = chartViewport
  const N = salesData.value.length
  if (N === 0) return ''
  const max = salesMax.value
  const xStep = (w - pad * 2) / Math.max(1, N - 1)
  const toXY = (i, v) => {
    const x = pad + i * xStep
    const y = pad + (1 - v / max) * (h - pad * 2)
    return [x, y]
  }
  return salesData.value.map((d, i) => {
    const [x, y] = toXY(i, d.amount)
    return i === 0 ? `M ${x} ${y}` : `L ${x} ${y}`
  }).join(' ')
})
const salesTicks = computed(() => {
  const N = salesData.value.length
  if (N === 0) return []
  const idxs = new Set([0, Math.floor((N - 1) / 2), N - 1])
  const { w, pad } = chartViewport
  const xStep = (w - pad * 2) / Math.max(1, N - 1)
  return salesData.value.map((d, i) => ({
    show: idxs.has(i),
    x: pad + i * xStep,
    label: d.label
  }))
})

/* [ADD] 숫자 → KRW 포맷 */
const fmtKRW = (v) => new Intl.NumberFormat('ko-KR').format(Math.round(v ?? 0))
/* --------------------------- */
/* [/ADD: Sales Chart]         */

onMounted(async () => {
  hotelId.value = Number(route.params.hotelId)
  await loadHotels()
  await loadTodayRemaining()
  await loadWeeklyCount()
  await loadTodayCheckIn()
  await loadNowCheckIn() 

  /* [ADD: Sales Chart] */
  await loadSales()
})

watch(() => route.params.hotelId, async (v) => { hotelId.value = Number(v) 
  await loadTodayRemaining()
  await loadWeeklyCount()
  await loadTodayCheckIn()
  await loadNowCheckIn() 

  /* [ADD: Sales Chart] */
  await loadSales()
})

/* [ADD: Sales Chart] 모드 변경 시 재로딩 (위에서 watch로 대체 but 안전) */
// watch(salesMode, loadSales)
</script>

<template>
  <div class="owner-dashboard">

    <!-- 상단 바 (좌: 대시보드, 우: 사이트 보기 / 로그아웃) -->
    <header class="topbar">
      <div class="title"><h1 class="text-xl font-bold">대시보드</h1></div>
      <div class="actions">
        <button class="btn ghost" @click="$router.push('/main')">사이트 보기</button>
        <button class="btn" @click="logout">로그아웃</button>
      </div>
    </header>

    <!-- KPI 타일 -->
    <section class="kpis">
      <div class="kpi card">
        <div class="kpi-label">오늘 잔여 객실</div>
        <div class="kpi-value">
          <span v-if="loadingRemain">…</span>
          <span v-else>{{ todayRemaining ?? '—' }}</span>
        </div>
        <div class="kpi-help">인벤토리 집계</div>
      </div>

      <div class="kpi card">
        <div class="kpi-label">이번 주 예약 수</div>
        <div class="kpi-value">
          <span v-if="loadingWeekly">…</span>
          <span v-else>{{ weeklyCount ?? '—' }}</span>
        </div>
        <div class="kpi-help">주간 합계</div>
      </div>

      <div class="kpi card">
        <div class="kpi-label">체크인 예정(오늘)</div>
        <div class="kpi-value">
          <span v-if="loadingTodayCheckIn">…</span>
          <span v-else>{{ todayCheckIn ?? '—' }}</span>
        </div>
        <div class="kpi-help">예약 기준</div>
      </div>

      <div class="kpi card">
        <div class="kpi-label">체크인 완료(현재)</div>
        <div class="kpi-value">
          <span v-if="loadingNow">…</span>
          <span v-else>{{ nowCheckIn ?? '—' }}</span>
        </div>
        <div class="kpi-help">실시간</div>
      </div>
    </section>

    <!-- [ADD: Sales Chart] 매출 차트(주/월 + 좌우 이동) -->
    <section class="panel card">
      <div class="panel-head between">
        <div class="panel-title">
          <h3>매출 추이</h3>
          <span class="muted">({{ salesMode === 'week' ? '주별' : '월별' }})</span>
        </div>

        <div class="segmented with-arrows">
          <!-- [ADD] 이전 버튼 -->
          <button class="seg-arrow" @click="shiftPrev" aria-label="이전 기간">‹</button>

          <!-- 기존 토글 -->
          <button class="seg-btn" :class="{ active: salesMode==='week' }" @click="salesMode='week'">주</button>
          <button class="seg-btn" :class="{ active: salesMode==='month' }" @click="salesMode='month'">월</button>

          <!-- [ADD] 다음 버튼 (미래 기간은 비활성) -->
          <button class="seg-arrow" :disabled="!canNext" @click="shiftNext" aria-label="다음 기간">›</button>
        </div>
      </div>

      <div class="chart-area">
        <div v-if="salesLoading" class="chart-empty">불러오는 중…</div>
        <div v-else-if="salesErr" class="chart-empty">{{ salesErr }}</div>
        <div v-else-if="!salesData.length" class="chart-empty">데이터가 없습니다.</div>

        <svg v-else
             :viewBox="`0 0 ${chartViewport.w} ${chartViewport.h}`"
             class="linechart">
          <rect x="0" y="0" :width="chartViewport.w" :height="chartViewport.h" fill="white" rx="12" />
          <line :x1="24" :y1="chartViewport.h-24" :x2="chartViewport.w-24" :y2="chartViewport.h-24"
                stroke="#e5e7eb" stroke-width="1"/>

          <defs>
            <linearGradient id="gradFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-opacity="0.25" stop-color="#1f2937"/>
              <stop offset="100%" stop-opacity="0" stop-color="#1f2937"/>
            </linearGradient>
          </defs>

          <!-- 면적 -->
          <path
            :d="`${salesPath} L ${chartViewport.w-24} ${chartViewport.h-24} L 24 ${chartViewport.h-24} Z`"
            fill="url(#gradFill)" />

          <!-- 라인 -->
          <path :d="salesPath" fill="none" stroke="#111827" stroke-width="2.5" stroke-linecap="round"/>

          <!-- 점 -->
          <g v-for="(d,i) in salesData" :key="'pt'+i">
            <circle
              :cx="24 + (i * ( (chartViewport.w-48) / Math.max(1, salesData.length-1) ))"
              :cy="24 + (1 - (d?.amount ?? 0) / salesMax) * (chartViewport.h-48)"
              r="3.2" fill="#111827" />
          </g>

          <!-- 점 위 금액 라벨 -->
          <g v-for="(d,i) in salesData" :key="'label'+i">
            <text
              :x="24 + (i * ( (chartViewport.w-48) / Math.max(1, salesData.length-1) ))"
              :y="24 + (1 - (d?.amount ?? 0) / salesMax) * (chartViewport.h-48) - 8"
              text-anchor="middle"
              font-size="11"
              fill="#111827"
              stroke="white"
              stroke-width="3"
              style="paint-order: stroke fill"
            >
              {{ fmtKRW(d?.amount) }}원
            </text>
          </g>

          <!-- x축 레이블 -->
          <template v-for="(t, i) in salesTicks" :key="'t'+i">
            <g v-if="t && t.show">
              <text :x="t.x" :y="chartViewport.h - 6" text-anchor="middle" font-size="11" fill="#9ca3af">
                {{ t.label }}
              </text>
            </g>
          </template>

        </svg>
      </div>
    </section>
    <!-- [/ADD: Sales Chart] -->

    <!-- 기존 그래프 컴포넌트는 하단 유지 (기능 유지) -->
    <DashboardGraphs />

    <!-- 호텔 정보 안내 라벨 -->
    <div class="hotel-label" v-if="currentHotel">
      {{ currentHotel.name }} · {{ currentHotel.region }} · 사업자 {{ currentHotel.businessNo }}
    </div>

    <!-- 바로가기 버튼들 -->
    <div class="quick-actions">
      <button class="btn primary"
              @click="$router.push(`/owner/hotels/${hotelId}/inventory`)">
        재고 캘린더로 이동
      </button>
      <button class="btn outline"
              @click="$router.push(`/owner/hotels/${hotelId}/bookings`)">
        예약 목록으로 이동
      </button>
    </div>
  </div>
</template>

<style scoped>
/* 레이아웃 컨테이너 */
.owner-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 24px;
  color: #111827;
}

/* 상단 바 */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.title { font-weight: 800; font-size: 18px; }
.actions { display: flex; gap: 12px; }

/* 공통 카드 */
.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 14px;
}

/* 필터 바 */
.filterbar { display: grid; gap: 12px; }
.range-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.filters-row {
  display: grid;
  grid-template-columns: 180px 1fr 140px auto auto auto;
  gap: 10px;
  align-items: center;
}
@media (max-width: 960px) {
  .filters-row {
    grid-template-columns: 1fr;
  }
}

.chip {
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 8px 12px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 12px;
  cursor: default;
}
.chip.is-active {
  background: #0f172a;
  color: #fff;
  border-color: #0f172a;
}
.date {
  height: 36px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 0 10px;
  font-size: 13px;
}
.select, .input {
  height: 36px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 0 12px;
  font-size: 13px;
  background: #fff;
}
.input::placeholder { color: #9ca3af; }
.spacer { flex: 1; }

/* 버튼 */
.btn {
  appearance: none;
  border: 0;
  outline: 0;
  padding: 9px 14px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all .15s ease;
  background: #0f172a;
  color: #fff;
}
.btn.small { padding: 7px 12px; border-radius: 10px; }
.btn.primary { background: #0f172a; color: #fff; }
.btn.outline {
  background: #fff;
  color: #111827;
  border: 1px solid #e5e7eb;
}
.btn.ghost {
  background: transparent;
  color: #1f2937;
  padding: 0;
  border-radius: 0;
}
.btn:hover { filter: brightness(0.97); }
.btn.ghost:hover { text-decoration: underline; filter: none; }

/* KPI */
.kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
@media (max-width: 1024px) {
  .kpis { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 560px) {
  .kpis { grid-template-columns: 1fr; }
}
.kpi-label { font-size: 12px; color: #6b7280; }
.kpi-value { font-size: 26px; font-weight: 800; margin-top: 4px; }
.kpi-help  { font-size: 12px; color: #9ca3af; margin-top: 2px; }

/* 표/차트 패널 */
.bottom-panels {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 12px;
}
@media (max-width: 1100px) {
  .bottom-panels { grid-template-columns: 1fr; }
}
.panel-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.panel-head.between {
  align-items: center;
  justify-content: space-between;
}
.panel-title { display: flex; align-items: baseline; gap: 8px; }
.panel-head h3 { font-size: 14px; font-weight: 800; }
.muted { color: #9ca3af; font-size: 12px; }

/* 테이블 */
.table-wrap { overflow: auto; }
table { width: 100%; border-collapse: collapse; }
thead th {
  text-align: left;
  font-size: 12px;
  color: #6b7280;
  font-weight: 700;
  padding: 10px 8px;
  border-bottom: 1px solid #f1f5f9;
}
tbody td {
  padding: 12px 8px;
  border-bottom: 1px solid #f8fafc;
  font-size: 13px;
}
tbody tr.empty td {
  text-align: center;
  color: #9ca3af;
  padding: 28px 8px;
}

/* 차트 자리표시자 (기존) */
.chart-placeholder {
  height: 260px;
  border: 1px dashed #e5e7eb;
  border-radius: 12px;
  background: linear-gradient(180deg, #fff 0%, #fbfdff 100%);
}

/* [ADD: Sales Chart] 토글/차트 스타일 */
.segmented {
  display: inline-flex;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}
.segmented.with-arrows {
  align-items: center;
}
.seg-btn {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  color: #374151;
  background: #fff;
  border: 0;
  cursor: pointer;
}
.seg-btn + .seg-btn { border-left: 1px solid #e5e7eb; }
.seg-btn.active {
  background: #111827;
  color: #fff;
}

/* [ADD] 좌우 화살표 버튼 */
.seg-arrow {
  padding: 6px 10px;
  font-size: 14px;
  line-height: 1;
  color: #374151;
  background: #fff;
  border: 0;
  cursor: pointer;
}
.seg-arrow:disabled {
  opacity: .45;
  cursor: not-allowed;
}
.seg-arrow + .seg-btn,
.seg-btn + .seg-arrow {
  border-left: 1px solid #e5e7eb;
}

.chart-area { min-height: 260px; position: relative; }
.chart-empty {
  min-height: 220px;
  display: grid;
  place-items: center;
  color: #9ca3af;
  font-size: 13px;
  border: 1px dashed #e5e7eb;
  border-radius: 12px;
}
.linechart { width: 100%; height: 260px; display: block; }
/* [/ADD: Sales Chart] */

/* 하단 안내/바로가기 */
.hotel-label {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
}
.quick-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

</style>

<script setup lang="js">
import { ref, watch, onMounted, computed } from 'vue'
import { api } from '@/lib/api'

/**
 * 사용법 요약
 * - 기본값: 주간 모드(mode='week'), 내부 화살표 내비게이션(nav=true) 활성화
 * - 주간 모드일 때 ‹ › 버튼으로 지난주/다음주 이동
 * - 월간으로 쓰고 싶으면 mode='month' 로 지정(이때 화살표 숨김)
 * - 부모에서 from/to를 강제로 넘기고 싶으면 nav=false 로 주면 됨(전달한 기간 그대로 사용)
 */
const props = defineProps({
  hotelId: { type: Number, required: true },

  // 외부에서 기간을 직접 주고 싶을 때 사용 (nav=false일 때만 사용됨)
  from:    { type: String, default: '' }, // 'YYYY-MM-DD'
  to:      { type: String, default: '' },

  // 'week' | 'month'
  mode:    { type: String, default: 'week' },

  // 주간 네비게이션(‹ ›) 사용 여부. true면 컴포넌트가 주간 범위를 직접 관리함
  nav:     { type: Boolean, default: true },

  // (선택) 초기 기준 날짜. 미지정 시 오늘
  initialAnchor: { type: String, default: '' }, // 'YYYY-MM-DD'
})

const loading = ref(false)
const err = ref('')
const rows = ref([]) // [{ name, rate, bookedNights, allotmentNights }]

// ===================== 날짜 유틸 =====================
const pad2 = (n) => String(n).padStart(2, '0')
const fmtYMD = (d) => `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())}`
const parseYMD = (s) => {
  if (!s) return null
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!m) return null
  const dt = new Date(Number(m[1]), Number(m[2])-1, Number(m[3]))
  return isNaN(dt.getTime()) ? null : dt
}
const mondayOf = (d) => {
  const t = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const day = t.getDay() || 7 // Sun=0 → 7
  if (day !== 1) t.setDate(t.getDate() - (day - 1))
  return t
}
const weekRangeOf = (anchorDate) => {
  const start = mondayOf(anchorDate)
  const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 6)
  return { start, end }
}
const monthStart = (d) => new Date(d.getFullYear(), d.getMonth(), 1)
const monthEnd   = (d) => new Date(d.getFullYear(), d.getMonth()+1, 0)

// ===================== 주간 내비게이션 상태 =====================
const anchor = ref(() => {
  const p = parseYMD(props.initialAnchor)
  return p ?? new Date()
})
// computed: 현재 표시 기간(from/to)
const range = computed(() => {
  // nav=true & mode=week 이면 내부에서 기간 관리
  if (props.nav && props.mode === 'week') {
    const { start, end } = weekRangeOf(anchor.value instanceof Function ? anchor.value() : anchor.value)
    return { from: fmtYMD(start), to: fmtYMD(end) }
  }

  // nav=false 이거나 month 모드면, 부모가 준 from/to 사용(월 모드는 부모가 한달 범위를 넘겨줄 수도 있음)
  return { from: props.from, to: props.to }
})

// 버튼 활성/비활성(미래 주로 이동 방지)
const canNext = computed(() => {
  if (!(props.nav && props.mode === 'week')) return false
  const cur = weekRangeOf(new Date())
  const nowStart = cur.start
  const curStart = weekRangeOf(anchor.value instanceof Function ? anchor.value() : anchor.value).start
  return curStart < nowStart
})
function shiftPrev() {
  if (!(props.nav && props.mode === 'week')) return
  const dt = anchor.value instanceof Function ? anchor.value() : new Date(anchor.value)
  dt.setDate(dt.getDate() - 7)
  anchor.value = dt
  loadData()
}
function shiftNext() {
  if (!(props.nav && props.mode === 'week')) return
  if (!canNext.value) return
  const dt = anchor.value instanceof Function ? anchor.value() : new Date(anchor.value)
  dt.setDate(dt.getDate() + 7)
  anchor.value = dt
  loadData()
}

// ===================== 데이터 정규화/로드 =====================
function normalize(items) {
  if (!Array.isArray(items)) return []
  return items.map((it, i) => {
    const name = it.roomTypeName ?? it.name ?? `타입${i+1}`
    let rate = it.occupancyRate
    if (rate == null) {
      const booked = Number(it.bookedNights ?? it.booked ?? 0)
      const allot  = Number(it.allotmentNights ?? it.allotment ?? 0)
      rate = allot > 0 ? (booked / allot) * 100 : 0
    }
    return {
      name,
      rate: Math.max(0, Math.min(100, Number(rate))),
      bookedNights: Number(it.bookedNights ?? it.booked ?? 0),
      allotmentNights: Number(it.allotmentNights ?? it.allotment ?? 0)
    }
  }).sort((a, b) => b.rate - a.rate)
}

async function loadData() {
  if (!props.hotelId) return

  // 유효 기간 확보
  const from = range.value.from
  const to   = range.value.to
  if (!from || !to) return

  loading.value = true
  err.value = ''
  try {
    const res = await api(
      `/api/owner/hotels/${props.hotelId}/analytics/room-type-occupancy?from=${from}&to=${to}`
    )
    if (!res.ok) throw new Error(await res.text())
    const raw = await res.json()
    rows.value = normalize(raw)
  } catch (e) {
    err.value = e?.message || '데이터 로드 실패'
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
watch(() => [props.hotelId, props.mode, props.nav, props.from, props.to], loadData)

// 포맷터
const fmtPct = (v) => `${Math.round(v)}%`
const fmtNum = (v) => new Intl.NumberFormat('ko-KR').format(Number(v || 0))
</script>

<template>
  <div class="rt-occu card">
    <div class="head">
      <div class="title">
        <h3>객실 타입별 예약 점유율</h3>
        <span class="muted">
          기간: {{ range.from }} ~ {{ range.to }}
        </span>
      </div>

      <!-- 주간 네비게이션 -->
      <div v-if="nav && mode === 'week'" class="segmented with-arrows">
        <button class="seg-arrow" @click="shiftPrev" aria-label="이전 주">‹</button>
        <div class="seg-label">주간</div>
        <button class="seg-arrow" :disabled="!canNext" @click="shiftNext" aria-label="다음 주">›</button>
      </div>
    </div>

    <div v-if="loading" class="empty">불러오는 중…</div>
    <div v-else-if="err" class="empty">{{ err }}</div>
    <div v-else-if="!rows.length" class="empty">데이터가 없습니다.</div>

    <div v-else class="bars">
      <div v-for="(r, i) in rows" :key="i" class="bar-row">
        <div class="label">
          <div class="name">{{ r.name }}</div>
        </div>
        <div class="bar-wrap" :title="`${r.name} · ${fmtPct(r.rate)}`">
          <div class="bar" :style="{ width: r.rate + '%' }"></div>
        </div>
        <div class="val">{{ fmtPct(r.rate) }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rt-occu.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 14px;
}
.head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; gap: 8px; }
.title { display: flex; gap: 8px; align-items: baseline; }
.title h3 { font-size: 14px; font-weight: 800; }
.muted { color: #9ca3af; font-size: 12px; }

.segmented.with-arrows {
  display: inline-flex;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}
.seg-arrow {
  padding: 6px 10px;
  font-size: 14px;
  line-height: 1;
  color: #374151;
  background: #fff;
  border: 0;
  cursor: pointer;
}
.seg-arrow:disabled { opacity: .45; cursor: not-allowed; }
.seg-label { padding: 6px 12px; font-size: 12px; color: #374151; border-left: 1px solid #e5e7eb; border-right: 1px solid #e5e7eb; }

.empty {
  min-height: 140px; display: grid; place-items: center;
  color: #9ca3af; font-size: 13px; border: 1px dashed #e5e7eb; border-radius: 12px;
}

.bars { display: grid; gap: 12px; }
.bar-row {
  display: grid; grid-template-columns: 1.4fr 1fr 70px;
  align-items: center; gap: 12px;
}
.label .name { font-size: 13px; color: #111827; font-weight: 700; }
.label .sub  { font-size: 12px; color: #6b7280; }
.bar-wrap { background: #f3f4f6; height: 12px; border-radius: 999px; overflow: hidden; }
.bar { height: 100%; background: #111827; }
.val { text-align: right; font-weight: 800; font-size: 12px; color: #111827; }
@media (max-width: 560px) {
  .bar-row { grid-template-columns: 1fr; }
  .val { text-align: left; }
}
</style>

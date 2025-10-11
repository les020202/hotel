<!-- src/views/admin/Bookings.vue -->
<template>
  <section class="wrap wrap--wide">
    <!-- 헤더 -->
    <div class="hero">
      <div>
        <h2>예약 관리</h2>
        <p>필터로 상태/호텔/기간을 빠르게 조회하세요.</p>
      </div>
    </div>

    <!-- 필터 -->
    <form class="toolbar" @submit.prevent="reload(0)">
      <div class="pill">
        <label class="lbl">상태</label>
        <select v-model="q.status">
          <option :value="null">전체</option>
          <option value="CONFIRMED">예약 확정</option>
          <option value="CANCELLED">예약 취소</option>
          <option value="PENDING">대기 중</option>
        </select>
      </div>

      <div class="pill grow">
        <label class="lbl">호텔 이름</label>
        <input v-model.trim="q.hotelName" placeholder="예: 콘래드, 신라, 제주…" />
      </div>

      <div class="pill">
        <label class="lbl">로그인ID</label>
        <input v-model.trim="q.loginId" placeholder="user 검색" />
      </div>

      <div class="pill">
        <label class="lbl">체크인 From</label>
        <input v-model="q.from" type="date" />
      </div>

      <div class="pill">
        <label class="lbl">체크인 To</label>
        <input v-model="q.to" type="date" />
      </div>

      <div class="pills-right">
        <button type="submit" class="btn primary">검색</button>
        <button type="button" @click="resetFilters" class="btn ghost">초기화</button>
      </div>
    </form>

    <!-- 표 -->
    <div class="card table-card no-h-scroll">
      <table class="table">
        <!-- 열 폭 고정: 칸 흔들림 방지 -->
        <colgroup>
          <col style="width:90px" />   <!-- 예약번호 -->
          <col style="width:180px" />  <!-- 고객 -->
          <col style="width:200px" />  <!-- 호텔 -->
          <col style="width:160px" />  <!-- 객실 -->
          <col style="width:130px" />  <!-- 체크인 -->
          <col style="width:80px" />   <!-- 박수 -->
          <col style="width:150px" />  <!-- 금액 -->
          <col style="width:120px" />  <!-- 상태 -->
          <col style="width:100px" />  <!-- 영수증 -->
          <col style="width:100px" />  <!-- 관리 -->
        </colgroup>

        <thead>
          <tr>
            <th class="th-left">예약번호</th>
            <th class="th-left">고객</th>
            <th class="th-left">호텔</th>
            <th class="th-left">객실</th>
            <th class="th-center">체크인</th>
            <th class="th-center">박수</th>
            <th class="th-right">금액</th>
            <th class="th-center">상태</th>
            <th class="th-center">영수증</th>
            <th class="th-center">관리</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="b in bookings" :key="b.bookingId">
            <td class="td-left">#{{ b.bookingId }}</td>
            <td class="td-left">
              <div class="b ellipsis">{{ b.userName || '-' }}</div>
              <div class="muted text-xs ellipsis">{{ b.userLoginId }}</div>
            </td>
            <td class="td-left ellipsis">{{ b.hotelName }}</td>
            <td class="td-left ellipsis">{{ b.roomTypeName }}</td>
            <td class="td-center">{{ b.checkIn || '-' }}</td>
            <td class="td-center">{{ b.nights ?? '-' }}</td>
            <td class="td-right nowrap">{{ nfmt(b.totalAmount) }} {{ b.currency }}</td>
            <td class="td-center">
              <span :class="badgeClass(b.status)">{{ statusLabel(b.status) }}</span>
            </td>
            <td class="td-center">
              <a v-if="b.receiptUrl" :href="b.receiptUrl" target="_blank" rel="noopener" class="link">보기</a>
              <span v-else class="muted">-</span>
            </td>
            <td class="td-center">
              <button
                class="btn danger xs"
                :disabled="!canCancel(b) || loading"
                @click="openCancel(b.bookingId)"
              >취소</button>
            </td>
          </tr>

          <tr v-if="!loading && !bookings.length">
            <td colspan="10" class="empty">데이터가 없습니다.</td>
          </tr>
          <tr v-if="loading">
            <td colspan="10" class="loading">불러오는 중…</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="pager">
      <div class="muted">총 {{ totalElements.toLocaleString() }}건</div>
      <div class="controls">
        <button class="btn ghost" :disabled="page<=0 || loading" @click="reload(page-1)">이전</button>
        <span class="muted">페이지 {{ page+1 }} / {{ totalPages }}</span>
        <button class="btn ghost" :disabled="page>=totalPages-1 || loading" @click="reload(page+1)">다음</button>
        <select v-model.number="size" @change="reload(0)" class="sel">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </div>
    </div>

    <p v-if="errorMsg" class="err">{{ errorMsg }}</p>

    <!-- 취소 모달 -->
    <CancelDialog :open="cancelOpen" @close="cancelOpen=false" @submit="submitCancel" />
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/api/_http'
import { cancelBooking } from '@/api/bookings'
import CancelDialog from '@/components/common/CancelDialog.vue'

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

const statusLabel = (s) => {
  const map = {
    CONFIRMED: '예약 확정',
    PENDING:   '대기 중',
    CANCELLED: '예약 취소',
  }
  return map[s] ?? s
}

const nfmt = (n) => (n == null ? '-' : Number(n).toLocaleString('ko-KR'))

const badgeClass = (s) => [
  'badge',
  s === 'CONFIRMED' ? 'ok' :
  s === 'CANCELLED' ? 'no' : 'warn'
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
    const res = await get(`/admin/bookings?${query}`)
    bookings.value = Array.isArray(res?.content) ? res.content : []
    page.value = Number(res?.number ?? nextPage)
    size.value = Number(res?.size ?? size.value)
    totalPages.value = Number(res?.totalPages ?? 0)
    totalElements.value = Number(res?.totalElements ?? 0)
  } catch (e) {
    console.error(e)
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

function canCancel(b) {
  if (!b || b.status === 'CANCELLED') return false
  if (!b.checkIn) return true
  const today = new Date()
  const t0 = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const ci  = new Date(b.checkIn)
  return t0 <= ci
}

onMounted(() => reload(0))
</script>

<style scoped>
:root{ --line:#e8ecf6; --card:#fff; --muted:#6b7280; --ink:#111827; }

/* 레이아웃 */
.wrap{ padding:14px }
.wrap--wide{ width:100% !important; max-width:none !important; margin:0 !important; }

/* 헤더 */
.hero{
  display:flex; align-items:center; justify-content:space-between;
  padding:16px 18px; border-radius:16px;
  background:linear-gradient(135deg,#f7faff,#f0f6ff);
  border:1px solid #eaf0ff; margin-bottom:12px;
}
.hero h2{ margin:0; font-size:18px; font-weight:800; color:var(--ink) }
.hero p{ margin:4px 0 0; color:#6b7280; font-size:12px }

/* 툴바 */
.toolbar{
  display:grid; grid-template-columns: repeat(6, minmax(0,1fr));
  gap:10px; align-items:end; margin-bottom:10px; flex-wrap:wrap;
}
.pill{
  display:flex; flex-direction:column; gap:6px; padding:10px; border:1px solid var(--line);
  background:#fff; border-radius:12px;
}
.pill.grow{ grid-column: span 2 / span 2; }
@media (max-width: 980px){
  .toolbar{ grid-template-columns: 1fr 1fr; }
  .pill.grow{ grid-column: span 2 / span 2; }
}
.lbl{ font-size:12px; color:#6b7280 }
.pill input, .pill select{
  height:36px; border:1px solid #e1e8f5; border-radius:10px; padding:0 10px; outline:none;
}
.pill input:focus, .pill select:focus{
  box-shadow:0 0 0 3px rgba(37,99,235,.12); border-color:#cfe0ff
}
.pills-right{ display:flex; gap:8px; align-items:center }

/* 버튼 */
.btn{
  height:36px; padding:0 14px; border-radius:10px; font-weight:800; cursor:pointer; border:1px solid #cfe0ff;
  background:#f5f9ff;
}
.btn.primary{ color:#fff; background:linear-gradient(135deg,#3b82f6,#2563eb); border-color:transparent; box-shadow:0 8px 20px rgba(37,99,235,.25) }
.btn.ghost{ background:#fff; color:#0f172a }
.btn.danger{ color:#b91c1c; background:#fff5f5; border-color:#fecaca }
.btn.danger:disabled{ opacity:.5; }
.btn.xs{ height:28px; font-size:12px; border-radius:10px }

/* 카드/테이블 */
.card{ background:#fff; border:1px solid var(--line); border-radius:12px }
.table-card{ overflow:auto }
.no-h-scroll{ overflow-x:hidden }

/* 테이블: 칸 정렬 안정화 */
.table{
  width:100%;
  border-collapse:collapse;
  table-layout:fixed; /* 열폭 고정 */
}
th,td{
  padding:12px;
  border-bottom:1px solid #f1f4fb;
  font-size:14px;
  vertical-align:middle; /* 가운데 정렬 */
  line-height:1.25;
}
th{
  color:#475569; font-weight:800; background:#fbfdff;
}

/* 헤더/본문 정렬 클래스 */
.th-left{ text-align:left }
.th-center{ text-align:center }
.th-right{ text-align:right }
.td-left{ text-align:left }
.td-center{ text-align:center }
.td-right{ text-align:right }

/* 글자 줄바꿈/말줄임 */
th, th *{ white-space:nowrap; word-break:keep-all }
td{ white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
.nowrap{ white-space:nowrap }
.ellipsis{ overflow:hidden; text-overflow:ellipsis; white-space:nowrap }

.b{ font-weight:700 } .muted{ color:#6b7280 } .text-xs{ font-size:12px }
.link{ color:#2563eb; text-decoration:underline }

/* 상태 배지 */
.badge{
  display:inline-flex; align-items:center; justify-content:center;
  min-width:72px; /* 글자 길이 달라도 칸 맞춤 */
  padding:4px 10px;
  border-radius:999px; font-size:12px; font-weight:800;
  border:1px solid #e5e7eb; background:#f9fafb;
}
.badge.ok{ background:#ecfdf5; border-color:#a7f3d0; color:#065f46 }
.badge.no{ background:#fef2f2; border-color:#fecaca; color:#991b1b }
.badge.warn{ background:#fff7ed; border-color:#fed7aa; color:#92400e }

/* 빈 상태/로딩 */
.empty{ text-align:center; color:#94a3b8; padding:18px 0 }
.loading{ text-align:center; color:#94a3b8; padding:18px 0 }

/* 페이지네이션 */
.pager{ display:flex; align-items:center; justify-content:space-between; margin-top:10px }
.controls{ display:flex; align-items:center; gap:8px }
.sel{ height:32px; border:1px solid #e1e8f5; border-radius:10px; padding:0 8px }
.err{ color:#e11d48; font-size:13px; margin-top:6px }
</style>

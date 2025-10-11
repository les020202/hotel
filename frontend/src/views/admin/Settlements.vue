<!-- src/views/admin/settlement.vue -->
<template>
  <div class="settlement-page" ref="pageEl">
<!-- ====== 주간 선택 바 (상단 고정) ====== -->
<section class="card week-bar" aria-label="정산 주 선택">
  <div class="week-row">
    <!-- 좌측: 지난 주 -->
    <button class="icon-btn chevron" @click="shiftWeek(-1)" aria-label="지난 주">‹</button>

    <!-- 가운데: 기간 -->
    <div class="week-range" aria-live="polite">
      <strong>{{ start }} ~ {{ end }}</strong>
    </div>

    <!-- 우측: 다음 주 -->
    <button class="icon-btn chevron" @click="shiftWeek(1)" aria-label="다음 주">›</button>

    <!-- 가장 오른쪽: 액션 -->
    <div class="week-actions">
      <button class="btn ghost" @click="shiftWeek(0)">이번 주</button>
      
      <button class="btn outline" :disabled="!lines?.length" @click="downloadCsv">CSV</button>
    </div>
  </div>
</section>
<!-- ====== KPI 카드 ====== -->
<section class="card kpi" aria-label="주간 KPI">
  <div class="kpi-grid kpi-grid--2x4">
    <!-- 1 -->
    <div class="kpi-card">
     <h3>총 정산서</h3>
<p>{{ statusCountsGlobal.ALL.toLocaleString() }} 건</p>
    </div>
    <!-- 2 -->
    <div class="kpi-card">
      <h3>총액(Gross)</h3>
      <p>{{ won(headerTotals.gross) }}</p>
    </div>
    <!-- 3 -->
 <!-- 수수료 -->
<div class="kpi-card kpi-card--fee">
  <h3>수수료</h3>
  <p class="big">{{ won(headerTotals.fee) }}</p>
  <ul class="mini">
    <li>Gross 대비 <b>{{ feeRate.toFixed(1) }}%</b></li>
    <li v-if="deltaFee !== null">
      지난주 대비
      <b :class="deltaFee>0 ? 'up' : deltaFee<0 ? 'down' : ''">
        {{ signed(deltaFee) }}%
      </b>
    </li>
  </ul>
</div>

<!-- 지급액(Net) -->
<div class="kpi-card kpi-card--net">
  <h3>지급액(Net)</h3>
  <p class="big">{{ won(headerTotals.net) }}</p>
  <p class="sub" v-if="deltaNet !== null">
    지난주 대비
    <b :class="deltaNet>0 ? 'up' : deltaNet<0 ? 'down' : ''">
      {{ signed(deltaNet) }}%
    </b>
  </p>
</div>



 <div class="kpi-card">
  <!-- 정산 상태 도넛 -->
<!-- ✅ 올바른 사용 -->
<Statusdonut
  :size="160"
  :settled="statusCountsGlobal.SETTLED"
  :planned="statusCountsGlobal.PLANNED"
  :nulled="statusCountsGlobal.NULL"
  :summary-filter="summaryFilter"
  ring-color="var(--border)"
  settled-color="var(--ok-600)"       
  planned-color="var(--accent-600)"   
  null-color="var(--danger-600)"      
  @toggle="v => { summaryFilter = (summaryFilter === v ? 'ALL' : v) }"
/>




</div>

    

    <!-- 7: 이상 변동 -->
    <div class="kpi-card">
      <h3>이상 변동</h3>
      <ul class="alert-list" v-if="kpi.alerts?.length">
        <li class="alert-item" v-for="a in kpi.alerts" :key="a.hotelId" @click="onFilterByHotel(a)">
          {{ a.hotelName }} ({{ a.deltaPct > 0 ? '+' : '' }}{{ a.deltaPct }}%)
        </li>
      </ul>
      <small v-else class="muted">특이사항 없음</small>
    </div>

    
  </div>
  <div class="kpi-foot">
  업데이트 {{ kpi.asOf }} · 활성 호텔 {{ kpi.coverage.activeHotels }}/{{ kpi.coverage.totalHotels }}
</div>

</section>
<div class="omnibox">
  <!-- 검색창 -->
  <div class="input-wrap">
    <input
      v-model.trim="hotelName"
      type="text"
      placeholder="호텔명을 입력하세요"
      autocomplete="off"
      @input="onHotelNameInput"
      @focus="onHotelNameInput"
      @keydown.enter.prevent="hotelSuggests[0] && selectHotel(hotelSuggests[0])"
      @keydown.esc="hotelSuggests = []"
    />
    <!-- 🔹 클리어 버튼 (검색창 안 X 표시) -->
    <button
      v-if="hotelId || hotelName"
      class="clear-btn"
      @click="clearHotel"
      aria-label="선택 해제"
      title="전체 보기로 돌아가기"
    >×</button>

    <!-- 자동완성 목록 -->
    <ul v-if="hotelSuggests.length" class="typeahead">
      <li
        v-for="h in hotelSuggests"
        :key="h.id"
        @click="selectHotel(h)"
        class="opt"
      >
        {{ h.name }} <span class="muted">(#{{ h.id }})</span>
      </li>
    </ul>
  </div>

  <!-- 조회 버튼 -->
  <button class="btn" :disabled="isQuerying" @click="queryAll">
        조회
  </button>

  <!-- 🔹 전체 보기 버튼 -->
  <button class="btn outline" :disabled="isQuerying" @click="clearHotel">
    전체 보기
  </button>
</div>






    <!-- ====== 탭 ====== -->
    <nav class="tabs card" aria-label="보기 전환">
      <button :class="['tab', tab==='summary' && 'active']" :aria-pressed="tab==='summary'" @click="switchTab('summary')">요약(호텔별)</button>
      <button :class="['tab', tab==='lines' && 'active']"   :aria-pressed="tab==='lines'"   @click="switchTab('lines')">라인(상세)</button>
      <button :class="['tab', tab==='statements' && 'active']" :aria-pressed="tab==='statements'" @click="switchTab('statements')">정산서</button>
      <button :class="['tab', tab==='settled' && 'active']" :aria-pressed="tab==='settled'" @click="switchTab('settled')">완료</button>

   <div class="status-filter">
  <label for="statusFilter">상태:</label>
  <select id="statusFilter" v-model="summaryFilter">
    <option value="ALL">모두 ({{ statusCountsGlobal.ALL }})</option>
    <option value="NULL">정산미완료 ({{ statusCountsGlobal.NULL }})</option>
    <option value="PLANNED">정산예정 ({{ statusCountsGlobal.PLANNED }})</option>
    <option value="SETTLED">정산완료 ({{ statusCountsGlobal.SETTLED }})</option>
  </select>
</div>

    </nav>

    <!-- ====== 본문 ====== -->
    <section class="card body" aria-live="polite">
      <div v-if="pageError" class="alert error" role="alert">
        <p>데이터를 불러오지 못했습니다. 다시 시도해주세요.</p>
        <button class="btn xs outline" @click="queryAll" aria-label="다시 시도">재시도</button>
      </div>

      <!-- 요약 -->
      <div v-if="tab==='summary'">
        <TableSkeleton v-if="loading.summary" />
        <template v-else>
          <EmptyState v-if="!summary?.length" text="해당 기간 데이터가 없습니다. 기간을 변경하거나 ‘이번 주’를 눌러보세요." />
          <div v-else>
           <div class="table-wrap">
  <table class="table" aria-label="호텔별 요약">
    <!-- ✅ 열 폭 고정 -->
    <colgroup>
      <col style="width:32%">  <!-- 호텔 -->
      <col style="width:10%">  <!-- 예약(건) -->
      <col style="width:15%">  <!-- 총액 -->
      <col style="width:12%">  <!-- 할인 -->
      <col style="width:12%">  <!-- 수수료 -->
      <col style="width:15%">  <!-- 지급액 -->
      <col style="width:14%">  <!-- 상태 -->
    </colgroup>

    <thead>
      <tr>
        <th>호텔</th>
        <th class="tr">예약<span class="th-unit"> (건)</span></th>
        <th class="tr">총액(Gross)</th>
        <th class="tr">할인</th>
        <th class="tr">수수료</th>
        <th class="tr">지급액(Net)</th>
        <th>상태</th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="s in summaryView"
        :key="s.hotelId"
        class="clickable"
        role="button"
        tabindex="0"
        @click.stop.prevent="onSummaryRowClick(s)"
        @keydown.enter.prevent="onSummaryRowClick(s)"
        @keydown.space.prevent="onSummaryRowClick(s)"
      >
        <td data-label="호텔">{{ s.hotelName }} <span class="muted">(#{{ s.hotelId }})</span></td>
        <td class="tr" data-label="예약(건)">{{ s.bookingCount?.toLocaleString() }}</td>
        <td class="tr" data-label="총액(Gross)">{{ won(s.grossSum) }}</td>
        <td class="tr" data-label="할인">{{ won(s.discountSum) }}</td>
        <td class="tr" data-label="수수료">{{ won(s.feeSum) }}</td>
        <td class="tr strong" data-label="지급액(Net)">{{ won(s.netSum) }}</td>
        <td data-label="상태">
          <span v-if="statusMap[s.hotelId]==='SETTLED'" class="badge ok">정산완료</span>
          <span v-else-if="statusMap[s.hotelId]==='PLANNED'" class="badge plan">정산예정</span>
          <span v-else class="badge">정산미완료</span>
        </td>
      </tr>
    </tbody>
  </table>
</div>

          </div>
        </template>
      </div>

      <!-- 라인 -->
      <div v-else-if="tab==='lines'">
        <div class="row">
          <div class="buttons right">
            <button class="btn" :disabled="!hotelId || loading.gen" @click="quickGenerateFromLines">
              {{ loading.gen ? '생성 중...' : '정산서 생성' }}
            </button>
          </div>
        </div>

        <TableSkeleton v-if="loading.lines" />
        <template v-else>
          <EmptyState v-if="!lines?.length" text="라인 데이터가 없습니다. 조건을 변경해 보세요." />
          <div v-else class="table-wrap">
            <table class="table" aria-label="라인 상세">
              <thead>
                <tr>
                  <th class="hide-sm">호텔</th>
                  <th>객실타입</th>
                  <th class="tr hide-sm">예약<span class="th-unit"> (건)</span></th>
                  <th class="tr">총액(Gross)</th>
                  <th class="tr hide-md">할인</th>
                  <th class="tr hide-md">수수료</th>
                  <th class="tr">지급액(Net)</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(r,i) in lines" :key="i">
                  <td class="hide-sm" data-label="호텔">{{ r.hotelName }} <span class="muted">(#{{ r.hotelId }})</span></td>
                  <td data-label="객실타입">{{ r.roomTypeName }} <span class="muted">( #{{ r.roomTypeId }} )</span></td>
                  <td class="tr hide-sm" data-label="예약(건)">{{ r.bookingCount?.toLocaleString() }}</td>
                  <td class="tr" data-label="총액(Gross)">{{ won(r.gross) }}</td>
                  <td class="tr hide-md" data-label="할인">{{ won(r.discount) }}</td>
                  <td class="tr hide-md" data-label="수수료">{{ won(r.fee) }}</td>
                  <td class="tr strong" data-label="지급액(Net)">{{ won(r.net) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>

      <!-- 정산서 -->
      <div v-else-if="tab==='statements'" id="statements-section">
        <div class="row gap" aria-label="정산서 생성 및 조회">
          <div class="payout">
            <div class="field">
              <label for="bankCode">은행코드</label>
              <input id="bankCode" v-model="bankCode" placeholder="예: 004" inputmode="numeric" @input="onlyDigits('bankCode')" />
              <small class="help">숫자 3자리</small>
            </div>
            <div class="field">
              <label for="accountNo">계좌번호</label>
              <input id="accountNo" v-model="accountNo" placeholder="숫자와 하이픈" @input="formatAccount()" />
              <small class="help">숫자/하이픈만 허용</small>
            </div>
            <div class="field">
              <label for="holderName">예금주</label>
              <input id="holderName" v-model="holderName" placeholder="예: (주)호텔스냅" />
            </div>
          </div>
        </div>

        <!-- 선택 호텔 표시 -->
        <div class="row">
          <strong>선택 호텔:</strong>&nbsp;
          <span>{{ hotelName || '미선택' }}</span>
          <span v-if="hotelId" class="muted">&nbsp;(#{{ hotelId }})</span>
        </div>

        <TableSkeleton v-if="loading.statements" />
        <template v-else>
          <EmptyState v-if="!hotelId" text="호텔을 선택하면 해당 호텔의 정산서가 표시됩니다." />
          <EmptyState v-else-if="!statements?.length" text="정산서가 이미 생성되었습니다. ‘완료’를 눌러 확인해보세요." />
          <div v-else class="table-wrap">
            <table class="table" aria-label="정산서 목록">
              <thead>
                <tr>
                  <th class="hide-md">ID</th>
                  <th>기간</th>
                  <th>상태</th>
                  <th class="tr">지급금액</th>
                  <th class="hide-sm">지급계좌</th>
                  <th class="hide-sm">생성</th>
                  <th>확정</th>
                  <th>동작</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in statements" :key="s.id">
                  <td class="hide-md" data-label="호텔">{{ hotelMap[s.hotelId] ?? ('#'+s.hotelId) }}</td>
                  <td data-label="기간">{{ s.periodStart }} ~ {{ s.periodEnd }}</td>
                  <td data-label="상태"><span :class="['badge', s.status==='SETTLED' ? 'ok' : 'plan']">{{ s.status }}</span></td>
                  <td class="tr strong" data-label="지급금액">{{ won(s.payableAmount) }}</td>
                  <td class="hide-sm" data-label="계좌">{{ s.payoutBankCode }} / {{ s.payoutAccountNo }} / {{ s.payoutHolderName }}</td>
                  <td class="hide-sm" data-label="생성">{{ dt(s.createdAt) }}</td>
                  <td data-label="확정">{{ s.settledAt ? dt(s.settledAt) : '-' }}</td>
                  <td data-label="동작">
                    <button class="btn xs" :disabled="s.status==='SETTLED'" @click="settle(s.id)" :aria-label="`정산서 #${s.id} 확정`">확정</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>

      <!-- 완료 -->
      <div v-else>
        <TableSkeleton v-if="loading.settled" />
        <template v-else>
          <EmptyState
            v-if="!settled?.length"
            :text="hotelId ? '이 호텔의 완료된 정산서가 없습니다.' : '완료된 정산서가 없습니다.'"
          />
          <div v-else class="table-wrap">
            <table class="table" aria-label="완료된 정산서 목록">
              <thead>
                <tr>
                  <th class="hide-md">ID</th>
                  <th>기간</th>
                  <th>상태</th>
                  <th class="tr">지급금액</th>
                  <th class="hide-sm">지급계좌</th>
                  <th class="hide-sm">생성</th>
                  <th>확정</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in settled" :key="s.id">
                  <td class="hide-md" data-label="호텔">{{ hotelMap[s.hotelId] ?? ('#'+s.hotelId) }}</td>
                  <td data-label="기간">{{ s.periodStart }} ~ {{ s.periodEnd }}</td>
                  <td data-label="상태"><span class="badge ok">SETTLED</span></td>
                  <td class="tr strong" data-label="지급금액">{{ won(s.payableAmount) }}</td>
                  <td class="hide-sm" data-label="계좌">
                    {{ s.payoutBankCode }} / {{ s.payoutAccountNo }} / {{ s.payoutHolderName }}
                  </td>
                  <td class="hide-sm" data-label="생성">{{ dt(s.createdAt) }}</td>
                  <td data-label="확정">{{ s.settledAt ? dt(s.settledAt) : '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </section>

    <!-- 토스트 -->
    <div class="toast" v-for="t in toasts" :key="t.id">{{ t.msg }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, reactive, computed, h, defineComponent } from 'vue'
import Statusdonut from '@/components/admin/Statusdonut.vue'
const pageEl = ref(null)
/* ===== 상태 ===== */
const tab = ref('summary')

/** 검색 상태(표/탭 전용 로컬 필터) */
const hotelId    = ref(null)
const hotelName  = ref('')
const hotelSuggests = ref([])
const hotelMap = ref({})
const roomTypeId = ref(null)

/** 주간 범위 */
const start = ref(mondayISO(new Date()))
const end   = ref(sundayISO(new Date()))

/** 지급 정보 */
const bankCode   = ref('012')
const accountNo  = ref('123-456-789012')
const holderName = ref('(주)hotelsMagic')

/** 표 데이터(로컬: 필터 반영) */
const summary    = ref([])
const lines      = ref([])
const statements = ref([])
const settled    = ref([])

/** KPI용 전역 데이터(필터 무시) */
const summaryGlobal    = ref([])
const statementsGlobal = ref([])
const settledGlobal    = ref([])

/** 상태 필터/맵 (표 전용) */
const statusMap = ref({})          // { [hotelId]: 'PLANNED' | 'SETTLED' | null }
const summaryFilter = ref('ALL')   // 'ALL' | 'NULL' | 'PLANNED' | 'SETTLED'

/** 표: 필터된 뷰 & 개수 */
const summaryView = computed(() => {
  const base = summary.value || []
  switch (summaryFilter.value) {
    case 'NULL':     return base.filter(r => !statusMap.value[r.hotelId])
    case 'PLANNED':  return base.filter(r => statusMap.value[r.hotelId] === 'PLANNED')
    case 'SETTLED':  return base.filter(r => statusMap.value[r.hotelId] === 'SETTLED')
    default:         return base
  }
})
const statusCounts = computed(() => {
  const res = { ALL: summary.value?.length || 0, NULL: 0, PLANNED: 0, SETTLED: 0 }
  for (const r of (summary.value || [])) {
    const st = statusMap.value[r.hotelId]
    if (!st) res.NULL++
    else res[st]++
  }
  return res
})
const pctRatio = (a,b) => (b ? (a/b)*100 : 0)
const round1 = n => Math.round(n*10)/10
const signed  = n => (n>0?'+':'') + round1(n)

const feeRate = computed(() => pctRatio(headerTotals.value.fee, headerTotals.value.gross))

 const deltaFee = computed(() => {
  const cur = kpi.thisWeek?.fee ?? 0
  const prev = kpi.lastWeek?.fee ?? 0
  // diffPct가 아래쪽에 있어 호이스팅 이슈를 피하려고 식을 직접 넣었어요.
  return prev ? round1(((cur - prev) / prev) * 100) : null
})
/** KPI(전역 고정) */
const headerTotals = computed(() => totals(summaryGlobal.value || []))

/** 전역 정산 상태 집계(KPI용) */
const statusCountsGlobal = computed(() => {
  const all = new Set((summaryGlobal.value || []).map(r => r.hotelId))
  const settledSet = new Set((settledGlobal.value || []).map(s => s.hotelId))
  const plannedSet = new Set(
    (statementsGlobal.value || [])
      .filter(s => !settledSet.has(s.hotelId))
      .map(s => s.hotelId)
  )
  let settledN = 0, plannedN = 0, nullN = 0
  all.forEach(hid => {
    if (settledSet.has(hid)) settledN++
    else if (plannedSet.has(hid)) plannedN++
    else nullN++
  })
  return { ALL: all.size, NULL: nullN, PLANNED: plannedN, SETTLED: settledN }
})
// 비율(Gross 대비 %) 계산
const ratio = (a, b) => (b ? (a / b) * 100 : 0)

// 이번주 Net vs 지난주 Net 증감(%), 소수 1자리
  const deltaNet = computed(() => {
  const cur = kpi.thisWeek?.net ?? 0
  const prev = kpi.lastWeek?.net ?? 0
  return prev ? round1(((cur - prev) / prev) * 100) : null
})

/** KPI 데이터(상단 글로벌) */
const kpi = reactive({
  thisWeek: { bookings:0, gross:0, net:0, fee:0, spark:[] },
  lastWeek: { bookings:0, gross:0, net:0, fee:0, spark:[] },
  funnel:   { created:0, reviewing:0, settled:0 },
  alerts:   [],
  coverage: { activeHotels:0, totalHotels:0 },
  asOf:     ''
})

const loading   = reactive({ summary:false, lines:false, statements:false, settled:false, gen:false })
const pageError = ref('')
const isQuerying = computed(()=> loading.summary || loading.lines || loading.statements || loading.settled)

/* ===== 유틸 ===== */
function toISODate(d){ return new Date(d.getTime() - d.getTimezoneOffset()*60000).toISOString().slice(0,10) }
function mondayISO(now){ const d=new Date(now); const day=d.getDay()||7; if(day!==1)d.setDate(d.getDate()-(day-1)); return toISODate(d) }
function sundayISO(now){ const d=new Date(now); const day=d.getDay()||7; d.setDate(d.getDate()+(7-day)); return toISODate(d) }
const won = (v) => (v ?? 0).toLocaleString('ko-KR') + '원'
const dt  = (s) => s ? s.replace('T',' ').slice(0,19) : '-'
const nowText = () => new Date().toLocaleString()

function qs(obj){
  const p = new URLSearchParams()
  Object.entries(obj).forEach(([k,v])=>{ if(v!==undefined && v!==null && v!=='') p.append(k,v) })
  return p.toString()
}
onMounted(() => {
  // 브라우저/OS마다 다른 스크롤바 폭 계산해서 이 뷰에만 넣어줌(폴백용)
  const sbw = window.innerWidth - document.documentElement.clientWidth
  pageEl.value?.style.setProperty('--sbw', sbw + 'px')
})
async function api(url, opts={}, key){
  try{
    if(key) loading[key]=true
    pageError.value=''
    const res = await fetch(url, { headers:{'Content-Type':'application/json'}, ...opts })
    const text = await res.text()
    if(!res.ok) throw new Error(text || `HTTP ${res.status}`)
    return text ? JSON.parse(text) : null
  }catch(e){ console.error(e); pageError.value=e?.message||'서버 오류'; return null }
  finally{ if(key) loading[key]=false }
}
// ===== 더미 지급정보 유틸 (호텔명 기반, 일관성 유지) =====
const BANK_CODES = ['004','011','020','088','090','081','071','003','005','023','027']
// 간단한 시드 RNG
function seededRng(str='dummy'){
  let h = 0
  for (let i=0; i<str.length; i++){ h = (h<<5) - h + str.charCodeAt(i); h |= 0 }
  return () => { h ^= h<<13; h ^= h>>>17; h ^= h<<5; return (h>>>0)/0xFFFFFFFF }
}
const zpad = (n,len) => String(n).padStart(len,'0')

function makeDummyPayout(hotelName){
  const name = (hotelName || '').trim() || 'HOTEL'
  const rnd = seededRng(name)
  const bankCode = BANK_CODES[Math.floor(rnd()*BANK_CODES.length)]
  const a1 = zpad(Math.floor(rnd()*1000), 3)
  const a2 = zpad(Math.floor(rnd()*10000), 4)
  const a3 = zpad(Math.floor(rnd()*1000000), 6)
  return {
    bankCode,
    accountNo: `${a1}-${a2}-${a3}`,
    holderName: name   // ✅ 예금주 = 호텔명
  }
}
async function loadPayoutDefault(){
  if(!hotelId.value && !hotelName.value) return
  // 1) 서버 시도
  try{
    if (hotelId.value){
      const r = await api(`/api/settlements/hotels/${hotelId.value}/payout`)
      if(r && (r.bankCode || r.accountNo || r.holderName)){
        if (!bankCode.value)   bankCode.value   = r.bankCode   ?? bankCode.value
        if (!accountNo.value)  accountNo.value  = r.accountNo  ?? accountNo.value
        if (!holderName.value) holderName.value = r.holderName ?? holderName.value
        return
      }
    }
  }catch{/* 무시하고 더미로 진행 */}

  // 2) 서버에 없으면 더미
  const d = makeDummyPayout(hotelName.value)
  if (!bankCode.value)   bankCode.value   = d.bankCode
  if (!accountNo.value)  accountNo.value  = d.accountNo
  if (!holderName.value) holderName.value = d.holderName
}


/* ===== 토스트 ===== */
const toasts = ref([])
function toast(msg){
  const id = Math.random().toString(36).slice(2)
  toasts.value.push({id, msg})
  setTimeout(()=>{ toasts.value = toasts.value.filter(t=>t.id!==id) }, 1800)
}

/* ===== 호텔 자동완성 ===== */

let suggestTimer
async function onHotelNameInput(){
  clearTimeout(suggestTimer)
  hotelId.value = null

  const keyword = hotelName.value?.trim()
  if(!keyword){ hotelSuggests.value=[]; return }

  suggestTimer = setTimeout(async ()=>{
    try{
      const raw = await api('/api/settlements/hotels/search?' + qs({ q: keyword }))
      // 응답이 배열이든 {content:[]}든 안전하게 처리
      const arr = Array.isArray(raw) ? raw : (raw?.content ?? [])
      hotelSuggests.value = arr.slice(0, 10)
    }catch(e){
      console.error('검색 실패', e)
      hotelSuggests.value=[]
    }
  }, 220)
}


function selectHotel(h){
  hotelName.value = h.name
  hotelId.value   = h.id
  hotelSuggests.value = []
  loadPayoutDefault()
   tab.value = 'lines'
  nextTick().then(loadLines)
}


/* ===== 주간 이동 ===== */
function setWeek(baseDate){
  start.value = mondayISO(baseDate)
  end.value   = sundayISO(baseDate)
}
function shiftWeek(step){
  if(step===0){ setWeek(new Date()); queryAll(); return }
  const s = new Date(start.value); s.setDate(s.getDate()+7*step)
  setWeek(s); queryAll()
}

/* ===== 상태 프로빙(표 전용) ===== */
async function probeStatuses(list){
  const hotelIds = (list || []).map(r => Number(r.hotelId))
  statusMap.value = {}
  const chunks = (arr, n = 8) => Array.from({ length: Math.ceil(arr.length / n) }, (_, i) => arr.slice(i * n, (i + 1) * n))
  for (const chunk of chunks(hotelIds, 8)){
    await Promise.all(chunk.map(async (hid) => {
      const done = await api('/api/settlements/statements?' + qs({ hotelId: hid, start: start.value, end: end.value, status: 'SETTLED' }))
      if (Array.isArray(done) && done.length){
        statusMap.value[hid] = 'SETTLED'; return
      }
      const planned = await api('/api/settlements/statements?' + qs({ hotelId: hid, start: start.value, end: end.value, status: 'PLANNED' }))
      statusMap.value[hid] = (Array.isArray(planned) && planned.length) ? 'PLANNED' : null
    }))
  }
}

/* ===== API (표/로컬) ===== */
async function loadSummary(range='this'){
  const url = `/api/settlements/summary?`+qs({
    hotelId: hotelId.value || undefined,   // 상태필터와 무관하게, 선택된 호텔이 있을 때만 전달✅ ALL일 때 hotelId 제거
    roomTypeId: roomTypeId.value || undefined,
    start: range==='this' ? start.value : prevWeek(start.value),
    end:   range==='this' ? end.value   : prevWeek(end.value)
  })

  const list = await api(url, {}, 'summary')
  if(range==='this') {
    summary.value = list ?? []
    await probeStatuses(summary.value)
  }
  return list ?? []
}
async function loadLines(){
  const url = `/api/settlements/items?`+qs({
    hotelId: hotelId.value||undefined,
    roomTypeId: roomTypeId.value||undefined,
    start:start.value, end:end.value
  })
  lines.value = await api(url, {}, 'lines') ?? []
}
async function loadStatements(){
  if(!hotelId.value){ statements.value = []; return }
  if(!start.value || !end.value){ statements.value=[]; return }
  const url = `/api/settlements/statements?` + qs({
    hotelId: hotelId.value, start: start.value, end: end.value, status: 'PLANNED'
  })
  statements.value = await api(url, {}, 'statements') ?? []
}
async function loadSettled(){
  if(!start.value || !end.value){ settled.value=[]; return }
  const query = { start: start.value, end: end.value, status: 'SETTLED' }
  if(hotelId.value){ query.hotelId = hotelId.value }
  const url = `/api/settlements/statements?` + qs(query)
  settled.value = await api(url, {}, 'settled') ?? []
}

/* ===== API (전역/KPI) ===== */
async function loadSummaryGlobal(range='this'){
  const url = `/api/settlements/summary?` + qs({
    start: range==='this' ? start.value : prevWeek(start.value),
    end:   range==='this' ? end.value   : prevWeek(end.value)
  })
  const list = await api(url, {}, 'summary')
  if(range==='this') summaryGlobal.value = list ?? []
  return list ?? []
}
async function loadStatementsGlobal(){
  if(!start.value || !end.value){ statementsGlobal.value=[]; return }
  const url = `/api/settlements/statements?` + qs({
    start: start.value, end: end.value, status: 'PLANNED'
  })
  statementsGlobal.value = await api(url, {}, 'statements') ?? []
}
async function loadSettledGlobal(){
  if(!start.value || !end.value){ settledGlobal.value=[]; return }
  const url = `/api/settlements/statements?` + qs({
    start: start.value, end: end.value, status: 'SETTLED'
  })
  settledGlobal.value = await api(url, {}, 'settled') ?? []
}

const canGenerate = computed(() =>
  !!(hotelId.value && start.value && end.value && bankCode.value && accountNo.value && holderName.value)
)

/* 라인 탭에서 원클릭 생성 + 정산서 탭 이동 */
async function quickGenerateFromLines(){
  if(!hotelId.value){ toast('호텔을 먼저 선택하세요.'); return }
  try{
    loading.gen = true
    if(!bankCode.value || !accountNo.value || !holderName.value){
      await loadPayoutDefault()
    }
    const body = {
      hotelId:   hotelId.value,
      start:     start.value,
      end:       end.value,
      bankCode:  bankCode.value,
      accountNo: accountNo.value,
      holderName:holderName.value
    }
    await api('/api/settlements/generate', { method:'POST', body: JSON.stringify(body) })
    await goToStatements()
    toast('정산서를 생성했습니다.')
  } finally {
    loading.gen = false
  }
}

async function generateWeekly(){
  if(!canGenerate.value){ toast('호텔/기간/계좌 정보를 확인해주세요.'); return }
  try{
    loading.gen = true
    const body = {
      hotelId:   hotelId.value,
      start:     start.value,
      end:       end.value,
      bankCode:  bankCode.value,
      accountNo: accountNo.value,
      holderName:holderName.value
    }
    await api('/api/settlements/generate', { method:'POST', body: JSON.stringify(body) })
    await goToStatements()
    toast('정산서가 생성되었습니다.')
  } finally {
    loading.gen = false
  }
}

async function settle(id){
  try{
    const res = await fetch(`/api/settlements/${id}/settle`, {
      method:'POST',
      headers:{ 'Content-Type':'application/json' }
    })
    if(!res.ok){
      const txt = await res.text()
          let msg = '정산서 확정 실패'
    try { 
  const obj = JSON.parse(txt)
  msg = obj.message || obj.error || msg   // 우선순위 반대로
} catch { 
  if (txt) msg = txt 
}

     toast(msg)  // 컷오프 전이면 "정산 확정은 10월 8일 이후 가능합니다." 그대로 뜸
      return                   // ⛔ 실패 시 여기서 종료 → 아래 UI 갱신 금지
    }
  }catch(e){
    toast('정산서 확정 실패: ' + (e?.message || '네트워크 오류'))
    return
  }

  // ✅ 성공한 경우에만 UI 반영
  statements.value = (statements.value || []).filter(s => s.id !== id)
  await Promise.all([loadSettled(), refreshKpi()])
  tab.value = 'settled'
  await nextTick()
  document.querySelector('.table-wrap')?.scrollIntoView({ behavior:'smooth', block:'start' })
  toast('정산서 확정 완료! 완료 탭으로 이동했어요.')
}


/* ===== KPI 계산 ===== */
function totals(list){
  const t = { bookings:0, gross:0, net:0, fee:0 }
  for(const r of list){
    t.bookings += r.bookingCount ?? 0
    t.gross    += r.grossSum ?? r.gross ?? 0
    t.net      += r.netSum   ?? r.net   ?? 0
    t.fee      += r.feeSum   ?? r.fee   ?? 0
  }
  return t
}
function mapByHotel(list){
  const m = new Map()
  list.forEach(r => m.set(String(r.hotelId), r))
  return m
}
const diffPct = (cur, prev) => (prev ? ((cur - prev) / prev) * 100 : 0)  // 증감율
function prevWeek(iso){ const d=new Date(iso); d.setDate(d.getDate()-7); return toISODate(d) }

async function refreshKpi(){
  const [thisList, lastList] = await Promise.all([loadSummaryGlobal('this'), loadSummaryGlobal('last')])
  await Promise.all([loadSettledGlobal(), loadStatementsGlobal()])

  const T = totals(thisList), L = totals(lastList)
  const spark = thisList.map(x => x.netSum || 0).slice(0, 12)

  const funnel = {
    created: (statementsGlobal.value?.length || 0),
    settled: (settledGlobal.value?.length || 0)
  }

  const mThis = mapByHotel(thisList), mLast = mapByHotel(lastList)
  const deltas = []
  mThis.forEach((v,k)=>{
    const cur = v.netSum ?? 0
    const prev = mLast.get(k)?.netSum ?? 0
    const d = diffPct(cur, prev)
    if(Math.abs(d) >= 30 && (cur>0 || prev>0)){
      deltas.push({ hotelId:Number(k), hotelName:v.hotelName, deltaPct: Math.round(d*10)/10 })
    }
  })
  deltas.sort((a,b)=>Math.abs(b.deltaPct)-Math.abs(a.deltaPct))

  const activeHotels = new Set(thisList.map(r=>r.hotelId)).size
  const totalHotels  = new Set([...thisList, ...lastList].map(r=>r.hotelId)).size

  kpi.thisWeek = { ...T, spark }
  kpi.lastWeek = { ...L, spark: [] }
  kpi.funnel   = funnel
  kpi.alerts   = deltas.slice(0,3)
  kpi.coverage = { activeHotels, totalHotels }
  kpi.asOf     = nowText()
}
/* 전체 데이터 조회 */
async function queryAll(){
  // 이름만 입력되어 있고, 정확히 1건이면 id 매핑
  // 이름만 입력되어 있고, 정확히 1건이면 id 매핑
if(!hotelId.value && hotelName.value?.trim()){
  const raw = await api('/api/settlements/hotels/search?' + qs({ q: hotelName.value.trim() }))
  const arr = Array.isArray(raw) ? raw : (raw?.content ?? [])
  if(arr.length === 1){
    hotelId.value   = arr[0].id
    hotelName.value = arr[0].name
    await loadPayoutDefault()
  }
}


  await Promise.all([
    loadSummary('this'),
    loadLines(),
    loadStatements(),
    loadSettled(),
    loadSummaryGlobal('this'),
    loadStatementsGlobal(),
    loadSettledGlobal()
  ])

  await refreshKpi()
  toast('데이터를 새로고침했어요.')
}

async function onSummaryRowClick(s){
  // 상태 먼저 세팅
  hotelId.value   = Number(s.hotelId)
  hotelName.value = s.hotelName

  // ✅ 먼저 라인 탭으로 전환
  tab.value = 'lines'
  await nextTick()

  // ✅ 라인 데이터를 바로 로드
  await loadLines()

  // 나머지(요약/정산서/KPI)는 백그라운드 느낌으로 갱신
  // (필요 없으면 아래 두 줄은 빼도 됨)
  loadStatements()
  refreshKpi()
}







/* 탭 이동 시 자동 로딩 */
function switchTab(to){
  tab.value = to
  if(to === 'statements') loadStatements()
  if(to === 'settled')    loadSettled()
  if(to === 'lines')      loadLines()
  if(to === 'summary')    loadSummary('this')
}

/* 라인 → 정산서로 이동 */
async function goToStatements(){
  tab.value = 'statements'
  await nextTick()
  await Promise.all([loadStatements(), loadPayoutDefault()])
  document.getElementById('statements-section')?.scrollIntoView({ behavior:'smooth', block:'start' })
}

/* 호텔 바뀌면 현재 탭 데이터 재조회(표 전용) */
watch(hotelId, () => {
  if(tab.value === 'lines'      && start.value && end.value) loadLines()
  if(tab.value === 'statements') loadStatements()
  if(tab.value === 'settled')    loadSettled()
})

onMounted(async () => {
  // 전체 호텔 불러오기 → hotelMap에 저장
  try {
    const list = await api('/api/settlements/hotels/search?q=')
    const arr = Array.isArray(list) ? list : (list?.content ?? [])
    hotelMap.value = Object.fromEntries((arr || []).map(h => [h.id, h.name]))
  } catch (e) { console.error(e) }

  await queryAll()
})



/* KPI 이벤트 */
function onFilterByHotel(h){
  hotelId.value   = h.hotelId
  hotelName.value = h.hotelName
  tab.value = 'summary'
  queryAll()
}

/* 폼 보조 */
function onlyDigits(which){
  if(which === 'bankCode'){
    bankCode.value = (bankCode.value || '').replace(/\D+/g,'').slice(0,3)
  }
}
function formatAccount(){
  const raw = (accountNo.value || '').replace(/[^\d-]+/g,'')
  accountNo.value = raw
}

/* CSV */
function downloadCsv(){
  if(!lines.value?.length){ toast('다운로드할 라인 데이터가 없습니다.'); return }
  const header=['Hotel','RoomType','Booking','Gross','Discount','Fee','Net']
  const rows = lines.value.map(r=>[
    `${r.hotelName} (#${r.hotelId})`,
    `${r.roomTypeName} (#${r.roomTypeId})`,
    r.bookingCount,
    r.gross, r.discount, r.fee, r.net
  ])
  const csv = [header, ...rows]
    .map(a => a.map(x => `"${String(x ?? '').replace(/"/g,'""')}"`).join(','))
    .join('\n')
  const blob = new Blob([csv], { type:'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `settlement_lines_${start.value}_${end.value}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
}

/* ===== 경량 컴포넌트 ===== */
const EmptyState = defineComponent({
  name: 'EmptyState',
  props: { text: { type: String, default: '' } },
  setup(props){
    return () => h('div',{class:'empty'},[ h('p', props.text || '데이터가 없습니다.') ])
  }
})
const TableSkeleton = defineComponent({
  name: 'TableSkeleton',
  setup(){
    const Row = () => h('div',{class:'sk-row'},[
      h('div',{class:'sk-col w40'}),
      h('div',{class:'sk-col w10'}),
      h('div',{class:'sk-col w10'}),
      h('div',{class:'sk-col w10 hide-sm'}),
      h('div',{class:'sk-col w10 hide-md'})
    ])
    return () => h('div',{class:'skeleton'}, Array.from({length:4}).map(() => Row()))
  }
})

</script>

<style scoped>

/* =========================
   THEME TOKENS
========================= */
.settlement-page{
   --bg:#f7f9fc;
   --surface:#ffffff;
   --surface-2:#f9fbff;
   --border:#e5eaf2;
   --text:#0f172a;
   --muted:#6b7280;
   --accent-600:#1d4ed8;
   --accent-700:#1e40af;
   --danger-600:#ef4444;
   --danger-700:#dc2626;
   --ok-600:#16a34a;      /* 완료(초록) */
   --ring:0 0 0 3px rgba(37,99,235,.18);
   --ctrl-h:44px;
     overflow-x: clip;                /* 넘친 걸 잘라서 상단 KPI가 스크롤바에 겹치지 않게 */
  scrollbar-gutter: stable both-edges; /* 스크롤바 자리 고정 -> 레이아웃 흔들림 방지 */
 }
 


/* =========================
   PAGE / CARD
========================= */
.settlement-page{
  background:var(--bg);
  color:var(--text);
  min-height:100vh;
  padding:24px;
  font-variant-numeric:tabular-nums;
}
.card{
  background:var(--surface);
  border:1px solid var(--border);
  border-radius:16px;
  padding:20px;
  margin-bottom:20px;
  box-shadow:0 6px 20px rgba(2,8,23,.06);
}

/* KPI 상단 고정 */
.kpi-sticky{
  position:sticky;
  top:12px;
  z-index:5;
}

/* =========================
   CONTROLS
========================= */
.card.controls{ padding:20px; }
.controls .controls-grid{
  display:grid;
  grid-template-columns:1fr 1fr auto;
  gap:14px;
  align-items:end;
}
.field{ position:relative; display:flex; flex-direction:column; gap:6px; }
.field label{ font-size:12px; font-weight:700; color:var(--muted); letter-spacing:.02em; }
.field .help{ font-size:11.5px; color:#7b8794; }

.controls .input-wrap{ position:relative; }
.controls input{
  height:var(--ctrl-h);
  background:#fff;
  border:1px solid var(--border);
  border-radius:10px;
  padding:8px 12px;
  font-size:14px;
  color:var(--text);
  outline:0;
}
.controls input:focus-visible{ box-shadow:var(--ring); border-color:var(--accent-600); }
.controls .clear{
  position:absolute; right:8px; top:50%; transform:translateY(-50%);
  border:0; background:transparent; font-size:18px; line-height:1;
  cursor:pointer; color:#94a3b8;
}
.controls .clear:hover{ color:#0f172a; }

/* 자동완성 */
.controls .typeahead{
  position:absolute; top: calc(var(--ctrl-h) + 8px); left:0; right:0;
  background:#fff; border:1px solid var(--border); border-radius:10px;
  max-height:240px; overflow:auto; z-index:10;
}
.controls .opt{ padding:10px 12px; cursor:pointer; }
.controls .opt:hover{ background:#eef4ff; }

/* 주간 스위치 */
.wk-ctrl{ display:flex; align-items:center; gap:8px; }
.icon-btn{
  width:var(--ctrl-h); height:var(--ctrl-h);
  display:inline-flex; align-items:center; justify-content:center;
  border:1px solid var(--border);
  border-radius:10px; background:#fff; cursor:pointer; font-size:18px;
}
.icon-btn:hover{ background:#f8fbff; }
.wk-range{ min-width:260px; text-align:center; padding:0 8px; font-weight:800; }
.btn.ghost{
  height:var(--ctrl-h);
  background:#f1f5ff; color:var(--accent-600);
  border:1px solid #cfe0ff;
}

/* =========================
   BUTTONS
========================= */
.btn{
  border:1px solid transparent;
  background:var(--accent-600);
  color:#fff;
  padding:0 14px;
  height:var(--ctrl-h);
  border-radius:10px;
  font-weight:800;
  cursor:pointer;
  display:inline-flex; align-items:center; justify-content:center;
  white-space:nowrap;
  transition:transform .06s ease, background .15s ease, box-shadow .15s ease;
}
.btn:hover{ background:var(--accent-700); transform:translateY(-1px); }
.btn:disabled{ opacity:.6; cursor:not-allowed; transform:none; }
.btn:focus-visible{ box-shadow:var(--ring); }

.btn.outline{
  background:#fff;
  color:var(--accent-600);
  border-color:var(--accent-600);
}
.btn.outline:hover{ background:#f0f4ff; }
.btn.xs{ height:28px; padding:0 10px; font-size:12px; }

.buttons{ display:flex; gap:10px; align-items:center; }
.buttons.right{ margin-left:auto; }

.note.tip{ margin-top:6px; color:var(--muted); font-size:13px; }

/* =========================
   TABS
========================= */
.tabs{
  display:flex; align-items:center; gap:8px; flex-wrap:wrap;
  padding:12px; background:var(--surface); border:1px solid var(--border);
  border-radius:16px;
}
.tab{
  padding:10px 12px; border-radius:10px; background:#f1f5ff;
  border:1px solid #dbe5ff; color:#103072; font-weight:800; cursor:pointer;
}
.tab.active{
  background:#e0ecff; border-color:#bcd3ff; color:var(--accent-600);
  box-shadow:inset 0 0 0 2px rgba(37,99,235,.18);
}
.tab:focus-visible{ box-shadow:var(--ring); }
.status-filter{ margin-left:auto; display:flex; align-items:center; gap:8px; }
.status-filter label{ font-size:13px; color:var(--muted); font-weight:700; }
.status-filter select{
  padding:6px 10px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; font-size:14px;
}
.status-filter select:focus-visible{ box-shadow:var(--ring); outline:none; }

/* =========================
   TABLES
========================= */
.table-wrap{ max-height:60vh; overflow:auto; border:1px solid var(--border); border-radius:14px; }
.table{ width:100%; border-collapse:separate; border-spacing:0; font-size:14px; }
.table thead th{
  position:sticky; top:0; z-index:1;
  background:#eef3ff; color:#0b1220; font-weight:900;
  border-bottom:1px solid var(--border);
  padding:12px 12px;
}
.table td{ padding:12px 12px; border-top:1px solid var(--border); vertical-align:middle; }
.table tbody tr:nth-child(even){ background:var(--surface-2); }
.table tbody tr:hover{ background:#eef4ff; }
.th-unit{ font-weight:600; color:var(--muted); }
.tr{ text-align:right; }
.strong{ font-weight:900; }
.clickable{ cursor:pointer; }
.clickable:focus-visible{ outline:0; box-shadow:inset 0 0 0 2px var(--accent-600); }

/* =========================
   BADGES
========================= */
.badge{
  display:inline-flex; align-items:center; gap:6px;
  padding:4px 10px; border-radius:999px; font-size:12px; font-weight:800;
  background:#eef5ff; color:#1d4ed8; border:1px solid #cfe0ff;
}
.badge.ok{ background:#e6f7ef; color:#0f5132; border-color:#b7e4c7; }
.badge.plan{ background:#eef5ff; color:#0b3ea1; border-color:#cfe0ff; }

/* =========================
   ALERT / EMPTY / SKELETON
========================= */
.alert.error{
  display:flex; gap:10px; align-items:center;
  background:#fff1f2; color:#7f1d1d; border:1px solid #fecdd3;
  border-radius:12px; padding:12px; margin-bottom:12px;
}
.empty{ text-align:center; color:#64748b; padding:20px 8px; }
.skeleton{ display:flex; flex-direction:column; gap:10px; padding:8px 0; }
.sk-row{ display:grid; grid-template-columns:1fr 120px 120px 120px 120px; gap:10px; }
.sk-col{ height:14px; border-radius:8px; background:linear-gradient(90deg,#eef2ff,#f8faff,#eef2ff); background-size:200% 100%; animation:shimmer 1.2s ease-in-out infinite; }
.w40{ grid-column:1/2; } .w10{ grid-column:auto; }
@keyframes shimmer{ 0%{background-position:0% 0} 100%{background-position:200% 0} }

/* =========================
   KPI CARDS
========================= */
.kpi-cards{
  display:grid;
  grid-template-columns:repeat(auto-fit, minmax(180px, 1fr));
  gap:16px;
}
.kpi-card{
  background:var(--surface-2);
  border:1px solid var(--border);
  border-radius:12px;
  padding:16px;
  text-align:center;
  box-shadow:0 4px 10px rgba(2,8,23,.05);
}
.kpi-card h3{
  font-size:13px;
  color:#667085;
  margin-bottom:8px;
  font-weight:800;
}
.kpi-card p{
  font-size:18px;
  font-weight:900;
  color:var(--text);
}

/* 진행률 */
.progress .bar{
  width:100%; height:10px; border-radius:999px;
  background:#e7efff; overflow:hidden; border:1px solid #dbe5ff;
}
.progress .fill{ height:100%; background:var(--accent-600); }

/* 알람 리스트 */
.alert-list{ display:flex; flex-direction:column; gap:6px; }
.alert-item{ cursor:pointer; font-size:13px; padding:6px 8px; border-radius:8px; }
.alert-item:hover{ background:#eef5ff; }

/* TOAST */
.toast{
  position:fixed; left:50%; bottom:24px; transform:translateX(-50%);
  background:#111827; color:#fff; padding:10px 14px; border-radius:10px; opacity:.95; z-index:9999;
}

/* RESPONSIVE */
.hide-sm{ display:table-cell; }
.hide-md{ display:table-cell; }

@media (max-width:1024px){
  .controls .controls-grid{ grid-template-columns:1fr 1fr auto; }
  .buttons.right{ grid-column:1 / -1; justify-content:flex-end; }
  .table-wrap{ max-height:none; }
}
@media (max-width:960px){
  .kpi-sticky{ position:static; }
  .controls .controls-grid{ grid-template-columns:1fr; }
  .actions{ justify-content:stretch; }
  .actions .btn{ flex:1; }
  .hide-md{ display:none; }
}
@media (max-width:640px){
  .status-filter{ width:100%; margin-left:0; justify-content:flex-end; }
  .tab{ font-size:13px; }
  .hide-sm{ display:none; }
}
/* ===== KPI 상단 주 선택 바 ===== */
.kpi .kpi-top{
  display:flex; align-items:center; gap:12px;
  justify-content:space-between;
  margin-bottom:14px;
  padding-bottom:10px;
  border-bottom:1px dashed var(--border);
}
.kpi .wk-ctrl{ display:flex; align-items:center; gap:8px; }
.kpi .wk-range{ min-width:280px; text-align:center; padding:0 8px; font-weight:800; }
.kpi .kpi-actions{ display:flex; gap:8px; }

/* controls는 호텔만 남겨 더 단순하게 */
.controls .controls-grid{
  grid-template-columns:1fr; /* 호텔만 한 줄 */
}
/* ===== 주간 선택 바 ===== */
.week-bar .week-row{
  display:grid;
  grid-template-columns: auto 1fr auto auto; /* ⟨  기간  ⟩  액션 */
  align-items:center;
  gap:10px;
}
.week-bar .chevron{
  width:40px; height:40px;
  border-radius:10px;
  border:1px solid var(--border);
  background:#fff;
  font-size:20px; line-height:1;
  display:flex; align-items:center; justify-content:center;
  cursor:pointer;
}
.week-bar .chevron:hover{ background:#f5f8ff; }
.week-bar .week-range{
  text-align:center; font-weight:900; letter-spacing:.02em;
}
.week-bar .week-range strong{ font-size:18px; }
.week-bar .week-actions{
  justify-self:end;
  display:flex; align-items:center; gap:8px;
}

/* ===== KPI 카드 ===== */
.kpi .kpi-grid{
  display:grid;
  grid-template-columns:repeat(auto-fit, minmax(220px, 1fr));
  gap:16px;
}
.kpi .kpi-card{
  background:#fff;
  border:1px solid var(--border);
  border-radius:14px;
  padding:16px;
  text-align:center;
  box-shadow:0 6px 18px rgba(2,8,23,.05);
}
.kpi .kpi-card h3{
  font-size:13px; color:#6b7280; font-weight:800; margin-bottom:6px;
}
.kpi .kpi-card p{
  font-size:22px; font-weight:900; margin:0;
}

/* 진행률 */
.progress .bar{
  width:100%; height:10px; border-radius:999px;
  background:#e7efff; overflow:hidden; border:1px solid #dbe5ff; margin-bottom:6px;
}
.progress .fill{ height:100%; background:var(--accent-600); }

/* 이상 변동 리스트 */
.alert-list{ display:flex; flex-direction:column; gap:6px; }
.alert-item{ cursor:pointer; font-size:13px; padding:6px 8px; border-radius:8px; }
.alert-item:hover{ background:#eef5ff; }
/* 2×4 고정 그리드 */
/* 베이스: 열 개수 정의하지 말고 공통 속성만 */
.kpi .kpi-grid{
  display:grid;
  gap:16px;
}

/* 2×4 고정 (특이성 ↑) */
.kpi .kpi-grid.kpi-grid--2x4{
  grid-template-columns: repeat(4, minmax(220px, 1fr));
}

/* 반응형 */
@media (max-width: 1024px){
  .kpi .kpi-grid.kpi-grid--2x4{ grid-template-columns: repeat(2, minmax(220px,1fr)); }
}
@media (max-width: 640px){
  .kpi .kpi-grid.kpi-grid--2x4{ grid-template-columns: 1fr; }
}

/* 반응형: 태블릿 2칸, 모바일 1칸 */
@media (max-width: 1024px){
  .kpi-grid--2x4{ grid-template-columns: repeat(2, minmax(220px,1fr)); }
}
@media (max-width: 640px){
  .kpi-grid--2x4{ grid-template-columns: 1fr; }
}
.kpi .kpi-card{
  background:#fff;
  border:1px solid var(--border);
  border-radius:14px;
  padding:16px;
  text-align:center;
  box-shadow:0 6px 18px rgba(2,8,23,.05);

  /* ✅ 컨테이너 쿼리 활성화 + 잘림 방지 */
  container-type: inline-size;
  overflow: hidden;
}
.donut-card{
  display:grid;
  gap:16px;
  align-items:center;
  /* 기본: 1열(도넛 위, 범례 아래) */
  grid-template-columns: 1fr;
}

/* ✅ 카드(컨테이너) 폭이 520px 이상이면 2열로 전환 */
@container (min-width: 520px){
  .donut-card{
    grid-template-columns: auto 180px;
  }
}

.chart-wrap{ display:flex; align-items:center; justify-content:center; }

/* 도넛 사이즈가 과하면 눌려 보이니 카드 폭에 맞춰 clamp */
.donut{ max-width: 100%; height: auto; }
.ring{ fill:none; stroke: var(--border, #e5eaf2); stroke-width: 18; }

.center .total{ font-size: 18px; font-weight: 800; fill: var(--text, #0f172a); }
.center .label{ font-size: 11px; fill: var(--muted, #6b7280); }

.legend{
  list-style:none; padding:0; margin:0;
  display:flex; flex-direction:column; gap:8px;
}

/* 컨테이너가 좁으면 범례를 가로로 칩처럼 */
@container (max-width: 519px){
  .legend{ flex-direction: row; flex-wrap: wrap; }
}

.li{
  display:flex; align-items:center; gap:10px;
  border:1px solid var(--border, #e5eaf2);
  border-radius:10px; padding:6px 8px; cursor:pointer;
  user-select:none; font-size:12px;
}
.li:hover{ background:#f8fbff; }
.li.active{ box-shadow: 0 0 0 3px rgba(37,99,235,.12) inset; border-color:#cfe0ff; }

.dot{ width:10px; height:10px; border-radius:50%; flex:0 0 10px; }
.name{ font-weight:700; color: var(--text, #0f172a); }
.val{ margin-left:auto; font-variant-numeric: tabular-nums; color: #334155; }
/* settlement.vue <style scoped>에 추가/수정 */

/* 이 뷰 자체를 스크롤 컨테이너로 만든다 → 세로 스크롤 생겨도 그리드가 안 튐 */
.settlement-page{
  overflow-y: auto;              /* 세로 스크롤은 여기서만 */
  overflow-x: clip;              /* 가로 넘침은 잘라서 페이지 전체 가로 스크롤 금지 */
  scrollbar-gutter: stable both-edges; /* 최신 브라우저: 세로 스크롤 공간 항상 예약 */

  /* 구형 브라우저 폴백: 우측 패딩에 스크롤바 폭만큼 여유(이미 24px 패딩 있으니 합산) */
  padding-right: max(24px, calc(24px + var(--sbw, 0px)));
}

/* 표는 내부에서만 스크롤되게(페이지 가로 스크롤 유발 금지) */
.table-wrap{
  overflow: auto;     /* 가로/세로 내부 스크롤 */
  max-width: 100%;
}
.table{
  width: max(100%, 1024px);  /* 넓으면 내부 가로 스크롤, 좁으면 100% */
}

/* KPI 그리드가 경계에서 1~2px 때문에 넘치지 않도록 자동 줄바꿈 */
.kpi .kpi-grid.kpi-grid--2x4{
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}
.kpi .kpi-card{ min-width: 0; } /* 카드 내용이 칸을 밀어내지 않게 */
/* 지급액(Net) 카드 보조 지표 */
.kpi-card--net .big{ margin-bottom:8px; }
.kpi-card--net .mini{
  display:flex; justify-content:center; gap:12px; flex-wrap:wrap;
  font-size:12px; color:#64748b;
}
.kpi-card--net .mini b{ font-weight:800; color:#0f172a; }
.kpi-card--net .mini b.up{ color: var(--ok-600); }      /* +초록 */
.kpi-card--net .mini b.down{ color: var(--danger-600); } /* -빨강 */
/* KPI 하단 각주 */
.kpi-foot{
  margin-top:6px;
  text-align:right;
  font-size:12px;
  color:#6b7280;
}
/* KPI 카드 공통: 같은 높이 + 중앙 정렬 */
.kpi .kpi-card{
  min-height: 180px;
  display:flex; flex-direction:column; align-items:center; justify-content:center;
  text-align:center;
}

/* 숫자/보조지표 리듬 */
.kpi .kpi-card .big { font-size:22px; font-weight:900; margin:4px 0 6px; }
.kpi .kpi-card .sub { font-size:12px; color:#64748b; }
.kpi .kpi-card .sub b { color:#0f172a; }

/* 수수료 보조지표 2줄 */
.kpi-card--fee .mini{
  display:flex; gap:12px; flex-wrap:wrap; justify-content:center;
  font-size:12px; color:#64748b; margin-top:4px;
}
.kpi-card--fee .mini b{ font-weight:800; color:#0f172a; }

/* 증감 색상 */
.up{ color: var(--ok-600); }
.down{ color: var(--danger-600); }
.controls--omnibox input {
  width: 100%;
  height: 42px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  background: #fff;
  color: var(--text);
}
.controls--omnibox input:focus-visible {
  box-shadow: var(--ring);
  border-color: var(--accent-600);
}





.omnibox {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  position: relative;
  width: 100%;
}

.input-wrap {
  position: relative;
  flex: 1;
}

.input-wrap input {
  width: 100%;
  height: var(--ctrl-h, 40px);
  border: 1px solid var(--border, #ddd);
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
}

.input-wrap input:focus-visible {
  box-shadow: var(--ring, 0 0 0 2px #2563eb33);
  border-color: var(--accent-600, #2563eb);
}

/* 자동완성 목록 */
.typeahead {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin: 4px 0 0;
  padding: 4px 0;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 6px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.typeahead .opt {
  padding: 6px 10px;
  cursor: pointer;
  font-size: 14px;
}

.typeahead .opt:hover {
  background: #f1f5f9;
}

.muted {
  color: #94a3b8;
  font-size: 12px;
  margin-left: 4px;
}
table {
  table-layout: fixed;
  width: 100%;
}
th, td {
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
.card table {
  margin: 0 -8px;   /* 패딩 만큼 당겨줌 */
  width: calc(100% + 16px);
}
table {
  table-layout: fixed;
  width: 100%;
  border-collapse: collapse;
}

th, td {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  padding: 8px 12px;
}

td.tr, th.tr {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
/* ===== 테이블 렌더 안정화 ===== */
.table {
  table-layout: fixed;                    /* 열 폭 고정 */
  width: max(100%, 1024px);               /* 가로 스크롤은 .table-wrap에서 처리 */
  border-collapse: separate;
  border-spacing: 0;
}

.table th,
.table td {
  white-space: nowrap;                    /* 줄바꿈 금지 */
  overflow: hidden;                       /* 넘치면 숨김 */
  text-overflow: ellipsis;                /* 말줄임 … */
  vertical-align: middle;
  line-height: 1.35;                      /* 글자 클리핑 방지 */
}

.table th.tr,
.table td.tr {
  text-align: right;
  font-variant-numeric: tabular-nums;     /* 숫자 고정폭 → 정렬 깔끔 */
}

/* 상태 배지 넘침 방지 */
.table td .badge {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 스크롤 컨테이너와 머리글 정렬 안정화 */
.table-wrap {
  overflow: auto;
  max-width: 100%;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

/* 헤더가 스크롤바에 밀리지 않게(필요 시) */
.table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
}
.table { table-layout: fixed; }
.table th, .table td { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.table th.tr, .table td.tr { text-align: right; font-variant-numeric: tabular-nums; }
.input-wrap {
  position: relative;
}
.input-wrap .clear-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  border: 0;
  background: transparent;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  color: #94a3b8;
  padding: 0 4px;
}
.input-wrap .clear-btn:hover { color: #0f172a; }

</style>

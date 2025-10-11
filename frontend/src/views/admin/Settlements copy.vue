<!-- src/views/admin/settlement.vue -->
<template>
  <div class="settlement-page">
    <!-- ====== 컨트롤 패널 ====== -->
    <section class="card controls" aria-label="조회 조건">
      <div class="row">
        <!-- 호텔명 자동완성 -->
        <div class="field">
          <label for="hotelName">호텔</label>
          <input
            id="hotelName"
            v-model.trim="hotelName"
            autocomplete="off"
            @input="onHotelNameInput"
            @focus="onHotelNameInput"
            @keydown.enter.prevent="hotelSuggests[0] && selectHotel(hotelSuggests[0])"
            @keydown.esc="hotelSuggests = []"
            placeholder="호텔명을 입력하세요"
            class="relative"
          />
          <small class="help">이름으로 검색 · 선택 시 ID로 매핑</small>

          <!-- 서제스트 -->
          <ul v-if="hotelSuggests.length" class="typeahead">
            <li v-for="h in hotelSuggests" :key="h.id"
                @click="selectHotel(h)" class="opt">
              {{ h.name }} <span class="muted">(#{{ h.id }})</span>
            </li>
          </ul>
          <button class="btn secondary xs" @click="clearHotel">전체 보기</button>
        </div>

        <!-- 주간 셀렉터 -->
        <div class="field">
          <label>정산 주</label>
          <div class="week-switch">
            <button class="btn xs outline" @click="shiftWeek(-1)">〈 지난 주</button>
            <span class="wk">{{ start }} ~ {{ end }}</span>
            <button class="btn xs outline" @click="shiftWeek(0)">이번 주</button>
            <button class="btn xs outline" @click="shiftWeek(1)">다음 주 〉</button>
          </div>
        </div>

        <div class="buttons">
          <button class="btn" :disabled="isQuerying" @click="queryAll" aria-label="조건으로 조회">
            {{ isQuerying ? '조회 중...' : '조회' }}
          </button>
          <button class="btn outline" :disabled="!lines?.length" @click="downloadCsv" aria-label="라인 데이터를 CSV로 내보내기">
            CSV 내보내기
          </button>
        </div>
      </div>
      <p class="muted note">정산은 <b>주간(월→일, 체크아웃 기준)</b>으로 산정됩니다. 통화 KRW · 플랫폼 수수료 15% · 객실타입 미입력 시 전체.</p>
    </section>

    <!-- ====== KPI BAR ====== -->
    <KpiBar
      :this-week="kpi.thisWeek"
      :last-week="kpi.lastWeek"
      :funnel="kpi.funnel"
      :alerts="kpi.alerts"
      :coverage="kpi.coverage"
      :as-of="kpi.asOf"
      @filterByStatus="onFilterByStatus"
      @filterByHotel="onFilterByHotel"
    />

  <nav class="tabs card" aria-label="보기 전환">
  <button :class="['tab', tab==='summary' && 'active']" :aria-pressed="tab==='summary'" @click="switchTab('summary')">요약(호텔별)</button>
  <button :class="['tab', tab==='lines' && 'active']" :aria-pressed="tab==='lines'" @click="switchTab('lines')">라인(상세)</button>
  <button :class="['tab', tab==='statements' && 'active']" :aria-pressed="tab==='statements'" @click="switchTab('statements')">정산서</button>
  <button :class="['tab', tab==='settled' && 'active']" :aria-pressed="tab==='settled'" @click="switchTab('settled')">완료</button>

  <!-- 오른쪽 끝 상태 필터 -->
  <div class="status-filter">
    <label for="statusFilter">상태:</label>
    <select id="statusFilter" v-model="summaryFilter">
      <option value="ALL">모두 ({{ statusCounts.ALL }})</option>
      <option value="NULL">정산미완료 ({{ statusCounts.NULL }})</option>
      <option value="PLANNED">정산예정 ({{ statusCounts.PLANNED }})</option>
      <option value="SETTLED">정산완료 ({{ statusCounts.SETTLED }})</option>
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
          <div v-else class="table-wrap">
          <!-- 상태 필터 드롭다운 -->



            <table class="table" aria-label="호텔별 요약">
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
                <tr v-for="s in summaryView" :key="s.hotelId" class="clickable"
                    role="button" tabindex="0"
                    @click.stop.prevent="onSummaryRowClick(s)"
                    @keydown.enter.prevent="onSummaryRowClick(s)"
                    @keydown.space.prevent="onSummaryRowClick(s)">
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

/* ===== 상태 ===== */
const tab = ref('summary')

/** 검색 상태 */
const hotelId    = ref(null)
const hotelName  = ref('')
const hotelSuggests = ref([])
const hotelMap = ref({})
const roomTypeId = ref(null) // API 필터 호환용

/** 주간 범위 */
const start = ref(mondayISO(new Date()))
const end   = ref(sundayISO(new Date()))

/** 지급 정보 */
const bankCode   = ref('012')
const accountNo  = ref('123-456-789012')
const holderName = ref('(주)hotelsMagic')

/** 표 데이터 */
const summary    = ref([])
const lines      = ref([])
const statements = ref([])
const settled    = ref([])

/** 상태 필터/맵 */
const statusMap = ref({})          // { [hotelId]: 'PLANNED' | 'SETTLED' | null }
const summaryFilter = ref('ALL')   // 'ALL' | 'NULL' | 'PLANNED' | 'SETTLED'

/** 필터된 뷰 & 개수 */
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

/** KPI 데이터 */
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
      const list = await api('/api/hotels/search?'+qs({ q: keyword }))
      hotelSuggests.value = Array.isArray(list) ? list.slice(0,10) : []
    }catch{ hotelSuggests.value=[] }
  }, 220)
}
function selectHotel(h){
  hotelName.value = h.name
  hotelId.value   = h.id
  hotelSuggests.value = []
  loadPayoutDefault()
}

/* 호텔 지급정보 기본값 로드(있으면 채움) */
async function loadPayoutDefault(){
  if(!hotelId.value) return
  try{
    const r = await api(`/api/hotels/${hotelId.value}/payout`)
    if(r){
      bankCode.value   = r.bankCode   ?? bankCode.value
      accountNo.value  = r.accountNo  ?? accountNo.value
      holderName.value = r.holderName ?? holderName.value
    }
  }catch{/* 무시 */}
}
function clearHotel(){
  hotelId.value = null
  hotelName.value = ''
  queryAll()
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

/* ===== 상태 프로빙 ===== */
async function probeStatuses(list){
  const hotelIds = (list || []).map(r => Number(r.hotelId))
  statusMap.value = {}

  const chunks = (arr, n=8) =>
    Array.from({length: Math.ceil(arr.length/n)}, (_,i)=>arr.slice(i*n,(i+1)*n))

  for (const chunk of chunks(hotelIds, 8)){
    await Promise.all(chunk.map(async (hid) => {
      // 확정 여부
      const done = await api('/api/settlements/statements?' + qs({
        hotelId: hid, start: start.value, end: end.value, status: 'SETTLED'
      }))
      if (Array.isArray(done) && done.length){
        statusMap.value[hid] = 'SETTLED'
        return
      }
      // 정산서 생성 여부
      const planned = await api('/api/settlements/statements?' + qs({
        hotelId: hid, start: start.value, end: end.value, status: 'PLANNED'
      }))
      statusMap.value[hid] = (Array.isArray(planned) && planned.length) ? 'PLANNED' : null
    }))
  }
}

/* ===== API 호출 ===== */
async function loadSummary(range='this'){
  const url = `/api/settlements/summary?`+qs({
    hotelId: hotelId.value || undefined,
    roomTypeId: roomTypeId.value || undefined,
    start: range==='this' ? start.value : prevWeek(start.value),
    end:   range==='this' ? end.value   : prevWeek(end.value)
  })
  const list = await api(url, {}, 'summary')
  if(range==='this') {
    summary.value = list ?? []
    await probeStatuses(summary.value)   // 상태 확인
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
    hotelId: hotelId.value,
    start:   start.value,
    end:     end.value,
    status:  'PLANNED'
  })
  statements.value = await api(url, {}, 'statements') ?? []
}
async function loadSettled(){
  if(!start.value || !end.value){ settled.value=[]; return }

  const query = {
    start:   start.value,
    end:     end.value,
    status:  'SETTLED'
  }
  if(hotelId.value){ query.hotelId = hotelId.value }

  const url = `/api/settlements/statements?` + qs(query)
  settled.value = await api(url, {}, 'settled') ?? []
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
  await api(`/api/settlements/${id}/settle`, { method:'POST' })
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
function pct(a,b){ if(!b) return 0; return ((a-b)/b)*100 }
function prevWeek(iso){ const d=new Date(iso); d.setDate(d.getDate()-7); return toISODate(d) }

async function refreshKpi(){
  const [thisList, lastList] = await Promise.all([loadSummary('this'), loadSummary('last')])
  await loadSettled()

  const T = totals(thisList), L = totals(lastList)
  const spark = thisList.map(x => x.netSum || 0).slice(0, 12)

  const funnel = {
    created: (statements.value?.length || 0),
    settled: (settled.value?.length || 0)
  }

  const mThis = mapByHotel(thisList), mLast = mapByHotel(lastList)
  const deltas = []
  mThis.forEach((v,k)=>{
    const cur = v.netSum ?? 0
    const prev = mLast.get(k)?.netSum ?? 0
    const d = pct(cur, prev)
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

/* ===== 흐름 ===== */
async function queryAll(){
  if(!hotelId.value && hotelName.value?.trim()){
    const list = await api('/api/hotels/search?' + qs({ name: hotelName.value.trim() }))
    if(Array.isArray(list) && list.length === 1){
      hotelId.value   = list[0].id
      hotelName.value = list[0].name
      await loadPayoutDefault()
    }
  }

  await Promise.all([
    loadSummary('this'),
    loadLines(),
    loadStatements(),
    loadSettled()
  ])
  await refreshKpi()
  toast('데이터를 새로고침했어요.')
}

/* 탭 이동 시 자동 로딩 */
function switchTab(to){
  tab.value = to
  if(to === 'statements') loadStatements()
  if(to === 'settled')    loadSettled()
  if(to === 'lines')      loadLines()
  if(to === 'summary')    loadSummary('this')
}

/* 라인 → 정산서로 이동(리스트 보기/스크롤) */
async function goToStatements(){
  tab.value = 'statements'
  await nextTick()
  await Promise.all([loadStatements(), loadPayoutDefault()])
  document.getElementById('statements-section')?.scrollIntoView({ behavior:'smooth', block:'start' })
}

/* 호텔 바뀌면 현재 탭 데이터 재조회 */
watch(hotelId, () => {
  if(tab.value === 'lines'      && start.value && end.value) loadLines()
  if(tab.value === 'statements') loadStatements()
  if(tab.value === 'settled')    loadSettled()
})

onMounted(async () => {
  // 전체 호텔 불러오기 → hotelMap에 저장
  try {
    const page = await api('/api/hotels/search?size=9999')
    const list = page?.content ?? []
    hotelMap.value = Object.fromEntries(list.map(h => [h.id, h.name]))
  } catch (e) { console.error(e) }

  await queryAll()
})

/* 요약(호텔별) 행 클릭: 즉시 조회 */
async function onSummaryRowClick(s){
  hotelId.value   = Number(s.hotelId)
  hotelName.value = s.hotelName
  await queryAll()
  tab.value = 'lines'
  await nextTick()
  loadLines()
}

/* KPI 이벤트 */
function onFilterByStatus(){
  tab.value = 'settled'
  loadSettled()
}
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
const KpiBar = defineComponent({
  name:'KpiBar',
  emits:['filterByStatus','filterByHotel'],
  props:{
    thisWeek:{type:Object, required:true},
    lastWeek:{type:Object, required:true},
    funnel:{type:Object, required:true},
    alerts:{type:Array,  required:true},
    coverage:{type:Object, required:true},
    asOf:{type:String, default:''}
  },
  setup(props,{emit}){
    const pct = (a,b)=> !b?0:Math.round(((a-b)/b)*1000)/10
    const wow = {
      bookings: computed(()=>pct(props.thisWeek.bookings, props.lastWeek.bookings)),
      gross:    computed(()=>pct(props.thisWeek.gross,    props.lastWeek.gross)),
      net:      computed(()=>pct(props.thisWeek.net,      props.lastWeek.net)),
      fee:      computed(()=>pct(props.thisWeek.fee,      props.lastWeek.fee)),
    }
    const barRatio = computed(()=>{
      const t = props.funnel.created + props.funnel.settled
      const settled = t? Math.round(props.funnel.settled/t*100):0
      return { settled }
    })

    const labelMap = { bookings:'예약건', gross:'총액', net:'지급', fee:'수수료' }
    const metricKeys = ['bookings','gross','net','fee']

    const pathSpark = computed(()=>{
      const a = props.thisWeek.spark||[]
      if(!a.length) return ''
      const w=80, h=20, max=Math.max(...a,1)
      return a.map((v,i)=>{
        const x = Math.round(i*(w/(a.length-1)))
        const y = Math.round(h - (v/max)*h)
        return `${i?'L':'M'}${x},${y}`
      }).join(' ')
    })

    return () => h('section',{class:'card kpi'}, [
      h('div',{class:'row wrap g12'}, [

        // 퍼널
        h('div',{class:'k-card'}, [
          h('div',{class:'k-h'}, '정산 진행률'),
          h('div',{class:'funnel'}, [
            h('div',{class:'bar'}, [
              h('div',{class:'fill', style:{width:`${barRatio.value.settled}%`}})
            ]),
            h('div',{class:'f-meta'}, `확정 ${props.funnel.settled}`)
          ]),
          h('div',{class:'k-actions'}, [
            h('button',{class:'link', onClick:()=>emit('filterByStatus','SETTLED')}, '확정 보기')
          ])
        ]),

        ...metricKeys.map(key =>
          h('div',{class:'k-card small', key}, [
            h('div',{class:'k-h'}, key==='bookings' ? labelMap[key] : `₩ ${labelMap[key]}`),
            h('div',{class:'k-num'},
              key==='bookings'
                ? (props.thisWeek.bookings ?? 0).toLocaleString('ko-KR')
                : (props.thisWeek[key] ?? 0).toLocaleString('ko-KR') + '원'
            ),
            h('div',{class:['k-wow', wow[key].value>=0?'up':'down']},
              (wow[key].value>=0?'▲ ':'▼ ') + Math.abs(wow[key].value) + '%'
            ),
            h('svg',{width:80,height:20, class:'spark'}, [
              h('path',{d:pathSpark.value, fill:'none', stroke:'currentColor', 'stroke-width':1})
            ])
          ])
        ),

        // 이상징후
        h('div',{class:'k-card'}, [
          h('div',{class:'k-h'}, '이상 변동(전주 대비 ±30%↑/↓)'),
          props.alerts.length
            ? h('ul',{class:'alert-list'},
                props.alerts.map(a =>
                  h('li',{class:'alert-item', onClick:()=>emit('filterByHotel', a)},
                    `${a.hotelName} (${a.deltaPct>0?'+':''}${a.deltaPct}%)`)
                ))
            : h('div',{class:'muted'}, '특이사항 없음')
        ]),

        // 커버리지 + asOf
        h('div',{class:'k-card small'}, [
          h('div',{class:'k-h'}, '활성 호텔'),
          h('div',{class:'k-num'}, `${props.coverage.activeHotels}/${props.coverage.totalHotels}`),
          h('div',{class:'muted'}, `업데이트: ${props.asOf}`)
        ])
      ])
    ])
  }
})
</script>

<style scoped>
:root{
  --bg:#f5f9ff; --card:#fff; --line:#d9e2f0; --text:#0b1220; --muted:#5b6b82;
  --pri:#60a5fa; --pri-600:#1d4ed8; --ok:#16a34a; --warn:#f59e0b; --danger:#ef4444; --focus:#1e40af;
}

/* 페이지 */
.settlement-page{ color:var(--text); background:var(--bg); padding:16px; min-height:100vh; }

/* 카드 공통 */
.card{ background:var(--card); border:1px solid var(--line); border-radius:14px; padding:16px; margin-bottom:12px;
       box-shadow:0 6px 18px rgba(59,130,246,.08); }

/* 컨트롤 패널 */
.controls .row{ display:grid; grid-template-columns:280px 1fr auto; gap:12px; align-items:end; }
.field{ display:flex; flex-direction:column; gap:6px; position:relative; }
.field label{ color:var(--muted); font-size:12.5px; font-weight:600; }
.field .help{ color:var(--muted); font-size:11px; }
.field input{ background:#fff; color:var(--text); border:1px solid var(--line); border-radius:10px; padding:10px 12px; font-size:14px; outline:none; }
.field input:focus-visible{ border-color:var(--focus); box-shadow:0 0 0 3px rgba(30,64,175,.25); }

.typeahead{ position:absolute; top:64px; left:0; right:0; background:#fff; border:1px solid var(--line); border-radius:10px; z-index:5; max-height:220px; overflow:auto; }
.opt{ padding:8px 10px; cursor:pointer; }
.opt:hover{ background:#eef4ff; }

.week-switch{ display:flex; align-items:center; gap:8px; }
.week-switch .wk{ font-weight:700; letter-spacing:.2px; }

.buttons{ display:flex; gap:8px; align-items:center; }
.buttons.right{ margin-left:auto; }
.row{ display:flex; gap:12px; align-items:center; }
.row.wrap{ flex-wrap:wrap; }
.payout{ display:flex; gap:12px; }

/* 버튼 */
.btn{ background:var(--pri-600); border:1px solid var(--pri-600); color:#fff; padding:10px 14px; border-radius:10px; cursor:pointer; font-weight:700;
      transition:transform .06s ease, background .15s ease; }
.btn:hover{ background:#153ea6; transform:translateY(-1px); }
.btn:disabled{ opacity:.6; cursor:not-allowed; transform:none; }
.btn:focus-visible{ outline:2px solid var(--focus); outline-offset:2px; }
.btn.secondary{ background:#eef5ff; border-color:#bcd3ff; color:#102a7a; }
.btn.secondary:hover{ background:#ddeaff; }
.btn.outline{ background:#f8fbff; border-color:var(--pri-600); color:var(--pri-600); }
.btn.xs{ padding:6px 10px; font-size:12px; }
.card .btn{ color:inherit; }

/* 탭 */
/* 탭: 같은 줄 정렬 + 줄바꿈 대응 */
.tabs{
  display:flex;
  align-items:center;
  gap:8px;
  flex-wrap:wrap;            /* 화면 좁아지면 다음 줄로 */
}
.tab{ padding:10px 12px; border-radius:10px; background:#f1f5ff; border:1px solid #dbe5ff; color:#103072; cursor:pointer; font-weight:700; }
.tab.active{ background:#e0ecff; border-color:#bcd3ff; color:#1d4ed8; box-shadow:inset 0 0 0 2px rgba(59,130,246,.18); }
.tab:focus-visible{ outline:2px solid var(--focus); outline-offset:2px; }

/* 테이블 */
.table-wrap{ max-height:56vh; overflow:auto; }
.table{ width:100%; border-collapse:collapse; font-size:14px; }
.table th,.table td{ border-top:1px solid var(--line); padding:12px 10px; vertical-align:middle; }
.table thead th{ position:sticky; top:0; z-index:1; background:#eaf2ff; color:#0b1220; font-weight:800; }
.th-unit{ font-weight:500; color:var(--muted); }
.tr{ text-align:right; }
.strong{ font-weight:800; }
.clickable{ cursor:pointer; }
.clickable:hover{ background:#eef4ff; }
.clickable:focus-visible{ outline:2px solid var(--focus); outline-offset:-2px; }

/* 상태/배지 */
.badge{ padding:4px 8px; border-radius:999px; font-size:12px; border:1px solid #cfe0ff; background:#eef5ff; color:#1d4ed8; font-weight:700; }
.badge.ok{ background:#e6f7ef; color:#0f5132; border-color:#b7e4c7; }
.badge.plan{ background:#eef5ff; color:#0b3ea1; border-color:#cfe0ff; }

/* 경고 */
.alert.error{ background:#fff1f2; border:1px solid #fecdd3; color:#7f1d1d; border-radius:10px; padding:12px; margin-bottom:12px; display:flex; align-items:center; gap:10px; }

/* Empty / 스켈레톤 */
.empty{ text-align:center; color:#5b6b82; padding:18px 8px; }
.skeleton{ display:flex; flex-direction:column; gap:10px; }
.sk-row{ display:grid; grid-template-columns:1fr 120px 120px 120px 120px; gap:10px; }
.sk-col{ height:14px; border-radius:8px; background:linear-gradient(90deg,#eef2ff,#f8faff,#eef2ff); background-size:200% 100%; animation:shimmer 1.2s ease-in-out infinite; }
.w40{ grid-column:1/2; } .w10{ grid-column:auto; }
@keyframes shimmer{ 0%{background-position:0% 0} 100%{background-position:200% 0} }

/* 반응형 */
.hide-sm{ display:table-cell; }
.hide-md{ display:table-cell; }
@media (max-width:960px){
  .controls .row{ grid-template-columns:1fr 1fr; }
  .buttons{ flex-wrap:wrap; }
  .payout{ flex-direction:column; width:100%; }
  .table-wrap{ max-height:none; }
  .hide-md{ display:none; }
}
@media (max-width:680px){
  .hide-sm{ display:none; }
}

/* ===== KPI 바 ===== */
:deep(.kpi){ width:100%; }
:deep(.kpi .row){ display:flex; align-items:stretch; gap:12px; flex-wrap:wrap; }
:deep(.k-card){ flex:1 1 260px; min-width:220px; border:1px solid var(--line); border-radius:12px; padding:12px; background:#fbfdff; }
:deep(.k-card.small){ flex:0 1 220px; }
:deep(.k-h){ font-size:12px; color:#31507a; font-weight:800; margin-bottom:6px; white-space:nowrap; }
:deep(.k-num){ font-size:20px; font-weight:900; margin:2px 0 6px; }
:deep(.k-wow){ font-size:12px; font-weight:800; }
:deep(.k-wow.up){ color:#0f766e; }
:deep(.k-wow.down){ color:#b91c1c; }
:deep(.spark){ display:block; margin-top:6px; color:#9aa7c7; }
:deep(.funnel .bar){ width:100%; height:8px; border-radius:999px; background:#e7efff; overflow:hidden; }
:deep(.funnel .fill){ height:8px; background:#1d4ed8; }
:deep(.f-meta){ margin-top:6px; color:#4b5a77; font-size:12px; }
:deep(.k-actions){ display:flex; gap:8px; margin-top:8px; }
:deep(.link){ background:transparent; border:none; color:#1d4ed8; cursor:pointer; text-decoration:underline; font-weight:700; }
@media (max-width:1200px){ :deep(.k-card){ flex:1 1 300px; } }
@media (max-width:960px){ :deep(.k-card){ flex:1 1 320px; } :deep(.k-card.small){ flex:1 1 240px; } }
@media (max-width:680px){ :deep(.k-card){ flex:1 1 100%; min-width:0; } :deep(.k-card.small){ flex:1 1 48%; }}

/* 토스트 */
.toast{
  position:fixed; left:50%; bottom:24px; transform:translateX(-50%) translateY(20px);
  background:#111827; color:#fff; padding:10px 14px; border-radius:10px; opacity:.95; z-index:9999;
}

/* ===== 상태 필터 segmented control ===== */
.segmented{
  display:flex; gap:8px; margin:6px 0 10px;
  position:sticky; top:0; background:#eaf2ff; padding:6px; z-index:2; border-radius:12px;
}
.seg{
  appearance:none; background:#fff; border:2px solid #dbe5ff; color:#0b1220;
  padding:8px 12px; border-radius:12px; font-weight:800; cursor:pointer;
  box-shadow:0 2px 6px rgba(17,24,39,.06);
}
.seg.active{ border-color:var(--pri-600); box-shadow:0 0 0 2px rgba(29,78,216,.15) inset; }
.seg:focus-visible{ outline:2px solid var(--focus); outline-offset:2px; }
.chip{
  display:inline-block; min-width:20px; padding:0 6px; margin-left:6px;
  border-radius:999px; font-size:12px; font-weight:800;
  background:#eef5ff; color:#1d4ed8; border:1px solid #cfe0ff; text-align:center;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0 10px;
}

.filter-bar label {
  font-weight: 600;
  color: var(--muted);
  font-size: 13px;
}

.filter-bar select {
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
}
.filter-bar select:focus-visible {
  border-color: var(--focus);
  box-shadow: 0 0 0 3px rgba(30, 64, 175, .25);
  outline: none;
}
.status-filter{
  margin-left:auto;          /* 핵심: 오른쪽 끝 */
  display:flex;
  align-items:center;
  gap:8px;
}



.status-filter select{
  padding:6px 10px;
  border:1px solid #cbd5e1;
  border-radius:8px;
  font-size:14px;
  background:#fff;
}

/* 모바일일 때는 다음 줄로 자연스럽게 내려가 우측 정렬 */
@media (max-width:680px){
  .status-filter{
    width:100%;
    margin-left:0;
    justify-content:flex-end;
    margin-top:6px;
  }
}
</style>

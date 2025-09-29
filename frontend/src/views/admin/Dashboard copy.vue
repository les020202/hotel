<!-- src/views/admin/Dashboard.vue -->
<template>
  <div class="dash">
    <!-- 헤더 -->
    <header class="page-head">
      <div>
        <h1>관리자 대시보드</h1>
        <p class="sub">기간 · 지역 · 호텔 필터로 지표를 즉시 비교하세요</p>
      </div>
      <div class="head-cta">
        <button class="btn ghost" @click="saveFilter" title="현재 필터 저장">필터 저장</button>
        <button class="btn ghost" @click="loadFilter" title="저장한 필터 불러오기">불러오기</button>
        <button class="btn" @click="exportCsv" title="현재 조회범위 CSV 내보내기">CSV</button>
      </div>
    </header>

    <!-- 필터 -->
    <section class="filters card">
      <div class="chips">
        <button class="chip" :class="{active: preset==='TODAY'}"  @click="setPreset('TODAY')">TODAY</button>
        <button class="chip" :class="{active: preset==='7D'}"     @click="setPreset('7D')">7D</button>
        <button class="chip" :class="{active: preset==='30D'}"    @click="setPreset('30D')">30D</button>
        <button class="chip" :class="{active: preset==='CUSTOM'}" @click="preset='CUSTOM'">CUSTOM</button>
        <span class="date-group">
          <input type="date" v-model="from" :disabled="preset!=='CUSTOM'">
          <span class="dashmark">—</span>
          <input type="date" v-model="to"   :disabled="preset!=='CUSTOM'">
        </span>
        <span class="period">{{ prettyPeriod }}</span>
      </div>

      <div class="filters-row" aria-live="polite">
        <select v-model="region">
          <option value="">지역(ALL)</option>
          <option v-for="r in regions" :key="r" :value="r">{{ r }}</option>
        </select>

        <input class="search" v-model.trim="hotelQuery" placeholder="호텔명/ID 검색" @keyup.enter="fetchAll" />

        <div class="active-filters" v-if="region || hotelQuery">
          <span v-if="region" class="af">지역: {{region}}</span>
          <span v-if="hotelQuery" class="af">호텔: “{{hotelQuery}}”</span>
          <button class="btn ghost small" @click="clearFilters">초기화</button>
        </div>

        <div class="right badges">
          <span v-if="deltas.todayVsYesterday <= -0.3" class="badge danger" role="status" @click="openDecomp('gmv')">
            ▼ 오늘 GMV 전일 대비 {{ pct(deltas.todayVsYesterday) }}
          </span>
          <span v-else-if="deltas.todayVsYesterday >= 0.3" class="badge success" role="status" @click="openDecomp('gmv')">
            ▲ 오늘 GMV 전일 대비 {{ pct(deltas.todayVsYesterday) }}
          </span>
          <span v-if="deltas.todayVsLastWeek <= -0.3" class="badge warn" role="status" @click="openDecomp('gmv')">
            ▽ 지난주 동일요일 {{ pct(deltas.todayVsLastWeek) }}
          </span>
          <span v-if="settlement.pendingAmt > 0" class="badge info" :title="settlement.periodText">
            정산 대기 {{ fmtW(settlement.pendingAmt) }}
          </span>
        </div>
      </div>
    </section>

    <!-- KPI (스파크라인 내장) -->
    <section class="kpis" :class="{loading}">
      <div class="card kpi" role="button" tabindex="0" @click="go('/admin/payments',{status:'SUCCEEDED'})">
        <p class="label">오늘 GMV</p>
        <h3 class="value">{{ fmtW(kpi.gmvToday) }}</h3>
        <svg v-if="kSeries.gmv?.length" :width="100" :height="28" viewBox="0 0 100 28" aria-hidden="true">
          <path :d="sparkPath(kSeries.gmv)" fill="none" stroke="var(--primary)" stroke-width="2"/>
          <circle v-if="kSeries.gmv.length" :cx="sparkLast(kSeries.gmv).x" :cy="sparkLast(kSeries.gmv).y" r="2.5" fill="var(--primary)"/>
        </svg>
        <p class="delta" :class="deltas.todayVsYesterday>=0?'up':'down'">
          <span aria-hidden="true">{{ deltas.todayVsYesterday>=0 ? '▲' : '▼' }}</span>
          {{ (deltas.todayVsYesterday*100).toFixed(1) }}%
        </p>
      </div>

      <div class="card kpi">
        <p class="label">이번 주 GMV</p>
        <h3 class="value">{{ fmtW(kpi.gmvWeek) }}</h3>
      </div>
      <div class="card kpi">
        <p class="label">이번 달 GMV</p>
        <h3 class="value">{{ fmtW(kpi.gmvMonth) }}</h3>
      </div>
      <div class="card kpi">
        <p class="label">환불 합계(월)</p>
        <h3 class="value">{{ fmtW(kpi.refundMonth) }}</h3>
      </div>

      <div class="card kpi">
        <p class="label">ADR(30일)</p>
        <h3 class="value">{{ fmtW(kpi.adr30) }}</h3>
      </div>
      <div class="card kpi">
        <p class="label">점유율(30일)</p>
        <h3 class="value">{{ pct(kpi.occ30) }}</h3>
      </div>
      <div class="card kpi">
        <p class="label">RevPAR(30일)</p>
        <h3 class="value">{{ fmtW(kpi.revpar30) }}</h3>
      </div>
      <div class="card kpi" role="button" tabindex="0" @click="go('/admin/bookings',{minNights:3})">
        <p class="label">장기숙박 비율</p>
        <h3 class="value">{{ pct(kpi.longStayRate30) }}</h3>
      </div>
    </section>

    <!-- 추이 -->
    <section class="charts">
      <div class="card chart" :class="{skeleton: loading}">
        <div class="card-title"><strong>{{ isToday ? '시간별 매출(오늘)' : `일별 매출(최근 ${trendDays}일)` }}</strong></div>
        <canvas v-show="!loading" ref="revChartEl" height="160"></canvas>
      </div>
      <div class="card chart" :class="{skeleton: loading}">
        <div class="card-title"><strong>{{ isToday ? '시간별 예약건수(오늘)' : `일별 예약건수(최근 ${trendDays}일)` }}</strong></div>
        <canvas v-show="!loading" ref="bkChartEl" height="160"></canvas>
      </div>
    </section>

    <!-- 랭킹 + 정산 -->
    <section class="grid">
      <div class="card table" :class="{skeleton: loading}">
        <div class="card-title">
          <div class="tabs">
            <button class="tab" :class="{active: rankTab==='todayRevenue'}" @click="rankTab='todayRevenue'">오늘 매출 Top10</button>
            <button class="tab" :class="{active: rankTab==='monthRevenue'}" @click="rankTab='monthRevenue'">월간 매출 Top10</button>
            <button class="tab" :class="{active: rankTab==='monthRefundRateLow'}" @click="rankTab='monthRefundRateLow'">월간 환불율 낮은 순</button>
            <button class="tab" :class="{active: rankTab==='velocityUp'}" @click="rankTab='velocityUp'">상승 Top10</button>
            <button class="tab" :class="{active: rankTab==='velocityDown'}" @click="rankTab='velocityDown'">하락 Top10</button>
          </div>
          <small class="muted" v-if="['todayRevenue','monthRevenue'].includes(rankTab)">
            합계: {{ fmtW(ranksTotal.revenue) }} / 예약 {{ ranksTotal.bookings.toLocaleString() }}건
          </small>
          <small class="muted" v-else-if="rankTab==='monthRefundRateLow'">* 최소 모수 {{minVolume}}건</small>
        </div>

        <table v-if="rankTab!=='monthRefundRateLow'">
          <thead>
            <tr><th>#</th><th>호텔명</th><th>지역</th><th>예약수</th><th>매출</th><th>Δ</th></tr>
          </thead>
          <tbody>
            <tr v-for="(r,i) in currentRankRows" :key="r.hotelId">
              <td>{{ i+1 }}</td>
              <td>{{ r.hotelName }}</td>
              <td>{{ r.region || '-' }}</td>
              <td>{{ r.bookings ?? '-' }}</td>
              <td>{{ fmtW(r.revenue ?? 0) }}</td>
              <td><span :class="(r.delta??0)>=0?'up':'down'">{{ (r.delta??0)>=0?'▲':'▼' }} {{ pct(r.delta ?? 0) }}</span></td>
            </tr>
            <tr v-if="!currentRankRows.length"><td colspan="6" class="muted center">데이터가 없습니다.</td></tr>
          </tbody>
        </table>

        <table v-else>
          <thead><tr><th>#</th><th>호텔명</th><th>지역</th><th>예약수</th><th>환불수</th><th>환불율</th></tr></thead>
          <tbody>
            <tr v-for="(r,i) in ranks.monthRefundRateLow" :key="r.hotelId">
              <td>{{ i+1 }}</td><td>{{ r.hotelName }}</td><td>{{ r.region || '-' }}</td>
              <td>{{ r.bookings }}</td><td>{{ r.refunds }}</td><td>{{ pct(r.refundRate) }}</td>
            </tr>
            <tr v-if="!ranks.monthRefundRateLow.length"><td colspan="6" class="muted center">데이터가 없습니다.</td></tr>
          </tbody>
        </table>
      </div>

      <div class="card settle" :class="{skeleton: loading}">
        <div class="card-title"><strong>정산 대기 금액</strong></div>
        <h2 class="value">{{ fmtW(settlement.pendingAmt) }}</h2>
        <p class="muted">{{ settlement.periodText }} · {{ settlement.count }}건 대기 · 전월 대비 {{ deltaBadge(settlement.deltaVsPrevMonth) }}</p>
        <button class="btn ghost small" @click="$emit('open-settlement')">정산 상세 보기</button>
      </div>
    </section>

    <!-- 원인 분해 Drawer + Overlay -->
    <transition name="fade"><div v-if="decomp.open" class="overlay" @click="decomp.open=false" /></transition>
    <transition name="slide">
      <aside v-if="decomp.open" class="drawer">
        <header>
          <strong>원인 분해: {{ decomp.metric.toUpperCase() }}</strong>
          <button class="btn ghost small" @click="decomp.open=false">닫기</button>
        </header>
        <div class="tabs">
          <button class="tab" :class="{active: decomp.by==='region'}" @click="loadDecomp('region')">지역</button>
          <button class="tab" :class="{active: decomp.by==='hotel'}"  @click="loadDecomp('hotel')">호텔</button>
          <button class="tab" :class="{active: decomp.by==='channel'}" @click="loadDecomp('channel')" :disabled="!hasChannel">채널</button>
        </div>

        <div class="mini-chart">
          <canvas ref="decompChartEl" height="140"></canvas>
        </div>

        <table class="mini-table">
          <thead>
            <tr><th>#</th><th v-text="decomp.byLabel"></th><th class="right">금액</th><th class="right">비중</th></tr>
          </thead>
          <tbody>
            <tr v-for="(r,i) in decomp.rows" :key="r.key">
              <td>{{ i+1 }}</td>
              <td>{{ r.label }}</td>
              <td class="right">{{ fmtW(r.gmv) }}</td>
              <td class="right">{{ pct(r.share) }}</td>
            </tr>
            <tr v-if="!decomp.rows.length"><td colspan="4" class="muted center">데이터 없음</td></tr>
          </tbody>
        </table>
      </aside>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import Chart from 'chart.js/auto'

/* 차트 인스턴스 & 레이스 가드 */
let revChart = null
let bkChart = null
let decChart = null
let drawGen = 0

/* -------- API 공통 -------- */
async function getApi(url){
  const res = await fetch(url, { credentials: 'include' })
  if (!res.ok) throw new Error(`GET ${url} ${res.status}`)
  return await res.json()
}

/* -------- 상태 -------- */
const preset = ref('7D')
const from = ref(''); const to = ref('')
const region = ref(''); const hotelQuery = ref('')
const loading = ref(true)
const regions = ref([])

const kpi = ref({ gmvToday:0, gmvWeek:0, gmvMonth:0, refundMonth:0, adr30:0, occ30:0, revpar30:0, longStayRate30:0 })
const deltas = ref({ todayVsYesterday:0, todayVsLastWeek:0 })
const settlement = ref({ pendingAmt:0, deltaVsPrevMonth:0, periodText:'이번 달', count:0 })

/* 스파크라인용 시리즈 */
const kSeries = ref({ gmv: [] })

/* 캔버스 refs */
const revChartEl = ref(null), bkChartEl = ref(null), decompChartEl = ref(null)

/* 랭킹 */
const rankTab = ref('todayRevenue'), minVolume = 10
const ranks = ref({ todayRevenue:[], monthRevenue:[], monthRefundRateLow:[], velocityUp:[], velocityDown:[] })
const ranksTotal = ref({ revenue:0, bookings:0 })
const currentRankRows = computed(()=>{
  if (rankTab.value==='todayRevenue') return ranks.value.todayRevenue
  if (rankTab.value==='monthRevenue') return ranks.value.monthRevenue
  if (rankTab.value==='velocityUp')   return ranks.value.velocityUp
  if (rankTab.value==='velocityDown') return ranks.value.velocityDown
  return []
})

/* 원인분해 Drawer */
const decomp = ref({ open:false, metric:'gmv', by:'region', byLabel:'지역', rows:[], total:0 })
const hasChannel = true // 채널 차원 없으면 false

/* Utils */
const fmtW = n => `${(Number(n)||0).toLocaleString()}원`
const pct  = x => `${(Number(x||0)*100).toFixed(1)}%`
const deltaBadge = x => (x>=0?`+${(x*100).toFixed(1)}%↑`:`${(x*100).toFixed(1)}%↓`)
const todayStr=()=>new Date().toISOString().slice(0,10)
const deltaDays=d=>{const dt=new Date();dt.setDate(dt.getDate()-d);return dt.toISOString().slice(0,10)}
const isToday = computed(()=> preset.value==='TODAY')
const trendDays = computed(()=> isToday.value? 1 : (preset.value==='7D'?7 : preset.value==='30D'?30 : Math.max(1, daysBetween(from.value,to.value))))
const prettyPeriod = computed(()=> from.value && to.value ? `${from.value} – ${to.value}` : '')

function daysBetween(a,b){const d1=new Date(a),d2=new Date(b);return Math.max(1,Math.round((d2-d1)/86400000)+1)}
function q(hasQuery = false) {
  const qs = new URLSearchParams()
  if (from.value) qs.set('from', from.value)
  if (to.value) qs.set('to', to.value)
  if (region.value) qs.set('region', region.value)
  if (hotelQuery.value) qs.set('hotel', hotelQuery.value.trim())
  const s = qs.toString()
  return s ? (hasQuery ? `&${s}` : `?${s}`) : ''
}

/* Regions */
async function loadRegions(){
  try{ regions.value = await getApi('/api/admin/regions') }
  catch(e){ console.warn('regions load fail', e); regions.value = [] }
}

/* 필터 */
function setPreset(p){
  preset.value=p
  if(p==='TODAY'){from.value=todayStr();to.value=todayStr()}
  if(p==='7D'){from.value=deltaDays(6);to.value=todayStr()}
  if(p==='30D'){from.value=deltaDays(29);to.value=todayStr()}
  // fetchAll()는 watch([region,from,to], ...)가 대신 호출
}
function clearFilters(){ region.value=''; hotelQuery.value=''; fetchAll() }
function saveFilter(){ localStorage.setItem('admin_filters', JSON.stringify({preset:preset.value,from:from.value,to:to.value,region:region.value,hotelQuery:hotelQuery.value})); alert('저장 완료') }
function loadFilter(){ const raw=localStorage.getItem('admin_filters'); if(!raw) return alert('없음'); const f=JSON.parse(raw); preset.value=f.preset; from.value=f.from; to.value=f.to; region.value=f.region; hotelQuery.value=f.hotelQuery; preset.value!=='CUSTOM'? setPreset(preset.value): fetchAll() }
function exportCsv(){ window.open('/api/admin/export.csv' + q(), '_blank') }

/* 이동 */
function go(path, extra={}){ console.log('navigate to', path + q(), { ...extra }) }

/* Drawer */
function openDecomp(metric){ decomp.value.metric=metric; decomp.value.open=true; loadDecomp('region') }
async function loadDecomp(by){
  decomp.value.by = by
  decomp.value.byLabel = by==='region' ? '지역' : by==='hotel' ? '호텔' : '채널'
  const res = await getApi(`/api/admin/decomp?metric=${decomp.value.metric}&by=${by}` + q(true))
  const rows = (res.rows||[]).map(x=>({ key:x.key, label:x.label, gmv:+x.gmv, share:+x.share }))
  decomp.value.rows = rows
  decomp.value.total = rows.reduce((s,x)=>s+x.gmv,0)
  drawDecompChart(rows)
}

/* 합계 재계산 */
function recomputeRanksTotal(){
  const base = (rankTab.value==='todayRevenue'? ranks.value.todayRevenue : ranks.value.monthRevenue)
  ranksTotal.value = {
    revenue: (base||[]).reduce((s,x)=>s+(+x.revenue||0),0),
    bookings: (base||[]).reduce((s,x)=>s+(+x.bookings||0),0)
  }
}

/* 데이터 로드 (레이스 가드) */
async function fetchAll(){
  loading.value = true
  const myGen = ++drawGen
  try{
    const ov = await getApi('/api/admin/overview' + q())
    if (myGen !== drawGen) return
    kpi.value = {
      gmvToday:+ov.gmvToday, gmvWeek:+ov.gmvWeek, gmvMonth:+ov.gmvMonth,
      refundMonth:+ov.refundMonth, adr30:+ov.adr30, occ30:+ov.occ30, revpar30:+ov.revpar30, longStayRate30:+ov.longStayRate30
    }
    deltas.value = { todayVsYesterday:+ov.todayVsYesterday, todayVsLastWeek:+ov.todayVsLastWeek }
    settlement.value = { pendingAmt:+ov.settlementPending, deltaVsPrevMonth:+ov.settlementDeltaVsPrevMonth, periodText:ov.settlementPeriodText||'이번 달', count:ov.settlementCount||0 }
  }catch(e){ console.warn('overview fail',e) }

  try{
    if (isToday.value) {
      const tr = await getApi('/api/admin/trends-hourly' + q())
      if (myGen !== drawGen) return
      kSeries.value.gmv = tr.revenue || []
      drawRevChart(tr.labels||[], tr.revenue||[], true)
      drawBkChart(tr.labels||[], tr.bookings||[], true)
    } else {
      const tr = await getApi('/api/admin/trends' + q())
      if (myGen !== drawGen) return
      kSeries.value.gmv = tr.revenue || []
      drawRevChart(tr.labels||[], tr.revenue||[], false)
      drawBkChart(tr.labels||[], tr.bookings||[], false)
    }
  }catch(e){ console.warn('trends fail',e); drawRevChart([],[],isToday.value); drawBkChart([],[],isToday.value); kSeries.value.gmv=[] }

  try{
    // 오늘 매출 Top10
const dayRev = await getApi('/api/admin/top10/today')

// 이번 달 매출 Top10
const monRev = await getApi('/api/admin/top10/monthly')
    const monRefLo = await getApi('/api/admin/rankings?range=month&type=refundRate&minVolume=' + minVolume + q(true))
    const vel      = await getApi('/api/admin/rankings/velocity?range=week&limit=10' + q(true))
    if (myGen !== drawGen) return
    ranks.value.todayRevenue=dayRev
    ranks.value.monthRevenue=monRev
    ranks.value.monthRefundRateLow=monRefLo
    ranks.value.velocityUp = (vel||[]).filter(v=>v.delta>=0).slice(0,10)
    ranks.value.velocityDown = (vel||[]).filter(v=>v.delta<0).slice(0,10)
    recomputeRanksTotal()
  }catch(e){ console.warn('rank fail',e) }

  if (myGen === drawGen) loading.value = false
}

/* 차트 (그라데이션 + 마지막 점 강조, 업데이트-only) */
function drawRevChart(labels, values, hourly){
  if (!revChart) {
    const ctx = revChartEl.value.getContext('2d')
    const bg = ctx.createLinearGradient(0,0,0,180)
    bg.addColorStop(0,'rgba(37,99,235,.18)')
    bg.addColorStop(1,'rgba(37,99,235,.02)')
    revChart = new Chart(revChartEl.value, {
      type:'line',
      data:{ labels, datasets:[{
        label:'매출(GMV)', data:values, fill:true, backgroundColor:bg, borderColor:'#2563EB',
        tension:.25, borderWidth:2,
        pointRadius:(c)=> c.dataIndex===values.length-1 ? 3 : 0,
        pointBackgroundColor:'#2563EB'
      }]},
      options:{
        plugins:{ legend:{ display:false }, tooltip:{ mode:'index', intersect:false } },
        scales:{ y:{ ticks:{ callback:v=>Number(v).toLocaleString() } } },
        interaction:{ mode:'index', intersect:false },
        animation:false, maintainAspectRatio:false, responsive:true
      }
    })
    return
  }
  // 업데이트만 (중첩 그리기 방지)
  revChart.data.labels = labels
  revChart.data.datasets[0].data = values
  revChart.data.datasets[0].pointRadius = (ctx)=> ctx.dataIndex===values.length-1 ? 3 : 0
  revChart.update('none')
}

function drawBkChart(labels, values, hourly){
  if (!bkChart) {
    bkChart = new Chart(bkChartEl.value, {
      type:'line',
      data:{ labels, datasets:[{
        label: hourly?'예약건수(시간)':'예약건수(일)', data:values, borderWidth:2, tension:.25,
        backgroundColor:'rgba(17,24,39,0.06)', borderColor:'rgba(17,24,39,0.9)',
        pointRadius: hourly?2:0
      }]},
      options:{
        plugins:{ legend:{ display:false } },
        scales:{ y:{ ticks:{ precision:0 } } },
        interaction:{ mode:'index', intersect:false },
        animation:false, maintainAspectRatio:false, responsive:true
      }
    })
    return
  }
  // 업데이트만
  bkChart.data.labels = labels
  bkChart.data.datasets[0].data = values
  bkChart.data.datasets[0].label = hourly?'예약건수(시간)':'예약건수(일)'
  bkChart.data.datasets[0].pointRadius = hourly?2:0
  bkChart.update('none')
}

function drawDecompChart(rows){
  const labels = rows.map(r=>r.label)
  const values = rows.map(r=>r.gmv)

  if (!decChart) {
    decChart = new Chart(decompChartEl.value, {
      type:'bar',
      data:{ labels, datasets:[{ label:'GMV', data:values, borderWidth:1, backgroundColor:'rgba(17,24,39,0.12)' }]},
      options:{ indexAxis:'y', plugins:{legend:{display:false}},
        scales:{ x:{ ticks:{ callback:v=>Number(v).toLocaleString() } } },
        animation:false, maintainAspectRatio:false
      }
    })
    return
  }

  // ✅ 업데이트만
  decChart.data.labels = labels
  decChart.data.datasets[0].data = values
  decChart.update('none')
}


/* 스파크라인 유틸 */
function sparkPoints(series, W=100, H=28){
  const s = (series||[]).map(n=>Number(n)||0)
  if(!s.length) return []
  const min = Math.min(...s), max = Math.max(...s), rng = (max-min)||1
  return s.map((v,i)=>({ x: (i/(s.length-1||1))*W, y: H - ((v-min)/rng)*(H-4) - 2 }))
}
function sparkPath(series){ return sparkPoints(series).map((p,i)=> (i?'L':'M')+p.x+','+p.y).join(' ') }
function sparkLast(series){ const pts = sparkPoints(series); return pts[pts.length-1] || {x:0,y:0} }

/* 라이프사이클 */
onMounted(() => { loadRegions(); setPreset('7D') })
watch([region, from, to], fetchAll)
watch(rankTab, recomputeRanksTotal)

/* 검색 디바운스 */
let searchTimer
watch(hotelQuery, () => { clearTimeout(searchTimer); searchTimer=setTimeout(fetchAll, 250) })
</script>

<!-- 전역 토큰(프로젝트에 이미 있으면 이 블록 삭제 가능) -->
<style>
:root{
  --bg:#F8FAFC; --card:#FFFFFF; --text:#0F1724; --muted:#6B7280;
  --primary:#2563EB; --primary-600:#1D4ED8;
  --success:#10B981; --danger:#EF4444; --warning:#F59E0B; --info:#06B6D4;
  --ring:#93C5FD; --shadow:0 8px 30px rgba(16,24,40,.06); --radius-lg:14px;
}
* { color: var(--text); }
.btn{ background:var(--primary); color:#fff; box-shadow:var(--shadow); border:0; border-radius:10px; padding:8px 12px; cursor:pointer }
.btn.ghost{ background:#fff; color:var(--text); border:1px solid #E5E7EB; box-shadow:none }
.btn.small{ padding:6px 10px; font-size:12px }
.badge{ border-radius:999px; padding:6px 10px; font-weight:700; font-size:12px }
.badge.success { background:rgba(16,185,129,.12); color:var(--success) }
.badge.danger  { background:rgba(239,68,68,.12); color:var(--danger) }
.badge.warn    { background:rgba(245,158,11,.14); color:var(--warning) }
.badge.info    { background:rgba(6,182,212,.12);  color:var(--info) }
:focus-visible{ outline:3px solid var(--ring); outline-offset:2px; border-radius:8px }
</style>

<style scoped>
/* 레이아웃 */
.dash{ display:flex; flex-direction:column; gap:14px; }
.card{ background:var(--card); border:1px solid #E8ECF3; border-radius:var(--radius-lg); padding:14px }

/* 헤더 */
.page-head{ display:flex; align-items:flex-end; justify-content:space-between; }
.page-head h1{ margin:0; font-size:20px; font-weight:800 }
.page-head .sub{ margin:2px 0 0; color:var(--muted) }
.head-cta{ display:flex; gap:8px }

/* 필터 */
.filters{ padding:14px }
.chips{ display:flex; align-items:center; gap:8px; flex-wrap:wrap; margin-bottom:10px }
.chip{ padding:8px 12px; border-radius:999px; border:1px solid #E5E7EB; background:#fff; cursor:pointer; font-weight:600 }
.chip.active{ background:var(--primary); color:#fff; border-color:var(--primary) }
.date-group{ display:flex; align-items:center; gap:6px }
.date-group input{ padding:8px 10px; border:1px solid #E5E7EB; border-radius:10px }
.dashmark{ color:var(--muted) }
.period{ color:var(--muted); margin-left:6px }

.filters-row{ display:flex; gap:8px; align-items:center; flex-wrap:wrap }
.filters-row .search, .filters-row select{
  padding:8px 10px; border:1px solid #E5E7EB; border-radius:10px; background:#fff
}
.active-filters{ display:flex; gap:6px; align-items:center }
.af{ padding:6px 10px; border-radius:999px; background:#F3F4F6; font-size:12px }
.filters-row .right{ margin-left:auto; display:flex; gap:8px; align-items:center }

/* KPI */
.kpis{ display:grid; grid-template-columns:repeat(4,1fr); gap:12px }
@media (max-width:1200px){ .kpis{ grid-template-columns:repeat(2,1fr) } }
.card.kpi{ cursor:pointer; transition: transform .08s ease, box-shadow .08s ease; }
.card.kpi:hover{ transform: translateY(-1px); box-shadow:var(--shadow) }
.kpi .label{ color:var(--muted); font-size:12px; margin:0 0 6px }
.kpi .value{ margin:0 0 4px; font-size:20px; font-weight:800 }
.delta{ margin:6px 0 0; font-weight:700; font-size:12px }
.delta.up{ color:var(--success) } .delta.down{ color:var(--danger) }

/* 차트 */
.charts{ display:grid; grid-template-columns:2fr 1fr; gap:12px }
@media (max-width:1100px){ .charts{ grid-template-columns:1fr } }
.chart{ min-height:220px }
.chart canvas{ width:100% }

/* 표/랭킹 */
.grid{ display:grid; grid-template-columns:1.6fr .4fr; gap:12px }
@media (max-width:1100px){ .grid{ grid-template-columns:1fr } }
.table table{ width:100%; border-collapse:collapse; font-size:14px }
.table th, .table td{ text-align:left; padding:10px; border-top:1px solid #EEF2F7 }
.table thead th{ background:#FAFBFF; color:var(--muted); font-weight:600; border-top:0 }
.up{ color:var(--success); font-weight:700 }
.down{ color:var(--danger); font-weight:700 }

/* 정산 카드 */
.settle .value{ margin:4px 0 8px; font-size:24px; font-weight:800 }

/* 탭 */
.tabs{ display:flex; gap:8px }
.tab{ padding:6px 10px; border-radius:8px; border:1px solid #E5E7EB; background:#fff; cursor:pointer; font-weight:600 }
.tab.active{ background:var(--primary); color:#fff; border-color:var(--primary) }

/* Drawer + Overlay */
.overlay{ position:fixed; inset:0; background:rgba(15,23,36,.28); z-index:39 }
.drawer{
  position: fixed; top:0; right:0; height:100vh; width:420px; max-width:95vw;
  background:#fff; border-left:1px solid #E5E7EB; padding:14px; box-shadow:-6px 0 24px rgba(0,0,0,.06); z-index:40;
  display:flex; flex-direction:column; gap:10px;
}
.drawer header{ display:flex; align-items:center; justify-content:space-between }
.mini-table{ width:100%; border-collapse:collapse; font-size:14px }
.mini-table th,.mini-table td{ padding:10px; border-top:1px solid #EEF2F7 }
.mini-table .right{ text-align:right }

/* 전환 */
.slide-enter-from, .slide-leave-to{ transform:translateX(100%) }
.slide-enter-active, .slide-leave-active{ transition: transform .2s ease }
.fade-enter-from, .fade-leave-to{ opacity:0 }
.fade-enter-active, .fade-leave-active{ transition: opacity .15s ease }
.chart canvas { width:100%; height:160px !important; }
.mini-chart canvas { width:100%; height:140px !important; }
/* 스켈레톤 */
.skeleton{ position:relative; overflow:hidden; color:transparent !important }
.skeleton::after{
  content:''; position:absolute; inset:0;
  background:linear-gradient(90deg,#f3f4f6 25%,#e5e7eb 37%,#f3f4f6 63%); background-size:400% 100%;
  animation:shimmer 1.4s infinite;
}
@keyframes shimmer{ 0%{background-position:100% 0} 100%{background-position:-100% 0} }
</style>

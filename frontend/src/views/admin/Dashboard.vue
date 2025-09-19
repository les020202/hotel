<template>
    <div class="dash">
      <!-- 필터바 -->
      <section class="filters">
        <div class="chips">
          <button class="chip" :class="{active: preset==='TODAY'}"  @click="setPreset('TODAY')">TODAY</button>
          <button class="chip" :class="{active: preset==='7D'}"     @click="setPreset('7D')">7D</button>
          <button class="chip" :class="{active: preset==='30D'}"    @click="setPreset('30D')">30D</button>
          <button class="chip" :class="{active: preset==='CUSTOM'}" @click="preset='CUSTOM'">CUSTOM</button>
  
          <input type="date" v-model="from" :disabled="preset!=='CUSTOM'">
          <input type="date" v-model="to"   :disabled="preset!=='CUSTOM'">
        </div>
  
        <div class="filters-row">
          <select v-model="region">
            <option value="">지역(ALL)</option>
            <option>서울</option>
            <option>부산</option>
            <option>제주</option>
            <option>대구</option>
          </select>
  
          <input class="search" v-model.trim="hotelQuery" placeholder="호텔명/ID 검색" />
  
          <select v-model="pg">
            <option value="">PG(ALL)</option>
            <option>토스</option>
            <option>KG</option>
          </select>
  
          <div class="right">
            <button class="btn ghost" @click="saveFilter">필터 저장</button>
            <button class="btn ghost" @click="loadFilter">저장된 필터 불러오기</button>
            <button class="btn" @click="exportCsv">CSV 내보내기</button>
          </div>
        </div>
      </section>
  
      <!-- KPI 영역 -->
      <section class="kpis">
        <div class="card kpi">
          <p class="label">총 매출(GMV)</p>
          <h3 class="value">{{ fmtK( metrics.gmv ) }}</h3>
        </div>
        <div class="card kpi">
          <p class="label">환불</p>
          <h3 class="value">{{ fmtK( metrics.refund ) }}</h3>
        </div>
        <div class="card kpi">
          <p class="label">순매출(Net)</p>
          <h3 class="value">{{ fmtK( metrics.net ) }}</h3>
        </div>
        <div class="card kpi">
          <p class="label">오늘 수수료</p>
          <h3 class="value">{{ fmtK( metrics.fee ) }}</h3>
        </div>
        <div class="card kpi">
          <p class="label">결제 실패율</p>
          <h3 class="value">{{ metrics.failRate.toFixed(1) }}%</h3>
        </div>
        <div class="card kpi">
          <p class="label">쿠폰 사용액</p>
          <h3 class="value">{{ fmtK( metrics.coupon ) }}</h3>
        </div>
      </section>
  
      <!-- 랭킹 & 차트 -->
      <section class="grid">
        <div class="card table">
          <div class="card-title">
            <strong>요새 핫한 호텔 (연속 매진 랭킹)</strong>
          </div>
          <table>
            <thead>
              <tr>
                <th>#</th><th>호텔명</th><th>지역</th><th>연속 매진</th><th>매진일 평균가</th><th>기간 GMV</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(h,i) in hotHotels" :key="h.id">
                <td>{{ i+1 }}</td>
                <td>{{ h.name }}</td>
                <td>{{ h.region || '-' }}</td>
                <td><span class="pill">{{ h.streak }}일</span></td>
                <td>{{ fmtK(h.avgPrice) }}</td>
                <td>{{ fmtK(h.gmv) }}</td>
              </tr>
              <tr v-if="!hotHotels.length">
                <td colspan="6" class="muted">데이터가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
  
        <div class="card chart">
          <div class="card-title"><strong>일별 추이</strong> <span class="muted">(매출/환불)</span></div>
          <canvas ref="chartEl" height="140"></canvas>
        </div>
      </section>
    </div>
  </template>
  
  <script setup>
  import { get } from '@/api/_http'
  import { ref, onMounted, watch } from 'vue'
  import Chart from 'chart.js/auto'
  
  /* ----------- 상태 ----------- */
  const preset = ref('7D')
  const from = ref('')
  const to = ref('')
  const region = ref('')
  const hotelQuery = ref('')
  const pg = ref('')
  const chartEl = ref(null)
  
  const metrics = ref({ gmv:0, refund:0, net:0, fee:0, failRate:0, coupon:0 })
  const hotHotels = ref([])
  let chart
  
  /* ----------- 유틸 ----------- */
  const fmtK = (n) => `${(n||0).toLocaleString()}원`
  const todayStr = () => new Date().toISOString().slice(0,10)
  const deltaDays = (d) => {
    const dt = new Date(); dt.setDate(dt.getDate()-d)
    return dt.toISOString().slice(0,10)
  }
  
  /* ----------- 필터 preset ----------- */
  function setPreset(p) {
    preset.value = p
    if (p==='TODAY'){ from.value = todayStr(); to.value = todayStr() }
    if (p==='7D'){ from.value = deltaDays(6); to.value = todayStr() }
    if (p==='30D'){ from.value = deltaDays(29); to.value = todayStr() }
    fetchAll()
  }
  
  /* ----------- 필터 저장/불러오기/CSV (더미) ----------- */
  function saveFilter(){
    const obj = { preset:preset.value, from:from.value, to:to.value, region:region.value, hotelQuery:hotelQuery.value, pg:pg.value }
    localStorage.setItem('admin_filters', JSON.stringify(obj))
    alert('필터를 저장했어요.')
  }
  function loadFilter(){
    const raw = localStorage.getItem('admin_filters')
    if(!raw) return alert('저장된 필터가 없어요.')
    const f = JSON.parse(raw)
    preset.value=f.preset; from.value=f.from; to.value=f.to; region.value=f.region; hotelQuery.value=f.hotleQuery; pg.value=f.pg
  }
  function exportCsv(){
    alert('CSV는 이후 연결할게요. (표/차트 데이터 직렬화)')
  }
  
  /* ----------- 데이터 로딩 ----------- */
  async function fetchAll(){
    // 1) 기본 개요
    try{
        
      const ov = await get('/admin/overview')
      const gmv = Number(ov.weekRevenue||0)
      const refund = Number(ov.refund||0) || 0        // API 없으면 0
      const coupon = Number(ov.coupon||0) || 0        // API 없으면 0
      const fee = Number(ov.fee||0) || Math.round(gmv * 0.015) // 임시 1.5%
      const failRate = Number(ov.failRate||0) || 0
  
      metrics.value = {
        gmv, refund, net: gmv - refund, fee, failRate, coupon
      }
    }catch(e){ console.warn('overview fail', e) }
  
    // 2) 일별 매출
    try{
      // days 계산 (to - from)
      const d1 = new Date(from.value), d2 = new Date(to.value)
      const days = Math.max(1, Math.round((d2 - d1) / (1000*60*60*24)) + 1)
      const rev = await get(`/admin/revenue/daily?days=${days}`)
      drawChart(rev.labels || [], rev.values || [])
    }catch(e){ console.warn('revenue fail', e); drawChart([],[]) }
  
    // 3) 핫 호텔 (선택 API, 없으면 빈 배열)
    try{
        const top = await get(`/admin/hotels/top?from=${from.value}&to=${to.value}`)
      hotHotels.value = Array.isArray(top) ? top : []
    }catch{ hotHotels.value = [] }
  }
  
  function drawChart(labels, values){
    if (chart) chart.destroy()
    chart = new Chart(chartEl.value, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          { label: '매출(GMV)', data: values, borderWidth: 1 }
          // 필요하면 환불 데이터셋 추가 가능
        ]
      },
      options: {
        responsive: true,
        plugins: { legend: { display:false } },
        scales: {
          y: { ticks: { callback: v => v.toLocaleString() } }
        }
      }
    })
  }
  
  onMounted(() => {
    setPreset('7D') // 초기 로딩
  })
  watch([region, hotelQuery, pg], () => { /* 필터 바뀌면 필요 시 재요청 */ })
  </script>
  
  <style scoped>
  .dash{ display:flex; flex-direction:column; gap:14px; }
  
  /* Filters */
  .filters{ background:#fff; border:1px solid #e8ecf3; border-radius:14px; padding:14px }
  .chips{ display:flex; align-items:center; gap:8px; flex-wrap:wrap; margin-bottom:10px }
  .chip{
    padding:8px 12px; border-radius:999px; border:1px solid #e8ecf3; background:#fff; cursor:pointer;
    font-weight:600; color:#111827;
  }
  .chip.active{ background:#111827; color:#fff; border-color:#111827 }
  .filters input[type="date"]{ padding:8px 10px; border:1px solid #e8ecf3; border-radius:10px }
  .filters-row{ display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
  .filters-row select, .filters-row .search{
    padding:8px 10px; border:1px solid #e8ecf3; border-radius:10px; background:#fff;
  }
  .filters-row .right{ margin-left:auto; display:flex; gap:8px }
  .btn{ padding:8px 12px; border-radius:10px; border:0; color:#fff; background:#111827; cursor:pointer }
  .btn.ghost{ background:#fff; color:#111827; border:1px solid #e8ecf3 }
  
  /* KPI */
  .kpis{
    display:grid; grid-template-columns:repeat(6,1fr); gap:12px;
  }
  @media (max-width:1200px){ .kpis{ grid-template-columns:repeat(3,1fr) } }
  @media (max-width:700px){ .kpis{ grid-template-columns:repeat(2,1fr) } }
  .card{
    background:#fff; border:1px solid #e8ecf3; border-radius:14px; padding:14px;
  }
  .kpi .label{ color:#6b7280; font-size:12px; margin:0 0 6px }
  .kpi .value{ margin:0; font-size:20px; font-weight:800 }
  
  /* Grid */
  .grid{
    display:grid; grid-template-columns:1.15fr .85fr; gap:12px;
  }
  @media (max-width:1100px){ .grid{ grid-template-columns:1fr } }
  
  .card-title{ display:flex; align-items:center; justify-content:space-between; margin-bottom:10px }
  .muted{ color:#9ca3af }
  
  .table table{ width:100%; border-collapse:collapse; font-size:14px }
  .table th, .table td{ text-align:left; padding:10px; border-top:1px solid #eef2f7 }
  .table thead th{ background:#fafbff; color:#6b7280; font-weight:600; border-top:0 }
  .pill{
    display:inline-block; padding:2px 8px; border-radius:999px; background:#111827; color:#fff; font-size:12px
  }
  
  /* Chart */
  .chart canvas{ width:100% }
  </style>
  
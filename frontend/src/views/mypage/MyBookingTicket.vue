<template>
    <div class="ticket-page">
      <div class="toolbar no-print">
        <button class="btn" @click="goBack">← 목록</button>
        <div class="spacer"></div>
        <button class="btn ghost" @click="printPage">인쇄 / PDF 저장</button>
      </div>
  
      <!-- 화면에 보이는 티켓 -->
      <div id="print-root">
        <div id="printable-ticket" ref="printableTicket" class="ticket-wrap">
          <div class="ticket-card">
            <!-- 좌측 날짜 -->
            <section class="col col-left">
              <div class="big-date">
                <div class="dow">{{ dow(checkIn) }}</div>
                <div class="md">
                  <div class="month">{{ monthShort(checkIn) }}</div>
                  <div class="day">{{ day(checkIn) }}</div>
                </div>
              </div>
  
              <div class="io">
                <div class="label">Check-in</div>
                <div class="time">{{ checkInTime }}</div>
              </div>
  
              <div class="sep"></div>
  
              <div class="big-date">
                <div class="dow">{{ dow(checkOut) }}</div>
                <div class="md">
                  <div class="month">{{ monthShort(checkOut) }}</div>
                  <div class="day">{{ day(checkOut) }}</div>
                </div>
              </div>
  
              <div class="io">
                <div class="label">Check-out</div>
                <div class="time">{{ checkOutTime }}</div>
              </div>
            </section>
  
            <!-- 중앙 -->
            <section class="col col-mid">
              <div class="header">
                <div class="avatar">{{ initials(displayName) }}</div>
                <div class="name">{{ displayName }}</div>
                <div class="room">{{ roomTitle }}</div>
              </div>
  
              <div class="facts">
                <div class="fact">
                  <div class="k">체크인</div>
                  <div class="v">{{ checkInTime }}</div>
                </div>
                <div class="fact">
                  <div class="k">체크아웃</div>
                  <div class="v">{{ checkOutTime }}</div>
                </div>
                <div class="fact">
                  <div class="k">결제</div>
                  <div class="v">{{ payMethod }}</div>
                </div>
              </div>
  
              <div class="code-row">
                <div class="code-left">
                  <div class="pnr">{{ pnr }}</div>
                  <div class="sub">예약번호</div>
                </div>
                <div class="barcode" aria-label="barcode"></div>
              </div>
            </section>
  
            <!-- 우측 -->
            <section class="col col-right">
              <div class="brand">
                <div class="hotel">{{ hotelName }}</div>
                <div class="city">{{ hotelCity }}</div>
              </div>
            </section>
          </div>
        </div>
      </div>
  
      <p v-if="error" class="error no-print">{{ error }}</p>
    </div>
  </template>
  
  <script setup>
  import { computed, ref, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { get } from '@/api/_http'
  
  const route = useRoute()
  const router = useRouter()
  const id = route.params.id
  
  const printableTicket = ref(null)
  const booking = ref(null)
  const error = ref('')
  
  function getCurrentUserName() {
    try {
      const up = JSON.parse(localStorage.getItem('userProfile') || 'null')
      return up?.name || up?.username || up?.userName || up?.nickname || ''
    } catch { return '' }
  }
  
  onMounted(async () => {
    try {
      const res = await get(`/my/bookings/${id}`)
      booking.value = res
    } catch (e) {
      console.error(e)
      error.value = e?.response?.data?.message || e.message || '불러오기 실패'
    }
  })
  
  /* 표시값 */
  const checkIn = computed(() => booking.value?.checkIn || '')
  const checkOut = computed(() => booking.value?.checkOut || '')
  const checkInTime = computed(() => booking.value?.checkInTime || '15:00')
  const checkOutTime = computed(() => booking.value?.checkOutTime || '11:00')
  const roomTitle = computed(() => booking.value?.roomTypeName || booking.value?.roomTitle || '')
  const payMethod = computed(() => booking.value?.paymentMethod || 'On arrival')
  const pnr = computed(() => booking.value?.pnr || booking.value?.bookingCode || booking.value?.bookingId || '')
  const hotelName = computed(() => booking.value?.hotelName || 'HOTEL')
  const hotelCity = computed(() => booking.value?.hotelCity || booking.value?.hotelAddress || '')
  const displayName = computed(() => booking.value?.guestName || getCurrentUserName() || 'Guest')
  
  function goBack() {
    router.back()
  }
  
  /** 인쇄: 페이지(A4 가로) 기준, 상단배치(공백 제거), 1페이지 */
  function printPage() {
    const node = printableTicket.value
    if (!node) return
  
    const w = window.open('', 'PRINT', 'width=1200,height=900')
    if (!w) return
  
    const html = `
  <!doctype html>
  <html>
  <head>
    <meta charset="utf-8" />
    <title>Ticket</title>
    <style>
      * { box-sizing: border-box; }
      html, body { margin: 0; padding: 0; background: #fff; }
      body {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
        font-family: system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
      }
  
      /* ✅ 페이지 기준: A4 가로, 여백 10mm */
      @page { size: A4 landscape; margin: 10mm; }
  
      /* 상단부터 배치(가운데 정렬 제거 → 밑 공백 제거) */
      .print-area {
        padding: 0; margin: 0;
      }
  
      /* 티켓을 페이지 너비에 맞추고, 높이는 자동(디자인 유지) */
      .ticket-wrap { display: block; }
      .ticket-card {
        width: 100%;        /* 페이지 프린트 영역 너비 100% */
        height: auto;       /* 비율 유지 */
        border: 0.6mm solid #e5e7eb;
        border-radius: 8mm;
        display: grid; grid-template-columns: 26% 1fr 26%;
        overflow: hidden;
  
        page-break-inside: avoid; break-inside: avoid;
        margin: 0;          /* 불필요 여백 제거 */
      }
  
      /* 내부 스타일 (화면과 동일 톤) */
      .col { padding: 18px; }
      .col-left { background:#f8fafc; border-right:1px solid #eef2f7; }
      .big-date { display:flex; align-items:center; gap:12px; }
      .dow { font-weight:700; color:#111827; }
      .md { display:flex; align-items:baseline; gap:8px; }
      .month { font-size: 20px; font-weight: 700; color:#111827; }
      .day { font-size: 28px; font-weight: 800; color:#111827; }
      .io { margin-top:10px; }
      .io .label { font-size:12px; color:#64748b; }
      .io .time { font-weight:700; color:#111827; }
      .sep { height:1px; background:#e5e7eb; margin:14px 0; }
  
      .col-mid { background:#e8f5ef; }
      .header { display:flex; align-items:center; gap:10px; }
      .avatar { width:28px; height:28px; border-radius:50%; background:#d1fae5; display:grid; place-items:center; font-weight:700; }
      .name { font-weight:700; }
      .room { margin-left:auto; font-size:12px; color:#374151; }
  
      .facts { display:flex; gap:18px; margin:14px 0 10px; }
      .fact .k { font-size:12px; color:#64748b; }
      .fact .v { font-weight:700; color:#111827; }
  
      .code-row { display:flex; align-items:center; gap:12px; }
      .code-left .pnr { font-size:22px; font-weight:800; letter-spacing:1px; }
      .code-left .sub { font-size:12px; color:#6b7280; }
      .barcode {
        flex:1; height:36px;
        background:
          repeating-linear-gradient(to right,
            #111827 0, #111827 2px,
            transparent 2px, transparent 6px);
        border-radius:4px;
      }
  
      .col-right { background:#ffffff; display:grid; place-items:center; }
      .brand { text-align:center; }
      .brand .hotel { font-weight:800; letter-spacing:1px; }
      .brand .city { font-size:12px; color:#6b7280; }
  
      /* 화면용 여백이 프린트에 섞이지 않도록 보장 */
      .no-print { display: none !important; }
    </style>
  </head>
  <body>
    <div class="print-area">
      ${node.outerHTML}
    </div>
    <script>
      window.addEventListener('load', () => {
        setTimeout(() => { window.print(); window.close(); }, 80);
      });
    <\/script>
  </body>
  </html>`
    w.document.open(); w.document.write(html); w.document.close()
  }
  
  /* helpers */
  function fmtDateParts(d) {
    if (!d) return null
    const [y, m, day] = String(d).split('-').map(Number)
    const dt = new Date(y, (m ?? 1) - 1, day ?? 1)
    if (Number.isNaN(dt.getTime())) return null
    return dt
  }
  function dow(d) {
    const dt = fmtDateParts(d)
    return dt ? dt.toLocaleDateString('en-US', { weekday: 'short' }) : ''
  }
  function monthShort(d) {
    const dt = fmtDateParts(d)
    return dt ? dt.toLocaleDateString('en-US', { month: 'short' }) : ''
  }
  function day(d) {
    const dt = fmtDateParts(d)
    return dt ? String(dt.getDate()).padStart(2, '0') : ''
  }
  function initials(name) {
    if (!name) return 'G'
    const parts = String(name).trim().split(/\s+/)
    return (parts[0]?.[0] || 'G').toUpperCase()
  }
  </script>
  
  <style scoped>
  /* 화면용 툴바 */
  .toolbar { display:flex; gap:8px; align-items:center; padding:12px; }
  .toolbar .spacer { flex:1; }
  .btn { padding:8px 12px; border-radius:8px; border:0; background:#0a6; color:#fff; cursor:pointer; }
  .btn.ghost { background:#111827; color:#fff; }
  
  .ticket-page { padding: 8px 12px 24px; }
  .ticket-wrap { display:flex; justify-content:center; }
  
  /* 화면 표시용 크기(웹뷰) */
  .ticket-card {
    width: 980px;             /* 화면 규격은 여전히 고정 */
    background: #fff; border:1px solid #e5e7eb; border-radius: 10px;
    display: grid; grid-template-columns: 260px 1fr 260px;
    overflow: hidden;
  }
  
  .col { padding: 18px; }
  .col-left { background:#f8fafc; border-right:1px solid #eef2f7; }
  .big-date { display:flex; align-items:center; gap:12px; }
  .dow { font-weight:700; color:#111827; }
  .md { display:flex; align-items:baseline; gap:8px; }
  .month { font-size: 20px; font-weight: 700; color:#111827; }
  .day { font-size: 28px; font-weight: 800; color:#111827; }
  .io { margin-top:10px; }
  .io .label { font-size:12px; color:#64748b; }
  .io .time { font-weight:700; color:#111827; }
  .sep { height:1px; background:#e5e7eb; margin:14px 0; }
  
  .col-mid { background:#e8f5ef; }
  .header { display:flex; align-items:center; gap:10px; }
  .avatar { width:28px; height:28px; border-radius:50%; background:#d1fae5; display:grid; place-items:center; font-weight:700; }
  .name { font-weight:700; }
  .room { margin-left:auto; font-size:12px; color:#374151; }
  
  .facts { display:flex; gap:18px; margin:14px 0 10px; }
  .fact .k { font-size:12px; color:#64748b; }
  .fact .v { font-weight:700; color:#111827; }
  
  .code-row { display:flex; align-items:center; gap:12px; }
  .code-left .pnr { font-size:22px; font-weight:800; letter-spacing:1px; }
  .code-left .sub { font-size:12px; color:#6b7280; }
  .barcode {
    flex:1; height:36px;
    background:
      repeating-linear-gradient(to right,
        #111827 0, #111827 2px,
        transparent 2px, transparent 6px);
    border-radius:4px;
  }
  
  .col-right { background:#ffffff; display:grid; place-items:center; }
  .brand { text-align:center; }
  .brand .hotel { font-weight:800; letter-spacing:1px; }
  .brand .city { font-size:12px; color:#6b7280; }
  
  .error { color:#dc2626; padding: 8px 12px; }
  </style>
  
  <!-- 전역 프린트 컬러 유지(충돌 방지용 최소화) -->
  <style>
  @media print {
    * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
  }
  </style>
  
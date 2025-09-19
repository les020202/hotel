<!-- src/views/admin/ReviewModeration.vue -->
<template>
    <div class="wrap">
      <header class="topbar">
        <div class="titles">
          <h1>리뷰 / 신고 관리</h1>
          <p class="sub">실시간 검색·필터 / 관리자 전용</p>
        </div>
  
        <div class="tabs">
          <button
            class="tab"
            :class="{active: tab==='reviews'}"
            @click="tab='reviews'; fetchReviews()">
            리뷰 관리
          </button>
          <button
            class="tab"
            :class="{active: tab==='reports'}"
            @click="tab='reports'; fetchReports()">
            신고 관리
          </button>
        </div>
      </header>
  
      <!-- 필터/검색 -->
      <section class="card filters">
        <div class="row">
          <input class="search" v-model.trim="q" placeholder="호텔명/작성자/내용/사유 검색" />
          <div class="sp"></div>
  
          <label class="chip">
            <span>최소평점</span>
            <select v-model.number="minRating">
              <option :value="null">전체</option>
              <option v-for="n in [1,2,3,4,5]" :key="'min'+n" :value="n">{{n}}</option>
            </select>
          </label>
          <label class="chip">
            <span>최대평점</span>
            <select v-model.number="maxRating">
              <option :value="null">전체</option>
              <option v-for="n in [1,2,3,4,5]" :key="'max'+n" :value="n">{{n}}</option>
            </select>
          </label>
  
          <!-- 리뷰 탭 전용: 공개여부 -->
          <label v-if="tab==='reviews'" class="chip">
            <span>공개여부</span>
            <select v-model="visible">
              <option :value="null">전체</option>
              <option :value="true">공개</option>
              <option :value="false">숨김</option>
            </select>
          </label>
  
          <!-- 신고 탭 전용: OWNER만 -->
          <label v-if="tab==='reports'" class="chip">
            <input type="checkbox" v-model="ownerOnly" />
            <span>OWNER 신고만</span>
          </label>
        </div>
      </section>
  
      <!-- 표 -->
      <section class="card table">
        <div class="table-head" v-if="tab==='reviews'">
          <span>#</span>
          <span>호텔</span>
          <span>작성자</span>
          <span class="center">평점</span>
          <span>내용</span>
          <span class="center">사진</span>
          <span class="center">공개</span>
          <span>작성일</span>
          <span class="center">액션</span>
        </div>
  
        <div class="table-head" v-else>
          <span>#신고</span>
          <span>호텔</span>
          <span>리뷰작성자</span>
          <span class="center">평점</span>
          <span>리뷰내용</span>
          <span>신고자</span>
          <span>사유</span>
          <span>신고일</span>
          <span class="center">액션</span>
        </div>
  
        <!-- 로딩 스켈레톤 -->
        <div v-if="loading" class="skeleton-wrap">
          <div class="skeleton-row" v-for="i in 6" :key="i" />
        </div>
  
        <!-- 리뷰 목록 -->
        <template v-else-if="tab==='reviews'">
          <div
            v-for="r in filteredReviews"
            :key="r.id"
            class="table-row">
            <span class="muted">#{{ r.id }}</span>
            <span class="ellipsis">{{ r.hotelName }}</span>
            <span class="ellipsis">{{ r.userName }}</span>
            <span class="center strong">{{ r.rating }}</span>
            <span class="ellipsis" :title="r.comment || '-'">{{ r.comment || '-' }}</span>
            <span class="center">{{ r.photoCount }}</span>
            <span class="center">
              <span :class="['badge', r.visible ? 'ok' : 'no']">
                {{ r.visible ? '공개' : '숨김' }}
              </span>
            </span>
            <span class="muted">{{ r.createdAt || '-' }}</span>
            <span class="center actions-col">
              <button class="btn danger sm" @click="remove(r.id)">삭제</button>
            </span>
          </div>
          <div v-if="!filteredReviews.length" class="empty">표시할 리뷰가 없습니다.</div>
        </template>
  
        <!-- 신고 목록 -->
        <template v-else>
          <div
            v-for="x in filteredReports"
            :key="`${x.reportId}-${x.id}`"
            class="table-row">
            <span class="muted">#{{ x.reportId }}</span>
            <span class="ellipsis">{{ x.hotelName }}</span>
            <span class="ellipsis">{{ x.userName }}</span>
            <span class="center strong">{{ x.rating }}</span>
            <span class="ellipsis" :title="x.comment || '-'">{{ x.comment || '-' }}</span>
  
            <!-- 신고자 (OWNER 강조) -->
            <span>
              <span
                :class="['badge', x.reporterRole==='ROLE_OWNER' ? 'owner' : '']"
                :title="x.reporterRole">
                {{ x.reporterName }}
              </span>
            </span>
  
            <span class="ellipsis" :title="x.reason || '-'">{{ x.reason || '-' }}</span>
            <span class="muted">{{ x.reportedAt || '-' }}</span>
            <span class="center actions-col">
              <button class="btn danger sm" @click="remove(x.id)">리뷰 삭제</button>
            </span>
          </div>
          <div v-if="!filteredReports.length" class="empty">표시할 신고가 없습니다.</div>
        </template>
      </section>
  
      <!-- 토스트 -->
      <div v-if="toast" class="toast">{{ toast }}</div>
    </div>
  </template>
  
  <script setup>
  import { ref, computed, watch, onMounted } from 'vue'
  import { get, del } from '@/api/_http'   // ENV-B: '/admin/...'로 호출
  
  const tab = ref('reviews')
  const loading = ref(false)
  const q = ref('')
  const minRating = ref(null)
  const maxRating = ref(null)
  const visible = ref(null)      // reviews 탭 전용
  const ownerOnly = ref(false)   // reports 탭 전용
  
  const rowsReviews = ref([])
  const rowsReports = ref([])
  const toast = ref('')
  
  /* 서버에서 받아온 걸 그대로 쓰고, 검색/필터는 클라 실시간 반영 */
  const filteredReviews = computed(() => {
    const k = q.value.toLowerCase()
    return rowsReviews.value.filter(r => {
      if (minRating.value && r.rating < minRating.value) return false
      if (maxRating.value && r.rating > maxRating.value) return false
      if (visible.value !== null && r.visible !== visible.value) return false
      if (!k) return true
      return [r.hotelName, r.userName, r.comment]
        .filter(Boolean)
        .some(v => String(v).toLowerCase().includes(k))
    })
  })
  
  const filteredReports = computed(() => {
    const k = q.value.toLowerCase()
    return rowsReports.value.filter(x => {
      if (minRating.value && x.rating < minRating.value) return false
      if (maxRating.value && x.rating > maxRating.value) return false
      if (ownerOnly.value && x.reporterRole !== 'ROLE_OWNER') return false
      if (!k) return true
      return [x.hotelName, x.userName, x.comment, x.reporterName, x.reason]
        .filter(Boolean)
        .some(v => String(v).toLowerCase().includes(k))
    })
  })
  
  async function fetchReviews() {
    loading.value = true
    try {
      // 서버 필터까지 걸고 싶으면 쿼리스트링 조합해서 넘겨도 됨
      const res = await get('/admin/reviews')
      rowsReviews.value = Array.isArray(res) ? res : []
    } catch (e) {
      rowsReviews.value = []
      console.error(e)
    } finally {
      loading.value = false
    }
  }
  
  async function fetchReports() {
    loading.value = true
    try {
      const res = await get('/admin/reviews/reports')
      rowsReports.value = Array.isArray(res) ? res : []
    } catch (e) {
      rowsReports.value = []
      console.error(e)
    } finally {
      loading.value = false
    }
  }
  
  async function remove(reviewId) {
    if (!confirm('해당 리뷰를 삭제할까요?\n신고/사진 등 연관 데이터도 함께 삭제됩니다.')) return
    try {
      await del(`/admin/reviews/${reviewId}`)
      // 탭별 목록에서 제거
      rowsReviews.value = rowsReviews.value.filter(r => r.id !== reviewId)
      rowsReports.value = rowsReports.value.filter(x => x.id !== reviewId)
      showToast('삭제되었습니다.')
    } catch (e) {
      console.error(e)
      showToast('삭제 실패: ' + (e?.message || '서버 오류'))
    }
  }
  
  function showToast(msg){ toast.value = msg; setTimeout(()=> toast.value='', 1500) }
  
  onMounted(() => {
    fetchReviews()
    fetchReports()
  })
  
  /* 실시간 검색은 computed로 반영, 서버 재조회 원하면 아래 debounce 후 fetch 호출 */
  </script>
  
  <style scoped>
  .wrap{ padding:20px 22px 40px; color:#0f172a; font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial }
  .topbar{
    display:flex; align-items:flex-end; justify-content:space-between; gap:16px;
    margin-bottom:16px; padding:18px 18px 16px; border:1px solid #e7edf7; border-radius:16px;
    background: radial-gradient(800px 260px at 8% 0%, #eef6ff 0%, transparent 60%), linear-gradient(180deg,#fff,#f9fbff);
    box-shadow: 0 10px 24px rgba(15,23,42,.05);
  }
  .titles h1{ margin:0; font-size:20px; font-weight:800 }
  .titles .sub{ margin:4px 0 0; color:#6b7280; font-size:12px }
  
  .tabs{ display:flex; gap:8px }
  .tab{
    padding:8px 12px; border-radius:12px; border:1px solid #e1e8f5; background:#fff; cursor:pointer;
    font-weight:800; color:#0f172a;
  }
  .tab.active{ background:#111827; color:#fff; border-color:#111827 }
  
  .card{ border:1px solid #e7edf7; border-radius:16px; background:#fff; box-shadow:0 12px 28px rgba(15,23,42,.05) }
  
  .filters{ padding:12px 14px; margin-bottom:12px }
  .filters .row{ display:flex; align-items:center; gap:10px; flex-wrap:wrap }
  .search{
    min-width:260px; flex:1 1 280px;
    padding:10px 12px; border-radius:12px; border:1px solid #e1e8f5;
  }
  .sp{ flex:1 }
  
  .chip{
    display:flex; align-items:center; gap:8px; padding:8px 10px;
    border:1px solid #e1e8f5; border-radius:12px; background:#fff;
  }
  .chip select{ border:0; outline:none; background:transparent }
  
  .table{ padding:8px }
  .table-head, .table-row{
    display:grid; grid-template-columns: 80px 1.1fr 1fr 90px 2fr 90px 90px 160px 130px;
    align-items:center; gap:10px; padding:12px;
  }
  .table-head{ position:sticky; top:0; background:#f9fbff; z-index:1; border-radius:12px; font-weight:700; color:#475569 }
  .table-row{ border-top:1px solid #f0f4fb }
  .table-row:hover{ background:#fcfdff }
  .center{ text-align:center }
  .strong{ font-weight:800 }
  .muted{ color:#6b7280 }
  .ellipsis{ overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
  
  .badge{ display:inline-block; padding:3px 10px; border-radius:999px; font-weight:800; font-size:12px; background:#eef2ff; color:#475569 }
  .badge.ok{ background:#0f172a; color:#fff }
  .badge.no{ background:#f1f5f9; color:#64748b }
  .badge.owner{ background:#fef3c7; color:#a16207; border:1px solid #fde68a } /* OWNER 강조 */
  
  .actions-col{ display:flex; align-items:center; justify-content:center; gap:6px }
  .btn{ padding:6px 10px; border-radius:10px; border:0; cursor:pointer; font-weight:800; font-size:12px }
  .btn.danger{ color:#fff; background:#ef4444 }
  
  .empty{ text-align:center; color:#94a3b8; padding:22px }
  .skeleton-wrap{ padding:8px 12px }
  .skeleton-row{ height:46px; border-radius:10px; margin:6px 0; background: linear-gradient(90deg,#f3f6fb 25%,#eaf0f9 37%,#f3f6fb 63%); background-size:400% 100%; animation: shimmer 1.2s infinite }
  @keyframes shimmer{ 0%{ background-position: 100% 0 } 100%{ background-position: 0 0 } }
  
  .toast{
    position:fixed; right:18px; bottom:18px; padding:10px 14px; border-radius:12px;
    color:#0f172a; background:#fff; border:1px solid #e7edf7; box-shadow:0 10px 26px rgba(15,23,42,.18);
    font-weight:700; z-index:60;
  }
  
  /* 좁은 화면 대응 */
  @media (max-width:1200px){
    .table-head, .table-row{
      grid-template-columns: 60px 1fr 1fr 70px 1.6fr 70px 80px 130px 100px;
    }
  }
  @media (max-width:900px){
    .table-head, .table-row{
      grid-template-columns: 60px 1fr .9fr 60px 1.2fr 60px 70px 110px 90px;
    }
  }
  </style>
  
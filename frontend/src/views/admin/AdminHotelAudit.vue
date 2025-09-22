<template>
    <section class="wrap">
      <!-- 상단 카드 헤더 -->
      <div class="hero">
        <div>
          <h2>호텔 심사</h2>
          <p>실시간 검색/필터 · 관리자 전용</p>
        </div>
        <AdminHotelTabs />
      </div>
  
      <!-- 검색/필터 바 -->
      <div class="toolbar">
        <input
          v-model.trim="q"
          class="search"
          placeholder="호텔명/작성자/사유 검색"
          @keyup.enter="load"
        />
  
        <div class="pills">
          <div class="pill">
            <span>상태</span>
            <select v-model="status">
              <option value="">전체</option>
              <option value="PENDING">대기</option>
              <option value="APPROVED">승인</option>
              <option value="REJECTED">반려</option>
            </select>
          </div>
  
          <div class="pill">
            <span>지역</span>
            <select v-model="region">
              <option value="">전체</option>
              <option v-for="r in regions" :key="r" :value="r">{{ r }}</option>
            </select>
          </div>
  
          
  
          <button class="pill ghost" @click="load">검색</button>
        </div>
      </div>
  
      <!-- 표 -->
      <div class="card table-card">
        <table class="table">
          <thead>
            <tr>
              <th style="width:86px">#신청</th>
              <th>호텔</th>
              <th>신청자</th>
              <th>지역</th>
              <th>신청일</th>
              <th style="width:220px">액션</th>
            </tr>
          </thead>
  
          <tbody>
            <tr v-for="it in rows" :key="it.id">
              <td>#{{ it.id }}</td>
              <td class="b">
                <button class="link" @click="openDetail(it.id)">{{ it.name }}</button>
              </td>
              <td>{{ it.applicantName || ('USER#'+it.applicantUserId) }}</td>
              <td>{{ it.region }}</td>
              <td>{{ fmt(it.createdAt) }}</td>
              <td>
                <button class="btn xs" @click="approve(it.id)">승인</button>
                <button class="btn xs danger" @click="openReject(it.id)">반려</button>
              </td>
            </tr>
  
            <tr v-if="!loading && rows.length===0">
              <td colspan="6" class="empty">표시할 신청이 없습니다.</td>
            </tr>
          </tbody>
        </table>
  
        <div v-if="loading" class="loading">불러오는 중…</div>
      </div>
  
      <!-- 상세 모달 -->
      <dialog ref="detailDlg" class="modal">
        <div class="modal-body" v-if="detail">
          <h3>#{{ detail.id }} - {{ detail.name }}</h3>
          <ul class="dl">
            <li><b>지역</b><span>{{ detail.region }}</span></li>
            <li><b>주소</b><span>{{ detail.address }}</span></li>
            <li><b>전화</b><span>{{ detail.phone }}</span></li>
            <li><b>홈페이지</b><span><a :href="detail.homepageUrl" target="_blank">{{ detail.homepageUrl }}</a></span></li>
            <li><b>설명</b><span>{{ detail.description }}</span></li>
            <li><b>상태</b><span>{{ detail.status }}</span></li>
          </ul>
          <div class="btns">
            <button class="btn" @click="detailDlg.close()">닫기</button>
          </div>
        </div>
      </dialog>
  
      <!-- 반려 모달 -->
      <dialog ref="rejectDlg" class="modal">
        <div class="modal-body">
          <h3>반려 사유 입력</h3>
          <textarea v-model.trim="rejectReason" class="textarea" rows="4" placeholder="사유를 입력하세요"></textarea>
          <div class="btns">
            <button class="btn danger" @click="doReject">반려</button>
            <button class="btn" @click="rejectDlg.close()">취소</button>
          </div>
        </div>
      </dialog>
    </section>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import AdminHotelTabs from './AdminHotelTabs.vue'
  
  const status = ref('PENDING')
  const region = ref('')
  const q = ref('')
  
  const rows = ref([])
  const loading = ref(false)
  const regions = ['서울','부산','대구','광주','대전','울산','경기','강원','충북','충남','전북','전남','경북','경남','세종','인천']
  
  const detailDlg = ref(null)
  const rejectDlg = ref(null)
  const detail = ref(null)
  const currentId = ref(null)
  const rejectReason = ref('')
  
  function fmt(iso){ return iso?.replace('T',' ').slice(0,19) || '' }
  function token(){ return localStorage.getItem('token') || '' }
  function authHeaders(){ return { 'Authorization': `Bearer ${token()}`, 'Content-Type':'application/json' } }
  
  async function load(){
    loading.value = true
    try{
      const params = new URLSearchParams()
      if (status.value) params.set('status', status.value)
      if (region.value) params.set('region', region.value)
      if (q.value)      params.set('q', q.value)
      params.set('page','0'); params.set('size','20')
  
      const res = await fetch(`/api/admin/hotel-apps?${params.toString()}`, { headers: authHeaders() })
      if (!res.ok) throw new Error('list failed')
      const data = await res.json()
      rows.value = data?.content || []
    }finally{
      loading.value = false
    }
  }
  
  async function openDetail(id){
    const res = await fetch(`/api/admin/hotel-apps/${id}`, { headers: authHeaders() })
    if (!res.ok) return alert('상세 조회 실패')
    detail.value = await res.json()
    detailDlg.value?.showModal()
  }
  
  async function approve(id){
    if (!confirm('승인하시겠어요?')) return
    const res = await fetch(`/api/admin/hotel-apps/${id}/approve`, {
      method:'POST', headers: authHeaders(), body: JSON.stringify({ note:'' })
    })
    if (!res.ok) return alert('승인 실패')
    await load(); alert('승인되었습니다.')
  }
  
  function openReject(id){
    currentId.value = id
    rejectReason.value = ''
    rejectDlg.value?.showModal()
  }
  async function doReject(){
    if (!rejectReason.value) return alert('사유를 입력하세요.')
    const res = await fetch(`/api/admin/hotel-apps/${currentId.value}/reject`, {
      method:'POST', headers: authHeaders(),
      body: JSON.stringify({ reason: rejectReason.value, note:'' })
    })
    if (!res.ok) return alert('반려 실패')
    rejectDlg.value?.close(); await load(); alert('반려되었습니다.')
  }
  
  onMounted(load)
  </script>
  
  <style scoped>
  :root{
    --line:#e8ecf6; --card:#fff; --muted:#6b7280; --ink:#111827;
  }
  .wrap{ padding:14px }
  
  /* 헤더 카드 */
  .hero{
    display:flex; align-items:center; justify-content:space-between;
    padding:16px 18px; border-radius:16px;
    background:linear-gradient(135deg,#f7faff,#f0f6ff);
    border:1px solid #eaf0ff; margin-bottom:12px;
  }
  .hero h2{ margin:0; font-size:18px; font-weight:800; color:var(--ink) }
  .hero p{ margin:4px 0 0; color:var(--muted); font-size:12px }
  
  /* 툴바 */
  .toolbar{ display:flex; gap:10px; align-items:center; margin-bottom:10px; flex-wrap:wrap }
  .search{ flex:1 1 360px; height:40px; border-radius:12px; border:1px solid var(--line);
    padding:0 14px; outline:none }
  .search:focus{ box-shadow:0 0 0 3px rgba(37,99,235,.1); border-color:#cfe0ff }
  .pills{ display:flex; gap:8px; flex-wrap:wrap }
  .pill{ display:flex; align-items:center; gap:8px; height:40px; padding:0 12px;
    border:1px solid var(--line); background:#fff; border-radius:20px; font-weight:700 }
  .pill select{ border:0; background:transparent; outline:none; font-weight:700 }
  .pill.ghost{ background:#f7faff } .pill.ghost:hover{ background:#eef5ff }
  
  /* 테이블 */
  .card{ background:#fff; border:1px solid var(--line); border-radius:12px }
  .table-card{ overflow:auto }
  .table{ width:100%; border-collapse:collapse }
  th,td{ padding:12px 12px; border-bottom:1px solid #f1f4fb; text-align:left; font-size:14px }
  th{ color:#475569; font-weight:800; background:#fbfdff }
  .b{ font-weight:700 }
  .ellipsis{ white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:520px }
  .empty{ text-align:center; color:#94a3b8; padding:18px 0 }
  .loading{ padding:14px; text-align:center }
  
  /* 버튼 & 모달 */
  .btn{ height:30px; padding:0 10px; border-radius:10px; border:1px solid #cfe0ff;
    background:#f5f9ff; font-weight:700; cursor:pointer; margin-right:6px }
  .btn:hover{ background:#eaf3ff }
  .btn.xs{ height:28px; font-size:12px }
  .btn.danger{ background:#fff5f5; border-color:#fecaca }
  .btn.danger:hover{ background:#ffe9e9 }
  .link{ background:none; border:0; color:#2563eb; text-decoration:underline; cursor:pointer }
  
  .modal{ border:0; border-radius:12px; padding:0; max-width:720px; width:92% }
  .modal-body{ padding:18px }
  .dl{ list-style:none; padding:0; margin:0 }
  .dl li{ display:flex; gap:12px; margin:6px 0 }
  .dl b{ min-width:80px }
  .textarea{ width:100%; border:1px solid var(--line); border-radius:10px; padding:8px; resize:vertical }
  .btns{ display:flex; gap:8px; justify-content:flex-end; margin-top:12px }
  </style>
  
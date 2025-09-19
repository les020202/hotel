<template>
    <section class="wrap">
      <!-- 상단 헤더 -->
      <div class="hero">
        <div>
          <h2>호텔 관리</h2>
          <p>실시간 검색/필터 · 관리자 전용</p>
        </div>
        <AdminHotelTabs />
      </div>
  
      <!-- 검색/필터 바 -->
      <div class="toolbar">
        <input
          v-model.trim="q"
          class="search"
          placeholder="호텔명/주소/전화 검색"
        />
  
        <div class="pills">
          <div class="pill">
            <span>지역</span>
            <select v-model="region">
              <option value="">전체</option>
              <option v-for="r in regions" :key="r" :value="r">{{ r }}</option>
            </select>
          </div>
        </div>
      </div>
  
      <!-- 표 -->
      <div class="card table-card">
        <table class="table">
          <thead>
            <tr>
              <th style="width:80px">#ID</th>
              <th>호텔명</th>
              <th>지역</th>
              <th>주소</th>
              <th style="width:220px">액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="it in rows" :key="it.id">
              <td>{{ it.id }}</td>
              <td class="b">{{ it.name }}</td>
              <td>{{ it.region }}</td>
              <td class="ellipsis">{{ it.address }}</td>
              <td>
                <button class="btn xs" @click="startEdit(it)">수정</button>
                <button class="btn xs danger" @click="removeRow(it)">삭제</button>
              </td>
            </tr>
  
            <tr v-if="!loading && rows.length===0">
              <td colspan="5" class="empty">표시할 호텔이 없습니다.</td>
            </tr>
          </tbody>
        </table>
  
        <div v-if="loading" class="loading">불러오는 중…</div>
      </div>
  
      <!-- 수정 모달 -->
      <div v-if="editOpen" class="modal-backdrop" @click.self="closeEdit">
        <div class="modal">
          <h3>호텔 수정</h3>
          <div class="form">
            <label>호텔명
              <input v-model.trim="editForm.name" class="input" />
            </label>
            <label>지역
              <select v-model="editForm.region" class="input">
                <option v-for="r in regions" :key="r" :value="r">{{ r }}</option>
              </select>
            </label>
            <label>주소
              <input v-model.trim="editForm.address" class="input" />
            </label>
            <label>전화
              <input v-model.trim="editForm.phone" class="input" />
            </label>
            <label>홈페이지
              <input v-model.trim="editForm.homepageUrl" class="input" />
            </label>
          </div>
          <div class="modal-actions">
            <button class="btn" @click="saveEdit">저장</button>
            <button class="btn ghost" @click="closeEdit">취소</button>
          </div>
        </div>
      </div>
    </section>
  </template>
  
  <script setup>
  import { ref, reactive, onMounted, watch } from 'vue'
  import AdminHotelTabs from './AdminHotelTabs.vue'
  
  const rows = ref([])
  const loading = ref(false)
  const q = ref('')
  const region = ref('')
  
  // 제주 제외(예시). DB에 저장된 실제 지역명 접두어가 서울/부산/… 로 시작한다는 가정.
  const regions = [
    '서울','부산','대구','인천','광주','대전','울산',
    '세종','경기','강원','충북','충남','전북','전남','경북','경남'
  ]
  
  // 모달 편집 상태
  const editOpen = ref(false)
  const editId = ref(null)
  const editForm = reactive({
    name: '', region: '', address: '', phone: '', homepageUrl: ''
  })
  
  function authHeaders(){
    const t = localStorage.getItem('token') || ''
    return { 'Authorization': `Bearer ${t}`, 'Content-Type':'application/json' }
  }
  
  async function refresh(){
    loading.value = true
    try{
      const params = new URLSearchParams()
      if (q.value) params.set('q', q.value)
      if (region.value) params.set('region', region.value) // 백엔드가 region LIKE :region% 로 처리
      params.set('page','0')
      params.set('size','1000') // 한번에 많이 가져오도록(85건 대응)
  
      const res = await fetch(`/api/admin/hotels?${params.toString()}`, { headers: authHeaders() })
      if (!res.ok) throw new Error('list failed')
      const data = await res.json()
      rows.value = data?.content || []
    }catch(e){
      console.error(e)
      rows.value = []
      alert('호텔 목록을 불러오지 못했습니다.')
    }finally{
      loading.value = false
    }
  }
  
  // ===== 자동 검색 =====
  // q는 디바운스, region은 즉시
  let tmr = null
  watch(q, () => {
    clearTimeout(tmr)
    tmr = setTimeout(refresh, 300)
  })
  watch(region, () => refresh())
  
  onMounted(refresh)
  
  // ====== 수정/삭제 ======
  async function startEdit(it){
    try{
      const res = await fetch(`/api/admin/hotels/${it.id}`, { headers: authHeaders() })
      if (!res.ok) throw new Error('detail failed')
      const d = await res.json()
      editId.value = d.id
      editForm.name = d.name || ''
      editForm.region = d.region || ''
      editForm.address = d.address || ''
      editForm.phone = d.phone || ''
      editForm.homepageUrl = d.homepageUrl || ''
      editOpen.value = true
    }catch(e){
      console.error(e)
      alert('상세를 불러오지 못했습니다.')
    }
  }
  function closeEdit(){
    editOpen.value = false
    editId.value = null
  }
  
  async function saveEdit(){
    try{
      const payload = {
        name: editForm.name,
        region: editForm.region,
        address: editForm.address,
        phone: editForm.phone,
        homepageUrl: editForm.homepageUrl
      }
      const res = await fetch(`/api/admin/hotels/${editId.value}`, {
        method: 'PUT', headers: authHeaders(), body: JSON.stringify(payload)
      })
      if (!res.ok) throw new Error('update failed')
      editOpen.value = false
      await refresh()
    }catch(e){
      console.error(e)
      alert('수정에 실패했습니다.')
    }
  }
  
  async function removeRow(it){
    if (!confirm('삭제할까요?')) return
    try{
      const res = await fetch(`/api/admin/hotels/${it.id}`, { method:'DELETE', headers: authHeaders() })
      if (!res.ok) throw new Error('delete failed')
      await refresh()
    }catch(e){
      console.error(e)
      alert('삭제 실패')
    }
  }
  </script>
  
  <style scoped>
  :root{ --line:#e8ecf6; --card:#fff; --muted:#6b7280; --ink:#111827; }
  .wrap{ padding:14px }
  .hero{
    display:flex; align-items:center; justify-content:space-between;
    padding:16px 18px; border-radius:16px;
    background:linear-gradient(135deg,#f7faff,#f0f6ff);
    border:1px solid #eaf0ff; margin-bottom:12px;
  }
  .hero h2{ margin:0; font-size:18px; font-weight:800; color:var(--ink) }
  .hero p{ margin:4px 0 0; color:var(--muted); font-size:12px }
  
  .toolbar{ display:flex; gap:10px; align-items:center; margin-bottom:10px; flex-wrap:wrap; }
  .search{
    flex:1 1 360px; height:40px; border-radius:12px; border:1px solid var(--line);
    padding:0 14px; outline:none;
  }
  .search:focus{ box-shadow:0 0 0 3px rgba(37,99,235,.1); border-color:#cfe0ff }
  
  .pills{ display:flex; gap:8px; flex-wrap:wrap }
  .pill{
    display:flex; align-items:center; gap:8px; height:40px; padding:0 12px;
    border:1px solid var(--line); background:#fff; border-radius:20px; font-weight:700;
  }
  .pill select{ border:0; background:transparent; outline:none; font-weight:700; }
  
  .card{ background:#fff; border:1px solid var(--line); border-radius:12px; }
  .table-card{ overflow:auto }
  .table{ width:100%; border-collapse:collapse }
  th,td{ padding:12px 12px; border-bottom:1px solid #f1f4fb; text-align:left; font-size:14px }
  th{ color:#475569; font-weight:800; background:#fbfdff }
  .b{ font-weight:700 }
  .ellipsis{ white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:520px }
  .empty{ text-align:center; color:#94a3b8; padding:18px 0 }
  .loading{ padding:14px; text-align:center }
  
  .btn{
    height:30px; padding:0 10px; border-radius:10px; border:1px solid #cfe0ff;
    background:#f5f9ff; font-weight:700; cursor:pointer; margin-right:6px;
  }
  .btn:hover{ background:#eaf3ff }
  .btn.xs{ height:28px; font-size:12px }
  .btn.ghost{ background:#fff; }
  .btn.danger{ background:#fff5f5; border-color:#fecaca }
  .btn.danger:hover{ background:#ffe9e9 }
  
  /* modal */
  .modal-backdrop{
    position:fixed; inset:0; background:rgba(0,0,0,.35);
    display:flex; align-items:center; justify-content:center; z-index:50;
  }
  .modal{
    width:min(560px,92vw); background:#fff; border-radius:12px; padding:18px;
    border:1px solid var(--line); box-shadow:0 12px 32px rgba(0,0,0,.15);
  }
  .modal h3{ margin:0 0 10px; font-size:18px; font-weight:800 }
  .form{ display:grid; gap:10px; margin:10px 0 }
  .input{
    width:100%; height:40px; border:1px solid var(--line); border-radius:10px; padding:0 12px;
  }
  .modal-actions{ display:flex; justify-content:flex-end; gap:8px }
  </style>
  
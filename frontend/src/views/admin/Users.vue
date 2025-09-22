<template>
    <div class="wrap">
      <header class="topbar">
        <div class="titles">
          <h1>유저 관리</h1>
          <p class="sub">검색: 아이디 / 이메일 / 이름</p>
        </div>
        <div class="actions">
          <div class="search">
            <svg viewBox="0 0 24 24"><path d="M21 21l-3.8-3.8M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15Z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
            <input v-model.trim="q" placeholder="아이디/이메일/이름 검색" />
          </div>
  
          <select v-model="role" class="select">
            <option value="">전체 역할</option>
            <option value="ROLE_USER">ROLE_USER</option>
            <option value="ROLE_ADMIN">ROLE_ADMIN</option>
            <option value="ROLE_OWNER">ROLE_OWNER</option>
          </select>
  
          <select v-model="status" class="select">
            <option value="">전체 상태</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="LOCKED">LOCKED</option>
            <option value="INACTIVE">INACTIVE</option>
            <option value="DELETED">DELETED</option>
          </select>
        </div>
      </header>
  
      <section class="card table">
        <div class="table-head">
          <span>#</span>
          <span>아이디</span>
          <span>이름</span>
          <span>이메일</span>
          <span>전화</span>
          <span>역할</span>
          <span>상태</span>
          <span>가입일</span>
          <span class="center">액션</span>
        </div>
  
        <div v-if="loading" class="skeleton-wrap">
          <div class="skeleton-row" v-for="i in 6" :key="i"></div>
        </div>
  
        <template v-else>
          <div v-for="u in rows" :key="u.id" class="table-row">
            <span class="muted">#{{ u.id }}</span>
            <span class="ellipsis">{{ u.loginId }}</span>
            <span class="ellipsis">{{ u.name }}</span>
            <span class="ellipsis">{{ u.email }}</span>
            <span class="ellipsis">{{ u.phone || '-' }}</span>
  
            <span>
              <select :value="u.role" @change="onChangeRole(u, $event.target.value)" class="pill-select">
                <option value="ROLE_USER">ROLE_USER</option>
                <option value="ROLE_ADMIN">ROLE_ADMIN</option>
                <option value="ROLE_OWNER">ROLE_OWNER</option>
              </select>
            </span>
  
            <span>
              <select :value="u.status" @change="onChangeStatus(u, $event.target.value)" class="pill-select">
                <option value="ACTIVE">ACTIVE</option>
                <option value="LOCKED">LOCKED</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </span>
  
            <span class="muted">{{ u.createdAt }}</span>
  
            <span class="center actions-col">
              <button class="btn danger sm" @click="remove(u)" :disabled="u.status==='DELETED'">삭제</button>
            </span>
          </div>
  
          <div v-if="!rows.length" class="empty">검색 결과가 없습니다.</div>
        </template>
      </section>
  
      <div v-if="toast" class="toast">{{ toast }}</div>
    </div>
  </template>
  
  <script setup>
  import { ref, watch } from 'vue'
  import { searchUsers, updateUserRole, updateUserStatus, deleteUser } from '@/api/adminUsers'
  
  // 상태
  const q = ref('')
  const role = ref('')
  const status = ref('')
  const loading = ref(true)
  const rows = ref([])
  const toast = ref('')
  
  // API 로드 (debounce)
  let timer
  watch([q, role, status], () => {
    clearTimeout(timer)
    timer = setTimeout(load, 250) // 입력 250ms 후 서버 검색
  }, { immediate: true })
  
  async function load() {
    loading.value = true
    try {
      rows.value = await searchUsers({ q: q.value, role: role.value, status: status.value }) || []
    } catch (e) {
      console.error(e)
      rows.value = []
    } finally {
      loading.value = false
    }
  }
  
  async function onChangeRole(u, newRole) {
    try {
      await updateUserRole(u.id, newRole)
      u.role = newRole
      showToast('권한을 변경했습니다.')
    } catch (e) {
      console.error(e); showToast('권한 변경 실패')
    }
  }
  
  async function onChangeStatus(u, newStatus) {
    try {
      await updateUserStatus(u.id, newStatus)
      u.status = newStatus
      showToast('상태를 변경했습니다.')
    } catch (e) {
      console.error(e); showToast('상태 변경 실패')
    }
  }
  
  async function remove(u) {
    if (!confirm(`정말 삭제하시겠어요?\n${u.loginId} (${u.email})`)) return
    try {
      await deleteUser(u.id)
      rows.value = rows.value.filter(r => r.id !== u.id)
      showToast('삭제되었습니다.')
    } catch (e) {
      console.error(e); showToast('삭제 실패')
    }
  }
  
  function showToast(msg){ toast.value = msg; setTimeout(() => toast.value='', 1500) }
  </script>
  
  <style scoped>
  .wrap{ padding:20px 22px 40px; color:#0f172a; font-family: ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial }
  .topbar{
    display:flex; align-items:flex-end; justify-content:space-between; gap:16px;
    margin-bottom:16px; padding:18px 18px 16px; border:1px solid #e7edf7; border-radius:16px;
    background: radial-gradient(800px 260px at 8% 0%, #eef6ff 0%, transparent 60%), linear-gradient(180deg,#fff,#f9fbff);
    box-shadow:0 10px 24px rgba(15,23,42,.05);
  }
  .titles h1{ margin:0; font-size:20px; font-weight:800; letter-spacing:.2px }
  .titles .sub{ margin:4px 0 0; color:#6b7280; font-size:12px }
  .actions{ display:flex; align-items:center; gap:10px }
  .search{
    display:flex; align-items:center; gap:8px; padding:8px 10px; width:260px;
    border:1px solid #e1e8f5; border-radius:12px; background:#fff; color:#6b7280;
  }
  .search svg{ width:18px;height:18px }
  .search input{ border:0; outline:none; flex:1; font-size:14px; background:transparent; color:#0f172a }
  .select{ height:36px; border-radius:10px; border:1px solid #e1e8f5; background:#fff; padding:0 10px }
  
  .card{ border:1px solid #e7edf7; border-radius:16px; background:#fff; box-shadow: 0 12px 28px rgba(15,23,42,.05) }
  .table{ padding:8px 8px 10px }
  
  .table-head,.table-row{
    display:grid; grid-template-columns: 70px 1.1fr 0.9fr 1.6fr 1fr 1fr 1fr 1.2fr 120px;
    align-items:center; gap:10px; padding:12px;
  }
  .table-head{ position:sticky; top:0; background:#f9fbff; z-index:1; border-radius:12px; font-weight:700; color:#475569 }
  .table-row{ border-top:1px solid #f0f4fb }
  .table-row:hover{ background:#fcfdff }
  
  .muted{ color:#6b7280 }
  .center{text-align:center}
  .ellipsis{ overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
  
  .actions-col{ display:flex; align-items:center; justify-content:center; gap:6px }
  .btn{ padding:6px 10px; border-radius:10px; border:0; cursor:pointer }
  .btn.danger{ background:#ef4444; color:#fff }
  .btn.sm{ font-size:12px }
  
  .pill-select{
    height:32px; border-radius:999px; border:1px solid #e4e9f6; background:#fff; padding:0 12px;
    font-weight:700; color:#0f172a;
  }
  
  .empty{ text-align:center; color:#94a3b8; padding:26px }
  .skeleton-wrap{ padding:8px 12px }
  .skeleton-row{ height:46px; border-radius:10px; margin:6px 0; background:linear-gradient(90deg,#f3f6fb 25%,#eaf0f9 37%,#f3f6fb 63%); background-size:400% 100%; animation:shimmer 1.2s infinite }
  @keyframes shimmer{ 0%{background-position:100% 0} 100%{background-position:0 0} }
  
  .toast{ position:fixed; right:18px; bottom:18px; padding:10px 14px; border-radius:12px;
    color:#0f172a; background:#fff; border:1px solid #e7edf7; box-shadow:0 10px 26px rgba(15,23,42,.18); font-weight:700; z-index:60 }
  </style>
  
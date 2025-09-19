<template>
    <div class="admin-shell">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="brand">
          <span class="dot"></span>
          <strong>관리자 페이지</strong>
        </div>
  
        <nav class="menu">
          <RouterLink class="item" to="/admin">
            <span>대시보드</span>
          </RouterLink>
          <RouterLink class="item" to="/admin/bookings">예약 관리</RouterLink>
          <RouterLink class="item" to="/admin/rooms">객실 현황</RouterLink>
          <RouterLink class="item" to="/admin/settlements">정산</RouterLink>
          <RouterLink class="item" to="/admin/users">유저 관리</RouterLink>
          <RouterLink class="item" to="/admin/hotels">호텔 관리</RouterLink>
          <RouterLink class="item" to="/admin/reviews">리뷰/신고</RouterLink>
          <RouterLink class="item" to="/admin/coupons">쿠폰/프로모션</RouterLink>
        </nav>
  
        <div class="ver">v0.1</div>
      </aside>
  
      <!-- Main -->
      <div class="main">
        <header class="topbar">
          <div class="title">
            {{ $route.meta.title || '대시보드' }}
          </div>
          <div class="actions">
            <button class="btn ghost" @click="$router.push('/main')">사이트 보기</button>
            <button class="btn" @click="logout">로그아웃</button>
          </div>
        </header>
  
        <main class="content">
          <RouterView />
        </main>
      </div>
    </div>
  </template>
  
  <script setup>
  import { useRouter, RouterLink, RouterView } from 'vue-router'
  const router = useRouter()
  const logout = () => {
    localStorage.removeItem('token')
    document.cookie = 'refreshToken=; Max-Age=0; path=/;'
    router.push('/login')
  }
  </script>
  
  <style scoped>
  :root{
    --bg:#f7f8fb; --card:#fff; --line:#e8ecf3;
    --text:#111827; --muted:#6b7280;
    --brand:#111827; --brand-accent:#111827;
  }
  
  .admin-shell{ display:flex; min-height:100vh; background:var(--bg); color:var(--text); }
  
  /* Sidebar */
  .sidebar{
    position:sticky; top:0; height:100vh; width:240px;
    background:#fff; border-right:1px solid var(--line);
    display:flex; flex-direction:column;
  }
  .brand{ display:flex; align-items:center; gap:10px; padding:20px 18px; font-size:18px}
  .dot{ width:10px; height:10px; border-radius:50%; background:linear-gradient(135deg,#60a5fa,#3b82f6); box-shadow:0 0 10px #60a5fa; }
  .menu{ padding:8px }
  .item{
    display:block; padding:10px 14px; margin:4px 8px; border-radius:10px; color:#374151;
  }
  .item.router-link-active{ background:#f3f4f6; font-weight:600 }
  .ver{ margin-top:auto; padding:12px 16px; color:#9ca3af; font-size:12px }
  
  /* Main */
  .main{ flex:1; display:flex; flex-direction:column; }
  .topbar{
    height:58px; background:#fff; border-bottom:1px solid var(--line);
    display:flex; align-items:center; justify-content:space-between; padding:0 16px;
  }
  .title{ font-weight:700 }
  .actions{ display:flex; gap:8px }
  .btn{
    padding:8px 12px; border-radius:10px; border:0; color:#fff; background:#111827; cursor:pointer;
  }
  .btn.ghost{ background:#fff; color:#111827; border:1px solid var(--line) }
  .content{ padding:18px }
  </style>
  
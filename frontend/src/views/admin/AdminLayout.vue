<template>
  <div class="admin-shell">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="brand">
        <span class="dot"></span>
        <strong>관리자 페이지</strong>
      </div>

      <nav class="menu">
        <RouterLink class="item" to="/admin">대시보드</RouterLink>
        <RouterLink class="item" to="/admin/bookings">예약 관리</RouterLink>
        <RouterLink class="item" to="/admin/settlements">정산</RouterLink>
        <RouterLink class="item" to="/admin/users">유저 관리</RouterLink>
        <RouterLink class="item" to="/admin/hotels">호텔 관리</RouterLink>
        <RouterLink class="item" to="/admin/reviews">리뷰/신고</RouterLink>
        <RouterLink class="item" to="/admin/coupons">쿠폰/프로모션</RouterLink>
        <RouterLink class="item" to="/admin/support">고객 지원</RouterLink>
      </nav>

      <div class="ver">v0.1</div>
    </aside>

    <!-- Main -->
    <div class="main">
      <header class="topbar">
        <div class="title">{{ $route.meta.title || '대시보드' }}</div>
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
}

/* 전체 레이아웃 */
.admin-shell{
  display:flex;
  min-height:100vh;
  width:100%;
  background:var(--bg);
  color:var(--text);
  overflow-x:hidden; /* 가로 스크롤 방지 */
}

/* Sidebar */
.sidebar{
  position:sticky; top:0;
  height:100vh;
  width:240px; flex-shrink:0;
  background:#fff; border-right:1px solid var(--line);
  display:flex; flex-direction:column;
}
.brand{ display:flex; align-items:center; gap:10px; padding:20px 18px; font-size:18px }
.dot{ width:10px; height:10px; border-radius:50%; background:linear-gradient(135deg,#60a5fa,#3b82f6); box-shadow:0 0 10px #60a5fa; }
.menu{ padding:8px; }
.item{ display:block; padding:10px 14px; margin:4px 8px; border-radius:10px; color:#374151; }
.item.router-link-active{ background:#f3f4f6; font-weight:600 }
.ver{ margin-top:auto; padding:12px 16px; color:#9ca3af; font-size:12px }

/* Main */
.main{
  flex:1; display:flex; flex-direction:column; min-width:0;
}
.topbar{
  height:58px; background:#fff; border-bottom:1px solid var(--line);
  display:flex; align-items:center; justify-content:space-between; padding:0 16px;
}
.title{ font-weight:700 }
.actions{ display:flex; gap:8px }
.btn{ padding:8px 12px; border-radius:10px; border:0; color:#fff; background:#111827; cursor:pointer; }
.btn.ghost{ background:#fff; color:#111827; border:1px solid var(--line) }

/* ✅ 본문을 화면 폭 가득 사용 */
.content{
  flex:1; width:100%; max-width:none;   /* 폭 제한 제거 */
  margin:0; padding:20px 28px;
  overflow:auto;
}

/* ✅ 하위 페이지가 자체적으로 중앙 고정(max-width, margin:auto)을 갖고 있으면 해제 */
:deep(.wrap),
:deep(.container),
:deep(.page){
  max-width:none !important;
  width:100% !important;
  margin:0 !important;
}

/* 표/카드가 스스로 min-width를 강제하는 경우 방지 */
:deep(.table){ min-width:0; table-layout:auto; }


/* ★ 사이드바 너비 변수 */
:root { --sidebar-w: 240px; }

/* 메인 영역이 화면 남는 폭 전부 사용 */
.main {
  width: calc(100vw - var(--sidebar-w));
  max-width: none !important;
}

/* 콘텐츠 박스(페이지 본문)도 가운데 고정 해제 */
.content {
  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;      /* auto 센터링 차단 */
  padding: 18px 24px;        /* 여백만 유지 */
}

/* 각 페이지 최상위 래퍼가 폭을 줄이지 않게 */
:deep(.wrap) {
  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;
}

/* 표 카드도 가로로 꽉 차게(스크롤바 최소화) */
:deep(.table-card), :deep(.table) {
  width: 100% !important;
  max-width: none !important;
}
/* =========================
   AdminLayout.vue 〈style scoped〉 전용
   - 전역X, RouterView 하위에만 적용
   ========================= */

/* 메인 영역은 화면 가로 전부 사용 */
.main { width: calc(100vw - 240px); max-width: none; }
.content { width: 100%; max-width: none; margin: 0; padding: 20px 28px; overflow: auto; }

/* 1) 가운데 고정 해제: 각 페이지의 wrap/container/page 같은 최상위 래퍼를 강제 풀어주기 */
:deep(.wrap),
:deep(.container),
:deep(.page) {
  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;           /* margin:0 auto 제거 */
}

/* Tailwind 등 임의 래퍼도 한 번 더: RouterView의 직계 자식 전부 폭 100% */
.content > :deep(*) {
  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;
}

/* 2) 표를 가로로 꽉 채우기 + 가로 스크롤 최소화 */
:deep(.table-card),
:deep(.table) {
  width: 100% !important;
  max-width: none !important;
  min-width: 0 !important;
  table-layout: auto;
}

/* 3) 헤더/버튼 세로깨짐(한글 한 글자씩 줄바꿈) 방지 */
:deep(.table th),
:deep(.table td) {
  word-break: keep-all;      /* 한글 단어 단위로 */
}
:deep(.table th),
:deep(.btn),
:deep(.badge) {
  white-space: nowrap;       /* 줄바꿈 금지 → 세로로 안 깨짐 */
}

/* (선택) 액션 버튼칸 버튼 줄바꿈 방지 */
:deep(.actions-col) { flex-wrap: nowrap; }

/* (선택) 스크롤바 톤 */
:deep(.table-card) { scrollbar-color: #a4b9d8 #eef2ff; scrollbar-width: thin; }
:deep(.table-card::-webkit-scrollbar){ height:10px; }
:deep(.table-card::-webkit-scrollbar-track){ background:#eef2ff; border-radius:8px; }
:deep(.table-card::-webkit-scrollbar-thumb){ background:#94a3b8; border-radius:8px; }
:deep(.table-card::-webkit-scrollbar-thumb:hover){ background:#c9d4e2; }
</style>

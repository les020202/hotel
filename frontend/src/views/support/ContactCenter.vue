<template>
  <div class="cc">
    <!-- 상단 바 -->
    <div class="topbar">
      <button class="icon" @click="$router.back()">‹</button>
      <div class="title">고객상담센터</div>
      <RouterLink to="/main" class="home-btn">Home</RouterLink>
    </div>

    <!-- 메뉴 리스트 -->
    <div class="list">
      <!-- 1:1 문의 작성 -->
      <button class="row" @click="$router.push('/support/contact/inquiry')">
        <div class="left">
          <div class="main">1:1 문의</div>
          <span class="pill">더 빠르게 상담하세요</span>
        </div>
        <div class="chev">›</div>
      </button>

      <!-- 내 문의함 -->
      <button class="row" @click="$router.push('/support/contact/my')">
        <div class="left">
          <div class="main">내 문의함</div>
          <span class="pill alt">내 문의/답변 확인</span>
        </div>
        <div class="chev">›</div>
      </button>

      <!-- 전화 문의 -->
      <a class="row" href="tel:0216441346">
        <div class="left">
          <div class="main">상담원 전화 문의: <b>02-1644-1346</b></div>
        </div>
        <div class="chev">›</div>
      </a>
    </div>

    <!-- 하위 라우트 표시 -->
    <RouterView />
  </div>
</template>

<script setup>
// 단순 네비게이션
</script>

<style scoped>
.cc{ background:#fff;border:1px solid #e5e7eb;border-radius:16px;overflow:hidden;max-width:900px;margin:0 auto;font-size:18px }
.topbar{ height:64px;display:flex;align-items:center;justify-content:space-between;padding:0 20px;border-bottom:1px solid #f1f5f9 }
.icon{ border:0;background:transparent;font-size:26px;cursor:pointer }
.title{ font-weight:700;font-size:20px }
.home-btn{ font-size:14px;color:#0a6;text-decoration:none;border:1px solid #0a6;padding:6px 12px;border-radius:8px }
.home-btn:hover{ background:#0a6;color:#fff }
.list{ display:flex;flex-direction:column }
.row{ display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #f3f4f6;background:#fff;text-decoration:none;color:#111;font-size:18px }
.row:hover{ background:#fafafa }
.row:last-child{ border-bottom:0 }
.left{ display:flex;align-items:center;gap:12px }
.main{ font-size:18px }
.pill{ font-size:14px;padding:6px 12px;border-radius:999px;background:#2563eb;color:#fff;white-space:nowrap }
.pill.alt{ background:#10b981 }
.chev{ font-size:24px;color:#9ca3af }
</style>

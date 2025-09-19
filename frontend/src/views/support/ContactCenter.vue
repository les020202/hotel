<template>
  <div class="cc">
    <!-- 상단 바 -->
    <div class="topbar">
      <!-- 뒤로가기: 브라우저 히스토리에서 이전 페이지로 이동 -->
      <button class="icon" @click="$router.back()">‹</button>

      <!-- 현재 페이지 제목 -->
      <div class="title">고객센터</div>

      <!-- 홈 버튼: 메인 페이지로 이동 -->
      <RouterLink to="/main" class="home-btn">Home</RouterLink>
    </div>

    <!-- 메뉴 리스트 -->
    <div class="list">
      <!-- 1:1 문의 (내부 라우트 이동) -->
      <button class="row" @click="$router.push('/support/contact/inquiry')">
        <div class="left">
          <div class="main">1:1 문의</div>
          <span class="pill">더 빠르게 상담하세요</span>
        </div>
        <div class="chev">›</div>
      </button>

      <!-- 전화 문의 (외부 tel 링크 연결) -->
      <a class="row" href="tel:0216441346">
        <div class="left">
          <div class="main">
            상담원 전화 문의: <b>02-1644-1346</b>
          </div>
        </div>
        <div class="chev">›</div>
      </a>
    </div>

    <!-- 하위 라우트 표시 영역 (예: 문의 작성 폼) -->
    <RouterView />
  </div>
</template>

<script setup>
// 별도 로직 없음, 단순 UI 전용 컴포넌트
</script>

<style scoped>
/* 전체 컨테이너 */
.cc {
  background:#fff;
  border:1px solid #e5e7eb;
  border-radius:16px;
  overflow:hidden;
  max-width: 900px;      /* 전체 폭 제한 */
  margin: 0 auto;
  font-size: 18px;       /* 기본 글자 크기 */
}

/* 상단바 영역 */
.topbar {
  height:64px;                          /* 높이 */
  display:flex;
  align-items:center;
  justify-content:space-between;
  padding:0 20px;
  border-bottom:1px solid #f1f5f9;
}
.icon {
  border:0;
  background:transparent;
  font-size:26px;                       /* 뒤로가기 버튼 크기 */
  cursor:pointer;
}
.title {
  font-weight:700;
  font-size:20px;                       /* 제목 텍스트 크기 */
}
.home-btn {
  font-size:14px;
  color:#0a6;
  text-decoration:none;
  border:1px solid #0a6;
  padding:6px 12px;
  border-radius:8px;
}
.home-btn:hover {
  background:#0a6;
  color:#fff;
}

/* 메뉴 리스트 */
.list { display:flex; flex-direction:column; }
.row {
  display:flex;
  align-items:center;
  justify-content:space-between;
  padding:20px 24px;                    /* 내부 여백 */
  border-bottom:1px solid #f3f4f6;
  background:#fff;
  text-decoration:none;
  color:#111;
  font-size:18px;                       /* 행 텍스트 크기 */
}
.row:hover { background:#fafafa; }
.row:last-child { border-bottom:0; }

.left { display:flex; align-items:center; gap:12px; }
.main { font-size:18px; }

/* 강조 라벨 (Pill 형태) */
.pill {
  font-size:14px;
  padding:6px 12px;
  border-radius:999px;
  background:#2563eb;
  color:#fff;
  white-space:nowrap;
}

/* 오른쪽 화살표 아이콘 */
.chev {
  font-size:24px;                       /* 크기 */
  color:#9ca3af;
}
</style>

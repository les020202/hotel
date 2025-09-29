<template>
  <div class="support">
    <!-- 상단 제목: 좌측 정렬 -->
    <div class="topbar">
      <div class="topbar-title">고객지원</div>
    </div>

    <!-- 상단 바: 아이콘 + 텍스트, 오른쪽 영업시간 -->
    <div class="bar">
      <div class="items">
        <!-- 공지사항 버튼 -->
        <RouterLink class="item" to="/support/notice">
          <span class="ico">📢</span>
          <span>공지사항</span>
        </RouterLink>

        <div class="divider" />

        <!-- FAQ 버튼 -->
        <RouterLink class="item" to="/support/faq">
          <span class="ico">❓</span>
          <span>자주 묻는 질문</span>
        </RouterLink>

        <div class="divider" />

        <!-- 고객상담센터 버튼 -->
        <RouterLink class="item" to="/support/contact">
          <span class="ico">🎧</span>
          <span>고객상담센터</span>
        </RouterLink>
      </div>

      <!-- 오른쪽 안내 문구 (상담 가능 시간) -->
      <div class="hours">오전 9시 – 다음 날 오전 3시</div>
    </div>

    <!-- 하위 라우트 내용이 들어가는 영역 -->
    <RouterView />
  </div>
</template>

<style scoped>
/* 전체 레이아웃: 세로 정렬 */
.support { display: flex; flex-direction: column; gap: 16px; }

/* === 상단 제목 바: 좌측 정렬 === */
.topbar {
  height: 60px; display: flex; align-items: center; gap: 8px;
  padding: 0 18px; border-bottom: 1px solid #f1f5f9;
  background: #fff; border-radius: 12px;
}
.topbar-title { font-weight: 800; font-size: 20px; text-align: left; }

/* 상단 안내 바 */
.bar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 16px; padding: 16px 18px;
  border: 1px solid #e5e7eb; border-radius: 12px; background: #fff;
}

/* 메뉴 아이템 그룹 */
.items { display: flex; align-items: center; gap: 14px; }

/* 개별 메뉴 버튼 스타일 */
.item {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 10px;
  text-decoration: none; color: #111;
}
.item:hover { background: #f7faf9; }

/* 아이콘 스타일 */
.ico { font-size: 18px; line-height: 1; }

/* 구분선 */
.divider { width: 1px; height: 18px; background: #e5e7eb; }

/* 영업 시간 표시 */
.hours { color: #6b7280; font-size: 14px; white-space: nowrap; }
</style>

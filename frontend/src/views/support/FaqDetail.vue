<!-- src/views/support/FaqDetail.vue -->
<template>
  <div v-if="item" class="faq-detail page">
    <!-- 🔝 상단 바 (공지사항과 동일 디자인) -->
    <div class="topbar">
      <!-- 뒤로가기 버튼 -->
      <button class="icon" @click="$router.back()">‹</button>
      <!-- 페이지 제목 -->
      <div class="title">자주 묻는 질문</div>
      <!-- 홈 버튼 -->
      <RouterLink to="/main" class="home-btn">Home</RouterLink>
    </div>

    <!-- FAQ 본문 -->
    <div class="body">
      <!-- 질문 -->
      <h2 class="q">{{ item.question }}</h2>
      <!-- 카테고리 표시 -->
      <div class="meta">분류: {{ item.category }}</div>
      <!-- 답변 -->
      <div class="a" v-text="item.answer" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/auth'

// 현재 라우트 정보 가져오기
const route = useRoute()
// FAQ 상세 데이터를 담을 상태
const item = ref(null)

// 컴포넌트 마운트 시 API 호출하여 FAQ 상세 불러오기
onMounted(async () => {
  const { data } = await api.get(`/support/faq/${route.params.id}`)
  item.value = data
})
</script>

<style scoped>
/* FAQ 상세 페이지 전체 컨테이너 */
.faq-detail.page{
  max-width: 980px; margin: 0 auto; background:#fff;
  border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,.06);
  overflow: hidden;
}

/* 상단 바 (공지사항과 동일 스타일) */
.topbar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f1f5f9;
}
.icon {
  border: 0;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
}
.title {
  font-weight: 700;
  font-size: 18px;
}
.home-btn {
  font-size: 14px;
  color: #0a6;
  text-decoration: none;
  border: 1px solid #0a6;
  padding: 6px 12px;
  border-radius: 8px;
}
.home-btn:hover {
  background: #0a6;
  color: #fff;
}

/* 본문 영역 */
.body {
  padding: 20px 16px 28px;
}
.q {
  font-size: 20px;
  font-weight: 800;
  margin-bottom: 12px;
}
.meta {
  color: #8a94a6;
  font-size: 13px;
  margin-bottom: 18px;
}
.a {
  font-size: 15px;
  line-height: 1.8;
  color: #222;
  white-space: pre-line; /* 줄바꿈 유지 */
}
</style>

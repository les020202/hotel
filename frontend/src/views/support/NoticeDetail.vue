<!-- src/views/support/NoticeDetail.vue -->
<template>
  <div v-if="item" class="detail">
    <!-- 🔝 상단 네비게이션 -->
    <div class="topbar">
      <!-- 이전 페이지로 이동 (뒤로가기) -->
      <button class="icon" @click="$router.back()">‹</button>
      <!-- 현재 페이지 제목 -->
      <div class="title">공지사항</div>
      <!-- 메인 화면으로 이동 -->
      <RouterLink to="/main" class="home-btn">Home</RouterLink>
    </div>

    <!-- 본문 영역 -->
    <!-- 공지 제목 -->
    <h2 class="notice-title">{{ item.title }}</h2>
    <!-- 작성일 (yyyy-MM-dd 포맷) -->
    <div class="meta">{{ formatDate(item.createdAt) }}</div>
    <!-- 본문 내용 (HTML 포함 가능, v-html 사용) -->
    <div class="content" v-html="item.content"></div>

    <!-- 이전글 / 다음글 네비게이션 -->
    <div class="nav">
      <div class="nav-left">
        <!-- 이전글이 존재할 때만 표시 -->
        <button v-if="nav.prevId" class="nav-btn" @click="goTo(nav.prevId)">‹ 이전글</button>
      </div>
      <div class="nav-right">
        <!-- 다음글이 존재할 때만 표시 -->
        <button v-if="nav.nextId" class="nav-btn" @click="goTo(nav.nextId)">다음글 ›</button>
      </div>
    </div>

    <!-- 목록으로 돌아가기 버튼 -->
    <div class="back">
      <RouterLink to="/support/notice" class="btn">목록으로</RouterLink>
    </div>
  </div>
</template>

<script setup>
/**
 * 공지사항 상세보기 페이지
 * - 선택한 공지의 내용을 표시
 * - 이전글/다음글로 이동 가능
 */
import axios from '../../api/auth'
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

// 라우터 객체
const route = useRoute()
const router = useRouter()

// 현재 공지사항 데이터
const item = ref(null)
// 이전/다음글 id 저장
const nav = ref({ prevId: null, nextId: null })

/** 날짜 포맷팅 함수 */
function formatDate(d) {
  const date = new Date(d)
  return date.toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

/** 공지사항 데이터 로딩 */
async function loadNotice(id) {
  // 공지 상세 조회
  const { data } = await axios.get(`/notices/${id}`)
  item.value = data

  // 이전/다음 글 id 조회 (백엔드 API 필요: GET /notices/{id}/nav)
  const { data: navData } = await axios.get(`/notices/${id}/nav`)
  nav.value = navData
}

/** 특정 id 공지로 이동 */
function goTo(id) {
  router.push(`/support/notice/${id}`)
}

// 페이지 로드시 현재 id로 데이터 로딩
onMounted(() => loadNotice(route.params.id))
// id가 바뀔 때마다 다시 로딩
watch(() => route.params.id, (newId) => { if (newId) loadNotice(newId) })
</script>

<style scoped>
/* 전체 컨테이너 */
.detail {
  background: #fff;
  border-radius: 12px;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  max-width: 900px;
  margin: 0 auto;
  min-height: 70vh;
}

/* 상단 네비게이션 영역 */
.topbar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f1f5f9;
}
.icon { border: 0; background: transparent; font-size: 24px; cursor: pointer; }
.title { font-weight: 700; font-size: 18px; }
.home-btn {
  font-size: 14px; color: #0a6; text-decoration: none;
  border: 1px solid #0a6; padding: 6px 12px; border-radius: 8px;
}
.home-btn:hover { background: #0a6; color: #fff; }

/* 본문 영역 */
.notice-title { font-size: 20px; font-weight: 700; margin: 16px; }
.meta { color: #888; margin: 0 16px 16px; font-size: 13px; }
.content { font-size: 15px; line-height: 1.6; color: #333; margin: 0 16px 24px; }
/* v-html 로 들어오는 <p> 태그에 간격 추가 */
.content :deep(p) { margin-bottom: 12px; }

/* 이전/다음글 네비게이션 */
.nav {
  display: flex;
  justify-content: space-between;
  padding: 0 16px 16px;
}
.nav-left { flex: 1; }
.nav-right { flex: 1; text-align: right; }
.nav-btn {
  background: #f9fafb;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 8px 14px;
  font-size: 14px;
  cursor: pointer;
  transition: background .2s;
}
.nav-btn:hover { background: #f0f0f0; }

/* 목록으로 돌아가기 버튼 */
.back {
  text-align: right;
  padding: 0 16px 20px;
}
.btn {
  display: inline-block;
  padding: 8px 14px;
  border: 1px solid #0a6;
  border-radius: 6px;
  background: #0a6;
  color: #fff;
  text-decoration: none;
  font-size: 14px;
}
.btn:hover { background: #088f58; }
</style>

<!-- src/views/support/NoticeList.vue -->
<template>
  <div class="notice-list">
    <!-- 🔝 상단 바 -->
    <div class="topbar">
      <!-- 이전 페이지(마이페이지 고객지원)으로 이동 -->
      <button class="icon" @click="$router.push('/mypage/support')">‹</button>
      <!-- 현재 페이지 제목 -->
      <div class="title">공지사항</div>
      <!-- 메인 화면으로 이동 -->
      <RouterLink to="/main" class="home-btn">Home</RouterLink>
    </div>

    <!-- 공지 리스트 -->
    <!-- v-for 로 공지 데이터 반복 렌더링 -->
    <div
      class="notice"
      v-for="n in items"
      :key="n.id"
      @click="$router.push(`/support/notice/${n.id}`)"
    >
      <div class="title">
        <!-- 상단 고정 글(pinned) 여부 표시 -->
        <span v-if="n.pinned" class="pin">공지</span>
        {{ n.title }}
      </div>
      <!-- 작성일 표시 -->
      <div class="date">{{ formatDate(n.createdAt) }}</div>
    </div>

    <!-- 더보기 버튼: 마지막 페이지가 아닐 때만 표시 -->
    <div v-if="!last" class="more">
      <button @click="loadMore">더보기</button>
    </div>
  </div>
</template>

<script setup>
/**
 * 공지사항 목록 페이지
 * - 공지 리스트 조회 및 정렬
 * - pinned 글 우선, 최신글이 위로 정렬
 * - "더보기" 버튼으로 페이징 처리
 */
import axios from '../../api/auth'
import { ref, onMounted } from 'vue'

// 공지 데이터 배열
const items = ref([])
// 현재 페이지 번호
const page = ref(0)
// 마지막 페이지 여부
const last = ref(false)

/** 날짜 포맷팅 (yyyy.MM.dd 형태) */
function formatDate(d) {
  return new Date(d).toISOString().split('T')[0].replace(/-/g, '.')
}

/** 공지 데이터 로드 */
async function loadData(reset = false) {
  if (reset) {
    page.value = 0
    items.value = []
    last.value = false
  }

  // 백엔드에서 페이지별 공지사항 가져오기
  const { data } = await axios.get(`/notices?page=${page.value}&size=10`)
  const content = data.content || []

  // ✅ 항상 pinned 우선, 최신글 순 정렬 적용
  items.value = [...items.value, ...content].sort((a, b) => {
    if (a.pinned && !b.pinned) return -1
    if (!a.pinned && b.pinned) return 1
    return b.id - a.id // id 내림차순 (최신글 우선)
  })

  // 마지막 페이지 여부 업데이트
  last.value = data.last
}

/** 더보기 버튼 클릭 시 다음 페이지 로드 */
function loadMore() {
  page.value++
  loadData()
}

// 페이지 로드시 첫 데이터 로딩
onMounted(() => loadData(true))
</script>

<style scoped>
/* 전체 컨테이너 */
.notice-list {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  overflow: hidden;
  max-width: 900px;
  margin: 0 auto;
  font-size: 16px;
}

/* 상단 바 */
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

/* 공지 리스트 아이템 */
.notice {
  padding: 16px 18px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.notice:hover {
  background: #fafafa;
}
.title {
  font-size: 15px;
  font-weight: 500;
}
.pin {
  color: #0a6;
  font-weight: 700;
  margin-right: 6px;
}
.date {
  font-size: 13px;
  color: #999;
}

/* 더보기 버튼 */
.more {
  text-align: center;
  padding: 16px;
}
.more button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fafafa;
  cursor: pointer;
}
.more button:hover {
  background: #f0f0f0;
}
</style>

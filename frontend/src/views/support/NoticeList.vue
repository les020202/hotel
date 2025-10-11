<!-- src/views/support/NoticeList.vue -->
<template>
  <div class="notice-list">
    <!-- 🔝 상단 바 -->
    <div class="topbar">
      <!-- 이전 페이지(마이페이지 고객지원)으로 이동 -->
      <button class="icon" @click="$router.push('/mypage/support')" aria-label="이전으로">‹</button>
      <!-- 현재 페이지 제목 (가운데, 고객상담센터/FAQ와 동일 폰트) -->
      <div class="title">공지사항</div>
    </div>

    <!-- 공지 리스트 -->
    <div
      class="notice"
      v-for="n in items"
      :key="n.id"
      @click="$router.push(`/support/notice/${n.id}`)"
    >
      <div class="title">
        <span v-if="n.pinned" class="pin">공지</span>
        {{ n.title }}
      </div>
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

const items = ref([])
const page = ref(0)
const last = ref(false)

function formatDate(d) {
  return new Date(d).toISOString().split('T')[0].replace(/-/g, '.')
}

async function loadData(reset = false) {
  if (reset) {
    page.value = 0
    items.value = []
    last.value = false
  }

  const { data } = await axios.get(`/notices?page=${page.value}&size=10`)
  const content = data.content || []

  items.value = [...items.value, ...content].sort((a, b) => {
    if (a.pinned && !b.pinned) return -1
    if (!a.pinned && b.pinned) return 1
    return b.id - a.id
  })

  last.value = data.last
}

function loadMore() {
  page.value++
  loadData()
}

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

/* 상단 바: 뒤로가기 좌측 고정, 제목 중앙(고객상담센터/FAQ와 동일 폰트) */
.topbar {
  position: relative;
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;
}
.icon {
  position: absolute;
  left: 16px;
  border: 0;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
  line-height: 1;
}
.topbar .title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-weight: 700;
  font-size: 18px; /* 고객상담센터/FAQ와 동일 */
  line-height: 1;
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
.notice:hover { background: #fafafa; }
.notice .title { font-size: 15px; font-weight: 500; }
.pin { color: #0a6; font-weight: 700; margin-right: 6px; }
.date { font-size: 13px; color: #999; }

/* 더보기 버튼 */
.more { text-align: center; padding: 16px; }
.more button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fafafa;
  cursor: pointer;
}
.more button:hover { background: #f0f0f0; }
</style>

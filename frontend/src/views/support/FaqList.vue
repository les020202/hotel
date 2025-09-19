<template>
  <div class="faq page">
    <!-- 🔝 상단 바 (공지사항과 동일 스타일) -->
    <div class="topbar">
      <button class="icon" @click="$router.back()">‹</button>
      <div class="title">자주 묻는 질문</div>
      <RouterLink to="/main" class="home-btn">Home</RouterLink>
    </div>

    <!-- 카테고리 (격자형, 항공 제외) -->
    <div class="cat-table">
      <button
        v-for="c in categories"
        :key="c.value"
        class="cell"
        :class="{ active: active === c.value }"
        @click="changeCategory(c.value)"
      >
        {{ c.label }}
      </button>
    </div>

    <!-- 질문 리스트 -->
    <div class="list">
      <button
        v-for="f in items"
        :key="f.id"
        class="row"
        @click="$router.push(`/support/faq/${f.id}`)"
      >
        <div class="q">{{ f.question }}</div>
        <div class="chev">›</div>
      </button>

      <!-- 질문이 없을 때 표시 -->
      <div v-if="loaded && items.length === 0" class="empty">
        등록된 질문이 없습니다.
      </div>
    </div>

    <!-- 더보기 버튼 -->
    <div v-if="items.length && !last" class="more">
      <button @click="loadMore">더보기</button>
    </div>
  </div>
</template>

<script setup>
// Vue 3 Composition API
import { ref, onMounted } from 'vue'
// axios 인스턴스 (api/auth.js)
import api from '@/api/auth'

// 카테고리 목록 (항공 제거, 총 5개)
const categories = [
  { label: '전체',        value: 'ALL' },
  { label: '숙소',        value: '숙소' },
  { label: '쿠폰/포인트/코인', value: '쿠폰' },
  { label: '결제/영수증',  value: '결제/영수증' },
  { label: '회원',        value: '회원' },
]

// 현재 선택된 카테고리
const active = ref('ALL')
// FAQ 데이터 리스트
const items = ref([])
// 현재 페이지 번호
const page = ref(0)
// 마지막 페이지 여부
const last = ref(false)
// 로딩 완료 여부
const loaded = ref(false)

// FAQ 데이터 가져오기
async function fetchFaq(reset = false) {
  if (reset) {
    items.value = []
    page.value = 0
    last.value = false
    loaded.value = false
  }

  // 요청 파라미터
  const params = { page: page.value, size: 10 }
  if (active.value !== 'ALL') params.category = active.value

  // API 호출
  const { data } = await api.get('/support/faq', { params })

  // content 필드가 있으면 페이징 구조, 없으면 단순 배열
  const list = data?.content ?? data ?? []

  // 기존 데이터에 이어붙이기
  items.value.push(...list)

  // 마지막 여부 판별
  last.value = data?.last ?? list.length < (params.size || 10)
  loaded.value = true
}

// 카테고리 변경
function changeCategory(v) {
  if (active.value === v) return
  active.value = v
  fetchFaq(true)
}

// 더보기 버튼 클릭 시
function loadMore() {
  page.value++
  fetchFaq()
}

// 컴포넌트 마운트 시 첫 데이터 로드
onMounted(() => fetchFaq(true))
</script>

<style scoped>
.faq.page{
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

/* 카테고리 표(3열) */
.cat-table{
  display:grid; grid-template-columns: repeat(3, 1fr);
  border-top:1px solid #eceff3; border-bottom:1px solid #eceff3;
}
.cell{
  background:#fff; border:0; cursor:pointer;
  padding:12px 14px; text-align:left; font-size:14px; color:#333;
  border-right:1px solid #eceff3; border-bottom:1px solid #eceff3;
}
.cell:nth-child(3n){ border-right:0; }
.cell.active{ background:#f3f7ff; font-weight:700; }

/* 리스트 */
.row{
  width:100%; display:flex; align-items:center; justify-content:space-between;
  padding:16px; border-bottom:1px solid #eef2f7; background:#f7fbff;
  cursor:pointer; text-align:left;
}
.row:hover{ background:#f1f7ff; }
.q{ color:#111; font-size:15px; }
.chev{ color:#9aa4b2; font-size:20px; }
.empty{ padding:22px; color:#666; text-align:center; }

/* 더보기 */
.more{ text-align:center; padding:14px; }
.more button{
  padding:8px 16px; border:1px solid #d1d5db; border-radius:8px;
  background:#fafafa; cursor:pointer;
}
.more button:hover{ background:#f0f0f0; }
</style>

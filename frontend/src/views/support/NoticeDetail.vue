<!-- src/views/support/NoticeDetail.vue -->
<template>
  <div v-if="item" class="detail">
    <!-- 🔝 상단 네비게이션 -->
    <div class="topbar">
      <button class="icon" @click="$router.back()">‹</button>
      <div class="title">공지사항</div>
      <RouterLink to="/main" class="home-btn">Home</RouterLink>
    </div>

    <!-- 본문 -->
    <h2 class="notice-title">{{ item.title }}</h2>
    <div class="meta">{{ formatDate(item.createdAt) }}</div>
    <div class="content" v-html="item.content"></div>

    <!-- ✅ 쿠폰 관련 키워드가 있으면 쿠폰함 버튼 노출 -->
    <div v-if="hasCouponKeyword" class="coupon-cta">
      <div class="cta-text"> 쿠폰함에서 쿠폰 코드를 입력하여 지급받으실 수 있습니다.</div>
      <!-- 필요 시 이 경로를 프로젝트 라우트에 맞게 수정 -->
      <RouterLink to="/mypage/coupon" class="btn coupon-btn">쿠폰함으로 이동</RouterLink>
    </div>

    <!-- 이전/다음글 네비게이션 -->
    <div class="nav">
      <div class="nav-left">
        <button v-if="nav.prevId" class="nav-btn" @click="goTo(nav.prevId)">‹ 이전글</button>
      </div>
      <div class="nav-right">
        <button v-if="nav.nextId" class="nav-btn" @click="goTo(nav.nextId)">다음글 ›</button>
      </div>
    </div>

    <!-- 목록으로 -->
    <div class="back">
      <RouterLink to="/support/notice" class="btn">목록으로</RouterLink>
    </div>
  </div>
</template>

<script setup>
/**
 * 공지사항 상세
 * - 제목/내용에 '쿠폰' 포함 시 쿠폰함 이동 CTA 노출
 */
import axios from '../../api/auth'
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const item = ref(null)
const nav = ref({ prevId: null, nextId: null })

function formatDate(d) {
  const date = new Date(d)
  return date.toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

async function loadNotice(id) {
  const { data } = await axios.get(`/notices/${id}`)
  item.value = data

  const { data: navData } = await axios.get(`/notices/${id}/nav`)
  nav.value = navData
}

function goTo(id) {
  router.push(`/support/notice/${id}`)
}

/** HTML -> 텍스트 변환 (본문에 '쿠폰' 포함 여부 체크용) */
function toPlain(html = '') {
  const div = document.createElement('div')
  div.innerHTML = String(html)
  return (div.textContent || div.innerText || '').trim()
}

/** ✅ 제목이나 본문에 '쿠폰' 포함되면 true */
const hasCouponKeyword = computed(() => {
  if (!item.value) return false
  const title = String(item.value.title || '')
  const bodyText = toPlain(item.value.content || '')
  return title.includes('쿠폰') || bodyText.includes('쿠폰')
})

onMounted(() => loadNotice(route.params.id))
watch(() => route.params.id, (newId) => { if (newId) loadNotice(newId) })
</script>

<style scoped>
.detail {
  background: #fff;
  border-radius: 12px;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  max-width: 900px;
  margin: 0 auto;
  min-height: 70vh;
}
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

.notice-title { font-size: 20px; font-weight: 700; margin: 16px; }
.meta { color: #888; margin: 0 16px 16px; font-size: 13px; }
.content { font-size: 15px; line-height: 1.6; color: #333; margin: 0 16px 24px; }
.content :deep(p) { margin-bottom: 12px; }

/* ✅ 쿠폰 CTA */
.coupon-cta{
  margin: 0 16px 20px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: space-between;
}
.coupon-cta .cta-text{
  color:#0f172a; font-weight:700;
}
.coupon-btn{
  padding: 8px 12px;
  border-radius: 8px;
  background: #1d4ed8;
  color:#fff;
  border: 1px solid #1d4ed8;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
}
.coupon-btn:hover{ filter: brightness(1.05); }

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

.back { text-align: right; padding: 0 16px 20px; }
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

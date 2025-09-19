<script setup>
import { ref, onMounted } from 'vue'
import { getMe, logout as apiLogout } from '@/api/auth'
import { useRouter } from 'vue-router'

// UI 관련 컴포넌트들
import SearchBar from '@/components/SearchBar.vue'
import RegionCards from '@/components/RegionCards.vue'
import RandomHotels from '@/components/RandomHotels.vue'
import PromoSignupCouponBanner from '@/components/PromoSignupCouponBanner.vue'
import HeroSearch from '@/components/HeroSearch.vue'

const router = useRouter()
const me = ref(null)
const msg = ref('')

// 검색 상태
const q         = ref('')
const checkIn   = ref('')
const checkOut  = ref('')
const adults    = ref(2)
const children  = ref(0)
const rooms     = ref(1)

// 사용자 정보 불러오기
async function fetchMe () {
  msg.value = ''
  try {
    me.value = await getMe()
  } catch (e) {
    msg.value = `토큰 확인 실패: ${e?.response?.status || ''}`
  }
}

onMounted(async () => {
  // 1) 소셜 로그인 성공 시 /main#token=... 으로 오므로, 해시에서 토큰 추출
  const m = location.hash.match(/token=([^&]+)/)
  if (m) {
    const token = decodeURIComponent(m[1])
    localStorage.setItem('token', token)
    history.replaceState({}, '', location.pathname) // 주소창에서 해시 제거
  }

  // 2) 저장된 토큰이 전혀 없으면 로그인 페이지로
  if (!localStorage.getItem('token')) {
    router.push('/login')
    return
  }

  // 3) 내 정보 로딩
  await fetchMe()
})

// 로그아웃 처리
async function logout () {
  try {
    // (A) 우리 API 로그아웃 (refreshToken 쿠키 만료)
    await apiLogout().catch(() => {})

    // (B) Spring Security /logout 도 함께 호출 (JSESSIONID 제거)
    await fetch('http://localhost:8888/logout', {
      method: 'POST',
      credentials: 'include'
    }).catch(() => {})
  } finally {
    // (C) 클라이언트 정리
    localStorage.removeItem('token')
    me.value = null
    // 하드 리로드로 세션/리다이렉트 상태 깔끔하게 초기화
    window.location.replace('/login')
  }
}

// 마이페이지 이동 함수
function goMyPage () {
  router.push('/mypage')
}

// 검색 처리 함수
function doSearch() {
  console.log("검색 조건:", q.value, checkIn.value, checkOut.value, adults.value, children.value, rooms.value)
}
</script>

<template>
  <div class="page">
    <div class="card">
      <!-- 헤더 -->
      <div class="head">
        <h1 class="title">메인 페이지</h1>
        <p class="greet" v-if="me">환영합니다, <b>{{ me.name }}</b>님</p>
      </div>

      <!-- 로그인 관련 버튼 -->
      <div class="row gap">
        <button class="btn primary" @click="checkToken">토큰 확인(/api/me)</button>
        <button class="btn" @click="goMyPage">마이페이지로 이동</button>
        <button class="btn" @click="logout">로그아웃</button>
      </div>

      <p class="hint" v-if="msg">{{ msg }}</p>
      <pre v-if="me" class="mt" style="white-space:pre-wrap">{{ JSON.stringify(me, null, 2) }}</pre>
    </div>

    <!-- HeroSearch 컴포넌트 (배경 화면과 검색창) -->
    <HeroSearch :full-bleed="true" class="mb-10" />

    <!-- 상단 검색바 -->
    <SearchBar
      v-model:q="q"
      v-model:checkIn="checkIn"
      v-model:checkOut="checkOut"
      v-model:adults="adults"
      v-model:children="children"
      v-model:rooms="rooms"
      @search="doSearch"
    />

    <!-- 지역 카드 섹션 (검색바 아래에 표시) -->
    <RegionCards class="mt-8" />

    <!-- 프로모션 배너 -->
    <PromoSignupCouponBanner class="mt-8" />

    <!-- 랜덤 5개 호텔 -->
    <RandomHotels class="mt-10" />
  </div>
</template>

<style scoped>
.page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px;
  position: relative;
  overflow: visible;   /* 화살표가 바깥으로 나가도 보이도록 */
}
.mt-8 { margin-top: 32px; }
</style>

<script setup>
import { ref, onMounted } from 'vue'
import { getMe, logout as apiLogout, isLoggedIn, setAuth } from '@/api/auth'
import { useRouter } from 'vue-router'


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
  //   (token= 다음 값만 안전하게 파싱)
  const m = location.hash.match(/(?:^|#|&)token=([^&]+)/)
  if (m) {
    const token = decodeURIComponent(m[1])
    // ✅ 앱 전역 규격: setAuth → localStorage('accessToken') 저장 + 브로드캐스트
    setAuth(token, null)
    // 주소창에서 해시 제거 (쿼리 유지)
    history.replaceState({}, '', location.pathname + location.search)
  }

  // 1-1) (옵션) 예전 로컬 키 'token'을 쓰던 경우 자동 마이그레이션
  const legacy = localStorage.getItem('token')
  if (legacy && !localStorage.getItem('accessToken')) {
    setAuth(legacy, null)
    localStorage.removeItem('token')
  }

  // 2) 저장된 토큰이 전혀 없으면 로그인 페이지로
  if (!isLoggedIn()) {
    router.push('/login')
    return
  }

  // 3) 내 정보 로딩 (토큰이 있을 때만 호출 → 불필요한 401 방지)
  try {
    const me = await getMe()
    // 프로필까지 전역 상태에 반영 (드롭다운 이름 등 즉시 사용)
    setAuth(localStorage.getItem('accessToken'), me)
  } catch {
    // 여기서 401 나면 인터셉터가 refresh 시도 후에도 실패한 케이스.
    // 필요하면 router.push('/login') 등으로 처리 가능.
  }
})
async function logout () {
  try {
    await apiLogout().catch(() => {})
    await fetch('http://localhost:8888/logout', { method: 'POST', credentials: 'include' }).catch(() => {})
  } finally {
    localStorage.removeItem('token')
    me.value = null
    window.location.replace('/login')
  }
}

function goMyPage () {
  router.push('/mypage')
}

function checkToken () {
  fetchMe()
}

function doSearch() {
  console.log("검색 조건:", q.value, checkIn.value, checkOut.value, adults.value, children.value, rooms.value)
}
</script>

<template>
  <div class="page">


    <HeroSearch :full-bleed="true" class="mb-10" />
    <SearchBar
      v-model:q="q"
      v-model:checkIn="checkIn"
      v-model:checkOut="checkOut"
      v-model:adults="adults"
      v-model:children="children"
      v-model:rooms="rooms"
      @search="doSearch"
    />
    <RegionCards class="mt-8" />
    <PromoSignupCouponBanner class="mt-8" />
    <RandomHotels class="mt-10" />
  </div>
</template>

<style scoped>
.page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px;
  position: relative;
  overflow: visible;
}
.mt-8 { margin-top: 32px; }
</style>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { isLoggedIn, setAuth, getMe, logout as apiLogout } from '@/api/auth'

import SearchBar from '@/components/SearchBar.vue'
import RegionCards from '@/components/RegionCards.vue'
import RandomHotels from '@/components/RandomHotels.vue'
import PromoSignupCouponBanner from '@/components/PromoSignupCouponBanner.vue'
import HeroSearch from '@/components/HeroSearch.vue'

const router = useRouter()
const me = ref(null)
const msg = ref('')

// 검색 상태
const q = ref('')
const checkIn = ref('')
const checkOut = ref('')
const adults = ref(2)
const children = ref(0)
const rooms = ref(1)

async function fetchMe () {
  msg.value = ''
  try {
    me.value = await getMe()
  } catch (e) {
    // 토큰이 없거나 만료되면 그냥 무시(메인은 공개 페이지)
    msg.value = ''
  }
}

onMounted(async () => {
  // 소셜 리다이렉트로 들어오는 #token=... 처리 (있을 때만)
  const m = location.hash && location.hash.match(/(?:^|#|&)token=([^&]+)/)
  if (m) {
    const token = decodeURIComponent(m[1])
    setAuth(token, null) // 전역 토큰 저장
    history.replaceState({}, '', location.pathname + location.search) // 해시 제거
  }

  // ✅ 메인은 공개: 토큰이 있으면 프로필만 불러오고, 없으면 아무것도 안 함
  if (isLoggedIn()) {
    try {
      const profile = await getMe()
      setAuth(localStorage.getItem('accessToken') || localStorage.getItem('token'), profile)
      me.value = profile
    } catch { /* 무시 */ }
  }
})

async function logout () {
  try { await apiLogout() } finally {
    // 메인은 공개 페이지이므로 그냥 새로고침/리다이렉트 없이 머물러도 OK
    me.value = null
  }
}

function goMyPage () { router.push('/mypage') }
function checkToken () { fetchMe() }

/* 기존 핸들러 유지(콘솔 출력) */
function doSearch (p) {
  console.log('검색 조건:', p || { q: q.value, checkIn: checkIn.value, checkOut: checkOut.value, adults: adults.value, children: children.value, rooms: rooms.value })
}
</script>

<template>
  <div class="page">
    <HeroSearch :full-bleed="true" class="mb-10" />

    <!-- SearchBar는 자동으로 /search로 이동(autoNavigate=true 기본) + 상위로도 이벤트 발행 -->
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

<script setup>
import { ref, onMounted } from 'vue'
import { getMe, logout as apiLogout } from '@/api/auth'
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
    // 토큰 없거나 실패해도 메인은 공개 페이지이므로 화면은 유지
    msg.value = e?.response?.status ? `토큰 확인 실패: ${e.response.status}` : ''
  }
}

onMounted(async () => {
  // 소셜 로그인 성공 시 /main#token=... 처리
  const m = location.hash.match(/token=([^&]+)/)
  if (m) {
    const token = decodeURIComponent(m[1])
    localStorage.setItem('token', token)
    history.replaceState({}, '', location.pathname)
  }

  // 토큰이 있으면 내 정보만 불러옴(없어도 메인 화면 그대로)
  if (localStorage.getItem('token')) {
    await fetchMe()
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

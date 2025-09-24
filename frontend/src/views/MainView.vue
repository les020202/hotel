<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { isLoggedIn, setAuth, getMe, logout as apiLogout } from '@/api/auth'

import SearchBar from '@/components/SearchBar.vue'
import RegionCards from '@/components/RegionCards.vue'
import RandomHotels from '@/components/RandomHotels.vue'
import PromoSignupCouponBanner from '@/components/PromoSignupCouponBanner.vue'

/* -----------------------------
 *  Hero(기존 HeroSearch 내용)
 * ----------------------------- */
const hero = ref('')
let heroTimer = null

// 커스터마이즈 값(필요시 바꿔도 됨)
const heroImages = [
  'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?q=80&w=1600&auto=format&fit=crop',
  'https://images.unsplash.com/photo-1491553895911-0055eca6402d?q=80&w=1600&auto=format&fit=crop',
  'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?q=80&w=1600&auto=format&fit=crop',
  'https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1?q=80&w=1600&auto=format&fit=crop',
]
const heroHeightPx = 420        // 숫자(px) 또는 '420px' 형태 문자열 사용 가능
const heroRotateMs = 0          // 0이면 자동교체 안 함
const heroTitle = 'hotel'
const heroSubtitle = '쉽고 빠르게 예약'

function pickHero() {
  if (!heroImages.length) return
  hero.value = heroImages[Math.floor(Math.random() * heroImages.length)]
}

/* -----------------------------
 *  메인 기존 로직
 * ----------------------------- */
const router = useRouter()
const me = ref(null)
const msg = ref('')

// 검색 상태(아래 검색바 v-model용)
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
  // Hero 초기화
  pickHero()
  if (heroRotateMs > 0) {
    heroTimer = setInterval(pickHero, heroRotateMs)
  }

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

onUnmounted(() => { if (heroTimer) clearInterval(heroTimer) })

async function logout () {
  try { await apiLogout() } finally {
    // 메인은 공개 페이지이므로 그냥 새로고침/리다이렉트 없이 머물러도 OK
    me.value = null
  }
}

function goMyPage () { router.push('/mypage') }
function checkToken () { fetchMe() }

/* 검색 이동 헬퍼 */
function isoToday () {
  return new Date().toISOString().slice(0,10)
}
function isoPlusDays (n) {
  const d = new Date()
  d.setDate(d.getDate() + n)
  return d.toISOString().slice(0,10)
}

/* SearchBar에서 submit 이벤트 받을 때 호출 */
function goSearch (p) {
  const chosenCheckIn  = p?.checkIn  || checkIn.value  || isoToday()
  const chosenCheckOut = p?.checkOut || checkOut.value || isoPlusDays(1)
  const text = (p?.q ?? q.value ?? '').trim()
  const guestsVal = p?.adults ?? adults.value ?? 1

  router.push({
    path: '/search',
    query: {
      checkIn:  chosenCheckIn,
      checkOut: chosenCheckOut,
      guests:   guestsVal,
      region:   text,
      q:        text,
      offset:   0
    }
  })
}

/* 기존 핸들러 유지(콘솔 출력) */
function doSearch (p) {
  console.log('검색 조건:', p || { q: q.value, checkIn: checkIn.value, checkOut: checkOut.value, adults: adults.value, children: children.value, rooms: rooms.value })
}
</script>

<template>
  <div class="page">
    <!-- ======= Hero (기존 HeroSearch) ======= -->
    <section
      class="hero"
      :style="{ '--hero-h': (typeof heroHeightPx === 'number' ? heroHeightPx + 'px' : String(heroHeightPx)) }"
    >
      <!-- 배경 레이어(여기서만 클리핑) -->
      <div class="hero__bg" :style="{ '--hero-url': `url('${hero}')` }">
        <div class="hero__scrim"></div>
      </div>

      <!-- 실제 콘텐츠 -->
      <div class="hero__inner">
        <h1 class="hero__title">{{ heroTitle }}</h1>
        <p class="hero__subtitle">{{ heroSubtitle }}</p>

        <!-- Hero 내부 검색바: 아래 SearchBar와 동일하게 동작 -->
      </div>
    </section>

    <!-- ======= 아래(기존) 검색바 ======= -->
    <SearchBar
      v-model:q="q"
      v-model:checkIn="checkIn"
      v-model:checkOut="checkOut"
      v-model:adults="adults"
      v-model:children="children"
      v-model:rooms="rooms"
      @submit="goSearch"
      class="mt-8"
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

/* ===== Hero (기존 HeroSearch 스타일) ===== */
.hero{
  position: relative;
  height: var(--hero-h);
  border-radius: 20px;
  /* ✅ 팝오버가 잘리지 않도록 숨김 제거 */
  overflow: visible;
}

/* 배경을 별도 레이어로 분리하여 여기서만 클리핑 */
.hero__bg{
  position:absolute; inset:0;
  border-radius: inherit;
  overflow: hidden;
  background: center/cover no-repeat var(--hero-url);
  z-index: 0;
}

/* 오버레이는 시각적으로만 깔고, 클릭은 통과 */
.hero__scrim{
  position:absolute; inset:0;
  background: linear-gradient(180deg, rgba(0,0,0,.45), rgba(0,0,0,.10));
  pointer-events: none; /* ✅ 클릭 방해 금지 */
}

/* 검색바와 텍스트는 배경 위로 */
.hero__inner{
  position: relative;
  z-index: 1; /* ✅ 확실히 위로 */
  inset:0;
  padding: 24px;
  display:flex; flex-direction:column;
  align-items:center; justify-content:center;
  gap: 18px; color:#fff; text-align:center;
}

.hero__title{ font-size: 38px; font-weight: 800; letter-spacing:-.3px; margin:0; }
.hero__subtitle{ margin:0; opacity:.96; }

/* 팝오버가 겹칠 때를 대비해 z-index 조금 올려 둠 */
.hero__search{
  width: min(980px, 96%);
  background:#fff;
  border-radius: 20px;
  padding: 14px;
  box-shadow: 0 16px 36px rgba(0,0,0,.20);
  position: relative;
  z-index: 10;
}

/* SearchBar 기본 박스(테두리/그림자) 제거해서 깨끗하게 */
:deep(.search-box){ border:none; background:transparent; box-shadow:none; padding:0; }
/* 입력 높이 조금 키우기(원하면 조절) */
:deep(.inp){ height: 52px; }

@media (max-width: 900px){
  .hero{ height: 320px; }
  .hero__title{ font-size: 28px; }
  .hero__search{ padding: 10px; border-radius: 14px; }
}
</style>

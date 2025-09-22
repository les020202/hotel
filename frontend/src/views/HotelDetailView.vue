<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-6 space-y-10">
    <!-- ===== 헤더 ===== -->
    <header class="flex items-start justify-between gap-6">
      <div class="min-w-0">
        <h1 class="text-2xl md:text-3xl font-extrabold truncate">
          {{ hotel?.name || '-' }}
        </h1>

        <div class="mt-2 flex flex-wrap items-center gap-3 text-gray-600">
          <div class="flex items-center gap-1">
            <span v-for="i in (hotel?.gradeLevel || 0)" :key="i" class="text-amber-500">★</span>
            <span class="ml-1 text-sm">{{ hotel?.gradeLevel }} Star Hotel</span>
          </div>
          <span class="text-gray-300">•</span>
          <div class="flex items-center gap-1 min-w-0">
            <span class="text-sm">📍</span>
            <span class="truncate">{{ hotel?.address || hotel?.region || '' }}</span>
          </div>
        </div>

        <div class="mt-2 flex items-center gap-3">
          <div class="border rounded-md px-2 py-1 text-sm font-semibold">
            {{ ratingText }}
          </div>
          <div class="text-sm text-gray-600">{{ ratingLabel(hotel?.rating) }}</div>
        </div>

        <!-- ===== 날짜/인원 바 ===== -->
        <div class="mt-3 flex flex-wrap items-center gap-2 text-sm">
          <input type="date" v-model="ci" :min="todayStr"
                 class="border rounded px-3 py-2" @keyup.enter="applyQuery" />
          <input type="date" v-model="co" :min="ci || todayStr"
                 class="border rounded px-3 py-2" @keyup.enter="applyQuery" />

          <select v-model.number="guests" class="border rounded px-3 py-2">
            <option v-for="n in 6" :key="n" :value="n">{{ n }}명</option>
          </select>

          <button
            class="px-4 py-2 rounded bg-black text-white disabled:opacity-50"
            :disabled="!isValidRange"
            @click="applyQuery"
          >
            적용
          </button>

          <span v-if="!isValidRange" class="ml-2 text-red-600">
            체크아웃은 체크인 이후여야 해요.
          </span>
        </div>
      </div>

      <!-- 시작가/액션 -->
      <div class="text-right shrink-0">
        <div class="text-sm text-gray-500">starting from</div>
        <div class="text-rose-500 text-2xl font-extrabold">
          ₩{{ money(startingFromPerNight) }}<span class="text-base text-gray-500">/night</span>
        </div>

        <!-- ✅ 호텔 단위 찜 토글 버튼 -->
        <div class="mt-2 flex items-center justify-end gap-2">
          <button
            class="border rounded-xl w-10 h-10 grid place-items-center"
            :title="hotelWished ? '찜 해제' : '찜하기'"
            @click="onToggleHotelWish"
          >
            <svg v-if="hotelWished" viewBox="0 0 24 24" class="w-6 h-6 fill-rose-500">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6.5 3.5 5 5.5 5c1.7 0 3.25 1.03 3.97 2.57h1.06C11.25 6.03 12.8 5 14.5 5 16.5 5 18 6.5 18 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" class="w-6 h-6 stroke-gray-700 fill-none">
              <path stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"
                    d="M12.1 20.3C7.14 15.78 4 12.94 4 9.5 4 7.5 5.5 6 7.5 6c1.54 0 3.04.99 3.57 2.36h1.87C13.46 6.99 14.96 6 16.5 6 18.5 6 20 7.5 20 9.5c0 3.44-3.14 6.28-8.1 10.8z"/>
            </svg>
          </button>
          <button class="border rounded-xl w-10 h-10 grid place-items-center">↗</button>
          <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-xl">
            Book now
          </button>
        </div>
      </div>
    </header>

    <!-- ===== 갤러리 ===== -->
    <section class="relative">
      <div class="relative left-1/2 -translate-x-1/2 w-screen">
        <div class="mx-auto w-full max-w-[min(92vw,1600px)] px-6 lg:px-12">
          <div class="relative">
            <transition name="fade" mode="out-in">
              <div :key="pageIndex" class="grid grid-cols-2 md:grid-cols-4 gap-4 lg:gap-6">
                <!-- Slide 1 -->
                <template v-if="pageIndex === 0">
                  <div class="col-span-2 row-span-2 relative overflow-hidden rounded-2xl">
                    <div class="w-full pb-[100%]"></div>
                    <img :src="coverImage" alt="메인"
                         class="absolute inset-0 h-full w-full object-cover object-center" loading="eager" />
                  </div>
                  <div v-for="(img,i) in firstFour" :key="'p0-'+i"
                       class="relative overflow-hidden rounded-2xl">
                    <div class="w-full pb-[100%]"></div>
                    <img :src="img" alt="" class="absolute inset-0 h-full w-full object-cover object-center" />
                  </div>
                </template>

                <!-- Slide 2 -->
                <template v-else>
                  <div v-for="(img,i) in secondEight" :key="'p1-'+i"
                       class="relative overflow-hidden rounded-2xl">
                    <div class="w-full pb-[100%]"></div>
                    <img :src="img" alt="" class="absolute inset-0 h-full w-full object-cover object-center" />
                  </div>
                </template>
              </div>
            </transition>

            <button v-if="pageCount>1"
                    class="hidden md:grid place-items-center absolute -left-1 top-1/2 -translate-y-1/2
                           w-10 h-10 rounded-full bg-white/90 hover:bg-white shadow"
                    @click="prevPage" aria-label="Previous">‹</button>
            <button v-if="pageCount>1"
                    class="hidden md:grid place-items-center absolute -right-1 top-1/2 -translate-y-1/2
                           w-10 h-10 rounded-full bg-white/90 hover:bg-white shadow"
                    @click="nextPage" aria-label="Next">›</button>

            <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 flex gap-2">
              <button v-for="i in 2" :key="'dot-'+i" @click="goPage(i-1)"
                      class="h-2 w-10 rounded-full"
                      :class="(i-1)===pageIndex ? 'bg-neutral-300' : 'bg-neutral-600'"></button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <hr class="border-gray-200 mt-8" />

    <!-- ===== 객실(오퍼) 리스트 ===== -->
    <section>
      <h2 class="text-xl font-bold mb-4">투숙 가능한 옵션</h2>

      <div v-if="loading" class="py-8 text-center text-gray-500">로딩 중…</div>

      <div v-if="!loading && offers.length === 0" class="text-gray-500 py-10 text-center">
        선택한 기간에 판매 가능한 객실이 없습니다.
      </div>

      <ul v-else class="space-y-8">
        <li v-for="t in offers" :key="t.roomTypeId" class="rounded-2xl border shadow-sm p-4 md:p-5">
          <div class="grid grid-cols-12 gap-4 md:gap-6">
            <div class="col-span-12 md:col-span-5">
              <div class="relative h-48 md:h-56 rounded-xl overflow-hidden bg-neutral-100">
                <img :src="safeImg(t.templateImageUrl)" class="w-full h-full object-cover" alt="">
              </div>
              <div class="mt-2 text-sm text-gray-600">
                기준 {{ t.capacity }}인 · {{ t.areaSqm }}㎡ · {{ nights }}박 요금
              </div>
            </div>

            <div class="col-span-12 md:col-span-7 flex flex-col">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="text-lg md:text-xl font-semibold truncate">{{ t.name }}</h3>
                  <div class="mt-1 text-sm text-gray-600">체크인 15:00 ~ 체크아웃 12:00</div>
                </div>
                <div class="text-right shrink-0">
                  <div class="text-rose-600 text-xl md:text-2xl font-extrabold">
                    ₩{{ money(Math.floor((t.priceSum || 0) / nights)) }}
                    <span class="text-sm text-gray-500">/night</span>
                  </div>
                  <div class="text-xs text-gray-500">총 ₩{{ money(t.priceSum) }}</div>
                </div>
              </div>

              <!-- 기존: 객실 카드 내 하트(로컬 토글) 그대로 유지 -->
              <div class="mt-2 text-sm text-amber-600" v-if="t.minRemaining != null && t.minRemaining <= 3">
                남은객실 {{ t.minRemaining }}개
              </div>

              <div class="mt-auto pt-4 flex items-center justify-end gap-3">
                <button
                  class="border rounded-xl w-11 h-11 grid place-items-center"
                  :title="isWishedRoom(t.roomTypeId) ? '찜 해제' : '찜하기'"
                  @click="toggleWishRoom(t.roomTypeId)"
                >
                  <svg v-if="isWishedRoom(t.roomTypeId)" viewBox="0 0 24 24" class="w-6 h-6 fill-rose-500">
                    <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6.5 3.5 5 5.5 5c1.7 0 3.25 1.03 3.97 2.57h1.06C11.25 6.03 12.8 5 14.5 5 16.5 5 18 6.5 18 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" class="w-6 h-6 stroke-gray-700 fill-none">
                    <path stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"
                          d="M12.1 20.3C7.14 15.78 4 12.94 4 9.5 4 7.5 5.5 6 7.5 6c1.54 0 3.04.99 3.57 2.36h1.87C13.46 6.99 14.96 6 16.5 6 18.5 6 20 7.5 20 9.5c0 3.44-3.14 6.28-8.1 10.8z"/>
                  </svg>
                </button>
                <button class="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-3 rounded-xl">예약하기</button>
              </div>
            </div>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getHotelDetail } from '@/api/hotelApi'

/* ✅ 위시리스트 + 인증 */
import { ensureWishlistLoaded, isWished, toggleWishlist } from '@/api/wishlistApi'
import { isLoggedIn } from '@/api/auth'

// ===== 상태 =====
const route   = useRoute()
const router  = useRouter()
const hotel   = ref(null)
const gallery = ref(null)
const offers  = ref([])
const nights  = ref(1)
const loading = ref(false)
const wishedIds = ref(new Set()) // 객실 카드용 로컬 토글

// 날짜/인원 (URL 쿼리와 동기화)
const ci = ref(route.query.checkIn || '')
const co = ref(route.query.checkOut || '')
const guests = ref(Number(route.query.guests || 1))
const todayStr = new Date().toISOString().slice(0,10)

const PLACEHOLDER = 'https://placehold.co/1200x1200?text=No+Image'
const money = v => v == null ? '-' : new Intl.NumberFormat('ko-KR').format(v)

// ===== 유틸 =====
const safeImg = (u) => {
  if (!u || typeof u !== 'string') return PLACEHOLDER
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('/')) return u
  return PLACEHOLDER
}

const ratingLabel = (r) => {
  if (r == null) return ''
  const x = +r
  if (x === 5.0) return 'amazing'
  if (x >= 4.5) return 'excellent'
  if (x >= 4.0) return 'very good'
  if (x >= 3.5) return 'good'
  if (x >= 3.0) return 'normal'
  if (x >= 2.5) return 'not bad'
  return 'bad'
}
const ratingText = computed(() =>
  hotel.value?.rating != null ? Number(hotel.value.rating).toFixed(1) : '—'
)

const startingFromPerNight = computed(() => {
  if (!offers.value.length || !nights.value) return 0
  const minTotal = Math.min(...offers.value.map(o => o.priceSum || 0))
  return Math.floor(minTotal / nights.value)
})

// ===== 갤러리 데이터 구성 =====
function seedFromHotel(h) {
  const id = Number(h?.id ?? 777)
  let s = (id * 9301 + 49297) % 233280
  return () => (s = (s * 9301 + 49297) % 233280) / 233280
}
function shuffleSeeded(arr, rnd) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(rnd() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}
const roomThumbsRaw = computed(() =>
  (gallery.value?.roomDefaults || []).map(img =>
    safeImg(typeof img === 'string' ? img : img.url)
  )
)
const roomThumbs = computed(() => shuffleSeeded(roomThumbsRaw.value, seedFromHotel(hotel.value)))

// Slide 1: 메인 + 4장, Slide 2: 8장
function padN(arr, n) { const v = [...arr]; while (v.length < n) v.push(PLACEHOLDER); return v.slice(0, n) }
const firstFour   = computed(() => padN(roomThumbs.value.slice(0, 4), 4))
const secondEight = computed(() => padN(roomThumbs.value.slice(4, 12), 8))

// 커버: 없으면 객실 첫 장
const coverImage = computed(() => {
  const c = gallery.value?.cover
  return c ? safeImg(c) : (roomThumbs.value[0] || PLACEHOLDER)
})

// 슬라이드 제어
const pageCount = 2
const pageIndex = ref(0)
function nextPage(){ pageIndex.value = (pageIndex.value + 1) % pageCount }
function prevPage(){ pageIndex.value = (pageIndex.value - 1 + pageCount) % pageCount }
function goPage(i){ if (i>=0 && i<pageCount) pageIndex.value = i }

// 객실 로컬 찜(기존 유지)
const isWishedRoom = (id) => wishedIds.value.has(id)
function toggleWishRoom(id) { isWishedRoom(id) ? wishedIds.value.delete(id) : wishedIds.value.add(id) }

// 날짜/인원 유효성 + 쿼리 적용
const isValidRange = computed(() => ci.value && co.value && new Date(co.value) > new Date(ci.value))
function applyQuery() {
  if (!isValidRange.value) return
  router.replace({
    name: 'hotel-detail',
    params: { id: route.params.id },
    query: {
      ...route.query,
      checkIn: ci.value,
      checkOut: co.value,
      guests: Number(guests.value || 1), // ★ 항상 포함
    }
  })
}

// 재조회 (백엔드 시그니처에 맞게 항상 guests 전달)
async function refetch() {
  loading.value = true
  try {
    const toISODate = (d) => {
      if (!d) return null
      const x = new Date(String(d))
      return Number.isNaN(x.getTime()) ? null : x.toISOString().slice(0, 10)
    }

    const ciSafe = toISODate(ci.value)
    const coSafe = toISODate(co.value)
    const gSafe  = Number(guests.value || 1)

    if (!ciSafe || !coSafe) throw new Error('날짜가 비어 있습니다.')
    if (!(new Date(coSafe) > new Date(ciSafe))) throw new Error('체크아웃은 체크인 이후여야 합니다.')

    const id = Number(route.params.id)
    const res = await getHotelDetail(id, ciSafe, coSafe, gSafe)

    hotel.value   = res.hotel
    gallery.value = res.gallery
    offers.value  = res.roomTypes || []
    nights.value  = offers.value[0]?.nights || diffDays(ciSafe, coSafe) || 1
    pageIndex.value = 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function diffDays(a, b) {
  if (!a || !b) return 0
  const d1 = new Date(a), d2 = new Date(b)
  return Math.max(0, Math.round((+d2 - +d1) / 86400000))
}

// ✅ 호텔 단위 위시 상태(서버 캐시 기반)
const hotelWished = computed(() => {
  const id = hotel.value?.id
  return id ? isWished(id) : false
})

async function onToggleHotelWish(){
  const id = hotel.value?.id
  if (!id) return
  if (!isLoggedIn()) {
    return router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
  try {
    await toggleWishlist(id)
    // 서버/캐시가 반영되면 hotelWished 가 자동으로 바뀜
  } catch (e) {
    console.error(e)
    alert('찜하기 처리 중 문제가 발생했어요.')
  }
}

// 라우트 쿼리 변경 감지 → 재조회
watch(() => route.query, () => {
  if (route.query.checkIn)  ci.value = route.query.checkIn
  if (route.query.checkOut) co.value = route.query.checkOut
  if (route.query.guests)   guests.value = Number(route.query.guests)
  refetch()
})

// 최초 로드
onMounted(() => {
  ensureWishlistLoaded()   // ✅ 위시리스트 초기 로드
  refetch()
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

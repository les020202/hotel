<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-6 space-y-10">
    <!-- 검색바 -->
    <SearchBar
      :key="sbKey"
      :q="initialQ"
      :check-in="ci"
      :check-out="co"
      :adults="guests"
      :children="0"
      @changed="onBarChanged"
      @submit="onBarSubmit"
    />

    <!-- 헤더 -->
    <header class="flex items-start justify-between gap-6">
      <div class="min-w-0">
        <h1 class="text-2xl md:text-3xl font-extrabold truncate">{{ hotel?.name || '-' }}</h1>

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

          <template v-if="hotel?.phone">
            <span class="text-gray-300">•</span>
            <a :href="telHref(hotel?.phone)" class="underline break-all">☎ {{ hotel?.phone }}</a>
          </template>
        </div>

        <div class="mt-2 flex items-center gap-3">
          <div class="border rounded-md px-2 py-1 text-sm font-semibold">
            {{ ratingText }}
          </div>
          <div class="text-sm text-gray-600">{{ ratingLabel(hotel?.rating) }}</div>
        </div>
      </div>

      <!-- 우측 액션 (찜 + 공유만) -->
      <div class="text-right shrink-0">
        <div class="text-sm text-gray-500">총 금액(최저)</div>
        <div class="text-rose-500 text-2xl font-extrabold">
          ₩{{ money(startingFromTotal) }}
        </div>
        <div class="mt-2 relative flex items-center justify-end gap-2">
          <!-- 찜 -->
          <button class="border rounded-xl w-10 h-10 grid place-items-center" title="찜">
            ♡
          </button>

          <!-- 공유 버튼 + 팝오버 -->
          <div class="relative">
            <button
              class="border rounded-xl w-10 h-10 grid place-items-center"
              title="공유하기"
              @click="shareOpen = !shareOpen"
            >↗</button>

            <div
              v-if="shareOpen"
              class="absolute right-0 mt-2 w-44 rounded-xl border bg-white shadow z-10 p-1"
            >
              <button
                class="w-full text-left px-3 py-2 rounded-lg hover:bg-neutral-100 text-sm"
                @click="copyUrl"
              >URL 복사</button>
              <button
                v-if="canShareKakao"
                class="w-full text-left px-3 py-2 rounded-lg hover:bg-neutral-100 text-sm"
                @click="shareKakao"
              >카카오톡으로 공유</button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 갤러리 (본문 폭에 맞춤) -->
    <section class="relative">
      <div class="w-full">
        <div class="mx-auto w-full">
          <div class="relative">
            <transition name="fade" mode="out-in">
              <div :key="pageIndex" class="grid grid-cols-2 md:grid-cols-4 gap-4 lg:gap-6">
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

    <!-- 편의시설 -->
    <section class="mt-4">
      <h2 class="text-xl font-bold mb-3">편의시설</h2>
      <template v-if="amenities.length">
        <ul class="grid grid-cols-2 md:grid-cols-4 gap-2">
          <li v-for="a in amenities" :key="a.code" class="flex items-center gap-2">
            <span class="text-emerald-600">✔</span><span class="truncate">{{ a.name }}</span>
          </li>
        </ul>
      </template>
      <p v-else class="text-gray-500">등록된 편의시설이 없습니다.</p>
    </section>

    <!-- 객실(오퍼) 리스트 -->
    <section>
      <h2 class="text-xl font-bold mb-4">투숙 가능한 옵션</h2>

      <div v-if="loading" class="py-8 text-center text-gray-500">로딩 중…</div>

      <div v-if="!loading && offers.length === 0" class="text-gray-500 py-10 text-center">
        선택한 기간에 판매 가능한 객실이 없습니다.
      </div>

      <ul v-else class="space-y-8">
        <li v-for="t in offers" :key="t.roomTypeId" class="rounded-2xl border shadow-sm p-4 md:p-5">
          <div class="grid grid-cols-12 gap-4 md:gap-6">
            <!-- 좌: 객실 이미지 -->
            <div class="col-span-12 md:col-span-5">
              <div class="relative h-48 md:h-56 rounded-xl overflow-hidden bg-neutral-100">
                <img :src="safeImg(t.templateImageUrl)" class="w-full h-full object-cover" alt="">
              </div>
              <div class="mt-2 text-sm text-gray-600">
                기준 {{ t.capacity }}인 · {{ t.areaSqm }}㎡ · {{ nights }}박
              </div>
            </div>

            <!-- 우: 설명/가격/버튼 -->
            <div class="col-span-12 md:col-span-7 flex flex-col">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="text-lg md:text-xl font-semibold truncate">{{ t.name }}</h3>
                  <div class="mt-1 text-sm text-gray-600">체크인 15:00 ~ 체크아웃 12:00</div>
                </div>
                <div class="text-right shrink-0">
                  <!-- 총액만 표시 -->
                  <div class="text-rose-600 text-xl md:text-2xl font-extrabold">
                    ₩{{ money(t.priceSum) }}
                  </div>
                  <div class="text-xs text-gray-500">{{ nights }}박 총액</div>
                </div>
              </div>

              <div class="mt-2 text-sm text-amber-600" v-if="t.minRemaining != null && t.minRemaining <= 3">
                남은객실 {{ t.minRemaining }}개
              </div>

              <div class="mt-auto pt-4 flex items-center justify-end gap-3">
                <button
                  class="border rounded-xl w-11 h-11 grid place-items-center"
                  :title="isWished(t.roomTypeId) ? '찜 해제' : '찜하기'"
                  @click="toggleWish(t.roomTypeId)"
                >
                  <svg v-if="isWished(t.roomTypeId)" viewBox="0 0 24 24" class="w-6 h-6 fill-rose-500">
                    <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6.5 3.5 5 5.5 5c1.7 0 3.25 1.03 3.97 2.57h1.06C11.25 6.03 12.8 5 14.5 5 16.5 5 18 6.5 18 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" class="w-6 h-6 stroke-gray-700 fill-none">
                    <path stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"
                          d="M12.1 20.3C7.14 15.78 4 12.94 4 9.5 4 7.5 5.5 6 7.5 6c1.54 0 3.04.99 3.57 2.36h1.87C13.46 6.99 14.96 6 16.5 6 18.5 6 20 7.5 20 9.5c0 3.44-3.14 6.28-8.1 10.8z"/>
                  </svg>
                </button>
                <!-- ✅ 예약하기: 클릭 시 goReservation(t) 호출 -->
                <button class="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-3 rounded-xl" @click="goReservation(t)">예약하기</button>
              </div>
            </div>
          </div>
        </li>
      </ul>
    </section>

    <!-- 지도: v-show 로 DOM 유지 -->
    <section class="mt-2" v-show="canShowMap">
  <h2 class="text-xl font-bold mb-3">위치</h2>

  <div class="relative">
    <!-- 지도 -->
    <div
      ref="mapEl"
      class="w-full h-72 md:h-96 rounded-xl overflow-hidden bg-neutral-100"
    ></div>

    <!-- 전면 오버레이: z-index 크게 + block + cursor -->
    <a
      :href="kakaoLink"
      target="_blank"
      rel="noopener"
      class="absolute inset-0 z-[999] block cursor-pointer"
      aria-label="카카오맵에서 열기"
      title="카카오맵에서 열기"
    >
      <!-- iOS/Safari에서 클릭영역 인식 보조용으로 보이지 않는 텍스트 -->
      <span class="sr-only">카카오맵에서 열기</span>
    </a>
  </div>

  <p v-if="mapError" class="text-sm text-red-500 mt-2">{{ mapError }}</p>
</section>


    <hr class="border-gray-200 mt-8" />
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SearchBar from '@/components/SearchBar.vue'
import { getHotelDetail } from '@/api/hotelApi'
// ✅ 예약용 API 추가 임포트
import { createReservationHold } from '@/api/reservation'
import { getMe } from '@/api/auth'

const route  = useRoute()
const router = useRouter()

const hotel     = ref(null)
const gallery   = ref(null)
const offers    = ref([])
const amenities = ref([])
const nights    = ref(1)
const loading   = ref(false)
const wishedIds = ref(new Set())

/* 공유 팝오버 & 카카오 SDK */
const shareOpen = ref(false)
const canShareKakao = ref(false)

async function ensureKakaoShare () {
  // Vite 환경 변수: VITE_KAKAO_JS_KEY (카카오 JavaScript 키)
  const key = import.meta.env.VITE_KAKAO_JS_KEY
  if (!key) { canShareKakao.value = false; return }
  if (window.Kakao?.isInitialized?.()) { canShareKakao.value = true; return }
  await new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = 'https://t1.kakaocdn.net/kakao_js_sdk/2.7.2/kakao.min.js'
    s.async = true
    s.onload = resolve
    s.onerror = reject
    document.head.appendChild(s)
  })
  try {
    window.Kakao.init(key)
    canShareKakao.value = true
  } catch { canShareKakao.value = false }
}

function copyUrl () {
  const url = location.href
  navigator.clipboard?.writeText(url).then(() => {
    shareOpen.value = false
    alert('링크가 복사되었습니다.')
  }).catch(() => {
    // 폴백
    const ta = document.createElement('textarea')
    ta.value = url
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    shareOpen.value = false
    alert('링크가 복사되었습니다.')
  })
}

function shareKakao () {
  if (!canShareKakao.value) return
  const title = hotel.value?.name || '호텔'
  const desc  = hotel.value?.address || ''
  const url   = location.href
  window.Kakao.Share.sendDefault({
    objectType: 'feed',
    content: {
      title,
      description: desc,
      imageUrl: gallery.value?.cover || (roomThumbs.value[0] || 'https://developers.kakao.com/assets/img/about/logos/kakaolink/kakaolink_btn_medium.png'),
      link: { mobileWebUrl: url, webUrl: url }
    },
    buttons: [{ title: '자세히 보기', link: { mobileWebUrl: url, webUrl: url } }]
  })
  shareOpen.value = false
}

/* Kakao Map */
const mapEl    = ref(null)
let kakaoMap   = null
let marker     = null
let mapBooting = false
const mapError = ref('')
const lastLat  = ref(null)
const lastLng  = ref(null)

const hasCoords = computed(() =>
  hotel.value?.latitude != null && hotel.value?.longitude != null
)
const canShowMap = computed(() =>
  hasCoords.value || !!(hotel.value?.address && hotel.value.address.trim())
)

/* SDK 준비 대기: index.html에 sdk.js 미리 선언 */
function waitKakaoReady () {
  return new Promise((resolve, reject) => {
    const ensure = () => {
      window.kakao.maps.load(() => {
        const ok = typeof window.kakao?.maps?.LatLng === 'function'
               && typeof window.kakao?.maps?.Map    === 'function'
        ok ? resolve() : setTimeout(ensure, 0)
      })
    }
    if (window.kakao?.maps) return ensure()
    const tag = document.querySelector('script[src*="dapi.kakao.com/v2/maps/sdk.js"]')
    if (!tag) return reject(new Error('Kakao SDK script not found in index.html'))
    tag.addEventListener('load', ensure, { once: true })
    tag.addEventListener('error', () => reject(new Error('Kakao SDK load error')), { once: true })
  })
}

/* 지도 초기화 공통 */
async function initMapWith (lat, lng) {
  const maps = window.kakao.maps
  const center = new maps.LatLng(Number(lat), Number(lng))

  if (!kakaoMap) {
    kakaoMap = new maps.Map(mapEl.value, { center, level: 4 })
  } else {
    kakaoMap.setCenter(center)
  }
  kakaoMap.setMapTypeId(maps.MapTypeId.ROADMAP)

  if (marker) marker.setMap(null)
  marker = new maps.Marker({ position: center, map: kakaoMap })

  lastLat.value = Number(lat)
  lastLng.value = Number(lng)

  kakaoMap.relayout()
  maps.event.trigger(kakaoMap, 'resize')
  kakaoMap.setCenter(center)
  setTimeout(() => {
    kakaoMap.relayout()
    maps.event.trigger(kakaoMap, 'resize')
    kakaoMap.setCenter(center)
  }, 0)
}

const kakaoLink = computed(() => {
  const name = hotel.value?.name || hotel.value?.address || '위치'
  const lat  = lastLat.value ?? Number(hotel.value?.latitude)
  const lng  = lastLng.value ?? Number(hotel.value?.longitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return '#'
  return `https://map.kakao.com/link/map/${encodeURIComponent(name)},${lat},${lng}`
})

async function initMap () {
  if (!mapEl.value || !canShowMap.value || mapBooting) return
  mapBooting = true
  mapError.value = ''

  await nextTick()
  try { await waitKakaoReady() }
  catch (e) { console.error(e); mapError.value = '카카오 SDK를 불러오지 못했습니다.'; mapBooting = false; return }

  const lat  = Number(hotel.value?.latitude)
  const lng  = Number(hotel.value?.longitude)
  const addr = (hotel.value?.address || '').replace(/\(.*?\)/g, '').trim()

  if (Number.isFinite(lat) && Number.isFinite(lng)) {
    await initMapWith(lat, lng)
    mapBooting = false
    return
  }
  if (!addr) {
    mapError.value = '지도를 표시할 주소/좌표가 없습니다.'
    mapBooting = false
    return
  }

  const geocoder = new window.kakao.maps.services.Geocoder()
  geocoder.addressSearch(addr, async (result, status) => {
    if (status === window.kakao.maps.services.Status.OK && result?.length) {
      const y = Number(result[0].y) // lat
      const x = Number(result[0].x) // lng
      await initMapWith(y, x)
    } else {
      mapError.value = '주소로 좌표를 찾을 수 없습니다.'
    }
    mapBooting = false
  })
}

/* 검색바/쿼리 */
const ci     = ref(route.query.checkIn  || '')
const co     = ref(route.query.checkOut || '')
const guests = ref(Number(route.query.guests || 1))
const initialQ = computed(() => route.query.region || (hotel.value?.name ?? ''))
const sbKey    = computed(() => `${route.params.id}-${initialQ.value}`)

function normalize (s){ return String(s ?? '').trim().replace(/\s+/g, ' ').toLowerCase() }

function onBarChanged(p){
  const text = String(p?.q ?? p?.region ?? '').trim()
  const hotelName = hotel.value?.name ?? ''
  const sameHotel = text === '' || normalize(text) === normalize(hotelName)
  if (!sameHotel) return
  router.replace({
    name: 'hotel-detail',
    params: { id: route.params.id },
    query: {
      ...route.query,
      region: hotelName,
      checkIn: p?.checkIn ?? ci.value,
      checkOut: p?.checkOut ?? co.value,
      guests: Number(p?.adults ?? p?.guests ?? guests.value)
    }
  })
}

function onBarSubmit(p){
  const text = String(p?.q ?? p?.region ?? '').trim()
  const next = {
    ...route.query,
    region: text || (hotel.value?.name ?? ''),
    q:      text || (hotel.value?.name ?? ''),
    checkIn:  String(p?.checkIn  ?? ci.value),
    checkOut: String(p?.checkOut ?? co.value),
    guests:   Number(p?.adults ?? p?.guests ?? guests.value),
    offset: 0
  }
  if (normalize(text) === normalize(hotel.value?.name ?? '') || text === '') {
    router.replace({ name: 'hotel-detail', params: { id: route.params.id }, query: next })
  } else {
    router.push({ name: 'search', query: next })
  }
}

watch(() => [route.query.checkIn, route.query.checkOut, route.query.guests], ([nci,nco,ng]) => {
  if (nci) ci.value = String(nci)
  if (nco) co.value = String(nco)
  if (ng)  guests.value = Number(ng)
  refetch()
})

/* 유틸 */
const PLACEHOLDER = 'https://placehold.co/1200x1200?text=No+Image'
const money = v => v == null ? '-' : new Intl.NumberFormat('ko-KR').format(Number(v))
const safeImg = u => (!u || typeof u !== 'string') ? PLACEHOLDER :
  (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('/')) ? u : PLACEHOLDER
const ratingLabel = r => {
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
const ratingText = computed(() => hotel.value?.rating != null ? Number(hotel.value.rating).toFixed(1) : '—')

/* 총액(최저) – 1박 단가 제거 */
const startingFromTotal = computed(() => {
  if (!offers.value.length) return 0
  const totals = offers.value.map(o => Number(o.priceSum || 0))
  return Math.min(...totals)
})

/* 갤러리 셔플 */
function seedFromHotel(h){ const id = Number(h?.id ?? 777); let s=(id*9301+49297)%233280; return () => (s=(s*9301+49297)%233280)/233280 }
function shuffleSeeded(arr, rnd){ const a=[...arr]; for(let i=a.length-1;i>0;i--){ const j=Math.floor(rnd()*(i+1)); [a[i],a[j]]=[a[j],a[i]] } return a }
const roomThumbsRaw = computed(() => (gallery.value?.roomDefaults || []).map(img => safeImg(typeof img==='string'?img:img.url)))
const roomThumbs    = computed(() => shuffleSeeded(roomThumbsRaw.value, seedFromHotel(hotel.value)))
function padN(arr,n){ const v=[...arr]; while(v.length<n) v.push(PLACEHOLDER); return v.slice(0,n) }
const firstFour   = computed(() => padN(roomThumbs.value.slice(0,4), 4))
const secondEight = computed(() => padN(roomThumbs.value.slice(4,12), 8))
const coverImage  = computed(() => gallery.value?.cover ? safeImg(gallery.value.cover) : (roomThumbs.value[0] || PLACEHOLDER))

/* 슬라이드 */
const pageCount = 2
const pageIndex = ref(0)
function nextPage(){ pageIndex.value = (pageIndex.value + 1) % pageCount }
function prevPage(){ pageIndex.value = (pageIndex.value - 1 + pageCount) % pageCount }
function goPage(i){ if (i>=0 && i<pageCount) pageIndex.value = i }

/* 위시 */
const isWished = id => wishedIds.value.has(id)
function toggleWish(id){ isWished(id) ? wishedIds.value.delete(id) : wishedIds.value.add(id) }

/* 전화 링크 */
function telHref(p){ return p ? 'tel:' + String(p).replace(/[^\d+]/g,'') : '#' }

/* 날짜차 */
function diffDays(a,b){ if(!a||!b) return 0; const d1=new Date(a), d2=new Date(b); return Math.max(0, Math.round((+d2-+d1)/86400000)) }

/* 데이터 로드 */
async function refetch(){
  loading.value = true
  try{
    const id = Number(route.params.id)
    const res = await getHotelDetail(id, ci.value, co.value, guests.value)
    hotel.value     = res.hotel
    gallery.value   = res.gallery
    offers.value    = res.roomTypes || []
    amenities.value = Array.isArray(res.amenities) ? res.amenities : []
    nights.value    = offers.value[0]?.nights || diffDays(ci.value, co.value) || 1
    // nights는 총액 텍스트에만 사용 (단가 계산 X)
    if (canShowMap.value) initMap()
    ensureKakaoShare()
  } finally {
    loading.value = false
  }
}

watch(() => hasCoords.value, v => { if (v && mapEl.value) initMap() })
onMounted(() => { refetch() })

// ✅ 예약 버튼 동작: Ready 단계를 건너뛰고 곧바로 /reservation 이동
async function goReservation(roomType) {
  const checkIn  = ci.value
  const checkOut = co.value
  const guestsN  = guests.value

  if (!checkIn || !checkOut) {
    alert('체크인/체크아웃 날짜를 선택하세요.')
    return
  }

  try {
    // ✅ 로그인 사용자 확인
    let userId
    try {
      const me = await getMe()
      userId = me.id
    } catch (e) {
      // 401 등 미로그인 → 로그인으로 유도
      const redirect = encodeURIComponent(location.pathname + location.search)
      router.push(`/login?redirect=${redirect}`)
      return
    }

    // ✅ ratePlanId: roomType.ratePlanId → 없으면 1(임시)
    const ratePlanId = roomType.ratePlanId ?? 1
    if (!roomType.ratePlanId) {
      console.warn('ratePlanId가 없어 임시로 1을 사용합니다. 운영 환경에서는 실제 요금제 ID를 내려주세요.')
    }

    const { data } = await createReservationHold({
      userId,
      hotelId: Number(route.params.id),
      roomTypeId: roomType.roomTypeId,
      ratePlanId,
      checkIn,
      checkOut,
      guests: guestsN,
    })

    router.push({
      path: '/reservation',
      query: {
        hotelId: route.params.id,
        roomTypeId: roomType.roomTypeId,
        hotelName: hotel.value?.name || '',
        roomTypeName: roomType.name || '',
        ratePlanId,
        checkIn,
        checkOut,
        guests: guestsN,
        holdCode: data.holdCode,
        expiresAt: data.expiresAt,
        totalAmount: data.totalAmount,
      }
    })
  } catch (e) {
    console.error(e)
    alert(e?.response?.data?.message ?? '예약 실패')
  }
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

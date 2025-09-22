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
          <div class="border rounded-md px-2 py-1 text-sm font-semibold">{{ ratingText }}</div>
          <div class="text-sm text-gray-600">{{ ratingLabel(hotel?.rating) }}</div>
        </div>
      </div>

      <div class="text-right shrink-0">
        <div class="text-sm text-gray-500">starting from</div>
        <div class="text-rose-500 text-2xl font-extrabold">
          ₩{{ money(startingFromPerNight) }}<span class="text-base text-gray-500">/night</span>
        </div>
        <div class="mt-2 flex items-center justify-end gap-2">
          <button class="border rounded-xl w-10 h-10 grid place-items-center">♡</button>
          <button class="border rounded-xl w-10 h-10 grid place-items-center">↗</button>
          <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-xl">Book now</button>
        </div>
      </div>
    </header>

    <!-- 갤러리 -->
    <section class="relative">
      <div class="relative left-1/2 -translate-x-1/2 w-screen">
        <div class="mx-auto w-full max-w-[min(92vw,1600px)] px-6 lg:px-12">
          <div class="relative">
            <transition name="fade" mode="out-in">
              <div :key="pageIndex" class="grid grid-cols-2 md:grid-cols-4 gap-4 lg:gap-6">
                <template v-if="pageIndex === 0">
                  <div class="col-span-2 row-span-2 relative overflow-hidden rounded-2xl">
                    <div class="w-full pb-[100%]"></div>
                    <img :src="coverImage" alt="메인" class="absolute inset-0 h-full w-full object-cover object-center" />
                  </div>
                  <div v-for="(img,i) in firstFour" :key="'p0-'+i" class="relative overflow-hidden rounded-2xl">
                    <div class="w-full pb-[100%]"></div>
                    <img :src="img" alt="" class="absolute inset-0 h-full w-full object-cover object-center" />
                  </div>
                </template>
                <template v-else>
                  <div v-for="(img,i) in secondEight" :key="'p1-'+i" class="relative overflow-hidden rounded-2xl">
                    <div class="w-full pb-[100%]"></div>
                    <img :src="img" alt="" class="absolute inset-0 h-full w-full object-cover object-center" />
                  </div>
                </template>
              </div>
            </transition>

            <button v-if="pageCount>1"
              class="hidden md:grid place-items-center absolute -left-1 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/90 hover:bg-white shadow"
              @click="prevPage" aria-label="Previous">‹</button>
            <button v-if="pageCount>1"
              class="hidden md:grid place-items-center absolute -right-1 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/90 hover:bg-white shadow"
              @click="nextPage" aria-label="Next">›</button>

            <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 flex gap-2">
              <button v-for="i in 2" :key="'dot-'+i" @click="goPage(i-1)"
                class="h-2 w-10 rounded-full" :class="(i-1)===pageIndex ? 'bg-neutral-300' : 'bg-neutral-600'"></button>
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
            <div class="col-span-12 md:col-span-5">
              <div class="relative h-48 md:h-56 rounded-xl overflow-hidden bg-neutral-100">
                <img :src="safeImg(t.templateImageUrl)" class="w-full h-full object-cover" alt="">
              </div>
              <div class="mt-2 text-sm text-gray-600">기준 {{ t.capacity }}인 · {{ t.areaSqm }}㎡ · {{ nights }}박 요금</div>
            </div>
            <div class="col-span-12 md:col-span-7 flex flex-col">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="text-lg md:text-xl font-semibold truncate">{{ t.name }}</h3>
                  <div class="mt-1 text-sm text-gray-600">체크인 15:00 ~ 체크아웃 12:00</div>
                </div>
                <div class="text-right shrink-0">
                  <div class="text-rose-600 text-xl md:text-2xl font-extrabold">
                    ₩{{ money(Math.floor((t.priceSum || 0) / nights)) }} <span class="text-sm text-gray-500">/night</span>
                  </div>
                  <div class="text-xs text-gray-500">총 ₩{{ money(t.priceSum) }}</div>
                </div>
              </div>
              <div class="mt-2 text-sm text-amber-600" v-if="t.minRemaining != null && t.minRemaining <= 3">
                남은객실 {{ t.minRemaining }}개
              </div>
              <div class="mt-auto pt-4 flex items-center justify-end gap-3">
                <button class="border rounded-xl w-11 h-11 grid place-items-center" :title="isWished(t.roomTypeId) ? '찜 해제' : '찜하기'" @click="toggleWish(t.roomTypeId)">
                  <svg v-if="isWished(t.roomTypeId)" viewBox="0 0 24 24" class="w-6 h-6 fill-rose-500">
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

    <!-- 지도 -->
    <section class="mt-2" v-if="canShowMap">
      <h2 class="text-xl font-bold mb-3">위치</h2>
      <div ref="mapEl" class="w-full h-72 md:h-96 rounded-xl overflow-hidden bg-neutral-100"></div>
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

const route  = useRoute()
const router = useRouter()

const hotel     = ref(null)
const gallery   = ref(null)
const offers    = ref([])
const amenities = ref([])
const nights    = ref(1)
const loading   = ref(false)
const wishedIds = ref(new Set())

/* Kakao Map */
const mapEl    = ref(null)
let kakaoMap   = null
let marker     = null
const mapError = ref('')

const hasCoords = computed(() =>
  hotel.value?.latitude != null && hotel.value?.longitude != null
)
const canShowMap = computed(() =>
  hasCoords.value || !!(hotel.value?.address && hotel.value.address.trim())
)

/* SDK 준비 대기: index.html에 script가 있다는 전제 */
function waitKakaoReady () {
  return new Promise((resolve, reject) => {
    if (window.kakao?.maps) return resolve()
    const tag = document.querySelector('script[src*="dapi.kakao.com/v2/maps/sdk.js"]')
    if (!tag) return reject(new Error('Kakao SDK script not found in index.html'))
    const onReady = () => window.kakao.maps.load(resolve)
    if (tag.dataset.__ready === '1') onReady()
    else {
      tag.addEventListener('load', () => { tag.dataset.__ready = '1'; onReady() }, { once: true })
      tag.addEventListener('error', () => reject(new Error('Kakao SDK load error')), { once: true })
    }
  })
}

async function initMapWith(lat, lng){
  const { kakao } = window
  const center = new kakao.maps.LatLng(lat, lng)
  if (!kakaoMap) kakaoMap = new kakao.maps.Map(mapEl.value, { center, level: 4 })
  else kakaoMap.setCenter(center)
  if (marker) marker.setMap(null)
  marker = new kakao.maps.Marker({ position: center, map: kakaoMap })
}

async function initMap(){
  mapError.value = ''
  if (!mapEl.value || !canShowMap.value) return
  await nextTick()
  try {
    await waitKakaoReady()
  } catch (e) {
    console.error(e)
    mapError.value = '카카오 SDK를 불러오지 못했습니다.'
    return
  }

  const lat  = Number(hotel.value?.latitude)
  const lng  = Number(hotel.value?.longitude)
  const addr = (hotel.value?.address || '').replace(/\(.*?\)/g, '').trim()

  if (Number.isFinite(lat) && Number.isFinite(lng)) {
    await initMapWith(lat, lng)
    return
  }
  if (!addr) {
    mapError.value = '지도를 표시할 주소/좌표가 없습니다.'
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
const startingFromPerNight = computed(() => {
  if (!offers.value.length || !nights.value) return 0
  const totals = offers.value.map(o => Number(o.priceSum || 0))
  return Math.floor(Math.min(...totals) / nights.value)
})
function seedFromHotel(h){ const id = Number(h?.id ?? 777); let s=(id*9301+49297)%233280; return () => (s=(s*9301+49297)%233280)/233280 }
function shuffleSeeded(arr, rnd){ const a=[...arr]; for(let i=a.length-1;i>0;i--){ const j=Math.floor(rnd()*(i+1)); [a[i],a[j]]=[a[j],a[i]] } return a }
const roomThumbsRaw = computed(() => (gallery.value?.roomDefaults || []).map(img => safeImg(typeof img==='string'?img:img.url)))
const roomThumbs    = computed(() => shuffleSeeded(roomThumbsRaw.value, seedFromHotel(hotel.value)))
function padN(arr,n){ const v=[...arr]; while(v.length<n) v.push(PLACEHOLDER); return v.slice(0,n) }
const firstFour   = computed(() => padN(roomThumbs.value.slice(0,4), 4))
const secondEight = computed(() => padN(roomThumbs.value.slice(4,12), 8))
const coverImage  = computed(() => gallery.value?.cover ? safeImg(gallery.value.cover) : (roomThumbs.value[0] || PLACEHOLDER))
const pageCount = 2
const pageIndex = ref(0)
function nextPage(){ pageIndex.value = (pageIndex.value + 1) % pageCount }
function prevPage(){ pageIndex.value = (pageIndex.value - 1 + pageCount) % pageCount }
function goPage(i){ if (i>=0 && i<pageCount) pageIndex.value = i }
const isWished = id => wishedIds.value.has(id)
function toggleWish(id){ isWished(id) ? wishedIds.value.delete(id) : wishedIds.value.add(id) }
function telHref(p){ return p ? 'tel:' + String(p).replace(/[^\d+]/g,'') : '#' }
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
    pageIndex.value = 0
    if (canShowMap.value) initMap()
  } finally {
    loading.value = false
  }
}
watch(() => hasCoords.value, v => { if (v && mapEl.value) initMap() })
onMounted(() => { refetch() })
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
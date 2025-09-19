<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-6 space-y-10">
    <!-- ✅ 검색바 -->
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

          <!-- 주소 -->
          <div class="flex items-center gap-1 min-w-0">
            <span class="text-sm">📍</span>
            <span class="truncate">{{ hotel?.address || hotel?.region || '' }}</span>
          </div>

          <!-- 전화번호 -->
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

      <!-- 시작가/액션 -->
      <div class="text-right shrink-0">
        <div class="text-sm text-gray-500">starting from</div>
        <div class="text-rose-500 text-2xl font-extrabold">
          ₩{{ money(startingFromPerNight) }}<span class="text-base text-gray-500">/night</span>
        </div>
        <div class="mt-2 flex items-center justify-end gap-2">
          <button class="border rounded-xl w-10 h-10 grid place-items-center">♡</button>
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

    <!-- ===== 편의시설 ===== -->
    <section class="mt-4">
      <h2 class="text-xl font-bold mb-3">편의시설</h2>

      <template v-if="amenities.length">
        <ul class="grid grid-cols-2 md:grid-cols-4 gap-2">
          <li v-for="a in amenities" :key="a.code" class="flex items-center gap-2">
            <span class="text-emerald-600">✔</span>
            <span class="truncate">{{ a.name }}</span>
          </li>
        </ul>
      </template>

      <p v-else class="text-gray-500">등록된 편의시설이 없습니다.</p>
    </section>

    <!-- ===== 지도 ===== -->
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

const route = useRoute()
const router = useRouter()

const hotel     = ref(null)
const gallery   = ref(null)
const offers    = ref([])
const amenities = ref([])
const nights    = ref(1)
const loading   = ref(false)
const wishedIds = ref(new Set())

/* ── Kakao Map ─────────────────────────────────────────────── */
const mapEl    = ref(null)
let kakaoMap   = null
let mapMarker  = null
const mapError = ref('')

function assertEnv () {
  if (!KAKAO_KEY) {
    mapError.value = 'KAKAO_KEY가 비어 있습니다. .env 의 VITE_KAKAO_APP_KEY를 확인하세요.'
    console.error('[KakaoMap] KAKAO_KEY is empty')
    return false
  }
  return true
}

const hasCoords = computed(() =>
  hotel.value?.latitude != null && hotel.value?.longitude != null
)
const canShowMap = computed(() =>
  hasCoords.value || !!(hotel.value?.address && hotel.value?.address.trim())
)

async function loadKakaoSdk () {
  if (window.kakao?.maps) return;

  if (!KAKAO_KEY) {
    mapError.value = 'KAKAO_KEY가 비어 있습니다. .env 의 VITE_KAKAO_APP_KEY를 확인하세요.';
    console.error('[KakaoMap] EMPTY KAKAO_KEY');
    throw new Error('EMPTY_KEY');
  }

  await new Promise((resolve, reject) => {
    if (document.querySelector('script[data-kakao-sdk="1"]')) {
      return window.kakao?.maps ? resolve() : reject(new Error('SDK tag exists but not ready'));
    }
    const s = document.createElement('script');
    s.dataset.kakaoSdk = '1';
    s.src = `https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey=${encodeURIComponent(KAKAO_KEY)}&libraries=services`;
    s.async = true;
    s.onload = () => window.kakao.maps.load(resolve);
    s.onerror = () => reject(new Error('SDK_LOAD_FAILED'));
    document.head.appendChild(s);
    console.log('[KakaoMap] appended:', s.src);
  });
}



async function initMapWithCoords (lat, lng) {
  const { kakao } = window;
  const center = new kakao.maps.LatLng(lat, lng);
  if (!kakaoMap) {
    kakaoMap = new kakao.maps.Map(mapEl.value, { center, level: 4 });
  } else {
    kakaoMap.setCenter(center);
  }
  if (mapMarker) mapMarker.setMap(null);
  mapMarker = new kakao.maps.Marker({ position: center, map: kakaoMap });
}

async function initMap () {
  mapError.value = '';

  if (!mapEl.value || !canShowMap.value) return;

  // 렌더 안정화
  await nextTick();

  try {
    await loadKakaoSdk();
  } catch (e) {
    if (String(e?.message) !== 'EMPTY_KEY') {
      mapError.value = '카카오 SDK를 불러오지 못했습니다.';
    }
    return;
  }

  const addrRaw = hotel.value?.address || '';
  // 괄호 등 제거해서 매칭률 올리기
  const addr = addrRaw.replace(/\(.*?\)/g, '').trim();

  const lat = Number(hotel.value?.latitude);
  const lng = Number(hotel.value?.longitude);

  try {
    if (Number.isFinite(lat) && Number.isFinite(lng)) {
      await initMapWithCoords(lat, lng);
      return;
    }
    if (!addr) {
      mapError.value = '지도를 표시할 주소/좌표가 없습니다.';
      return;
    }

    const { kakao } = window;
    const geocoder = new kakao.maps.services.Geocoder();
    geocoder.addressSearch(addr, async (result, status) => {
      console.log('[Kakao geocode]', { addr, status, result });
      if (status === kakao.maps.services.Status.OK && result?.length) {
        const y = Number(result[0].y); // lat
        const x = Number(result[0].x); // lng
        await initMapWithCoords(y, x);
      } else {
        mapError.value = `지오코딩 실패: status=${status}`;
      }
    });
  } catch (e) {
    console.error(e);
    mapError.value = '지도를 불러오는 중 오류가 발생했습니다.';
  }
}
/* ─────────────────────────────────────────────────────────── */

/* ── 쿼리/검색바 ───────────────────────────────────────────── */
const ci     = ref(route.query.checkIn  || '')
const co     = ref(route.query.checkOut || '')
const guests = ref(Number(route.query.guests || 1))

const initialQ = computed(() =>
  (route.query.region) || (hotel.value?.name ?? '')
)
const sbKey = computed(() => `${route.params.id}-${initialQ.value}`)

function normalize (s) {
  return String(s ?? '').trim().replace(/\s+/g, ' ').toLowerCase()
}

function onBarChanged (p) {
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

function onBarSubmit (p) {
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

watch(() => [route.query.checkIn, route.query.checkOut, route.query.guests], ([nci, nco, ng]) => {
  if (nci) ci.value = String(nci)
  if (nco) co.value = String(nco)
  if (ng)  guests.value = Number(ng)
  refetch()
})
/* ─────────────────────────────────────────────────────────── */

/* ── 표시 유틸 ─────────────────────────────────────────────── */
const PLACEHOLDER = 'https://placehold.co/1200x1200?text=No+Image'
const money = (v) => v == null ? '-' : new Intl.NumberFormat('ko-KR').format(Number(v))
const safeImg = (u) => (!u || typeof u !== 'string')
  ? PLACEHOLDER
  : (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('/')) ? u : PLACEHOLDER
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
const ratingText = computed(() => hotel.value?.rating != null ? Number(hotel.value.rating).toFixed(1) : '—')
const startingFromPerNight = computed(() => {
  if (!offers.value.length || !nights.value) return 0
  const totals = offers.value.map(o => Number(o.priceSum || 0))
  const minTotal = Math.min(...totals)
  return Math.floor(minTotal / nights.value)
})

function seedFromHotel (h) {
  const id = Number(h?.id ?? 777)
  let s = (id * 9301 + 49297) % 233280
  return () => (s = (s * 9301 + 49297) % 233280) / 233280
}
function shuffleSeeded (arr, rnd) {
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
function padN (arr, n) { const v = [...arr]; while (v.length < n) v.push(PLACEHOLDER); return v.slice(0, n) }
const firstFour   = computed(() => padN(roomThumbs.value.slice(0, 4), 4))
const secondEight = computed(() => padN(roomThumbs.value.slice(4, 12), 8))
const coverImage  = computed(() => gallery.value?.cover ? safeImg(gallery.value.cover) : (roomThumbs.value[0] || PLACEHOLDER))

const pageCount = 2
const pageIndex = ref(0)
function nextPage(){ pageIndex.value = (pageIndex.value + 1) % pageCount }
function prevPage(){ pageIndex.value = (pageIndex.value - 1 + pageCount) % pageCount }
function goPage(i){ if (i>=0 && i<pageCount) pageIndex.value = i }

const isWished = (id) => wishedIds.value.has(id)
function toggleWish(id){ isWished(id) ? wishedIds.value.delete(id) : wishedIds.value.add(id) }

const KAKAO_KEY = import.meta.env.VITE_KAKAO_APP_KEY || '';
function telHref (p){
  if(!p) return '#'
  return 'tel:' + String(p).replace(/[^\d+]/g, '')
}

function diffDays (a, b) {
  if (!a || !b) return 0
  const d1 = new Date(a), d2 = new Date(b)
  return Math.max(0, Math.round((+d2 - +d1) / 86400000))
}
/* ─────────────────────────────────────────────────────────── */

/* ── 데이터 로드 ───────────────────────────────────────────── */
async function refetch () {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getHotelDetail(id, ci.value, co.value, guests.value)

    hotel.value     = res.hotel
    gallery.value   = res.gallery
    offers.value    = res.roomTypes || []
    amenities.value = Array.isArray(res.amenities) ? res.amenities : []
    nights.value    = offers.value[0]?.nights || diffDays(ci.value, co.value) || 1
    pageIndex.value = 0

    if (canShowMap.value) initMap()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

watch(() => hasCoords.value, (v) => { if (v && mapEl.value) initMap() })

onMounted(() => { refetch() })
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

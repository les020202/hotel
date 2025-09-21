<template>
  <!-- 상위 레이아웃의 max-width 영향을 깨고, 뷰포트 전체폭을 강제로 사용 -->
  <div class="search-root">
    <!-- 페이지 실제 폭: 1560~1680px 컨테이너 -->
    <div class="page">
      <!-- 검색 폼 -->
      <header class="search-controls">
        <input type="date" v-model="checkIn"  class="border rounded px-3 py-2" />
        <input type="date" v-model="checkOut" class="border rounded px-3 py-2" />

        <select v-model="region" @change="regionExact = false" class="border rounded px-3 py-2">
          <option :value="null">전체 지역</option>
          <option v-for="r in regions" :key="r" :value="r">{{ r }}</option>
        </select>

        <!-- 인원 -->
        <select v-model.number="guests" class="border rounded px-3 py-2">
          <option v-for="n in 6" :key="n" :value="n">{{ n }}명</option>
        </select>

        <button
          class="bg-black text-white px-4 py-2 rounded disabled:opacity-50"
          :disabled="!isValidRange || loading"
          @click="onSearch"
        >
          검색
        </button>

        <span class="text-sm text-gray-500" v-if="total !== null">
          총 {{ total.toLocaleString('ko-KR') }}개
        </span>
      </header>

      <!-- 유효성 안내 -->
      <p v-if="!isValidRange" class="text-red-600 mb-4">
        체크아웃은 체크인보다 이후여야 합니다.
      </p>

      <!-- 목록 -->
      <HotelList
        :items="items"
        :check-in="checkIn"
        :check-out="checkOut"
        :guests="guests"
      />

      <!-- 더보기 -->
      <div class="mt-6 text-center" v-if="hasMore">
        <button
          class="px-5 py-3 border rounded-lg hover:bg-gray-50 disabled:opacity-50"
          :disabled="loading || !isValidRange"
          @click="loadMore"
        >
          {{ loading ? '로딩중...' : '더보기 5개' }}
        </button>
      </div>

      <!-- 빈 결과 -->
      <div v-if="items.length === 0 && !loading && isValidRange" class="text-center text-gray-500 py-20">
        조건에 맞는 호텔이 없습니다.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import HotelList from '@/components/HotelList.vue'
import { fetchHotels } from '@/api/searchApi.js'

/** 프론트 약칭 → 백엔드(DB) 정식 값 매핑 */
const REGION_ALIAS = {
  '서울': '서울특별시',
  '경기': '경기도',
  '강원': '강원도',
  '부산': '부산광역시',
  '인천': '인천광역시',
  '대전': '대전광역시',
  '대구': '대구광역시',
  '울산': '울산광역시',
  '광주': '광주광역시',
}

const regions = [
  '서울','인천','울산','경상북도','경상남도','부산','전라남도','전라북도',
  '강원','경기','충청북도','충청남도','광주','대구','대전'
]

const today = new Date()
const tomorrow = new Date(today); tomorrow.setDate(today.getDate() + 1)
const fmtDate = (d) => d.toISOString().slice(0, 10)

const checkIn  = ref(fmtDate(today))
const checkOut = ref(fmtDate(tomorrow))
const region   = ref(null)       // UI 표시값(약칭 또는 정식)
const regionExact = ref(false)   // true면 URL에서 온 region을 그대로 사용
const guests   = ref(1)

const items = ref([])
const total = ref(null)
const hasMore = ref(false)
const nextOffset = ref(0)
const limit = 5
const loading = ref(false)

const isValidRange = computed(() => {
  if (!checkIn.value || !checkOut.value) return false
  return new Date(checkOut.value) > new Date(checkIn.value)
})

/** API에 넘길 실제 region 값 */
const regionForApi = computed(() => {
  if (!region.value) return null
  if (regionExact.value) return region.value            // 카드로 진입한 정확한 값(예: 서울특별시)
  return REGION_ALIAS[region.value] ?? region.value     // 드롭다운 약칭이면 정식명으로 매핑
})

async function loadPage(offset = 0, append = false) {
  if (!isValidRange.value) {
    items.value = []; total.value = 0; hasMore.value = false; nextOffset.value = 0
    return
  }
  loading.value = true
  try {
    const res = await fetchHotels({
      checkIn: checkIn.value,
      checkOut: checkOut.value,
      region: regionForApi.value,  // ← 매핑된 값으로 호출
      limit,
      offset,
      guests: guests.value,
    })
    if (append) items.value.push(...res.items)
    else items.value = res.items

    total.value = res.total
    hasMore.value = res.hasMore
    nextOffset.value = res.nextOffset ?? (offset + res.items.length)
  } finally {
    loading.value = false
  }
}

function reload()   { loadPage(0, false) }
function loadMore() { loadPage(nextOffset.value, true) }

const router = useRouter()
const route  = useRoute()

function syncFromRoute() {
  const q = route.query
  if (q.checkIn)  checkIn.value  = q.checkIn
  if (q.checkOut) checkOut.value = q.checkOut
  region.value = q.region ?? null
  regionExact.value = q.regionExact === 'true'   // 카드 클릭이면 true
  guests.value = Number(q.guests || 1)
}

function onSearch() {
  const q = { checkIn: checkIn.value, checkOut: checkOut.value, guests: guests.value }
  if (region.value) q.region = region.value
  // 사용자가 검색 버튼을 누르면 exact 조건은 해제(드롭다운 선택 기준으로 검색)
  router.replace({ name: 'search', query: q }).finally(reload)
}

watch(() => route.query, () => { syncFromRoute(); reload() })

onMounted(() => { syncFromRoute(); loadPage(0, false) })
</script>

<style scoped>
/* 검색 페이지를 부모 max-width 밖으로 꺼내서 뷰포트 전체폭 사용 */
.search-root{
  position: relative;
  left: 50%;
  right: 50%;
  margin-left: -50vw;
  margin-right: -50vw;
  width: 100vw;
}

/* 실제 컨텐츠 폭(카드 영역) */
.page{
  width: 100%;
  max-width: 1560px;
  padding-left: 24px;
  padding-right: 24px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 검색 폼을 카드 폭과 맞춰 가운데 정렬 */
.search-controls{
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;    /* ✅ 가운데 정렬 */
  gap: 12px;
  max-width: 1100px;          /* ✅ 카드(.hotel-card)와 동일 폭 */
  margin: 0 auto 16px;        /* ✅ 좌우 중앙 + 아래 여백 */
}

.search-controls > *{ flex: 0 0 auto; }
.search-controls input[type="date"],
.search-controls select{ min-width: 160px; }
.search-controls button{ min-width: 88px; }
</style>

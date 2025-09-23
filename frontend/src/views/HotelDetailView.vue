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

      <!-- 우측 액션 -->
      <div class="text-right shrink-0">
        <div class="text-sm text-gray-500">총 금액(최저)</div>
        <div class="text-rose-500 text-2xl font-extrabold">
          ₩{{ money(startingFromTotal) }}
        </div>
        <div class="mt-2 relative flex items-center justify-end gap-2">
          <!-- 찜 -->
          <button class="border rounded-xl w-10 h-10 grid place-items-center" title="찜">♡</button>

          <!-- 공유 -->
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

    <!-- 갤러리 -->
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

    <!-- 객실 리스트 -->
    <section>
      <h2 class="text-xl font-bold mb-4">투숙 가능한 옵션</h2>
      <div v-if="loading" class="py-8 text-center text-gray-500">로딩 중…</div>
      <div v-if="!loading && offers.length === 0" class="text-gray-500 py-10 text-center">
        선택한 기간에 판매 가능한 객실이 없습니다.
      </div>

      <ul v-else class="space-y-8">
        <li v-for="t in offers" :key="t.roomTypeId" class="rounded-2xl border shadow-sm p-4 md:p-5">
          <!-- ... 생략 (기존 객실 UI 유지) -->
        </li>
      </ul>
    </section>

    <!-- 지도 -->
    <section class="mt-2" v-show="canShowMap">
      <h2 class="text-xl font-bold mb-3">위치</h2>
      <div class="relative">
        <div ref="mapEl" class="w-full h-72 md:h-96 rounded-xl overflow-hidden bg-neutral-100"></div>
        <a :href="kakaoLink" target="_blank" rel="noopener"
           class="absolute inset-0 z-[999] block cursor-pointer"
           aria-label="카카오맵에서 열기" title="카카오맵에서 열기"></a>
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

const route  = useRoute()
const router = useRouter()

const hotel     = ref(null)
const gallery   = ref(null)
const offers    = ref([])
const amenities = ref([])
const nights    = ref(1)
const loading   = ref(false)

/* 유틸 */
function diffDays(a,b){ 
  if(!a||!b) return 0
  const d1=new Date(a), d2=new Date(b)
  return Math.max(0, Math.round((+d2-+d1)/86400000))
}

/* 데이터 로드 */
async function refetch(){
  loading.value = true
  try{
    const id = Number(route.params.id)
    const res = await getHotelDetail(id, route.query.checkIn, route.query.checkOut, route.query.guests)
    hotel.value     = res.hotel
    gallery.value   = res.gallery
    offers.value    = res.roomTypes || []
    amenities.value = Array.isArray(res.amenities) ? res.amenities : []
    nights.value    = offers.value[0]?.nights || diffDays(route.query.checkIn, route.query.checkOut) || 1
  } finally {
    loading.value = false
  }
}

onMounted(() => { refetch() })
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

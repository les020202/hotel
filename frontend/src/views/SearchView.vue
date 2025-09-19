<script setup>
import { ref, onMounted, onBeforeUnmount, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import SearchBar from "@/components/SearchBar.vue";
import HotelList from "@/components/HotelList.vue";
import FiltersSidebar from "@/components/FiltersSidebar.vue";
import { fetchHotels } from "@/api/searchApi.js";

const route = useRoute(), router = useRouter();

const q        = ref("");
const checkIn  = ref(null);
const checkOut = ref(null);
const adults   = ref(2);
const children = ref(0);

const items      = ref([]);
const total      = ref(null);
const hasMore    = ref(false);
const nextOffset = ref(0);
const limit      = 10;
const loading    = ref(false);
const errorMsg   = ref("");

const sentinel = ref(null);
let io = null;
const supportsIO = ref(false);

/* 우하단 맨위로 버튼 */
const showBackTop = ref(false);
function onScroll(){
  showBackTop.value = (window.scrollY || document.documentElement.scrollTop) > 400;
}
function backToTop(){
  const el = document.scrollingElement || document.documentElement || document.body;
  if (el.scrollTo) el.scrollTo({ top: 0, behavior: "smooth" });
  else el.scrollTop = 0;
}

const sort = ref("rating");

/* 필터 상태 */
const filters = ref({
  priceMin: 0,
  priceMax: 1_000_000,
  grades: [],
  ratingBands: [],
  amenityIds: [],
});

const isValidRange = computed(() =>
  !!checkIn.value && !!checkOut.value &&
  new Date(checkOut.value) > new Date(checkIn.value)
);

/* ✅ URL → 화면 상태 동기화 (q || region, adults || guests 지원) */
function syncFromRoute() {
  const rq = route.query;
  q.value        = (rq.q ?? rq.region ?? "");
  checkIn.value  = (rq.checkIn ?? null);
  checkOut.value = (rq.checkOut ?? null);

  // 호환: guests가 오면 adults로 사용
  adults.value   = rq.adults != null ? Number(rq.adults)
                  : rq.guests != null ? Number(rq.guests)
                  : 2;
  children.value = rq.children != null ? Number(rq.children) : 0;
}


/* ✅ 페이지 로드 */
async function loadPage(offset=0, append=false) {
  if (!isValidRange.value) {
    items.value = []; total.value = 0; hasMore.value = false; nextOffset.value = 0; return;
  }
  if (loading.value) return;
  loading.value = true; errorMsg.value = "";
  try {
    const res = await fetchHotels({
      // 🔑 백엔드가 어떤 키를 봐도 되도록 둘 다 전달
      q: q.value,
      region: q.value,
      checkIn: checkIn.value,
      checkOut: checkOut.value,
      // guests 호환(정책에 맞춰 조정 가능)
      guests: adults.value,                         // 또는 adults.value + Math.max(0, children.value - 2)
      adults: adults.value,
      children: children.value,

      priceMin:  filters.value.priceMin,
      priceMax:  filters.value.priceMax,
      grades:     filters.value.grades,
      ratingBands: filters.value.ratingBands,
      amenityIds:  filters.value.amenityIds,
      sort: sort.value,
      limit, offset
    });
    const list = Array.isArray(res) ? res : (res.items ?? []);
    if (append) items.value.push(...list); else items.value = list;

    total.value = res.total ?? null;
    hasMore.value = res.hasMore ?? (list.length === limit);
    nextOffset.value = res.nextOffset ?? (offset + list.length);
  } catch (e) {
    console.error(e);
    errorMsg.value = "목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.";
  } finally {
    loading.value = false;
  }
}

function onSortChange(){
  items.value = []; total.value = null; hasMore.value = false; nextOffset.value = 0;
  reload();
  setTimeout(setupIO, 0);
}

function reload(){ loadPage(0,false); }
function loadMore(){ loadPage(nextOffset.value,true); }

/* SearchBar 이벤트 */
function onChanged(p){
  q.value        = p.q ?? "";
  checkIn.value  = p.checkIn;
  checkOut.value = p.checkOut;
  adults.value   = Number(p.adults ?? adults.value);
  children.value = Number(p.children ?? children.value);
}

function onSubmit(payload){
  q.value        = payload.q ?? "";
  checkIn.value  = payload.checkIn;
  checkOut.value = payload.checkOut;
  adults.value   = Number(payload.adults);
  children.value = Number(payload.children);

  // ✅ URL에 q와 region 둘 다 넣기, guests도 넣어주기
 router.replace({
  name: "search",
  query: {
    q: q.value,
    region: q.value,
    checkIn: checkIn.value,
    checkOut: checkOut.value,
    guests: String(adults.value),
    adults: String(adults.value),
    children: String(children.value),
    offset: "0",
  },
});

}

/* 필터 변경 → 최상단 스크롤 후 즉시 재조회 */
function onFilterChange(f){
  const rawMin = f.priceMin ?? f.minPrice;
  const rawMax = f.priceMax ?? f.maxPrice;
  const pm = Number(rawMin);
  const px = Number(rawMax);

  filters.value = {
    priceMin: Number.isFinite(pm) ? pm : undefined,
    priceMax: Number.isFinite(px) ? px : undefined,
    grades: Array.isArray(f.grades) ? f.grades.map(Number) : [],
    ratingBands: Array.isArray(f.ratingBands) ? f.ratingBands.map(Number) : [],
    amenityIds: Array.isArray(f.amenityIds) ? f.amenityIds.map(Number) : [],
  };

  backToTop();
  items.value = []; total.value = null; hasMore.value = false; nextOffset.value = 0;
  reload();
  setTimeout(setupIO, 0);
}

/* 무한 스크롤 */
function setupIO(){
  teardownIO();
  if (!sentinel.value || !supportsIO.value) return;
  io = new IntersectionObserver((entries)=>{
    const ent = entries[0];
    if (!ent.isIntersecting) return;
    if (!hasMore.value || loading.value) return;
    loadPage(nextOffset.value, true);
  }, { root: null, rootMargin: "600px 0px 600px 0px", threshold: 0.01 });
  io.observe(sentinel.value);
}
function teardownIO(){ if (io) { io.disconnect(); io = null; } }

/* ✅ 쿼리 변하면 즉시 재조회 (마운트 시도 포함) */
watch(
  () => route.query,
  () => { syncFromRoute(); reload(); setTimeout(setupIO, 0); },
  { immediate: true }
);

onMounted(() => {
  supportsIO.value = typeof window !== "undefined" && "IntersectionObserver" in window;
  window.addEventListener("scroll", onScroll, { passive: true });
});
onBeforeUnmount(() => {
  teardownIO();
  window.removeEventListener("scroll", onScroll);
});

const totalLabel = computed(() => total.value==null? "" : `${total.value.toLocaleString("ko-KR")}개`);
</script>

<template>
  <div class="search-root">
    <div class="top-rail">
      <div class="searchbar-wrap">
        <SearchBar
          :q="q" :check-in="checkIn" :check-out="checkOut"
          :adults="adults" :children="children"
          @changed="onChanged"
          @submit="onSubmit"
        />
      </div>
    </div>

    <div class="page grid">
      <FiltersSidebar class="sidebar"
        :show-title="false"
        :init-price-min="filters.priceMin ?? 0"
        :init-price-max="filters.priceMax ?? 1000000"
        :min-bound="0" :max-bound="1000000"
        :init-grades="filters.grades"
        :init-rating-bands="filters.ratingBands"
        :init-amenity-ids="filters.amenityIds"
        @change="onFilterChange"
      />

      <div class="content">
        <div class="toolbar">
          <p class="total" v-if="total !== null">총 {{ totalLabel }}</p>
          <select v-model="sort" @change="onSortChange">
            <option value="rating">평점순(기본)</option>
            <option value="price_asc">낮은가격순</option>
            <option value="price_desc">높은가격순</option>
            <option value="grade_desc">성급순</option>
            <option value="reviews_desc">리뷰 많은순</option>
          </select>
        </div>

        <HotelList
          :items="items"
          :check-in="checkIn || ''"
          :check-out="checkOut || ''"
          :guests="adults + Math.max(0, children - 2)"
        />

        <p v-if="errorMsg" class="mt-6 text-center text-red-500">{{ errorMsg }}</p>
        <div v-if="loading" class="loading">로딩중...</div>

        <div ref="sentinel" class="sentinel" aria-hidden="true"></div>

        <div class="mt-6 text-center" v-if="hasMore && !supportsIO">
          <button class="px-5 py-3 border rounded-lg hover:bg-gray-50 disabled:opacity-50"
                  :disabled="loading || !isValidRange"
                  @click="loadMore">
            {{ loading ? '로딩중...' : '더보기' }}
          </button>
        </div>

        <div v-if="items.length === 0 && !loading && isValidRange"
             class="text-center text-gray-500 py-20">
          조건에 맞는 호텔이 없습니다.
        </div>
      </div>
    </div>

    <button
      v-if="showBackTop"
      class="backtop"
      @click.stop.prevent="backToTop"
      aria-label="맨 위로 이동">↑ 맨위로</button>
  </div>
</template>

<style scoped>
:root { --searchbar-max: 1160px; }
.top-rail{ background:#121a38; padding: 14px 0; }
.searchbar-wrap{ width:100%; max-width: var(--searchbar-max); margin:0 auto; padding:0 24px; }
.page{ width:100%; max-width:1560px; padding: 24px; margin:0 auto; box-sizing:border-box; }
.toolbar{ margin: 12px 0 8px; display:flex; justify-content:space-between; align-items:center; gap:12px; }
.toolbar select{ border:1px solid #d1d5db; border-radius:8px; padding:8px 10px; background:#fff; }
.total{ color:#6b7280; font-size:14px; }
.grid{ display:grid; grid-template-columns: 300px 1fr; gap:24px; }
.sidebar{ position:sticky; top:12px; align-self:start; }
.content{ min-width:0; }
.sentinel{ width:100%; height:1px; }
.loading{ text-align:center; color:#6b7280; padding:16px 0; }
.backtop{ position:fixed; right:24px; bottom:24px; padding:10px 12px; border-radius:999px; border:1px solid #e5e7eb; background:#ffffffcc; backdrop-filter: blur(6px); font-weight:700; box-shadow:0 6px 18px rgba(0,0,0,.08); }
.backtop:hover{ background:#fff; }
</style>

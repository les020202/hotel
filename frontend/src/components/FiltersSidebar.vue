<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { fetchAmenities } from "@/api/searchApi.js";

/* ===== Props ===== */
const props = defineProps({
  /* 상단 제목 노출 여부 (SearchView에서 false로 넘기면 숨김) */
  showTitle: { type: Boolean, default: true },

  /* 가격 초기/경계값 */
  initPriceMin: { type: Number, default: 0 },
  initPriceMax: { type: Number, default: 1_000_000 },
  minBound:     { type: Number, default: 0 },
  maxBound:     { type: Number, default: 1_000_000 },

  /* 필터 초기값 */
  initGrades:      { type: Array, default: () => [] },
  initRatingBands: { type: Array, default: () => [] },
  initAmenityIds:  { type: Array, default: () => [] },
});
const emit = defineEmits(["change"]);

/* ===== State ===== */
const priceMin = ref(props.initPriceMin);
const priceMax = ref(props.initPriceMax);
const grades   = ref([...props.initGrades]);          // [5,4] ...
const ratingBands = ref([...props.initRatingBands]);  // [1,2,3,4]
const amenityIds  = ref([...props.initAmenityIds]);   // [id...]

/* ===== Amenities (일부 숨김) ===== */
const rawAmenities = ref([]);
const HIDE_AMENITY_NAMES = new Set([
  "비치프론트/해변 접근","반려동물 동반 가능","비즈니스 센터","공항 셔틀",
  "무료 취소","스파/사우나","룸서비스","장애인 편의시설",
]);
const amenityOptions = computed(() =>
  rawAmenities.value.filter(a => !HIDE_AMENITY_NAMES.has(a.name))
);

onMounted(async () => {
  try { rawAmenities.value = await fetchAmenities("HOTEL"); }
  catch (e) { console.error("amenities load error", e); rawAmenities.value = []; }
  notify(); // 최초 1회 방출
});

/* ===== Slider / Input 동기화 & 교차 방지 ===== */
const STEP = 1000;
function clamp(v, lo, hi){
  const n = Math.round(v / STEP) * STEP;
  return Math.min(hi, Math.max(lo, n));
}

/* 두 핸들: min은 max-STEP 을 넘지 못함, max는 min+STEP 보다 작을 수 없음 */
function onMinThumb(e){
  const v = Number(e.target.value);
  priceMin.value = clamp(v, props.minBound, Math.max(props.minBound, priceMax.value - STEP));
}
function onMaxThumb(e){
  const v = Number(e.target.value);
  priceMax.value = clamp(v, Math.min(props.maxBound, priceMin.value + STEP), props.maxBound);
}

/* 인풋 직접 입력 시도 */
function onMinInput(e){
  const raw = String(e.target.value).replace(/[^\d]/g, "");
  priceMin.value = clamp(Number(raw || 0), props.minBound, Math.max(props.minBound, priceMax.value - STEP));
}
function onMaxInput(e){
  const raw = String(e.target.value).replace(/[^\d]/g, "");
  priceMax.value = clamp(Number(raw || 0), Math.min(props.maxBound, priceMin.value + STEP), props.maxBound);
}

/* 트랙 채움 비율 */
const minPct = computed(() =>
  ((priceMin.value - props.minBound) / (props.maxBound - props.minBound)) * 100
);
const maxPct = computed(() =>
  ((priceMax.value - props.minBound) / (props.maxBound - props.minBound)) * 100
);

/* ===== Emit (디바운스) ===== */
let t = null;
function notify(immediate=false){
  clearTimeout(t);
  const run = () => emit("change", {
    minPrice: Number(priceMin.value),
    maxPrice: Number(priceMax.value),
    grades: [...grades.value].sort((a,b)=>a-b),
    ratingBands: [...ratingBands.value].sort((a,b)=>a-b),
    amenityIds: [...amenityIds.value].map(Number),
  });
  immediate ? run() : (t = setTimeout(run, 300));
}

/* 가격 외 필터 바뀔 때도 디바운스 emit */
watch([grades, ratingBands, amenityIds], () => notify(false), { deep: true });

/* 토글 유틸 */
function toggle(arrRef, v){
  const i = arrRef.value.indexOf(v);
  if (i >= 0) arrRef.value.splice(i,1); else arrRef.value.push(v);
}
const toggleGrade      = (n)=> toggle(grades, n);
const toggleRatingBand = (n)=> toggle(ratingBands, n);

/* 가격 제외 필터 전체 해제 */
function clearNonPriceFilters(){
  grades.value = [];
  ratingBands.value = [];
  amenityIds.value = [];
  notify(true);
}

/* 라벨 */
function bandLabel(n){ return `${n}.0 – ${n===4?'5.0':`${n}.9`}`; }
</script>

<template>
  <aside class="filters">
    <h3 v-if="showTitle" class="ttl">Filters</h3>

    <!-- 가격 -->
    <section class="blk">
      <div class="hdr">가격</div>

      <!-- 한 줄 듀얼 핸들 슬라이더 -->
      <div class="track">
        <div class="track__bg"></div>
        <div class="track__fill" :style="{ left: minPct + '%', right: (100 - maxPct) + '%' }"></div>

        <!-- 두 range를 겹쳐서 thumb만 포인터 이벤트 -->
        <input class="thumb thumb--min"
               type="range" :min="minBound" :max="maxBound" :step="STEP"
               :value="priceMin" @input="onMinThumb" @change="notify(false)" />
        <input class="thumb thumb--max"
               type="range" :min="minBound" :max="maxBound" :step="STEP"
               :value="priceMax" @input="onMaxThumb" @change="notify(false)" />
      </div>

      <!-- 가격 인풋 (슬라이더와 동폭) -->
      <div class="price-row">
        <div class="price-box">
          <span class="cur">₩</span>
          <input class="price-input"
                 :value="priceMin.toLocaleString('ko-KR')"
                 @input="onMinInput" @change="notify(false)" />
        </div>
        <span class="dash">—</span>
        <div class="price-box">
          <span class="cur">₩</span>
          <input class="price-input"
                 :value="priceMax.toLocaleString('ko-KR')"
                 @input="onMaxInput" @change="notify(false)" />
        </div>
      </div>
    </section>

    <!-- 성급 + 전체 해제 -->
    <section class="blk">
      <div class="hdr hdr--with-action">
        <span>성급</span>
        <button class="reset-btn" type="button" @click="clearNonPriceFilters">필터 전체 해제</button>
      </div>
      <ul class="grade-list">
        <li v-for="n in [5,4,3,2,1]" :key="'g'+n">
          <label class="row">
            <input type="checkbox" class="ck" :value="n" v-model="grades" />
            <span class="stars" aria-hidden="true">
              <span v-for="i in n" :key="i" class="star">★</span>
            </span>
          </label>
        </li>
      </ul>
    </section>

    <!-- 평점 -->
    <section class="blk">
      <div class="hdr">평점</div>
      <ul class="rating-list">
        <li v-for="n in [4,3,2,1]" :key="'b'+n">
          <label class="row">
            <input type="checkbox" class="ck" :value="n" v-model="ratingBands" />
            <span class="band">
              <span class="star gold"></span>
              <span class="band-text">{{ bandLabel(n) }}</span>
            </span>
          </label>
        </li>
      </ul>
    </section>

    <!-- 어메니티 (숨김 리스트 제외, 전부 노출) -->
    <section class="blk">
      <div class="hdr">어메니티</div>
      <ul class="amen-list">
        <li v-for="a in amenityOptions" :key="a.id">
          <label class="row">
            <input type="checkbox" class="ck" :value="a.id" v-model="amenityIds" />
            <span>{{ a.name }}</span>
          </label>
        </li>
      </ul>
    </section>
  </aside>
</template>

<style scoped>
.filters{ width:300px; padding:8px; }
.ttl{ font-weight:800; font-size:20px; margin-bottom:10px; }
.blk{ padding:12px 6px; border-bottom:1px solid #eee; }
.hdr{ font-weight:700; margin-bottom:10px; display:flex; align-items:center; justify-content:space-between; }
.reset-btn{ font-size:12px; padding:4px 8px; border:1px solid #d1d5db; border-radius:8px; background:#fff; }
.reset-btn:hover{ background:#f9fafb; }

/* ===== Slider ===== */
.track{ position:relative; height:28px; margin:8px 0 6px; }
.track__bg{
  position:absolute; left:0; right:0; top:50%; transform:translateY(-50%);
  height:6px; background:#e5e7eb; border-radius:999px;
}
.track__fill{
  position:absolute; top:50%; transform:translateY(-50%);
  height:6px; background:#8fb5ff; border-radius:999px;
}

/* range 2개를 겹치되, thumb만 포인터 이벤트 */
.thumb{
  position:absolute; left:0; right:0; top:0; width:100%; height:28px; margin:0;
  appearance:none; background:transparent; pointer-events:none;
}
.thumb--min{ z-index:3; }
.thumb--max{ z-index:2; }

.thumb::-webkit-slider-thumb{
  appearance:none; width:20px; height:20px; border-radius:50%;
  background:#fff; border:3px solid #7da1ff; box-shadow:0 1px 2px rgba(0,0,0,.08);
  margin-top:-7px; cursor:pointer; pointer-events:auto;
}
.thumb::-moz-range-thumb{
  width:20px; height:20px; border-radius:50%;
  background:#fff; border:3px solid #7da1ff; cursor:pointer; pointer-events:auto;
}
.thumb::-webkit-slider-runnable-track,
.thumb::-moz-range-track{ background:transparent; }

/* 가격 인풋: 슬라이더와 유사한 폭(오버플로우 방지) */
.price-row{ display:flex; align-items:center; gap:10px; max-width:260px; }
.price-box{ position:relative; width:120px; }
.cur{ position:absolute; left:10px; top:50%; transform:translateY(-50%); color:#6b7280; font-size:14px; }
.price-input{ width:100%; padding:8px 10px 8px 24px; border:1px solid #d1d5db; border-radius:8px; font-size:14px; }
.dash{ color:#9ca3af; }

/* 리스트 공통 */
.row{ display:flex; align-items:center; gap:10px; cursor:pointer; }
.ck{ width:18px; height:18px; }
.grade-list, .rating-list, .amen-list{ display:flex; flex-direction:column; gap:10px; }

/* 별 색 */
.star, .gold{ color:#c78a00; }
.band{ display:flex; align-items:center; gap:8px; }
.band-text{ font-size:14px; color:#374151; }
</style>

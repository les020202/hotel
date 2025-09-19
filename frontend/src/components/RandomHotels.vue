<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { hotelsSearch, fetchMinPrices } from '@/api/auth'

const router = useRouter()
const hotels = ref([])
const minPrices = ref({})   // ✅ hotelId -> minPrice
const loading = ref(true)
const error = ref('')

const SIZE = 5

async function loadRandom() {
  loading.value = true
  error.value = ''
  try {
    // 1) 전체 개수
    const first = await hotelsSearch({ page: 0, size: 1 })
    const totalElements = first?.totalElements ?? first?.total ?? 0

    // 2) 랜덤 페이지
    const totalPagesBySize = Math.max(1, Math.ceil(totalElements / SIZE))
    const randomPage = Math.floor(Math.random() * totalPagesBySize)

    // 3) 호출
    const page = await hotelsSearch({ page: randomPage, size: SIZE })
    hotels.value = page?.content ?? page?.items ?? []

    // ✅ 4) 최저가 한번에 조회
    const ids = hotels.value.map(h => h.id).filter(Boolean)
    if (ids.length){
      minPrices.value = await fetchMinPrices(ids)
    } else {
      minPrices.value = {}
    }
  } catch (e) {
    error.value = e?.message || '랜덤 호텔을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function goDetail(h) {
  router.push({ path: '/search', query: { q: h.name, page: 0 } })
}
function onCardKey(h, e){
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    goDetail(h)
  }
}
function imgOf(h) {
  if (h.coverImageUrl) return h.coverImageUrl
  return `https://picsum.photos/seed/hotel-${h.id || Math.random()}/600/400`
}
function priceOf(h){
  const p = minPrices.value?.[h.id]
  return (p || p === 0) ? `₩${Number(p).toLocaleString('ko-KR')}` : '—'
}

onMounted(loadRandom)
</script>

<template>
  <section class="rand-wrap">
    <div class="head">
      <h2>추천 호텔</h2>
      <button class="refresh" @click="loadRandom" :disabled="loading">
        {{ loading ? '불러오는 중...' : '새로고침' }}
      </button>
    </div>

    <p v-if="error" class="err">{{ error }}</p>

    <div v-else class="grid">
      <article
        v-for="h in hotels"
        :key="h.id"
        class="card"
        role="button"
        tabindex="0"
        :aria-label="`${h.name} 상세 보기`"
        @click="goDetail(h)"
        @keydown="onCardKey(h, $event)"
      >
        <div class="img">
          <img :src="imgOf(h)" :alt="h.name" loading="lazy" />
        </div>
        <div class="body">
          <h3 class="name">{{ h.name }}</h3>

          <p class="sub">{{ h.address }}</p>

          <!-- ✅ 최저가 라인 -->
          <p class="price">
            <strong>{{ priceOf(h) }}</strong><span v-if="minPrices[h.id]">~</span>
          </p>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
/* 섹션 + 카드 고정 높이 변수 */
.rand-wrap{
  margin: 40px 0 8px;
  --card-h: 320px;        /* 카드 전체 높이 (필요 시 숫자만 바꿔 조절) */
}

.head{
  display:flex; align-items:center; justify-content:space-between;
  margin-bottom: 14px;
}
.head h2{ margin:0; font-size:20px; }
.refresh{
  height:32px; padding:0 12px; border:1px solid #e6e6e6; background:#fff; border-radius:8px;
  cursor:pointer; font-size:14px;
}
.refresh:disabled{ opacity:.6; cursor:not-allowed; }
.err{ color:#d00; margin:12px 0; }

/* 그리드 */
.grid{
  display:grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap:16px;
  align-items:stretch;
}

/* 카드 */
.card{
  background:#fff; border:1px solid #eee; border-radius:16px; overflow:hidden;
  display:flex; flex-direction:column;
  cursor:pointer;
  transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease;
  will-change: transform, box-shadow, border-color;
  height: var(--card-h);             /* ✅ 높이 고정 */
}
.card:hover, .card:focus-within{
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(0,0,0,.14);
  border-color:#e3e3e3;
}
.card:focus-visible{ outline:3px solid #111; outline-offset:2px; }

/* 썸네일(호버시 확대) */
.img{ height:160px; background:#f3f3f3; position:relative; overflow:hidden; }
.img img{
  width:100%; height:100%; object-fit:cover; display:block;
  transition: transform .35s ease; will-change: transform;
}
.img::after{
  content:""; position:absolute; inset:0;
  background: radial-gradient(ellipse at center, rgba(0,0,0,.08), rgba(0,0,0,0) 70%);
  opacity:0; transition: opacity .25s ease; pointer-events:none;
}
.card:hover .img img, .card:focus-within .img img{ transform: scale(1.05); }
.card:hover .img::after, .card:focus-within .img::after{ opacity:.18; }

/* 본문 */
.body{
  padding:12px 14px 16px;
  flex:1;
  display:flex; flex-direction:column; gap:6px;
}

/* 제목 1줄 고정 + 말줄임 */
.name{
  margin:0 0 6px;
  font-size:16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 주소 2줄 고정 + 말줄임 */
.sub{
  margin:0; color:#777; font-size:13px; line-height:1.3;
  display:-webkit-box;
  -webkit-line-clamp:2;
  -webkit-box-orient:vertical;
  overflow:hidden;
}

/* 보조 메타(필요 시 사용) */
.meta{ margin:0 0 6px; color:#444; font-size:14px; }

/* ✅ 가격: 카드 하단 고정 + 폰트 업 */
.price{
  margin-top:auto;                 /* 아래로 밀착 */
  padding-top:10px;
  border-top:1px solid #f2f2f2;
  color:#111;
  font-size:15px;
  line-height:1.15;
}
.price strong{
  font-size:20px;
  font-weight:800;
}

/* 반응형 */
@media (max-width:1280px){ .grid{ grid-template-columns: repeat(4, 1fr); } }
@media (max-width:1000px){
  .grid{ grid-template-columns: repeat(3, 1fr); }
  .rand-wrap{ --card-h: 300px; }   /* 카드 높이 미세조정 */
}
@media (max-width:700px){
  .grid{ grid-template-columns: repeat(2, 1fr); }
  .rand-wrap{ --card-h: 290px; }
}
@media (max-width:480px){
  .grid{ grid-template-columns: 1fr; }
  .rand-wrap{ --card-h: 280px; }
}
</style>


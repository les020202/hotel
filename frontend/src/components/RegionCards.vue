<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 외부 이미지 URL (안전용 기본 이미지)
const fallback = 'https://picsum.photos/seed/hotel/800/500'

// ✅ DB의 region 값에 맞춰 query 세팅 (사용 중인 이미지 그대로)
const REGIONS = [
  { title: '서울',     query: '서울특별시', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Lotte_World_day_view_2.jpg/1920px-Lotte_World_day_view_2.jpg', avg: 120000 },
  { title: '경기도',   query: '경기도',     image: 'https://www.dynastykorea.com/wp-content/uploads/2020/11/Hwaseong-Fortress-Suwon-Korea.webp', avg:  95000 },
  { title: '인천',     query: '인천광역시', image: 'https://res.klook.com/image/upload/fl_lossy.progressive,q_60/Mobile/City/wwtolyueeed5sztqmjfi.jpg',   avg: 110000 },
  { title: '대전',     query: '대전광역시', image: 'https://cdn.imweb.me/thumbnail/20220813/2691735ea4093.jpg',   avg:  95000 },
  { title: '충청남도', query: '충청남도',   image: 'https://koreatravelpost.com/wp-content/uploads/2021/02/Chungcheong-Mancheonha-Skywalk-1.jpg', avg:  90000 },
  { title: '충청북도', query: '충청북도',   image: 'https://english.visitkorea.or.kr/public/internal/ShoppingSpecial/img/explorelocals/detail/chungcheongbuk-do/chungbuk.png', avg:  90000 },
  { title: '울산',     query: '울산광역시', image: 'https://live.staticflickr.com/65535/49364909593_5a09606373_b.jpg',     avg: 105000 },
  { title: '대구',     query: '대구광역시', image: 'https://i0.wp.com/mytravelation.com/wp-content/uploads/2023/11/83-Tower-Daegu.jpg?resize=723%2C483&ssl=1',     avg:  95000 },
  { title: '부산',     query: '부산광역시', image: 'https://cdn.ardentnews.co.kr/news/photo/202506/6452_30483_1545.jpg',     avg: 100000 },
  { title: '경상남도', query: '경상남도',   image: 'https://world-tour.in/storage/2019/12/south-korea-gyeongsangnam-do-jirisan-national-park-1383860054-768x512.jpg', avg:  95000 },
  { title: '경상북도', query: '경상북도',   image: 'https://koreatravelplanning.com/wp-content/uploads/2022/08/Donggung-Palace-Wolji-Pond-Night-Illuminations-in-Gyeongju.jpg', avg:  90000 },
  { title: '광주',     query: '광주광역시', image: 'https://skyticket.com/guide/wp-content/uploads/2025/03/pixta_33524211_S-e1546827427627.jpg',   avg:  95000 },
  { title: '전라남도', query: '전라남도',   image: 'https://images.indianexpress.com/2023/06/yeosu.jpg',   avg:  90000 },
  { title: '전라북도', query: '전라북도',   image: 'https://blog.trazy.com/wp-content/uploads/2023/07/jeonju-hanok-village-guide-featured-image-scaled-1200x720.jpg',   avg:  90000 },
  { title: '강원도',   query: '강원도',     image: 'https://dynamic-media-cdn.tripadvisor.com/media/photo-o/18/38/1d/14/pavilion-along-the-coast.jpg?w=500&h=500&s=1',   avg:  95000 },
]

const PAGE_SIZE = 4
const page   = ref(0)
const pages  = Math.ceil(REGIONS.length / PAGE_SIZE)
const slice  = computed(() => {
  const start = page.value * PAGE_SIZE
  return REGIONS.slice(start, start + PAGE_SIZE)
})

function prev () { page.value = (page.value - 1 + pages) % pages }
function next () { page.value = (page.value + 1) % pages }

function goRegion (r) {
  router.push({ path: '/search', query: { region: r.query, regionExact: 'true' } })
}
function onCardKey(r, e){
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    goRegion(r)
  }
}
</script>

<template>
  <section class="wrap">
    <div class="head">
      <h2>여행에 빠지다</h2>
      <div class="right"><div class="pager"></div></div>
    </div>

    <div class="carousel">
      <!-- 왼쪽 화살표 -->
      <button class="edge left" aria-label="이전" @click="prev"></button>

      <!-- 카드 4개 그리드 (★ 카드 전체가 버튼 역할) -->
      <div class="grid">
        <article
          v-for="r in slice" :key="r.query"
          class="card"
          role="button" tabindex="0"
          :aria-label="`${r.title} 지역 호텔 보기`"
          @click="goRegion(r)"
          @keydown="onCardKey(r, $event)"
        >
          <div class="img">
            <img :src="r.image || fallback" :alt="r.title" loading="lazy" />
          </div>
          <div class="body">
            <h3>{{ r.title }}</h3>
            <p class="meta">평균가 ₩{{ r.avg.toLocaleString() }}</p>
          </div>
        </article>
      </div>

      <!-- 오른쪽 화살표 -->
      <button class="edge right" aria-label="다음" @click="next"></button>
    </div>
  </section>
</template>

<style scoped>
.wrap { margin: 32px 0 8px; }
.head { display:flex; align-items:center; justify-content:space-between; margin-bottom: 12px; }
.head h2 { margin:0; }
.right { display:flex; align-items:center; gap:12px; }

.grid {
  display:grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.card {
  background:#fff; border:1px solid #eee; border-radius:16px; overflow:hidden;
  display:flex; flex-direction:column;
  cursor: pointer;                               /* ★ 카드 자체를 버튼처럼 */
  transition: transform .18s ease, box-shadow .18s ease;
  will-change: transform, box-shadow;
}
.card:focus-visible{
  outline: 3px solid #111; outline-offset: 2px;   /* ★ 키보드 접근성 */
}

.img { height:180px; background:#f3f3f3; position:relative; overflow:hidden; }
.img img { width:100%; height:100%; object-fit:cover; display:block; transition: transform .35s ease; will-change: transform; }
.img::after{
  content:""; position:absolute; inset:0;
  background: radial-gradient(ellipse at center, rgba(0,0,0,.08), rgba(0,0,0,0) 70%);
  opacity:0; transition: opacity .25s ease; pointer-events:none;
}

.body { padding:12px 14px 16px; }
h3 { margin:0 0 6px; }
.meta { margin:0; color:#666; font-size:14px; }

/* Hover / Focus 효과 (카드 기준) */
.card:hover, .card:focus-within{ transform: translateY(-6px); box-shadow: 0 12px 28px rgba(0,0,0,.14); }
.card:hover .img img, .card:focus-within .img img{ transform: scale(1.05); }
.card:hover .img::after, .card:focus-within .img::after{ opacity:.18; }

/* === 화살표(기존 스타일 유지) === */
.carousel { position: relative; }
.edge{
  position: absolute; top: 50%; transform: translateY(-50%);
  width: 72px; height: 220px; background: transparent; border: none; cursor: pointer; z-index: 5; padding: 0;
}
.edge.left  { left: -70px; }
.edge.right { right: -70px; }
.edge::before{
  content: ""; position: absolute; left: 50%; top: 50%;
  width: 28px; height: 28px; border-top: 4px solid #bfc3c7; border-right: 4px solid #bfc3c7;
  transform: translate(-50%, -50%) rotate(45deg); transition: border-color .2s ease, transform .15s ease;
  filter: drop-shadow(0 2px 8px rgba(0,0,0,.07));
}
.edge.left::before{ transform: translate(-50%, -50%) rotate(225deg); }
.edge:hover::before{ border-color:#9aa0a6; }
.edge:active::before{ transform: translate(-50%, -50%) scale(.96) rotate(45deg); }
.edge.left:active::before{ transform: translate(-50%, -50%) scale(.96) rotate(225deg); }

@media (max-width: 1000px){ .grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px){ .grid { grid-template-columns: 1fr; } }
@media (max-width: 1200px){
  .edge{ width: 56px; height: 180px; }
  .edge.left{ left: -32px; }
  .edge.right{ right: -32px; }
  .edge::before{ width: 24px; height: 24px; border-width: 3px; }
}
@media (max-width: 800px){
  .edge{ width: 44px; height: 150px; }
  .edge.left{ left: -22px; }
  .edge.right{ right: -22px; }
  .edge::before{ width: 20px; height: 20px; border-width: 3px; }
}
</style>

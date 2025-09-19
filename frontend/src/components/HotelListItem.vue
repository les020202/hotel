<script setup>
import { computed } from 'vue'

const props = defineProps({
  hotel: { type: Object, required: true },
})

// 대표 이미지(없으면 placeholder)
const cover = computed(() =>
  props.hotel.coverImageUrl ||
  `https://picsum.photos/seed/hotel-${props.hotel.id}/420/260`
)

// 성급(별 개수): gradeLevel 우선, 없으면 officialGrade에서 “n성” 추출
const stars = computed(() => {
  if (props.hotel.gradeLevel) return Math.min(5, Math.max(1, Number(props.hotel.gradeLevel)))
  const m = String(props.hotel.officialGrade || '').match(/([0-9])\s*성/)
  return m ? Math.min(5, Number(m[1])) : 0
})
const starArray = computed(() => Array.from({ length: 5 }, (_, i) => i < stars.value))

// 위치 표시
const locationText = computed(() =>
  [props.hotel.region, props.hotel.address].filter(Boolean).join(' · ')
)
</script>

<template>
  <article class="hl-item">
    <!-- 좌측 썸네일 -->
    <router-link :to="`/hotels/${hotel.id}`" class="thumb">
  <img :src="hotel.coverImageUrl || fallback" :alt="hotel.name" />
</router-link>

    <!-- 중앙 정보 -->
    <div class="middle">
      <h3 class="title">{{ hotel.name }}</h3>
      <div class="loc">{{ locationText }}</div>

      <div class="meta">
        <span class="stars" :aria-label="`성급 ${stars}`">
          <svg v-for="(filled, idx) in starArray" :key="idx" viewBox="0 0 20 20"
               class="star" :class="filled ? 'filled' : 'empty'">
            <path d="M10 1.6l2.47 5.01 5.53.8-4 3.9.94 5.5L10 14.9 5.06 16.8l.94-5.5-4-3.9 5.53-.8L10 1.6z"/>
          </svg>
          <span class="grade" v-if="hotel.officialGrade"> / {{ hotel.officialGrade }}</span>
        </span>

        <span class="rating" v-if="hotel.rating">
          평점 <b>{{ Number(hotel.rating).toFixed(1) }}</b>
        </span>
      </div>
      <div class="actions">
        <button class="btn ghost">♡ 찜하기</button>
      </div>
    </div>

    <!-- 우측 강조 영역 -->
    <aside class="right">
      <div class="score" v-if="hotel.rating">
        <strong>{{ Number(hotel.rating).toFixed(1) }}</strong>
        <span>이용 후기</span>
      </div>
      <div class="grade-pill" v-if="stars">성급 {{ stars }}</div>
    </aside>
  </article>
</template>

<style scoped>
.hl-item{
  display:grid;
  grid-template-columns: 260px 1fr 140px;
  gap:18px;
  padding:16px;
  border:1px solid #eee;
  border-radius:14px;
  background:#fff;
}
.thumb{ position:relative; display:block; border-radius:10px; overflow:hidden; }
.thumb img{ width:100%; height:100%; object-fit:cover; display:block; }
.thumb-badge{
  position:absolute; right:10px; bottom:10px;
  padding:6px 10px; font-size:12px; color:#fff; background:rgba(0,0,0,.55);
  border-radius:8px;
}

.middle{ display:flex; flex-direction:column; gap:8px; }
.title{ margin:0; font-size:18px; font-weight:700; }
.loc{ color:#666; font-size:14px; }
.meta{ display:flex; align-items:center; gap:10px; color:#444; }
.stars{ display:inline-flex; align-items:center; gap:4px; }
.star{ width:16px; height:16px; }
.filled path{ fill:#f8b500 }
.empty  path{ fill:#e5e7eb }
.grade{ color:#777; font-size:13px; margin-left:2px; }

.chips{ display:flex; gap:8px; flex-wrap:wrap; }
.chip{ font-size:12px; padding:4px 8px; background:#f3f4f6; border-radius:999px; color:#555; }

.actions{ display:flex; gap:8px; margin-top:6px; }
.btn{ padding:8px 12px; border-radius:8px; border:1px solid #111; background:#111; color:#fff; font-size:14px; }
.btn.ghost{ border:1px solid #e5e7eb; background:#fff; color:#111; }

.right{ display:flex; flex-direction:column; align-items:flex-end; justify-content:space-between; padding:6px 0; }
.score strong{ font-size:22px; }
.score span{ display:block; color:#777; font-size:12px; margin-top:2px; text-align:right; }
.grade-pill{
  background:#111; color:#fff; padding:6px 10px; border-radius:999px; font-size:12px;
}
:root{
  --thumb-w: 260px; /* 필요하면 여기 숫자만 바꿔서 전체 썸네일 크기 조절 */
  --thumb-h: 160px;
}
.thumb{ width: var(--thumb-w); aspect-ratio: 16/10; height: auto; }
.thumb img{ width:100%; height:100%; object-fit:cover; }

@media (max-width: 1000px){
  .hl-item{ grid-template-columns: 220px 1fr; }
  .right{ display:none; }
}
@media (max-width: 640px){
  .hl-item{ grid-template-columns: 1fr; }
}
</style>

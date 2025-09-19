<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import SearchBar from '@/components/SearchBar.vue'

/**
 * 커스터마이즈 가능한 프롭들
 * - images: 배경 이미지 목록
 * - height: 히어로 섹션 높이(px/문자)
 * - rotateMs: 자동 교체 간격(ms). 0이면 자동 교체 안 함
 * - title / subtitle: 상단 문구
 */
const props = defineProps({
  images: {
    type: Array,
    default: () => [
      'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?q=80&w=1600&auto=format&fit=crop',
      'https://images.unsplash.com/photo-1491553895911-0055eca6402d?q=80&w=1600&auto=format&fit=crop',
      'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?q=80&w=1600&auto=format&fit=crop',
      'https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1?q=80&w=1600&auto=format&fit=crop',
    ]
  },
  height: { type: [Number, String], default: 420 },
  rotateMs: { type: Number, default: 0 },
  title: { type: String, default: 'hotel' },
  subtitle: { type: String, default: '쉽고 빠르게 예약' },
})

const hero = ref('')
let timer = null

function pick() {
  if (!props.images?.length) return
  hero.value = props.images[Math.floor(Math.random() * props.images.length)]
}

onMounted(() => {
  pick()
  if (props.rotateMs > 0) {
    timer = setInterval(pick, props.rotateMs)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <section
    class="hero"
    :style="{
      '--hero-url': `url('${hero}')`,
      '--hero-h': typeof height === 'number' ? height + 'px' : String(height)
    }"
  >
    <div class="hero__scrim"></div>

    <div class="hero__inner">
      <h1 class="hero__title">{{ title }}</h1>
      <p class="hero__subtitle">{{ subtitle }}</p>

      <!-- SearchBar는 내부에 검색 로직이 있으므로
           v-model 전달 없이 그대로 사용해도 동작 -->
      <div class="hero__search">
        <SearchBar />
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero{
  position: relative;
  height: var(--hero-h);
  border-radius: 20px;
  overflow: hidden;
  background: center/cover no-repeat var(--hero-url);
}
.hero__scrim{
  position:absolute; inset:0;
  background: linear-gradient(180deg, rgba(0,0,0,.45), rgba(0,0,0,.10));
}
.hero__inner{
  position:absolute; inset:0;
  padding: 24px;
  display:flex; flex-direction:column;
  align-items:center; justify-content:center;
  gap: 18px; color:#fff; text-align:center;
}
.hero__title{ font-size: 38px; font-weight: 800; letter-spacing:-.3px; margin:0; }
.hero__subtitle{ margin:0; opacity:.96; }

.hero__search{
  width: min(980px, 96%);
  background:#fff;
  border-radius: 20px;
  padding: 14px;
  box-shadow: 0 16px 36px rgba(0,0,0,.20);
}

/* SearchBar 기본 박스(테두리/그림자) 제거해서 깨끗하게 */
:deep(.search-box){ border:none; background:transparent; box-shadow:none; padding:0; }
/* 입력 높이 조금 키우기(원하면 조절) */
:deep(.inp){ height: 52px; }

@media (max-width: 900px){
  .hero{ height: 320px; }
  .hero__title{ font-size: 28px; }
  .hero__search{ padding: 10px; border-radius: 14px; }
}
</style>

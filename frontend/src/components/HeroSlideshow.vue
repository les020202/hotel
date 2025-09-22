<!-- /src/components/HeroSlideshow.vue -->
<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  images: { type: Array, required: true },   // [{src, alt}]
  interval: { type: Number, default: 15000 }
})

const idx = ref(0)
let timer
function start(){
  stop()
  timer = setInterval(() => {
    idx.value = (idx.value + 1) % props.images.length
  }, props.interval)
}
function stop(){ if (timer) clearInterval(timer) }

onMounted(start)
onUnmounted(stop)
watch(() => props.interval, start)
</script>

<template>
  <div class="hero-slideshow" role="img" :aria-label="images[idx]?.alt || 'hero image'">
    <!-- 트랙을 좌로 이동시키는 방식 -->
    <div class="track" :style="{ transform: `translateX(-${idx * 100}%)` }">
      <div class="slide" v-for="(im,i) in images" :key="i"
           :style="{ backgroundImage: `url(${im.src})` }"></div>
    </div>

    <div class="dots" v-if="images.length > 1">
      <button v-for="(it,i) in images" :key="i"
              :class="['dot',{active: i===idx}]"
              @click="idx=i" :aria-label="`슬라이드 ${i+1}`"/>
    </div>
  </div>
</template>

<style scoped>
.hero-slideshow{ position:absolute; inset:0; overflow:hidden; }
.track{
  display:flex; width:100%; height:100%;
  transition: transform .6s ease;          /* ✅ 옆으로 미끄러짐 */
}
.slide{
  flex:0 0 100%; height:100%;
  background-size:cover;
  background-position:center;               /* 필요시 center 70% 등으로 조정 */
  will-change: transform;
}

/* 점 인디케이터 */
.dots{ position:absolute; left:14px; bottom:12px; display:flex; gap:8px; z-index:3 }
.dot{
  width:7px; height:7px; border-radius:999px;
  background: rgba(255, 255, 255, 0.644);
  border:1px solid rgba(255,255,255,.9);
  box-shadow:0 2px 6px rgba(0,0,0,.25);
  opacity:.75; transition:width .18s ease, opacity .18s ease;
}
.dot.active{ width:18px; opacity:1; background:#fff }
</style>

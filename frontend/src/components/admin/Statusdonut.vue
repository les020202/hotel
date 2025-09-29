<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: { type: Number, default: 160 },
  settled: { type: Number, default: 0 },
  planned: { type: Number, default: 0 },
  nulled: { type: Number, default: 0 },
  settledColor: { type: String, default: '#16a34a' },
  plannedColor: { type: String, default: '#1d4ed8' },
  nullColor: { type: String, default: '#ef4444' },
  ringColor: { type: String, default: '#e5eaf2' }
})

const total = computed(() => props.settled + props.planned + props.nulled)

// ✅ 크기에 따라 항상 정중앙/정반경 사용
const c = computed(() => props.size / 2)
const r = computed(() => props.size / 2 - 10)
const circumference = computed(() => 2 * Math.PI * r.value)

const settledPct = computed(() => props.settled / (total.value || 1))
const plannedPct = computed(() => props.planned / (total.value || 1))
const nulledPct  = computed(() => props.nulled  / (total.value || 1))

const dashSettled = computed(() => circumference.value * settledPct.value)
const dashPlanned = computed(() => circumference.value * plannedPct.value)
const dashNulled  = computed(() => circumference.value * nulledPct.value)
</script>

<template>
  <div class="donut-card">
    <svg
      class="donut"
      :width="size" :height="size"
      :viewBox="`0 0 ${size} ${size}`"
    >
      <circle class="ring" :cx="c" :cy="c" :r="r" :stroke="ringColor" stroke-width="18" fill="none" />
      <circle class="seg"  :cx="c" :cy="c" :r="r"
        :stroke="settledColor"
        :stroke-dasharray="`${dashSettled} ${circumference - dashSettled}`"
        stroke-width="18" fill="none"
        :transform="`rotate(-90 ${c} ${c})`"
      />
      <circle class="seg"  :cx="c" :cy="c" :r="r"
        :stroke="plannedColor"
        :stroke-dasharray="`${dashPlanned} ${circumference - dashPlanned}`"
        :stroke-dashoffset="-dashSettled"
        stroke-width="18" fill="none"
        :transform="`rotate(-90 ${c} ${c})`"
      />
      <circle class="seg"  :cx="c" :cy="c" :r="r"
        :stroke="nullColor"
        :stroke-dasharray="`${dashNulled} ${circumference - dashNulled}`"
        :stroke-dashoffset="-(dashSettled + dashPlanned)"
        stroke-width="18" fill="none"
        :transform="`rotate(-90 ${c} ${c})`"
      />
      <text :x="c" :y="c - 5" text-anchor="middle" class="total">{{ total }}</text>
      <text :x="c" :y="c + 12" text-anchor="middle" class="label">정산 진행 상태</text>
    </svg>

    <ul class="legend legend--compact" aria-label="상태 색상 안내">
      <li class="li"><span class="dot" :style="{ background: settledColor }"></span><span class="name">완료</span></li>
      <li class="li"><span class="dot" :style="{ background: plannedColor }"></span><span class="name">예정</span></li>
      <li class="li"><span class="dot" :style="{ background: nullColor }"></span><span class="name">미완료</span></li>
    </ul>
  </div>
</template>

<style scoped>
/* ✅ 카드 안에서 수평·수직 중앙 정렬 */
.donut-card{
  display:flex;
  flex-direction:column;
  align-items:center;
  justify-content:center;
  text-align:center;
}

/* ✅ svg를 인라인 대신 블록으로 -> 가운데 정확히 */
.donut{
  display:block;
  margin:0 auto;
}

/* 텍스트 더 작게 */
.total { font-size:16px; font-weight:800; fill:#0f172a; }
.label { font-size:10px;  fill:#6b7280; }

/* 범례: 한 줄, 가운데 */
.legend.legend--compact{
  display:flex; align-items:center; justify-content:center;
  gap:10px; margin-top:8px; flex-wrap:nowrap;
}
.legend.legend--compact .li{ display:inline-flex; align-items:center; gap:6px; font-size:11px; }
.legend.legend--compact .dot{ width:8px; height:8px; border-radius:50%; }
.legend.legend--compact .name{ font-weight:700; color:#475569; }
</style>

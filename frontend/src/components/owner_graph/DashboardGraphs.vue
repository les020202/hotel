<script setup lang="ts">
/**
 * 그래프는 현재 더미데이터로만 렌더링합니다.
 * 실제 연동 시에는 props로 값만 주입하면 됩니다.
 */
import { computed } from 'vue'

/* ===== 더미 데이터 ===== */
const remainingByType = [
  { label: '싱글',  value: 3 },
  { label: '더블',  value: 2 },
  { label: '디럭스', value: 1 },
  { label: '스위트', value: 1 },
]

const weeklyBookings = [
  { label: '일', v: 1 },
  { label: '월', v: 0 },
  { label: '화', v: 1 },
  { label: '수', v: 0 },
  { label: '목', v: 0 },
  { label: '금', v: 0 },
  { label: '토', v: 0 },
]

const checkinByHour = [
  { h: '09', v: 0 }, { h: '10', v: 0 }, { h: '11', v: 0 },
  { h: '12', v: 0 }, { h: '13', v: 0 }, { h: '14', v: 0 },
  { h: '15', v: 0 }, { h: '16', v: 1 }, { h: '17', v: 0 },
]

/* ===== 간단 유틸 ===== */
const maxRemain = computed(() => Math.max(1, ...remainingByType.map(d => d.value)))
const maxWeek   = computed(() => Math.max(1, ...weeklyBookings.map(d => d.v)))
const maxHour   = computed(() => Math.max(1, ...checkinByHour.map(d => d.v)))

function scaleY(v: number, h: number, pad: number, vmax: number) {
  const usable = h - pad * 2
  return pad + (usable - (v / (vmax || 1)) * usable)
}
function barX(i: number, n: number, w: number, pad: number) {
  const bw = (w - pad * 2) / n
  return pad + i * bw + 6
}
function barW(n: number, w: number, pad: number) {
  const bw = (w - pad * 2) / n
  return Math.max(6, bw - 12)
}
</script>

<template>
  <!-- 그래프 3개 영역 -->
  <div class="mt-8 grid grid-cols-1 lg:grid-cols-3 gap-6">
    <!-- 1) 오늘 잔여 객실 (룸타입 분포) -->
    <div class="rounded-2xl p-4 bg-white border">
      <div class="text-sm font-semibold mb-2">오늘 잔여 객실 (룸타입 분포)</div>
      <svg :width="360" :height="180" viewBox="0 0 360 180" class="w-full">
        <line x1="24" y1="156" x2="336" y2="156" stroke="#e5e7eb" />
        <g v-for="(d, i) in remainingByType" :key="d.label">
          <rect
            :x="barX(i, remainingByType.length, 360, 24)"
            :y="scaleY(d.value, 180, 24, maxRemain)"
            :width="barW(remainingByType.length, 360, 24)"
            :height="156 - scaleY(d.value, 180, 24, maxRemain)"
            rx="6"
            class="fill-gray-800/80"
          />
          <text
            :x="barX(i, remainingByType.length, 360, 24) + barW(remainingByType.length, 360, 24)/2"
            y="170" text-anchor="middle" class="text-[10px] fill-gray-500 select-none">
            {{ d.label }}
          </text>
          <text
            :x="barX(i, remainingByType.length, 360, 24) + barW(remainingByType.length, 360, 24)/2"
            :y="scaleY(d.value, 180, 24, maxRemain) - 6"
            text-anchor="middle" class="text-[10px] fill-gray-600 select-none">
            {{ d.value }}
          </text>
        </g>
      </svg>
      <p class="text-xs text-gray-400 mt-2">* 더미 데이터</p>
    </div>

    <!-- 2) 이번 주 예약 수(일~토) -->
    <div class="rounded-2xl p-4 bg-white border">
      <div class="text-sm font-semibold mb-2">이번 주 예약 추이 (일~토)</div>
      <svg :width="360" :height="180" viewBox="0 0 360 180" class="w-full">
        <line x1="24" y1="156" x2="336" y2="156" stroke="#e5e7eb" />
        <polyline
          :points="weeklyBookings.map((d, i) => {
              const x = 24 + i * ((336-24) / (weeklyBookings.length-1))
              const y = scaleY(d.v, 180, 24, maxWeek)
              return `${x},${y}`
            }).join(' ')"
          fill="none" stroke="#111827" stroke-width="2" />
        <g v-for="(d, i) in weeklyBookings" :key="i">
          <circle
            :cx="24 + i * ((336-24) / (weeklyBookings.length-1))"
            :cy="scaleY(d.v, 180, 24, maxWeek)"
            r="3" fill="#111827" />
          <text
            :x="24 + i * ((336-24) / (weeklyBookings.length-1))"
            y="170" text-anchor="middle" class="text-[10px] fill-gray-500 select-none">
            {{ d.label }}
          </text>
          <text
            :x="24 + i * ((336-24) / (weeklyBookings.length-1))"
            :y="scaleY(d.v, 180, 24, maxWeek) - 8"
            text-anchor="middle" class="text-[10px] fill-gray-600 select-none">
            {{ d.v }}
          </text>
        </g>
      </svg>
      <p class="text-xs text-gray-400 mt-2">* 더미 데이터</p>
    </div>

    <!-- 3) 시간대별 체크인 예정 -->
    <div class="rounded-2xl p-4 bg-white border">
      <div class="text-sm font-semibold mb-2">시간대별 체크인 예정</div>
      <svg :width="360" :height="180" viewBox="0 0 360 180" class="w-full">
        <line x1="24" y1="156" x2="336" y2="156" stroke="#e5e7eb" />
        <g v-for="(d, i) in checkinByHour" :key="d.h">
          <rect
            :x="barX(i, checkinByHour.length, 360, 24)"
            :y="scaleY(d.v, 180, 24, maxHour)"
            :width="barW(checkinByHour.length, 360, 24)"
            :height="156 - scaleY(d.v, 180, 24, maxHour)"
            rx="6"
            class="fill-gray-800/80"
          />
          <text
            :x="barX(i, checkinByHour.length, 360, 24) + barW(checkinByHour.length, 360, 24)/2"
            y="170" text-anchor="middle" class="text-[10px] fill-gray-500 select-none">
            {{ d.h }}시
          </text>
          <text
            :x="barX(i, checkinByHour.length, 360, 24) + barW(checkinByHour.length, 360, 24)/2"
            :y="scaleY(d.v, 180, 24, maxHour) - 6"
            text-anchor="middle" class="text-[10px] fill-gray-600 select-none">
            {{ d.v }}
          </text>
        </g>
      </svg>
      <p class="text-xs text-gray-400 mt-2">* 더미 데이터</p>
    </div>
  </div>
</template>
<style scoped>

</style>

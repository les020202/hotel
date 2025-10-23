<!-- src/views/system/TooManyRequests.vue -->
<template>
  <div class="min-h-screen bg-neutral-50 dark:bg-neutral-900 flex items-center justify-center px-4">
    <div
      class="w-full max-w-md rounded-2xl border border-neutral-200/70 dark:border-neutral-700/60 bg-white/90 dark:bg-neutral-800/80 shadow-lg backdrop-blur
             animate-[pop_.32s_ease-out]"
      role="alertdialog"
      aria-labelledby="rate-limit-title"
      aria-describedby="rate-limit-desc"
    >
      <!-- 헤더 아이콘/타이틀 -->
      <div class="p-6 pb-3 text-center">
        <div class="mx-auto mb-4 w-12 h-12 rounded-full grid place-items-center
                    bg-gradient-to-br from-indigo-500/15 to-sky-500/15
                    ring-1 ring-indigo-400/30 dark:ring-indigo-300/20">
          <!-- 모래시계 아이콘 -->
          <svg viewBox="0 0 24 24" class="w-6 h-6 text-indigo-600 dark:text-indigo-300">
            <path fill="currentColor"
                  d="M6 2h12a1 1 0 0 1 1 1v2a5 5 0 0 1-2.5 4.33l-1.5.9a2 2 0 0 0-1 1.74v.06a2 2 0 0 0 1 1.74l1.5.9A5 5 0 0 1 19 20v2a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-2a5 5 0 0 1 2.5-4.33l1.5-.9a2 2 0 0 0 1-1.74v-.06a2 2 0 0 0-1-1.74l-1.5-.9A5 5 0 0 1 5 5V3a1 1 0 0 1 1-1Zm1 3a3 3 0 0 0 1.5 2.6l1.5.9A4 4 0 0 1 12 10a4 4 0 0 1 2-1.5l1.5-.9A3 3 0 0 0 17 5V4H7v1Zm10 14a3 3 0 0 0-1.5-2.6l-1.5-.9A4 4 0 0 1 12 14a4 4 0 0 1-2 1.5l-1.5.9A3 3 0 0 0 7 19v1h10v-1Z"/>
          </svg>
        </div>

        <h1 id="rate-limit-title" class="text-xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
          요청이 너무 많습니다
        </h1>
        <p id="rate-limit-desc" class="mt-1.5 text-sm text-neutral-600 dark:text-neutral-400">
          잠시 후에 다시 시도해주세요.
        </p>

        <!-- 남은 시간 뱃지 -->
        <div class="mt-3 inline-flex items-center gap-2">
          <span
            class="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium
                   bg-neutral-100 text-neutral-700 ring-1 ring-black/5
                   dark:bg-neutral-700/60 dark:text-neutral-200">
            {{ ready ? '이제 다시 시도할 수 있어요' : `대기시간 ${mm}:${ss}` }}
          </span>
        </div>
      </div>

      <!-- 버튼 -->
      <div class="px-6 pt-4 pb-6 flex items-center justify-center gap-2">
        <button
          type="button"
          :disabled="!ready"
          @click="retry"
          class="rounded-lg px-4 py-2 text-sm font-medium
                 text-white disabled:text-white/60
                 bg-indigo-600 hover:bg-indigo-600/90 disabled:bg-indigo-400
                 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/70"
        >
          다시 시도
          <span v-if="!ready" class="ml-1.5 opacity-80">({{ mm }}:{{ ss }})</span>
        </button>
        
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// sessionStorage에 저장된 TTL(tooManyUntil)로 남은 시간 계산
const remainMs = ref(0)
const ready = computed(() => remainMs.value <= 0)
const mm = computed(() => String(Math.max(0, Math.floor(remainMs.value / 1000 / 60))).padStart(2, '0'))
const ss = computed(() => String(Math.max(0, Math.floor(remainMs.value / 1000) % 60)).padStart(2, '0'))

let timer = null
const tick = () => {
  const until = Number(sessionStorage.getItem('tooManyUntil') || 0)
  remainMs.value = Math.max(0, until - Date.now())
  if (remainMs.value <= 0 && until) {
    // 만료되면 바로 정리
    sessionStorage.removeItem('tooManyUntil')
  }
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 250)
  // 단축키: R → 다시 시도
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('keydown', onKey)
})

function onKey(e) {
  if (e.key.toLowerCase() === 'r') retry()
}

function retry() {
  if (!ready.value) return
  const back = sessionStorage.getItem('tooManyBack') || '/'
  // 깔끔하게 정리
  sessionStorage.removeItem('tooManyBack')
  sessionStorage.removeItem('tooManyUntil')
  // 원래 페이지로
  router.replace(back)
}

function goHome() {
  router.replace({ path: '/' })
}
</script>

<style>
@keyframes pop {
  0%   { transform: translateY(8px) scale(.98); opacity: 0 }
  100% { transform: translateY(0)    scale(1);   opacity: 1 }
}
</style>

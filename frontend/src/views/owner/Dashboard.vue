<script setup lang="js">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import DashboardGraphs from '@/components/owner_graph/DashboardGraphs.vue'

const route = useRoute()
const router = useRouter()

const hotelId = ref(null)
const myHotels = ref([])
const currentHotel = computed(() => myHotels.value.find(h => h.id === hotelId.value))
  import { api } from '@/lib/api'
async function loadHotels() {

const res = await api('/api/owner/hotels')
  if (res.ok) {
    myHotels.value = await res.json()
  }
}

const logout = () => {
    localStorage.removeItem('token')
    document.cookie = 'refreshToken=; Max-Age=0; path=/;'
    router.push('/login')
  }

/* 오늘 잔여 객실 상태 */
const todayRemaining = ref(null)
const loadingRemain = ref(false)

/*  이번 주 예약 수 상태 */
const weeklyCount = ref(null)
const loadingWeekly = ref(false)

/* 오늘 체크인 수 상태 */
const todayCheckIn = ref(null)
const loadingTodayCheckIn = ref(false)
const nowCheckIn = ref(null)
const loadingNow = ref(false)


/*  오늘 잔여 객실 로드 */
async function loadTodayRemaining () {
  if (!hotelId.value) return
  loadingRemain.value = true
  try {
    const res = await api(`/api/owner/hotels/${hotelId.value}/inventory/today`)
    if (res.ok) {
      const data = await res.json()
      todayRemaining.value =
        typeof data === 'number'
          ? data
          : (data.totalRemainingQty ?? data.total ?? data.value ?? null)
    } else {
      todayRemaining.value = null
    }
  } catch {
    todayRemaining.value = null
  } finally {
    loadingRemain.value = false
  }
}

/*  이번 주 예약 수 로더 */
async function loadWeeklyCount () {
  if (!hotelId.value) return
  loadingWeekly.value = true
  try {
    // 백엔드: GET /api/owner/hotels/{hotelId}/bookings/weekly-count → { weeklyCount: number }
    const res = await api(`/api/owner/hotels/${hotelId.value}/bookings/weekly-count`)
    if (res.ok) {
      const data = await res.json()
      weeklyCount.value =
        typeof data === 'number'
          ? data
          : (data.weeklyCount ?? data.count ?? null)
    } else {
      weeklyCount.value = null
    }
  } catch {
    weeklyCount.value = null
  } finally {
    loadingWeekly.value = false
  }
}

/* 오늘 체크인 상태 로더*/ 
async function loadTodayCheckIn () {
  if (!hotelId.value) return
  loadingTodayCheckIn.value = true
  try {
    const res = await api(`/api/owner/hotels/${hotelId.value}/bookings/today-checkin-count`)
    if (res.ok) {
      const data = await res.json()
      todayCheckIn.value =
        typeof data === 'number'
          ? data
          : (data.todayCheckInCount ?? data.count ?? null)
    } else {
      todayCheckIn.value = null
    }
  } catch {
    todayCheckIn.value = null
  } finally {
    loadingTodayCheckIn.value = false
  }
}
async function loadNowCheckIn () {
  if (!hotelId.value) return
  loadingNow.value = true
  try {
    const res = await api(`/api/owner/hotels/${hotelId.value}/checkins/now-count`)
    if (res.ok) {
      const data = await res.json()
      nowCheckIn.value =
        typeof data === 'number'
          ? data
          : (data.nowCheckInCount ?? data.count ?? null)
    } else {
      nowCheckIn.value = null
    }
  } catch {
    nowCheckIn.value = null
  } finally {
    loadingNow.value = false
  }
}

onMounted(async () => {
  hotelId.value = Number(route.params.hotelId)
  await loadHotels()
  await loadTodayRemaining()
  await loadWeeklyCount()
  await loadTodayCheckIn()
  await loadNowCheckIn() 
})

watch(() => route.params.hotelId, async (v) => { hotelId.value = Number(v) 
  await loadTodayRemaining()
  await loadWeeklyCount()
  await loadTodayCheckIn()
  await loadNowCheckIn() 
})
</script>

<template>
  <div class="main">
        <header class="topbar">
          <div class="title">
            {{ $route.meta.title || '대시보드' }}
          </div>
          <div class="actions">
            <button class="btn ghost" @click="$router.push('/main')">사이트 보기</button>
            <button class="btn" @click="logout">로그아웃</button>
          </div>
        </header>
  
        <main class="content">
          <RouterView />
        </main>
    </div>
  <div>
    <h1 class="text-xl font-bold">오너 대시보드</h1>
    <p class="text-gray-500 mt-1" v-if="currentHotel">
      {{ currentHotel.name }} · {{ currentHotel.region }} · 사업자 {{ currentHotel.businessNo }}
    </p>

    <!-- 요약 카드 (샘플 자리표시자) -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6">
      <div class="rounded-2xl p-4 bg-white border">
        <div class="text-xs text-gray-500">오늘 잔여 객실</div>
        <div class="text-2xl font-semibold mt-1">
          <span v-if="loadingRemain">…</span>
          <span v-else>{{ todayRemaining ?? '—' }}</span>
        </div>
        <div class="text-xs text-gray-400 mt-1">룸타입 선택 시 인벤토리에서 확인</div>
      </div>
      <div class="rounded-2xl p-4 bg-white border">
        <div class="text-xs text-gray-500">이번 주 예약 수</div>
        <div class="text-2xl font-semibold mt-1">
          <span v-if="loadingWeekly">…</span>
          <span v-else>{{ weeklyCount ?? '—' }}</span>
        </div>
        <div class="text-xs text-gray-400 mt-1">예약 API 연동 시 표시</div>
      </div>
      <div class="rounded-2xl p-4 bg-white border">
        <div class="text-xs text-gray-500">체크인 예정(오늘)</div>
        <div class="text-2xl font-semibold mt-1">
          <span v-if="loadingTodayCheckIn">…</span>
          <span v-else>{{ todayCheckIn ?? '—' }}</span>
        </div>
        <div class="text-xs text-gray-400 mt-1">예약 API 연동 시 표시</div>
      </div>
      <div class="rounded-2xl p-4 bg-white border">
        <div class="text-xs text-gray-500">체크인 완료</div>
        <div class="text-2xl font-semibold mt-1">
          <span v-if="loadingNow">…</span>
          <span v-else>{{ nowCheckIn ?? '—' }}</span>
        </div>
        <div class="text-xs text-gray-400 mt-1">예약 API 연동 시 표시</div>
      </div>
    </div>

    <DashboardGraphs />

    <div class="mt-8 flex gap-2">
      <button class="px-4 py-2 rounded-xl bg-black text-white hover:opacity-90"
              @click="router.push(`/owner/hotels/${hotelId}/inventory`)">
        재고 캘린더로 이동
      </button>
      <button class="px-4 py-2 rounded-xl border hover:bg-gray-100"
              @click="router.push(`/owner/hotels/${hotelId}/bookings`)">
        예약 목록으로 이동
      </button>
    </div>
  </div>
</template>
<style scoped>
/* 전체 영역(기존 마크업 유지) */
.main {
  display: flex;
  flex-direction: column;
}

/* 상단 바: 좌측 타이틀, 우측 액션 정렬 */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  /* 페이지 여백 느낌 (스샷처럼 살짝 위 공간) */
  margin-top: 8px;
  margin-bottom: 12px;
}

/* "대시보드" 타이틀 – 두 번째 스샷처럼 굵고 진한 컬러 */
.title {
  font-weight: 700;           /* bold */
  font-size: 16px;            /* 살짝 작게 */
  line-height: 1.2;
  color: #111827;             /* slate-900 */
}

/* 우측 액션 컨테이너(사이 간격) */
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 공통 버튼 베이스 */
.btn {
  appearance: none;
  border: 0;
  outline: 0;
  padding: 8px 14px;          /* pill 느낌 */
  border-radius: 12px;        /* 둥근 모서리 */
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all .15s ease;
  background: #0f172a;        /* slate-900 */
  color: #fff;
}

/* hover 효과 */
.btn:hover { filter: brightness(0.95); }
.btn:active { transform: translateY(1px); }

/* "사이트 보기"는 링크처럼 보이는 고스트 스타일(스샷처럼) */
.btn.ghost {
  background: transparent;
  color: #1f2937;             /* gray-800 */
  padding: 0;                 /* 네모 박스 없이 텍스트만 */
  border-radius: 0;
  font-weight: 600;
  text-decoration: none;      /* 기본은 밑줄 없음 */
}

/* hover 시 살짝 강조 + 밑줄 */
.btn.ghost:hover {
  color: #111827;
  text-decoration: underline; /* 스샷 느낌의 링크 스타일 */
}

/* 반응형: 좁은 화면에서 버튼 간격 조정 */
@media (max-width: 640px) {
  .actions { gap: 8px; }
  .btn { padding: 7px 12px; border-radius: 10px; }
}
</style>

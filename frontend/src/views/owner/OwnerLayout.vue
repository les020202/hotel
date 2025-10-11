<!-- src/views/owner/OwnerLayout.vue -->
<script setup lang="js">
import { onMounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { api } from '@/lib/api'

const route = useRoute()
const router = useRouter()

const hotels = ref([])               // [{id,name,region,gradeLevel,businessNo}]
const currentHotelId = ref(null)     // number

async function fetchMyHotels() {
  try {
    const res = await api('/api/owner/hotels', { method: 'GET', credentials: 'include' })
    if (!res.ok) throw new Error('Failed to load my hotels')
    hotels.value = await res.json()

    // 초기 선택
    if (!currentHotelId.value && hotels.value.length) {
      currentHotelId.value = hotels.value[0].id
      const seg = route.path.split('/')
      const isChild = seg.includes('hotels') && seg.length >= 4
      router.replace(isChild ? `/owner/hotels/${currentHotelId.value}${childSuffix()}`
                             : `/owner/hotels/${currentHotelId.value}`)
    }
  } catch (e) {
    console.error(e)
  }
}

function childSuffix() {
  // 현재 자식 경로 유지 (dashboard | inventory | bookings)
  if (route.path.endsWith('/inventory')) return '/inventory'
  if (route.path.endsWith('/bookings')) return '/bookings'
  if (route.path.endsWith('/assign')) return '/assign'
  if (route.path.endsWith('/rooms')) return '/rooms'
  if (route.path.endsWith('/reviews')) return '/reviews'
  return ''
}

function onHotelChange() {
  if (!currentHotelId.value) return
  router.push(`/owner/hotels/${currentHotelId.value}${childSuffix()}`)
}

function deleteCookie(name) {
  // HttpOnly 쿠키는 JS로 삭제되지 않을 수 있으나 가능한 범위에서 만료 시도
  const past = 'Thu, 01 Jan 1970 00:00:00 GMT'
  document.cookie = `${name}=; expires=${past}; path=/`
  document.cookie = `${name}=; expires=${past}; path=/; domain=${location.hostname}`
}

/** 로그아웃 (서버 + 클라이언트 완전 정리) */
async function logout () {
  try {
    // 서버 세션/쿠키 쓰는 경우를 대비해 credentials 포함
    await api('/api/auth/logout', { method: 'POST', credentials: 'include' }).catch(() => {})
  } finally {
    // 1) 로컬/세션 스토리지 토큰 정리
    const keys = ['accessToken','refreshToken','ACCESS_TOKEN','REFRESH_TOKEN','jwt','token']
    keys.forEach(k => { localStorage.removeItem(k); sessionStorage.removeItem(k) })

    // 2) axios 기본 Authorization 헤더 제거(있는 경우)
    try {
      if (api?.defaults?.headers?.common?.Authorization) {
        delete api.defaults.headers.common.Authorization
      }
      if (api?.defaults?.headers?.Authorization) {
        delete api.defaults.headers.Authorization
      }
      if (api?.setAuth) api.setAuth(null)
      if (api?.setToken) api.setToken(null)
    } catch (_) {}

    // 3) (옵션) 알림 브로드캐스트 — 구독 안 해도 문제 없음
    window.dispatchEvent(new CustomEvent('auth:changed', { detail: { isLoggedIn:false } }))

    // 4) 로그인 페이지로 이동 후, 앱 전체 리셋(헤더 포함) 위해 강제 새로고침
    await router.replace('/login')
    // 헤더가 상태를 캐시하고 있어도 새로고침으로 초기화됨
    window.location.reload()
  }
}

// 라우트 파라미터에서 초기값 세팅
onMounted(async () => {
  const hid = Number(route.params.hotelId)
  if (!isNaN(hid)) currentHotelId.value = hid
  await fetchMyHotels()
})

// 라우트 변경으로 :hotelId 바뀌면 드롭다운 동기화
watch(() => route.params.hotelId, (v) => {
  const hid = Number(v)
  if (!isNaN(hid)) currentHotelId.value = hid
})
</script>

<template>
  <div class="flex min-h-screen text-sm">
    <!-- Sidebar -->
    <aside class="owner-sidebar">
      <div class="sidebar-inner">
        <!-- 상단: 내 호텔 + 사이트 보기(빨간 영역) -->
        <div class="hotel-top">
          <div class="hotel-select">
            <div class="hotel-select__label">내 호텔</div>
            <select class="hotel-select__control"
                    v-model.number="currentHotelId"
                    @change="onHotelChange"
                    :disabled="!hotels.length">
              <option v-for="h in hotels" :key="h.id" :value="h.id">
                {{ h.name }} ({{ h.businessNo }})
              </option>
            </select>
          </div>

          <RouterLink to="/main" class="view-site-btn">
            사이트 보기
          </RouterLink>
        </div>

        <!-- 네비게이션 -->
        <nav class="nav">
          <RouterLink
            :to="currentHotelId ? `/owner/hotels/${currentHotelId}` : '/owner'"
            class="nav-item"
            :class="{'is-disabled': !currentHotelId}"
          >
            대시보드
          </RouterLink>

          <RouterLink
            :to="currentHotelId ? `/owner/hotels/${currentHotelId}/inventory` : '/owner'"
            class="nav-item"
            :class="{'is-disabled': !currentHotelId}"
          >
            예약 캘린더
          </RouterLink>

          <RouterLink
            :to="currentHotelId ? `/owner/hotels/${currentHotelId}/bookings` : '/owner'"
            class="nav-item"
            :class="{'is-disabled': !currentHotelId}"
          >
            예약
          </RouterLink>

          <RouterLink
            :to="`/owner/hotels/${currentHotelId}/assign`"
            class="nav-item"
            :class="{'is-active-soft': $route.path.includes('/assign')}"
          >
            호실 배정
          </RouterLink>

          <RouterLink
            :to="`/owner/hotels/${currentHotelId}/rooms`"
            class="nav-item"
            :class="{'is-active-soft': $route.path.includes('/rooms')}"
          >
            객실 현황
          </RouterLink>

          <RouterLink
            :to="currentHotelId ? `/owner/hotels/${currentHotelId}/reviews` : '/owner'"
            class="nav-item"
            :class="{
              'is-disabled': !currentHotelId,
              'is-active-soft': $route.path.includes('/reviews')
            }"
          >
            리뷰 조회
          </RouterLink>

          <!-- 리뷰 조회 바로 아래: 로그아웃 (검정색 버튼) -->
          <button class="logout-btn nav-item" type="button" @click="logout">
            로그아웃
          </button>
        </nav>
      </div>
    </aside>

    <!-- Main -->
    <main class="flex-1 p-6 bg-gray-50">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
/* ===== Sidebar Shell ===== */
.owner-sidebar{
  width: 260px;
  background: #0f2745;                 /* 네이비 */
  color: #d7e2ee;                       /* 밝은 회색 텍스트 */
  border-right: 1px solid rgba(255,255,255,.06);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.sidebar-inner{
  padding: 18px 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

/* 상단 레이아웃 (셀렉트 + 사이트보기 버튼) */
.hotel-top{
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: end;
}

/* ===== Hotel select ===== */
.hotel-select__label{
  font-size: 12px;
  color: #9fb3c8;
  letter-spacing: .2px;
}
.hotel-select__control{
  margin-top: 6px;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border-radius: 10px;
  background: rgba(255,255,255,.06);
  color: #e9f0f7;
  border: 1px solid rgba(255,255,255,.12);
  outline: none;
}
.hotel-select__control:disabled{
  opacity: .6;
  cursor: not-allowed;
}

/* 빨간 영역 버튼: 사이트 보기 */
.view-site-btn{
  height: 36px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,.25);
  background: rgba(255,255,255,.08);
  color: #fff;
  text-decoration: none;
  font-weight: 600;
  white-space: nowrap;
}
.view-site-btn:hover{
  background: rgba(255,255,255,.15);
}

/* ===== Navigation ===== */
.nav{
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}
.nav-item{
  position: relative;
  display: block;
  padding: 10px 12px 10px 16px;
  border-radius: 10px;
  color: #d7e2ee;
  text-decoration: none;
  transition: background .15s ease, color .15s ease, transform .06s ease;
}
.nav-item:hover{
  background: rgba(255,255,255,.08);
}
.nav-item.is-disabled{
  opacity: .45;
  pointer-events: none;
}
/* 좌측 포커스 바(활성/호버에서 보이도록) */
.nav-item::before{
  content:'';
  position:absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 2px;
  background: transparent;
  transition: background .15s ease;
}
.nav-item:hover::before{
  background: rgba(0, 212, 255, .55);   /* 시안 포커스 */
}
/* 라우터 활성화 스타일 */
:deep(.router-link-exact-active).nav-item,
.nav-item.is-active-soft{
  background: rgba(255,255,255,.14);
  color: #ffffff;
  font-weight: 700;
}
:deep(.router-link-exact-active).nav-item::before,
.nav-item.is-active-soft::before{
  background: #00d4ff;
}

/* 스크롤바 (사이드바 내부가 길어질 때) */
.owner-sidebar::-webkit-scrollbar{
  width: 10px;
}
.owner-sidebar::-webkit-scrollbar-thumb{
  background: rgba(255,255,255,.12);
  border-radius: 10px;
}
.owner-sidebar::-webkit-scrollbar-track{
  background: transparent;
}

/* 드롭다운 펼쳤을 때 옵션 목록(흰 배경 + 검정 글자) */
.hotel-select__control:focus { background: #ffffff; color: #111827; }
.hotel-select__control option { background: #ffffff; color: #111827; }
.hotel-select__control option:checked { background: #e5e7eb; color: #111827; }

/* 로그아웃 버튼: 흰 배경/검정 텍스트 */
.logout-btn{
  background: #ffffff;        /* 흰색 배경 */
  color: #111827;             /* 검정 텍스트 */
  border: 1px solid rgba(17,24,39,.2);
  border-radius: 10px;
  padding: 10px 12px 10px 16px;
  font-weight: 700;
  cursor: pointer;
  text-align: center;
}
.logout-btn:hover{
  background: #f3f4f6;        /* 연한 회색 */
}
</style>

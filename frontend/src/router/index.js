
// frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import SignupView from '@/views/SignupView.vue'
import LoginView from '@/views/LoginView.vue'
import MainView from '@/views/MainView.vue'
import AssignView from '@/views/owner/AssignView.vue'

// 예약/결제
import ReservationPage from '@/views/reservation/ReservationPage.vue'
import PaymentSuccess from '@/views/reservation/PaymentSuccess.vue'
import PaymentFail from '@/views/reservation/PaymentFail.vue'

// 관리자
import AdminLayout from '@/views/admin/AdminLayout.vue'
import AdminDashboard from '@/views/admin/Dashboard.vue'

import FindPasswordView from '@/views/FindPasswordView.vue' // 새 비밀번호 찾기
// 목록 / 상세
import SearchView from '@/views/SearchView.vue'
import HotelDetailView from '@/views/HotelDetailView.vue'

// 마이페이지 관련
import MyPage from '@/views/mypage/MyPage.vue'
import Account from '@/views/mypage/Account.vue'

import Coupons from '@/views/mypage/Coupons.vue'
import History from '@/views/mypage/History.vue'
import Support from '@/views/mypage/Support.vue'

// 고객지원 하위 라우트
import NoticeList from '@/views/support/NoticeList.vue'
import NoticeDetail from '@/views/support/NoticeDetail.vue'
import FaqList from '@/views/support/FaqList.vue'
import FaqDetail from '@/views/support/FaqDetail.vue'
import ContactCenter from '@/views/support/ContactCenter.vue'
import SupportInquiry from '@/views/support/SupportInquiry.vue'
import MyTickets from '@/views/support/MyTickets.vue'
import MyTicketDetail from '@/views/support/MyTicketDetail.vue'

//찜 관련
import Wishlist from '@/views/Wishlist.vue'

// (선택) 403 페이지
const Forbidden = { template: '<div style="padding:2rem">권한이 없습니다 (403)</div>' }

// === ⬇️ 오너 뷰 추가 (JS 버전) ===
import OwnerLayout from '@/views/owner/OwnerLayout.vue'
import OwnerDashboard from '@/views/owner/Dashboard.vue'
import OwnerInventory from '@/views/owner/InventoryView.vue'
import OwnerBookings from '@/views/owner/BookingsView.vue'


// JWT payload 파서 (역할 확인용)
function parseJwt(token) {
  try {
    if (!token) return null
    // "Bearer ..." 접두사 제거
    if (token.startsWith('Bearer ')) token = token.slice(7)
    const base64Url = token.split('.')[1]
    if (!base64Url) return null
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}
function hasOwnerRole(claims) {
  if (!claims) return false
  const bucket = []

  // ✅ 단일 role 키 지원 (지금 네 토큰 구조)
  if (typeof claims.role === 'string') bucket.push(claims.role)

  // 그 외 흔한 위치들
  if (Array.isArray(claims.roles)) bucket.push(...claims.roles)
  if (Array.isArray(claims.authorities)) bucket.push(...claims.authorities)
  if (Array.isArray(claims.scopes)) bucket.push(...claims.scopes)
  if (typeof claims.scope === 'string') bucket.push(...claims.scope.split(/[ ,]/))
  if (typeof claims.authority === 'string') bucket.push(...claims.authority.split(/[ ,]/))
  if (claims.realm_access && Array.isArray(claims.realm_access.roles)) bucket.push(...claims.realm_access.roles)
  if (Array.isArray(claims['cognito:groups'])) bucket.push(...claims['cognito:groups'])

  // 대소문자 무시 비교
  const norm = bucket.filter(Boolean).map(x => String(x).toUpperCase().trim())
  return norm.includes('ROLE_OWNER') || norm.includes('OWNER')
}

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  },
  routes: [
    { path: '/signup', component: SignupView },
    { path: '/login',  component: LoginView  },
    { path: '/find-password', component: FindPasswordView },
    { path: '/main', component: MainView, meta: { requiresAuth: true } },
    { path: '/', redirect: '/main' },
    { path: '/search', name: 'search', component: SearchView },

    {
      path: '/hotels/:id',
      name: 'hotel-detail',
      component: HotelDetailView,
      props: route => ({
        id: Number(route.params.id),
        checkIn: route.query.checkIn,
        checkOut: route.query.checkOut
      })
    },

    // 예약 플로우
    { path: '/reservation', component: ReservationPage },

    // ✅ 토스 결제 결과 콜백 라우트 (비로그인 접근 허용)
    { path: '/pay/success', name: 'PaySuccess', component: PaymentSuccess },
    { path: '/pay/fail',    name: 'PayFail',    component: PaymentFail },

    // ✅ 관리자 라우트 (ROLE_ADMIN 전용)
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
      children: [
        { path: '', name: 'AdminDashboard', component: AdminDashboard, meta: { title: '대시보드' } },
        { path: 'bookings',    name: 'AdminBookings',    component: () => import('@/views/admin/Bookings.vue'),           meta: { title: '예약 관리' } },
        { path: 'rooms',       name: 'AdminRooms',       component: () => import('@/views/admin/Rooms.vue'),              meta: { title: '객실 현황' } },
        { path: 'settlements', name: 'AdminSettlements', component: () => import('@/views/admin/Settlements.vue'),        meta: { title: '정산' } },
        { path: 'reviews',     name: 'AdminReviews',     component: () => import('@/views/admin/ReviewModeration.vue'),   meta: { title: '리뷰/신고' } },
        { path: 'coupons',     name: 'AdminCoupons',     component: () => import('@/views/admin/Coupons.vue'),            meta: { title: '쿠폰/프로모션' } },
        { path: 'users',       name: 'AdminUsers',       component: () => import('@/views/admin/Users.vue'),              meta: { title: '유저 관리' } },

        // ✅ 추가: 호텔 관리 / 호텔 심사
        { path: 'hotels',       name: 'AdminHotels',      component: () => import('@/views/admin/AdminHotelManage.vue'), meta: { title: '호텔 관리' } },
        { path: 'hotel-audit',  name: 'AdminHotelAudit',  component: () => import('@/views/admin/AdminHotelAudit.vue'),  meta: { title: '호텔 심사' } },
      ]
    },

    { path: '/403', component: Forbidden },
    { path: '/:pathMatch(.*)*', redirect: '/main' },

    // ✅ 마이페이지 라우트
    {
      path: '/mypage',
      component: MyPage,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/mypage/account' },
        { path: 'account', component: Account },
        { path: 'coupon', component: Coupons },
        { path: 'history', component: History },
        { path: 'support', component: Support }
      ]
    },

    // ✅ 고객지원 페이지 (로그인 없이도 접근 가능)
    { path: '/support/notice', component: NoticeList },      
    { path: '/support/notice/:id', component: NoticeDetail },
    { path: '/support/faq', component: FaqList },
    { path: '/support/faq/:id', component: FaqDetail },
    { path: '/support/contact', component: ContactCenter },
    { path: '/support/contact/inquiry', component: SupportInquiry },
    { path: '/support/contact/my', component: MyTickets },                       // 내 문의함 목록
    { path: '/support/contact/ticket/:id', component: MyTicketDetail, props: true }, // 상세/스레드
    //찜페이지
    { path: '/wishlist', name: 'Wishlist', component: Wishlist },

    // === ⬇️ 오너 전용 라우트 추가 (ROLE_OWNER 필요) ===
    {
      path: '/owner',
      component: OwnerLayout,
      meta: { requiresAuth: true, requiresOwner: true },
      children: [
        { path: 'hotels/:hotelId', component: OwnerDashboard },
        { path: 'hotels/:hotelId/inventory', component: OwnerInventory },
        { path: 'hotels/:hotelId/bookings', component: OwnerBookings },
        { path: 'hotels/:hotelId/assign', component: AssignView },
        // router/index.js 의 owner children에 추가
{ path: 'hotels/:hotelId/rooms', component: () => import('@/views/owner/HouseStatus.vue') }
      ]
    }
  ]
})

// ✅ 전역 가드: 소셜 로그인 리다이렉트 + 인증/역할 체크 + 날짜 정규화
router.beforeEach((to, from, next) => {
  // 1) hash(#token=...) 또는 query(?token=...)에서 토큰 추출
  const hash = to.hash || window.location.hash
  const m = hash && hash.match(/token=([^&]+)/)
  const tokenFromHash = m ? decodeURIComponent(m[1]) : null
  const tokenFromQuery = to.query?.token
  if (tokenFromHash || tokenFromQuery) {
    const token = tokenFromHash || tokenFromQuery
    localStorage.setItem('token', token)
    window.history.replaceState({}, '', to.path) // URL에서 제거
  }

  // 2) 인증/역할 체크
  const token = localStorage.getItem('token')
  const isPayCallback = to.path.startsWith('/pay/')

  // 2) 인증 체크
  if (!isPayCallback && to.meta.requiresAuth && !token) {
    return next('/login')
  }

  // 3) 로그인 상태에서 /login, /signup 접근 막기
  if (!isPayCallback && (to.path === '/login' || to.path === '/signup') && token) {
    return next('/main')
  }

  // 4) 역할 가드
  const needRoles = to.meta?.roles || []
  if (needRoles.length) {
    const user = token ? parseJwt(token) : null
    const role = user?.role || user?.authorities || user?.roles?.[0]
    if (!role || !needRoles.includes(role)) {
      return next('/403')
    }
  }

  // 오너 전용
  if (to.matched.some(r => r.meta && r.meta.requiresOwner)) {
    const claims = parseJwt(token)
    if (!hasOwnerRole(claims)) {
      // 오너 권한 없으면 메인으로
      return next('/main')
    }
  }

  // 3) 날짜 정규화: 검색/상세 공통
  const needDates = to.name === 'search' || to.name === 'hotel-detail'
  if (needDates) {
    const q = { ...(to.query || {}) }
    const fmt = (d) => new Date(d).toISOString().slice(0, 10)
    const isValid = (s) => !!s && !Number.isNaN(new Date(String(s)).getTime())
    const addDays = (base, n) => {
      const d = new Date(base); d.setDate(d.getDate() + n); return fmt(d)
    }

    let changed = false
    if (!isValid(q.checkIn)) { q.checkIn = fmt(new Date()); changed = true }
    if (!isValid(q.checkOut)) { q.checkOut = addDays(q.checkIn, 1); changed = true }

    const inD = new Date(String(q.checkIn))
    const outD = new Date(String(q.checkOut))
    if (outD <= inD) { q.checkOut = addDays(q.checkIn, 1); changed = true }

    if (changed) {
      return next({ name: to.name, params: to.params, query: q, replace: true })
    }
  }

  // ✅ 항상 마지막에 next()
  next()
})

export async function api(path, opts = {}) {
  const token = localStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json', ...(opts.headers || {}) }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(path, { ...opts, headers })
  if (res.status === 401) {
    // 만료/인증실패 → 로그인으로
    localStorage.removeItem('token')
    window.location.href = '/login'
  }
  return res
}

export default router

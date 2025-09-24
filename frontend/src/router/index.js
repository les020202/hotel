// frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

// 기본/인증
import SignupView from '@/views/SignupView.vue'
import LoginView from '@/views/LoginView.vue'
import MainView from '@/views/MainView.vue'

// 예약/결제
import ReservationPage from '@/views/reservation/ReservationPage.vue'
import PaymentSuccess from '@/views/reservation/PaymentSuccess.vue'
import PaymentFail from '@/views/reservation/PaymentFail.vue'

// 관리자
import AdminLayout from '@/views/admin/AdminLayout.vue'
import AdminDashboard from '@/views/admin/Dashboard.vue'

// 찾기/검색/상세
import FindPasswordView from '@/views/FindPasswordView.vue'
import SearchView from '@/views/SearchView.vue'
import HotelDetailView from '@/views/HotelDetailView.vue'

// 마이페이지
import MyPage from '@/views/mypage/MyPage.vue'
import Account from '@/views/mypage/Account.vue'
import Coupons from '@/views/mypage/Coupons.vue'
import History from '@/views/mypage/History.vue'
import Support from '@/views/mypage/Support.vue'
import Payment from '@/views/mypage/Payment.vue'
import AddCard from '@/views/mypage/AddCard.vue'

// 고객지원 퍼블릭(사용자용) 하위 라우트
import NoticeList from '@/views/support/NoticeList.vue'
import NoticeDetail from '@/views/support/NoticeDetail.vue'
import FaqList from '@/views/support/FaqList.vue'
import FaqDetail from '@/views/support/FaqDetail.vue'
import ContactCenter from '@/views/support/ContactCenter.vue'
import SupportInquiry from '@/views/support/SupportInquiry.vue'
import MyTickets from '@/views/support/MyTickets.vue'
import MyTicketDetail from '@/views/support/MyTicketDetail.vue'

// 찜
import Wishlist from '@/views/Wishlist.vue'

// (선택) 403 페이지
const Forbidden = { template: '<div style="padding:2rem">권한이 없습니다 (403)</div>' }

// === 오너 뷰 ===
import OwnerLayout from '@/views/owner/OwnerLayout.vue'
import OwnerDashboard from '@/views/owner/Dashboard.vue'
import OwnerInventory from '@/views/owner/InventoryView.vue'
import OwnerBookings from '@/views/owner/BookingsView.vue'
import AssignView from '@/views/owner/AssignView.vue'

// === 관리자 고객지원(공지/FAQ/문의) — lazy load 컴포넌트 ===
const AdminSupportLayout  = () => import('@/views/admin/support/SupportLayout.vue')
const AdminSupportNotices = () => import('@/views/admin/support/Notices.vue')
const AdminSupportFaqs    = () => import('@/views/admin/support/Faqs.vue')
const AdminSupportTickets = () => import('@/views/admin/support/Tickets.vue')

/* ============================
 * JWT 도우미
 * ============================ */
function parseJwt(token) {
  try {
    if (!token) return null
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
  if (typeof claims.role === 'string') bucket.push(claims.role)
  if (Array.isArray(claims.roles)) bucket.push(...claims.roles)
  if (Array.isArray(claims.authorities)) bucket.push(...claims.authorities)
  if (Array.isArray(claims.scopes)) bucket.push(...claims.scopes)
  if (typeof claims.scope === 'string') bucket.push(...claims.scope.split(/[ ,]/))
  if (typeof claims.authority === 'string') bucket.push(...claims.authority.split(/[ ,]/))
  if (claims.realm_access?.roles) bucket.push(...claims.realm_access.roles)
  if (Array.isArray(claims['cognito:groups'])) bucket.push(...claims['cognito:groups'])
  const norm = bucket.filter(Boolean).map(x => String(x).toUpperCase().trim())
  return norm.includes('ROLE_OWNER') || norm.includes('OWNER')
}

/* ============================
 * 라우터
 * ============================ */
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, saved) {
    return saved || { top: 0 }
  },
  routes: [
    // 공개
    { path: '/main', name: 'main', component: MainView, meta: { public: true } },
    { path: '/', redirect: { name: 'main' } },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/signup', component: SignupView, meta: { public: true } },
    { path: '/find-password', component: FindPasswordView, meta: { public: true } },
    { path: '/search', name: 'search', component: SearchView, meta: { public: true } },
    {
      path: '/hotels/:id',
      name: 'hotel-detail',
      component: HotelDetailView,
      meta: { public: true },
      props: route => ({
        id: Number(route.params.id),
        checkIn: route.query.checkIn,
        checkOut: route.query.checkOut
      })
    },

    // (router/index.js 안의 routes 배열에서 예약/결제 부분만 교체)

// 예약/결제 (콜백은 비로그인 허용)
{
  path: '/reservation',
  // ⚠️ 이 파일만 지연 로딩으로 바꿔 순환 의존 깨기
  component: () => import('@/views/reservation/ReservationPage.vue'),
},
{
  path: '/reservation/success',
  name: 'PaySuccess',
  component: () => import('@/views/reservation/PaymentSuccess.vue'),
  meta: { public: true },
},
{
  path: '/reservation/fail',
  name: 'PayFail',
  component: () => import('@/views/reservation/PaymentFail.vue'),
  meta: { public: true },
},

// Toss가 호출하는 짧은 콜백 URL도 받기 (동일 컴포넌트로 연결)
{
  path: '/pay/success',
  component: () => import('@/views/reservation/PaymentSuccess.vue'),
  meta: { public: true },
},
{
  path: '/pay/fail',
  component: () => import('@/views/reservation/PaymentFail.vue'),
  meta: { public: true },
},

    // 관리자 (ROLE_ADMIN)
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

        // 호텔 관리 / 호텔 심사
        { path: 'hotels',       name: 'AdminHotels',      component: () => import('@/views/admin/AdminHotelManage.vue'), meta: { title: '호텔 관리' } },
        { path: 'hotel-audit',  name: 'AdminHotelAudit',  component: () => import('@/views/admin/AdminHotelAudit.vue'),  meta: { title: '호텔 심사' } },

        // ✅ 관리자 고객지원 루트
        { path: 'support',        component: () => import('@/views/admin/support/SupportLayout.vue') ,
          children: [
            { path: '', redirect: '/admin/support/notices' },
            { path: 'notices',  component: () => import('@/views/admin/support/Notices.vue') },
            { path: 'faqs',     component: () => import('@/views/admin/support/Faqs.vue') },
            { path: 'tickets',  component: () => import('@/views/admin/support/Tickets.vue') },
            { path: 'tickets/:id', component: () => import('@/views/admin/support/TicketDetail.vue'), props: true },
          ]
        },
      ]
    },

    { path: '/403', component: Forbidden },
    { path: '/:pathMatch(.*)*', redirect: '/main' },

    // 마이페이지
    {
      path: '/mypage',
      component: MyPage,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/mypage/account' },
        { path: 'account', component: Account },
        { path: 'coupon', component: Coupons },
        { path: 'history', component: History },
        { path: 'support', component: Support },
        // 두 파일에 있던 Payment / AddCard 라우트도 추가
        { path: 'payment', component: Payment },
        { path: 'add-card', component: AddCard },
        { path: 'bookings', name: 'MyBookings', component: () => import('@/views/mypage/MyBookings.vue') },
    { path: 'bookings/:id', name: 'MyBookingDetail', component: () => import('@/views/mypage/MyBookingDetail.vue'), props: true },
        
      ]
    },

    // 고객지원(퍼블릭)
    { path: '/support/notice', component: NoticeList },
    { path: '/support/notice/:id', component: NoticeDetail },
    { path: '/support/faq', component: FaqList },
    { path: '/support/faq/:id', component: FaqDetail },
    { path: '/support/contact', component: ContactCenter },
    { path: '/support/contact/inquiry', component: SupportInquiry },
    { path: '/support/contact/my', component: MyTickets },
    { path: '/support/contact/ticket/:id', component: MyTicketDetail, props: true },

    // 찜
    { path: '/wishlist', name: 'Wishlist', component: Wishlist },

    // 오너 (ROLE_OWNER)
    {
      path: '/owner',
      component: OwnerLayout,
      meta: { requiresAuth: true, requiresOwner: true },
      children: [
        { path: 'hotels/:hotelId', component: OwnerDashboard },
        { path: 'hotels/:hotelId/inventory', component: OwnerInventory },
        { path: 'hotels/:hotelId/bookings', component: OwnerBookings },
        { path: 'hotels/:hotelId/assign', component: AssignView },
        { path: 'hotels/:hotelId/rooms', component: () => import('@/views/owner/HouseStatus.vue') },
      ]
    },

    { path: '/403', component: Forbidden },
    { path: '/:pathMatch(.*)*', redirect: '/main' }
  ]
})

// 전역 가드: 소셜 로그인 리다이렉트 + 인증/역할 체크 + 날짜 정규화
router.beforeEach((to, from, next) => {
  const hash = to.hash || window.location.hash
  const m = hash && hash.match(/token=([^&]+)/)
  const tokenFromHash = m ? decodeURIComponent(m[1]) : null
  const tokenFromQuery = to.query?.token
  if (tokenFromHash || tokenFromQuery) {
    const token = tokenFromHash || tokenFromQuery
    localStorage.setItem('token', token)
    window.history.replaceState({}, '', to.path) // URL 정리
  }

  const token = localStorage.getItem('token')
  const isPublic = to.meta?.public === true
 const isPayCallback = to.path.startsWith('/pay/') || to.path.startsWith('/reservation/')

  // 2) 인증 필요 라우팅
  if (!isPublic && !isPayCallback && to.meta.requiresAuth && !token) {
    return next('/login')
  }

  if (!isPayCallback && (to.path === '/login' || to.path === '/signup') && token) {
    return next('/main')
  }

  const needRoles = to.meta?.roles || []
  if (needRoles.length) {
    const user = token ? parseJwt(token) : null
    const role = user?.role || user?.authorities || user?.roles?.[0]
    if (!role || !needRoles.includes(role)) {
      return next('/403')
    }
  }

  if (to.matched.some(r => r.meta && r.meta.requiresOwner)) {
    const claims = parseJwt(token)
    if (!hasOwnerRole(claims)) {
      return next('/main')
    }
  }

  const needDates = to.name === 'search' || to.name === 'hotel-detail'
  if (needDates) {
    const q = { ...(to.query || {}) }
    const fmt = (d) => new Date(d).toISOString().slice(0, 10)
    const isValid = (s) => !!s && !Number.isNaN(new Date(String(s)).getTime())
    const addDays = (base, n) => { const d = new Date(base); d.setDate(d.getDate() + n); return fmt(d) }

    let changed = false
    if (!isValid(q.checkIn))  { q.checkIn  = fmt(new Date()); changed = true }
    if (!isValid(q.checkOut)) { q.checkOut = addDays(q.checkIn, 1); changed = true }

    const inD = new Date(String(q.checkIn))
    const outD = new Date(String(q.checkOut))
    if (outD <= inD) { q.checkOut = addDays(q.checkIn, 1); changed = true }

    if (changed) {
      return next({ name: to.name, params: to.params, query: q, replace: true })
    }
  }

  next()
})

export async function api(path, opts = {}) {
  const token = localStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json', ...(opts.headers || {}) }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(path, { ...opts, headers })
  if (res.status === 401) {
    localStorage.removeItem('token')
    window.location.href = '/login'
  }
  return res
}

export default router

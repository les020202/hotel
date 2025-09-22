// frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
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

// JWT payload 파서
function parseJwt(token) {
  try { return JSON.parse(atob(token.split('.')[1])) } catch { return null }
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
    { path: '/support/faq', component: FaqList},             
    { path: '/support/faq/:id', component: FaqDetail},       
    { path: '/support/contact', component: ContactCenter },  
    { path: '/support/contact/inquiry', component: SupportInquiry },
    { path: '/support/contact/my', component: MyTickets },
    { path: '/support/contact/ticket/:id', component: MyTicketDetail, props: true },

    // 찜 페이지
    { path: '/wishlist', name: 'Wishlist', component: Wishlist }
  ]
})

// ✅ 전역 가드: 소셜 로그인 리다이렉트 + 인증 체크
router.beforeEach((to, from, next) => {
  // 1) hash(#token=...) 또는 query(?token=...)에서 토큰 추출
  const hash = to.hash || window.location.hash
  const m = hash && hash.match(/token=([^&]+)/)
  const tokenFromHash = m ? decodeURIComponent(m[1]) : null
  const tokenFromQuery = to.query?.token
  if (tokenFromHash || tokenFromQuery) {
    const token = tokenFromHash || tokenFromQuery
    localStorage.setItem('token', token)
    window.history.replaceState({}, '', to.path)
  }

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

  // 5) 검색/상세 공통 날짜 정규화
  const needDates = to.name === 'search' || to.name === 'hotel-detail'
  if (needDates) {
    const q = { ...(to.query || {}) }
    const fmt = (d) => new Date(d).toISOString().slice(0, 10)
    const isValid = (s) => !!s && !Number.isNaN(new Date(String(s)).getTime())
    const addDays = (base, n) => {
      const d = new Date(base)
      d.setDate(d.getDate() + n)
      return fmt(d)
    }

    let changed = false
    if (!isValid(q.checkIn)) {
      q.checkIn = fmt(new Date())
      changed = true
    }
    if (!isValid(q.checkOut)) {
      q.checkOut = addDays(q.checkIn, 1)
      changed = true
    }

    const inD = new Date(String(q.checkIn))
    const outD = new Date(String(q.checkOut))
    if (outD <= inD) {
      q.checkOut = addDays(q.checkIn, 1)
      changed = true
    }

    if (changed) {
      return next({
        name: to.name,
        params: to.params,
        query: q,
        replace: true
      })
    }
  }

  // ✅ 항상 마지막에 next()
  next()
})

export default router

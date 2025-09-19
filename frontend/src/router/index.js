// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

// 일반 페이지
import SignupView from '@/views/SignupView.vue'
import LoginView from '@/views/LoginView.vue'
import MainView from '@/views/MainView.vue'
import FindPasswordView from '@/views/FindPasswordView.vue'

// 예약/결제
import ReservationPage from '@/views/reservation/ReservationPage.vue'
import PaymentSuccess from '@/views/reservation/PaymentSuccess.vue'
import PaymentFail from '@/views/reservation/PaymentFail.vue'

// 관리자
import AdminLayout from '@/views/admin/AdminLayout.vue'
import AdminDashboard from '@/views/admin/Dashboard.vue'

// (선택) 403 페이지
const Forbidden = { template: '<div style="padding:2rem">권한이 없습니다 (403)</div>' }

// JWT payload 파서
function parseJwt(token) {
  try { return JSON.parse(atob(token.split('.')[1])) } catch { return null }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/signup', component: SignupView },
    { path: '/login', component: LoginView },
    { path: '/find-password', component: FindPasswordView },
    { path: '/main', component: MainView, meta: { requiresAuth: true } },

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

    { path: '/', redirect: '/main' },
    { path: '/:pathMatch(.*)*', redirect: '/main' }
  ],
  scrollBehavior() { return { top: 0 } }
})

// 로그인/권한 가드
router.beforeEach((to, from, next) => {
  // 소셜 로그인 등 해시/쿼리로 온 토큰 흡수
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

  // 1) 로그인 필요 라우트 처리 (결제 콜백은 예외)
  if (!isPayCallback && to.meta.requiresAuth && !token) return next('/login')

  // 2) 로그인 상태에서 /login, /signup 접근 막기
  if (!isPayCallback && (to.path === '/login' || to.path === '/signup') && token) return next('/main')

  // 3) 역할 가드 (ADMIN 전용 등)
  const needRoles = to.meta?.roles || []
  if (needRoles.length) {
    const user = token ? parseJwt(token) : null
    const role = user?.role || user?.authorities || user?.roles?.[0]
    if (!role || !needRoles.includes(role)) return next('/403')
  }

  next()
})

export default router

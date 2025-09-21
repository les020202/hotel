import { createRouter, createWebHistory } from 'vue-router'
import SignupView from '@/views/SignupView.vue'
import LoginView from '@/views/LoginView.vue'
import MainView from '@/views/MainView.vue'

import FindPasswordView from '@/views/FindPasswordView.vue'
import SearchView from '@/views/SearchView.vue'
import HotelDetailView from '@/views/HotelDetailView.vue'

// 마이페이지 관련
import MyPage from '@/views/mypage/MyPage.vue'
import Account from '@/views/mypage/Account.vue'
import Coupons from '@/views/mypage/Coupons.vue'
import History from '@/views/mypage/History.vue'
import Support from '@/views/mypage/Support.vue'

// 고객지원
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

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  },
  routes: [
    // 메인은 공개
    { path: '/main', component: MainView, name: 'main', meta: { public: true } },
    { path: '/', redirect: { name: 'main' } },

    // 인증 화면들(원하면 메타 유지해도 무방)
    { path: '/login',  component: LoginView,  meta: { public: true } },
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

    // 보호 라우트
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

    // 고객지원
    { path: '/support/notice', component: NoticeList, meta: { public: true } },
    { path: '/support/notice/:id', component: NoticeDetail, meta: { public: true } },
    { path: '/support/faq', component: FaqList, meta: { public: true } },
    { path: '/support/faq/:id', component: FaqDetail, meta: { public: true } },
    { path: '/support/contact', component: ContactCenter, meta: { public: true } },
    { path: '/support/contact/inquiry', component: SupportInquiry, meta: { public: true } },
    { path: '/support/contact/my', component: MyTickets, meta: { requiresAuth: true } },
    { path: '/support/contact/ticket/:id', component: MyTicketDetail, props: true, meta: { requiresAuth: true } },

    // 찜
    { path: '/wishlist', name: 'Wishlist', component: Wishlist, meta: { requiresAuth: true } },
  ]
})

router.beforeEach((to, from, next) => {
  // 1) 소셜 리다이렉트 토큰 수거
  const hash = to.hash || window.location.hash
  const m = hash && hash.match(/token=([^&]+)/)
  const tokenFromHash = m ? decodeURIComponent(m[1]) : null
  const tokenFromQuery = (to.query && to.query.token) || null

  if (tokenFromHash || tokenFromQuery) {
    const token = tokenFromHash || tokenFromQuery
    localStorage.setItem('token', token)
    window.history.replaceState({}, '', to.path) // URL 정리
  }

  // 2) 보호 라우트 접근 제어
  const token = localStorage.getItem('token')
  const isPublic = to.meta && to.meta.public === true
  const needsAuth = to.meta && to.meta.requiresAuth === true

  if (needsAuth && !token) return next({ path: '/login', query: { redirect: to.fullPath } })
  if ((to.path === '/login' || to.path === '/signup') && token) return next({ name: 'main' })

  // 3) 검색/상세 날짜 정규화
  const needDates = to.name === 'search' || to.name === 'hotel-detail'
  if (needDates) {
    const q = { ...(to.query || {}) }
    const fmt = d => new Date(d).toISOString().slice(0, 10)
    const isValid = s => !!s && !Number.isNaN(new Date(String(s)).getTime())
    const addDays = (base, n) => {
      const d = new Date(base)
      d.setDate(d.getDate() + n)
      return fmt(d)
    }

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

export default router

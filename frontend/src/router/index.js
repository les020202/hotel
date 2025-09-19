import { createRouter, createWebHistory } from 'vue-router'
import SignupView from '@/views/SignupView.vue'
import LoginView from '@/views/LoginView.vue'
import MainView from '@/views/MainView.vue'

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

const router = createRouter({
  // ✅ history 옵션 반드시 필요
  history: createWebHistory(),

  // UX: 페이지 전환 시 맨 위로, 뒤로가기는 저장된 위치로
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

    // ✅ 마이페이지 라우트
    {
      path: '/mypage',
      component: MyPage,
      meta: { requiresAuth: true }, // 토큰 필요
      children: [
        { path: '', redirect: '/mypage/account' }, // 기본 진입 시 account로 이동
        { path: 'account', component: Account },   // 내 계정 관리
        { path: 'coupon', component: Coupons },   //쿠폰
        { path: 'history', component: History },   // 예약/이용 내역
        { path: 'support', component: Support }    // 고객지원 진입
      ]
    },
    // ✅ 고객지원 페이지 (로그인 없이도 접근 가능)
    { path: '/support/notice', component: NoticeList },      
    { path: '/support/notice/:id', component: NoticeDetail },
    { path: '/support/faq', component: FaqList},             
    { path: '/support/faq/:id', component: FaqDetail},       
    { path: '/support/contact', component: ContactCenter },  
    { path: '/support/contact/inquiry', component: SupportInquiry },
    { path: '/support/contact/my', component: MyTickets },                       // 내 문의함 목록

    { path: '/support/contact/ticket/:id', component: MyTicketDetail, props: true }, // 상세/스레드

    //찜페이지
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
    localStorage.setItem('token', token)         // 토큰 저장
    window.history.replaceState({}, '', to.path) // URL에서 제거 (화면은 그대로)
  }

  // 2) 이후 토큰 기준 접근 제어
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) return next('/login')
  if ((to.path === '/login' || to.path === '/signup') && token) return next('/main')

  // --- 3) 날짜 정규화: 검색/상세 공통 ---
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

  next()
})

export default router

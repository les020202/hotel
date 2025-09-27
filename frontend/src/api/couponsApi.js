// src/api/couponsApi.js
import api from '@/api/auth' // ✅ 토큰 포함 axios 인스턴스

/** GET /api/user/coupon?all=true|false */
export function fetchMyCoupons({ all = true } = {}) {
  return api.get('/user/coupon', { params: { all } }).then(r => r.data)
}

/** POST /api/coupons/claim  (쿠폰 코드로 발급) */
export function claimCoupon(code) {
  return api.post('/coupons/claim', { code }).then(r => r.data)
  // 필요시 에러 메시지 껍질:
  // .catch(err => {
  //   const msg = err?.response?.data?.message || '쿠폰 지급 실패'
  //   throw new Error(msg)
  // })
}

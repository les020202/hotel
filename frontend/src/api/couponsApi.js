import api from '@/api/auth' // 토큰 포함 axios 인스턴스

// GET /api/user/coupon?all=true|false
export function fetchMyCoupons({ all = true } = {}) {
  return api.get('/user/coupon', { params: { all } }).then(r => r.data)
}

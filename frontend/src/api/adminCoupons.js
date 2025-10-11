// src/api/adminCoupons.js
import { get, post } from '@/api/_http'

// Authorization 헤더가 자동으로 붙는 래퍼 사용
export const fetchCoupons = () => get('/admin/coupons')
export const createCoupon = (payload) => post('/admin/coupons', payload)

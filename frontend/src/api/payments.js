// src/api/payments.js
import axios from 'axios'

// 베이스 계산 (중복 /api 방지)
const raw = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/, '')
const apiBase = raw
  ? (raw.endsWith('/api') ? raw : `${raw}/api`)
  : '/api'

// 인증 불필요 인스턴스
const noAuth = axios.create({
  baseURL: apiBase,
  withCredentials: true,
})

// 결제 승인 요청: 받은 바디를 그대로 전송 (guestName/guestPhone 포함)
export const confirmPayment = (payload) => {
  // payload: { paymentKey, orderId, amount, holdCode?, guestName?, guestPhone? }
  return noAuth.post('/payments/confirm', payload).then(r => r.data)
}

export const confirmTossPayment = confirmPayment

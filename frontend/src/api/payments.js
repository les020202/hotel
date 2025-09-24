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

// 결제 승인 요청
export const confirmPayment = ({ paymentKey, orderId, amount, holdCode }) => {
  const payload = { paymentKey, orderId, amount }
  if (holdCode) payload.holdCode = holdCode
  // 🔑 여기엔 '/payments/confirm' (앞에 /api 붙이지 않음!)
  return noAuth.post('/payments/confirm', payload).then(r => r.data)
}

export const confirmTossPayment = confirmPayment

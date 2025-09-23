// src/api/payments.js
import api from '@/api/auth'

/** 권장 이름 */
export const confirmPayment = ({ paymentKey, orderId, amount, holdCode }) => {
  const payload = { paymentKey, orderId, amount }
  if (holdCode) payload.holdCode = holdCode   // undefined면 안 보냄
  return api.post('/payments/confirm', payload).then(r => r.data)
}

/** 기존 코드 호환용 (선택) */
export const confirmTossPayment = confirmPayment

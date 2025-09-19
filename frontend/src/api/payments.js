// src/api/payments.js
import api from '@/api/auth'

 export const confirmTossPayment = ({ paymentKey, orderId, amount, holdCode }) =>
   api.post('/payments/toss/confirm', { paymentKey, orderId, amount, holdCode })
     .then(r => r.data)
     
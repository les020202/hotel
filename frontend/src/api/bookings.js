// src/api/bookings.js
import http from '@/api/_http'

// 예약 취소 (USER/OWNER/ADMIN 공통)
export function cancelBooking(bookingId, reason = '') {
  return http.post(`/api/bookings/${bookingId}/cancel`, { reason })
}

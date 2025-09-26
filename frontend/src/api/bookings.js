import { post } from '@/api/_http'

export function cancelBooking(bookingId, reason = '') {
  return post(`/api/bookings/${bookingId}/cancel`, { reason })
}

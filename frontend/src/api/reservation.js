// path: src/api/reservation.js
import api from '@/api/auth'

/** 대표 호텔 */
export const getFeaturedHotel = (hotelId) =>
  hotelId ? api.get(`/hotels/${hotelId}/featured`) : api.get('/hotels/featured')

/** ✅ 쿠폰 목록 (유저에게 발급/사용 가능) */
export const getAvailableCoupons = (userId) => {
  if (!userId) throw new Error('userId가 필요합니다.')
  return api.get(`/coupons/available`, { params: { userId } })
}

/** 예약 홀드 생성 */
export const createReservationHold = ({
  userId, hotelId, roomTypeId, ratePlanId, checkIn, checkOut,
  guests = 1, couponCode, guestName, phone,
}) => {
  if (!userId || !hotelId || !roomTypeId || !ratePlanId || !checkIn || !checkOut) {
    throw new Error('필수 값 누락: userId, hotelId, roomTypeId, ratePlanId, checkIn, checkOut')
  }
  return api.post('/reservations/hold', {
    userId, hotelId, roomTypeId, ratePlanId, checkIn, checkOut,
    guests, couponCode, guestName, phone,
  })
}

/** ✅ 리프라이스(쿠폰 적용/해제) — 반드시 PUT! */
export const repriceReservationHold = (holdCode, { couponCode, userId } = {}) => {
  if (!holdCode) throw new Error('holdCode가 필요합니다.')
  const body = {}
  if (typeof couponCode !== 'undefined' && couponCode !== null) body.couponCode = couponCode
  if (typeof userId === 'number') body.userId = userId
  return api.put(`/reservations/hold/${encodeURIComponent(holdCode)}/reprice`, body)
}

/** 홀드 취소 */
export const cancelReservationHold = (holdCode) => {
  if (!holdCode) throw new Error('holdCode가 필요합니다.')
  return api.delete(`/reservations/hold/${encodeURIComponent(holdCode)}`)
}

/** (옵션) 빠른 확정 예약 */
export const createQuickReservation = (payload) => api.post('/reservations/quick', payload)

/** (ADMIN) 만료 홀드 정리 */
export const releaseExpiredHolds = () => api.post('/reservations/holds/release-expired')

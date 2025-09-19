// path: src/api/reservation.js
import api from '@/api/auth'

/**
 * 대표 호텔 정보 + 룸타입들
 * - hotelId 주면: GET /hotels/{hotelId}/featured
 * - 없으면:      GET /hotels/featured
 */
export const getFeaturedHotel = (hotelId) =>
  hotelId
    ? api.get(`/hotels/${hotelId}/featured`)
    : api.get('/hotels/featured')

/**
 * 예약 홀드 생성
 * [백엔드 스펙]
 *  POST /reservations/hold
 *  body: {
 *    userId: number,        // ✅ 필수
 *    hotelId: number,       // ✅ 필수
 *    roomTypeId: number,    // ✅ 필수
 *    ratePlanId: number,    // ✅ 필수
 *    checkIn: 'YYYY-MM-DD', // ✅ 필수
 *    checkOut:'YYYY-MM-DD', // ✅ 필수 (checkOut > checkIn)
 *    guests: number,        // 기본 1
 *    couponCode?: string,   // 옵션
 *    guestName?: string,    // 옵션(표시용)
 *    phone?: string         // 옵션(표시용)
 *  }
 * [응답]
 *  { holdCode: string, expiresAt: string, totalAmount: number }
 */
export const createReservationHold = ({
  userId,
  hotelId,
  roomTypeId,
  ratePlanId,
  checkIn,
  checkOut,
  guests = 1,
  couponCode,
  guestName,
  phone,
}) => {
  // 프런트 방어적 검증(빠른 피드백)
  if (!userId || !hotelId || !roomTypeId || !ratePlanId || !checkIn || !checkOut) {
    throw new Error('필수 값 누락: userId, hotelId, roomTypeId, ratePlanId, checkIn, checkOut')
  }
  return api.post('/reservations/hold', {
    userId,
    hotelId,
    roomTypeId,
    ratePlanId,
    checkIn,
    checkOut,
    guests,
    couponCode,
    guestName,
    phone,
  })
}

/**
 * 예약 홀드 취소(수동)
 *  DELETE /reservations/hold/{holdCode}
 *  - 백엔드: booked 복구 + booking_holds 삭제(멱등)
 */
export const cancelReservationHold = (holdCode) => {
  if (!holdCode) throw new Error('holdCode가 필요합니다.')
  return api.delete(`/reservations/hold/${encodeURIComponent(holdCode)}`)
}

/**
 * (옵션) 빠른 확정 예약 — 현재 사용 안 하면 보류 가능
 *  POST /reservations/quick
 */
export const createQuickReservation = (payload) =>
  api.post('/reservations/quick', payload)

/**
 * (운영/테스트용) 만료 홀드 일괄 정리 — ADMIN 전용
 *  POST /reservations/holds/release-expired
 *  일반 프런트에는 노출하지 마세요.
 */
export const releaseExpiredHolds = () =>
  api.post('/reservations/holds/release-expired')

// src/api/hotelApi.js
import api from '@/api/auth' // 공용 axios 인스턴스 (토큰/401-리프레시/withCredentials 포함)

/**
 * 호텔 상세 조회
 * GET /api/hotels/{id}?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&guests=N
 *  - 백엔드 컨트롤러는 params={"checkIn","checkOut","guests"} 조건이라 세 파라미터 모두 필요합니다.
 */
export const getHotelDetail = (id, checkIn, checkOut, guests = 1) => {
  if (!id) throw new Error('getHotelDetail: id is required')

  const toISODate = (d) => {
    if (!d) return null
    const x = new Date(String(d))
    return Number.isNaN(x.getTime()) ? null : x.toISOString().slice(0, 10)
  }

  const ci = toISODate(checkIn)
  const co = toISODate(checkOut)
  const g  = Number(guests || 1)

  if (!ci || !co) throw new Error('getHotelDetail: checkIn/checkOut are required')
  if (!(new Date(co) > new Date(ci))) throw new Error('getHotelDetail: checkOut must be after checkIn')

  return api
    .get(`/hotels/${id}`, { params: { checkIn: ci, checkOut: co, guests: g } })
    .then((r) => r.data)
}

export default {
  getHotelDetail,
}

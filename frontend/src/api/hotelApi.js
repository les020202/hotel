// src/api/hotelApi.js
// 공용 Axios 인스턴스(토큰/401-리프레시/withCredentials 포함)
import api from '@/api/auth' 

/**
 * 호텔 상세 조회
 * GET /api/hotels/{id}?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&guests=N
 *  - 백엔드 컨트롤러는 params={"checkIn","checkOut","guests"} 조건이므로 세 파라미터 모두 필요합니다.
 *  - 기본 엔드포인트는 `/hotels/{id}` 이고, 필요하면 opts.details=true 로 `/hotels/{id}/details` 사용 가능.
 *  - 프로젝트에 따라 api 인스턴스의 baseURL이 `/api` 인 경우가 일반적입니다. (예: baseURL: '/api')
 */
function toISODate(d) {
  if (!d) return null
  const x = new Date(String(d))
  return Number.isNaN(x.getTime()) ? null : x.toISOString().slice(0, 10)
}

/**
 * @param {number|string} id              호텔 ID (필수)
 * @param {string|Date}   checkIn         체크인 (필수, YYYY-MM-DD 또는 Date)
 * @param {string|Date}   checkOut        체크아웃 (필수, YYYY-MM-DD 또는 Date)
 * @param {number}        guests          투숙 인원 (기본 1)
 * @param {object}        opts            옵션
 * @param {boolean}       opts.details    true면 `/hotels/{id}/details` 엔드포인트 사용
 * @returns {Promise<any>}                응답 데이터
 */
export const getHotelDetail = (id, checkIn, checkOut, guests = 1, opts = {}) => {
  if (!id) throw new Error('getHotelDetail: id is required')

  const ci = toISODate(checkIn)
  const co = toISODate(checkOut)
  const g  = Number(guests || 1)

  if (!ci || !co) throw new Error('getHotelDetail: checkIn/checkOut are required')
  if (!(new Date(co) > new Date(ci))) {
    throw new Error('getHotelDetail: checkOut must be after checkIn')
  }

  // won 브랜치의 쿼리스트링 구성 아이디어는 params로 흡수(axios가 안전하게 처리)
  const path = opts.details ? `/hotels/${id}/details` : `/hotels/${id}`
  return api.get(path, { params: { checkIn: ci, checkOut: co, guests: g } })
           .then(r => r.data)
}

export default {
  getHotelDetail,
}

// src/api/wishlistApi.js
import api from '@/api/auth' 
// ✅ 공용 Axios 인스턴스 사용
// - Authorization 헤더(JWT) 자동 첨부
// - 401 응답 시 refresh 토큰으로 재발급/재시도
// - baseURL(/api)·withCredentials 등 공통 설정 일괄 적용
// → 로그인/세션 정책을 한 곳에서 관리하고, 위시리스트 API도 동일 정책으로 동작하게 함

/**
 * 위시리스트 페이지 데이터 조회 (최신 등록순 페이징)
 * @param {Object} options
 * @param {number} [options.limit=4]  - 한 번에 가져올 아이템 수 (UI는 4개씩 더보기에 맞춤)
 * @param {number} [options.offset=0] - 서버 페이징 오프셋(다음 페이지 시작 위치)
 * @returns {Promise<{
 *   items: Array<{ wishlistId:number, createdAt:string, hotel:object }>,
 *   total: number,
 *   hasMore: boolean,
 *   nextOffset: number
 * }>}
 *
 * - GET /api/wishlists?limit=4&offset=0
 * - 컨트롤러는 로그인 사용자 기준으로 최신 등록순 반환
 * - 응답 포맷은 WishlistPageDto 가정
 */
export const fetchWishlist = ({ limit = 4, offset = 0 } = {}) =>
  api.get('/wishlists', { params: { limit, offset } }).then(r => r.data)

/**
 * 위시리스트 추가(찜)
 * @param {number} hotelId - 찜할 호텔 ID
 * @returns {Promise<any>} - 생성된 위시리스트 레코드(또는 성공 응답)
 *
 * - POST /api/wishlists  { hotelId }
 * - 서버에는 사용자-호텔 유니크 제약(중복 방지)이 있을 수 있음
 * - UI에서는 이미 찜 여부를 표시하거나, 응답에 따라 버튼 토글
 */
export const addWishlist = (hotelId) =>
  api.post('/wishlists', { hotelId }).then(r => r.data)

/**
 * 위시리스트 삭제(찜 해제)
 * @param {number} wishlistId - 위시리스트 PK (user_id + hotel_id 아님)
 * @returns {Promise<boolean>} - 성공 시 true
 *
 * - DELETE /api/wishlists/{wishlistId}
 * - 성공하면 목록에서 해당 아이템 제거하고, 필요 시 다음 페이지를 더 불러 UI를 채움
 */
export const deleteWishlist = (wishlistId) =>
  api.delete(`/wishlists/${wishlistId}`).then(() => true)

/**
 * 호환용 삭제 헬퍼
 * @param {number|{ wishlistId:number }} idOrObj
 * @returns {Promise<boolean>}
 *
 * - 컴포넌트에서 아이템 객체 자체를 넘기거나 숫자 ID를 넘겨도 동작하게 만든 편의 함수
 *   removeWishlist(123)                     → deleteWishlist(123)
 *   removeWishlist({ wishlistId: 123 })     → deleteWishlist(123)
 * - 잘못된 인자가 들어오면 명시적인 에러로 개발 단계에서 바로 알림
 */
export const removeWishlist = (idOrObj) => {
  if (typeof idOrObj === 'number') return deleteWishlist(idOrObj)
  if (idOrObj && typeof idOrObj.wishlistId === 'number') return deleteWishlist(idOrObj.wishlistId)
  return Promise.reject(new Error('removeWishlist: pass wishlistId (number) or { wishlistId }'))
}

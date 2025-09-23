// src/api/wishlistApi.js

import api from '@/api/auth' 
import { reactive } from 'vue'
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

// ---------------------------------------------
// [추가 기능] 전역 상태 + 하트 토글 헬퍼
// ---------------------------------------------

/**
 * 전역 찜 상태 (이 모듈이 싱글톤처럼 동작)
 * - ids: Set<hotelId>
 * - map: Map<hotelId, wishlistId> (서버 삭제 시 필요)
 */
const store = reactive({
  loaded: false,     // 서버에서 최소 1회 로드했는지
  busy: false,       // 토글/로드 동시실행 보호
  ids: new Set(),    // 호텔ID Set
  map: new Map(),    // hotelId -> wishlistId
})

/** 외부에서 상태를 관찰하고 싶을 때 사용(선택) */
export function wishlistState() {
  return store
}

/** 내부: 서버에서 받은 items를 상태에 반영 */
function ingest(items = []) {
  const nextIds = new Set(store.ids)
  const nextMap = new Map(store.map)
  for (const it of items) {
    const hotelId = it?.hotel?.id ?? it?.hotelId ?? it?.id
    const wishlistId = it?.wishlistId ?? it?.id
    if (!hotelId) continue
    nextIds.add(Number(hotelId))
    if (wishlistId) nextMap.set(Number(hotelId), Number(wishlistId))
  }
  store.ids = nextIds
  store.map = nextMap
}

/**
 * 로그인 상태일 때 최초 1회 전체(또는 여러 페이지) 로딩
 * - 페이징 API를 돌면서 전부 Set에 채움
 * - 규모가 크면 limit를 늘리거나 일부만 싱크해도 OK
 */
export async function ensureWishlistLoaded() {
  if (store.loaded || store.busy) return
  if (!isLoggedIn()) { store.loaded = true; return }

  store.busy = true
  try {
    let offset = 0
    const limit = 100
    while (true) {
      const page = await fetchWishlist({ limit, offset })
      const items = page?.items ?? page ?? []
      ingest(items)
      if (!page?.hasMore) break
      offset = page?.nextOffset ?? (offset + items.length)
    }
  } finally {
    store.busy = false
    store.loaded = true
  }
}

/** 강제 재동기화가 필요할 때 */
export async function refreshWishlistState() {
  store.loaded = false
  store.ids.clear()
  store.map.clear()
  await ensureWishlistLoaded()
}

/** 현재 호텔이 찜 상태인지 */
export function isWished(hotelId) {
  if (!hotelId) return false
  return store.ids.has(Number(hotelId))
}

/**
 * 하트 토글(낙관적 업데이트)
 * - 비로그인: AUTH_REQUIRED 에러 throw → 호출부에서 /login으로 유도
 * - 서버 실패 시 UI 롤백
 */
export async function toggleWishlist(hotelId) {
  hotelId = Number(hotelId)
  if (!hotelId) return

  if (!isLoggedIn()) {
    const err = new Error('AUTH_REQUIRED')
    err.code = 'AUTH_REQUIRED'
    throw err
  }

  // 초기 로딩 보장
  await ensureWishlistLoaded()

  // 동시 요청 보호(원하면 제거 가능)
  if (store.busy) return
  store.busy = true

  const already = store.ids.has(hotelId)

  // 낙관적 UI
  if (already) store.ids.delete(hotelId)
  else store.ids.add(hotelId)

  try {
    if (already) {
      // 서버 삭제에는 wishlistId가 필요 → map에서 찾아 사용
      const wid = store.map.get(hotelId)
      if (!wid) {
        // map에 없으면 재동기화 후 재시도
        await refreshWishlistState()
        const wid2 = store.map.get(hotelId)
        if (!wid2) throw new Error('Missing wishlistId for hotelId=' + hotelId)
        await deleteWishlist(wid2)
      } else {
        await deleteWishlist(wid)
      }
      store.map.delete(hotelId)
    } else {
      const created = await addWishlist(hotelId)
      // 응답에서 wishlistId 추출(필드명이 환경마다 다를 수 있음 → 방어코드)
      const wid =
        created?.wishlistId ??
        created?.id ??
        created?.data?.wishlistId ??
        created?.data?.id
      if (wid) store.map.set(hotelId, Number(wid))
    }
  } catch (e) {
    // 실패 시 롤백
    if (already) store.ids.add(hotelId)
    else store.ids.delete(hotelId)
    throw e
  } finally {
    store.busy = false
  }
}

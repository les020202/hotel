// src/api/wishlistApi.js
import api, { isLoggedIn } from '@/api/auth'
import { reactive } from 'vue'

/** 페이지네이션 조회 */
export const fetchWishlist = ({ limit = 100, offset = 0 } = {}) =>
  api.get('/wishlists', { params: { limit, offset } }).then(r => r.data)

/** 생성 (호텔 찜) */
export const addWishlist = (hotelId) =>
  api.post('/wishlists', { hotelId }).then(r => r.data)

/** 삭제 (PK 기준) — 폴백용 */
export const deleteWishlist = (wishlistId) =>
  api.delete(`/wishlists/${wishlistId}`).then(() => true)

/** 삭제 (호텔 기준) — 기본 경로 */
export const deleteWishlistByHotel = (hotelId) =>
  api.delete(`/wishlists/by-hotel/${hotelId}`).then(() => true)

/** 단일 호텔 찜 여부 확인(경량) */
export const hasWishlist = (hotelId) =>
  api.get('/wishlists/has', { params: { hotelId } }).then(r => r.data)

/* ---------------------------------------------
   전역 상태 (싱글톤)
--------------------------------------------- */
const store = reactive({
  loaded: false,
  busy: false,
  ids: new Set(),         // Set<hotelId>
  map: new Map(),         // hotelId -> wishlistId
})

function ingest(items = []) {
  const nextIds = new Set(store.ids)
  const nextMap = new Map(store.map)
  for (const it of items) {
    const hotelId = it?.hotel?.id ?? it?.hotelId ?? it?.id
    const wishlistId = it?.wishlistId ?? it?.id
    if (!hotelId) continue
    nextIds.add(Number(hotelId))
    if (wishlistId != null) nextMap.set(Number(hotelId), Number(wishlistId))
  }
  store.ids = nextIds
  store.map = nextMap
}

export function wishlistState() { return store }

/** 전체 동기화(로그인시에만) */
export async function ensureWishlistLoaded() {
  if (store.loaded || store.busy) return
  if (!isLoggedIn()) { store.loaded = true; return }
  store.busy = true
  try {
    let offset = 0
    const limit = 200
    while (true) {
      const page = await fetchWishlist({ limit, offset })
      const items = page?.items ?? []
      ingest(items)
      if (!page?.hasMore) break
      offset = page?.nextOffset ?? (offset + items.length)
      if (items.length === 0) break
    }
  } finally {
    store.busy = false
    store.loaded = true
  }
}

/** 단일 호텔만 빠르게 동기화 */
export async function syncCurrentHotel(hotelId) {
  hotelId = Number(hotelId)
  if (!hotelId || !isLoggedIn()) return
  try {
    const r = await hasWishlist(hotelId)
    if (r?.wished) {
      store.ids.add(hotelId)
      if (r?.wishlistId != null) store.map.set(hotelId, Number(r.wishlistId))
    } else {
      store.ids.delete(hotelId)
      store.map.delete(hotelId)
    }
  } catch { /* 네트워크 오류는 무시 */ }
}

/** 목록 화면: 여러 호텔을 한 번에 맞춰두고 싶을 때 */
export async function syncHotels(hotelIds = []) {
  if (!isLoggedIn()) return
  await ensureWishlistLoaded() // 대부분 이걸로 커버됨
  // 별도 호출 없이 store.ids 기준으로 즉시 반영됨
}

/** 현재 호텔이 찜 상태인지 */
export function isWished(hotelId) {
  if (!hotelId) return false
  return store.ids.has(Number(hotelId))
}

/** 하트 토글(알림 없음) — 호텔ID 기준 삭제 우선 */
export async function toggleWishlist(hotelId) {
  hotelId = Number(hotelId)
  if (!hotelId) return

  if (!isLoggedIn()) {
    const err = new Error('AUTH_REQUIRED')
    err.code = 'AUTH_REQUIRED'
    throw err
  }

  await ensureWishlistLoaded()
  if (store.busy) return
  store.busy = true

  const already = store.ids.has(hotelId)
  // 낙관적 UI
  if (already) store.ids.delete(hotelId)
  else store.ids.add(hotelId)

  try {
    if (already) {
      // 호텔 기준 삭제 우선
      try {
        await deleteWishlistByHotel(hotelId)
        store.map.delete(hotelId)
      } catch (e) {
        // PK 폴백
        const wid = store.map.get(hotelId)
        if (!wid) {
          await syncCurrentHotel(hotelId)
          const wid2 = store.map.get(hotelId)
          if (!wid2) throw e
          await deleteWishlist(wid2)
        } else {
          await deleteWishlist(wid)
        }
        store.map.delete(hotelId)
      }
    } else {
      const created = await addWishlist(hotelId)
      const wid =
        created?.wishlistId ??
        created?.id ??
        created?.data?.wishlistId ??
        created?.data?.id
      if (wid != null) store.map.set(hotelId, Number(wid))
    }
  } catch (e) {
    // 롤백
    if (already) store.ids.add(hotelId)
    else store.ids.delete(hotelId)
    throw e
  } finally {
    store.busy = false
  }
}

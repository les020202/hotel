// src/api/wishlistApi.js
import api, { isLoggedIn } from '@/api/auth'
import { ref, shallowRef } from 'vue'

export const fetchWishlist = ({ limit = 100, offset = 0 } = {}) =>
  api.get('/wishlists', { params: { limit, offset } }).then(r => r.data)

export const addWishlist = (hotelId) =>
  api.post('/wishlists', { hotelId }).then(r => r.data)

export const deleteWishlist = (wishlistId) =>
  api.delete(`/wishlists/${wishlistId}`).then(() => true)

export const deleteWishlistByHotel = (hotelId) =>
  api.delete(`/wishlists/by-hotel/${hotelId}`).then(() => true)

export const hasWishlist = (hotelId) =>
  api.get('/wishlists/has', { params: { hotelId } }).then(r => r.data)

// ── 전역 상태(싱글톤) + 시그널 ───────────────────────────────
const _loaded = ref(false)
const _busy   = ref(false)
export const wishlistSignal = ref(0)

const _idsRef = shallowRef(new Set())     // Set<hotelId:number>
const _mapRef = shallowRef(new Map())     // Map<hotelId, wishlistId>

function bump() { wishlistSignal.value++ }
function setIds(next){ _idsRef.value = next; bump() }
function setMap(next){ _mapRef.value = next; bump() }

export function isWished(hotelId) {
  const hid = Number(hotelId) || 0
  return _idsRef.value.has(hid)
}

function ingest(items = []) {
  const nextIds = new Set(_idsRef.value)
  const nextMap = new Map(_mapRef.value)
  for (const it of items) {
    const hid = Number(it?.hotel?.id ?? it?.hotelId ?? it?.id) || 0
    const wid = it?.wishlistId ?? it?.id
    if (!hid) continue
    nextIds.add(hid)
    if (wid != null) nextMap.set(hid, Number(wid))
  }
  setIds(nextIds); setMap(nextMap)
}

export async function ensureWishlistLoaded() {
  if (_loaded.value || _busy.value) return
  if (!isLoggedIn()) { _loaded.value = true; return }
  _busy.value = true
  try {
    let offset = 0, limit = 200
    while (true) {
      const page = await fetchWishlist({ limit, offset })
      const items = page?.items ?? []
      ingest(items)
      if (!page?.hasMore) break
      offset = page?.nextOffset ?? (offset + items.length)
      if (items.length === 0) break
    }
  } finally {
    _busy.value = false
    _loaded.value = true
  }
}

export async function syncCurrentHotel(hotelId) {
  const hid = Number(hotelId) || 0
  if (!hid || !isLoggedIn()) return
  try {
    const r = await hasWishlist(hid)
    const nextIds = new Set(_idsRef.value)
    const nextMap = new Map(_mapRef.value)
    if (r?.wished) {
      nextIds.add(hid)
      if (r?.wishlistId != null) nextMap.set(hid, Number(r.wishlistId))
    } else {
      nextIds.delete(hid)
      nextMap.delete(hid)
    }
    setIds(nextIds); setMap(nextMap)
  } catch { /* ignore */ }
}

/**
 * ✅ 핵심 수정: 화면에 보이는 여러 호텔의 위시 상태를 서버로부터 “재검증”
 *  - 서버에 배치 API가 있으면 그걸 사용하고,
 *  - 없으면 hasWishlist를 id별로 병렬 호출
 */
export async function syncHotels(hotelIds = []) {
  if (!isLoggedIn()) return
  const ids = [...new Set(hotelIds.map(n => Number(n) || 0).filter(Boolean))]
  if (ids.length === 0) return

  // 배치 엔드포인트가 있다면 주석 해제해서 사용하세요.
  // try {
  //   const { data } = await api.post('/wishlists/has-many', { hotelIds: ids })
  //   // data: [{ hotelId, wished, wishlistId }]
  //   const nextIds = new Set(_idsRef.value)
  //   const nextMap = new Map(_mapRef.value)
  //   for (const it of (data || [])) {
  //     const hid = Number(it.hotelId) || 0
  //     if (!hid) continue
  //     if (it.wished) {
  //       nextIds.add(hid)
  //       if (it.wishlistId != null) nextMap.set(hid, Number(it.wishlistId))
  //     } else {
  //       nextIds.delete(hid)
  //       nextMap.delete(hid)
  //     }
  //   }
  //   setIds(nextIds); setMap(nextMap)
  //   return
  // } catch { /* fallback below */ }

  // 폴백: 개별 조회 병렬
  const results = await Promise.allSettled(ids.map(id => hasWishlist(id)))
  const nextIds = new Set(_idsRef.value)
  const nextMap = new Map(_mapRef.value)
  for (let i = 0; i < ids.length; i++) {
    const hid = ids[i]
    const r = results[i]
    if (r.status !== 'fulfilled') continue
    const v = r.value
    if (v?.wished) {
      nextIds.add(hid)
      if (v?.wishlistId != null) nextMap.set(hid, Number(v.wishlistId))
    } else {
      nextIds.delete(hid)
      nextMap.delete(hid)
    }
  }
  setIds(nextIds); setMap(nextMap)
}

export async function toggleWishlist(hotelId) {
  const hid = Number(hotelId) || 0
  if (!hid) return
  if (!isLoggedIn()) {
    const err = new Error('AUTH_REQUIRED')
    err.code = 'AUTH_REQUIRED'
    throw err
  }
  await ensureWishlistLoaded()
  if (_busy.value) return
  _busy.value = true

  const already = _idsRef.value.has(hid)
  // 낙관적
  const optimistic = new Set(_idsRef.value)
  already ? optimistic.delete(hid) : optimistic.add(hid)
  setIds(optimistic)

  try {
    if (already) {
      try {
        await deleteWishlistByHotel(hid)
        const nm = new Map(_mapRef.value); nm.delete(hid); setMap(nm)
      } catch (e) {
        const wid = _mapRef.value.get(hid)
        if (!wid) {
          await syncCurrentHotel(hid)
          const wid2 = _mapRef.value.get(hid)
          if (!wid2) throw e
          await deleteWishlist(wid2)
        } else {
          await deleteWishlist(wid)
        }
        const nm = new Map(_mapRef.value); nm.delete(hid); setMap(nm)
      }
    } else {
      const created = await addWishlist(hid)
      const wid = created?.wishlistId ?? created?.id ?? created?.data?.wishlistId ?? created?.data?.id
      if (wid != null) {
        const nm = new Map(_mapRef.value); nm.set(hid, Number(wid)); setMap(nm)
      }
    }
  } catch (e) {
    // 롤백
    const rb = new Set(_idsRef.value)
    already ? rb.add(hid) : rb.delete(hid)
    setIds(rb)
    throw e
  } finally {
    _busy.value = false
  }
}

import api from '@/api/auth'

// 호텔명 검색 (오토컴플릿)
export async function searchHotels(q, { limit = 10 } = {}) {
  const res = await api.get('/api/admin/hotels/search', { params: { q, limit } })
  return await res.json?.() ?? res.data   // fetch형/axios형 둘 다 커버
}

// 특정 호텔의 객실타입 목록
export async function listRoomTypes(hotelId) {
  if (!hotelId) return []
  const res = await api.get(`/api/admin/hotels/${hotelId}/room-types`)
  return await res.json?.() ?? res.data
}

export default { searchHotels, listRoomTypes }

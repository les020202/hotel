import axios from 'axios'

export const getHotelDetail = (id, checkIn, checkOut, guests = 1) => {
  const qs = new URLSearchParams()
  if (checkIn) qs.append('checkIn', checkIn)
  if (checkOut) qs.append('checkOut', checkOut)
  if (guests != null) qs.append('guests', guests) // ✅ 인원 추가

  const suffix = qs.toString() ? `?${qs}` : ''
  // 백엔드가 /api/hotels/{id}/details 라우트면 아래 주석처럼 바꿔도 OK
  // return axios.get(`/api/hotels/${id}/details${suffix}`).then(r => r.data)
  return axios.get(`/api/hotels/${id}${suffix}`).then(r => r.data)
}

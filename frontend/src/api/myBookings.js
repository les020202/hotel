// src/api/myBookings.js
import http from './http'

/**
 * 목록 조회 (Page<MyBookingSummary>)
 * @param {number} page 0-based
 * @param {number} size
 */
export async function fetchMyBookings(page = 0, size = 10) {
  const { data } = await http.get('/my/bookings', { params: { page, size } })
  return data
}

/**
 * 단건 조회 (MyBookingSummary)
 * @param {number} id bookingId
 */
export async function fetchMyBooking(id) {
  const { data } = await http.get(`/my/bookings/${id}`)
  return data
}

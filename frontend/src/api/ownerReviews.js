// src/api/ownerReviews.js
import { get, post } from '@/api/_http'

// 목록 조회 (오너: 내 호텔 리뷰 - 공개/숨김 모두 가능)
export async function ownerFetchReviews(hotelId, { page = 0, size = 20, q, visible, all = false, from, to } = {}) {
  const qs = new URLSearchParams()
  qs.set('page', String(page))
  qs.set('size', String(size))
  if (q) qs.set('q', q)
  if (visible !== undefined && visible !== null) qs.set('visible', String(visible))
  if (all) qs.set('all', 'true')
  if (from) qs.set('from', from) // YYYY-MM-DD
  if (to) qs.set('to', to)       // YYYY-MM-DD
  return get(`/owner/hotels/${hotelId}/reviews?${qs.toString()}`)
}

// 신고 (오너 → OWNER 플래그 경로)
export async function ownerReportReview(hotelId, reviewId, { reason, detail } = {}) {
  return post(`/owner/hotels/${hotelId}/reviews/${reviewId}/report`, { reason, detail })
}

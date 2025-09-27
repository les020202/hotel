// src/api/adminReviews.js
// 관리자 전용 리뷰/신고 API
import { get, del } from '@/api/_http'

/**
 * 리뷰 목록
 * 서버(권장) 파라미터:
 * - hotelId?: number
 * - visible?: boolean
 * - reportedOnly?: boolean
 * - q?: string
 * - from?: 'YYYY-MM-DD'
 * - to?: 'YYYY-MM-DD'
 * - page?: number
 * - size?: number
 *
 * 호환 파라미터(옵션): minRating/maxRating 또는 min/max → 서버가 쓰면 사용, 아니면 무시
 */
export async function adminFetchReviews(params = {}) {
  const qs = new URLSearchParams()

  if (params.hotelId != null)   qs.set('hotelId', String(params.hotelId))
  if (params.visible != null)   qs.set('visible', String(params.visible))
  if (params.reportedOnly != null) qs.set('reportedOnly', String(params.reportedOnly))
  if (params.q)                 qs.set('q', params.q)

  // 날짜는 YYYY-MM-DD 문자열 기대 (Date 객체면 변환)
  const toDateStr = (d) => {
    if (!d) return null
    if (typeof d === 'string') return d
    try {
      const dt = new Date(d)
      if (isNaN(dt)) return null
      return dt.toISOString().slice(0, 10)
    } catch { return null }
  }
  const fromStr = toDateStr(params.from)
  const toStr   = toDateStr(params.to)
  if (fromStr) qs.set('from', fromStr)
  if (toStr)   qs.set('to', toStr)

  if (params.page != null) qs.set('page', String(params.page))
  if (params.size != null) qs.set('size', String(params.size))

  // 호환: 평점 필터 (서버에서 지원하면 사용)
  if (params.minRating != null) { qs.set('minRating', String(params.minRating)); qs.set('min', String(params.minRating)) }
  if (params.maxRating != null) { qs.set('maxRating', String(params.maxRating)); qs.set('max', String(params.maxRating)) }

  return get(`/admin/reviews${qs.toString() ? `?${qs}` : ''}`)
}

/**
 * 신고 목록
 * 서버 파라미터:
 * - minRating?: number
 * - maxRating?: number
 * - ownerOnly?: boolean
 * - q?: string
 */
export async function adminFetchReports(params = {}) {
  const qs = new URLSearchParams()
  if (params.minRating != null) { qs.set('minRating', String(params.minRating)); qs.set('min', String(params.minRating)) }
  if (params.maxRating != null) { qs.set('maxRating', String(params.maxRating)); qs.set('max', String(params.maxRating)) }
  if (params.ownerOnly)         qs.set('ownerOnly', 'true')
  if (params.q)                 qs.set('q', params.q)
  return get(`/admin/reviews/reports${qs.toString() ? `?${qs}` : ''}`)
}

/** 리뷰 상세 (평점/내용/사진 등 상세보기용) */
export async function adminFetchReviewDetail(reviewId) {
  return get(`/admin/reviews/${reviewId}`)
}

/** 리뷰 삭제(신고/사진 포함 하드삭제) */
export async function adminDeleteReview(reviewId) {
  return del(`/admin/reviews/${reviewId}`)
}

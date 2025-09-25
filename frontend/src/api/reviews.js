// src/api/reviews.js
import { get, post, upload, postMultipart, del } from '@/api/_http'

/**
 * 호텔 상세의 리뷰 목록 (페이지네이션)
 * GET /api/hotels/{hotelId}/reviews?page&size
 */
export async function fetchHotelReviews(hotelId, page = 0, size = 10) {
  return get(`/hotels/${hotelId}/reviews?page=${page}&size=${size}`)
}

/**
 * 호텔 평균 평점/리뷰 수
 * GET /api/hotels/{hotelId}/reviews/rating
 * -> { avg: number, count: number }
 */
export async function fetchHotelRating(hotelId) {
  return get(`/hotels/${hotelId}/reviews/rating`)
}

/**
 * 현재 로그인 사용자의 작성 가능 여부
 * GET /api/hotels/{hotelId}/reviews/eligibility
 * -> { eligible: boolean, bookingId?: number, reason?: string }
 */
export async function checkReviewEligibility(hotelId) {
  return get(`/hotels/${hotelId}/reviews/eligibility`)
}

/**
 * 리뷰 생성 (사진 1장 포함 가능)
 * POST /api/hotels/{hotelId}/reviews  (multipart/form-data)
 * form: bookingId, rating, comment?, photo?
 * -> { id, ... }
 */
export async function createReview(hotelId, { bookingId, rating, comment, photo }) {
  const fd = new FormData()
  if (bookingId != null) fd.append('bookingId', String(bookingId))
  fd.append('rating', String(rating))
  if (comment) fd.append('comment', comment)
  if (photo)   fd.append('photo', photo) // 사진 1장

  return postMultipart(`/hotels/${hotelId}/reviews`, fd)
}

/**
 * 리뷰 사진 1장 업로드 (별도 엔드포인트를 쓸 경우)
 * POST /api/reviews/{reviewId}/photo
 * multipart: file
 * -> { url }
 */
export async function uploadReviewPhoto(reviewId, file) {
  const fd = new FormData()
  fd.append('file', file)
  return upload(`/reviews/${reviewId}/photo`, fd)
}

export async function deleteReview(id) {
  return del(`/reviews/${id}`)
}

/**
 * 리뷰 신고
 * POST /api/reviews/{reviewId}/report
 * body: { reason }
 */
export async function reportReview(reviewId, reason) {
  return post(`/reviews/${reviewId}/report`, { reason })
}

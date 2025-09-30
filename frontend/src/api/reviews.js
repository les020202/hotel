// src/api/reviews.js
import { get, post, upload, postMultipart, del } from '@/api/_http'
import api from '@/api/auth';

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
 * 리뷰 사진 업로드 (별도 엔드포인트를 쓰는 경우)
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
 * 허용 입력:
 *  - { reason } | { code } | { reasonCode }
 *  - { detail }
 *  - { reporterType }  // 서버가 쓰면 전달, 아니면 무시
 */
export async function reportReview(reviewId, payload = {}) {
  // 다양한 키를 reason으로 정규화하고 공백/placeholder 제거
  let reason =
    (payload.reason ?? payload.code ?? payload.reasonCode ?? '')
      .toString()
      .trim()
  if (reason === '선택' || reason === '선택하세요') reason = ''

  const body = {
    // 서버에서 null/빈 값은 "기타"로 처리하므로 프론트에서 막지 않는다.
    reason: reason || null,
    detail: (payload.detail ?? '').toString().trim() || null,
  }
  if (payload.reporterType) body.reporterType = payload.reporterType

  return post(`/reviews/${reviewId}/report`, body)
}

export async function fetchMyReviews(page = 0, size = 10) {
  const { data } = await api.get("/my/reviews", { params: { page, size } });
  return data;
}
export async function deleteMyReview(reviewId) {
  await api.delete(`/my/reviews/${reviewId}`);
}


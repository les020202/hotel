import { get, post } from '@/api/_http'

// 관리자: 신고 목록
export const adminListReviewReports = (params={}) => {
  const q = new URLSearchParams()
  if (params.status) q.set('status', params.status)        // PENDING | DECIDED
  if (params.reporterType) q.set('reporterType', params.reporterType) // USER | OWNER
  if (params.reason) q.set('reason', params.reason)        // 스팸/욕설 등
  if (params.hotelName) q.set('hotelName', params.hotelName)
  if (params.loginId) q.set('loginId', params.loginId)
  q.set('page', String(params.page ?? 0))
  q.set('size', String(params.size ?? 20))
  return get(`/admin/review-reports?${q.toString()}`)
}

// 관리자: 판정(KEEP or REMOVE)
export const adminDecideReviewReport = (reportId, { action, decisionReason }) =>
  post(`/admin/review-reports/${reportId}/decide`, { action, decisionReason })

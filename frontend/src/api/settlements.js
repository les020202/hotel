// src/api/settlements.js
import { api } from '@/router'

// 빈 값은 제외하고 쿼리스트링 생성
const qs = (obj = {}) =>
  new URLSearchParams(
    Object.entries(obj).filter(([, v]) => v !== undefined && v !== null && v !== '')
  ).toString()

/** 호텔별 요약 */
export const fetchSummaryByHotel = (params) =>
  api(`/api/settlements/summary?${qs(params)}`).then(r => r.json())

/** 라인(상세) */
export const fetchSettlementItems = (params) =>
  api(`/api/settlements/items?${qs(params)}`).then(r => r.json())

/** 정산서 목록 (hotelId 없어도, status 필터 가능) */
export const fetchStatements = (params) =>
  api(`/api/settlements/statements?${qs(params)}`).then(r => r.json())

/** 완료된 정산서 목록(통일: /statements?status=SETTLED 사용) */
export const fetchSettledStatements = (params) =>
  api(`/api/settlements/statements?${qs({ ...params, status: 'SETTLED' })}`).then(r => r.json())

/** 주간 정산서 생성 */
export const generateSettlement = (payload) =>
  api('/api/settlements/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }).then(r => r.json())

/** 월 전체 주차 생성 */
export const generateMonth = (payload) =>
  api(`/api/settlements/generate-month?${qs({
    hotelId: payload.hotelId,
    month: payload.month,
    bankCode: payload.bankCode,
    accountNo: payload.accountNo,
    holderName: payload.holderName,
  })}`, {
    method: 'POST',
  }).then(r => r.json())

/** 데모 주간 생성 */
export const generateDemoWeekly = () =>
  api('/api/settlements/generate-demo-weekly', { method: 'POST' }).then(r => r.json())

/** 정산 확정 */
export const settleStatement = (id) =>
  api(`/api/settlements/${id}/settle`, { method: 'POST' }).then(r => r.ok)

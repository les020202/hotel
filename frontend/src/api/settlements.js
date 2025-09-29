// src/api/settlements.js
import api from './auth'

// 요약
export const fetchSummaryByHotel = (params) =>
  api.get('/settlements/summary', { params }).then(r => r.data)

// 상세
export const fetchSettlementItems = (params) =>
  api.get('/settlements/items', { params }).then(r => r.data)

// 정산서 목록
export const fetchStatements = (params) =>
  api.get('/settlements/statements', { params }).then(r => r.data)

// 완료된 정산서 목록
export const fetchSettledStatements = (params) =>
  api.get('/settlements/statements', { params: { ...params, status: 'SETTLED' } }).then(r => r.data)

// 주간 생성
export const generateSettlement = (payload) =>
  api.post('/settlements/generate', payload).then(r => r.data)

// 월간 생성 (RequestParam 방식)
export const generateMonth = (payload) =>
  api.post('/settlements/generate-month', null, { params: payload }).then(r => r.data)

// 데모 주간 생성
export const generateDemoWeekly = () =>
  api.post('/settlements/generate-demo-weekly').then(r => r.data)

// 정산 확정 (void → 성공 여부만 반환)
export const settleStatement = (id) =>
  api.post(`/settlements/${id}/settle`).then(() => true)

// 호텔 검색
export const searchHotels = (keyword) =>
  api.get('/hotels/search', { params: { q: keyword } }).then(r => r.data)

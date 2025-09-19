import api from '@/api/auth' // ✅ 공용 Axios 인스턴스(토큰/리프레시/withCredentials 설정 포함)

/**
 * [고객지원(티켓) API 유틸]
 * - 이 파일은 "내 티켓" 목록/상세/메시지 조회 및 생성용 API를 모아둔 모듈입니다.
 * - 모든 요청은 공용 axios 인스턴스(api)를 사용하여:
 *    1) Authorization 헤더(JWT) 자동 첨부
 *    2) 401 발생 시 refresh 토큰으로 재발급 후 재시도
 *    3) baseURL(/api)·withCredentials 등 공통 옵션 적용
 * - 라우트 규칙(백엔드 스펙 가정):
 *    목록:     GET    /support/tickets
 *    단건:     GET    /support/tickets/{id}
 *    메시지:   GET    /support/tickets/{id}/messages
 *    메시지추가:POST  /support/tickets/{id}/messages
 *    티켓생성: POST   /support/tickets
 */

/**
 * 내 티켓 목록 조회
 * @param {Object} params - 쿼리 파라미터 (예: { page, size, status, q })
 *   - status: 'ALL'은 서버 Enum에 없을 수 있어 전송 시 제거 (400 방지)
 *   - q: 빈 문자열이면 제거하여 불필요한 검색 방지
 * @returns {Promise<import('axios').AxiosResponse>} Axios 응답(백엔드의 페이징 포맷에 따름)
 * @example
 *   getMyTickets({ page:0, size:10, status:'OPEN', q:'환불' })
 */
export function getMyTickets(params = {}) {
  const query = { ...params }
  // 🔹 서버에서 허용하지 않는 'ALL' 값은 제외(예: OPEN/CLOSED/PENDING만 허용)
  if (query.status === 'ALL') delete query.status
  // 🔹 빈 검색어는 전달하지 않음
  if (!query.q) delete query.q
  // GET /support/tickets?page=..&size=..&status=..&q=..
  return api.get('/support/tickets', { params: query })
}

/**
 * 티켓 단건 상세 조회
 * @param {number|string} id - 티켓 ID
 * @returns {Promise<import('axios').AxiosResponse>} Axios 응답
 * @example
 *   getTicket(123)
 */
export function getTicket(id) {
  // GET /support/tickets/{id}
  return api.get(`/support/tickets/${id}`)
}

/**
 * 티켓 메시지(대화) 목록 조회
 * @param {number|string} id - 티켓 ID
 * @returns {Promise<import('axios').AxiosResponse>} Axios 응답 (메시지 배열)
 * @example
 *   getTicketMessages(123)
 *   // 필요 시 백엔드 스펙에 맞춰 page/size 같은 params 확장 가능
 */
export function getTicketMessages(id) {
  // GET /support/tickets/{id}/messages
  return api.get(`/support/tickets/${id}/messages`)
}

/**
 * 티켓에 메시지 추가(답글)
 * @param {number|string} id - 티켓 ID
 * @param {string} content - 메시지 본문(필수)
 * @returns {Promise<import('axios').AxiosResponse>} Axios 응답(생성된 메시지 또는 상태코드)
 * @example
 *   postTicketMessage(123, '추가 자료 전달드립니다.')
 *   // 파일 업로드가 필요하면 FormData 기반의 별도 API를 추가하세요.
 */
export function postTicketMessage(id, content) {
  // POST /support/tickets/{id}/messages  { content }
  return api.post(`/support/tickets/${id}/messages`, { content })
}

/**
 * 새 티켓 생성(문의 등록)
 * @param {Object} payload
 * @param {string} payload.subject - 문의 제목(필수)
 * @param {string} payload.firstMessage - 최초 메시지(필수)
 * @returns {Promise<import('axios').AxiosResponse>} Axios 응답(생성된 티켓 정보)
 * @example
 *   openTicket({ subject:'결제 취소 문의', firstMessage:'주문번호 2025-...' })
 *   // 카테고리/우선순위/첨부 등은 향후 payload 확장으로 대응
 */
export function openTicket({ subject, firstMessage }) {
  // POST /support/tickets  { subject, firstMessage }
  return api.post('/support/tickets', { subject, firstMessage })
}


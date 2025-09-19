// axios 인스턴스는 기존에 쓰는 '@/api/auth'의 default export(api) 재활용
import api from '@/api/auth'

// 내 문의 목록
export function getMyTickets(params) {
  return api.get('/support/tickets/my', { params })
}

// 단건 조회
export function getTicket(id) {
  return api.get(`/support/tickets/${id}`)
}

// 메시지 목록(스레드)
export function getTicketMessages(id) {
  // 백엔드에서 ticket 조회 시 messages를 함께 내려주면 이 API는 생략 가능
  return api.get(`/support/tickets/${id}/messages`)
}

// 사용자 메시지 등록(답글)
export function postTicketMessage(id, content) {
  return api.post(`/support/tickets/${id}/messages`, { content })
}

// 티켓 종료(사용자 측)
export function closeTicket(id) {
  return api.post(`/support/tickets/${id}/close`)
}


export function openTicket({ subject, firstMessage }) {
  return api.post('/api/support/tickets', { subject, firstMessage })
}


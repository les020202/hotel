// frontend/src/api/adminSupport.js
import { api } from '@/router'

/** 서버 응답을 공통 형태로 맞춤 */
function normalizePage(json) {
  // 배열이 바로 오면(페이징 없이)도 지원
  if (Array.isArray(json)) {
    return { list: json, page: 0, size: json.length, total: json.length }
  }
  // PagedResponse (items/limit/offset/total...) or Spring Page (content/number/size/totalElements...)
  const list = json.items || json.content || []
  const page = json.page ?? json.number ?? 0
  const size = json.size ?? json.limit ?? list.length ?? 0
  const total = json.total ?? json.totalElements ?? list.length ?? 0
  return { list, page, size, total }
}

// ---------- 공지 ----------
export async function listNotices({ page = 0, size = 100, pinned } = {}) {
  const qs = new URLSearchParams({ page, size })
  if (typeof pinned === 'boolean') qs.set('pinned', String(pinned))
  const res = await api(`/api/admin/support/notices?${qs.toString()}`)
  const json = await res.json()
  return normalizePage(json)
}
export async function createNotice(payload) {
  const res = await api('/api/admin/support/notices', {
    method: 'POST', body: JSON.stringify(payload)
  })
  return res.ok
}
export async function updateNotice(id, payload) {
  const res = await api(`/api/admin/support/notices/${id}`, {
    method: 'PUT', body: JSON.stringify(payload)
  })
  return res.ok
}
export async function deleteNotice(id) {
  const res = await api(`/api/admin/support/notices/${id}`, { method: 'DELETE' })
  return res.ok
}

// ---------- FAQ ----------
export async function listFaqs({ page = 0, size = 200, category } = {}) {
  const qs = new URLSearchParams({ page, size })
  if (category) qs.set('category', category)
  const res = await api(`/api/admin/support/faqs?${qs.toString()}`)
  const json = await res.json()
  return normalizePage(json)
}
export async function createFaq(payload) {
  const res = await api('/api/admin/support/faqs', {
    method: 'POST', body: JSON.stringify(payload)
  })
  return res.ok
}
export async function updateFaq(id, payload) {
  const res = await api(`/api/admin/support/faqs/${id}`, {
    method: 'PUT', body: JSON.stringify(payload)
  })
  return res.ok
}
export async function deleteFaq(id) {
  const res = await api(`/api/admin/support/faqs/${id}`, { method:'DELETE' })
  return res.ok
}

// ---------- 티켓/문의 ----------
export async function listTickets({ page = 0, size = 50, status, q } = {}) {
  const qs = new URLSearchParams({ page, size })
  if (status) qs.set('status', status)
  if (q) qs.set('q', q)
  const res = await api(`/api/admin/support/tickets?${qs.toString()}`)
  const json = await res.json()
  return normalizePage(json)
}
export async function getTicket(id) {
  const res = await api(`/api/admin/support/tickets/${id}`)
  return await res.json() // {ticket:{...}, messages:[...]} 형태 유지
}
export async function deleteTicket(id) {
  const res = await api(`/api/admin/support/tickets/${id}`, { method:'DELETE' })
  return res.ok
}
export async function replyTicket(ticketId, content) {
  const res = await api(`/api/admin/support/tickets/${ticketId}/reply`, {
    method:'POST', body: JSON.stringify({ content })
  })
  return res.ok
}
export async function updateMessage(msgId, content) {
  const res = await api(`/api/admin/support/messages/${msgId}`, {
    method:'PUT', body: JSON.stringify({ content })
  })
  return res.ok
}
export async function deleteMessage(msgId) {
  const res = await api(`/api/admin/support/messages/${msgId}`, { method:'DELETE' })
  return res.ok
}
// src/api/adminUsers.js
import { get, put, del } from '@/api/_http'

// 검색 (q/role/status — 빈 값은 전달하지 않음)
export function searchUsers({ q = '', role = '', status = '' } = {}) {
  const p = new URLSearchParams()
  if (q) p.set('q', q)
  if (role) p.set('role', role)
  if (status) p.set('status', status)
  return get(`/admin/users?${p.toString()}`)
}

export function updateUserRole(id, role) {
  return put(`/admin/users/${id}/role`, { role })
}

export function updateUserStatus(id, status) {
  return put(`/admin/users/${id}/status`, { status })
}

export function deleteUser(id) {
  return del(`/admin/users/${id}`)
}

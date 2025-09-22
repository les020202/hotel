// src/api/_http.js
// - VITE_API_BASE 가 있으면 그걸 쓰고(예: http://172.16.15.59:8888/api)
// - 없으면 '/api' 로 호출 → Vite proxy가 백엔드로 포워딩
const API_BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/+$/, '')

// /api 중복 방지 + 절대 URL도 허용
function build(url) {
  if (/^https?:\/\//i.test(url)) return url // absolute passed
  const cleanPath = String(url || '').replace(/^\/+/, '') // strip leading /
  // base가 /api 로 끝나고, path가 'api/...' 로 시작하면 앞의 api 한번만 쓰기
  const path = API_BASE.endsWith('/api') && cleanPath.startsWith('api/')
    ? cleanPath.slice(4)
    : cleanPath
  return `${API_BASE}/${path}`
}

function authHeaders() {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function handle(res, method, url) {
  if (res.ok) {
    if (res.status === 204) return
    const ct = res.headers.get('content-type') || ''
    return ct.includes('application/json') ? res.json() : res.text()
  }
  // 에러 본문 최대한 돌려주기
  let body = ''
  try { body = await res.text() } catch {}
  const message = body || res.statusText || 'Request failed'
  throw new Error(
    JSON.stringify({ status: res.status, method, url, error: message })
  )
}

/* -------- HTTP wrappers -------- */
export async function get(url) {
  const r = await fetch(build(url), {
    method: 'GET',
    headers: { ...authHeaders() },
    credentials: 'include',
  })
  return handle(r, 'GET', url)
}

export async function post(url, body) {
  const r = await fetch(build(url), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    credentials: 'include',
    body: JSON.stringify(body ?? {}),
  })
  return handle(r, 'POST', url)
}

export async function put(url, body) {
  const r = await fetch(build(url), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    credentials: 'include',
    body: JSON.stringify(body ?? {}),
  })
  return handle(r, 'PUT', url)
}

export async function del(url) {
  const r = await fetch(build(url), {
    method: 'DELETE',
    headers: { ...authHeaders() },
    credentials: 'include',
  })
  return handle(r, 'DELETE', url)
}

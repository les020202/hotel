// src/api/_http.js
// - VITE_API_BASE 가 있으면 그걸 쓰고(예: http://172.16.15.59:8888/api)
// - 없으면 '/api' 로 호출 → Vite proxy가 백엔드로 포워딩
const API_BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/+$/, '')

// /api 중복 방지 + 절대 URL도 허용
function build (url) {
  if (/^https?:\/\//i.test(url)) return url // absolute passed
  const cleanPath = String(url || '').replace(/^\/+/, '') // strip leading /
  const path = API_BASE.endsWith('/api') && cleanPath.startsWith('api/')
    ? cleanPath.slice(4)
    : cleanPath
  return `${API_BASE}/${path}`
}

function authHeaders () {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/** 에러 본문/헤더에서 errorId 추출 + 메시지 구성 */
async function parseErrorPayload (res) {
  const ct = res.headers.get('content-type') || ''
  let raw = ''
  try { raw = await res.text() } catch {}

  let json
  if (ct.includes('application/json')) {
    try { json = raw ? JSON.parse(raw) : undefined } catch {}
  }

  const errorId =
    (json && (json.errorId || json.errId || json.id)) ||
    res.headers.get('x-error-id') ||
    ''

  const message =
    (json && (json.message || json.error || json.detail)) ||
    raw ||
    res.statusText ||
    'Request failed'

  return { status: res.status, message, errorId, body: json ?? raw }
}

/**
 * 공용 응답 처리: 성공은 JSON/text 반환, 실패는 에러 파싱 후
 * - 기본은 에러 페이지로 라우팅
 * - options.suppressRedirect === true면 라우팅 안 하고 throw
 */
async function handle (res, method, url, options = {}) {
  if (res.ok) {
    if (res.status === 204) return
    const ct = res.headers.get('content-type') || ''
    return ct.includes('application/json') ? res.json() : res.text()
  }

  const { status, message, errorId, body } = await parseErrorPayload(res)

  // 필요 시 자동 라우팅 (기본 ON)
  const shouldRedirect =
    options.suppressRedirect !== true &&
    [400,401,403,404,405,408,413,429,500,502,503,504].includes(status)

  if (shouldRedirect) {
    // 동적 import로 순환참조 방지
    try {
      const { router } = await import('@/router')
      router.push({
        name: 'Error',
        params: { status },
        query: errorId ? { errid: errorId } : {}
      })
    } catch (e) {
      // 라우터 가져오지 못해도 throw는 계속 진행
      console.warn('Failed to route to Error page:', e)
    }
  }

  // 호출부에서 잡아 디테일 처리할 수 있도록 표준화된 Error 던짐
  const payload = { status, method, url, errorId, error: message, body }
  const err = new Error(JSON.stringify(payload))
  err.payload = payload
  throw err
}

/* -------- HTTP wrappers (옵션 추가: { suppressRedirect?: boolean }) -------- */
export async function get (url, options = {}) {
  const r = await fetch(build(url), {
    method: 'GET',
    headers: { ...authHeaders() },
    credentials: 'include'
  })
  return handle(r, 'GET', url, options)
}

export async function post (url, body, options = {}) {
  const r = await fetch(build(url), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    credentials: 'include',
    body: JSON.stringify(body ?? {})
  })
  return handle(r, 'POST', url, options)
}

export async function put (url, body, options = {}) {
  const r = await fetch(build(url), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    credentials: 'include',
    body: JSON.stringify(body ?? {})
  })
  return handle(r, 'PUT', url, options)
}

export async function del (url, options = {}) {
  const r = await fetch(build(url), {
    method: 'DELETE',
    headers: { ...authHeaders() },
    credentials: 'include'
  })
  return handle(r, 'DELETE', url, options)
}

export async function upload (url, formData, options = {}) {
  const fd = formData instanceof FormData ? formData : new FormData()
  if (!(formData instanceof FormData) && formData) {
    Object.entries(formData).forEach(([k, v]) => fd.append(k, v))
  }
  const r = await fetch(build(url), {
    method: 'POST',
    headers: { ...authHeaders() }, // 'Content-Type' 넣지 않기!
    credentials: 'include',
    body: fd
  })
  return handle(r, 'POST', url, options)
}

export async function postMultipart (url, formData, options = {}) {
  const r = await fetch(build(url), {
    method: 'POST',
    headers: { ...authHeaders() },
    credentials: 'include',
    body: formData
  })
  return handle(r, 'POST', url, options)
}

export const http = { get, post, put, del, upload, postMultipart }
export default http

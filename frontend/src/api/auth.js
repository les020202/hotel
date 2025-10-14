// src/api/auth.js
import axios from 'axios'
import router from '@/router'

// === Axios 인스턴스 생성 ===
// VITE_API_BASE 가 없으면 기본으로 /api 사용 (proxy 통해 8888로 전달)
const baseURL = import.meta.env.VITE_API_BASE || '/api'

const api = axios.create({
  baseURL,
  withCredentials: true, // ✅ refresh 쿠키 사용
})

// === 로컬 스토리지 키 ===
const KEY_PRIMARY = 'token'
const KEY_LEGACY = 'accessToken'

/*
  Note:
  - login(payload) 는 payload에 recaptchaToken 필드를 포함할 수 있도록 설계되어 있습니다.
    ex) login({ loginId, password, recaptchaToken })
  - backend AuthController.login 에서 recaptchaToken을 검사하도록 구현되어 있으면
    추가 수정 없이 바로 동작합니다.
*/

let accessToken =
  localStorage.getItem(KEY_PRIMARY) ||
  localStorage.getItem(KEY_LEGACY) ||
  null

let userProfile = JSON.parse(localStorage.getItem('userProfile') || 'null')

// === 상태 helpers ===
export function getUser() {
  return userProfile
}
export function isLoggedIn() {
  return !!(
    accessToken ||
    localStorage.getItem(KEY_PRIMARY) ||
    localStorage.getItem(KEY_LEGACY)
  )
}
function emitAuthChanged() {
  try {
    window.dispatchEvent(
      new CustomEvent('auth:changed', {
        detail: { user: userProfile, token: accessToken },
      }),
    )
  } catch (_) {}
}
export function onAuthChanged(handler) {
  window.addEventListener('auth:changed', handler)
  return () => window.removeEventListener('auth:changed', handler)
}
export function setAuth(at, user) {
  accessToken = at || null
  userProfile = user || null

  if (at) {
    localStorage.setItem(KEY_PRIMARY, at)
    localStorage.setItem(KEY_LEGACY, at)
  } else {
    localStorage.removeItem(KEY_PRIMARY)
    localStorage.removeItem(KEY_LEGACY)
  }

  if (user) localStorage.setItem('userProfile', JSON.stringify(user))
  else localStorage.removeItem('userProfile')

  emitAuthChanged()
}

// (옵션) 공통 에러 메시지 추출기 — 일부 API에서 사용
function getErrorMessage(e) {
  const r = e?.response
  return (
    r?.data?.error ||
    r?.data?.message ||
    (typeof r?.data === 'string' ? r.data : '') ||
    e.message ||
    '요청 실패'
  )
}

// === 로그인/로그아웃 ===
// ✅ 로그인 호출은 payload 를 그대로 전송합니다.
//    payload에 recaptchaToken을 포함하면 백엔드에서 검증 후 처리합니다.
export async function login(payload) {
  try {
    const { data } = await api.post('/auth/login', payload, { withCredentials: true })
    // data 예시: { token, attempts?, locked? ... }
    return data
  } catch (e) {
    // 서버 JSON 그대로 던지기 → LoginView에서 { error, attempts, locked } 사용
    if (e?.response?.data) throw e.response.data
    // 네트워크/기타 예외 최소 형태
    throw { error: 'UNKNOWN', attempts: 0, locked: false }
  }
}

export async function logout() {
  try {
    await api.post('/auth/logout', {}, { withCredentials: true })
  } catch {}
  setAuth(null, null)
  router.push('/login')
}

// === Axios 인터셉터 ===
api.interceptors.request.use((config) => {
  const latest =
    localStorage.getItem(KEY_PRIMARY) ||
    localStorage.getItem(KEY_LEGACY) ||
    accessToken
  if (latest) {
    accessToken = latest
    config.headers.Authorization = `Bearer ${latest}`
  }
  return config
})

let refreshing = false
let waiters = []
function waitRefresh() {
  return new Promise((res) => waiters.push(res))
}
function resume() {
  waiters.forEach((r) => r())
  waiters = []
}

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error?.config
    const status = error?.response?.status
    if (!original || original._retry) return Promise.reject(error)

    // ✅ 두 번째 파일 정책 반영: /auth/ 경로는 refresh 시도하지 않음
    const isAuthPath = typeof original.url === 'string' && original.url.includes('/auth/')

    if (status === 401 && !isAuthPath) {
      if (refreshing) {
        await waitRefresh()
        return api(original)
      }
      refreshing = true
      original._retry = true
      try {
        const resp = await axios.post(
          `${baseURL}/auth/refresh`,
          {},
          { withCredentials: true }
        )
        const newToken = resp.data?.token || resp.data?.accessToken
        if (newToken) {
          setAuth(newToken, userProfile)
          return api(original)
        } else {
          setAuth(null, null)
          return Promise.reject(error)
        }
      } catch (e) {
        setAuth(null, null)
        return Promise.reject(e)
      } finally {
        refreshing = false
        resume()
      }
    }
    return Promise.reject(error)
  },
)

// === 공개 API ===
// 사용자 관련
export const getMe = () => api.get('/users/me').then((r) => r.data)
export const checkUsername = (loginId) =>
  api.get('/auth/check-username', { params: { loginId } }).then((r) => r.data)
export const checkEmail = (email) =>
  api.get('/auth/check-email', { params: { email } }).then((r) => r.data)

export const signup = async (payload, verificationCode) => {
  try {
    const { data } = await api.post(
      `/auth/signup?verificationCode=${encodeURIComponent(String(verificationCode ?? ''))}`,
      payload,
      { headers: { 'Content-Type': 'application/json' } }
    )
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

export const updateMyEmail = async ({ email, verificationCode }) => {
  try {
    const { data } = await api.patch('/users/me/email', { email, verificationCode })
  return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 이메일 인증 관련
export const sendEmailCode = async (email) =>
  (await api.post('/auth/email/send', { email })).data
export const verifyEmailCode = async (email, code) =>
  (await api.post('/auth/email/verify', { email, code })).data

// 비밀번호 재설정
export const resetPassword = async (email, code, newPassword) =>
  (await api.post('/auth/reset-password', { email, code, newPassword })).data

// 프로필/커버 이미지 관련
export const setProfileTemplate = async (template) => {
  try {
    return (await api.put('/users/me/profile/template', { template })).data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}
export const setCoverTemplate = async (template) => {
  try {
    return (await api.put('/users/me/cover/template', { template })).data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

export const uploadProfileImage = async (file) => {
  const fd = new FormData()
  fd.append('file', file)
  try {
    return (await api.post('/users/me/profile/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })).data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}
export const uploadCoverImage = async (file) => {
  const fd = new FormData()
  fd.append('file', file)
  try {
    return (await api.post('/users/me/cover/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })).data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 호텔 관련
export async function hotelsSearch(params) {
  return (await api.get('/hotels/search', { params })).data
}
export async function hotelDetail(id) {
  return (await api.get(`/hotels/${id}`)).data
}
export async function fetchMinPrices(ids) {
  const params = { ids: ids.join(',') }
  return (await api.get('/hotels/min-price', { params })).data || {}
}
export async function fetchRegions(excludeJeju = true) {
  return (await api.get('/hotels/regions', { params: { excludeJeju } })).data
}

// 마케팅 구독
export async function subscribeEmail(email) {
  return (await api.post('/marketing/subscribe', { email })).data
}

// 위시리스트
export async function getWishlist() {
  return (await api.get('/my/wishlist')).data
}
export async function addWishlist(hotelId) {
  return (await api.post('/my/wishlist', { hotelId })).data
}
export async function removeWishlist(hotelId) {
  return (await api.delete(`/my/wishlist/${hotelId}`)).data
}

export default api

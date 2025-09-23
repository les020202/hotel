// src/api/auth.js
import axios from 'axios'
import router from '@/router'

// === Axios 인스턴스 생성 ===
// VITE_API_BASE 가 없으면 기본으로 http://localhost:8888/api 사용
const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8888/api'

const api = axios.create({
  baseURL,
  withCredentials: true, // ✅ refresh 쿠키 사용
})

// === 라우터는 'token'을 사용 → 1순위 'token', 호환용으로 'accessToken'도 함께 사용
const KEY_PRIMARY = 'token'
const KEY_LEGACY  = 'accessToken'

// === 인증 상태 (메모리 + 로컬스토리지 동기화) ===
// 우선순위: token → accessToken
let accessToken =
  localStorage.getItem(KEY_PRIMARY) ||
  localStorage.getItem(KEY_LEGACY) ||
  null

let userProfile = JSON.parse(localStorage.getItem('userProfile') || 'null')

export function getUser() {
  return userProfile
}

export function isLoggedIn() {
  // 어느 키든 존재하면 로그인 상태로 간주
  return !!(
    accessToken ||
    localStorage.getItem(KEY_PRIMARY) ||
    localStorage.getItem(KEY_LEGACY)
  )
}

/** 👉 로그인 상태 변경을 앱 전역으로 브로드캐스트 */
function emitAuthChanged() {
  try {
    window.dispatchEvent(
      new CustomEvent('auth:changed', {
        detail: { user: userProfile, token: accessToken },
      })
    )
  } catch (_) {
    /* no-op */
  }
}

/** 앱 어디서든 구독 가능 (반환값: 해제 함수) */
export function onAuthChanged(handler) {
  window.addEventListener('auth:changed', handler)
  return () => window.removeEventListener('auth:changed', handler)
}

/** 토큰/유저 저장 + 브로드캐스트
 * - 라우터와 일치하도록 'token' 키를 1순위로 저장
 * - 과거 코드 호환을 위해 'accessToken'에도 동일 값 동기화
 */
export function setAuth(at, user) {
  accessToken = at || null
  userProfile = user || null

  if (at) {
    localStorage.setItem(KEY_PRIMARY, at)   // 'token'
    localStorage.setItem(KEY_LEGACY, at)    // 'accessToken' (호환)
  } else {
    localStorage.removeItem(KEY_PRIMARY)
    localStorage.removeItem(KEY_LEGACY)
  }

  if (user) localStorage.setItem('userProfile', JSON.stringify(user))
  else localStorage.removeItem('userProfile')

  emitAuthChanged()
}

/** 로그인 (서버는 {loginId, password} 를 받습니다) */
export async function login(payloadOrId, maybePassword) {
  // 1) 인자 정규화: login({ loginId, password }) 또는 login(loginId, password)
  let body
  if (typeof payloadOrId === 'object' && payloadOrId !== null) {
    body = {
      loginId: payloadOrId.loginId ?? payloadOrId.username ?? payloadOrId.id,
      password: payloadOrId.password,
    }
  } else {
    body = { loginId: payloadOrId, password: maybePassword }
  }

  if (!body?.loginId || !body?.password) {
    throw new Error('login() requires loginId and password')
  }

  try {
    // 2) 로그인 요청 (리프레시 쿠키 수신을 위해 withCredentials 유지)
    const { data } = await api.post('/auth/login', body, { withCredentials: true })

    // 서버가 token 또는 accessToken을 줄 수 있으므로 모두 대응
    const token = data?.accessToken ?? data?.token
    if (!token) throw new Error('No access token in response')

    // 3) 토큰 저장 (유저 정보는 잠시 null → /users/me로 채움)
    setAuth(token, null)

    // 4) 프로필 불러와 state 갱신
    try {
      const me = await getMe()
      setAuth(token, me)
    } catch (_) {
      // 프로필 실패해도 로그인은 성공으로 간주
    }

    return data
  } catch (err) {
    const resp = err?.response
    if (resp?.data) {
      const e = new Error(resp.data?.error || 'LOGIN_FAILED')
      e.attempts = resp.data?.attempts
      e.locked = resp.data?.locked
      throw e
    }
    throw err
  }
}

/** 로그아웃 */
export async function logout() {
  try {
    await api.post('/auth/logout', {}, { withCredentials: true }) // ✅ 쿠키 삭제 요청
  } catch {}
  setAuth(null, null)
  router.push('/login')
}

/** ----------------------------------------------------------------------------
 * Axios 인터셉터
 * ---------------------------------------------------------------------------*/
api.interceptors.request.use((config) => {
  // 다른 탭에서 갱신된 경우 반영: 최신 토큰 재확인
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

// 401 처리 & 동시 요청 큐
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

    if (status === 401) {
      // 이미 갱신 중이면 큐에 대기
      if (refreshing) {
        await waitRefresh()
        return api(original)
      }

      refreshing = true
      original._retry = true
      try {
        // ✅ refresh 쿠키를 사용하므로 withCredentials 필요
        const resp = await axios.post(
          `${baseURL}/auth/refresh`,
          {},
          { withCredentials: true }
        )
        const newToken = resp.data?.token || resp.data?.accessToken
        if (newToken) {
          setAuth(newToken, userProfile) // 토큰만 갱신
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
  }
)

/** ----------------------------------------------------------------------------
 * API helpers
 * ---------------------------------------------------------------------------*/
// 호텔 검색
export async function hotelsSearch(params) {
  return (await api.get('/hotels/search', { params })).data
}

// 호텔 상세 조회
export async function hotelDetail(id) {
  return (await api.get(`/hotels/${id}`)).data
}

// 호텔 최저가 묶음 조회
export async function fetchMinPrices(ids) {
  const params = { ids: ids.join(',') }
  const { data } = await api.get('/hotels/min-price', { params })
  return data || {}
}

// 지역 목록 (distinct)
export async function fetchRegions(excludeJeju = true) {
  return (await api.get('/hotels/regions', { params: { excludeJeju } })).data
}

// 이메일 마케팅 구독
export async function subscribeEmail(email) {
  return (await api.post('/marketing/subscribe', { email })).data
}

// 위시리스트 관련 API
export async function getWishlist() {
  return (await api.get('/my/wishlist')).data
}

export async function addWishlist(hotelId) {
  return (await api.post('/my/wishlist', { hotelId })).data
}

export async function removeWishlist(hotelId) {
  return (await api.delete(`/my/wishlist/${hotelId}`)).data
}

// === 이메일 관련 ===
export async function checkEmail(email) {
  return (await api.get('/auth/check-email', { params: { email } })).data
}

export async function sendEmailCode(email) {
  return (await api.post('/auth/email/send', { email })).data
}

export async function verifyEmailCode(email, code) {
  return (await api.post('/auth/email/verify', { email, code })).data
}

// === 비밀번호 관련 ===
export async function resetPassword(email, code, newPassword) {
  return (await api.post('/auth/reset-password', { email, code, newPassword })).data
}

// === 프로필 관련 ===
export const setProfileTemplate = (template) =>
  api.put('/users/me/profile/template', { template }).then((r) => r.data)

export const setCoverTemplate = (template) =>
  api.put('/users/me/cover/template', { template }).then((r) => r.data)

export const uploadProfileImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return api
    .post('/users/me/profile/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data)
}

export const uploadCoverImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return api
    .post('/users/me/cover/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data)
}

// === 사용자 정보 ===
export const getMe = () => api.get('/users/me').then((r) => r.data)

export const checkUsername = (loginId) =>
  api.get('/auth/check-username', { params: { loginId } }).then((r) => r.data)

export const signup = (payload, verificationCode) =>
  api
    .post(`/auth/signup?verificationCode=${encodeURIComponent(String(verificationCode ?? ''))}`, payload)
    .then((r) => r.data)

export const updateMyEmail = ({ email, verificationCode }) =>
  api.patch('/users/me/email', { email, verificationCode }).then((r) => r.data)

export default api

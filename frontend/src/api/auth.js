import axios from 'axios'
import router from '@/router'

// === Axios 인스턴스 생성 ===
const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8888/api'

const api = axios.create({
  baseURL,
  withCredentials: true, // refresh 쿠키 사용
})

// === 인증 상태 (메모리 + 로컬스토리지 동기화) ===
let accessToken = localStorage.getItem('accessToken') || null
let userProfile = JSON.parse(localStorage.getItem('userProfile') || 'null')

export function getUser() {
  return userProfile
}

export function isLoggedIn() {
  return !!accessToken
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
    /* no-op (SSR 등) */
  }
}

/** 앱 어디서든 구독 가능 (반환값: 해제 함수) */
export function onAuthChanged(handler) {
  window.addEventListener('auth:changed', handler)
  return () => window.removeEventListener('auth:changed', handler)
}

/** 토큰/유저 저장 + 브로드캐스트 */
export function setAuth(at, user) {
  accessToken = at
  userProfile = user || null
  if (at) localStorage.setItem('accessToken', at)
  else localStorage.removeItem('accessToken')

  if (user) localStorage.setItem('userProfile', JSON.stringify(user))
  else localStorage.removeItem('userProfile')

  emitAuthChanged()
}

/** 로그인 */
export async function login(username, password) {
  const { data } = await api.post('/auth/login', { username, password })
  setAuth(data.accessToken, data.user)
  return data
}

/** 로그아웃 */
export async function logout() {
  try {
    await api.post('/auth/logout', {})
  } catch {}
  setAuth(null, null) // 이벤트도 함께 발생
  router.push('/login')
}

/** ----------------------------------------------------------------------------
 * Axios 인터셉터
 * ---------------------------------------------------------------------------*/
api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
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
        const resp = await axios.post(
          `${baseURL}/auth/refresh`,
          {},
          { withCredentials: true }
        )
        const newToken = resp.data?.accessToken
        if (newToken) {
          // 토큰만 갱신, userProfile은 유지
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
// 이메일 중복 체크
export async function checkEmail(email) {
  return (await api.get('/auth/check-email', { params: { email } })).data
}

// 이메일 인증 코드 전송
export async function sendEmailCode(email) {
  return (await api.post('/auth/email/send', { email })).data
}

// 이메일 인증 코드 검증
export async function verifyEmailCode(email, code) {
  return (await api.post('/auth/email/verify', { email, code })).data
}

// === 비밀번호 관련 ===
// 새 비밀번호 재설정
export async function resetPassword(email, code, newPassword) {
  return (await api.post('/auth/reset-password', { email, code, newPassword })).data
}

// === 프로필 관련 ===
// 프로필 템플릿 변경
export const setProfileTemplate = (template) =>
  api.put('/users/me/profile/template', { template }).then((r) => r.data)

// 커버 템플릿 변경
export const setCoverTemplate = (template) =>
  api.put('/users/me/cover/template', { template }).then((r) => r.data)

// 프로필 이미지 업로드 (multipart/form-data)
export const uploadProfileImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return api
    .post('/users/me/profile/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data)
}

// 커버 이미지 업로드 (multipart/form-data)
export const uploadCoverImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return api
    .post('/users/me/cover/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data)
}

// === 사용자 정보 ===
// 내 정보 (마이페이지용)
export const getMe = () => api.get('/users/me').then((r) => r.data)

// 사용자 아이디 중복 체크
export const checkUsername = (loginId) =>
  api.get('/auth/check-username', { params: { loginId } }).then((r) => r.data)

// 회원가입
export const signup = (payload, verificationCode) =>
  api
    .post(`/auth/signup?verificationCode=${encodeURIComponent(String(verificationCode ?? ''))}`, payload)
    .then((r) => r.data)

// === 이메일 변경 ===
// 내 이메일 변경 (인증코드 필요)
export const updateMyEmail = ({ email, verificationCode }) =>
  api.patch('/users/me/email', { email, verificationCode }).then((r) => r.data)

export default api

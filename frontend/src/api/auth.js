// src/api/auth.js
import axios from 'axios'
import router from '@/router'

// === Axios 인스턴스 ===
// baseURL: .env.development 의 VITE_API_BASE=/api 혹은 http://localhost:8888/api 등
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  withCredentials: true, // refresh token 등 쿠키 사용 시 필요
})

// === 공통 에러 메시지 추출기 ===
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

// === 요청 인터셉터: JWT 자동 첨부 ===
api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// === 응답 인터셉터: 401 → refresh → 대기 중 요청 재시도 ===
let isRefreshing = false
let subscribers = []
const onRefreshed = (token) => { subscribers.forEach((cb) => cb(token)); subscribers = [] }
const addSubscriber = (cb) => subscribers.push(cb)

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const { config, response } = err
    if (!response) return Promise.reject(err)

    const isAuthPath = config.url?.includes('/auth/')
    if (response.status === 401 && !config._retry && !isAuthPath) {
      config._retry = true

      // 이미 다른 refresh가 진행 중이면 refresh 완료까지 대기
      if (isRefreshing) {
        return new Promise((resolve) => {
          addSubscriber((newToken) => {
            config.headers.Authorization = 'Bearer ' + newToken
            resolve(api(config))
          })
        })
      }

      // refresh 시도
      isRefreshing = true
      try {
        const { data } = await api.post('/auth/refresh')
        const newToken = data.token
        localStorage.setItem('token', newToken)
        onRefreshed(newToken)

        config.headers.Authorization = 'Bearer ' + newToken
        return api(config)
      } catch (e) {
        // refresh 실패 → 로그인 재유도
        localStorage.removeItem('token')
        router.push('/login')
        return Promise.reject(new Error(getErrorMessage(e)))
      } finally {
        isRefreshing = false
      }
    }

    // 그 외 에러는 메시지 정리해서 던짐
    return Promise.reject(new Error(getErrorMessage(err)))
  }
)

// ===================== 공개 API =====================

// 아이디 중복 체크
export const checkUsername = (loginId) =>
  api.get('/auth/check-username', { params: { loginId } }).then((r) => r.data)

// 이메일 중복 체크
export const checkEmail = (email) =>
  api.get('/auth/check-email', { params: { email } }).then((r) => r.data)

// 회원가입 (이메일 인증코드 쿼리스트링로 전달)
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

// 로그인
export const login = async (payload) => {
  try {
    const { data } = await api.post('/auth/login', payload)
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 로그아웃
export const logout = () => api.post('/auth/logout').then((r) => r.data)

// 내 정보 (마이페이지)
// 서버가 /me만 제공한다면 아래 라인을 api.get('/me')로 교체
export const getMe = () => api.get('/users/me').then((r) => r.data)

// ===== 이메일 인증 관련 =====

// 인증 코드 전송
export const sendEmailCode = async (email) => {
  try {
    const { data } = await api.post('/auth/email/send', { email })
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 인증 코드 검증
export const verifyEmailCode = async (email, code) => {
  try {
    const { data } = await api.post('/auth/email/verify', { email, code })
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 이메일 변경 (인증코드 필요)
export const updateMyEmail = async ({ email, verificationCode }) => {
  try {
    const { data } = await api.patch('/users/me/email', { email, verificationCode })
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 비밀번호 재설정
export const resetPassword = async (email, code, newPassword) => {
  try {
    const { data } = await api.post('/auth/reset-password', { email, code, newPassword })
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// ===== 프로필/커버 & 이미지 업로드 =====

// 프로필 템플릿 변경
export const setProfileTemplate = async (template) => {
  try {
    const { data } = await api.put('/users/me/profile/template', { template })
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 커버 템플릿 변경
export const setCoverTemplate = async (template) => {
  try {
    const { data } = await api.put('/users/me/cover/template', { template })
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 프로필 이미지 업로드 (multipart/form-data)
export const uploadProfileImage = async (file) => {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const { data } = await api.post('/users/me/profile/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

// 커버 이미지 업로드 (multipart/form-data)
export const uploadCoverImage = async (file) => {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const { data } = await api.post('/users/me/cover/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  } catch (e) {
    throw new Error(getErrorMessage(e))
  }
}

export default api

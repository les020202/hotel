import axios from 'axios'
import router from '@/router'


// === Axios 인스턴스 생성 ===
// ⚠️ baseURL 은 반드시 .env.development 의 VITE_API_BASE 로만 지정
// (예: VITE_API_BASE=/api  또는  VITE_API_BASE=http://localhost:8888/api)
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api', // API 기본 URL (환경변수에서 읽음)
  withCredentials: true                   // 쿠키 포함 여부 (refresh token 등 사용 가능)
})

// ── 요청 인터셉터: 토큰 첨부
api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token') // 로컬스토리지에 저장된 JWT
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// ── 응답 인터셉터: 401 → refresh → 재시도

let isRefreshing = false   // refresh 요청 중 여부
let subscribers = []       // refresh 대기중인 요청 리스트

const onRefreshed = (token) => { 
  subscribers.forEach(cb => cb(token)) // refresh 후 토큰 전달
  subscribers = [] 
}
const addSubscriber = (cb) => subscribers.push(cb)

api.interceptors.response.use(
  res => res, // 정상 응답 그대로 반환
  async err => {
    const { config, response } = err
    if (!response) return Promise.reject(err)


    const isAuthPath = config.url?.includes('/auth/')
    if (response.status === 401 && !config._retry && !isAuthPath) {
      config._retry = true
      if (isRefreshing) {

        // refresh 완료 기다렸다가 요청 재시도
        return new Promise(resolve => {
          addSubscriber((newToken) => {
            config.headers.Authorization = 'Bearer ' + newToken
            resolve(api(config))
          })
        })
      }
      isRefreshing = true
      try {
        // refresh 실패 → 토큰 제거 후 로그인 페이지로 이동
        const { data } = await api.post('/auth/refresh')
        localStorage.setItem('token', data.token)
        onRefreshed(data.token)
        config.headers.Authorization = 'Bearer ' + data.token
        return api(config)
      } catch (e) {
        localStorage.removeItem('token')
        router.push('/login')
        return Promise.reject(e)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(err)
  }
)



// === 공개 API ===

// 사용자 아이디 중복 체크
export const checkUsername = (loginId) =>
  api.get('/auth/check-username', { params: { loginId } }).then(r => r.data)

// 사용자 이메일 중복 체크
export const checkEmail = (email) =>
  api.get('/auth/check-email', { params: { email } }).then(r => r.data)

// 회원가입

export const signup = (payload, verificationCode) =>
  api.post(`/auth/signup?verificationCode=${encodeURIComponent(String(verificationCode ?? ''))}`, payload)
     .then(r => r.data)

// 로그인
export const login = (payload) =>
  api.post('/auth/login', payload).then(r => r.data)

// 로그아웃
export const logout = () =>
  api.post('/auth/logout').then(r => r.data)

// ✅ 내 정보 (마이페이지용)
export const getMe = () =>
  api.get('/users/me').then(r => r.data)

// 이메일 인증 코드 전송
export const sendEmailCode = (email) =>
  api.post('/auth/email/send', { email }).then(r => r.data)

// 이메일 인증 코드 검증
export const verifyEmailCode = (email, code) =>
  api.post('/auth/email/verify', { email, code }).then(r => r.data)

// 새 비밀번호 재설정
export const resetPassword = (email, code, newPassword) =>
  api.post('/auth/reset-password', { email, code, newPassword }).then(r => r.data)


// === 이미지(프로필/커버) 관련 ===

// 프로필 템플릿 변경
export const setProfileTemplate = (template) =>
  api.put('/users/me/profile/template', { template }).then(r => r.data)

// 커버 템플릿 변경
export const setCoverTemplate = (template) =>
  api.put('/users/me/cover/template', { template }).then(r => r.data)

// 프로필 이미지 업로드 (multipart/form-data)
export const uploadProfileImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return api.post('/users/me/profile/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
           .then(r => r.data)
}

// 커버 이미지 업로드 (multipart/form-data)
export const uploadCoverImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return api.post('/users/me/cover/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
           .then(r => r.data)
}

export default api

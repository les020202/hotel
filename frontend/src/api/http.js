// src/api/http.js
import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  withCredentials: true, // 세션/쿠키 쓰면 유지
})

// (옵션) 토큰 헤더
// http.interceptors.request.use(cfg => {
//   const token = localStorage.getItem('token')
//   if (token) cfg.headers.Authorization = `Bearer ${token}`
//   return cfg
// })

export default http

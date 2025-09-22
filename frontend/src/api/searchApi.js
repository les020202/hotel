import axios from "axios";

// VITE_API_BASE 예시:
//  - '' (프록시 사용)            -> 요청: /api/...
//  - 'http://localhost:8080'     -> 요청: http://localhost:8080/api/...
//  - 'http://localhost:8080/api' -> 요청: http://localhost:8080/api/...
const RAW = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/,'');
const baseURL = RAW ? (RAW.endsWith('/api') ? RAW : `${RAW}/api`) : '/api';

const api = axios.create({
  baseURL,
  withCredentials: true,
});

// JWT 자동 부착
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ✅ guests 추가
export async function fetchHotels({
  checkIn,
  checkOut,
  region = null,
  limit = 5,
  offset = 0,
  guests = 1,           // <--- 추가
}) {
  const params = { checkIn, checkOut, limit, offset, guests };  // <--- 추가
  if (region && region.length) params.region = region;
  const { data } = await api.get("/search/hotels", { params }); // baseURL에 /api 포함됨
  return data;
}

export default api;

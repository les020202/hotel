// src/api/wishlistApi.js
import axios from 'axios';

// ✅ 이 파일만으로 동작하도록 자체 axios 인스턴스 구성
const api = axios.create({
  // 환경변수 있으면 사용, 없으면 백엔드 기본 포트
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8888',
  withCredentials: true, // CORS 쿠키/세션 쓰면 유지
});

// ✅ 매 요청에 JWT 토큰 자동 첨부
api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

/** 위시리스트 추가 (찜) */
export async function addWishlist(hotelId) {
  const { data } = await api.post('/api/wishlists', { hotelId });
  return data; // 필요시 {id, hotelId, ...} 같은 응답 사용
}

/** 위시리스트 해제 (찜 취소) */
export async function removeWishlist(hotelId) {
  const { data } = await api.delete(`/api/wishlists/${hotelId}`);
  return data;
}

/** (선택) 현재 호텔이 찜됐는지 조회하고 싶다면 */
export async function getWishlistStatus(hotelId) {
  const { data } = await api.get(`/api/wishlists/${hotelId}`);
  return data; // 예: { wished: true }
}

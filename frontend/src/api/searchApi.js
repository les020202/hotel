// src/api/searchApi.js
import axios from "axios";

// VITE_API_BASE 예시
//  - '' (프록시 사용)            -> 요청: /api/...
//  - 'http://localhost:8080'     -> 요청: http://localhost:8080/api/...
//  - 'http://localhost:8080/api' -> 요청: http://localhost:8080/api/...
const RAW = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/, '');
const baseURL = RAW ? (RAW.endsWith('/api') ? RAW : `${RAW}/api`) : '/api';

const api = axios.create({
  baseURL,
  withCredentials: true,
});

// ✅ JWT 자동 부착 (token + accessToken 모두 지원)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

/** 내부: 객체에서 null/undefined/빈문자열 제거 */
function cleanParams(obj) {
  const out = {};
  for (const k of Object.keys(obj || {})) {
    const v = obj[k];
    if (v === undefined || v === null) continue;
    if (typeof v === 'string' && v.trim() === '') continue;
    out[k] = v;
  }
  return out;
}

/** 내부: 응답 정규화 */
function normalizeSearchResponse(data, { limit, offset }) {
  if (Array.isArray(data)) {
    const items = data;
    return {
      items,
      total: null,
      hasMore: items.length === limit,
      nextOffset: offset + items.length,
    };
  }
  const items = Array.isArray(data?.items) ? data.items : [];
  return {
    items,
    total: data?.total ?? null,
    hasMore: data?.hasMore ?? (items.length === limit),
    nextOffset: data?.nextOffset ?? (offset + items.length),
  };
}

/**
 * 호텔 검색
 */
export async function fetchHotels({
  q = '',
  checkIn,
  checkOut,
  adults = 2,
  children = 0,

  // 서버 표준 키
  minPrice,
  maxPrice,
  grades,
  ratingBands,
  amenityIds,

  sort,
  limit = 10,
  offset = 0,

  // 하위호환 입력
  priceMin,
  priceMax,
  ratingAtLeast,
  amenities,
  region,
  guests,
}) {
  // 하위호환 매핑
  if (!q && region) q = region;
  if ((adults == null || Number.isNaN(adults)) && guests != null) adults = guests;
  if (minPrice == null && priceMin != null) minPrice = priceMin;
  if (maxPrice == null && priceMax != null) maxPrice = priceMax;
  if ((!Array.isArray(ratingBands) || ratingBands.length === 0) && (ratingAtLeast != null)) {
    ratingBands = [Number(ratingAtLeast)];
  }
  if ((!Array.isArray(amenityIds) || amenityIds.length === 0) && Array.isArray(amenities)) {
    amenityIds = amenities;
  }

  // 날짜 포맷 안전화 (YYYY-MM-DD)
  const toISODate = (d) => {
    if (!d) return null;
    const x = new Date(String(d));
    return Number.isNaN(x.getTime()) ? null : x.toISOString().slice(0, 10);
  };
  const ci = toISODate(checkIn);
  const co = toISODate(checkOut);
  if (!ci || !co) throw new Error('fetchHotels: checkIn/checkOut are required');
  if (!(new Date(co) > new Date(ci))) throw new Error('fetchHotels: checkOut must be after checkIn');

  // 배열 직렬화 (서버는 CSV 수신)
  const gradesStr      = Array.isArray(grades) && grades.length ? grades.join(',') : undefined;
  const ratingBandsStr = Array.isArray(ratingBands) && ratingBands.length ? ratingBands.join(',') : undefined;
  const amenityIdsStr  = Array.isArray(amenityIds) && amenityIds.length ? amenityIds.join(',') : undefined;

  const params = cleanParams({
    q, checkIn: ci, checkOut: co, adults, children,
    minPrice, maxPrice,
    grades: gradesStr,
    ratingBands: ratingBandsStr,
    amenityIds: amenityIdsStr,
    sort,
    limit, offset,
  });

  const { data } = await api.get("/search/hotels", { params });
  return normalizeSearchResponse(data, { limit, offset });
}

/** ✅ 호텔 목록(페이지 단위) 조회 */
export async function hotelsSearch({ page = 0, size = 10 } = {}) {
  const { data } = await api.get('/search/hotels', { params: { page, size } });
  return data; // { content: [...], totalElements: 123, ... }
}

/** ✅ 최저가 조회 (여러 호텔 id 한번에) */
export async function fetchMinPrices(hotelIds = []) {
  if (!Array.isArray(hotelIds) || hotelIds.length === 0) return {};
  const { data } = await api.get('/search/min-prices', {
    params: { hotelIds: hotelIds.join(',') },
  });
  return data || {};
}

/** 호텔 자동완성 */
export async function fetchHotelSuggest(q, limit = 8) {
  const { data } = await api.get("/search/suggest", { params: { q, limit } });
  return Array.isArray(data) ? data : [];
}

/** 어메니티 목록 */
export async function fetchAmenities(scope = "HOTEL") {
  const { data } = await api.get("/amenities", { params: { scope } });
  return Array.isArray(data) ? data : [];
}

export default api;

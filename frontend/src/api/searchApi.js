// src/api/searchApi.js
import axios from "axios";

// VITE_API_BASE 예시:
//  - '' (프록시 사용)            -> 요청: /api/...
//  - 'http://localhost:8888'     -> 요청: http://localhost:8888/api/...
//  - 'http://localhost:8888/api' -> 요청: http://localhost:8888/api/...
const RAW = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/, '');
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

/** 내부: 객체에서 null/undefined/빈문자열 제거 */
function cleanParams(obj){
  const out = {};
  for (const k of Object.keys(obj || {})) {
    const v = obj[k];
    if (v === undefined || v === null) continue;
    if (typeof v === 'string' && v.trim() === '') continue;
    out[k] = v;
  }
  return out;
}

/** 내부: 응답 정규화 -> 항상 { items, total, hasMore, nextOffset } 형태 */
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
 * @param {Object} p
 * @param {string}   p.q
 * @param {string}   p.checkIn        YYYY-MM-DD
 * @param {string}   p.checkOut       YYYY-MM-DD
 * @param {number}   p.adults
 * @param {number}   p.children
 * @param {number}   p.minPrice       시작가 하한  (서버 표준)
 * @param {number}   p.maxPrice       시작가 상한  (서버 표준)
 * @param {number[]} p.grades         [5,4,...]
 * @param {number[]} p.ratingBands    [1|2|3|4] 다중
 * @param {number[]} p.amenityIds     [amenityId,...]
 * @param {number}   [p.limit=10]
 * @param {number}   [p.offset=0]
 * ---- 하위호환 입력(자동 매핑) ----
 * @param {number}   [p.priceMin]     -> minPrice
 * @param {number}   [p.priceMax]     -> maxPrice
 * @param {number}   [p.ratingAtLeast]-> ratingBands=[ratingAtLeast]
 * @param {string[]} [p.amenities]    -> amenityIds (서버가 코드 수용 시 그대로 전달 가능)
 * @param {string}   [p.region]       -> q
 * @param {number}   [p.guests]       -> adults
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

  // 구명/하위호환 입력
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
    // 구버전이 코드 배열을 보낼 때 임시 수용(서버가 코드/ID 둘 다 허용하면 정상 동작)
    amenityIds = amenities;
  }

  // 배열 직렬화(서버는 CSV 수신)
  const gradesStr      = Array.isArray(grades) && grades.length ? grades.join(',') : undefined;
  const ratingBandsStr = Array.isArray(ratingBands) && ratingBands.length ? ratingBands.join(',') : undefined;
  const amenityIdsStr  = Array.isArray(amenityIds) && amenityIds.length ? amenityIds.join(',') : undefined;

  const params = cleanParams({
    q, checkIn, checkOut, adults, children,
    minPrice, maxPrice,                       // ★ 서버 키로 보냄
    grades: gradesStr,
    ratingBands: ratingBandsStr,
    amenityIds: amenityIdsStr,
    sort,
    limit, offset,
  });

  const { data } = await api.get("/search/hotels", { params });
  return normalizeSearchResponse(data, { limit, offset });
}

/** 호텔 자동완성 */
export async function fetchHotelSuggest(q, limit = 8) {
  const { data } = await api.get("/search/suggest", { params: { q, limit } });
  return Array.isArray(data) ? data : [];
}

/** 어메니티 목록 (필터 렌더용) */
export async function fetchAmenities(scope = "HOTEL"){
  const { data } = await api.get("/amenities", { params: { scope } });
  // [{ id, code, name, sortOrder }]
  return Array.isArray(data) ? data : [];
}

export default api;

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

// JWT 자동 부착 (레거시 'token'과 신규 'accessToken' 둘 다 지원)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
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
 * @param {string}   [p.q]            - 키워드(이름/주소 등)
 * @param {string}   p.checkIn        - YYYY-MM-DD
 * @param {string}   p.checkOut       - YYYY-MM-DD
 * @param {number}   [p.adults]       - 레거시; 서버에는 guests 로 보냄
 * @param {number}   [p.children]
 * @param {number}   [p.minPrice]
 * @param {number}   [p.maxPrice]
 * @param {number[]} [p.grades]
 * @param {number[]} [p.ratingBands]
 * @param {number[]} [p.amenityIds]
 * @param {string}   [p.sort]
 * @param {number}   [p.limit=10]
 * @param {number}   [p.offset=0]
 * @param {string}   [p.region]       - 백엔드 지원
 * @param {boolean}  [p.regionExact]  - (백엔드 미사용; 그대로 보냈다가 무시돼도 OK)
 * @param {number}   [p.guests]       - adults와 동의어 (최종 서버 전송용)
 * ---- 하위호환 입력(자동 매핑) ----
 * @param {number}   [p.priceMin]     -> minPrice
 * @param {number}   [p.priceMax]     -> maxPrice
 * @param {number}   [p.ratingAtLeast]-> ratingBands=[ratingAtLeast]
 * @param {string[]} [p.amenities]    -> amenityIds
 */
export async function fetchHotels({
  q = '',
  checkIn,
  checkOut,
  adults,
  children = 0,

  // 서버 표준 키(현재 백엔드는 region/guests만 사용하지만, 유지)
  minPrice,
  maxPrice,
  grades,
  ratingBands,
  amenityIds,

  sort,
  limit = 10,
  offset = 0,

  // 추가 필터/옵션
  region,
  regionExact,
  guests,

  // 하위호환 입력
  priceMin,
  priceMax,
  ratingAtLeast,
  amenities,
}) {
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

  // 하위호환 매핑/보정
  if ((adults == null || Number.isNaN(adults)) && guests != null) adults = guests;
  if (minPrice == null && priceMin != null) minPrice = priceMin;
  if (maxPrice == null && priceMax != null) maxPrice = priceMax;
  if ((!Array.isArray(ratingBands) || ratingBands.length === 0) && (ratingAtLeast != null)) {
    ratingBands = [Number(ratingAtLeast)];
  }
  if ((!Array.isArray(amenityIds) || amenityIds.length === 0) && Array.isArray(amenities)) {
    amenityIds = amenities;
  }

  // 배열 직렬화(서버가 받는다면 CSV로)
  const gradesStr      = Array.isArray(grades) && grades.length ? grades.join(',') : undefined;
  const ratingBandsStr = Array.isArray(ratingBands) && ratingBands.length ? ratingBands.join(',') : undefined;
  const amenityIdsStr  = Array.isArray(amenityIds) && amenityIds.length ? amenityIds.join(',') : undefined;

  // 최종 guests 값 결정 (adults 우선 → guests), 기본 1
  const g = Number(adults ?? guests ?? 1);

  // ✅ 백엔드가 요구하는 핵심 파라미터는 반드시 포함
  //   - checkIn, checkOut, region(선택), guests(숫자), limit, offset
  //   - 나머지(q, children 등)는 기존 호환 유지(서버가 무시해도 OK)
  const params = cleanParams({
    q,
    checkIn: ci,
    checkOut: co,
    region,
    guests: g,
    limit,
    offset,

    // 이하 옵션은 서버가 지원하면 활용, 아니면 무시됨
    children,
    minPrice,
    maxPrice,
    grades: gradesStr,
    ratingBands: ratingBandsStr,
    amenityIds: amenityIdsStr,
    sort,
    regionExact,
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
  return Array.isArray(data) ? data : [];
}

export default api;

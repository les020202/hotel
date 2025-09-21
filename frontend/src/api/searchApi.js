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
 * @param {string}   [p.q]            - 키워드(이름/주소 등 전체 검색)
 * @param {string}   p.checkIn        - YYYY-MM-DD
 * @param {string}   p.checkOut       - YYYY-MM-DD
 * @param {number}   [p.adults=2]
 * @param {number}   [p.children=0]
 * @param {number}   [p.minPrice]     - 시작가 하한 (서버 표준)
 * @param {number}   [p.maxPrice]     - 시작가 상한 (서버 표준)
 * @param {number[]} [p.grades]       - 예: [5,4,...]
 * @param {number[]} [p.ratingBands]  - 예: [1|2|3|4] 다중
 * @param {number[]} [p.amenityIds]   - [amenityId,...]
 * @param {string}   [p.sort]
 * @param {number}   [p.limit=10]
 * @param {number}   [p.offset=0]
 * @param {string}   [p.region]       - 정확/부분 일치용 지역 파라미터 (서버에서 사용)
 * @param {boolean}  [p.regionExact]  - true면 지역 정확 일치(예: 서울특별시만)
 * @param {number}   [p.guests]       - adults와 동의어
 * ---- 하위호환 입력(자동 매핑) ----
 * @param {number}   [p.priceMin]     -> minPrice
 * @param {number}   [p.priceMax]     -> maxPrice
 * @param {number}   [p.ratingAtLeast]-> ratingBands=[ratingAtLeast]
 * @param {string[]} [p.amenities]    -> amenityIds (서버가 코드 수용 시 그대로 전달 가능)
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

  // 추가 필터/옵션
  region,          // ← 프론트에서 전달한 지역 (정식명 권장: 예 '서울특별시')
  regionExact,     // ← 정확 일치 여부 (true/false)
  guests,

  // 하위호환 입력
  priceMin,
  priceMax,
  ratingAtLeast,
  amenities,
}) {
  // 하위호환 매핑
  // ⚠️ 기존에는 (!q && region) q = region 으로 region을 q에 덮어썼지만,
  //     이제는 'region'을 별도 파라미터로 서버에 전달하므로 덮어쓰지 않습니다.
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

  // ✅ region / regionExact를 그대로 서버로 전달
  const params = cleanParams({
    q, checkIn, checkOut, adults, children,
    minPrice, maxPrice,                       // 서버 표준 키
    grades: gradesStr,
    ratingBands: ratingBandsStr,
    amenityIds: amenityIdsStr,
    sort,
    limit, offset,
    region,            // ★ 추가: 지역 필터
    regionExact,       // ★ 추가: 정확 일치 여부
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

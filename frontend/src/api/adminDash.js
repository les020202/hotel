// frontend/src/api/adminDash.js
import axios from "axios";

const api = axios.create({
  baseURL: "/api/admin/dashboard",
  // 필요하면 withCredentials, headers 등 설정
});

const toISO = (d) =>
  typeof d === "string" ? d : (d ?? new Date()).toISOString().slice(0, 10);

export const AdminDashAPI = {
  // 개요 카드
  getOverview({ region, hotel, from, to }) {
    return api.get("/overview", {
      params: { region, hotel, from: toISO(from), to: toISO(to) },
    });
  },

  // 일별 추이
  getTrendsDaily({ region, hotel, from, to }) {
    return api.get("/trends/daily", {
      params: { region, hotel, from: toISO(from), to: toISO(to) },
    });
  },

  // 시간별 추이(보통 to 날짜 기준)
  getTrendsHourly({ region, hotel, from, to }) {
    return api.get("/trends/hourly", {
      params: { region, hotel, from: toISO(from), to: toISO(to) },
    });
  },

  // 매출 랭킹 or 환불율 랭킹
  getRankings({ range = "custom", type = "revenue", minVolume = 0, region, hotel, from, to }) {
    return api.get("/rankings", {
      params: {
        range,
        type,          // "revenue" | "refundRate"
        minVolume,
        region,
        hotel,
        from: toISO(from),
        to: toISO(to),
      },
    });
  },

  // 상승/하락 속도
  getVelocity({ range = "7d", limit = 10, region, hotel, from, to }) {
    return api.get("/velocity", {
      params: { range, limit, region, hotel, from: toISO(from), to: toISO(to) },
    });
  },

  // 분해(지역/호텔/채널)
  getDecomposition({ by = "region", region, hotel, from, to }) {
    // metric은 현재 서버에서 한 종류(GMV)로 처리—필요시 추가 파라미터 확장
    return api.get("/decomposition", {
      params: { by, region, hotel, from: toISO(from), to: toISO(to) },
    });
  },

  // 정산 스냅샷
  getSettlement({ region, hotel, from, to }) {
    return api.get("/settlement", {
      params: { region, hotel, from: toISO(from), to: toISO(to) },
    });
  },
};

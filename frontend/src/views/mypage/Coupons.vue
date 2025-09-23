<!-- src/views/mypage/Coupons.vue -->
<template>
  <div class="coupon-page page">
    <!-- Top Bar (Home 버튼 없음) -->
    <div class="topbar">
      <button class="icon" @click="$router.back()" aria-label="뒤로가기">‹</button>
      <div class="topbar-title">내 쿠폰함</div>
      <span class="spacer" aria-hidden="true"></span>
    </div>

    <!-- Tabs -->
    <div class="tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{ active: activeTab === t.key }"
        @click="activeTab = t.key"
      >
        {{ t.label }}
      </button>

      <label class="inline">
        <input
          type="checkbox"
          v-model="showAll"
          @change="load"
          :disabled="activeTab !== 'all'"
        />
        만료 포함(서버)
      </label>
      <span v-if="activeTab !== 'all'" class="hint">
        {{ activeTab === 'valid' ? '유효 탭에서는 자동으로 해제됩니다.' : '만료 탭에서는 자동으로 적용됩니다.' }}
      </span>
    </div>

    <!-- Error -->
    <div v-if="err" class="error">{{ err }}</div>

    <!-- Loading skeleton -->
    <ul v-if="loading" class="list">
      <li v-for="n in 4" :key="n" class="item skeleton">
        <div class="row">
          <div class="sk-bar sk-w-40"></div>
          <span class="sk-chip sk-w-16"></span>
        </div>
        <div class="meta">
          <div class="sk-bar sk-w-70"></div>
        </div>
      </li>
    </ul>

    <!-- Empty State -->
    <div v-else-if="filtered.length === 0" class="empty">
      <div class="empty-emoji">🎫</div>
      <div class="empty-title">표시할 쿠폰이 없습니다.</div>
      <div class="empty-sub">다른 탭(전체/유효/만료)도 확인해 보세요.</div>
      <button class="btn-outline" @click="load">새로고침</button>
    </div>

    <!-- List -->
    <ul v-else class="list">
      <li
        v-for="c in filtered"
        :key="c.id"
        class="item coupon-card"
        :class="{ expired: isExpired(c) }"
      >
        <!-- 헤더: 제목 + 배지 -->
        <div class="card-head">
          <div class="card-title">{{ c.title }}</div>
          <div class="badges">
            <span class="badge" v-if="c.stackable">중복가능</span>
            <span class="badge danger" v-if="isExpired(c)">만료</span>
          </div>
        </div>

        <!-- 본문: 큰 금액 + 코드/기간 -->
        <div class="card-body">
          <div class="amount">
            {{ formatMoney(c.amount) }}
          </div>

          <div class="details">
            <div class="line">
              <span class="label">코드</span>
              <span class="value code mono">{{ c.code }}</span>
            </div>
            <div class="line">
              <span class="label">기간</span>
              <span
                class="value period"
                :class="{ infinite: !c.validFrom && !c.validTo }"
              >
                <span class="cal-ico" aria-hidden="true">📅</span>
                {{ c.validFrom ? c.validFrom : '제한없음' }}
                <span class="tilde">~</span>
                {{ c.validTo ? c.validTo : '제한없음' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 액션 -->
        <div class="actions">
          <button class="btn-ghost" @click="copy(c.code)">코드 복사</button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import api from '@/api/auth' // baseURL: '/api' 가정

const items = ref([])
const showAll = ref(true)          // ✅ 전체 탭 기본: 체크 ON
const err = ref('')
const loading = ref(false)

// Tabs
const tabs = [
  { key: 'all', label: '전체' },
  { key: 'valid', label: '유효' },
  { key: 'expired', label: '만료' }
]
const activeTab = ref('all')

// 서버 로드
async function load () {
  err.value = ''
  loading.value = true
  try {
    const { data } = await api.get('/coupons', { params: { all: showAll.value } })
    items.value = data
  } catch (e) {
    err.value = `쿠폰 조회 실패: ${e?.response?.status || ''}`
  } finally {
    loading.value = false
  }
}

// 탭 변경 시 동작
watch(activeTab, (val) => {
  if (val === 'valid') {
    // 유효 탭: 자동 OFF
    if (showAll.value) {
      showAll.value = false
      load()
    }
  } else if (val === 'expired') {
    // 만료 탭: 자동 ON
    if (!showAll.value) {
      showAll.value = true
      load()
    }
  } else if (val === 'all') {
    // 전체 탭: 기본 ON (사용자는 이후 해제 가능)
    if (!showAll.value) {
      showAll.value = true
      load()
    }
  }
})

function isExpired (c) {
  if (!c.validTo) return false
  const today = new Date().toISOString().slice(0, 10)
  return c.validTo < today
}

const isValidToday = (c) => {
  const today = new Date().toISOString().slice(0, 10)
  const fromOk = !c.validFrom || c.validFrom <= today
  const toOk = !c.validTo || c.validTo >= today
  return fromOk && toOk
}

const filtered = computed(() => {
  if (activeTab.value === 'valid') return items.value.filter(isValidToday)
  if (activeTab.value === 'expired') return items.value.filter((c) => isExpired(c))
  return items.value
})

function formatMoney (v) {
  return (v ?? 0).toLocaleString('ko-KR', { style: 'currency', currency: 'KRW' })
}

async function copy (text) {
  try {
    await navigator.clipboard.writeText(text)
    alert('쿠폰 코드가 복사되었습니다.')
  } catch {
    alert('복사 실패. 수동으로 복사해 주세요.')
  }
}

onMounted(load)
</script>

<style scoped>
/* 레이아웃 */
.page {
  max-width: 980px;
  margin: 0 auto;
  padding: 16px;
}

/* Topbar (Home 버튼 제거 버전) */
.topbar {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid #f1f5f9;
  margin: -16px -16px 16px;
  background: #fff;
}
.icon {
  border: 0;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
}
.topbar-title {
  font-weight: 700;
  font-size: 18px;
  flex: 1;
  text-align: center;
}
/* 좌우 균형용 spacer */
.spacer {
  display: inline-block;
  width: 34px; /* .icon 크기와 매칭 */
}

/* Tabs */
.tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 8px 0 16px;
}
.tab {
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #111827;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
}
.tab.active {
  background: #0ea5e9;
  border-color: #0ea5e9;
  color: #fff;
}
.tabs .inline {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 13px;
}
.hint { color: #64748b; font-size: 12px; }

/* List & 기본 Item */
.list { display: grid; gap: 12px; }
.item {
  position: relative;
  border: 1px solid #e5e7eb;
  padding: 14px 14px 12px;
  border-radius: 14px;
  background: #fff;
  transition: box-shadow .15s ease, transform .05s ease;
}
.item:hover { box-shadow: 0 2px 10px rgba(2, 6, 23, 0.06); }

/* --- Coupon card (redesign) --- */
.coupon-card {
  border: 1px solid #e5e7eb;
  background: linear-gradient(180deg, #ffffff 0%, #fcfcff 100%);
  border-radius: 16px;
  padding: 16px 16px 12px;
  transition: box-shadow .15s ease, transform .05s ease, border-color .15s ease;
}
.coupon-card:hover {
  box-shadow: 0 4px 18px rgba(2, 6, 23, 0.08);
  border-color: #dbeafe;
}
.coupon-card::before {
  /* 좌측 포인트 라인 */
  content: "";
  position: absolute;
  left: 0; top: 10px; bottom: 10px;
  width: 4px;
  border-radius: 4px;
  background: linear-gradient(180deg, #0ea5e9, #22c55e);
  opacity: .9;
}

/* 헤더 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.card-title {
  font-weight: 800;
  font-size: 16px;
  letter-spacing: -0.2px;
}
.badges { display: inline-flex; gap: 6px; }

/* 본문 */
.card-body {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

/* 금액 크게 */
.amount {
  font-weight: 900;
  font-size: 22px;         /* 크게 */
  line-height: 1.2;
  letter-spacing: -0.3px;
  color: #0b7;
}

/* 상세 라인 */
.details { display: grid; gap: 8px; }
.line {
  display: grid;
  grid-template-columns: 56px 1fr;
  align-items: center;
  gap: 10px;
}
.label {
  color: #64748b;
  font-size: 13px;
}
.value { color: #0f172a; }

/* 코드 스타일 */
.code {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 13px;
}

/* 기간 강조(크게/굵게 + pill) */
.period {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #eef6ff;
  border: 1px solid #dbeafe;
  padding: 6px 10px;
  border-radius: 10px;
  font-size: 15px;         /* 크게 */
  font-weight: 700;        /* 굵게 */
  color: #0f172a;
}
.period .tilde { opacity: .7; padding: 0 2px; }
.period.infinite { background: #ecfdf5; border-color: #bbf7d0; }
.cal-ico { font-size: 16px; }

/* 만료 상태 톤 다운 */
.coupon-card.expired { opacity: .85; }
.coupon-card.expired .amount { color: #334155; }
.coupon-card.expired .period {
  background: #fff1f2; border-color: #fecaca; color: #991b1b;
}

/* 액션 */
.actions { margin-top: 8px; display: flex; justify-content: flex-end; }
.btn-ghost {
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 10px;
  font-size: 12px;
  cursor: pointer;
}
.btn-ghost:hover { border-color: #cbd5e1; }

/* 배지 */
.badge {
  font-size: 11px;
  border: 1px solid #e5e7eb;
  padding: 2px 8px;
  border-radius: 999px;
  color: #0369a1;
  background: #f0f9ff;
  white-space: nowrap;
}
.badge.danger {
  border-color: #fecaca;
  color: #b91c1c;
  background: #fef2f2;
}

/* Empty */
.empty {
  border: 1px dashed #e5e7eb;
  background: #fafafa;
  border-radius: 16px;
  padding: 40px 20px;
  text-align: center;
  color: #475569;
}
.empty-emoji { font-size: 36px; margin-bottom: 6px; }
.empty-title { font-weight: 700; color: #111827; margin-bottom: 4px; }
.empty-sub { margin-bottom: 14px; }
.btn-outline {
  padding: 8px 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
  cursor: pointer;
}

/* Error */
.error {
  color: #ef4444;
  margin: 8px 0 12px;
  padding: 8px 10px;
  border: 1px solid #fecaca;
  background: #fff1f2;
  border-radius: 10px;
}

/* Loading skeleton */
.skeleton .sk-bar,
.skeleton .sk-chip {
  display: inline-block;
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(90deg,#f1f5f9,#e2e8f0,#f1f5f9);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}
.skeleton .sk-bar { width: 60%; }
.skeleton .sk-chip { width: 40px; height: 18px; border-radius: 999px; }
.sk-w-40 { width: 40%; }
.sk-w-70 { width: 70%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>

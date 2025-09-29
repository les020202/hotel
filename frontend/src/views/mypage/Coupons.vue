<!-- src/views/coupons/CouponPage.vue -->
<template>
  <div class="coupon-page page">
    <div class="topbar">
      <div class="topbar-title">내 쿠폰함</div>
      <span class="spacer" aria-hidden="true"></span>
    </div>

    <!-- ✅ 쿠폰 코드 입력/지급 박스 -->
    <div class="claim-box">
      <input
        class="claim-input"
        v-model.trim="claimCode"
        maxlength="64"
        placeholder="쿠폰 코드를 입력하세요"
        @keyup.enter="onClaim"
      />
      <button class="btn-claim" :disabled="claiming || !claimCode" @click="onClaim">
        <span v-if="!claiming">지급</span>
        <span v-else class="spinner" aria-label="처리중"></span>
      </button>
    </div>
    <p v-if="claimMsg" class="claim-msg" :class="{ ok: claimOk, bad: !claimOk }">
      {{ claimMsg }}
    </p>

    <!-- ✅ 상단 토글: 기본 해제(사용 가능만 표시) / 체크 시 사용완료+만료 포함 -->
    <div class="toolbar">
      <label class="inline">
        <input
          type="checkbox"
          v-model="showHistory"
          @change="load"
        />
        만료/사용완료 포함(서버)
      </label>
      <small class="hint">
        {{ showHistory ? '모든 쿠폰(사용가능/사용완료/만료)을 표시합니다.' : '오늘 사용 가능한 쿠폰만 표시합니다.' }}
      </small>
    </div>

    <div v-if="err" class="error">{{ err }}</div>

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

    <div v-else-if="filtered.length === 0" class="empty">
      <div class="empty-emoji">🎫</div>
      <div class="empty-title">
        {{ showHistory ? '표시할 쿠폰이 없습니다.' : '오늘 사용 가능한 쿠폰이 없습니다.' }}
      </div>
      <div class="empty-sub">
        {{ showHistory ? '필터에 해당하는 쿠폰이 없어요.' : '만료/사용완료 쿠폰을 보려면 위 체크를 켜보세요.' }}
      </div>
      <button class="btn-outline" @click="load">새로고침</button>
    </div>

    <ul v-else class="list">
      <li
        v-for="c in filtered"
        :key="c.id"
        class="item coupon-card"
        :class="{ expired: isExpired(c) || c.status === 'EXPIRED' }"
      >
        <div class="card-head">
          <div class="card-title">{{ c.title }}</div>
          <div class="badges">
            <span class="badge" v-if="c.stackable">중복가능</span>
            <span class="badge" v-if="c.status === 'USED'">사용완료</span>
            <span class="badge danger" v-if="isExpired(c) || c.status === 'EXPIRED'">만료</span>
          </div>
        </div>

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
              <span class="value period" :class="{ infinite: !c.validFrom && !c.validTo }">
                <span class="cal-ico" aria-hidden="true">📅</span>
                {{ c.validFrom ? c.validFrom : '제한없음' }}
                <span class="tilde">~</span>
                {{ c.validTo ? c.validTo : '제한없음' }}
              </span>
            </div>
            <div class="line">
              <span class="label">상태</span>
              <span class="value">{{ statusLabel(c.status, c) }}</span>
            </div>
            <div class="line" v-if="c.issuedAt">
              <span class="label">지급일</span>
              <span class="value">{{ c.issuedAt }}</span>
            </div>
          </div>
        </div>

        <div class="actions">
          <button class="btn-ghost" @click="copy(c.code)">코드 복사</button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { fetchMyCoupons, claimCoupon } from '@/api/couponsApi'

const items = ref([])
const err = ref('')
const loading = ref(false)

/* claim UI state */
const claimCode = ref('')
const claiming  = ref(false)
const claimMsg  = ref('')
const claimOk   = ref(false)

/* ✅ 기본: 오늘 사용 가능한 것만 표시. 체크 시 사용완료+만료 포함 */
const showHistory = ref(false)

/* 서버에서 만료/사용완료 포함 여부를 토글에 맞춰 가져오도록 */
async function load () {
  err.value = ''
  loading.value = true
  try {
    const includeExpiredParam = showHistory.value // 체크 시 만료 포함
    // 일부 백엔드가 USED를 expired와 함께 보내므로 호환용으로 all도 전송
    items.value = await fetchMyCoupons({
      includeExpired: includeExpiredParam,
      all: includeExpiredParam,
    })
  } catch (e) {
    err.value = `쿠폰 조회 실패: ${e?.response?.status || ''}`
  } finally {
    loading.value = false
  }
}

/* === 날짜/상태 유틸 === */
function isExpired (c) {
  if (c.status === 'EXPIRED') return true
  if (!c.validTo) return false
  const today = new Date().toISOString().slice(0, 10)
  return c.validTo < today
}

function isAvailableToday (c) {
  const today = new Date().toISOString().slice(0, 10)
  const fromOk = !c.validFrom || c.validFrom <= today
  const toOk   = !c.validTo   || c.validTo   >= today
  return c.status === 'AVAILABLE' && fromOk && toOk && !isExpired(c)
}

function formatMoney (v) {
  return (v ?? 0).toLocaleString('ko-KR', { style: 'currency', currency: 'KRW' })
}

function statusLabel (status, c) {
  if (status === 'AVAILABLE') return isExpired(c) ? '만료' : '사용 가능'
  if (status === 'USED')      return '사용 완료'
  if (status === 'EXPIRED')   return '만료'
  return status
}

async function copy (text) {
  try {
    await navigator.clipboard.writeText(text)
    alert('쿠폰 코드가 복사되었습니다.')
  } catch {
    alert('복사 실패. 수동으로 복사해 주세요.')
  }
}

/* ✅ 쿠폰 지급 */
async function onClaim () {
  claimMsg.value = ''
  claimOk.value  = false
  const code = (claimCode.value || '').trim()
  if (!code) return

  try {
    claiming.value = true
    const res = await claimCoupon(code)
    claimOk.value  = true
    claimMsg.value = (res?.message || '쿠폰이 지급되었습니다.')
    claimCode.value = ''
    await load()
  } catch (e) {
    const status = e?.response?.status
    const data   = e?.response?.data
    claimOk.value  = false
    claimMsg.value = data?.message
      || (status === 404 ? '존재하지 않는 쿠폰 코드입니다.' :
          status === 409 ? '이미 보유했거나 지급 불가한 쿠폰입니다.' :
          `지급 실패 [${status ?? 'ERR'}]`)
  } finally {
    claiming.value = false
  }
}

/* ✅ 필터링
   - showHistory = false  -> 오늘 사용 가능한 쿠폰만
   - showHistory = true   -> 전체(사용가능 + 사용완료 + 만료), 정렬: 사용가능 → 사용완료 → 만료
*/
const filtered = computed(() => {
  if (!showHistory.value) return items.value.filter(isAvailableToday)

  const available = []
  const used = []
  const expired = []

  for (const c of items.value) {
    if (isAvailableToday(c)) available.push(c)
    else if (c.status === 'USED') used.push(c)
    else if (isExpired(c) || c.status === 'EXPIRED') expired.push(c)
    // 그 외 상태는 요구사항 외라서 표시하지 않음
  }
  return [...available, ...used, ...expired]
})

onMounted(load)
</script>

<style scoped>
.page { max-width: 980px; margin: 0 auto; padding: 16px; }
.topbar { height: 60px; display: flex; align-items: center; gap: 8px; padding: 0 16px; border-bottom: 1px solid #f1f5f9; margin: -16px -16px 16px; background: #fff; }
.icon { border: 0; background: transparent; font-size: 24px; cursor: pointer; }
.topbar-title { font-weight: 700; font-size: 18px; flex: 1; text-align: center; }
.spacer { display: inline-block; width: 34px; }

/* ✅ claim box */
.claim-box{
  display:flex; gap:8px; align-items:center;
  padding: 10px; border:1px solid #e5e7eb; border-radius: 12px; background:#fff; margin-bottom:12px;
}
.claim-input{
  flex:1; height:38px; border:1px solid #e5e7eb; border-radius:10px; padding:0 12px; font-size:14px; outline:none;
}
.claim-input:focus{ box-shadow:0 0 0 3px rgba(14,165,233,.18); border-color:#bae6fd; }
.btn-claim{
  height:38px; padding:0 14px; border-radius:10px; border:1px solid #0ea5e9; background:#0ea5e9; color:#fff; font-weight:800; cursor:pointer;
}
.btn-claim:disabled{ opacity:.6; cursor:not-allowed; }
.claim-msg{ margin:8px 2px 6px; font-size:13px; }
.claim-msg.ok{ color:#065f46; }
.claim-msg.bad{ color:#b91c1b; }

/* ✅ 힌트/토글 영역 */
.toolbar { display:flex; align-items:center; gap:12px; margin: 8px 0 16px; }
.toolbar .inline { display:inline-flex; align-items:center; gap:6px; color:#475569; font-size:13px; }
.hint { color:#64748b; font-size:12px; }

.list { display: grid; gap: 12px; }
.item { position: relative; border: 1px solid #e5e7eb; padding: 14px 14px 12px; border-radius: 14px; background: #fff; transition: box-shadow .15s ease, transform .05s ease; }
.item:hover { box-shadow: 0 2px 10px rgba(2, 6, 23, 0.06); }

.coupon-card { border: 1px solid #e5e7eb; background: linear-gradient(180deg, #ffffff 0%, #fcfcff 100%); border-radius: 16px; padding: 16px 16px 12px; transition: box-shadow .15s ease, transform .05s ease, border-color .15s ease; }
.coupon-card:hover { box-shadow: 0 4px 18px rgba(2, 6, 23, 0.08); border-color: #dbeafe; }
.coupon-card::before { content: ""; position: absolute; left: 0; top: 10px; bottom: 10px; width: 4px; border-radius: 4px; background: linear-gradient(180deg, #0ea5e9, #22c55e); opacity: .9; }

.card-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 10px; }
.card-title { font-weight: 800; font-size: 16px; letter-spacing: -0.2px; }
.badges { display: inline-flex; gap: 6px; }

.card-body { display: grid; grid-template-columns: 1fr; gap: 10px; }
.amount { font-weight: 900; font-size: 22px; line-height: 1.2; letter-spacing: -0.3px; color: #0b7; }

.details { display: grid; gap: 8px; }
.line { display: grid; grid-template-columns: 56px 1fr; align-items: center; gap: 10px; }
.label { color: #64748b; font-size: 13px; }
.value { color: #0f172a; }

.code { display: inline-flex; align-items: center; gap: 6px; background: #f8fafc; border: 1px dashed #e2e8f0; padding: 4px 8px; border-radius: 8px; font-size: 13px; }

.period { display: inline-flex; align-items: center; gap: 8px; background: #eef6ff; border: 1px solid #dbeafe; padding: 6px 10px; border-radius: 10px; font-size: 15px; font-weight: 700; color: #0f172a; }
.period .tilde { opacity: .7; padding: 0 2px; }
.period.infinite { background: #ecfdf5; border-color: #bbf7d0; }
.cal-ico { font-size: 16px; }

.coupon-card.expired { opacity: .9; }
.coupon-card.expired .amount { color: #334155; }
.coupon-card.expired .period { background: #fff1f2; border-color: #fecaca; color: #991b1b; }

.actions { margin-top: 8px; display: flex; justify-content: flex-end; }
.btn-ghost { padding: 6px 10px; border: 1px solid #e5e7eb; background: #fff; border-radius: 10px; font-size: 12px; cursor: pointer; }
.btn-ghost:hover { border-color: #cbd5e1; }

.badge { font-size: 11px; border: 1px solid #e5e7eb; padding: 2px 8px; border-radius: 999px; color: #0369a1; background: #f0f9ff; white-space: nowrap; }
.badge.danger { border-color: #fecaca; color: #b91c1c; background: #fef2f2; }

.empty { border: 1px dashed #e5e7eb; background: #fafafa; border-radius: 16px; padding: 40px 20px; text-align: center; color: #475569; }
.empty-emoji { font-size: 36px; margin-bottom: 6px; }
.empty-title { font-weight: 700; color: #111827; margin-bottom: 4px; }
.empty-sub { margin-bottom: 14px; }
.btn-outline { padding: 8px 12px; border-radius: 10px; background: #fff; border: 1px solid #e5e7eb; cursor: pointer; }

.error { color: #ef4444; margin: 8px 0 12px; padding: 8px 10px; border: 1px solid #fecaca; background: #fff1f2; border-radius: 10px; }

.skeleton .sk-bar, .skeleton .sk-chip { display: inline-block; height: 12px; border-radius: 6px; background: linear-gradient(90deg,#f1f5f9,#e2e8f0,#f1f5f9); background-size: 200% 100%; animation: shimmer 1.2s infinite; }
.skeleton .sk-bar { width: 60%; }
.skeleton .sk-chip { width: 40px; height: 18px; border-radius: 999px; }
.sk-w-40 { width: 40%; }
.sk-w-70 { width: 70%; }
@keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }

/* small spinner */
.spinner{ display:inline-block; width:16px; height:16px; border:2px solid rgba(0,0,0,.15); border-top-color:#0f172a; border-radius:50%; animation:spin 1s linear infinite }
@keyframes spin{ to{ transform:rotate(360deg) } }
</style>

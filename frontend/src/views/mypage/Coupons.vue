<!-- src/views/coupons/CouponPage.vue (예시 경로: 기존 파일 교체) -->
<template>
  <div class="coupon-page page">
    <div class="topbar">
      <button class="icon" @click="$router.back()" aria-label="뒤로가기">‹</button>
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
      <div class="empty-title">표시할 쿠폰이 없습니다.</div>
      <div class="empty-sub">다른 탭(전체/유효/만료)도 확인해 보세요.</div>
      <button class="btn-outline" @click="load">새로고침</button>
    </div>

    <ul v-else class="list">
      <li
        v-for="c in filtered"
        :key="c.id"
        class="item coupon-card"
        :class="{ expired: isExpired(c) }"
      >
        <div class="card-head">
          <div class="card-title">{{ c.title }}</div>
          <div class="badges">
            <span class="badge" v-if="c.stackable">중복가능</span>
            <span class="badge danger" v-if="isExpired(c)">만료</span>
            <span class="badge" v-if="c.status === 'USED'">사용완료</span>
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
import { ref, onMounted, computed, watch } from 'vue'
import { fetchMyCoupons, claimCoupon } from '@/api/couponsApi'

const items = ref([])
const showAll = ref(true)
const err = ref('')
const loading = ref(false)

/* claim UI state */
const claimCode = ref('')
const claiming  = ref(false)
const claimMsg  = ref('')
const claimOk   = ref(false)

const tabs = [
  { key: 'all', label: '전체' },
  { key: 'valid', label: '유효' },
  { key: 'expired', label: '만료' }
]
const activeTab = ref('all')

async function load () {
  err.value = ''
  loading.value = true
  try {
    items.value = await fetchMyCoupons({ all: showAll.value })
  } catch (e) {
    err.value = `쿠폰 조회 실패: ${e?.response?.status || ''}`
  } finally {
    loading.value = false
  }
}

watch(activeTab, (val) => {
  if (val === 'valid') {
    if (showAll.value) { showAll.value = false; load() }
  } else if (val === 'expired') {
    if (!showAll.value) { showAll.value = true; load() }
  } else if (val === 'all') {
    if (!showAll.value) { showAll.value = true; load() }
  }
})

function isExpired (c) {
  if (!c.validTo) return false
  const today = new Date().toISOString().slice(0, 10)
  return c.validTo < today || c.status === 'EXPIRED'
}

const isValidToday = (c) => {
  const today = new Date().toISOString().slice(0, 10)
  const fromOk = !c.validFrom || c.validFrom <= today
  const toOk   = !c.validTo   || c.validTo   >= today
  return c.status === 'AVAILABLE' && fromOk && toOk
}

const filtered = computed(() => {
  if (activeTab.value === 'valid')   return items.value.filter(isValidToday)
  if (activeTab.value === 'expired') return items.value.filter((c) => isExpired(c))
  return items.value
})

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
    // 서버에서 메시지/지급정보 리턴한다고 가정
    claimOk.value  = true
    claimMsg.value = (res?.message || '쿠폰이 지급되었습니다.')
    claimCode.value = ''
    // 목록 즉시 갱신
    await load()
  } catch (e) {
    const status = e?.response?.status
    const data   = e?.response?.data
    claimOk.value  = false
    // 대표적인 에러 메시지 처리
    claimMsg.value = data?.message
      || (status === 404 ? '존재하지 않는 쿠폰 코드입니다.' :
          status === 409 ? '이미 보유했거나 지급 불가한 쿠폰입니다.' :
          `지급 실패 [${status ?? 'ERR'}]`)
  } finally {
    claiming.value = false
  }
}

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

.tabs { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin: 8px 0 16px; }
.tab { padding: 6px 12px; border: 1px solid #e5e7eb; background: #fff; color: #111827; border-radius: 999px; font-size: 13px; cursor: pointer; }
.tab.active { background: #0ea5e9; border-color: #0ea5e9; color: #fff; }
.tabs .inline { margin-left: auto; display: inline-flex; align-items: center; gap: 6px; color: #475569; font-size: 13px; }
.hint { color: #64748b; font-size: 12px; }

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

.code { display: inline-flex; align-items: center; gap: 6px; background: #f8fafc; border: 1 dashed #e2e8f0; padding: 4px 8px; border-radius: 8px; font-size: 13px; }

.period { display: inline-flex; align-items: center; gap: 8px; background: #eef6ff; border: 1px solid #dbeafe; padding: 6px 10px; border-radius: 10px; font-size: 15px; font-weight: 700; color: #0f172a; }
.period .tilde { opacity: .7; padding: 0 2px; }
.period.infinite { background: #ecfdf5; border-color: #bbf7d0; }
.cal-ico { font-size: 16px; }

.coupon-card.expired { opacity: .85; }
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

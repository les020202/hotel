<!-- src/views/reservation/ReservationPage.vue -->
<script setup>
/* 한국어 설명, 코드는 English */
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { cancelReservationHold, getAvailableCoupons, repriceReservationHold } from '@/api/reservation'
import { loadPaymentWidget } from '@tosspayments/payment-widget-sdk'
import api from '@/api/auth' 
/* ---------------------- Routing & Query ---------------------- */
const route  = useRoute()
const router = useRouter()

const holdCode      = ref(String(route.query.holdCode || ''))
const holdExpiresAt = ref(String(route.query.expiresAt || route.query.holdExpiresAt || ''))
const hotelId       = ref(route.query.hotelId ? Number(route.query.hotelId) : null)
const roomTypeId    = ref(route.query.roomTypeId ? Number(route.query.roomTypeId) : null)
const ratePlanId    = ref(route.query.ratePlanId ? Number(route.query.ratePlanId) : 1)
const userId        = ref(route.query.userId ? Number(route.query.userId) : null)
const guests        = ref(route.query.guests ? Number(route.query.guests) : 1)
const checkIn       = ref(String(route.query.checkIn || ''))
const checkOut      = ref(String(route.query.checkOut || ''))
const hotelName     = ref(String(route.query.hotelName || ''))
const roomTypeName  = ref(String(route.query.roomTypeName || ''))
const quotedTotal   = ref(route.query.totalAmount ? Number(route.query.totalAmount) : null)

/* ---------------------- Server time sync (expiry) ---------------------- */
const serverOffsetMs = ref(0)
async function syncServerTime() {
  try {
    const base = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/,'')
    const url =
      base
        ? (base.endsWith('/api') ? `${base}/time` : `${base}/api/time`)
        : null

    if (url) {
      const r = await fetch(url, { cache: 'no-store' })
      if (r.ok) {
        const j = await r.json().catch(() => ({}))
        const serverNow = Date.parse(j.now || '')
        if (!Number.isNaN(serverNow)) {
          serverOffsetMs.value = serverNow - Date.now()
          return
        }
      }
    }
    // fallback: server Date header
    const head = await fetch(window.location.origin, { method: 'HEAD', cache: 'no-store' })
    const dateHeader = head.headers.get('Date')
    if (dateHeader) {
      const serverNow = Date.parse(dateHeader)
      if (!Number.isNaN(serverNow)) serverOffsetMs.value = serverNow - Date.now()
    }
  } catch {}
}
function nowMs(){ return Date.now() + serverOffsetMs.value }

/* ---------------------- Toast / Modal ---------------------- */
const toasts = ref([])
function toast(message, ms = 3000) {
  const id = Date.now()
  toasts.value.push({ id, message })
  setTimeout(() => { toasts.value = toasts.value.filter(t => t.id !== id) }, ms)
}
const showExpireModal = ref(false)

/* ---------------------- Expiry countdown ---------------------- */
const remainSec = ref(0)
const expired   = computed(() => remainSec.value <= 0)
let timerId = null
const warned60 = ref(false)
function tick () {
  const expMs = Date.parse(holdExpiresAt.value)
  remainSec.value = Number.isNaN(expMs) ? 0 : Math.max(0, Math.floor((expMs - nowMs()) / 1000))
  if (remainSec.value === 0 && timerId) { clearInterval(timerId); timerId = null; showExpireModal.value = true }
  if (remainSec.value === 60 && !warned60.value) { warned60.value = true; toast('곧 만료돼요. 1분 남았습니다.') }
}
function startTimer(){ tick(); timerId = setInterval(tick, 1000) }
const remainText = computed(() => {
  const s = Math.max(0, remainSec.value)
  return `${Math.floor(s/60)}:${String(s%60).padStart(2,'0')}`
})

/* ---------------------- Leave page: cancel hold ---------------------- */
onBeforeRouteLeave(async (to, from, next) => {
  if (!holdCode.value) return next();
  const goingToResult =
    ['/reservation/payment-success', '/reservation/payment-fail', '/reservation/success', '/reservation/fail']
      .includes(to.path);
  if (goingToResult) return next();

  if (!confirm('이 페이지를 떠나면 예약 홀드가 취소됩니다. 계속 이동할까요?')) return next(false);
  try { await cancelReservationHold(holdCode.value) } catch (e) { console.error(e) }
  next();
})

async function cancelHold() {
  if (!holdCode.value) return
  try {
    await cancelReservationHold(holdCode.value)
    toast('홀드를 취소했습니다.')
    router.replace(hotelId.value ? `/hotels/${hotelId.value}` : '/')
  } catch (e) {
    console.error(e); toast('홀드 취소 실패')
  }
}

/* ---------------------- Price & Coupons ---------------------- */
const price = reactive({
  base: quotedTotal.value ?? 240000,
  taxes: quotedTotal.value ? 0 : 24000,
  fee:   quotedTotal.value ? 0 : 5000,
})

const coupons = ref([])
const couponsLoading = ref(false)
const selectedCoupon = ref(null)

const isServerQuoted = computed(() => quotedTotal.value != null)
const subTotal = computed(() => Number(price.base||0) + Number(price.taxes||0) + Number(price.fee||0))

const discount = computed(() => {
  if (isServerQuoted.value) return 0
  const c = selectedCoupon.value
  if (!c) return 0
  let d = 0
  if (c.type === 'amount') d = c.value
  else if (c.type === 'percent') d = Math.round(price.base * (c.value/100))
  else if (c.type === 'percentCap') { d = Math.round(price.base * (c.value/100)); d = Math.min(d, c.cap||d) }
  return Math.min(d, subTotal.value)
})

const totalRaw = computed(() =>
  isServerQuoted.value ? (Number(quotedTotal.value) || 0) : (subTotal.value - discount.value)
)

const displayDiscount = computed(() => {
  if (isServerQuoted.value) {
    const sub = Number(subTotal.value || 0)
    const tot = Number(quotedTotal.value || 0)
    return Math.max(0, sub - tot)
  }
  return Number(discount.value || 0)
})

const applicableCoupons = computed(() => coupons.value)

const showCouponModal = ref(false)
const openCouponModal  = () => showCouponModal.value = true
const closeCouponModal = () => showCouponModal.value = false

async function handleOpenCoupon() {
  try {
    couponsLoading.value = true
    
   const { data } = await getAvailableCoupons()  // JWT에서 userId 추출
    coupons.value = (data || []).map(c => ({
      code: c.code,
      name: c.title,
      type: 'amount',
      value: c.amount,
      desc: c.expiresAt ? `만료: ${new Date(c.expiresAt).toLocaleDateString('ko-KR')}` : '제한 없음'
    }))
    showCouponModal.value = true
  } catch (e) {
    const status = e?.response?.status
   if (status === 401) toast('로그인 후 쿠폰을 사용할 수 있어요.')
   else toast('쿠폰 조회 실패')
   console.error(e)
  } finally {
    couponsLoading.value = false
  }
}

const applyCouponLocal = async (c) => {
  selectedCoupon.value = c
  closeCouponModal()
  await nextTick()
  await ensureWidgetAmountUpToDate()
}

async function handleApplyCoupon(c) {
  if (!holdCode.value) return toast('홀드 정보가 없습니다.')
  try {
    
    const { data } = await repriceReservationHold(holdCode.value, {couponCode: c?.code ?? null })
    quotedTotal.value   = Number(data.totalAmount ?? data.quotedTotal ?? 0)
    holdExpiresAt.value = data.expiresAt
    selectedCoupon.value = c || null
    await ensureWidgetAmountUpToDate()
    closeCouponModal()
    toast(c ? '쿠폰이 적용되었습니다.' : '쿠폰이 해제되었습니다.')
  } catch (e) {
    const status = e?.response?.status
    const msg = e?.response?.data?.message || e?.message || '알 수 없는 오류'
    console.error('reprice error:', status, e?.response?.data)
    toast(`쿠폰 적용 실패 [${status}] ${msg}`)
  }
}

const removeCoupon = async () => {
  if (!isServerQuoted.value) {
    selectedCoupon.value = null
    await nextTick()
    await ensureWidgetAmountUpToDate()
    return
  }
  await handleApplyCoupon(null)
}

/* ---------------------- Guest & Validation ---------------------- */
const guest    = reactive({ name: '', phone: '' })
const guestErr = reactive({ name: '', phone: '' })
const guestValid = computed(() => {
  const nameOk  = !!guest.name.trim()
  const phoneOk = /^\d{9,12}$/.test(guest.phone.replace(/\D/g,''))
  return nameOk && phoneOk
})
function validateGuest() {
  guestErr.name = guestErr.phone = ''
  if (!guest.name.trim()) guestErr.name = '이름을 입력하세요.'
  if (!/^\d{9,12}$/.test(guest.phone.replace(/\D/g,''))) guestErr.phone = '하이픈 없이 9~12자리 번호'
  return !(guestErr.name || guestErr.phone)
}

/* ---------------------- Stored Cards (demo) ---------------------- */
const cards = ref([{ id:1, brand:'VISA', last4:'4321', exp:'02/27', selected:true }])
const hasCards = computed(() => cards.value.length > 0)
const isCardSelected = computed(() => cards.value.some(c => c.selected))
const selectCard = (id) => cards.value.forEach(c => c.selected = c.id === id)

/* Add card modal (placeholder) */
const showAdd = ref(false)
const saving  = ref(false)
const saveProfile = ref(true)
const newCard = reactive({ number:'', exp:'', cvc:'', name:'', country:'Korea, Republic of' })
const errors  = reactive({ number:'', exp:'', cvc:'', name:'' })
function resetForm(){ newCard.number=''; newCard.exp=''; newCard.cvc=''; newCard.name=''; newCard.country='Korea, Republic of'; errors.number=errors.exp=errors.cvc=errors.name=''; saveProfile.value = true }
const openAddCard  = ()=> { resetForm(); showAdd.value = true }
const closeAddCard = ()=> showAdd.value = false

/* ---------------------- Payment amount (single source of truth) ---------------------- */
function krw(n){ const v = Number(n); return Number.isFinite(v) ? Math.max(0, Math.round(v)) : 0 }
const payAmount = computed(() => krw(quotedTotal.value ?? 0))

/* ---------------------- Toss Payments Widget ---------------------- */
const paymentMethodElId = 'payment-method'
const agreementElId     = 'agreement'

const paymentWidget = ref(null)
const paymentMethodsWidget = ref(null)

let amountSyncSeq = 0
async function ensureWidgetAmountUpToDate() {
  if (!paymentMethodsWidget.value) return
  const value = krw(payAmount.value)
  if (value <= 0) { toast('결제 금액이 0원입니다. 쿠폰/요금 다시 확인해주세요.'); return }
  const mySeq = ++amountSyncSeq
  console.log('[DEBUG] ensureWidgetAmountUpToDate', {
  payAmount: payAmount.value,
  value,
  quotedTotal: quotedTotal.value,
  subTotal: subTotal.value,
  discount: discount.value,
  seq: mySeq
})
  await paymentMethodsWidget.value.updateAmount(value) // 숫자만!
  // out-of-order 방지 목적의 시퀀스. (별도 취소는 불필요 — 이중 동기화가 마무리 보장)
}

async function setupToss() {
  try {
    const clientKey   = import.meta.env.VITE_TOSS_CLIENT_KEY
    const customerKey = 'customer_' + (userId.value || Date.now())
    paymentWidget.value = await loadPaymentWidget(clientKey, customerKey)
    const amt = krw(payAmount.value)
paymentMethodsWidget.value = await paymentWidget.value.renderPaymentMethods(
  '#' + paymentMethodElId,
  amt, // 숫자만!
  { variantKey: 'DEFAULT' }
)
console.debug('[TOSS] renderPaymentMethods amount =', amt)
    await paymentWidget.value.renderAgreement('#' + agreementElId, { variantKey: 'AGREEMENT' })
    await ensureWidgetAmountUpToDate()
  } catch (e) {
    console.error('Toss setup failed:', e)
    alert('결제 위젯 초기화 중 오류가 발생했습니다.\n콘솔 로그를 확인해 주세요.')
  }
}

/* ---------------------- Pay button state ---------------------- */
const isPaying = ref(false)
const canPay = computed(() =>
  !expired.value &&
  guestValid.value &&
  isCardSelected.value &&
  !!paymentMethodsWidget.value &&
  krw(payAmount.value) > 0
)

/* ---------------------- Request payment (double-lock) ---------------------- */
/* ---------------------- Request payment (double-lock + 서버 최신화) ---------------------- */
async function requestPay() {
  if (!canPay.value) return
  isPaying.value = true
  try {
    if (remainSec.value <= 3) {
      toast('만료 임박으로 결제를 시작할 수 없습니다.')
      return
    }

    console.debug('[DEBUG] requestPay start with payAmount =', payAmount.value)

    // ✅ 1) 서버에서 홀드 최신 금액 다시 확인
    try {
      const { data } = await api.get(`/reservations/holds/${holdCode.value}`)
      if (data && data.totalAmount != null) {
        quotedTotal.value = Number(data.totalAmount)
        holdExpiresAt.value = data.expiresAt
        console.debug('[DEBUG] refreshed quotedTotal from server =', quotedTotal.value)
      }
    } catch (err) {
      console.warn('[WARN] getHoldByCode failed, fallback to local quotedTotal', err)
    }

    // ✅ 2) 위젯 금액 강제 동기화 (double sync + 지연)
    await ensureWidgetAmountUpToDate()
    await new Promise(r => setTimeout(r, 200))
    await ensureWidgetAmountUpToDate()

    // ✅ 3) Toss 요청
    const extra = new URLSearchParams({
      holdCode: holdCode.value || '',
      hotelId: String(hotelId.value ?? ''),
      hotelName: hotelName.value ?? '',
      roomTypeName: roomTypeName.value ?? '',
      checkIn: checkIn.value || '',
      checkOut: checkOut.value || '',
      guests: String(guests.value ?? 1),
      guestName: guest.name || ''
    }).toString()

    await paymentWidget.value.requestPayment({
      orderId: genOrderId(),
      orderName: '호텔 예약',
      successUrl: `${window.location.origin}/reservation/success?${extra}`,
      failUrl:    `${window.location.origin}/reservation/fail`,
      customerName: guest.name || '예약고객',
      customerMobilePhone: guest.phone.replace(/\D/g, ''),
      customerEmail: 'guest@example.com',
    })
  } catch (err) {
    console.error('[TOSS][requestPayment] fail (raw):', err)
    const code = (err && (err.code || err.response?.data?.code)) ?? 'UNKNOWN'
    const message = (err && (err.message || err.response?.data?.message)) ?? '알 수 없는 오류'
    console.error('[TOSS][requestPayment] fail (parsed):', { code, message })
    toast(`결제 시작 실패: ${message}`)
  } finally {
    isPaying.value = false
  }
}





/* ---------------------- Mount / Unmount ---------------------- */
onMounted(async () => {
  if (!holdCode.value || !holdExpiresAt.value) {
    alert('예약 홀드 정보가 없습니다. 다시 선택해 주세요.')
    router.replace(hotelId.value ? `/hotels/${hotelId.value}` : '/')
    return
  }
  await syncServerTime()
  startTimer()
  setupToss().catch(console.error)
})
onUnmounted(() => { if (timerId) clearInterval(timerId) })

/* ---------------------- Utils ---------------------- */
const genOrderId = () => {
  const ts = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0,14)
  const rand = Math.random().toString(36).slice(2, 10).toUpperCase()
  const frag = String(holdCode.value || '').replace(/[^A-Z0-9]/gi, '').slice(-4).toUpperCase()
  return frag ? `ORDER-${ts}-${frag}-${rand}` : `ORDER-${ts}-${rand}`
}
</script>

<template>
  <main class="reservation">
    <div class="progress" v-show="isPaying"><div class="bar"></div></div>

    <div class="holdbar" :class="{ danger: expired || remainSec <= 30 }">
      <div class="left">
        <span class="tag">HOLD</span>
        <span class="code">#{{ holdCode }}</span>
        <span class="sep">|</span>
        <span class="ttl" v-if="!expired">만료까지 <b>{{ remainText }}</b></span>
        <span class="ttl expired" v-else>만료됨</span>
      </div>
      <div class="right">
        <button class="btn sm outline" :disabled="isPaying" @click="cancelHold">홀드 취소</button>
      </div>
    </div>

    <section class="grid">
      <article class="card">
        <header class="room-head">
          <h2 class="room-title">예약 진행</h2>
          <strong class="price">
            {{ payAmount.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}
            <span class="unit">총액</span>
          </strong>
        </header>

        <!-- 예약 개요 -->
        <div class="panel">
          <div class="panel-title">예약 개요</div>
          <ul class="facts">
            <li><b>호텔</b><span>{{ hotelName || ('#' + (hotelId ?? '-')) }}</span></li>
            <li><b>객실타입</b><span>{{ roomTypeName || ('#' + (roomTypeId ?? '-')) }}</span></li>
            <li><b>요금제</b><span>{{ ratePlanId ?? '-' }}</span></li>
            <li><b>인원</b><span>{{ guests || 1 }}명</span></li>
            <li><b>체크인</b><span>{{ checkIn || '-' }}</span></li>
            <li><b>체크아웃</b><span>{{ checkOut || '-' }}</span></li>
          </ul>
          <p class="warn" v-if="expired">⚠️ 홀드가 만료되어 결제를 진행할 수 없습니다. 다시 예약을 시도해주세요.</p>
        </div>

        <!-- 쿠폰 -->
        <div class="panel coupon" :class="{ disabled: isServerQuoted }">
          <div class="panel-title">쿠폰 적용하기</div>

          <div class="coupon-row" v-if="selectedCoupon">
            <div class="pill">
              <strong>{{ selectedCoupon.name }}</strong>
              <small class="muted">{{ selectedCoupon.desc }}</small>
            </div>
            <button class="btn sm" @click="removeCoupon">해제</button>
            <button class="btn sm outline" @click="handleOpenCoupon">변경</button>
          </div>

          <div class="coupon-row" v-else>
            <span class="muted">적용된 쿠폰이 없습니다.</span>
            <button class="btn sm outline" @click="handleOpenCoupon">쿠폰 선택</button>
          </div>
        </div>

        <!-- 예약자 정보 -->
        <div class="panel guest">
          <div class="panel-title">예약자 정보</div>
          <div class="row gap">
            <div class="col">
              <label class="label">대표 이름</label>
              <input class="input" :class="{invalid: !!(guestErr && guestErr.name)}"
                     v-model.trim="guest.name" placeholder="홍길동" @blur="validateGuest"/>
              <p class="err" v-if="guestErr && guestErr.name">{{ guestErr.name }}</p>
            </div>
            <div class="col">
              <label class="label">전화번호</label>
              <input class="input" :class="{invalid: !!(guestErr && guestErr.phone)}"
                     v-model.trim="guest.phone" placeholder="01012345678" inputmode="tel" @blur="validateGuest"/>
              <p class="err" v-if="guestErr && guestErr.phone">{{ guestErr.phone }}</p>
            </div>
          </div>
          <p class="hint">체크인 안내 및 비상 연락용으로만 사용됩니다.</p>
        </div>

        <!-- 카드 선택 -->
        <div class="panel">
          <div class="panel-title">카드 결제</div>
          <div v-if="hasCards" class="card-list">
            <button v-for="c in cards" :key="c.id" class="card-row" :class="{active:c.selected}" @click="selectCard(c.id)">
              <span class="brand">{{ c.brand }}</span>
              <span class="mask">**** {{ c.last4 }}</span>
              <span class="exp">{{ c.exp }}</span>
              <span class="radio" aria-hidden="true"></span>
            </button>
          </div>
          <button class="add-slot" @click="openAddCard"><span class="plus">+</span><span>Add a new card</span></button>
        </div>

        <!-- Toss 결제 위젯 -->
        <div class="panel">
          <div class="panel-title">결제하기</div>
          <div id="payment-method" style="margin-top:10px"></div>
          <div id="agreement" style="margin-top:10px"></div>
          <button class="btn primary" style="margin-top:12px"
                  :disabled="!canPay || isPaying"
                  @click="requestPay">
            <span v-if="!isPaying">
              {{ expired ? '만료됨' : `₩${(payAmount ?? 0).toLocaleString('ko-KR') } 결제하기` }}
            </span>
            <span v-else class="spinner" aria-label="처리중"></span>
          </button>
        </div>
      </article>

      <!-- 우측 요약 -->
      <aside class="summary card">
        <div class="sum-head">
          <img class="thumb" src="https://images.unsplash.com/photo-1560066984-138dadb4c035?w=400&q=60" alt="" />
          <div class="sum-txt">
            <div class="sum-hotel">호텔 #{{ hotelId ?? '-' }}</div>
            <div class="sum-room">룸타입 #{{ roomTypeId ?? '-' }}</div>
          </div>
        </div>

        <dl class="kv">
          <div class="row"><dt>Base Fare</dt><dd>{{ price.base.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd></div>
          <div class="row"><dt>Discount</dt>
            <dd v-if="displayDiscount">-{{ displayDiscount.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd>
            <dd v-else>₩0</dd>
          </div>
          <div class="row"><dt>Taxes</dt><dd>{{ price.taxes.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd></div>
          <div class="row"><dt>Service Fee</dt><dd>{{ price.fee.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd></div>
          <div class="row total"><dt>Total</dt><dd>{{ payAmount.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd></div>
        </dl>
      </aside>
    </section>

    <!-- 쿠폰 선택 모달 -->
    <div v-if="showCouponModal" class="overlay" @click.self="closeCouponModal">
      <div class="modal">
        <div class="modal-head">
          <h3>사용 가능한 쿠폰</h3>
          <button class="x" @click="closeCouponModal" aria-label="닫기">×</button>
        </div>

        <div v-if="couponsLoading" class="empty">불러오는 중…</div>

        <div v-else-if="applicableCoupons.length" class="coupon-list">
          <button v-for="c in applicableCoupons" :key="c.code" class="coupon-item"
                  @click="isServerQuoted ? handleApplyCoupon(c) : applyCouponLocal(c)">
            <div class="left">
              <div class="cname">{{ c.name }}</div>
              <div class="cdesc">{{ c.desc }}</div>
            </div>
            <div class="right"><span class="apply">적용</span></div>
          </button>
        </div>

        <div v-else class="empty">적용 가능한 쿠폰이 없습니다.</div>
      </div>
    </div>

    <!-- 만료 안내 모달 -->
    <div v-if="showExpireModal" class="overlay" @click.self="showExpireModal=false">
      <div class="modal">
        <div class="modal-head">
          <h3>홀드가 만료되었습니다</h3>
          <button class="x" @click="showExpireModal=false" aria-label="닫기">×</button>
        </div>
        <p>다시 날짜를 선택해 예약을 진행해주세요.</p>
        <button class="btn sm" @click="router.replace(hotelId ? `/hotels/${hotelId}` : '/')">다시 예약하기</button>
      </div>
    </div>

    <!-- 카드 추가 모달(placeholder) -->
    <div v-if="showAdd" class="overlay" @click.self="closeAddCard">
      <div class="modal">
        <div class="modal-head">
          <h3>카드추가</h3>
          <button class="x" @click="closeAddCard" aria-label="닫기">×</button>
        </div>
        <div class="form"><!-- 생략 --></div>
      </div>
    </div>

    <!-- 토스트 -->
    <div class="toast-wrap">
      <div v-for="t in toasts" :key="t.id" class="toast">{{ t.message }}</div>
    </div>
  </main>
</template>

<style scoped>
.progress{position:sticky;top:0;z-index:40;height:3px;background:transparent}
.progress .bar{height:3px;width:100%;background:linear-gradient(90deg,#60a5fa,#2563eb);animation:load 1.2s linear infinite}
@keyframes load{0%{transform:translateX(-100%)}100%{transform:translateX(100%)}}
.holdbar{position:sticky;top:3px;z-index:30;display:flex;align-items:center;justify-content:space-between;padding:10px 12px;margin:-8px 0 12px;background:#0f172a;color:#e5edff;border-radius:10px;border:1px solid #1f2a44}
.holdbar .tag{font-weight:900;font-size:12px;padding:4px 6px;border-radius:6px;background:#1d4ed8;margin-right:6px}
.holdbar .code{font-weight:800}.holdbar .sep{opacity:.5;margin:0 10px}
.holdbar .ttl b{font-variant-numeric:tabular-nums}.holdbar .ttl.expired{color:#fca5a5;font-weight:800}
.holdbar.danger{background:#111827;border-color:#4b5563}
:root{--bg:#f5f8ff;--card:#ffffff;--line:#e6eef8;--txt:#0f172a;--muted:#6b7280;--primary:#2563eb;--primary-2:#1d4ed8;--accent:#60a5fa}
.reservation{width:100%;min-height:100vh;margin:0;padding:24px clamp(12px,2.5vw,36px);box-sizing:border-box;background:var(--bg)}
.reservation .grid{display:grid;grid-template-columns:minmax(680px,3.2fr) minmax(420px,1.2fr);gap:clamp(20px,2vw,36px);align-items:start}
@media (min-width:1400px){.reservation .grid{grid-template-columns:3.6fr 1fr}}
@media (min-width:1800px){.reservation .grid{grid-template-columns:4fr 1fr}}
@media (max-width:960px){.reservation .grid{grid-template-columns:1fr}}
.card,.summary{width:100%;background:var(--card);border:1px solid var(--line);border-radius:16px;padding:18px;color:var(--txt);box-shadow:0 8px 24px rgba(15,23,42,.06)}
.room-head{display:flex;align-items:flex-start;gap:12px;justify-content:space-between}.room-title{margin:0;font-size:22px;font-weight:800;color:var(--txt);line-height:1.2}
.price .unit{font-size:.8em;color:var(--muted);margin-left:4px}
.panel{border:1px solid var(--line); border-radius:12px; padding:14px; margin:10px 0; background:#f8fbff}
.panel-title{font-weight:800;color:var(--txt)}
.facts{display:grid;grid-template-columns:1fr 1fr;gap:8px;list-style:none;padding:0;margin:10px 0 0}
.facts li{display:flex;justify-content:space-between;padding:8px 12px;border:1px dashed #dbe8ff;border-radius:10px;background:#fff}
.warn{margin-top:8px;color:#b91c1c;font-weight:700}
.panel.coupon{background:#f1f7ff;border-color:#dbe8ff}
.coupon-row{display:flex;align-items:center;gap:10px;justify-content:space-between;margin-top:8px}
.pill{display:flex;align-items:center;gap:8px;padding:8px 12px;border-radius:999px;background:#e8f0ff;border:1px solid #d5e5ff;color:#1e3a8a}
.muted{color:#64748b}
.btn{height:44px;padding:0 16px;border-radius:12px;border:1px solid transparent;cursor:pointer;font-weight:800;display:inline-flex;align-items:center;justify-content:center}
.btn.sm{height:34px;padding:0 12px;border-radius:10px;background:var(--primary);color:#fff}
.btn.sm.outline{background:#fff;color:#1e40af;border-color:#cfe0ff}
.btn.primary{margin-top:6px;background:var(--primary);color:#4368cc;border-color:var(--primary)}
.btn.primary:hover{background:var(--primary-2)}
.panel.guest .label{font-size:14px;color:#1e3a8a;font-weight:700}
.row.gap{display:flex;gap:12px}.col{flex:1 1 0}
.input{width:100%;height:44px;border-radius:10px;border:1px solid #d5e3fb;padding:0 12px;font-size:15px;outline:none;background:#fff;color:#0f172a}
.input:focus{box-shadow:0 0 0 3px rgba(37,99,235,.25);border-color:var(--primary)}
.input.invalid{border-color:#fca5a5;box-shadow:0 0 0 3px rgba(244,63,94,.18)}
.err{color:#e11d48;font-size:12px;margin-top:4px}
.hint{margin:6px 0 0;color:#64748b;font-size:12px}
.card-list{display:grid;gap:10px;margin-top:8px}
.card-row{display:flex;align-items:center;gap:10px;padding:12px 14px;border-radius:10px;border:2px solid #dfe9ff;background:#f6f9ff;position:relative;cursor:pointer}
.card-row .brand{font-weight:900;font-size:12px;padding:4px 6px;border-radius:6px;background:#1f2937;color:#fff}
.card-row .mask{color:#0f172a;font-weight:700;margin-left:6px}
.card-row .exp{color:#64748b;font-size:13px;margin-left:auto;margin-right:28px}
.card-row .radio{position:absolute;right:10px;top:50%;transform:translateY(-50%);width:18px;height:18px;border-radius:50%;border:2px solid #3b82f6;background:#e8f0ff}
.card-row.active{background:#e7f0ff;border-color:#5aa0ff}
.add-slot{width:100%;margin-top:10px;padding:20px;border:2px dashed #cfe0ff;border-radius:10px;background:#fff;color:#6b7280;display:flex;flex-direction:column;align-items:center;gap:6px;cursor:pointer}
.add-slot .plus{font-size:22px;line-height:1;color:#3b82f6}
.summary .sum-head{display:flex;gap:12px;align-items:center;margin-bottom:12px}
.summary .thumb{width:64px;height:64px;object-fit:cover;border-radius:8px}
.sum-hotel{font-weight:700;color:var(--txt)}.sum-room{color:#64748b;font-size:13px}
.kv{margin:8px 0 0}.kv .row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #e6eef8}
.kv .row:last-child{border-bottom:0}
.kv .row.total dt{font-weight:800}.kv .row.total dd{font-weight:900;color:#0f172a}
.overlay{position:fixed;inset:0;background:rgba(0,0,0,.35);display:grid;place-items:center;z-index:50}
.modal{width:min(720px,94vw);background:#fff;border:1px solid #e6eef8;color:#0f172a;border-radius:16px;box-shadow:0 30px 80px rgba(0,0,0,.15);padding:22px 22px 26px}
.modal-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}
.modal-head h3{margin:0;font-size:20px;font-weight:800}
.x{background:transparent;border:0;font-size:24px;color:#334155;cursor:pointer;line-height:1}
.coupon-list{display:grid;gap:10px}
.coupon-item{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:14px 16px;border-radius:12px;border:1px solid #dfe9ff;background:#f6f9ff;cursor:pointer}
.cname{font-weight:800;color:#0f172a}.cdesc{color:#64748b;font-size:13px}
.apply{font-weight:800;color:#1d4ed8}
.empty{text-align:center;color:#64748b;padding:18px 6px}
.spinner{display:inline-block;width:18px;height:18px;border:2px solid rgba(0,0,0,.15);border-top-color:#0f172a;border-radius:50%;animation:spin 1s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
.toast-wrap{position:fixed;left:50%;bottom:18px;transform:translateX(-50%);z-index:60;display:flex;flex-direction:column;gap:8px}
.toast{background:#0f172a;color:#e5edff;border:1px solid #253357;padding:10px 14px;border-radius:10px;box-shadow:0 8px 24px rgba(0,0,0,.25);font-weight:700}
</style>

<style>
html body{display:block !important;place-items:unset !important;}
#app{max-width:none !important;width:100% !important;margin:0 !important;padding:0 !important;display:block !important;grid-template-columns:none !important;}
</style>

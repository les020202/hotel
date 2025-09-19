<!-- src/views/reservation/ReservationPage.vue -->
<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { cancelReservationHold } from '@/api/reservation'
import { loadPaymentWidget } from '@tosspayments/payment-widget-sdk' // ✅ 위젯 SDK

/* ---------------------- 라우팅 & 쿼리 파라미터 ---------------------- */
const route  = useRoute()
const router = useRouter()

const holdCode      = ref(String(route.query.holdCode || ''))
const holdExpiresAt = ref(String(route.query.expiresAt || route.query.holdExpiresAt || ''))
const hotelId       = ref(route.query.hotelId ? Number(route.query.hotelId) : null)
const roomTypeId    = ref(route.query.roomTypeId ? Number(route.query.roomTypeId) : null)
const ratePlanId    = ref(route.query.ratePlanId ? Number(route.query.ratePlanId) : 1) // 안전 기본값
const userId        = ref(route.query.userId ? Number(route.query.userId) : null)
const guests        = ref(route.query.guests ? Number(route.query.guests) : 1)
const checkIn       = ref(String(route.query.checkIn || ''))
const checkOut      = ref(String(route.query.checkOut || ''))
const hotelName     = ref(String(route.query.hotelName || ''))
const roomTypeName  = ref(String(route.query.roomTypeName || ''))
const quotedTotal   = ref(route.query.totalAmount ? Number(route.query.totalAmount) : null)

/* ---------------------- 타이머(만료 카운트다운) ---------------------- */
const remainSec = ref(0)
const expired   = computed(() => remainSec.value <= 0)
let timerId = null
function tick () {
  const expMs = Date.parse(holdExpiresAt.value)
  remainSec.value = Number.isNaN(expMs) ? 0 : Math.max(0, Math.floor((expMs - Date.now()) / 1000))
  if (remainSec.value === 0 && timerId) { clearInterval(timerId); timerId = null }
}
function startTimer() { tick(); timerId = setInterval(tick, 1000) }

onMounted(() => {
  if (!holdCode.value || !holdExpiresAt.value) {
    alert('예약 홀드 정보가 없습니다. 다시 선택해 주세요.')
    router.replace(hotelId.value ? `/hotels/${hotelId.value}` : '/')
    return
  }
  startTimer()
})
onUnmounted(() => { if (timerId) clearInterval(timerId) })
const remainText = computed(() => {
  const s = Math.max(0, remainSec.value)
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
})

/* ---------------------- 뒤로가기/페이지 이탈 시 홀드 취소 ---------------------- */
onBeforeRouteLeave(async (to, from, next) => {
  if (!holdCode.value) return next()
  if (!confirm('이 페이지를 떠나면 예약 홀드가 취소됩니다. 계속 이동할까요?')) return next(false)
  try { await cancelReservationHold(holdCode.value) } catch(e){ console.error(e) }
  next()
})

/* ---------------------- 홀드 수동 취소 버튼 ---------------------- */
const cancelling = ref(false)
async function cancelHold() {
  if (!holdCode.value) return
  if (!confirm('현재 예약 홀드를 취소할까요?')) return
  try {
    cancelling.value = true
    await cancelReservationHold(holdCode.value)
    holdCode.value = ''
    const hotelIdNum = Number(route.query.hotelId || route.params.id)
    const qCheckIn   = String(route.query.checkIn || checkIn.value || '')
    const qCheckOut  = String(route.query.checkOut || checkOut.value || '')
    const guestsN    = Number(route.query.guests || guests.value || 1)
    router.replace({ name: 'hotel-detail', params: { id: hotelIdNum }, query: { checkIn: qCheckIn, checkOut: qCheckOut, guests: guestsN } })
  } catch (e) {
    console.error(e); alert(e?.response?.data?.message ?? '홀드 취소에 실패했습니다.')
  } finally { cancelling.value = false }
}

/* ---------------------- 가격/쿠폰 ---------------------- */
const price = reactive({ base: quotedTotal.value ?? 240000, taxes: quotedTotal.value ? 0 : 24000, fee: quotedTotal.value ? 0 : 5000 })
const coupons = ref([
  { id:'C10',  name:'10% 할인', type:'percent', value:10, minSpend:100000, desc:'10만원 이상 10%' },
  { id:'F15',  name:'₩15,000 즉시 할인', type:'amount',  value:15000, minSpend:0, desc:'제한 없음' },
  { id:'WKND', name:'주말 20% (최대 ₩30,000)', type:'percentCap', value:20, cap:30000, minSpend:150000, desc:'15만원 이상, 최대 3만원' },
])
const selectedCoupon    = ref(null)
const subTotal          = computed(()=> price.base + price.taxes + price.fee)
const discount          = computed(()=>{
  const c = selectedCoupon.value
  if(!c) return 0
  let d = 0
  if (c.type==='amount') d = c.value
  else if (c.type==='percent') d = Math.round(price.base * (c.value/100))
  else if (c.type==='percentCap') { d = Math.round(price.base * (c.value/100)); d = Math.min(d, c.cap||d) }
  return Math.min(d, subTotal.value)
})
const total             = computed(()=> subTotal.value - discount.value)
const applicableCoupons = computed(()=> coupons.value.filter(c => price.base >= (c.minSpend||0)))
const showCouponModal   = ref(false)
const openCouponModal   = ()=> showCouponModal.value = true
const closeCouponModal  = ()=> showCouponModal.value = false
const applyCoupon       = (c)=>{ selectedCoupon.value = c; closeCouponModal() }
const removeCoupon      = ()=> { selectedCoupon.value = null }

/* ---------------------- 예약자 정보 ---------------------- */
const guest    = reactive({ name: '', phone: '' })
const guestErr = reactive({ name: '', phone: '' })
function validateGuest() {
  guestErr.name = guestErr.phone = ''
  if (!guest.name.trim()) guestErr.name = '이름을 입력하세요.'
  if (!/^\d{9,12}$/.test(guest.phone.replace(/\D/g,''))) guestErr.phone = '하이픈 없이 9~12자리 번호'
  return !(guestErr.name || guestErr.phone)
}

/* ---------------------- (샘플) 저장된 카드/카드 추가 ---------------------- */
const cards = ref([{ id:1, brand:'VISA', last4:'4321', exp:'02/27', selected:true }])
const hasCards = computed(()=> cards.value.length>0)
const selectCard = (id)=> cards.value.forEach(c=>c.selected = c.id===id)

const showAdd = ref(false)
const saving  = ref(false)
const saveProfile = ref(true)
const newCard = reactive({ number:'', exp:'', cvc:'', name:'', country:'Korea, Republic of' })
const errors  = reactive({ number:'', exp:'', cvc:'', name:'' })

function resetForm(){
  newCard.number=''; newCard.exp=''; newCard.cvc=''; newCard.name=''; newCard.country='Korea, Republic of'
  errors.number=errors.exp=errors.cvc=errors.name=''
  saveProfile.value = true
}
const openAddCard  = ()=> { resetForm(); showAdd.value = true }
const closeAddCard = ()=> showAdd.value = false

const formatCardNumber = v => v.replace(/\D/g,'').slice(0,16).replace(/(\d{4})(?=\d)/g,'$1 ')
const onNumberInput = e => e.target.value = (newCard.number = formatCardNumber(e.target.value))
const onExpInput    = e => { let v = e.target.value.replace(/\D/g,'').slice(0,4); if(v.length>=3) v=v.slice(0,2)+'/'+v.slice(2); e.target.value = (newCard.exp = v) }
const onCvcInput    = e => e.target.value = (newCard.cvc = e.target.value.replace(/\D/g,'').slice(0,4))

function detectBrand(num){
  const n = num.replace(/\s/g,'')
  if(/^4/.test(n)) return 'VISA'
  if(/^5[1-5]/.test(n)) return 'Mastercard'
  if(/^3[47]/.test(n)) return 'AMEX'
  if(/^6(?:011|5)/.test(n)) return 'Discover'
  return 'CARD'
}
function validateCard(){
  errors.number=errors.exp=errors.cvc=errors.name=''
  const digits = newCard.number.replace(/\s/g,'')
  if(!/^\d{16}$/.test(digits)) errors.number='카드 번호 16자리를 입력하세요.'
  const m = newCard.exp.match(/^(\d{2})\/(\d{2})$/)
  if(!m) errors.exp='MM/YY 형식으로 입력하세요.'
  else { const mm=+m[1]; if(mm<1||mm>12) errors.exp='월(MM)이 올바르지 않습니다.' }
  if(!/^\d{3,4}$/.test(newCard.cvc)) errors.cvc='CVC 3~4자리를 입력하세요.'
  if(!newCard.name.trim()) errors.name='카드 소유자 이름을 입력하세요.'
  return !(errors.number||errors.exp||errors.cvc||errors.name)
}
async function submitCard(){
  if(!validateCard()) return
  saving.value = true
  try{
    const id = Date.now()
    const last4 = newCard.number.replace(/\D/g,'').slice(-4)
    const brand = detectBrand(newCard.number)
    cards.value = cards.value.map(c=>({ ...c, selected:false }))
    cards.value.unshift({ id, brand, last4, exp:newCard.exp, selected:true })
    showAdd.value = false
  } finally { saving.value = false }
}

/* ---------------------- Toss 결제 위젯 ---------------------- */
const paymentMethodElId = 'payment-method'
const agreementElId     = 'agreement'

let paymentWidget = null
let paymentMethodsWidget = null

async function setupToss() {
  try {
    const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY
    const customerKey = 'customer_' + (userId.value || Date.now())
    console.log('[TOSS] clientKey =', clientKey)
    console.log('[TOSS] customerKey =', customerKey)

    paymentWidget = await loadPaymentWidget(clientKey, customerKey)

    // 결제수단 위젯
    paymentMethodsWidget = await paymentWidget.renderPaymentMethods(
      '#' + paymentMethodElId,
      { value: total.value, currency: 'KRW' },
      { variantKey: 'DEFAULT' }
    )

    // 약관 위젯
    await paymentWidget.renderAgreement(
      '#' + agreementElId,
      { variantKey: 'AGREEMENT' }
    )
  } catch (e) {
    console.error('Toss setup failed:', e)
    alert('결제 위젯 초기화 중 오류가 발생했습니다.\n콘솔 로그를 확인해 주세요.')
  }
}

async function requestPay() {
  if (expired.value) {
    alert('홀드가 만료되었습니다. 처음 화면으로 돌아가 다시 예약해주세요.')
    return
  }
  if (!validateGuest()) return
  if (!paymentWidget) { alert('결제 위젯이 아직 준비되지 않았습니다.'); return }

  await paymentWidget.requestPayment({
    orderId: genOrderId(),
    orderName: '호텔 예약',
    successUrl: `${window.location.origin}/reservation/success?holdCode=${encodeURIComponent(holdCode.value)}`,
    failUrl:    `${window.location.origin}/reservation/fail?holdCode=${encodeURIComponent(holdCode.value)}`,
    customerName: guest.name || '예약고객',
    customerMobilePhone: guest.phone.replace(/\D/g, ''),
    customerEmail: 'guest@example.com'
  })
}

/* 금액 변경 시 위젯 금액 동기화 */
watch(total, async (v) => {
  if (paymentMethodsWidget) {
    try {
      await paymentMethodsWidget.updateAmount({ value: v, currency: 'KRW' })
    } catch (e) {
      console.warn('amount 업데이트 실패:', e)
    }
  }
})

onMounted(() => { setupToss().catch(console.error) })

/* 유틸 */
const genOrderId = ()=> 'ORDER-' + Math.random().toString(36).slice(2,10).toUpperCase()
</script>

<template>
  <main class="reservation">
    <!-- 상단 고정바 -->
    <div class="holdbar" :class="{ danger: expired || remainSec <= 30 }">
      <div class="left">
        <span class="tag">HOLD</span>
        <span class="code">#{{ holdCode }}</span>
        <span class="sep">|</span>
        <span class="ttl" v-if="!expired">만료까지 <b>{{ remainText }}</b></span>
        <span class="ttl expired" v-else>만료됨</span>
      </div>
      <div class="right">
        <button class="btn sm outline" :disabled="cancelling" @click="cancelHold">
          {{ cancelling ? '취소 중…' : '홀드 취소' }}
        </button>
      </div>
    </div>

    <section class="grid">
      <article class="card">
        <header class="room-head">
          <h2 class="room-title">예약 진행</h2>
          <strong class="price">
            {{ total.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}
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
            <li><b>체크인</b><span>{{ checkIn || '-' }}</span></li>
            <li><b>체크아웃</b><span>{{ checkOut || '-' }}</span></li>
            <li><b>인원</b><span>{{ guests || 1 }}명</span></li>
          </ul>
          <p class="warn" v-if="expired">⚠️ 홀드가 만료되어 결제를 진행할 수 없습니다. 다시 예약을 시도해주세요.</p>
        </div>

        <!-- 쿠폰 -->
        <div class="panel coupon">
          <div class="panel-title">쿠폰 적용하기</div>
          <div class="coupon-row" v-if="selectedCoupon">
            <div class="pill">
              <strong>{{ selectedCoupon.name }}</strong>
              <small class="muted">· {{ selectedCoupon.desc }}</small>
            </div>
            <button class="btn sm" @click="removeCoupon">해제</button>
            <button class="btn sm outline" @click="openCouponModal">변경</button>
          </div>
          <div class="coupon-row" v-else>
            <span class="muted">적용된 쿠폰이 없습니다.</span>
            <button class="btn sm outline" @click="openCouponModal">쿠폰 선택</button>
          </div>
        </div>

        <!-- 예약자 정보 -->
        <div class="panel guest">
          <div class="panel-title">예약자 정보</div>
          <div class="row gap">
            <div class="col">
              <label class="label">대표 이름</label>
              <input class="input"
                     :class="{invalid: !!(guestErr && guestErr.name)}"
                     v-model.trim="guest.name" placeholder="홍길동" />
              <p class="err" v-if="guestErr && guestErr.name">{{ guestErr.name }}</p>
            </div>
            <div class="col">
              <label class="label">전화번호</label>
              <input class="input"
                     :class="{invalid: !!(guestErr && guestErr.phone)}"
                     v-model.trim="guest.phone" placeholder="01012345678" inputmode="tel" />
              <p class="err" v-if="guestErr && guestErr.phone">{{ guestErr.phone }}</p>
            </div>
          </div>
          <p class="hint">체크인 안내 및 비상 연락용으로만 사용됩니다.</p>
        </div>

        <!-- 카드 결제(보유 카드 표시) -->
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

        <!-- 토스 결제 위젯 -->
        <div class="panel">
          <div class="panel-title">결제하기</div>
          <div id="payment-method" style="margin-top:10px"></div>
          <div id="agreement" style="margin-top:10px"></div>
          <button class="btn primary" style="margin-top:12px" :disabled="expired" @click="requestPay">
            {{ expired ? '만료됨' : '결제 진행' }}
            (총액 {{ total.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }})
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
            <dd v-if="discount">-{{ discount.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd>
            <dd v-else>₩0</dd>
          </div>
          <div class="row"><dt>Taxes</dt><dd>{{ price.taxes.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd></div>
          <div class="row"><dt>Service Fee</dt><dd>{{ price.fee.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd></div>
          <div class="row total"><dt>Total</dt><dd>{{ total.toLocaleString('ko-KR',{style:'currency',currency:'KRW'}) }}</dd></div>
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
        <div v-if="applicableCoupons.length" class="coupon-list">
          <button v-for="c in applicableCoupons" :key="c.id" class="coupon-item" @click="applyCoupon(c)">
            <div class="left"><div class="cname">{{ c.name }}</div><div class="cdesc">{{ c.desc }}</div></div>
            <div class="right"><span class="apply">적용</span></div>
          </button>
        </div>
        <div v-else class="empty">적용 가능한 쿠폰이 없습니다.</div>
      </div>
    </div>

    <!-- 카드 추가 모달 -->
    <div v-if="showAdd" class="overlay" @click.self="closeAddCard">
      <div class="modal">
        <div class="modal-head">
          <h3>카드추가</h3>
          <button class="x" @click="closeAddCard" aria-label="닫기">×</button>
        </div>
        <div class="form">
          <label class="label">Card Number</label>
          <div class="rel">
            <input class="input" placeholder="4321 4321 4321 4321" :class="{invalid:errors.number}" v-model="newCard.number" @input="onNumberInput"/>
            <span class="brandchip">{{ detectBrand(newCard.number) }}</span>
          </div>
          <p class="err" v-if="errors.number">{{ errors.number }}</p>

          <div class="row gap">
            <div class="col">
              <label class="label">Exp. Date</label>
              <input class="input" placeholder="MM/YY" :class="{invalid:errors.exp}" v-model="newCard.exp" @input="onExpInput"/>
              <p class="err" v-if="errors.exp">{{ errors.exp }}</p>
            </div>
            <div class="col">
              <label class="label">CVC</label>
              <input class="input" placeholder="123" :class="{invalid:errors.cvc}" v-model="newCard.cvc" @input="onCvcInput"/>
              <p class="err" v-if="errors.cvc">{{ errors.cvc }}</p>
            </div>
          </div>

          <label class="label">Name on Card</label>
          <input class="input" placeholder="John Doe" :class="{invalid:errors.name}" v-model.trim="newCard.name"/>
          <p class="err" v-if="errors.name">{{ errors.name }}</p>

          <label class="label">Country or Region</label>
          <select class="input" v-model="newCard.country">
            <option>Korea, Republic of</option>
            <option>United States</option>
            <option>Japan</option>
            <option>China</option>
            <option>Canada</option>
          </select>

          <label class="remember"><input type="checkbox" v-model="saveProfile"/><span>정보 저장하기</span></label>

          <button class="btn add" :disabled="saving" @click="submitCard">
            <span v-if="!saving">Add Card</span>
            <span v-else class="spinner" aria-label="저장중"></span>
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.holdbar{position:sticky;top:0;z-index:30;display:flex;align-items:center;justify-content:space-between;padding:10px 12px;margin:-8px 0 12px;background:#0f172a;color:#e5edff;border-radius:10px;border:1px solid #1f2a44}
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
.price{color:#f43f5e;font-weight:800}.price .unit{font-size:.8em;color:var(--muted);margin-left:4px}
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
.card-row{display:flex;align-items:center;gap:10px;padding:12px 14px;border-radius:10px;border:1px solid #dfe9ff;background:#f6f9ff;position:relative;cursor:pointer}
.card-row .brand{font-weight:900;font-size:12px;padding:4px 6px;border-radius:6px;background:#1f2937;color:#fff}
.card-row .mask{color:#0f172a;font-weight:700;margin-left:6px}
.card-row .exp{color:#64748b;font-size:13px;margin-left:auto;margin-right:28px}
.card-row .radio{position:absolute;right:10px;top:50%;transform:translateY(-50%);width:18px;height:18px;border-radius:50%;border:2px solid #3b82f6;background:#e8f0ff}
.card-row.active{background:#e7f0ff;border-color:#bfd7ff}
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
.form{display:grid;gap:10px;margin-top:10px}
.label{font-size:14px;color:#1e3a8a;font-weight:700}
.rel{position:relative}.brandchip{position:absolute;right:10px;top:50%;transform:translateY(-50%);font-size:12px;font-weight:800;background:#eef4ff;color:#1e293b;padding:4px 6px;border-radius:6px}
.btn.add{width:100%;height:48px;background:#2563eb;color:#fff;font-weight:800;border:1px solid #1d4ed8;border-radius:10px}
.spinner{display:inline-block;width:18px;height:18px;border:2px solid rgba(0,0,0,.15);border-top-color:#0f172a;border-radius:50%;animation:spin 1s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
</style>

<!-- #app 제한 해제는 유지 -->
<style>
html body{display:block !important;place-items:unset !important;}
#app{max-width:none !important;width:100% !important;margin:0 !important;padding:0 !important;display:block !important;grid-template-columns:none !important;}
</style>

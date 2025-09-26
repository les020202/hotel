<!-- src/views/reservation/PaymentSuccess.vue -->
<template>
  <div class="container">
    <div class="box">
      <img
        width="90"
        src="https://static.toss.im/illusts/check-blue-spot-ending-frame.png"
        alt="결제 완료"
      />
      <h2>결제를 완료했어요 🎉</h2>
      <p v-if="confirming" class="muted">서버에 결제 승인 확인 중…</p>

      <!-- 결제 요약 -->
      <section class="grid">
        <div><b>결제금액</b></div>
        <div class="tr">{{ amountDisplay }}</div>

        <div><b>주문번호</b></div>
        <div class="tr">{{ orderId }}</div>

        <template v-if="resp?.receiptUrl">
          <div><b>영수증</b></div>
          <div class="tr">
            <a :href="resp.receiptUrl" target="_blank" rel="noopener">영수증 보기</a>
          </div>
        </template>

        <template v-if="resp?.bookingId">
          <div><b>예약번호</b></div>
          <div class="tr">#{{ resp.bookingId }}</div>
        </template>
      </section>

      <!-- 예약 정보 (우리가 successUrl에 실어 보낸 요약) -->
      <h3 class="mt">예약 정보</h3>
      <section class="grid">
        <div><b>호텔</b></div>
        <div class="tr">{{ hotelName || ('#' + (hotelId ?? '-')) }}</div>

        <div><b>객실타입</b></div>
        <div class="tr">{{ roomTypeName || '-' }}</div>

        <div><b>체크인</b></div>
        <div class="tr">{{ checkIn }}</div>

        <div><b>체크아웃</b></div>
        <div class="tr">
          {{ checkOut }}
          <span v-if="nights">({{ nights }}박)</span>
        </div>

        <div><b>인원</b></div>
        <div class="tr">{{ guests }}명</div>
      </section>

      <!-- (옵션) 디버그 JSON -->
      <details class="mt">
        <summary>디버그 정보</summary>
        <div class="kv"><b>paymentKey</b> <code>{{ paymentKey }}</code></div>
        <pre>{{ prettyJson }}</pre>
      </details>

      <div class="btns">
        <button class="btn" type="button" :disabled="confirming" @click="router.push('/')">처음으로</button>
        <button class="btn ghost" type="button" :disabled="confirming" @click="router.push('/mypage/history')">
          예약내역으로
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { confirmPayment } from '@/api/payments'

const route = useRoute()
const router = useRouter()

/* --------- Toss query params --------- */
const paymentKey = ref('')
const orderId    = ref('')
const amount     = ref(0)

/* --------- Our extra params (successUrl에 실어 보냄) --------- */
const holdCode     = ref('')
const hotelId      = ref(null)
const hotelName    = ref('')
const roomTypeName = ref('')
const checkIn      = ref('')
const checkOut     = ref('')
const guests       = ref(1)
const guestName    = ref('')

/* --------- Server response --------- */
const resp        = ref(null)
const confirming  = ref(false)
let confirmedOnce = false

/* --------- UI computed --------- */
const amountDisplay = computed(() =>
  amount.value ? `${Number(amount.value).toLocaleString('ko-KR')}원` : ''
)
const prettyJson = computed(() =>
  resp.value ? JSON.stringify(resp.value, null, 2) : ''
)
const nights = computed(() => {
  const a = Date.parse(checkIn.value), b = Date.parse(checkOut.value)
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0
  return Math.round((b - a) / 86400000)
})

function readParams () {
  // Toss가 successUrl에 붙여준 표준 파라미터
  paymentKey.value = String(route.query.paymentKey || '')
  orderId.value    = String(route.query.orderId    || '')
  amount.value     = Number(route.query.amount     || 0)

  // 우리가 successUrl에 실어 보낸 요약 값
  holdCode.value     = String(route.query.holdCode     || localStorage.getItem('holdCode') || '')
  hotelId.value      = route.query.hotelId ? Number(route.query.hotelId) : null
  hotelName.value    = String(route.query.hotelName    || '')
  roomTypeName.value = String(route.query.roomTypeName || '')
  checkIn.value      = String(route.query.checkIn      || '')
  checkOut.value     = String(route.query.checkOut     || '')
  guests.value       = route.query.guests ? Number(route.query.guests) : 1
  guestName.value    = String(route.query.guestName    || '')
}

async function confirmOnServer () {
  if (confirmedOnce) return
  confirmedOnce = true

  readParams()

  // 필수 파라미터 체크
  if (!paymentKey.value || !orderId.value || !Number.isFinite(amount.value) || amount.value <= 0) {
    router.replace({
      path: '/reservation/fail',
      query: { message: '필수 결제 파라미터가 없습니다.', code: 'MISSING_PARAMS', ...(holdCode.value && { holdCode: holdCode.value }) }
    })
    return
  }

  try {
    confirming.value = true

    // ⚠️ 서버 시큐리티에 허용된 경로와 반드시 동일해야 함
    // SecurityConfig: .requestMatchers(HttpMethod.POST, "/api/payments/toss/confirm").permitAll()
    const data = await confirmPayment({
      paymentKey: paymentKey.value,
      orderId:    orderId.value,
      amount:     Number(amount.value),
      holdCode:   holdCode.value || undefined
    })

    resp.value = data

    // ✅ 브라우저 alert로 알림
    window.alert('예약이 확정되었어요. 메일을 확인해주세요.')

    // 민감 파라미터가 URL에 남지 않도록 정리 (bookingId만 유지)
    localStorage.removeItem('holdCode')
    router.replace({
      path: route.path,
      query: { bookingId: data?.bookingId ?? '' }
    })
  } catch (e) {
    const err = e?.response?.data
    router.replace({
      path: '/reservation/fail',
      query: {
        message: err?.message ?? '결제 승인 실패',
        code:    err?.code ?? 'CONFIRM_FAILED',
        paymentKey: paymentKey.value,
        orderId: orderId.value,
        ...(holdCode.value && { holdCode: holdCode.value })
      }
    })
  } finally {
    confirming.value = false
  }
}

onMounted(() => {
  // 성공 페이지에 진입하면 즉시 confirm 진행
  confirmOnServer().catch(() => {
    router.replace({ path: '/reservation/fail', query: { message: '예상치 못한 오류', code: 'UNKNOWN' } })
  })
})

console.log('[SUCCESS PAGE]', route.query.paymentKey, route.query.orderId, route.query.amount, route.query.holdCode)
</script>

<style scoped>
.container{max-width:720px;margin:40px auto;padding:0 12px}
.box{padding:24px;border:1px solid #e5e7eb;border-radius:12px;background:#fff}
.muted{margin:8px 0;color:#64748b}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:10px}
.tr{text-align:right}
.mt{margin-top:18px}
pre{background:#f9fafb;padding:10px;border-radius:8px;font-size:12px;color:#374151;overflow:auto}
.btns{margin-top:24px;display:flex;gap:8px}
.btn{padding:10px 14px;border-radius:8px;background:#1b64da;color:#fff;border:none;cursor:pointer}
.btn.ghost{background:#f3f4f6;color:#111827}
.btn:disabled{opacity:.6;cursor:not-allowed}
</style>

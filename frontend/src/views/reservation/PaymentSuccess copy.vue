<!-- src/views/reservation/PaymentSuccess.vue -->
<template>
  <div class="container">
    <div class="box_section">
      <img
        width="90"
        src="https://static.toss.im/illusts/check-blue-spot-ending-frame.png"
        alt="결제 완료"
      />
      <h2>결제를 완료했어요 🎉</h2>
      <p v-if="confirming" style="margin:8px 0;color:#64748b">서버에 결제 승인 확인 중…</p>

      <!-- 결제 요약 -->
      <section class="grid">
        <div><b>결제금액</b></div>
        <div class="tr">{{ amountDisplay }}</div>

        <div><b>주문번호</b></div>
        <div class="tr">{{ orderId }}</div>
        

        <div v-if="responseJson?.receiptUrl"><b>영수증</b></div>
        <div v-if="responseJson?.receiptUrl" class="tr">
          <a :href="responseJson.receiptUrl" target="_blank" rel="noopener">영수증 보기</a>
        </div>

        <div v-if="responseJson?.bookingId"><b>예약번호</b></div>
        <div v-if="responseJson?.bookingId" class="tr">#{{ responseJson.bookingId }}</div>
      </section>

      <!-- 예약 정보 -->
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

      <!-- 예약자 -->
      <h3 class="mt">예약자</h3>
      <section class="grid">
        <div><b>이름</b></div>
        <div class="tr">{{ guestName || '-' }}</div>
      </section>

      <!-- (옵션) 디버그 JSON -->
      <details class="mt">
  <summary>디버그 정보</summary>
  <div style="margin:8px 0"><b>paymentKey</b>: {{ paymentKey }}</div>
  <pre>{{ prettyJson }}</pre>
</details>

      <div class="btns">
        <button class="button" type="button" :disabled="confirming" @click="router.push('/')">처음으로</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { confirmPayment } from '@/api/payments'

const route = useRoute()
const router = useRouter()

// Toss가 붙여주는 값
const paymentKey = ref('')
const orderId = ref('')
const amount = ref(0)

// 우리가 successUrl에 실어 보낸 요약값
const holdCode = ref('')
const hotelId = ref(null)
const hotelName = ref('')
const roomTypeName = ref('')
const checkIn = ref('')
const checkOut = ref('')
const guests = ref(1)
const guestName = ref('')

// 서버 confirm 응답
const responseJson = ref(null)
const confirming = ref(false)
let confirmedOnce = false

const amountDisplay = computed(() =>
  amount.value ? `${Number(amount.value).toLocaleString('ko-KR')}원` : ''
)
const prettyJson = computed(() =>
  responseJson.value ? JSON.stringify(responseJson.value, null, 2) : ''
)
const nights = computed(() => {
  const a = Date.parse(checkIn.value), b = Date.parse(checkOut.value)
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0
  return Math.round((b - a) / (1000 * 60 * 60 * 24))
})

function readParams () {
  // Toss 파라미터
  paymentKey.value = String(route.query.paymentKey || '')
  orderId.value    = String(route.query.orderId    || '')
  amount.value     = Number(route.query.amount     || 0)

  // 우리 요약 파라미터
  holdCode.value     = String(route.query.holdCode    || localStorage.getItem('holdCode') || '')
  hotelId.value      = route.query.hotelId ? Number(route.query.hotelId) : null
  hotelName.value    = String(route.query.hotelName   || '')
  roomTypeName.value = String(route.query.roomTypeName|| '')
  checkIn.value      = String(route.query.checkIn     || '')
  checkOut.value     = String(route.query.checkOut    || '')
  guests.value       = route.query.guests ? Number(route.query.guests) : 1
  guestName.value    = String(route.query.guestName   || '')
}

async function confirmOnServer () {
  if (confirmedOnce) return
  confirmedOnce = true

  readParams()

  if (!paymentKey.value || !orderId.value || !amount.value) {
    router.replace({
      path: '/reservation/fail',
      query: {
        message: '필수 결제 파라미터가 없습니다.',
        code: 'MISSING_PARAMS',
        ...(holdCode.value ? { holdCode: holdCode.value } : {})
      }
    })
    return
  }

  try {
    confirming.value = true
    const { data } = await confirmPayment({
      paymentKey: paymentKey.value,
      orderId: orderId.value,
      amount: amount.value,
      holdCode: holdCode.value || undefined,
    })
    responseJson.value = data

    // URL 민감 파라미터 정리(bookingId만 남김)
    localStorage.removeItem('holdCode')
    router.replace({
      path: route.path,
      query: { bookingId: data?.bookingId ?? '' }
    })
  } catch (e) {
    const errData = e?.response?.data
    router.replace({
      path: '/reservation/fail',
      query: {
        message: errData?.message ?? '결제 승인 실패',
        code: errData?.code ?? 'CONFIRM_FAILED',
        paymentKey: paymentKey.value,
        orderId: orderId.value,
        ...(holdCode.value ? { holdCode: holdCode.value } : {})
      }
    })
  } finally {
    confirming.value = false
  }
}

onMounted(() => {
  confirmOnServer().catch(() => {
    router.replace({
      path: '/reservation/fail',
      query: { message: '예상치 못한 오류', code: 'UNKNOWN' }
    })
  })
})
</script>

<style scoped>
.container{max-width:720px;margin:40px auto}
.box_section{padding:24px;border:1px solid #e5e7eb;border-radius:12px;background:#fff}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:10px}
.tr{text-align:right}
.mt{margin-top:18px}
pre{background:#f9fafb;padding:10px;border-radius:8px;font-size:12px;color:#333}
.btns{margin-top:24px;display:flex;gap:8px}
.button{padding:10px 14px;border-radius:8px;background:#1b64da;color:#fff;border:none;cursor:pointer}
.button:disabled{opacity:.6;cursor:not-allowed}
</style>

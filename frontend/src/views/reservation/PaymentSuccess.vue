<!-- src/views/reservation/PaymentSuccess.vue -->
<template>
  <div class="container">
    <div class="box_section">
      <img
        width="100"
        src="https://static.toss.im/illusts/check-blue-spot-ending-frame.png"
        alt="결제 완료"
      />
      <h2>결제를 완료했어요 🎉</h2>

      <div class="p-grid typography--p mt-50">
        <div class="p-grid-col text--left"><b>결제금액</b></div>
        <div class="p-grid-col text--right">{{ amountDisplay }}</div>
      </div>

      <div class="p-grid typography--p mt-10">
        <div class="p-grid-col text--left"><b>주문번호</b></div>
        <div class="p-grid-col text--right">{{ orderId }}</div>
      </div>

      <div class="p-grid typography--p mt-10">
        <div class="p-grid-col text--left"><b>paymentKey</b></div>
        <div class="p-grid-col text--right">{{ paymentKey }}</div>
      </div>

      <!-- 서버 응답 JSON 디버깅 -->
      <pre class="mt-20">{{ prettyJson }}</pre>

      <div class="p-grid mt-30">
        <button class="button p-grid-col5" type="button" @click="router.push('/')">
          처음으로
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { confirmTossPayment } from '@/api/payments'

const router = useRouter()
const paymentKey = ref('')
const orderId = ref('')
const amount = ref(0)
const holdCode = ref('')
const responseJson = ref(null)
const confirming = ref(false)

const amountDisplay = computed(() =>
  amount.value ? `${Number(amount.value).toLocaleString('ko-KR')}원` : ''
)
const prettyJson = computed(() =>
  responseJson.value ? JSON.stringify(responseJson.value, null, 2) : ''
)

async function confirmOnServer() {
  const params = new URLSearchParams(window.location.search)
  paymentKey.value = params.get('paymentKey') ?? ''
  orderId.value = params.get('orderId') ?? ''
  amount.value = Number(params.get('amount') ?? 0)
  holdCode.value = params.get('holdCode') ?? ''

  if (!paymentKey.value || !orderId.value || !amount.value) {
    const msg = encodeURIComponent('필수 결제 파라미터가 없습니다.')
    router.replace(
      `/reservation/fail?message=${msg}&code=MISSING_PARAMS${
        holdCode.value ? `&holdCode=${encodeURIComponent(holdCode.value)}` : ''
      }`
    )
    return
  }

  try {
    confirming.value = true
    responseJson.value = await confirmTossPayment({
      paymentKey: paymentKey.value,
      orderId: orderId.value,
      amount: amount.value,
      holdCode: holdCode.value || undefined,
    })
  } catch (e) {
    const msg = encodeURIComponent(e?.response?.data?.message ?? '결제 승인 실패')
    const code = encodeURIComponent(e?.response?.data?.code ?? 'CONFIRM_FAILED')
    router.replace(
      `/reservation/fail?message=${msg}&code=${code}${
        holdCode.value ? `&holdCode=${encodeURIComponent(holdCode.value)}` : ''
      }`
    )
  } finally {
    confirming.value = false
  }
}

onMounted(() => {
  confirmOnServer().catch(() => {
    router.replace(
      '/reservation/fail?message=' +
        encodeURIComponent('예상치 못한 오류') +
        '&code=UNKNOWN'
    )
  })
})
</script>

<style scoped>
.container {
  max-width: 640px;
  margin: 40px auto;
}
pre {
  background: #f9f9f9;
  padding: 10px;
  border-radius: 8px;
  font-size: 12px;
  color: #333;
}
</style>

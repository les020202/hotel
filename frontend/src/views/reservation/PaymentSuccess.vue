<template>
    <div class="container">
      <div class="box_section">
        <img
          width="100"
          src="https://static.toss.im/illusts/check-blue-spot-ending-frame.png"
          alt="결제 완료"
        />
        <h2>결제를 완료했어요</h2>
  
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
          <div class="p-grid-col text--right pk">{{ paymentKey }}</div>
        </div>
  
        <div class="p-grid mt-30">
          <button
            class="button p-grid-col5"
            type="button"
            @click="go('https://docs.tosspayments.com/guides/v2/payment-widget/integration')"
          >
            연동 문서
          </button>
          <button
            class="button p-grid-col5 discord"
            type="button"
            @click="go('https://discord.gg/A4fRFXQhRu')"
          >
            실시간 문의
          </button>
        </div>
      </div>
  
      <div class="box_section left">
        <b>Response Data :</b>
        <div class="response">
          <pre v-if="responseJson">{{ prettyJson }}</pre>
          <p v-else>응답 대기 중…</p>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { computed, onMounted, ref } from 'vue'
  
  const paymentKey = ref('')
  const orderId = ref('')
  const amount = ref(0)
  const responseJson = ref(null)
  
  const amountDisplay = computed(() =>
    amount.value ? `${Number(amount.value).toLocaleString()}원` : ''
  )
  const prettyJson = computed(() =>
    responseJson.value ? JSON.stringify(responseJson.value, null, 2) : ''
  )
  
  function go(url) {
    window.location.href = url
  }
  
  async function confirm() {
    // 쿼리 파라미터 검증: 클라이언트 값 조작 방지용으로
    // 서버에서 실제 금액/주문 검증은 반드시 다시 수행해야 함.
    const params = new URLSearchParams(window.location.search)
    paymentKey.value = params.get('paymentKey') ?? ''
    orderId.value = params.get('orderId') ?? ''
    amount.value = Number(params.get('amount') ?? 0)
  
    const requestData = {
      paymentKey: paymentKey.value,
      orderId: orderId.value,
      amount: amount.value
    }
  
    const res = await fetch('/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestData)
    })
  
    const json = await res.json()
  
    if (!res.ok) {
      // 결제 실패 비즈니스 로직: 실패 페이지로 이동
      const message = encodeURIComponent(json.message ?? '결제 승인 실패')
      const code = encodeURIComponent(json.code ?? 'UNKNOWN')
      window.location.href = `/fail?message=${message}&code=${code}`
      return
    }
  
    // 결제 성공 비즈니스 로직: 응답 표시
    responseJson.value = json
  }
  
  onMounted(() => {
    confirm().catch((e) => {
      responseJson.value = { error: true, message: String(e) }
    })
  })
  </script>
  
  <style scoped>
  .container {
    display: grid;
    gap: 20px;
    justify-items: start;
  }
  .box_section {
    width: 600px;
    padding: 24px;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    text-align: center;
    background: #fff;
  }
  .box_section.left {
    text-align: left;
  }
  .mt-50 { margin-top: 50px; }
  .mt-30 { margin-top: 30px; }
  .mt-10 { margin-top: 10px; }
  
  .p-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    align-items: center;
  }
  .p-grid-col { padding: 4px 0; }
  .text--left { text-align: left; }
  .text--right { text-align: right; }
  .typography--p { font-size: 14px; color: #111827; }
  .pk { white-space: normal; width: 250px; justify-self: end; }
  
  .button {
    padding: 10px 14px;
    border-radius: 8px;
    background: #1b64da;
    color: #fff;
    border: none;
    cursor: pointer;
  }
  .button.discord {
    background: #e8f3ff;
    color: #1b64da;
  }
  .response {
    white-space: pre-wrap;
    word-break: break-word;
  }
  </style>
  
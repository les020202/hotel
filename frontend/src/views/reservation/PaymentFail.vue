<!-- src/views/reservation/PaymentFail.vue -->
<template>
  <div id="info" class="box_section">
    <img
      width="100"
      src="https://static.toss.im/lotties/error-spot-no-loop-space-apng.png"
      alt="결제 실패"
    />
    <h2>결제를 실패했어요</h2>

    <div class="p-grid typography--p mt-50">
      <div class="p-grid-col text--left"><b>에러메시지</b></div>
      <div class="p-grid-col text--right">{{ message }}</div>
    </div>
    <div class="p-grid typography--p mt-10">
      <div class="p-grid-col text--left"><b>에러코드</b></div>
      <div class="p-grid-col text--right">{{ code }}</div>
    </div>

    <div class="p-grid mt-30">
      <button class="button p-grid-col5" type="button" @click="$router.push('/')">처음으로</button>
      <button class="button p-grid-col5 ghost" type="button" @click="$router.push('/search')">다시 찾아보기</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { cancelReservationHold } from '@/api/reservation'

const message = ref('')
const code = ref('')
let didCancel = false

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  message.value = params.get('message') ?? '알 수 없는 오류'
  code.value    = params.get('code') ?? '-'

  const holdCode = params.get('holdCode') || ''
  // 실패 시 자동으로 홀드 정리(재고 복구)
  if (holdCode && !didCancel) {
    try {
      didCancel = true
      await cancelReservationHold(holdCode)
    } catch (e) {
      console.error('hold cancel failed:', e)
    }
  }
})
</script>

<style scoped>
.box_section{width:600px;padding:24px;border:1px solid #e5e7eb;border-radius:12px;background:#fff;text-align:center}
.mt-50{margin-top:50px}.mt-30{margin-top:30px}.mt-10{margin-top:10px}
.p-grid{display:grid;grid-template-columns:1fr 1fr;align-items:center}
.p-grid-col{padding:4px 0}.text--left{text-align:left}.text--right{text-align:right}.typography--p{font-size:14px;color:#111827}
.button{padding:10px 14px;border-radius:8px;background:#1b64da;color:#fff;border:none;cursor:pointer}
.button.ghost{background:#e8f3ff;color:#1b64da}
</style>

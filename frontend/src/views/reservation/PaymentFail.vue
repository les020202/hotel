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
  </template>
  
  <script setup>
  import { onMounted, ref } from 'vue'
  
  const message = ref('')
  const code = ref('')
  
  function go(url) {
    window.location.href = url
  }
  
  onMounted(() => {
    const params = new URLSearchParams(window.location.search)
    message.value = params.get('message') ?? '알 수 없는 오류'
    code.value = params.get('code') ?? '-'
  })
  </script>
  
  <style scoped>
  .box_section {
    width: 600px;
    padding: 24px;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: #fff;
    text-align: center;
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
  </style>
  
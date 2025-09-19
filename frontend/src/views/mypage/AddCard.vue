<template>
  <div class="add-card">
    <h2>카드 추가</h2>
    <form @submit.prevent="addCard" class="form">
      <!-- 카드 브랜드 -->
      <label>
        브랜드
        <input v-model="brand" placeholder="VISA / MASTER" required />
      </label>

      <!-- 카드 번호 입력 -->
      <label>
        카드번호
        <input
          v-model="number"
          placeholder="1111-2222-3333-4444"
          pattern="[0-9-]+"
          maxlength="19"
          required
        />
      </label>

      <!-- 만료 월/연도 -->
      <div class="row">
        <label>
          만료월
          <input v-model.number="expMonth" type="number" min="1" max="12" placeholder="MM" required />
        </label>
        <label>
          만료연도
          <input v-model.number="expYear" type="number" min="2024" max="2035" placeholder="YYYY" required />
        </label>
      </div>

      <button type="submit" class="btn">추가</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

// Vue Router 인스턴스
const router = useRouter()

// 입력값 상태
const brand = ref('')
const number = ref('')
const expMonth = ref('')
const expYear = ref('')

// 카드 추가 함수
function addCard() {
  const newCard = {
    id: Date.now(), // 임시 ID (밀리초 기반)
    brand: brand.value,
    last4: number.value.replace(/\D/g, '').slice(-4), // 마지막 4자리 추출
    expMonth: expMonth.value,
    expYear: expYear.value
  }
  // 기존 localStorage 카드 목록 불러오기
  const stored = localStorage.getItem('cards')
  const cards = stored ? JSON.parse(stored) : []

  // 새 카드 추가
  cards.push(newCard)
  localStorage.setItem('cards', JSON.stringify(cards))

  alert('카드가 추가되었습니다')
  router.push('/mypage/payment') // 카드 목록 화면으로 이동
}
</script>

<style scoped>
.add-card {
  max-width: 420px; margin: 0 auto; padding: 20px;
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 12px;
}
h2 { margin-bottom: 16px; font-size: 20px; font-weight: 700; color: #111827; }
.form { display: flex; flex-direction: column; gap: 14px; }
label { display: flex; flex-direction: column; gap: 6px; font-size: 14px; color: #374151; }
input {
  padding: 10px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px;
}
input:focus { border-color: #0a6; box-shadow: 0 0 0 2px rgba(0,170,100,.2); outline: none; }
.row { display: flex; gap: 12px; }
.row label { flex: 1; }
.btn {
  margin-top: 12px; padding: 12px; border: none; border-radius: 8px;
  background: #0a6; color: white; font-size: 15px; font-weight: 600; cursor: pointer;
}
.btn:hover { background: #089956; }
</style>

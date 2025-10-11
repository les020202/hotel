<template>
  <div class="payment">
    <h2>결제수단</h2>

    <div class="grid">
      <!-- 저장된 카드 목록 반복 출력 -->
      <div v-for="c in cards" :key="c.id" class="ccard">
        <div class="cc-top">
          <!-- 카드번호 앞자리(가려진 부분) -->
          <div class="dots">•••• •••• ••••</div>
          <!-- 삭제 버튼 -->
          <button class="trash" title="삭제" @click="removeCard(c)">✕</button>
        </div>
        <div class="cc-mid">
          <!-- 카드 마지막 4자리 -->
          <div class="last4">{{ c.last4 }}</div>
        </div>
        <div class="cc-bot">
          <!-- 유효기간 -->
          <div class="valid">
            <div class="cap">Valid Thru</div>
            <div class="val">{{ (''+c.expMonth).padStart(2,'0') }}/{{ (''+c.expYear).slice(-2) }}</div>
          </div>
          <!-- 카드 브랜드 (VISA 등) -->
          <div class="brand">{{ c.brand }}</div>
        </div>
      </div>

      <!-- 새 카드 추가 버튼 (타일 형태) -->
      <RouterLink to="/mypage/add-card" class="add-tile">
        <div class="plus">+</div>
        <div class="txt">Add a new card</div>
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 카드 목록 상태
const cards = ref([])

// 컴포넌트 마운트 시 localStorage에서 카드 불러오기
onMounted(() => {
  const stored = localStorage.getItem('cards')
  cards.value = stored ? JSON.parse(stored) : []
})

// 카드 삭제 함수
function removeCard(card) {
  if (!confirm('이 카드를 삭제할까요?')) return
  cards.value = cards.value.filter(c => c.id !== card.id)
  localStorage.setItem('cards', JSON.stringify(cards.value))
}
</script>

<style scoped>
/* 카드 목록을 그리드 형태로 배치 */
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }

/* 개별 카드 디자인 */
.ccard {
  background: #aee3d1; /* 민트 톤 배경 */
  border-radius: 16px; padding: 16px;
  box-shadow: inset 0 0 0 1px rgba(0,0,0,.04);
  display: flex; flex-direction: column; gap: 10px;
}

/* 카드 상단: 점선 번호 + 삭제 버튼 */
.cc-top { display: flex; justify-content: space-between; align-items: center; }
.dots { letter-spacing: 2px; color: #103b33; font-weight: 700; }
.trash {
  border: 0; background: rgba(255,255,255,.6); border-radius: 8px; padding: 4px 8px;
  cursor: pointer; color: #103b33;
}

/* 카드 중앙: 마지막 4자리 강조 */
.cc-mid .last4 { font-size: 28px; font-weight: 800; letter-spacing: 2px; color: #103b33; }

/* 카드 하단: 유효기간 + 브랜드 */
.cc-bot { display: flex; justify-content: space-between; align-items: end; }
.cap { font-size: 11px; color: #0f3a32; opacity: .8; }
.val { font-weight: 700; color: #103b33; }
.brand { font-weight: 800; color: #103b33; }

/* 카드 추가 버튼 (타일 형태) */
.add-tile {
  display: grid; place-items: center; gap: 6px; text-decoration: none;
  border: 2px dashed #cbd5e1; border-radius: 16px; color: #334155; background: #fff;
  min-height: 160px;
}
.add-tile:hover { background: #f8fafc; }
.plus { font-size: 28px; line-height: 1; }
.txt { font-size: 14px; }
</style>

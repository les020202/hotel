<template>
  <div class="history">
    <h2>예약내역</h2>

    <div class="list">
      <!-- 예약 티켓 반복 출력 -->
      <div class="ticket" v-for="r in dummy" :key="r.id">
        <div class="left">
          <div class="icon">🏨</div>
          <div class="dates">
            <div><b>Check-in</b> {{ r.checkIn }}</div>
            <div><b>Check-out</b> {{ r.checkOut }}</div>
          </div>
        </div>

        <div class="mid">
          <div>체크인 12:00pm</div>
          <div>체크아웃 11:30am</div>
        </div>

        <div class="right">
          <button class="btn">Download Ticket</button>
          <!-- 추후 실제 PDF 다운로드 기능 구현 예정 -->
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 더미 데이터: 실제 API 연동 전까지 샘플 예약내역 표시
const dummy = [
  { id:1, checkIn:'Thu, Dec 8', checkOut:'Fri, Dec 9' },
  { id:2, checkIn:'Sat, Dec 10', checkOut:'Sun, Dec 11' },
  { id:3, checkIn:'Mon, Dec 12', checkOut:'Tue, Dec 13' }
]
</script>

<style scoped>
.list { display: flex; flex-direction: column; gap: 10px; }
.ticket {
  display: grid; grid-template-columns: 1.2fr 1fr auto; align-items: center;
  border: 1px solid #e5e7eb; background: #fff; border-radius: 12px; padding: 12px 14px;
}
.left { display: flex; align-items: center; gap: 10px; }
.icon { width: 44px; height: 44px; border-radius: 10px; background: #f1f5f9; display: grid; place-items: center; }
.dates b { margin-right: 6px; }
.mid { color: #6b7280; text-align: right; }
.right { display: flex; justify-content: flex-end; }
.btn { padding: 8px 12px; border: 0; border-radius: 8px; background: #0a6; color: #fff; cursor: pointer; }
</style>

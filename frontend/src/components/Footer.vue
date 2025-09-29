<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="grid5">
        <div>
          <h4>About</h4>
          <ul class="text-muted">
            <li><a href="#">회사 소개</a></li>
            <li><a href="#">채용</a></li>
            <li><a href="#">Press</a></li>
          </ul>
        </div>

        <div>
          <h4>Work with us</h4>
          <ul class="text-muted">
            <li><a href="#">파트너 등록</a></li>
            <li><a href="#">광고 문의</a></li>
          </ul>
        </div>

        <div>
          <h4>Guide</h4>
          <ul class="text-muted">
            <li>
              <!-- 위치 #1: 가이드 그룹 안 텍스트 링크 -->
              <router-link to="/support/bugreport" class="inline-link">버그 신고</router-link>
            </li>
            <li><a href="#">이용 안내</a></li>
            <li><a href="#">여행 가이드</a></li>
          </ul>
        </div>
      </div>

      <div class="bottom-row">
        <div class="policies">
          <a href="#">이용약관</a>
          <a href="#">개인정보 처리방침</a>
          <a href="#">쿠키 정책</a>
        </div>

        <div class="right-actions">
          <div class="sns">
            <a href="https://instagram.com" target="_blank" aria-label="Instagram">Instagram</a>
            <a href="https://x.com" target="_blank" aria-label="X">X</a>
            <a href="https://youtube.com" target="_blank" aria-label="YouTube">YouTube</a>
          </div>

          <!-- 위치 #2: 하단 오른쪽 강조 버튼 -->
          <router-link to="/support/bugreport" class="bug-btn" aria-label="버그 신고 페이지로 이동">
            🐞 버그 신고
          </router-link>
        </div>
      </div>

      <div class="biz">
        상호: 스테이새비 | 주소: 서울특별시 가상구 가상로 123 | 사업자등록번호: 123-45-67890
      </div>
    </div>
  </footer>
</template>

<script setup>
import { ref } from 'vue'
import { subscribeEmail } from '@/api/auth'
const email = ref(''); const msg = ref('')
async function onSubmit(){ await subscribeEmail(email.value); msg.value = '구독이 완료되었습니다.'; email.value = '' }
</script>

<style scoped>
/* 배경색/경계선: 화면 전체 */
.footer{
  width: 100%;
  background: #f7f9fc;
  border-top: 1px solid #e5edf7;
}

/* 내부 래퍼: 풀폭 + 여백만 유지 */
.footer-inner{
  width: 100%;
  max-width: none !important;
  margin: 0 !important;
  padding: 24px 16px !important;
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
}

/* 3열 묶음 */
.grid5{
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 24px;
  align-items: start;
}

h4{
  font-weight: 700;
  margin: 0 0 8px;
  color: #0f172a;
}

.text-muted{
  color:#475569;
  padding:0; margin:0; list-style:none;
}
.text-muted li{ margin:6px 0; }
.text-muted a,
.inline-link{ color:inherit; text-decoration:none; }
.text-muted a:hover,
.inline-link:hover{ text-decoration:underline; }

/* 하단 라인 */
.bottom-row{
  display:flex; justify-content:space-between; align-items:center;
  margin-top:20px; padding-top:12px; border-top:1px solid #e5edf7;
}
.policies{ font-size:12px; display:flex; gap:12px; color:#475569; }
.policies a{ color:inherit; text-decoration:none; }
.policies a:hover{ text-decoration:underline; }

/* 오른쪽: SNS + 버그버튼 묶음 */
.right-actions{
  display:flex; align-items:center; gap:12px; flex-wrap:wrap;
}
.sns{ display:flex; gap:8px; }
.sns a{ color:#0f172a; text-decoration:none; }
.sns a:hover{ text-decoration:underline; }

/* 강조 버그 버튼 */
.bug-btn{
  display:inline-flex; align-items:center; gap:6px;
  padding:8px 12px; border-radius:10px; border:1px solid #cfe0ff;
  background:#f5f9ff; color:#0f172a; font-weight:700; text-decoration:none;
  transition: transform .05s ease, box-shadow .2s, background .2s;
}
.bug-btn:hover{
  background:#eaf3ff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.bug-btn:active{ transform: translateY(1px); }

.biz{
  color:#64748b; font-size:12px; margin-top:8px;
}

/* 반응형 */
@media (max-width:640px){
  .grid5{ grid-template-columns:1fr; gap:16px; }
  .bottom-row{ flex-direction:column; align-items:flex-start; gap:10px; }
  .right-actions{ width:100%; justify-content:space-between; }
}
</style>

<template>
  <footer class="footer">
    <div class="container" style="padding: 24px 16px;">
      <div class="grid5">
        <div>
          <h4 style="font-weight:600; margin-bottom:8px;">About</h4>
          <ul class="text-muted">
            <li><a href="#">회사 소개</a></li>
            <li><a href="#">채용</a></li>
            <li><a href="#">Press</a></li>
          </ul>
        </div>
        <div>
          <h4 style="font-weight:600; margin-bottom:8px;">Work with us</h4>
          <ul class="text-muted">
            <li><a href="#">파트너 등록</a></li>
            <li><a href="#">광고 문의</a></li>
          </ul>
        </div>
        <div>
          <h4 style="font-weight:600; margin-bottom:8px;">Guide</h4>
          <ul class="text-muted">
            <li><a href="#">이용 안내</a></li>
            <li><a href="#">여행 가이드</a></li>
          </ul>
        </div>
        
      </div>

      <div class="flex" style="justify-content:space-between; margin-top:20px; padding-top:12px; border-top:1px solid #eee;">
        <div class="text-muted" style="font-size:12px; display:flex; gap:12px;">
          <a href="#">이용약관</a>
          <a href="#">개인정보 처리방침</a>
          <a href="#">쿠키 정책</a>
        </div>
        <div class="flex" style="gap:8px;">
          <a href="https://instagram.com" target="_blank" aria-label="Instagram">Instagram</a>
          <a href="https://x.com" target="_blank" aria-label="X">X</a>
          <a href="https://youtube.com" target="_blank" aria-label="YouTube">YouTube</a>
        </div>
      </div>

      <div class="text-muted" style="font-size:12px; margin-top:8px;">
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


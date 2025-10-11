<template>
    <div class="inquiry">
      <button class="back-btn" @click="goBack">← 뒤로가기</button>
  
      <h2 class="title">버그 신고</h2>
      <p class="desc">발견한 오류/버그를 자세히 알려주세요. 빠르게 확인하겠습니다.</p>
  
      <form @submit.prevent="submitBug" class="form">
        <label>
          <span class="label">제목</span>
          <input v-model="subject" required placeholder="예) 로그인 후 페이지가 하얗게 나옵니다" />
        </label>
  
        <label>
          <span class="label">내용</span>
          <textarea v-model="message" required placeholder="재현 방법, 발생 시각/브라우저/화면 등을 자세히 적어주세요."></textarea>
        </label>
  
        <button type="submit" class="btn">버그 신고 등록</button>
      </form>
    </div>
  </template>
  
  <script setup>
  /**
   * BugReport.vue
   * - support_tickets를 재사용하여 버그 신고를 남긴다
   * - 제목 앞에 '버그 ' 접두어를 자동 부여(이미 있으면 중복 부여 X)
   */
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import api from '@/api/auth' // 기존 axios 인스턴스
  
  const router = useRouter()
  const subject = ref('')
  const message = ref('')
  
  function goBack(){ router.back() }
  
  /** '버그 ' 접두어 강제 부여 */
  function toBugSubject(raw){
    const s = (raw || '').trim()
    if (!s) return '버그 (제목 미입력)'
    // 이미 '버그', '[버그]', 'BUG' 등 시작 처리(한글 기준 우선)
    const lowered = s.toLowerCase()
    if (s.startsWith('버그 ') || s.startsWith('[버그]') || lowered.startsWith('bug ')) return s
    return `버그 ${s}`
  }
  
  async function submitBug(){
    try {
      const bugTitle = toBugSubject(subject.value)
      await api.post('/support/tickets', {
        subject: bugTitle,
        firstMessage: message.value
      })
      alert('버그 신고가 등록되었습니다. 감사합니다!')
  
      subject.value = ''
      message.value = ''
  
      // 신고 후 마이페이지 > 고객지원 목록으로 이동(원한다면 버그 전용 목록 라우트로 변경 가능)
      router.push('/mypage/support')
    } catch (e) {
      console.error('버그 신고 등록 실패', e)
      alert('등록에 실패했습니다. 잠시 후 다시 시도해 주세요.')
    }
  }
  </script>
  
  <style scoped>
  .inquiry{ max-width:720px; margin:0 auto; padding:32px 24px; background:#fff; border-radius:12px; box-shadow:0 2px 10px rgba(0,0,0,0.08) }
  .back-btn{ background:none; border:none; color:#0a6; font-size:14px; font-weight:600; cursor:pointer; margin-bottom:16px; display:inline-block }
  .back-btn:hover{ text-decoration:underline }
  .title{ font-size:22px; font-weight:700; margin-bottom:8px }
  .desc{ font-size:14px; color:#555; margin-bottom:20px }
  .form{ display:flex; flex-direction:column; gap:18px }
  .label{ font-size:14px; font-weight:600; margin-bottom:6px; display:block }
  input, textarea{ width:100%; padding:12px; border:1px solid #ddd; border-radius:8px; font-size:14px; outline:none; transition:border .2s }
  input:focus, textarea:focus{ border-color:#0a6; box-shadow:0 0 0 2px rgba(0,170,100,.1) }
  textarea{ min-height:150px; resize:vertical }
  .btn{ align-self:flex-start; padding:12px 20px; border:0; border-radius:8px; background:#0a6; color:#fff; font-weight:600; cursor:pointer; transition:background .2s }
  .btn:hover{ background:#088f58 }
  </style>
  
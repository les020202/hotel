<template>
  <div class="inquiry">
    <!-- 뒤로가기 버튼 -->
    <button class="back-btn" @click="goBack">← 뒤로가기</button>

    <!-- 페이지 제목 -->
    <h2 class="title">문의</h2>
    <!-- 안내 문구 -->
    <p class="desc">궁금한 점이나 불편 사항을 작성해 주시면 신속히 답변드리겠습니다.</p>

    <!-- 문의 등록 폼 -->
    <form @submit.prevent="submitInquiry" class="form">
      <!-- 제목 입력 -->
      <label>
        <span class="label">제목</span>
        <input
          v-model="subject"
          required
          placeholder="문의 제목을 입력하세요"
          :maxlength="SUBJECT_MAX"
        />
      </label>

      <!-- 내용 입력 -->
      <label>
        <span class="label">내용</span>
        <textarea
          v-model="message"
          required
          placeholder="문의하실 내용을 입력하세요"
          :maxlength="BODY_MAX"
        ></textarea>
      </label>

      <!-- 제출 버튼 -->
      <button type="submit" class="btn" :disabled="submitting">
        {{ submitting ? '등록 중…' : '문의 등록' }}
      </button>
    </form>
  </div>
</template>

<script setup>
/**
 * SupportInquiry.vue
 * - 제목/내용 입력 → 백엔드(/support/tickets)로 전송
 * - 전송 직전 sanitize + trim + 길이 제한(제목 150, 내용 8000)
 * - 디자인/기능 흐름은 그대로 유지
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/auth'  // axios 인스턴스 (공통 API)
import { sanitizeText, clamp } from '@/utils/sanitize'

const router = useRouter()
const SUBJECT_MAX = 150
const BODY_MAX = 8000

// 입력 데이터 (양방향 바인딩)
const subject = ref('')  // 문의 제목
const message = ref('')  // 문의 내용
const submitting = ref(false)

/** 이전 화면으로 돌아가기 */
function goBack() {
  router.back()
}

/** 문의 등록 요청 */
async function submitInquiry() {
  if (submitting.value) return
  submitting.value = true
  try {
    // ✅ 전송 직전 클라이언트 측 정화(보조 방어)
    const cleanSubject = clamp(sanitizeText(subject.value), SUBJECT_MAX)
    const cleanMessage = clamp(sanitizeText(message.value), BODY_MAX)

    if (!cleanSubject) {
      alert('제목을 입력해주세요.')
      submitting.value = false
      return
    }
    if (!cleanMessage) {
      alert('내용을 입력해주세요.')
      submitting.value = false
      return
    }

    await api.post('/support/tickets', {
      subject: cleanSubject,
      firstMessage: cleanMessage
    })
    alert('문의가 등록되었습니다!')

    // 입력값 초기화
    subject.value = ''
    message.value = ''

    // 문의 등록 후 고객지원 목록으로 이동
    router.push('/mypage/support')
  } catch (e) {
    console.error('문의 등록 실패', e)
    alert('문의 등록에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* 컨테이너 */
.inquiry {
  max-width: 720px;
  margin: 0 auto;
  padding: 32px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.08);
}

/* 뒤로가기 버튼 */
.back-btn {
  background: none;
  border: none;
  color: #0a6;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 16px;
  display: inline-block;
}
.back-btn:hover {
  text-decoration: underline;
}

/* 제목 */
.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}

/* 설명 문구 */
.desc {
  font-size: 14px;
  color: #555;
  margin-bottom: 20px;
}

/* 폼 스타일 */
.form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 입력 라벨 */
.label {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
  display: block;
}

/* input, textarea 공통 스타일 */
input, textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border 0.2s;
}
input:focus, textarea:focus {
  border-color: #0a6;
  box-shadow: 0 0 0 2px rgba(0, 170, 100, 0.1);
}

/* textarea 추가 옵션 */
textarea {
  min-height: 150px;
  resize: vertical;
}

/* 제출 버튼 */
.btn {
  align-self: flex-start;
  padding: 12px 20px;
  border: 0;
  border-radius: 8px;
  background: #0a6;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn:hover {
  background: #088f58;
}
</style>

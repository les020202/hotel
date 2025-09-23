<script setup>
import { ref, reactive, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { sendEmailCode, verifyEmailCode, resetPassword } from '@/api/auth'

const router = useRouter()

/* ---------------- State ---------------- */
const form = reactive({
  email: '',
  password: '',
  password2: ''
})

const code = ref('')
const codeMsg = ref('')
const emailVerified = ref(false)
const cooldown = ref(0)
let cooldownTimer = null

const show1 = ref(false)
const show2 = ref(false)
const loading = ref(false)
const msg = ref('')

/* ---------------- Helpers ---------------- */
const rxPassword = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^\w\s]).{10,30}$/
const passwordValid = computed(() => rxPassword.test(form.password))
const passwordsMatch = computed(() => form.password === form.password2)

function startCooldown(sec) {
  clearInterval(cooldownTimer)
  cooldown.value = sec
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}
onBeforeUnmount(() => clearInterval(cooldownTimer))

/* ---------------- Actions ---------------- */
async function onSendCode() {
  codeMsg.value = ''
  emailVerified.value = false
  if (!form.email) { codeMsg.value = '이메일을 입력하세요.'; return }
  try {
    await sendEmailCode(form.email)
    codeMsg.value = '인증 코드를 전송했습니다.'
    startCooldown(60)
  } catch (e) {
    codeMsg.value = e?.response?.data?.error || e?.message || '코드 전송 실패'
  }
}

async function onVerifyCode() {
  codeMsg.value = ''
  try {
    const resp = await verifyEmailCode(form.email, code.value)
    if (resp?.verified || resp?.data?.verified) {
      emailVerified.value = true
      codeMsg.value = '이메일 인증 완료!'
      clearInterval(cooldownTimer)
      cooldown.value = 0
    } else {
      codeMsg.value = '인증 코드가 일치하지 않습니다.'
    }
  } catch (e) {
    codeMsg.value = e?.response?.data?.error || e?.message || '인증 실패'
  }
}

async function onSubmit() {
  msg.value = ''

  if (!emailVerified.value) { msg.value = '이메일 인증이 필요합니다.'; return }
  if (!passwordValid.value) {
    msg.value = '비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 10~30자여야 합니다.'
    return
  }
  if (!passwordsMatch.value) { msg.value = '비밀번호가 일치하지 않습니다.'; return }

  loading.value = true
  try {
    await resetPassword(form.email, code.value, form.password)
    alert('비밀번호가 변경되었습니다. 로그인 페이지로 이동합니다.')
    router.push('/login')
  } catch (e) {
    msg.value = e?.response?.data?.error || e?.message || '비밀번호 변경 실패'
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="auth-shell">
    <section class="auth-card">
      <!-- 상단 툴바 -->
      <div class="toolbar">
        <button class="back" @click="goLogin" aria-label="로그인으로 돌아가기">
          <svg viewBox="0 0 24 24" class="ico" aria-hidden="true">
            <path d="M15 18l-6-6 6-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          로그인으로
        </button>
      </div>

      <div class="auth-pane">
        <h2 class="title">비밀번호 찾기</h2>
        <p class="subtitle">가입 시 등록한 이메일로 인증하고 비밀번호를 재설정하세요.</p>

        <form class="form" @submit.prevent="onSubmit">
          <!-- 이메일 & 인증 코드 -->
          <input class="input" v-model.trim="form.email" placeholder="이메일 입력" required />

          <div class="row gap wrap">
            <button type="button" class="btn outline" :disabled="cooldown>0" @click="onSendCode">
              {{ cooldown>0 ? `재발송(${cooldown}s)` : '인증코드 발송' }}
            </button>
            <input class="input flex1" v-model="code" placeholder="인증 코드 입력" />
            <button type="button" class="btn outline" @click="onVerifyCode">확인</button>
          </div>
          <p class="hint" :class="emailVerified ? 'ok' : 'bad'">{{ codeMsg }}</p>

          <!-- 새 비밀번호 -->
          <div class="passwrap">
            <input
              :type="show1 ? 'text' : 'password'"
              class="input"
              v-model="form.password"
              placeholder="새 비밀번호"
              :class="{ invalid: form.password && !passwordValid }"
              autocomplete="new-password"
              required
            />
            <button type="button" class="eye" @click="show1 = !show1">
              👁
            </button>
          </div>

          <!-- 비밀번호 확인 -->
          <div class="passwrap">
            <input
              :type="show2 ? 'text' : 'password'"
              class="input"
              v-model="form.password2"
              placeholder="비밀번호 확인"
              :class="{ invalid: form.password2 && !passwordsMatch }"
              autocomplete="new-password"
              required
            />
            <button type="button" class="eye" @click="show2 = !show2">
              👁
            </button>
          </div>

          <button class="btn primary" type="submit" :disabled="loading">
            <span v-if="!loading">비밀번호 변경</span>
            <span v-else class="spinner"></span>
          </button>

          <p class="msg center" v-if="msg">{{ msg }}</p>
        </form>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* 스타일은 기존 그대로 유지 */
</style>

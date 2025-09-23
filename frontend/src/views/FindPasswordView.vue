<script setup>
import { ref, reactive, computed, onBeforeUnmount, watch } from 'vue'
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
  } catch(e) {
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
  } catch(e) {
    codeMsg.value = e?.response?.data?.error || e?.message || '인증 실패'
  }
}

async function onSubmit() {
  msg.value = ''

  if (!emailVerified.value) {
    msg.value = '이메일 인증이 필요합니다.'
    return
  }
  if (!passwordValid.value) {
    msg.value = '비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 10~30자여야 합니다.'
    return
  }
  if (!passwordsMatch.value) {
    msg.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  loading.value = true
  try {
    // ✅ resetPassword 호출 시 인증 코드 포함
    await resetPassword(form.email, code.value, form.password)
    alert('비밀번호가 변경되었습니다. 로그인 페이지로 이동합니다.')
    router.push('/login')
  } catch(e) {
    msg.value = e?.response?.data?.error || e?.message || '비밀번호 변경 실패'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <section class="auth-card">
      <div class="auth-pane">
        <h2 class="title">비밀번호 찾기</h2>
        <p class="subtitle">가입 시 등록한 이메일로 인증하고 비밀번호를 재설정하세요.</p>

        <div class="form">
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
          <div class="row gap wrap">
            <input :type="show1?'text':'password'" class="input flex1" v-model="form.password" placeholder="새 비밀번호" :class="{ invalid: form.password && !passwordValid }" />
            <button type="button" class="eye" @click="show1=!show1">👁</button>
          </div>
          <div class="row gap wrap">
            <input :type="show2?'text':'password'" class="input flex1" v-model="form.password2" placeholder="비밀번호 확인" :class="{ invalid: form.password2 && !passwordsMatch }" />
            <button type="button" class="eye" @click="show2=!show2">👁</button>
          </div>

          <button class="btn primary" :disabled="loading" @click="onSubmit">
            <span v-if="!loading">비밀번호 변경</span>
            <span v-else class="spinner" aria-label="처리중"></span>
          </button>

          <p class="msg center" v-if="msg">{{ msg }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
:root{
  --bg-1:#f3f8ff; --bg-2:#eef5ff;
  --card:#ffffffee; --card-border:#d6e6ff;
  --text-1:#111827; --text-2:#4a5a75; --text-body:#1f2a37;
  --brand-1:#5daeff; --brand-2:#9fd3ff;
  --focus:rgba(93,174,255,.22);
  --ok:#10b981; --bad:#ef4444;
}

.auth-shell{
  min-height:100vh; display:grid; place-items:center; padding:48px 16px;
  background:linear-gradient(180deg,var(--bg-1),var(--bg-2)); color:var(--text-body);
  font-family:ui-sans-serif,system-ui,Segoe UI,Roboto,Helvetica,Arial;
}
.auth-card{
  width:min(600px,95vw); display:grid; grid-template-columns:1fr;
  border-radius:18px; background:var(--card); border:1px solid var(--card-border);
  box-shadow:0 18px 60px rgba(16,44,84,.12); overflow:hidden; backdrop-filter:blur(10px);
}
.auth-pane{ padding:36px 32px }
.title{ margin:0 0 6px; font-size:24px; font-weight:800; color:var(--text-1) }
.subtitle{ margin:0 0 20px; color:var(--text-2); font-size:13px }

.form{ display:grid; gap:12px; margin-top:8px }
.row{ display:flex; align-items:center }
.row.gap{ gap:10px }
.row.wrap{ flex-wrap:wrap }
.flex1{ flex:1 1 200px }

.input{
  width:100%; height:42px; padding:10px 12px; border-radius:12px;
  border:1px solid #cfe0ff; background:#fff; color:var(--text-body);
  outline:none; transition:border-color .18s ease, box-shadow .18s ease, background .18s ease;
  font-size:15px; box-sizing:border-box;
}
.input::placeholder{ color:#9aa8c3 }
.input:hover{ background:#fbfdff }
.input:focus{ border-color:var(--brand-1); box-shadow:0 0 0 3px var(--focus) }
.input.invalid{ border-color:#fecaca; box-shadow:0 0 0 3px rgba(239,68,68,.2) }

.eye{
  position:absolute; top:50%; right:8px; transform:translateY(-50%);
  width:28px; height:28px; display:grid; place-items:center;
  border:0; background:transparent; color:#6a7a99; cursor:pointer; border-radius:8px;
}
.eye:hover{ background:#f2f6ff; color:#0f2547 }

.btn{
  padding:0 16px; height:42px; border-radius:999px; font-weight:800; font-size:15px;
  display:inline-flex; align-items:center; justify-content:center; cursor:pointer; border:0;
  transition:transform .06s ease, box-shadow .12s ease, background .18s ease, border-color .18s ease;
}
.btn.primary{ width:100%; background:linear-gradient(135deg,#3b82f6,#2563eb); color:#fff; box-shadow:0 8px 20px rgba(37,99,235,.28) }
.btn.primary:hover{ background:linear-gradient(135deg,#2563eb,#1d4ed8) }
.btn.primary:disabled{ opacity:.65; cursor:not-allowed; box-shadow:none }
.btn.outline{ background:#f0f7ff; border:1px solid #d7e7ff; color:#0f2547; height:42px; }
.btn.outline:hover{ background:#e9f3ff }

.ok { color:#10b981; font-weight:600; font-size:14px; }
.bad { color:#ef4444; font-weight:600; font-size:14px; }
.hint{ margin-top:-4px; }

.spinner{ display:inline-block; width:18px; height:18px; border:2px solid rgba(255,255,255,.6); border-top-color:#ffffff; border-radius:50%; animation:spin 1s linear infinite; }
.spinner--inline{ width:16px; height:16px; border-width:2px; border-color:#9bbcf8; border-top-color:#2563eb; }
@keyframes spin{ to { transform: rotate(360deg) } }

.msg.center{ text-align:center; font-size:13px; color:#e11d48; }
*,*::before,*::after{ box-sizing:border-box; }
</style>

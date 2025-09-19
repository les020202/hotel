<script setup>
import { ref, reactive, computed, onBeforeUnmount, nextTick } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import {
  checkUsername, checkEmail, signup,
  sendEmailCode, verifyEmailCode
} from '@/api/auth'

const showTerms = ref(false)

const router = useRouter()

/* ---------------- State ---------------- */
const form = reactive({
  loginId:'', password:'', password2:'',
  name:'', email:'', phone:'',
  gender:'MALE',
  birthDate:''
})

/* 이름/아이디/이메일 중복확인 */
const idCheck = ref(null)        // null | true | false
const emailCheck = ref(null)     // null | true | false
const idLoading = ref(false)
const emailLoading = ref(false)

/* 이메일 파트 + 도메인 선택 */
const emailLocal = ref('')                       // 값
const emailDomainMode = ref('gmail.com')         // 'gmail.com' | 'naver.com' | 'custom'
const emailDomainCustom = ref('')                // 값(직접입력)
const emailLocalEl = ref(null)                   // 엘리먼트 ref
const emailDomainCustomEl = ref(null)

/* 이메일 인증 */
const code = ref('')
const emailVerified = ref(false)
const codeMsg = ref('')
const cooldown = ref(0)
let cooldownTimer = null

/* 비번 보기 */
const show1 = ref(false)
const show2 = ref(false)

/* 연락처 010-xxxx-xxxx */
const phone2 = ref('')
const phone3 = ref('')
const phone2Ref = ref(null)
const phone3Ref = ref(null)

/* 동의 */
const agree = ref(false)

/* 전송/알림 */
const loading = ref(false)
const msg = ref('')

/* ---------------- Helpers ---------------- */
const getAvailable = (res) => {
  if (!res) return undefined
  if (typeof res.available !== 'undefined') return res.available
  if (res.data && typeof res.data.available !== 'undefined') return res.data.available
  return undefined
}

/* 이메일 전체 문자열 */
const emailFull = computed(() => {
  const domain = emailDomainMode.value === 'custom'
    ? (emailDomainCustom.value || '').trim()
    : emailDomainMode.value
  const local = (emailLocal.value || '').trim()
  return local && domain ? `${local}@${domain}` : ''
})

/* 이름/아이디/비번/이메일 규칙 */
const rxPassword = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^\w\s]).{10,30}$/   // 영문(대/소 무관)+숫자+특수, 10~30
const isNameValid = computed(() => form.name && [...form.name].length <= 10)
const isLoginIdLenValid = computed(() => (form.loginId || '').length >= 8 && (form.loginId || '').length <= 20)
const isPasswordValid = computed(() => rxPassword.test(form.password || ''))
const isPasswordSame = computed(() => (form.password || '') === (form.password2 || ''))
const isEmailFormatValid = computed(() => {
  if (!emailFull.value) return false
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailFull.value)
})

/* 폰 자동 이동 & 숫자 제한 */
function onlyDigits(v){ return (v || '').replace(/\D+/g, '') }
function onPhone2Input(e){
  const t = e.target
  phone2.value = onlyDigits(t.value).slice(0,4)
  if (phone2.value.length === 4) nextTick(() => phone3Ref.value?.focus())
}
function onPhone3Input(e){
  const t = e.target
  phone3.value = onlyDigits(t.value).slice(0,4)
}

/* cooldown */
function startCooldown (sec) {
  clearInterval(cooldownTimer)
  cooldown.value = sec
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}
onBeforeUnmount(() => clearInterval(cooldownTimer))

/* 나이 계산 (만 나이) */
function getAge(birth){ // 'YYYY-MM-DD'
  if (!birth) return null
  const today = new Date()
  const b = new Date(birth)
  if (isNaN(b.getTime())) return null
  let age = today.getFullYear() - b.getFullYear()
  const m = today.getMonth() - b.getMonth()
  if (m < 0 || (m === 0 && today.getDate() < b.getDate())) age--
  return age
}

/* ---------------- Actions ---------------- */
async function onCheckId(){
  if (!form.loginId?.trim()){
    idCheck.value = null
    msg.value = '아이디를 입력하세요.'
    return
  }
  if (!isLoginIdLenValid.value){
    idCheck.value = null
    msg.value = '아이디는 8~20자까지 입력 가능합니다.'
    return
  }
  idLoading.value = true
  msg.value = ''
  try{
    const res = await checkUsername(form.loginId.trim())
    const available = getAvailable(res)
    if (typeof available === 'undefined') throw new Error('서버 응답 형식이 올바르지 않습니다.')
    idCheck.value = !!available
  }catch(e){
    idCheck.value = false
    msg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '아이디 확인 실패'
  }finally{
    idLoading.value = false
  }
}

async function onCheckEmail(){
  if (!emailFull.value){
    emailCheck.value = null
    msg.value = '이메일을 입력하세요.'
    return
  }
  if (!isEmailFormatValid.value){
    emailCheck.value = null
    msg.value = '이메일 형식을 확인하세요.'
    return
  }
  emailLoading.value = true
  msg.value = ''
  try{
    const res = await checkEmail(emailFull.value)
    const available = getAvailable(res)
    if (typeof available === 'undefined') throw new Error('서버 응답 형식이 올바르지 않습니다.')
    emailCheck.value = !!available
  }catch(e){
    emailCheck.value = false
    msg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '이메일 확인 실패'
  }finally{
    emailLoading.value = false
  }
}

async function onSendCode () {
  codeMsg.value = ''
  emailVerified.value = false
  if (!emailFull.value) { codeMsg.value = '이메일을 먼저 입력하세요.'; return }
  if (!isEmailFormatValid.value) { codeMsg.value = '이메일 형식을 확인하세요.'; return }
  try{
    const res = await checkEmail(emailFull.value)
    const available = getAvailable(res)
    if (available === false) { codeMsg.value = '이미 사용 중인 이메일입니다.'; return }
  }catch(e){
    codeMsg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '이메일 확인 실패'
    return
  }
  try {
    await sendEmailCode(emailFull.value)
    codeMsg.value = '인증 코드를 전송했습니다.'
    startCooldown(60)
  } catch (e) {
    codeMsg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '코드 전송 실패'
  }
}

async function onVerifyCode () {
  codeMsg.value = ''
  try {
    const resp = await verifyEmailCode(emailFull.value, code.value)
    const { verified } = resp?.data ?? resp
    if (verified) {
      emailVerified.value = true
      codeMsg.value = '이메일 인증 완료!'
      clearInterval(cooldownTimer); cooldown.value = 0
    } else {
      emailVerified.value = false
      codeMsg.value = '인증 코드가 일치하지 않습니다.'
    }
  } catch (e) {
    emailVerified.value = false
    codeMsg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '인증 실패'
  }
}

/* Submit: 위에서부터 에러 안내 + 첫 오류 포커스 */
const nameRef = ref(null)
const loginIdRef = ref(null)
const passwordRef = ref(null)
const password2Ref = ref(null)
const birthRef = ref(null)

function focusEl(r){ r?.value && r.value.focus() }

async function onSubmit () {
  msg.value = ''

  // compose fields
  form.email = emailFull.value
  form.phone = `010-${(phone2.value||'').padEnd(4,'')}-${(phone3.value||'').padEnd(4,'')}`

  if (!form.name?.trim() || !isNameValid.value){
    msg.value = !form.name?.trim() ? '이름을 입력하세요.' : '이름은 한글 10자까지 입력 가능합니다.'
    return focusEl(nameRef)
  }
  if (!form.loginId?.trim() || !isLoginIdLenValid.value){
    msg.value = !form.loginId?.trim() ? '아이디를 입력하세요.' : '아이디는 8~20자까지 입력 가능합니다.'
    return focusEl(loginIdRef)
  }
  if (!isPasswordValid.value){
    msg.value = '비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 10~30자여야 합니다.'
    return focusEl(passwordRef)
  }
  if (!isPasswordSame.value){
    msg.value = '비밀번호가 일치하지 않습니다.'
    return focusEl(password2Ref)
  }
  if (!emailFull.value || !isEmailFormatValid.value){
    msg.value = !emailFull.value ? '이메일을 입력하세요.' : '이메일 형식을 확인하세요.'
    return focusEl(emailLocalEl)
  }
  if (!emailVerified.value){
    msg.value = '이메일 인증이 필요합니다.'
    return focusEl(emailLocalEl)
  }
  if (!(onlyDigits(phone2.value).length === 4 && onlyDigits(phone3.value).length === 4)){
    msg.value = '휴대폰 번호는 010-XXXX-XXXX 형식으로 입력하세요.'
    return focusEl(phone2Ref)
  }
  if (!form.birthDate){
    msg.value = '생년월일을 입력하세요.'
    return focusEl(birthRef)
  }
  const age = getAge(form.birthDate)
  if (age === null || age < 19){
    msg.value = '만 19세 이상만 가입할 수 있습니다.'
    return focusEl(birthRef)
  }
  if (!agree.value){
    msg.value = '개인정보 이용에 동의해야 가입할 수 있습니다.'
    return
  }

  loading.value = true
  try {
    await signup({
      loginId: form.loginId.trim(),
      password: form.password,
      name: form.name.trim(),
      email: form.email.trim(),
      phone: form.phone,
      gender: form.gender,
      birthDate: form.birthDate || null
    }, code.value)

    alert('회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.')
    router.push('/login')
  } catch (e) {
    msg.value = e?.response?.data?.error || e?.response?.data?.message || '가입 실패'
  } finally { loading.value = false }
}
</script>

<template>
  <div class="auth-shell">
    <section class="auth-card">
      <!-- Left visual -->
      <aside class="auth-visual">
        <div class="brand">
          <span class="logo-dot"></span>
        <h1>Create <span class="thin">Account</span></h1>
        </div>
        <h2 class="hero">딱 1분이면 충분해요.</h2>
        <p class="hero-sub">한 번의 가입으로 더 빠른 예약을.</p>
      </aside>

      <!-- Right form -->
      <div class="auth-pane">
        <h2 class="title">회원가입</h2>
        <p class="subtitle">아래 정보를 입력해주세요.</p>

        <div class="form">
          <!-- 이름 -->
          <label class="sr-only" for="name">이름</label>
          <input id="name" class="input" :class="{'invalid': form.name && !isNameValid}"
                 ref="nameRef" v-model.trim="form.name" placeholder="이름(한글 최대 10자)" maxlength="10" required />
          <p v-if="form.name && !isNameValid" class="bad help">최대 10자까지 입력 가능합니다.</p>

          <!-- 아이디 + 중복확인 -->
          <div class="row gap wrap">
            <label class="sr-only" for="loginId">아이디</label>
            <input id="loginId" class="input flex1" :class="{'invalid': form.loginId && !isLoginIdLenValid}"
                   ref="loginIdRef" v-model.trim="form.loginId" placeholder="아이디(8~20자)" required />
            <button type="button" class="btn outline" :disabled="idLoading" @click="onCheckId">
              <span v-if="!idLoading">중복확인</span>
              <span v-else class="spinner spinner--inline" aria-label="확인 중"></span>
            </button>
            <span v-if="idCheck!==null" :class="idCheck ? 'ok' : 'bad'">
              {{ idCheck ? '사용가능' : '중복' }}
            </span>
          </div>
          <p v-if="form.loginId && !isLoginIdLenValid" class="bad help">8~20자까지 입력 가능합니다.</p>

          <!-- 비밀번호 + 확인 -->
          <div class="row gap wrap">
            <label class="sr-only" for="pw">비밀번호</label>
            <div class="passwrap flex1">
              <input :type="show1?'text':'password'" id="pw" class="input"
                     :class="{'invalid': form.password && !isPasswordValid}"
                     ref="passwordRef" v-model="form.password"
                     placeholder="비밀번호(영문/숫자/특수문자 포함 10~30자)" required />
              <button type="button" class="eye" @click="show1=!show1" :aria-label="show1?'비밀번호 숨기기':'비밀번호 보기'">
                <svg viewBox="0 0 24 24" class="eye-ico" aria-hidden="true">
                  <path d="M1.5 12s3.5-6.5 10.5-6.5S22.5 12 22.5 12s-3.5 6.5-10.5 6.5S1.5 12 1.5 12Z" fill="none" stroke="currentColor" stroke-width="1.6"/>
                  <circle cx="12" cy="12" r="2.7" fill="none" stroke="currentColor" stroke-width="1.6"/>
                </svg>
              </button>
            </div>

            <label class="sr-only" for="pw2">비밀번호 확인</label>
            <div class="passwrap flex1">
              <input :type="show2?'text':'password'" id="pw2" class="input"
                     :class="{'invalid': form.password2 && !isPasswordSame}"
                     ref="password2Ref" v-model="form.password2" placeholder="비밀번호 확인" required />
              <button type="button" class="eye" @click="show2=!show2" :aria-label="show2?'비밀번호 숨기기':'비밀번호 보기'">
                <svg viewBox="0 0 24 24" class="eye-ico" aria-hidden="true">
                  <path d="M1.5 12s3.5-6.5 10.5-6.5S22.5 12 22.5 12s-3.5 6.5-10.5 6.5S1.5 12 1.5 12Z" fill="none" stroke="currentColor" stroke-width="1.6"/>
                  <circle cx="12" cy="12" r="2.7" fill="none" stroke="currentColor" stroke-width="1.6"/>
                </svg>
              </button>
            </div>
          </div>
          <p v-if="form.password && !isPasswordValid" class="bad help">
            영문(대/소문자 무관), 숫자, 특수문자를 모두 포함하여 10~30자로 입력하세요.
          </p>
          <p v-if="form.password2 && !isPasswordSame" class="bad help">비밀번호가 일치하지 않습니다.</p>

          <!-- 이메일 (로컬 + 도메인 선택/직접입력) -->
          <div class="row gap wrap">
            <label class="sr-only" for="emailLocal">이메일 아이디</label>
            <input id="emailLocal" class="input flex1"
                   ref="emailLocalEl" v-model.trim="emailLocal" placeholder="이메일 아이디" required />
            <span>@</span>
            <select class="input select" v-model="emailDomainMode" aria-label="도메인 선택">
              <option value="gmail.com">gmail.com</option>
              <option value="naver.com">naver.com</option>
              <option value="custom">직접입력</option>
            </select>
            <input v-if="emailDomainMode==='custom'" class="input flex1"
                   ref="emailDomainCustomEl" v-model.trim="emailDomainCustom" placeholder="도메인(예: example.com)" />
            <button type="button" class="btn outline" :disabled="emailLoading" @click="onCheckEmail">
              <span v-if="!emailLoading">중복확인</span>
              <span v-else class="spinner spinner--inline" aria-label="확인 중"></span>
            </button>
            <span v-if="emailCheck!==null" :class="emailCheck ? 'ok' : 'bad'">
              {{ emailCheck ? '사용가능' : '중복' }}
            </span>
          </div>
          <p v-if="emailFull && !isEmailFormatValid" class="bad help">아이디@도메인.최상위도메인 형식으로 입력하세요.</p>

          <!-- 이메일 인증 -->
          <div class="row gap wrap">
            <label class="sr-only" for="code">인증 코드</label>
            <input id="code" class="input flex1" v-model="code" placeholder="인증 코드 입력" />
            <button type="button" class="btn outline" :disabled="cooldown>0" @click="onSendCode">
              {{ cooldown>0 ? `재발송(${cooldown}s)` : '인증코드 발송' }}
            </button>
            <button type="button" class="btn outline" @click="onVerifyCode">확인</button>
          </div>
          <p class="hint" :class="emailVerified ? 'ok' : 'bad'">{{ codeMsg }}</p>

          <!-- 연락처: 숫자만, 각 4자리, 자동 이동 -->
          <div class="row gap wrap">
            <input class="input phone-010" value="010" disabled />
            <input class="input phone" ref="phone2Ref" v-model="phone2"
                   @input="onPhone2Input" placeholder="1234"
                   inputmode="numeric" pattern="[0-9]*" maxlength="4" />
            <span>-</span>
            <input class="input phone" ref="phone3Ref" v-model="phone3"
                   @input="onPhone3Input" placeholder="5678"
                   inputmode="numeric" pattern="[0-9]*" maxlength="4" />
          </div>

          <!-- 성별 -->
          <div class="row gap wrap">
            <label class="radio"><input type="radio" value="MALE" v-model="form.gender" required /> 남자</label>
            <label class="radio"><input type="radio" value="FEMALE" v-model="form.gender" /> 여자</label>
          </div>

          <!-- 생년월일 -->
          <label class="sr-only" for="birth">생년월일</label>
          <input id="birth" class="input" ref="birthRef" v-model="form.birthDate" type="date" placeholder="YYYY-MM-DD" required />

<!--약관동의-->
          <label class="agree">
    <input type="checkbox" v-model="agree" required />
    <span>
      개인정보 이용에 동의하시겠습니까?
      <button class="link-button" @click="showTerms = true">[상세보기]</button>
    </span>
  </label>

  <!-- 모달 -->
  <div v-if="showTerms" class="modal-overlay" @click.self="showTerms = false">
    <div class="modal">
      <h3>개인정보 수집 및 이용 동의</h3>
      
      <p><strong>1. 수집하는 개인정보 항목</strong></p>
      <ul>
        <li>필수 항목: 이름, 생년월일, 아이디, 비밀번호, 이메일, 휴대전화번호</li>
        <li>선택 항목: 성별</li>
      </ul>

      <p><strong>2. 수집 목적</strong></p>
      <ul>
        <li>회원 가입 의사 확인 및 본인 식별/인증</li>
        <li>예약 서비스 제공 및 상담</li>
        <li>고지사항 전달 및 고객 대응</li>
      </ul>

      <p><strong>3. 보유 및 이용 기간</strong></p>
      <ul>
        <li>회원 탈퇴 후 5일까지 (분쟁 해결 및 소비자 보호 목적)</li>
        <li>관련 법령에 따라 별도 보관 시 해당 법령의 보유 기간 준수</li>
      </ul>

      <p><strong>4. 동의 거부 권리</strong></p>
      <ul>
        <li>이용자는 개인정보 수집 및 이용에 대한 동의를 거부할 수 있습니다.</li>
        <li>다만, 동의하지 않을 경우 회원 가입 및 서비스 이용이 제한될 수 있습니다.</li>
      </ul>

      <p><strong>5. 관련 법령 고지</strong></p>
      <ul>
        <li>「개인정보 보호법」 제15조 (개인정보의 수집·이용)</li>
        <li>「정보통신망 이용촉진 및 정보보호 등에 관한 법률」 제22조 (개인정보의 수집·이용 동의 등)</li>
      </ul>

      <p>자세한 내용은 <strong>개인정보 처리방침</strong>을 참고해 주세요.</p>

      <!-- 닫기 버튼 -->
      <div style="text-align: right; margin-top: 16px;">
        <button class="link-button" @click="showTerms = false">닫기</button>
      </div>
    </div>
  </div>


          <div class="row between hint">
            <span>계정이 있으신가요?</span>
            <RouterLink class="link" to="/login">로그인</RouterLink>
          </div>

          <button class="btn primary" :disabled="loading" @click="onSubmit">
            <span v-if="!loading">가입하기</span>
            <span v-else class="spinner" aria-label="처리중"></span>
          </button>

          <p class="msg center" v-if="msg">{{ msg }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ===== Theme Tokens ===== */
:root{
  --bg-1:#f3f8ff; --bg-2:#eef5ff;
  --card:#ffffffee; --card-border:#d6e6ff;
  --text-1:#111827; --text-2:#4a5a75; --text-body:#1f2a37;
  --brand-1:#5daeff; --brand-2:#9fd3ff;
  --focus:rgba(93,174,255,.22);
  --ok:#10b981; --bad:#ef4444;
}

/* 접근성용 숨김 라벨 */
.sr-only{
  position:absolute !important; width:1px; height:1px; padding:0; margin:-1px;
  overflow:hidden; clip:rect(0,0,0,0); white-space:nowrap; border:0;
}

/* Layout */
.auth-shell{
  min-height:100vh; display:grid; place-items:center; padding:48px 16px;
  background:linear-gradient(180deg,var(--bg-1),var(--bg-2)); color:var(--text-body);
  font-family:ui-sans-serif,system-ui,Segoe UI,Roboto,Helvetica,Arial;
}
.auth-card{
  width:min(1040px,95vw); display:grid; grid-template-columns:1.05fr 1.15fr;
  border-radius:18px; background:var(--card); border:1px solid var(--card-border);
  box-shadow:0 18px 60px rgba(16,44,84,.12); overflow:hidden; backdrop-filter:blur(10px);
}
@media (max-width:980px){ .auth-card{ grid-template-columns:1fr } .auth-visual{ display:none } }

/* Left Visual */
.auth-visual{
  padding:48px 40px; border-right:1px solid var(--card-border);
  background:
    radial-gradient(600px 300px at 20% 20%, #d6ebff80, transparent 60%),
    radial-gradient(600px 300px at 80% 80%, #cfe7ff80, transparent 60%),
    linear-gradient(180deg,#f6fbff,#eef6ff);
}
.brand{ display:flex; align-items:center; gap:12px; margin-bottom:12px }
.logo-dot{ width:14px; height:14px; border-radius:50%;
  background:linear-gradient(135deg,var(--brand-1),var(--brand-2));
  box-shadow:0 0 14px var(--brand-1); }
.brand h1{ margin:0; font-weight:800; color:#0f2547; letter-spacing:.2px }
.brand .thin{ font-weight:300; opacity:.95 }
.hero{ margin:14px 0 8px; font-size:26px; line-height:1.34; letter-spacing:.2px; color:#0f2547; word-break:keep-all }
.hero-sub{ margin:0; color:var(--text-2); font-size:14px }

/* Right Pane */
.auth-pane{ padding:36px 32px }
.title{ margin:0 0 6px; font-size:24px; font-weight:800; color:var(--text-1) }
.subtitle{ margin:0 0 20px; color:var(--text-2); font-size:13px }

/* Form */
.form{ display:grid; gap:12px; margin-top:8px }
.row{ display:flex; align-items:center }
.row.between{ justify-content:space-between }
.gap{ gap:10px }
.wrap{ flex-wrap:wrap }
.flex1{ flex:1 1 200px }

/* Inputs */
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

/* Select (도메인) */
.select{ width:auto; min-width:140px; }

/* Password eye */
.passwrap{ position:relative }
.passwrap .input{ padding-right:44px }
.eye{
  position:absolute; top:50%; right:8px; transform:translateY(-50%);
  width:28px; height:28px; display:grid; place-items:center;
  border:0; background:transparent; color:#6a7a99; cursor:pointer; border-radius:8px;
}
.eye:hover{ background:#f2f6ff; color:#0f2547 }
.eye-ico{ width:20px; height:20px; display:block }

/* Phone */
.phone-010{ width:80px; text-align:center; opacity:.7 }
.phone{ width:120px; text-align:center }

/* Buttons */
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

/* Radio & Agree */
.radio{ display:inline-flex; align-items:center; gap:6px; color:var(--text-2); }
.radio input{ accent-color:#2563eb; }
.agree{ display:flex; align-items:center; gap:8px; font-size:13px; color:var(--text-2); }

/* Feedback */
.ok { color:#10b981; font-weight:600; font-size:14px; }
.bad { color:#ef4444; font-weight:600; font-size:14px; }
.help{ margin-top:-4px; }

/* Spinner & Messages */
.spinner{ display:inline-block; width:18px; height:18px; border:2px solid rgba(255,255,255,.6); border-top-color:#ffffff; border-radius:50%; animation:spin 1s linear infinite; }
.spinner--inline{ width:16px; height:16px; border-width:2px; border-color:#9bbcf8; border-top-color:#2563eb; }
@keyframes spin{ to { transform: rotate(360deg) } }

.msg.center{ text-align:center; font-size:13px; color:#e11d48; }
*,*::before,*::after{ box-sizing:border-box; }

.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex; justify-content: center; align-items: center;
  z-index: 1000;
}
.modal {
  background: white;
  border-radius: 12px;
  padding: 24px;
  max-width: 560px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.modal h3 {
  margin-top: 0;
  font-size: 20px;
  font-weight: bold;
  color: #1f2a37;
}
.modal p,
.modal ul {
  margin-bottom: 16px;
  font-size: 14px;
  color: #374151;
}
.modal ul {
  padding-left: 20px;
  list-style-type: disc;
}
.link-button {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
  text-decoration: underline;
  font-size: 13px;
  margin-left: 6px;
}
.link-button:hover {
  text-decoration: none;
}

</style>
<!-- src/views/mypage/Account.vue -->
<template>
  <div class="account">
    <h2>Account</h2>

    <div class="box">
      <!-- Name -->
      <div class="row">
        <div class="label">Name</div>
        <div class="value" v-if="!edit.name">{{ me.name || '-' }}</div>
        <div class="value" v-else>
          <input
            v-model.trim="me.name"
            :class="{'invalid': edit.name && !isNameValid}"
            placeholder="이름(한글 최대 10자)"
            maxlength="10"
          />
          <small v-if="edit.name && !me.name" class="bad help">이름을 입력하세요.</small>
          <small v-else-if="edit.name && !isNameValid" class="bad help">최대 10자까지 입력 가능합니다.</small>
        </div>
        <button class="small" @click="edit.name = !edit.name">
          {{ edit.name ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Email (형식 + 중복확인 + 인증코드) -->
      <div class="row">
        <div class="label">Email</div>
        <div class="value mono" v-if="!edit.email">{{ me.email || '-' }}</div>
        <div class="value" v-else>
          <div class="email-edit">
            <input
              v-model.trim="me.email"
              :class="{'invalid': edit.email && me.email && !isEmailFormatValid}"
              placeholder="example@domain.com"
            />
            <button type="button" class="small" :disabled="emailLoading" @click="onCheckEmail">
              <span v-if="!emailLoading">중복확인</span>
              <span v-else class="spinner spinner--inline" aria-label="확인 중"></span>
            </button>

            <input
              v-model.trim="emailCode"
              class="w120"
              placeholder="인증코드"
              :disabled="!canSendCode"
            />
            <button class="small" :disabled="cooldown>0 || !canSendCode" @click="onSendCode">
              {{ cooldown>0 ? `재발송(${cooldown}s)` : '인증코드 발송' }}
            </button>
            <button class="small" :disabled="!emailCode" @click="onVerifyCode">확인</button>
          </div>

          <small v-if="edit.email && me.email && !isEmailFormatValid" class="bad help">
            아이디@도메인.최상위도메인 형식으로 입력하세요.
          </small>
          <small v-if="emailCheck!==null" :class="emailCheck ? 'ok help' : 'bad help'">
            {{ emailCheck ? '사용가능' : '이미 사용 중인 이메일입니다.' }}
          </small>
          <small v-if="emailMsg" :class="emailVerified ? 'ok help' : 'bad help'">{{ emailMsg }}</small>
          <small v-if="errors.email" class="bad help">{{ errors.email }}</small>
        </div>
        <button class="small" @click="toggleEmailEdit">
          {{ edit.email ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Password (반반 레이아웃 + 눈 아이콘 입력창 안쪽 우측 고정) -->
      <div class="row">
        <div class="label">Password</div>

        <div class="value mono" v-if="!edit.password">********</div>

        <div class="value" v-else>
          <div class="pw-grid">
            <!-- Left: 현재 비밀번호 -->
            <div class="col">
              <div class="pw-head">현재 비밀번호</div>
              <div class="passwrap">
                <input
                  :type="showPwCur ? 'text' : 'password'"
                  v-model="passwordForm.currentPassword"
                  placeholder="현재 비밀번호"
                />
                <button
                  type="button"
                  class="eye"
                  :aria-label="showPwCur ? '현재 비밀번호 숨기기' : '현재 비밀번호 보기'"
                  @click="showPwCur = !showPwCur"
                >
                  <svg viewBox="0 0 24 24" class="eye-ico" aria-hidden="true">
                    <path d="M1.5 12s3.5-6.5 10.5-6.5S22.5 12 22.5 12s-3.5 6.5-10.5 6.5S1.5 12 1.5 12Z" fill="none" stroke="currentColor" stroke-width="1.6"/>
                    <circle cx="12" cy="12" r="2.7" fill="none" stroke="currentColor" stroke-width="1.6"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Right: 새 비밀번호 + 확인 -->
            <div class="col">
              <div class="pw-head">새 비밀번호</div>
              <div class="passwrap">
                <input
                  :type="showPwNew ? 'text' : 'password'"
                  v-model="passwordForm.newPassword"
                  :class="{'invalid': passwordForm.newPassword && !isPasswordValid}"
                  placeholder="영문/숫자/특수문자 포함 10~30자"
                />
                <button
                  type="button"
                  class="eye"
                  :aria-label="showPwNew ? '새 비밀번호 숨기기' : '새 비밀번호 보기'"
                  @click="showPwNew = !showPwNew"
                >
                  <svg viewBox="0 0 24 24" class="eye-ico" aria-hidden="true">
                    <path d="M1.5 12s3.5-6.5 10.5-6.5S22.5 12 22.5 12s-3.5 6.5-10.5 6.5S1.5 12 1.5 12Z" fill="none" stroke="currentColor" stroke-width="1.6"/>
                    <circle cx="12" cy="12" r="2.7" fill="none" stroke="currentColor" stroke-width="1.6"/>
                  </svg>
                </button>
              </div>
              <small v-if="passwordForm.newPassword && !isPasswordValid" class="bad help">
                영문(대/소문자 무관), 숫자, 특수문자를 모두 포함하여 10~30자로 입력하세요.
              </small>

              <div class="passwrap mt8">
                <input
                  :type="showPwConfirm ? 'text' : 'password'"
                  v-model="passwordForm.confirmPassword"
                  :class="{'invalid': passwordForm.confirmPassword && !isPasswordSame}"
                  placeholder="새 비밀번호 확인"
                />
                <button
                  type="button"
                  class="eye"
                  :aria-label="showPwConfirm ? '비밀번호 확인 숨기기' : '비밀번호 확인 보기'"
                  @click="showPwConfirm = !showPwConfirm"
                >
                  <svg viewBox="0 0 24 24" class="eye-ico" aria-hidden="true">
                    <path d="M1.5 12s3.5-6.5 10.5-6.5S22.5 12 22.5 12s-3.5 6.5-10.5 6.5S1.5 12 1.5 12Z" fill="none" stroke="currentColor" stroke-width="1.6"/>
                    <circle cx="12" cy="12" r="2.7" fill="none" stroke="currentColor" stroke-width="1.6"/>
                  </svg>
                </button>
              </div>
              <small v-if="passwordForm.confirmPassword && !isPasswordSame" class="bad help">
                비밀번호가 일치하지 않습니다.
              </small>
            </div>
          </div>

          <small v-if="errors.password" class="bad help">{{ errors.password }}</small>
        </div>

        <button class="small" @click="togglePasswordEdit">
          {{ edit.password ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Phone -->
      <div class="row">
        <div class="label">Phone number</div>
        <div class="value" v-if="!edit.phone">{{ me.phone || '-' }}</div>
        <div class="value" v-else>
          <input
            :value="me.phone"
            @input="onPhoneInput"
            inputmode="tel"
            placeholder="010-1234-5678"
            :maxlength="13"
            :class="{'invalid': me.phone && errors.phone}"
          />
          <small v-if="errors.phone" class="bad help">{{ errors.phone }}</small>
        </div>
        <button class="small" @click="edit.phone = !edit.phone">
          {{ edit.phone ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Address -->
      <div class="row">
        <div class="label">Address</div>
        <div class="value" v-if="!edit.addr">
          <div>{{ me.address1 || '-' }}</div>
          <div class="sub">{{ me.address2 }}</div>
          <div class="sub">({{ me.postcode || '우편번호 없음' }})</div>
        </div>
        <div class="value addr-edit" v-else>
          <div class="addr-row">
            <input
              v-model.trim="me.postcode"
              class="w120"
              inputmode="numeric"
              maxlength="5"
              placeholder="우편번호"
              readonly
            />
            <button type="button" class="small" @click="showPostcode = true">주소 찾기</button>
          </div>
          <input v-model.trim="me.address1" placeholder="도로명 주소" readonly />
          <input ref="address2Ref" v-model.trim="me.address2" placeholder="상세 주소" />
        </div>
        <button class="small" @click="edit.addr = !edit.addr">
          {{ edit.addr ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- DOB -->
      <div class="row">
        <div class="label">Date of birth</div>
        <div class="value mono">{{ birth || '-' }}</div>
        <button class="small" disabled>Change</button>
      </div>

      <!-- Gender -->
      <div class="row">
        <div class="label">Gender</div>
        <div class="value" v-if="!edit.gender">{{ me.gender }}</div>
        <div class="value" v-else>
          <select v-model="me.gender">
            <option>MALE</option>
            <option>FEMALE</option>
            <option>UNKNOWN</option>
          </select>
        </div>
        <button class="small" @click="edit.gender = !edit.gender">
          {{ edit.gender ? 'Done' : 'Change' }}
        </button>
      </div>
    </div>

    <!-- 저장 버튼 -->
    <div class="actions">
      <button class="save" :disabled="isSaveDisabled" @click="save">저장</button>
    </div>

    <!-- 쿠폰함 -->
    <div class="coupon">
      <RouterLink to="/mypage/coupon" class="btn">쿠폰함 이동</RouterLink>
    </div>

    <!-- 우편번호 모달 -->
    <PostcodeSearch v-if="showPostcode" @close="showPostcode = false" @select="onSelectPostcode" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { RouterLink } from 'vue-router'
import api, { checkEmail, sendEmailCode, verifyEmailCode, updateMyEmail } from '@/api/auth'
import PostcodeSearch from '@/views/mypage/PostcodeSearch.vue'

/* ---------- State ---------- */
const me = ref({
  email: '', birthDate: null, name: '', phone: '',
  address1: '', address2: '', postcode: '', gender: 'UNKNOWN'
})

const edit = ref({
  name: false, phone: false, addr: false, gender: false, password: false, email: false
})

const saving = ref(false)
const errors = ref({ phone: '', password: '', email: '' })

/* 이메일 */
const emailCheck = ref(null)
const emailLoading = ref(false)
const emailCode = ref('')
const emailVerified = ref(false)
const emailMsg = ref('')
const cooldown = ref(0)
let cooldownTimer = null

/* 비밀번호 눈 토글 */
const showPwCur = ref(false)
const showPwNew = ref(false)
const showPwConfirm = ref(false)

/* 비밀번호 변경 입력 */
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/* 우편번호 */
const showPostcode = ref(false)
const address2Ref = ref(null)

/* 규칙 */
const rxPassword = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^\w\s]).{10,30}$/
const isPasswordValid = computed(() => {
  if (!edit.value.password) return true
  if (!passwordForm.value.newPassword) return true
  return rxPassword.test(passwordForm.value.newPassword)
})
const isPasswordSame = computed(() =>
  !edit.value.password || passwordForm.value.newPassword === passwordForm.value.confirmPassword
)
const isNameValid = computed(() => !edit.value.name || (!!me.value.name && [...me.value.name].length <= 10))
const isEmailFormatValid = computed(() =>
  !edit.value.email || (!!me.value.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(me.value.email))
)

const initialEmail = ref('')
const emailChanged = computed(() => edit.value.email && me.value.email && me.value.email !== initialEmail.value)
const birth = computed(() => me.value.birthDate ?? '')

/* 전화번호 */
const phoneRe = /^01[016-9]-\d{3,4}-\d{4}$/
function formatPhone(v) {
  const d = (v || '').replace(/\D/g, '').slice(0, 11)
  if (d.length <= 3) return d
  if (d.length < 7) return `${d.slice(0, 3)}-${d.slice(3)}`
  if (d.length < 11) return `${d.slice(0, 3)}-${d.slice(3, 6)}-${d.slice(6)}`
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`
}
function onPhoneInput(e) {
  me.value.phone = formatPhone(e.target.value)
  errors.value.phone =
    me.value.phone && !phoneRe.test(me.value.phone)
      ? '휴대폰 번호는 010-1234-5678 형식이어야 합니다.'
      : ''
}

/* 초기 로드 */
onMounted(async () => {
  const { data } = await api.get('/users/me')
  me.value = data
  me.value.phone = formatPhone(me.value.phone)
  initialEmail.value = me.value.email || ''
})

/* 주소 선택 */
function onSelectPostcode({ zonecode, address1 }) {
  me.value.postcode = zonecode
  me.value.address1 = address1
  requestAnimationFrame(() => address2Ref.value?.focus())
}

/* 이메일 중복확인 헬퍼 */
function getAvailable(res) {
  if (!res) return undefined
  if (typeof res.available !== 'undefined') return res.available
  if (res.data && typeof res.data.available !== 'undefined') return res.data.available
  return undefined
}

/* 이메일 중복확인 */
async function onCheckEmail() {
  errors.value.email = ''
  emailCheck.value = null
  emailVerified.value = false
  emailMsg.value = ''

  if (!me.value.email) { errors.value.email = '이메일을 입력하세요.'; return }
  if (!isEmailFormatValid.value) { errors.value.email = '이메일 형식을 확인하세요.'; return }

  if (me.value.email === initialEmail.value) {
    emailCheck.value = true
    emailMsg.value = '현재 이메일과 동일합니다.'
    return
  }

  emailLoading.value = true
  try {
    const res = await checkEmail(me.value.email.trim())
    const available = getAvailable(res)
    if (typeof available === 'undefined') throw new Error('서버 응답 형식이 올바르지 않습니다.')
    emailCheck.value = !!available
    if (!available) emailMsg.value = '이미 사용 중인 이메일입니다.'
  } catch (e) {
    emailCheck.value = false
    errors.value.email = e?.response?.data?.message || e?.response?.data?.error || e?.message || '이메일 확인 실패'
  } finally {
    emailLoading.value = false
  }
}

/* 인증코드 타이머 */
function startCooldown(sec){
  clearInterval(cooldownTimer)
  cooldown.value = sec
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}
onBeforeUnmount(()=> clearInterval(cooldownTimer))

/* 코드 발송 가능 */
const canSendCode = computed(() =>
  edit.value.email &&
  isEmailFormatValid.value &&
  emailCheck.value !== false &&
  me.value.email &&
  me.value.email !== initialEmail.value
)

/* 인증코드 발송 */
async function onSendCode(){
  emailMsg.value = ''
  emailVerified.value = false
  if (!canSendCode.value){ emailMsg.value = '이메일 중복확인을 먼저 완료하세요.'; return }
  try {
    await sendEmailCode(me.value.email.trim())
    emailMsg.value = '인증 코드를 전송했습니다.'
    startCooldown(60)
  } catch (e) {
    emailMsg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '코드 전송 실패'
  }
}

/* 코드 검증 */
async function onVerifyCode(){
  emailMsg.value = ''
  if (!emailCode.value){ emailMsg.value = '인증 코드를 입력하세요.'; return }
  try {
    const r = await verifyEmailCode(me.value.email.trim(), emailCode.value.trim())
    const { verified } = r?.data ?? r
    emailVerified.value = !!verified
    emailMsg.value = verified ? '이메일 인증 완료!' : '인증 코드가 일치하지 않습니다.'
    if (verified){ clearInterval(cooldownTimer); cooldown.value = 0 }
  } catch (e) {
    emailVerified.value = false
    emailMsg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '인증 실패'
  }
}

/* 이메일 편집 토글 */
function toggleEmailEdit() {
  if (edit.value.email) {
    errors.value.email = ''
    emailCheck.value = null
    emailCode.value = ''
    emailVerified.value = false
    emailMsg.value = ''
    clearInterval(cooldownTimer); cooldown.value = 0
    me.value.email = initialEmail.value
  }
  edit.value.email = !edit.value.email
}

/* 저장 가능 여부 */
const isSaveDisabled = computed(() => {
  if (saving.value) return true
  if (edit.value.name && (!me.value.name || !isNameValid.value)) return true
  if (emailChanged.value) {
    if (!isEmailFormatValid.value) return true
    if (emailCheck.value === false) return true
    if (!emailVerified.value) return true
  }
  if (me.value.phone && errors.value.phone) return true
  if (edit.value.password) {
    if (!passwordForm.value.currentPassword || !passwordForm.value.newPassword || !passwordForm.value.confirmPassword) return true
    if (!isPasswordValid.value || !isPasswordSame.value) return true
  }
  return false
})

/* 저장 */
async function save() {
  if (isSaveDisabled.value) return
  saving.value = true
  try {
    if (emailChanged.value && emailVerified.value) {
      await updateMyEmail({
        email: me.value.email.trim(),
        verificationCode: emailCode.value.trim()
      })
      initialEmail.value = me.value.email
      emailVerified.value = false
      emailCode.value = ''
      emailMsg.value = '이메일이 변경되었습니다.'
    }

    await api.put('/users/me', {
      name: me.value.name,
      phone: me.value.phone || null,
      address1: me.value.address1,
      address2: me.value.address2,
      postcode: me.value.postcode || null,
      gender: me.value.gender
    })

    if (edit.value.password) {
      await api.put('/users/me/password', {
        currentPassword: passwordForm.value.currentPassword,
        newPassword: passwordForm.value.newPassword
      })
      alert('비밀번호가 변경되었습니다.')
    }

    alert('저장 완료')
    edit.value.name = edit.value.phone = edit.value.addr = edit.value.gender = false
    edit.value.password = false
    edit.value.email = false
    emailCheck.value = null
    emailMsg.value ||= ''
  } catch (e) {
    alert(e?.response?.data?.message || '저장 실패')
  } finally {
    saving.value = false
  }
}

/* 비밀번호 수정 토글 (리셋) */
function togglePasswordEdit() {
  if (edit.value.password) {
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    errors.value.password = ''
    showPwCur.value = showPwNew.value = showPwConfirm.value = false
  }
  edit.value.password = !edit.value.password
}
</script>

<style scoped>
/* 전체 레이아웃 */
.account h2 { margin-bottom: 12px; }
.box { border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; }

/* 항목 줄 하나 */
.row {
  display: grid; grid-template-columns: 160px 1fr auto; align-items: center;
  gap: 12px; padding: 14px 16px; border-bottom: 1px solid #f1f5f9;
}
.row:last-child { border-bottom: 0; }

/* 라벨/값 */
.label { color: #6b7280; font-size: 13px; }
.value { color: #111827; }
.value.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
.value .sub { color: #6b7280; font-size: 12px; }

/* 이메일 편집 줄 배치 */
.email-edit { display: flex; gap: 8px; align-items: center; }

/* 주소 입력 스타일 */
.addr-edit { display: grid; gap: 8px; grid-template-columns: 1fr; }
.addr-row { display:flex; gap:8px; align-items:center; }
.w120 { width:120px; text-align:center; }

/* 입력 필드 공통 */
input, select {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  box-sizing: border-box;
  outline: none;
}

/* 비밀번호 반반 레이아웃 */
.pw-grid{
  display:grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}
.pw-grid .col{ min-width: 0; }
.pw-head{ font-size:13px; color:#6b7280; margin-bottom:6px; }

/* 비밀번호 눈 아이콘: 입력창 내부 우측 고정 */
.passwrap{
  position: relative;
  width: 100%;
  display: inline-block;
}
.passwrap input{
  width: 100%;
  padding-right: 44px; /* 아이콘 자리 확보 */
}
.eye{
  position: absolute;
  top: 50%;
  right: 10px;              /* 맨 오른쪽으로 밀착 */
  transform: translateY(-50%);
  width: 28px; height: 28px;
  display: grid; place-items: center;
  border: 0; background: transparent;
  color: #6a7a99; cursor: pointer; border-radius: 8px;
  outline: none;
}
.eye:hover{ background:#f2f6ff; color:#0f2547; }
.eye:focus-visible{ box-shadow: 0 0 0 2px rgba(37,99,235,.25); border-radius: 8px; }
.eye-ico{ width: 20px; height: 20px; display: block; }
.mt8{ margin-top:8px; }

/* 유효성 피드백 */
.ok { color:#10b981; font-size: 12px; }
.bad { color:#ef4444; font-size: 12px; }
.help { display:block; margin-top: 4px; }
.invalid { border-color:#fecaca !important; box-shadow:0 0 0 3px rgba(239,68,68,.15); }

/* 버튼들 */
.small {
  padding: 8px 10px; border: 1px solid #e5e7eb; background: #fff;
  border-radius: 10px; cursor: pointer; font-size: 12px;
}
.small[disabled] { opacity: .6; cursor: not-allowed; }

/* 저장 버튼 영역 */
.actions { margin-top: 14px; display: flex; justify-content: flex-end; }
.save {
  padding: 10px 14px; border: 0; border-radius: 8px; background: #0a6; color: #fff; cursor: pointer;
}
.save[disabled]{ opacity:.6; cursor:not-allowed; }

/* 쿠폰 버튼 */
.coupon { margin-top: 16px; }
.btn { padding: 10px 14px; border-radius: 8px; border: 1px solid #0a6; color: #0a6; text-decoration: none; }

/* 인라인 스피너 */
.spinner--inline{ display:inline-block; width:14px; height:14px; border:2px solid #cbd5e1; border-top-color:#2563eb; border-radius:50%; animation:spin 1s linear infinite; }
@keyframes spin{ to { transform: rotate(360deg) } }

/* 반응형 */
@media (max-width: 760px){
  .pw-grid{ grid-template-columns: 1fr; }
}
</style>
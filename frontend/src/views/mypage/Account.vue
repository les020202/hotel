<!-- src/views/mypage/Account.vue -->
<template>
  <div class="account">
    <h2>Account</h2>

    <div class="box">
      <!-- Name (이름 수정: 10자 제한 안내) -->
      <div class="row">
        <div class="label">Name</div>
        <!-- 읽기 모드 -->
        <div class="value" v-if="!edit.name">{{ me.name || '-' }}</div>
        <!-- 수정 모드 -->
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

      <!-- Email (변경 가능: 형식 + 중복확인) -->
      <div class="row">
        <div class="label">Email</div>

        <!-- 읽기 모드 -->
        <div class="value mono" v-if="!edit.email">{{ me.email || '-' }}</div>

        <!-- 수정 모드 -->
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
          </div>
          <small v-if="edit.email && me.email && !isEmailFormatValid" class="bad help">
            아이디@도메인.최상위도메인 형식으로 입력하세요.
          </small>
          <small v-if="emailCheck!==null" :class="emailCheck ? 'ok help' : 'bad help'">
            {{ emailCheck ? '사용가능' : '이미 사용 중인 이메일입니다.' }}
          </small>
          <small v-if="errors.email" class="bad help">{{ errors.email }}</small>
        </div>

        <button class="small" @click="toggleEmailEdit">
          {{ edit.email ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Password (비밀번호 변경 가능: 규칙 + 확인 일치) -->
      <div class="row">
        <div class="label">Password</div>

        <!-- 읽기 모드 -->
        <div class="value mono" v-if="!edit.password">********</div>

        <!-- 수정 모드 -->
        <div class="value" v-else>
          <input
            type="password"
            v-model="passwordForm.currentPassword"
            placeholder="현재 비밀번호"
          />
          <input
            type="password"
            v-model="passwordForm.newPassword"
            :class="{'invalid': passwordForm.newPassword && !isPasswordValid}"
            placeholder="새 비밀번호 (영문/숫자/특수문자 포함 10~30자)"
          />
          <small v-if="passwordForm.newPassword && !isPasswordValid" class="bad help">
            영문(대/소문자 무관), 숫자, 특수문자를 모두 포함하여 10~30자로 입력하세요.
          </small>
          <input
            type="password"
            v-model="passwordForm.confirmPassword"
            :class="{'invalid': passwordForm.confirmPassword && !isPasswordSame}"
            placeholder="새 비밀번호 확인"
          />
          <small v-if="passwordForm.confirmPassword && !isPasswordSame" class="bad help">
            비밀번호가 일치하지 않습니다.
          </small>
          <small v-if="errors.password" class="bad help">{{ errors.password }}</small>
        </div>

        <button class="small" @click="togglePasswordEdit">
          {{ edit.password ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Phone (전화번호 수정: 형식 유지) -->
      <div class="row">
        <div class="label">Phone number</div>
        <!-- 읽기 모드 -->
        <div class="value" v-if="!edit.phone">{{ me.phone || '-' }}</div>
        <!-- 수정 모드 -->
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

      <!-- Address (우편번호 API 연동) -->
      <div class="row">
        <div class="label">Address</div>

        <!-- 읽기 모드 -->
        <div class="value" v-if="!edit.addr">
          <div>{{ me.address1 || '-' }}</div>
          <div class="sub">{{ me.address2 }}</div>
          <div class="sub">({{ me.postcode || '우편번호 없음' }})</div>
        </div>

        <!-- 수정 모드 -->
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

      <!-- Date of birth (읽기 전용) -->
      <div class="row">
        <div class="label">Date of birth</div>
        <div class="value mono">{{ birth || '-' }}</div>
        <button class="small" disabled>Change</button>
      </div>

      <!-- Gender (성별 수정) -->
      <div class="row">
        <div class="label">Gender</div>
        <!-- 읽기 모드 -->
        <div class="value" v-if="!edit.gender">{{ me.gender }}</div>
        <!-- 수정 모드 -->
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
      <button
        class="save"
        :disabled="isSaveDisabled"
        @click="save"
      >
        저장
      </button>
    </div>

    <!-- 쿠폰함 이동 -->
    <div class="coupon">
      <RouterLink to="/coupon" class="btn">쿠폰함 이동</RouterLink>
    </div>

    <!-- 우편번호 모달 -->
    <PostcodeSearch
      v-if="showPostcode"
      @close="showPostcode = false"
      @select="onSelectPostcode"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import api, { checkEmail } from '@/api/auth'
import PostcodeSearch from '@/views/mypage/PostcodeSearch.vue'

// ---------- State ----------
const me = ref({
  email: '', birthDate: null, name: '', phone: '',
  address1: '', address2: '', postcode: '', gender: 'UNKNOWN'
})

const edit = ref({
  name: false, phone: false, addr: false, gender: false, password: false, email: false
})

const saving = ref(false)
const errors = ref({ phone: '', password: '', email: '' })

// 이메일 중복확인 상태
const emailCheck = ref(null) // null | true | false
const emailLoading = ref(false)

// 비밀번호 변경 입력
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 우편번호 모달/상세주소 포커스
const showPostcode = ref(false)
const address2Ref = ref(null)

// 정규식/규칙 (회원가입과 동일)
const rxPassword = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^\w\s]).{10,30}$/
const isPasswordValid = computed(() => {
  if (!edit.value.password) return true
  if (!passwordForm.value.newPassword) return true
  return rxPassword.test(passwordForm.value.newPassword)
})
const isPasswordSame = computed(() => {
  if (!edit.value.password) return true
  return passwordForm.value.newPassword === passwordForm.value.confirmPassword
})

// 이름: 1~10자
const isNameValid = computed(() => {
  if (!edit.value.name) return true
  if (!me.value.name) return false
  return [...me.value.name].length <= 10
})

// 이메일 형식
const isEmailFormatValid = computed(() => {
  if (!edit.value.email) return true
  if (!me.value.email) return false
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(me.value.email)
})

// 생일 표기
const birth = computed(() => me.value.birthDate ?? '')

// 전화번호 포맷/검증
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

// 내 정보 로드
onMounted(async () => {
  const { data } = await api.get('/users/me')
  me.value = data
  me.value.phone = formatPhone(me.value.phone)
})

// 우편번호 선택 처리
function onSelectPostcode({ zonecode, address1 }) {
  me.value.postcode = zonecode
  me.value.address1 = address1
  requestAnimationFrame(() => address2Ref.value?.focus())
}

// 이메일 중복확인
function getAvailable(res) {
  if (!res) return undefined
  if (typeof res.available !== 'undefined') return res.available
  if (res.data && typeof res.data.available !== 'undefined') return res.data.available
  return undefined
}
async function onCheckEmail() {
  errors.value.email = ''
  emailCheck.value = null

  if (!me.value.email) {
    errors.value.email = '이메일을 입력하세요.'
    return
  }
  if (!isEmailFormatValid.value) {
    errors.value.email = '이메일 형식을 확인하세요.'
    return
  }

  emailLoading.value = true
  try {
    const res = await checkEmail(me.value.email.trim())
    const available = getAvailable(res)
    if (typeof available === 'undefined') throw new Error('서버 응답 형식이 올바르지 않습니다.')
    emailCheck.value = !!available
  } catch (e) {
    emailCheck.value = false
    errors.value.email = e?.response?.data?.message || e?.response?.data?.error || e?.message || '이메일 확인 실패'
  } finally {
    emailLoading.value = false
  }
}

// 이메일 편집 토글 시 상태 초기화
function toggleEmailEdit() {
  if (edit.value.email) {
    errors.value.email = ''
    emailCheck.value = null
  }
  edit.value.email = !edit.value.email
}

// 저장 버튼 비활성 조건
const isSaveDisabled = computed(() => {
  if (saving.value) return true
  // 이름 검증
  if (edit.value.name && (!me.value.name || !isNameValid.value)) return true
  // 이메일 검증
  if (edit.value.email) {
    if (!me.value.email || !isEmailFormatValid.value) return true
    // 중복확인 실패 시 저장 불가
    if (emailCheck.value === false) return true
  }
  // 전화번호 검증
  if (me.value.phone && errors.value.phone) return true
  // 비밀번호 검증(편집 중일 때만)
  if (edit.value.password) {
    if (!passwordForm.value.currentPassword || !passwordForm.value.newPassword || !passwordForm.value.confirmPassword) return true
    if (!isPasswordValid.value || !isPasswordSame.value) return true
  }
  return false
})

// 저장
async function save() {
  if (isSaveDisabled.value) return

  saving.value = true
  try {
    // 1) 기본 사용자 정보 업데이트 (이름/전화/주소/성별/이메일)
    await api.put('/users/me', {
      name: me.value.name,
      phone: me.value.phone || null,
      address1: me.value.address1,
      address2: me.value.address2,
      postcode: me.value.postcode || null,
      gender: me.value.gender,
      email: me.value.email
    })

    // 2) 비밀번호 변경
    if (edit.value.password) {
      await api.put('/users/me/password', {
        currentPassword: passwordForm.value.currentPassword,
        newPassword: passwordForm.value.newPassword
      })
      alert('비밀번호가 변경되었습니다.')
    }

    alert('저장 완료')
    window.location.reload()
  } catch (e) {
    alert(e?.response?.data?.message || '저장 실패')
  } finally {
    saving.value = false
  }
}

// 비밀번호 수정 토글 (리셋)
function togglePasswordEdit() {
  if (edit.value.password) {
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    errors.value.password = ''
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

/* 입력 필드 */
input, select {
  padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 8px;
}

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
</style>

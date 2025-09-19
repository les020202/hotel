<template>
  <div class="account">
    <h2>Account</h2>

    <div class="box">
      <!-- Name (이름 수정) -->
      <div class="row">
        <div class="label">Name</div>
        <!-- 읽기 모드 -->
        <div class="value" v-if="!edit.name">{{ me.name || '-' }}</div>
        <!-- 수정 모드 -->
        <div class="value" v-else>
          <input v-model="me.name" />
        </div>
        <button class="small" @click="edit.name = !edit.name">
          {{ edit.name ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Email (읽기 전용, 변경 불가) -->
      <div class="row">
        <div class="label">Email</div>
        <div class="value mono">{{ me.email || '-' }}</div>
        <button class="small" disabled>Change</button>
      </div>

      <!-- Password (비밀번호 변경 가능) -->
      <div class="row">
        <div class="label">Password</div>
        <!-- 읽기 모드: 마스킹된 비밀번호 표시 -->
        <div class="value mono" v-if="!edit.password">********</div>
        <!-- 수정 모드: 현재/새 비밀번호 입력 -->
        <div class="value" v-else>
          <input
            type="password"
            v-model="passwordForm.currentPassword"
            placeholder="현재 비밀번호"
          />
          <input
            type="password"
            v-model="passwordForm.newPassword"
            placeholder="새 비밀번호"
          />
          <input
            type="password"
            v-model="passwordForm.confirmPassword"
            placeholder="새 비밀번호 확인"
          />
          <!-- 비밀번호 에러 표시 -->
          <small v-if="errors.password" class="err">{{ errors.password }}</small>
        </div>
        <button class="small" @click="togglePasswordEdit">
          {{ edit.password ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Phone (전화번호 수정) -->
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
          />
          <!-- 전화번호 에러 메시지 -->
          <small v-if="errors.phone" class="err">{{ errors.phone }}</small>
        </div>
        <button class="small" @click="edit.phone = !edit.phone">
          {{ edit.phone ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Address (주소 수정) -->
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
          <input v-model="me.address1" placeholder="주소1" />
          <input v-model="me.address2" placeholder="주소2" />
          <input
            v-model="me.postcode"
            inputmode="numeric"
            maxlength="5"
            placeholder="우편번호"
          />
        </div>
        <button class="small" @click="edit.addr = !edit.addr">
          {{ edit.addr ? 'Done' : 'Change' }}
        </button>
      </div>

      <!-- Date of birth (읽기 전용, 변경 불가) -->
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
        :disabled="!!errors.phone || saving"
        @click="save"
      >
        저장
      </button>
    </div>

    <!-- 쿠폰함 이동 -->
    <div class="coupon">
      <RouterLink to="/coupon" class="btn">쿠폰함 이동</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '@/api/auth'

// 사용자 정보 (기본값 초기화)
const me = ref({
  email: '', birthDate: null, name: '', phone: '',
  address1: '', address2: '', postcode: '', gender: 'UNKNOWN'
})

// 어떤 항목을 수정 모드로 열지 상태 저장
const edit = ref({
  name: false, phone: false, addr: false, gender: false, password: false
})

// 저장 버튼 상태
const saving = ref(false)

// 에러 메시지 저장
const errors = ref({ phone: '', password: '' })

// 비밀번호 변경 입력값 저장
const passwordForm = ref({
  currentPassword: '',   // 현재 비밀번호
  newPassword: '',       // 새 비밀번호
  confirmPassword: ''    // 새 비밀번호 확인
})

// 생일 표시용 computed (백엔드에서 가져온 birthDate 그대로 보여줌)
const birth = computed(() => me.value.birthDate ?? '')

// 휴대폰 번호 정규식 (010-1234-5678 형태만 허용)
const phoneRe = /^01[016-9]-\d{3,4}-\d{4}$/

// 숫자 입력값을 휴대폰 형식으로 자동 변환
function formatPhone(v) {
  const d = (v || '').replace(/\D/g, '').slice(0, 11) // 숫자만 추출
  if (d.length <= 3) return d
  if (d.length < 7) return `${d.slice(0, 3)}-${d.slice(3)}`
  if (d.length < 11) return `${d.slice(0, 3)}-${d.slice(3, 6)}-${d.slice(6)}`
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7, 11)}`
}

// 전화번호 입력 이벤트 처리
function onPhoneInput(e) {
  me.value.phone = formatPhone(e.target.value)
  errors.value.phone =
    me.value.phone && !phoneRe.test(me.value.phone)
      ? '휴대폰 번호는 010-1234-5678 형식이어야 합니다.'
      : ''
}

// 컴포넌트 마운트 시 내 정보 불러오기
onMounted(async () => {
  const { data } = await api.get('/users/me') // 백엔드에서 사용자 정보 가져오기
  me.value = data
  me.value.phone = formatPhone(me.value.phone)
})

// 저장하기 (이름/전화번호/주소/성별 + 비밀번호 변경)
async function save() {
  // 전화번호 유효성 검사
  if (me.value.phone && !phoneRe.test(me.value.phone)) {
    errors.value.phone = '휴대폰 번호는 010-1234-5678 형식이어야 합니다.'
    return
  }
  saving.value = true
  try {
    // 기본 사용자 정보 저장
    await api.put('/users/me', {
      name: me.value.name,
      phone: me.value.phone || null,
      address1: me.value.address1,
      address2: me.value.address2,
      postcode: me.value.postcode || null,
      gender: me.value.gender
    })

    // 비밀번호 변경 처리
    if (edit.value.password) {
      // 새 비밀번호 확인 검증
      if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
        errors.value.password = '새 비밀번호가 일치하지 않습니다.'
        saving.value = false
        return
      }
      // 백엔드에 비밀번호 변경 요청
      await api.put('/users/me/password', {
        currentPassword: passwordForm.value.currentPassword,
        newPassword: passwordForm.value.newPassword
      })
      alert('비밀번호가 변경되었습니다.')
    }

    alert('저장 완료')
    window.location.reload() // 새로고침해서 반영
  } catch (e) {
    alert(e?.response?.data?.message || '저장 실패')
  } finally {
    saving.value = false
  }
}

// 비밀번호 수정 모드 on/off
function togglePasswordEdit() {
  if (edit.value.password) {
    // Done 눌렀을 때 값 초기화
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
/* 라벨 스타일 */
.label { color: #6b7280; font-size: 13px; }
/* 값 스타일 */
.value { color: #111827; }
.value.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
.value .sub { color: #6b7280; font-size: 12px; }
/* 주소 입력 스타일 */
.addr-edit { display: grid; gap: 8px; grid-template-columns: 1fr; }
/* 입력 필드 */
input, select {
  padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 8px;
}
/* 작은 버튼 (Change/Done) */
.small {
  padding: 8px 10px; border: 1px solid #e5e7eb; background: #fff;
  border-radius: 10px; cursor: pointer; font-size: 12px;
}
.small[disabled] { opacity: .6; cursor: not-allowed; }
/* 에러 메시지 */
.err { color: #e11d48; font-size: 12px; margin-top: 4px; display: block; }

/* 저장 버튼 영역 */
.actions { margin-top: 14px; display: flex; justify-content: flex-end; }
.save {
  padding: 10px 14px; border: 0; border-radius: 8px; background: #0a6; color: #fff; cursor: pointer;
}
.save[disabled]{ opacity:.6; cursor:not-allowed; }

/* 쿠폰 버튼 */
.coupon { margin-top: 16px; }
.btn { padding: 10px 14px; border-radius: 8px; border: 1px solid #0a6; color: #0a6; text-decoration: none; }
</style>

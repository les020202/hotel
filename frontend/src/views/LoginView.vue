<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { login as apiLogin } from '@/api/auth'
import HeroSlideshow from '@/components/HeroSlideshow.vue'

const router = useRouter()
const loginId = ref('')
const password = ref('')
const msg = ref('')
const loading = ref(false)
const show = ref(false)
const rememberId = ref(false)

const heroImages = [
  { src: '/hero/hotel-1.jpg', alt: '야경 호텔' },
  { src: '/hero/hotel-2.jpg', alt: '바다 전망 리조트' },
  { src: '/hero/hotel-3.jpg', alt: '숲 속 힐링 리조트' }
]

// reCAPTCHA 관련
const recaptchaSiteKey = import.meta.env.VITE_RECAPTCHA_SITEKEY || '본인_SITE_KEY'
const recaptchaToken = ref(null)
const recaptchaWidgetId = ref(null)
const recaptchaLoaded = ref(false)
let scriptEl = null

function parseJwt (token) {
  try { return JSON.parse(atob(token.split('.')[1])) } catch { return null }
}

function getRoleFromPayload (user) {
  if (!user) return null
  if (user.role) return user.role.startsWith('ROLE_') ? user.role : `ROLE_${user.role}`
  if (Array.isArray(user.authorities) && user.authorities.length) {
    const r = user.authorities[0]; return r.startsWith('ROLE_') ? r : `ROLE_${r}`
  }
  if (Array.isArray(user.roles) && user.roles.length) {
    const r = user.roles[0]; return r.startsWith('ROLE_') ? r : `ROLE_${r}`
  }
  return null
}

onMounted(() => {
  const saved = localStorage.getItem('remember_login_id')
  if (saved) { loginId.value = saved; rememberId.value = true }

  // reCAPTCHA 스크립트 동적 로드 (v2)
  if (!window.grecaptcha) {
    scriptEl = document.createElement('script')
    scriptEl.src = 'https://www.google.com/recaptcha/api.js?render=explicit'
    scriptEl.async = true
    scriptEl.defer = true
    scriptEl.onload = () => {
      // 렌더링은 약간 시간차가 있으므로 setTimeout으로 안전하게 처리
      setTimeout(() => {
        tryRenderRecaptcha()
      }, 200)
    }
    document.head.appendChild(scriptEl)
  } else {
    tryRenderRecaptcha()
  }
})

onUnmounted(() => {
  // cleanup
  if (scriptEl && scriptEl.parentNode) scriptEl.parentNode.removeChild(scriptEl)
  if (recaptchaWidgetId.value != null && window.grecaptcha) {
    try { window.grecaptcha.reset(recaptchaWidgetId.value) } catch {}
  }
})

function tryRenderRecaptcha () {
  if (!window.grecaptcha || recaptchaLoaded.value) return
  const el = document.getElementById('recaptcha-container')
  if (!el) return
  try {
    recaptchaWidgetId.value = window.grecaptcha.render(el, {
      'sitekey': recaptchaSiteKey,
      'callback': (token) => {
        recaptchaToken.value = token
        // 사용자에게 입력 폼 보여주려면 여기에서 처리 (이미 보임)
      },
      'expired-callback': () => {
        recaptchaToken.value = null
      }
    })
    recaptchaLoaded.value = true
  } catch (e) {
    console.error('reCAPTCHA render error', e)
  }
}

async function onLogin() {
  msg.value = ''
  loading.value = true

  // reCAPTCHA 토큰 확인 (개발 환경에서는 토큰 체크를 느슨히 하고 싶다면 주석 가능)
  if (!recaptchaToken.value) {
    msg.value = '자동화 방지를 위해 reCAPTCHA를 완료해주세요.'
    loading.value = false
    return
  }

  try {
    // 기존 API 모듈 사용: login({loginId, password, recaptchaToken})
    const { token } = await apiLogin({
      loginId: loginId.value,
      password: password.value,
      recaptchaToken: recaptchaToken.value
    })

    if (rememberId.value) localStorage.setItem('remember_login_id', loginId.value)
    else localStorage.removeItem('remember_login_id')

    const role = getRoleFromPayload(parseJwt(token))
    localStorage.setItem('token', token)

    const target =
      role === 'ROLE_ADMIN' ? '/admin' :
      role === 'ROLE_OWNER' ? '/owner' : '/main'

    // 로그인 직후 reCAPTCHA 리셋(다음 로그인 시 재획득 필요)
    try { window.grecaptcha.reset(recaptchaWidgetId.value); recaptchaToken.value = null } catch {}

    await router.push(target)
  } catch (e) {
    // e는 API 에러에서 throw된 객체 (아래 api/auth 예시와 호환)
    const { error, attempts = 0, locked = false } = e || {}
    if (locked) {
      msg.value = '계정이 잠겼습니다. 1시간 후에 다시 시도해주세요.'
    } else if (error === 'INVALID_CREDENTIALS') {
      msg.value = `아이디 또는 비밀번호가 올바르지 않습니다. (틀린 횟수: ${attempts})`
    } else if (error === 'RECAPTCHA_FAILED') {
      msg.value = 'reCAPTCHA 검증에 실패했습니다. 새로고침 후 다시 시도하세요.'
    } else if (typeof error === 'string' && error.length) {
      msg.value = error
    } else {
      msg.value = '로그인 실패'
    }
    // reCAPTCHA 리셋
    try { window.grecaptcha.reset(recaptchaWidgetId.value); recaptchaToken.value = null } catch {}
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <section class="auth-card">
      <aside class="auth-visual">
        <div class="vis-box">
          <HeroSlideshow :images="heroImages" :interval="15000" default-pos="50% 60%" />
        </div>
        <div class="visual-copy">
          <div class="brand">
            <span class="logo-dot"></span>
            <h1>Hotel <span class="thin">Reserve</span></h1>
          </div>
          <h2 class="hero">편안한 여행의 시작,<br />감성적인 예약 경험을.</h2>
          <p class="hero-sub">간단한 로그인으로 여정을 이어가세요.</p>
        </div>
      </aside>

      <div class="auth-pane">
        <h2 class="title">로그인</h2>
        <p class="subtitle">계정 정보를 입력해주세요.</p>
        <form class="form" @submit.prevent="onLogin">
          <div class="field">
            <label class="sr-only" for="loginId">아이디</label>
            <input id="loginId" class="input" v-model.trim="loginId" placeholder="아이디" autocomplete="username" required autofocus />
          </div>

          <div class="field">
            <label class="sr-only" for="password">비밀번호</label>
            <div class="passwrap">
              <input :type="show ? 'text' : 'password'" id="password" class="input" v-model.trim="password" placeholder="비밀번호" autocomplete="current-password" required />
              <button type="button" class="eye" @click="show = !show" :aria-label="show ? '비밀번호 숨기기' : '비밀번호 보기'">
                <svg viewBox="0 0 24 24" class="eye-ico" aria-hidden="true">
                  <path d="M1.5 12s3.5-6.5 10.5-6.5S22.5 12 22.5 12s-3.5 6.5-10.5 6.5S1.5 12 1.5 12Z" fill="none" stroke="currentColor" stroke-width="1.6"/>
                  <circle cx="12" cy="12" r="2.7" fill="none" stroke="currentColor" stroke-width="1.6"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- reCAPTCHA 위젯 -->
          <div id="recaptcha-container" style="margin: 6px 0 12px;"></div>

          <div class="row between hint">
            <span>계정이 없으신가요?</span>
            <RouterLink class="link" to="/signup">회원가입</RouterLink>
          </div>
          <div class="row between hint">
            <RouterLink class="link" to="/find-password">비밀번호 찾기</RouterLink>
          </div>

          <label class="remember">
            <input type="checkbox" v-model="rememberId" />
            <span>아이디 기억하기</span>
          </label>

          <button class="btn primary" type="submit" :disabled="loading">
            <span v-if="!loading">로그인</span>
            <span v-else class="spinner" aria-label="진행중"></span>
          </button>
        </form>

        <div class="divider"><span>또는</span></div>

        <div class="social-icons">
          <a class="icon-btn google" href="http://localhost:8888/oauth2/authorization/google?prompt=select_account" aria-label="Google로 로그인">
            <img class="icon-img" src="https://developers.google.com/identity/images/g-logo.png" alt="" />
          </a>
          <a class="icon-btn naver" href="http://localhost:8888/oauth2/authorization/naver?auth_type=reprompt" aria-label="Naver로 로그인">
            <img class="icon-img invert" src="https://cdn.jsdelivr.net/gh/simple-icons/simple-icons/icons/naver.svg" alt="" />
          </a>
          <a class="icon-btn kakao" href="http://localhost:8888/oauth2/authorization/kakao?prompt=login" aria-label="Kakao로 로그인">
            <img class="icon-img" src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/KakaoTalk_logo.svg/960px-KakaoTalk_logo.svg.png" alt="" />
          </a>
        </div>

        <p class="text-sm center" v-if="msg">{{ msg }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ===================== */
/* Theme Tokens          */
/* ===================== */

:root{
  --bg-1:#f3f8ff; --bg-2:#eef5ff;
  --card:#ffffffee; --card-border:#d6e6ff;
  --text-1:#111827; --text-2:#4a5a75; --text-body:#1f2a37;
  --brand-1:#5daeff; --brand-2:#9fd3ff;
  --focus:rgba(93,174,255,.22);
}
.sr-only{ position:absolute !important; width:1px; height:1px; padding:0; margin:-1px; overflow:hidden; clip:rect(0,0,0,0); white-space:nowrap; border:0 }
.auth-shell{
  min-height:100vh; display:grid; place-items:center; padding:48px 16px;
  background:linear-gradient(180deg,var(--bg-1),var(--bg-2)); color:var(--text-body);
  font-family:ui-sans-serif,system-ui,Segoe UI,Roboto,Helvetica,Arial;
}
.auth-card{
  width:min(960px,94vw); display:grid; grid-template-columns:1.05fr .95fr;
  border-radius:18px; background:var(--card); border:1px solid var(--card-border);
  box-shadow:0 18px 60px rgba(16,44,84,.12); overflow:hidden; backdrop-filter:blur(10px);
}
@media (max-width:860px){ .auth-card{ grid-template-columns:1fr } .auth-visual{ display:none } }

/* ✅ 왼쪽: 슬라이드 영역 */
.auth-visual{
  position:relative;
  border-right:1px solid var(--card-border);
  min-height:460px;
}
/* 슬라이드가 영역을 꽉 채우도록 래퍼 */
.vis-box{
  position:absolute; inset:0; overflow:hidden;
}
/* 가독성 위한 그라데이션 오버레이 */
.vis-box::after{
  content:""; position:absolute; inset:0;
  background:linear-gradient(180deg, rgba(6,12,24,.45), rgba(6,12,24,.25) 40%, rgba(6,12,24,.35));
  pointer-events:none;
}

/* 텍스트 */
.visual-copy{
  position:absolute; left:32px; top:28px; right:32px; z-index:3; color:#eef6ff;
  text-shadow:0 1px 2px rgba(0,0,0,.25);
}
.brand{ display:flex; align-items:center; gap:12px; margin-bottom:12px }
.logo-dot{ width:14px; height:14px; border-radius:50%;
  background:linear-gradient(135deg,var(--brand-1),var(--brand-2));
  box-shadow:0 0 14px var(--brand-1);
}
.brand h1{ margin:0; font-weight:800; letter-spacing:.2px }
.brand .thin{ font-weight:300; opacity:.95 }
.hero{ margin:14px 0 8px; font-size:28px; line-height:1.34; letter-spacing:.2px; word-break:keep-all }
.hero-sub{ margin:0; color:#dce8ff; font-size:14px }

.auth-pane{ padding:36px 32px }
.title{ margin:0 0 6px; font-size:24px; font-weight:800; color:var(--text-1) }
.subtitle{ margin:0 0 20px; color:var(--text-2); font-size:13px }
.form{ display:grid; gap:14px; margin-top:8px }
.field{ display:block; width:100%; min-width:0 }
.input{
  width:100%; height:42px; padding:10px 12px; border-radius:12px;
  border:1px solid #cfe0ff; background:#fff; color:var(--text-body);
  outline:none; transition:border-color .18s ease, box-shadow .18s ease, background .18s ease;
  font-size:15px; box-sizing:border-box;
}
.input::placeholder{ color:#9aa8c3 }
.input:hover{ background:#fbfdff }
.input:focus{ border-color:var(--brand-1); box-shadow:0 0 0 3px var(--focus) }
.passwrap{ position:relative; width:100%; min-width:0 }
.passwrap .input{ padding-right:44px }
.eye{
  position:absolute; top:50%; right:8px; transform:translateY(-50%);
  width:28px; height:28px; display:grid; place-items:center;
  border:0; background:transparent; color:#6a7a99; cursor:pointer; border-radius:8px;
}
.eye:hover{ background:#f2f6ff; color:#0f2547 }
.eye-ico{ width:20px; height:20px; display:block }

.row{ display:flex; align-items:center; gap:10px }
.row.between{ justify-content:space-between }
.hint{ color:#4a5a75 }
.link{ color:#2f7bff; text-underline-offset:3px; white-space:nowrap }
.link:hover{ text-decoration:underline }
.btn{
  width:100%; height:46px; padding:0 16px; border-radius:999px;
  font-weight:800; font-size:15px; border:0; cursor:pointer;
  display:inline-flex; align-items:center; justify-content:center;
  transition:transform .06s ease, box-shadow .12s ease, background .18s ease;
}
.btn.primary{ background:linear-gradient(135deg,#3b82f6,#2563eb); color:#fff; box-shadow:0 8px 20px rgba(37,99,235,.28) }
.btn.primary:hover{ background:linear-gradient(135deg,#2563eb,#1d4ed8) }
.btn.primary:active{ transform:translateY(1px) }
.btn:disabled{ opacity:.65; cursor:not-allowed; transform:none; box-shadow:none }

.spinner{ display:inline-block; width:18px; height:18px; border:2px solid rgba(255,255,255,.6); border-top-color:#fff; border-radius:50%; animation:spin 1s linear infinite }
@keyframes spin{ to{ transform:rotate(360deg) } }

.divider{ display:flex; align-items:center; gap:10px; margin:16px 0; color:#4a5a75; font-size:12px }
.divider::before,.divider::after{ content:""; flex:1; height:1px; background:#e1ecff }

.social-icons{ display:flex; justify-content:center; gap:16px; margin-top:6px }
.icon-btn{ width:48px; height:48px; border-radius:50%; display:grid; place-items:center;
  background:#fff; border:1px solid #d7e7ff; box-shadow:0 6px 14px rgba(16,44,84,.08);
  transition:transform .06s ease, box-shadow .12s ease, background-color .18s ease;
}
.icon-btn:hover{ transform:translateY(-1px); box-shadow:0 10px 22px rgba(16,44,84,.12) }
.icon-img{ width:22px; height:22px; display:block }
.icon-img.invert{ filter: invert(1) }
.icon-btn.naver{ background:#03c75a; border-color:#03c75a }
.icon-btn.kakao{ background:#fee500; border-color:#e6cd00 }

.text-sm.center{ text-align:center; font-size:13px; color:#e11d48 }
*, *::before, *::after { box-sizing: border-box }

.remember{ display:flex; align-items:center; gap:8px; margin:6px 0 2px; color:#334155; font-size:14px; user-select:none }
.remember input{ width:16px; height:16px }
</style>
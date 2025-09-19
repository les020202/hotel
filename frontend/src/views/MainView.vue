<script setup>
import { ref, onMounted } from 'vue'
import { getMe, logout as apiLogout } from '@/api/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const me = ref(null)
const msg = ref('')

async function fetchMe () {
  msg.value = ''
  try {
    me.value = await getMe()
  } catch (e) {
    msg.value = `토큰 확인 실패: ${e?.response?.status || ''}`
  }
}

onMounted(async () => {

  // 1) 소셜 로그인 성공 시 /main#token=... 으로 오므로, 해시에서 토큰 추출
  const m = location.hash.match(/token=([^&]+)/)
  if (m) {
    const token = decodeURIComponent(m[1])
    localStorage.setItem('token', token)
    history.replaceState({}, '', location.pathname) // 주소창에서 해시 제거
  }

  // 2) 저장된 토큰이 전혀 없으면 로그인 페이지로

  if (!localStorage.getItem('token')) {
    router.push('/login')
    return
  }

  // 3) 내 정보 로딩
  await fetchMe()
})

async function checkToken () { await fetchMe() }


/* =============================
 * 로그아웃: 서버 쿠키 제거 → 로컬 정리 → 하드 리다이렉트
 * ============================= */
async function logout () {
  try {
    // (A) 우리 API 로그아웃 (refreshToken 쿠키 만료)
    await apiLogout().catch(() => {})

    // (B) Spring Security /logout 도 함께 호출 (JSESSIONID 제거)
    await fetch('http://localhost:8888/logout', {
      method: 'POST',
      credentials: 'include'
    }).catch(() => {})
  } finally {
    // (C) 클라이언트 정리
    localStorage.removeItem('token')
    me.value = null
    // 하드 리로드로 세션/리다이렉트 상태 깔끔하게 초기화
    window.location.replace('/login')
    
    // ✅ 마이페이지 이동 함수
    function goMyPage () {
      router.push('/mypage')
    }
  }
}

</script>

<template>
  <div class="page">
    <div class="card">
      <div class="head">
        <h1 class="title">메인 페이지</h1>
        <p class="greet" v-if="me">환영합니다, <b>{{ me.name }}</b>님</p>
      </div>

      <div class="row gap">
        <button class="btn primary" @click="checkToken">토큰 확인(/api/me)</button>
        <button class="btn" @click="goMyPage">마이페이지로 이동</button> <!-- ✅ 추가 -->
        <button class="btn" @click="logout">로그아웃</button>
      </div>

      <p class="hint" v-if="msg">{{ msg }}</p>
      <pre v-if="me" class="mt" style="white-space:pre-wrap">{{ JSON.stringify(me, null, 2) }}</pre>
    </div>
  </div>
</template>


<style scoped>
.page{min-height:100vh;display:flex;align-items:center;justify-content:center;background:#0b0b0c}
.card{width:100%;max-width:1440px;background:#111318;border:1px solid #24262b;border-radius:16px;padding:28px}
.head{display:flex;align-items:baseline;justify-content:space-between;margin-bottom:10px}
.title{font-size:22px;font-weight:800;color:#e5e7eb}
.greet{color:#cbd5e1}
.row{display:flex;align-items:center}.gap{gap:10px}
.btn{padding:.65rem 1rem;border:1px solid #30343a;border-radius:10px;background:#171a1f;color:#e5e7eb;cursor:pointer}
.btn.primary{background:#6b46c1;border-color:#6b46c1}
.hint{color:#cbd5e1}.mt{margin-top:12px}
</style>
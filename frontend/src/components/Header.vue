<template>
  <header :class="['header', solid && 'solid']" role="banner">
    <div class="container header-inner">
      <!-- 로고 -->
      <div class="logo">
        <router-link to="/" aria-label="홈으로 이동">류경호텔</router-link>
      </div>

      <!-- 가운데 타이틀 -->
      <div class="center-title">
        {{ centerTitle }}
      </div>

      <!-- 우측 액션 -->
      <div class="actions">
        <button class="btn ghost" @click="onHotelCreateClick" aria-label="호텔 등록">호텔 등록</button>

        <!-- 로그인 상태 -->
        <template v-if="user">
          <button class="btn ghost" @click="onWishlistClick" aria-label="위시리스트">
            ♡ <span class="hidden-sm">찜하기</span>
          </button>

          <div ref="dropdownRef" class="dropdown-wrap">
            <button class="btn ghost" @click="open = !open" :aria-expanded="open" aria-haspopup="menu">
              {{ user.name }}
            </button>

            <!-- ▼ 드롭다운: name 노출 + 로그아웃 가능 -->
            <div v-if="open" role="menu" class="dropdown card p-4">
              <div class="userbox">
                <div class="user-name">{{ user?.name }}</div>
                <div class="user-sub" v-if="user?.email">{{ user.email }}</div>
              </div>
              <hr class="sep" />

              <button class="btn ghost w100" @click="go('/mypage')">마이페이지</button>
              <button class="btn ghost w100" @click="go('/mypage/history')">예약내역</button>
              <button class="btn ghost w100" @click="go('/mypage/support')">고객지원</button>

              <hr class="sep" />
              <button class="btn primary w100" @click="doLogout">로그아웃</button>
            </div>
          </div>
        </template>

        <!-- 비로그인 상태 -->
        <template v-else>
          <router-link class="btn primary" to="/login">로그인</router-link>
          <router-link class="btn primary" to="/signup">회원가입</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUser, onAuthChanged, logout as apiLogout } from '@/api/auth'

const route = useRoute()
const router = useRouter()

// 로그인 사용자 상태
const user = ref(getUser())

// 드롭다운/헤더 상태
const open = ref(false)
const solid = ref(false)
const dropdownRef = ref(null)
const onScroll = () => { solid.value = window.scrollY > 4 }

// 로그인 상태 변화 수신
let stopAuthWatch = null

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll)

  stopAuthWatch = onAuthChanged((e) => {
    user.value = e.detail.user
    open.value = false
  })

  document.addEventListener('click', onDocClick, true)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  stopAuthWatch?.()
  document.removeEventListener('click', onDocClick, true)
})

function onDocClick(ev){
  if (!open.value) return
  const el = dropdownRef.value
  if (el && !el.contains(ev.target)) open.value = false
}

const centerTitle = computed(() => {
  const p = route.path
  if (p.startsWith('/login')) return '로그인'
  if (p.startsWith('/signup')) return '회원가입'
  if (p.startsWith('/find-password')) return '비밀번호 찾기'
  return ''
})

function onWishlistClick(){
  router.push('/wishlist')
}
function onHotelCreateClick(){
  router.push('/hotelapply')   // 호텔 등록(신청) 페이지로 이동
}
function go(path){ open.value = false; router.push(path) }

// ✅ 로그아웃
async function doLogout(){
  try {
    // 서버 로그아웃(리프레시 쿠키 만료)
    await apiLogout().catch(() => {})
  } finally {
    // 드롭다운 닫기 + 이동
    open.value = false
    router.push('/login')
  }
}

</script>

<style scoped>
.header {
  position: sticky;
  top: 0; left: 0; right: 0;
  backdrop-filter: blur(8px);
  background: rgba(255,255,255,.6);
  z-index: 100;
  transition: background-color .2s ease;
}
.header.solid { background: #fff; }

/* 헤더 내부 레이아웃 */
.header-inner{
  height: 64px;
  display:flex; align-items:center;
  justify-content: space-between;
}

/* 전체폭 배경 + 가운데 컨텐츠 */
.container{
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 16px;
  box-sizing: border-box;
}

/* 좌/중/우 컬럼 */
.logo{ flex:1; font-weight:700; font-size:20px; letter-spacing:.2px; }
.center-title{ flex:1; text-align:center; color:#4b5563; font-size:14px; }
.actions{ flex:1; display:flex; justify-content:flex-end; gap:8px; position:relative; }
.dropdown-wrap{ position:relative; }

/* 드롭다운 */
.dropdown{
  position:absolute; right:0; margin-top:6px; width:220px; z-index:50;
}
.userbox{ display:flex; flex-direction:column; gap:2px; padding-bottom:4px; }
.user-name{ font-weight:800; color:#0f172a; }
.user-sub{ font-size:12px; color:#64748b; }
.sep{ margin:8px 0; }

/* 버튼/카드 공통 */
.btn.ghost { border:1px solid #e5e7eb; background:#fff; padding:6px 10px; border-radius:8px; }
.btn.primary { background:#111; color:#fff; padding:6px 10px; border-radius:8px; }
.card { background:#fff; border:1px solid #eee; border-radius:12px; box-shadow:0 4px 16px rgba(0,0,0,.06); }
.p-4 { padding:12px; }
.w100{ width:100%; }
.hidden-sm { display:inline-block; }

@media (max-width:640px){
  .hidden-sm { display:none; }
}
</style>

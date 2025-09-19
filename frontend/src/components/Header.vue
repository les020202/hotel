<template>
  <header :class="['header', solid && 'solid']" role="banner">
    <div class="container" style="height: var(--header-h); display:flex; align-items:center;">

      <!-- 로고 -->
      <div style="flex:1">
        <router-link to="/" aria-label="홈으로 이동"
          style="font-weight:700; font-size:20px; letter-spacing:.2px;">
          류경호텔
        </router-link>
      </div>

      <!-- 가운데 타이틀 -->
      <div style="flex:1; text-align:center; color:#4b5563; font-size:14px;">
        {{ centerTitle }}
      </div>

      <!-- 우측 액션 -->
      <div style="flex:1; display:flex; justify-content:flex-end; gap:8px; position:relative;">
        <button class="btn ghost" @click="onHotelCreateClick" aria-label="호텔 등록">
          <span class="hidden-sm">호텔 등록</span>
        </button>

        <!-- ★ 변경: 찜하기 버튼을 로그인 영역(v-if="user") 안으로 이동 -->
        <!-- 로그인 상태 -->
        <template v-if="user">
          <!-- ★ 변경: 여기로 이동 -->
          <button class="btn ghost" @click="onWishlistClick" aria-label="위시리스트">
            ♡ <span class="hidden-sm">찜하기</span>
          </button>

          <div ref="dropdownRef" style="position:relative;">
            <button class="btn ghost" @click="open = !open" :aria-expanded="open" aria-haspopup="menu">
              {{ user.nickname || user.username }} ▼
            </button>
            <div v-if="open" role="menu"
                 style="position:absolute; right:0; margin-top:6px; width:200px; z-index:50"
                 class="card p-4">
              <button class="btn ghost" style="width:100%; margin-bottom:6px" @click="go('/account')">마이페이지</button>
              <button class="btn ghost" style="width:100%; margin-bottom:6px" @click="go('/payments')">예약내역</button>
              <button class="btn ghost" style="width:100%; margin-bottom:6px" @click="go('/settings')">설정</button>
              <button class="btn ghost" style="width:100%; margin-bottom:6px" @click="go('/support')">고객지원</button>
              <hr style="margin:8px 0;">
              <button class="btn primary" style="width:100%;" @click="doLogout">로그아웃</button>
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
import { getUser, logout, onAuthChanged } from '@/api/auth'

const route = useRoute()
const router = useRouter()

const user = ref(getUser())       // 초기의 로그인 상태
const open = ref(false)
const solid = ref(false)
const dropdownRef = ref(null)

const onScroll = () => { solid.value = window.scrollY > 4 }
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

let stopAuthWatch = null
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

/* ★ 변경: 버튼이 로그인 상태에서만 보이므로 로그인 체크 제거 */
function onWishlistClick(){
  // 추후 위시리스트 페이지로 이동하도록 바꿔도 됩니다.
  alert('위시리스트는 추후 화면에서 제공됩니다. (API는 준비됨)')
}

function onHotelCreateClick(){
  alert('호텔 등록은 추후 제공됩니다.')
}

function go(path){ open.value = false; router.push(path) }

async function doLogout(){
  await logout()
  open.value = false
  router.push('/')
}
</script>

<style scoped>
.header {
  position: sticky; top:0; left:0; right:0;
  backdrop-filter: blur(8px);
  background: rgba(255,255,255,.6);
  z-index: 100;
  transition: background-color .2s ease;
}
.header.solid { background: #fff; }
.btn.ghost { border:1px solid #e5e7eb; background:#fff; padding:6px 10px; border-radius:8px; }
.btn.primary { background:#111; color:#fff; padding:6px 10px; border-radius:8px; }
.card { background:#fff; border:1px solid #eee; border-radius:12px; box-shadow:0 4px 16px rgba(0,0,0,.06); }
.p-4 { padding:12px; }
.hidden-sm { display:inline-block; }
@media (max-width:640px){
  .hidden-sm { display:none; }
}
</style>

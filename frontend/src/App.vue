<template>
  <div>
    <!-- /admin, /owner가 아닌 경우에만 헤더/푸터 노출 -->
    <Header v-if="showChrome" />
    <main
      :aria-label="pageAria"
      :style="showChrome ? 'padding-top: 8px;' : ''"
    >
      <router-view />
    </main>
    <Footer v-if="showChrome" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'

const route = useRoute()

// 부모/자식 라우트를 모두 포함한 matched에서 /admin 또는 /owner 여부를 판별
const isAdminOrOwner = computed(() =>
  route.matched.some(m =>
    m.path === '/admin' ||
    m.path.startsWith('/admin/') ||
    m.path === '/owner' ||
    m.path.startsWith('/owner/')
  )
)

const showChrome = computed(() => !isAdminOrOwner.value)

const pageAria = computed(() => {
  const p = route.path
  if (p === '/' || p.startsWith('/main')) return '홈'
  if (p.startsWith('/login')) return '로그인'
  if (p.startsWith('/signup')) return '회원가입'
  if (p.startsWith('/find-password')) return '비밀번호 찾기'
  return '콘텐츠'
})
</script>

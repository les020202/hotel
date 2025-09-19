<template>
  <div>
    <Header />
    <main :aria-label="pageAria" style="padding-top: 8px;">
      <router-view />
    </main>
    <Footer />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'

const route = useRoute()
const pageAria = computed(() => {
  const p = route.path
  if (p === '/') return '홈'
  if (p.startsWith('/login')) return '로그인'
  if (p.startsWith('/signup')) return '회원가입'
  if (p.startsWith('/find-password')) return '비밀번호 찾기'
  return '콘텐츠'
})
</script>

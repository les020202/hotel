<template>
    <section class="min-h-[60vh] flex items-center justify-center p-6">
      <div class="max-w-xl w-full space-y-4 text-center">
        <h1 class="text-3xl font-bold">{{ title }}</h1>
        <p class="text-gray-600">{{ message }}</p>
        <p v-if="errorId" class="text-xs text-gray-400">오류 ID: {{ errorId }}</p>
        <div class="flex gap-3 justify-center pt-2">
          <button class="px-4 py-2 rounded border" @click="$router.push('/')">홈으로</button>
        </div>
      </div>
    </section>
  </template>
  
  <script setup>
  import { computed } from 'vue'
  import { useRoute } from 'vue-router'
  
  const route = useRoute()
  // status는 라우터 props 또는 쿼리로 전달 (우선순위: props → query → 500)
  const status = computed(() => Number(route.params.status ?? route.query.status ?? 500))
  const errorId = computed(() => route.query.errid ?? route.params.errid ?? '')
  
  const MAP = {
    400: { title: '잘못된 요청입니다', message: '입력값을 확인하고 다시 시도해주세요.' },
    401: { title: '로그인이 필요합니다', message: '세션이 만료되었거나 권한이 없습니다.' },
    403: { title: '접근이 거부되었습니다', message: '요청하신 리소스에 권한이 없습니다.' },
    404: { title: '페이지를 찾을 수 없습니다', message: '주소를 다시 확인해주세요.' },
    405: { title: '허용되지 않은 요청입니다', message: '요청 방법을 다시 확인해주세요.' },
    500: { title: '서버 오류가 발생했습니다', message: '잠시 후 다시 시도해주세요.' },
    502: { title: '게이트웨이 오류', message: '외부 서비스 응답에 문제가 있습니다.' },
    504: { title: '게이트웨이 타임아웃', message: '잠시 후 다시 시도해주세요.' }
  }
  
  const title = computed(() => (MAP[status.value]?.title ?? MAP[500].title))
  const message = computed(() => (MAP[status.value]?.message ?? MAP[500].message))
  
  function reload () { location.reload() }
  </script>
  
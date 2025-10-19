<!-- src/components/common/SafeText.vue -->
<script setup>
import { computed } from 'vue'
import { sanitizeText, tokenizeWithLinks } from '@/utils/sanitize'

const props = defineProps({
  text: { type: String, default: '' },
  autolink: { type: Boolean, default: true }
})

// 1) 서버가 실수로 HTML을 보내도 전부 제거
const clean = computed(() => sanitizeText(props.text ?? ''))

// 2) URL만 토큰화해 <a>로 직접 렌더 (v-html 금지)
const tokens = computed(() => props.autolink ? tokenizeWithLinks(clean.value) : [{ type:'text', text: clean.value }])
</script>

<template>
  <span class="whitespace-pre-wrap break-words">
    <template v-for="(t,i) in tokens" :key="i">
      <template v-if="t.type === 'text'">{{ t.text }}</template>
      <template v-else>
        <a
          class="text-blue-600 underline underline-offset-2"
          :href="t.href"
          target="_blank"
          rel="noopener noreferrer"
        >{{ t.text }}</a>
      </template>
    </template>
  </span>
</template>

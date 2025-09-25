<!-- src/components/review/ReportDialog.vue -->
<template>
  <div v-if="open" class="fixed inset-0 z-[1100] bg-black/40 grid place-items-center p-4">
    <div class="w-full max-w-md rounded-2xl bg-white shadow-xl p-4">
      <header class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold">리뷰 신고</h3>
        <button class="text-gray-500 hover:text-black" @click="$emit('close')">✕</button>
      </header>

      <div class="space-y-3">
        <label class="block text-sm text-gray-600">사유</label>
        <select v-model="code" class="w-full rounded border px-2 py-2">
          <option disabled value="">선택하세요</option>
          <option value="ABUSE">욕설/비하</option>
          <option value="SPAM">광고/도배</option>
          <option value="PERSONAL">개인정보 노출</option>
          <option value="OTHER">기타</option>
        </select>

        <div v-if="code==='OTHER'" class="space-y-1">
          <label class="block text-sm text-gray-600">기타 사유</label>
          <textarea v-model.trim="detail" rows="4" class="w-full rounded border px-2 py-2"
                    placeholder="자세한 내용을 적어주세요."></textarea>
        </div>
      </div>

      <div class="mt-4 flex justify-end gap-2">
        <button class="px-3 py-2 rounded border" @click="$emit('close')">취소</button>
        <button class="px-3 py-2 rounded bg-black text-white"
                :disabled="!code || (code==='OTHER' && !detail)"
                @click="submit">
          신고하기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'submit'])

const code = ref('')
const detail = ref('')

watch(() => props.open, v => { if (v) { code.value=''; detail.value='' } })

function submit() {
  // 백엔드가 {reason:string}만 받는 현 구조에 맞춰 문자열로 합침
  const reason =
    code.value === 'ABUSE'    ? '[ABUSE] 욕설/비하' :
    code.value === 'SPAM'     ? '[SPAM] 광고/도배' :
    code.value === 'PERSONAL' ? '[PERSONAL] 개인정보 노출' :
    `[OTHER] ${detail.value}`;

  emit('submit', { code: code.value, detail: detail.value, reason })
  emit('close')
}
</script>

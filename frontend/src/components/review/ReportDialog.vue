<!-- src/components/review/ReportDialog.vue -->
<template>
  <div v-if="open" class="fixed inset-0 z-[1100] bg-black/40 grid place-items-center p-4">
    <div class="w-full max-w-md rounded-2xl bg-white shadow-xl p-4" @click.stop>
      <header class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold">리뷰 신고</h3>
        <button class="text-gray-500 hover:text-black" @click="$emit('close')" aria-label="닫기">✕</button>
      </header>

      <!-- form으로 감싸고 기본 submit 막기 -->
      <form @submit.prevent="onSubmit" class="space-y-3">
        <div>
          <label class="block text-sm text-gray-600">사유</label>
          <select v-model="code" class="w-full rounded border px-2 py-2">
            <option disabled value="">선택하세요</option>
            <option value="ABUSE">욕설/비하</option>
            <option value="SPAM">광고/도배</option>
            <option value="PERSONAL">개인정보 노출</option>
            <option value="OTHER">기타</option>
          </select>
        </div>

        <div v-if="code==='OTHER'" class="space-y-1">
          <label class="block text-sm text-gray-600">기타 사유</label>
          <textarea
            v-model.trim="detail"
            rows="4"
            class="w-full rounded border px-2 py-2"
            placeholder="자세한 내용을 적어주세요."
          />
        </div>

        <div class="mt-4 flex justify-end gap-2">
          <button type="button" class="px-3 py-2 rounded border" @click="$emit('close')">취소</button>
          <button
            type="submit"
            class="px-3 py-2 rounded bg-black text-white"
            :disabled="submitting || !canSubmit"
          >
            {{ submitting ? '신고 중…' : '신고하기' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false } // 부모에서 전송중 상태 내려줄 수 있게(옵션)
})
const emit = defineEmits(['close', 'submit'])

const code = ref('')
const detail = ref('')

watch(() => props.open, v => {
  if (v) { code.value=''; detail.value='' }
})

const canSubmit = computed(() =>
  !!code.value && (code.value !== 'OTHER' || !!detail.value)
)

function onSubmit() {
  // payload 키를 reasonCode로 명확히 전달 (reportReview에서 정상 처리됨)
  emit('submit', {
    reasonCode: code.value,
    detail: code.value === 'OTHER' ? (detail.value || '') : ''
  })
  // 성공/실패 처리에 따라 닫는 타이밍은 부모가 결정하는게 안전하지만,
  // 기존 동작 유지가 필요하면 아래 한 줄 유지/제거를 선택하면 됨.
  emit('close')
}

// ESC 닫기
function onKey(e) {
  if (!props.open) return
  if (e.key === 'Escape') emit('close')
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

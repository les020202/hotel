<!-- src/components/common/CancelDialog.vue -->
<template>
  <div v-if="open" class="fixed inset-0 z-[1200] bg-black/40 grid place-items-center p-4">
    <div class="w-full max-w-md rounded-2xl bg-white shadow-xl overflow-hidden">
      <header class="px-5 py-4 border-b font-semibold">예약 취소</header>

      <div class="p-5 space-y-4">
        <div>
          <label class="block text-sm text-gray-600 mb-1">취소 사유</label>
          <select v-model="sel" class="w-full border rounded-lg px-3 py-2">
            <option disabled value="">사유를 선택하세요</option>
            <option value="CHANGE_OF_PLAN">일정 변경</option>
            <option value="PRICE_ISSUE">가격/결제 이슈</option>
            <option value="HOTEL_ISSUE">호텔 측 문제</option>
            <option value="ETC">기타 (직접 입력)</option>
          </select>
        </div>

        <div v-if="sel==='ETC'">
          <label class="block text-sm text-gray-600 mb-1">기타 사유</label>
          <textarea v-model="etc" rows="3" class="w-full border rounded-lg px-3 py-2" placeholder="사유를 입력하세요"></textarea>
        </div>

        <p class="text-xs text-gray-500">취소 후 예약은 목록에 ‘취소됨’으로 표시됩니다.</p>
      </div>

      <footer class="px-5 py-3 border-t flex justify-end gap-2">
        <button class="px-3 py-2 rounded border" @click="$emit('close')">닫기</button>
        <button
          class="px-3 py-2 rounded bg-black text-white disabled:opacity-50"
          :disabled="!sel || (sel==='ETC' && !etc.trim())"
          @click="submit"
        >확인</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'submit'])

const sel = ref('')
const etc = ref('')

watch(() => props.open, v => { if (v){ sel.value=''; etc.value='' } })

function submit(){
  const reason = sel.value === 'ETC' ? etc.value.trim() : sel.value
  emit('submit', { reason })
  emit('close')
}
</script>

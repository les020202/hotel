<!-- src/components/review/ReviewFormModal.vue -->
<template>
  <div v-if="open" class="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4">
    <div class="w-full max-w-md rounded-2xl bg-white p-4 shadow-xl">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold">리뷰 작성</h3>
        <button class="text-gray-500 hover:text-black" @click="$emit('close')">✕</button>
      </div>

      <!-- 작성 불가 안내 -->
      <div
        v-if="!validBookingId"
        class="mb-3 rounded-md bg-amber-50 border border-amber-200 text-amber-800 px-3 py-2 text-sm"
      >
        리뷰 작성 가능한 예약을 확인하지 못했어요. 잠시 후 다시 시도해주세요.
      </div>

      <form @submit.prevent="submit">
        <!-- 별점 -->
        <div class="mb-4">
          <label class="block text-sm text-gray-600 mb-1">평점</label>
          <div class="flex gap-1 text-2xl">
            <button
              v-for="i in 5" :key="i" type="button" :aria-label="`${i}점`"
              @click="rating = i"
            >
              <span :class="i <= rating ? 'text-yellow-500' : 'text-gray-300'">★</span>
            </button>
          </div>
          <p class="text-xs text-gray-500 mt-1">1~5점 중 선택</p>
        </div>

        <!-- 코멘트 -->
        <div class="mb-4">
          <label class="block text-sm text-gray-600 mb-1">코멘트 (선택)</label>
          <textarea
            v-model.trim="comment" rows="4"
            class="w-full resize-none rounded border px-2 py-1"
            placeholder="숙소 이용 경험을 적어주세요."
          />
        </div>

        <!-- 사진 1장 -->
        <div class="mb-4">
          <label class="block text-sm text-gray-600 mb-1">사진 (최대 1장)</label>
          <input ref="fileEl" type="file" accept="image/*" @change="onPick" />
          <p v-if="fileName" class="text-xs text-gray-500 mt-1">선택: {{ fileName }}</p>
        </div>

        <div class="flex justify-end gap-2">
          <button type="button" class="px-3 py-2 rounded border" @click="$emit('close')">취소</button>
          <button
            type="submit"
            class="px-3 py-2 rounded text-white"
            :class="canSubmit ? 'bg-black' : 'bg-gray-400 cursor-not-allowed'"
            :disabled="!canSubmit"
            :title="submitHint"
          >
            {{ submitting ? '저장 중…' : '등록' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { createReview } from '@/api/reviews'

const props = defineProps({
  open:     { type: Boolean, default: false },
  hotelId:  { type: Number,  required: true },
  // ✅ 선택적: eligibility 로부터 전달되지만 초기엔 undefined일 수 있음
  bookingId:{ type: [Number, String, null], default: null },
})
const emit = defineEmits(['close', 'submitted'])

const rating     = ref(0)
const comment    = ref('')
const file       = ref(null)
const fileName   = ref('')
const submitting = ref(false)
const fileEl     = ref(null)

// bookingId 유효성 (숫자로 해석 가능해야 함)
const normalizedBookingId = computed(() => {
  const n = Number(props.bookingId)
  return Number.isFinite(n) ? n : null
})
const validBookingId = computed(() => normalizedBookingId.value !== null)

// 버튼 활성/비활성
const canSubmit  = computed(() => rating.value >= 1 && !submitting.value && validBookingId.value)
const submitHint = computed(() => {
  if (!validBookingId.value) return '작성 가능한 예약이 없습니다.'
  if (rating.value < 1)      return '평점을 선택해주세요.'
  return ''
})

watch(() => props.open, (v) => { if (v) reset() })

function onPick(e) {
  const f = e.target.files?.[0] || null
  file.value = f
  fileName.value = f?.name || ''
  // 같은 파일을 다시 선택해도 change가 동작하도록 초기화
  if (fileEl.value) fileEl.value.value = ''
}

function reset() {
  rating.value   = 0
  comment.value  = ''
  file.value     = null
  fileName.value = ''
  submitting.value = false
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await createReview(props.hotelId, {
      bookingId: normalizedBookingId.value,   // ✅ 숫자 보장
      rating:    rating.value,
      comment:   comment.value || null,
      photo:     file.value || null,
    })
    emit('submitted')
    emit('close')
  } catch (e) {
    console.error(e)
    alert(e?.message || '리뷰 등록에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}
</script>

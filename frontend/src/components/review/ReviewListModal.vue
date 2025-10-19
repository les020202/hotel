<!-- src/components/review/ReviewListModal.vue -->
<template>
  <div v-if="open" class="fixed inset-0 z-[1000] bg-black/40 grid place-items-center p-4">
    <div class="w-full max-w-4xl max-h-[90vh] rounded-2xl bg-white shadow-xl overflow-hidden flex flex-col">
      <!-- 헤더 -->
      <header class="flex items-center justify-between px-5 py-3 border-b">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-gray-100 grid place-items-center">💬</div>
          <div>
            <div class="font-semibold">리뷰 전체</div>
            <div class="text-xs text-gray-500">스크롤하면 자동으로 더 불러와요</div>
          </div>
        </div>
        <button class="text-gray-500 hover:text-black" @click="$emit('close')">✕</button>
      </header>

      <!-- 리스트(스크롤 영역) -->
      <div ref="scrollEl" class="flex-1 overflow-auto px-5 py-4 space-y-4">
        <article v-for="r in items" :key="r.id" class="border rounded-xl p-4">
          <div class="flex items-start gap-3">
            <!-- 아바타 -->
            <img
              v-if="r.profileImageType === 'UPLOADED' && r.profileImageUrl"
              :src="r.profileImageUrl"
              class="w-10 h-10 rounded-full object-cover"
            />
            <div v-else class="w-10 h-10 rounded-full bg-gray-200 grid place-items-center text-sm">👤</div>

            <div class="min-w-0 flex-1">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2 min-w-0">
                  <span class="font-semibold truncate">{{ r.reviewerName || '익명' }}</span>

                  <!-- 내 리뷰 뱃지 -->
                  <span
                    v-if="isMine(r)"
                    class="text-[11px] px-2 py-0.5 rounded-full bg-blue-50 border border-blue-200 text-blue-700"
                  >내 리뷰</span>

                  <span
                    v-if="r.roomTypeName"
                    class="text-xs px-2 py-0.5 rounded-full bg-gray-100 border"
                  >{{ r.roomTypeName }}</span>

                  <div class="text-yellow-500 ml-2">
                    <span v-for="i in 5" :key="i">{{ i <= (r.rating || 0) ? '★' : '☆' }}</span>
                  </div>
                </div>
                <div class="text-xs text-gray-500">{{ dt(r.createdAt) }}</div>
              </div>

              <div class="mt-3 grid grid-cols-[120px,1fr] gap-3">
                <!-- 사진이 있을 때만 썸네일 -->
                <div v-if="r.photos?.length" class="w-30 h-30 rounded overflow-hidden bg-gray-100">
                  <img
                    :src="r.photos[0]"
                    class="w-full h-full object-cover cursor-zoom-in"
                    @click="openImage(r.photos[0])"
                  />
                </div>

                <!-- 본문: v-html 없이 안전 -->
                <div class="whitespace-pre-wrap">{{ r.comment || '' }}</div>
              </div>

              <!-- 액션 -->
              <div class="mt-2 flex justify-end gap-3 text-sm">
                <button
                  v-if="!isMine(r)"
                  class="text-gray-500 underline"
                  @click="openReport(r.id)"
                >
                  신고하기
                </button>
                <button
                  v-if="isMine(r)"
                  class="text-red-600 underline"
                  @click="onDelete(r.id)"
                >
                  삭제
                </button>
              </div>
            </div>
          </div>
        </article>

        <!-- 무한스크롤 센티널 -->
        <div ref="sentinel" class="h-6"></div>

        <div v-if="loading" class="text-center text-gray-400 py-4">불러오는 중…</div>
        <div v-if="ended && !loading" class="text-center text-gray-400 py-4">모두 읽었습니다.</div>
      </div>
    </div>

    <!-- 이미지 라이트박스 -->
    <div
      v-if="imgOpen"
      class="fixed inset-0 z-[1100] bg-black/80 grid place-items-center p-4"
      @click="imgOpen = false"
    >
      <img :src="imgSrc" class="max-w-full max-h-full object-contain" />
    </div>

    <!-- 신고 모달 -->
    <ReportDialog
      :open="reportOpen"
      @close="reportOpen = false"
      @submit="submitReport"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { fetchHotelReviews, deleteReview, reportReview } from '@/api/reviews'
import ReportDialog from './ReportDialog.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  hotelId: { type: Number, required: true },
  meId: { type: Number, default: null },
})
const emit = defineEmits(['close', 'changed'])

const items = ref([])
const page = ref(0)
const size = ref(10)
const totalPages = ref(0)
const loading = ref(false)
const ended = ref(false)

const scrollEl = ref(null)
const sentinel = ref(null)
let observer

function dt(s) {
  return s ? String(s).replace('T', ' ').slice(0, 16) : ''
}
function isMine(r) {
  return (props.meId ?? null) === r.userId
}

async function load() {
  if (loading.value || ended.value) return
  loading.value = true
  try {
    const res = await fetchHotelReviews(props.hotelId, page.value, size.value)
    items.value.push(...(res?.content || []))
    totalPages.value = res?.totalPages ?? 0
    page.value++
    if (page.value >= totalPages.value) ended.value = true
  } finally {
    loading.value = false
  }
}

function setupObserver() {
  if (observer) observer.disconnect()
  observer = new IntersectionObserver(
    ([e]) => { if (e.isIntersecting) load() },
    { root: scrollEl.value, threshold: 0.1 }
  )
  if (sentinel.value) observer.observe(sentinel.value)
}

function resetAndLoad() {
  items.value = []
  page.value = 0
  totalPages.value = 0
  ended.value = false
  try { scrollEl.value?.scrollTo({ top: 0 }) } catch {}
  setTimeout(() => { setupObserver(); load() }, 0)
}

watch(() => props.open, (v) => {
  if (v) resetAndLoad()
  else if (observer) observer.disconnect()
})

onMounted(() => { if (props.open) { setupObserver(); load() } })
onBeforeUnmount(() => { if (observer) observer.disconnect() })

// 라이트박스
const imgOpen = ref(false)
const imgSrc = ref('')
function openImage(src) { imgSrc.value = src; imgOpen.value = true }

// 삭제
async function onDelete(id) {
  if (!confirm('이 리뷰를 삭제할까요?')) return
  try {
    await deleteReview(id)
    emit('changed')
    resetAndLoad()
  } catch (err) {
    let msg = '삭제에 실패했습니다.'
    try { const j = JSON.parse(err?.message || '{}'); if (j?.error) msg = j.error } catch {}
    alert(msg)
    console.error('deleteReview error:', err)
  }
}

/* 신고 */
const reportOpen = ref(false)
const reportTargetId = ref(null)
function openReport(id) {
  reportTargetId.value = id
  reportOpen.value = true
}
async function submitReport({ reason, reasonCode, detail }) {
  if (!reportTargetId.value) return
  try {
    const payload = reasonCode ? { reasonCode, detail: detail || '' } : { reason: reason || '' }
    await reportReview(reportTargetId.value, payload)
    alert('신고가 접수되었습니다.')
  } catch (err) {
    let msg = '신고에 실패했습니다.'
    try { const j = JSON.parse(err?.message || '{}'); if (j?.error) msg = j.error } catch {}
    alert(msg)
    console.error('reportReview error:', err)
  } finally {
    reportTargetId.value = null
  }
}
</script>

<!-- src/components/review/ReviewSection.vue -->
<template>
  <section class="mt-8">
    <!-- 헤더 -->
    <header class="mb-4 flex items-end justify-between">
      <div>
        <h3 class="text-lg font-semibold">리뷰</h3>
        <p v-if="rating" class="text-sm text-gray-600">
          평균 ★ {{ rating.avg?.toFixed(1) }} ({{ rating.count }}명)
        </p>
      </div>

      <div class="flex items-center gap-2">
        <button
          v-if="eligibility?.eligible"
          class="px-3 py-2 rounded bg-black text-white"
          @click="openForm = true"
        >
          리뷰 작성
        </button>
      </div>
    </header>

    <!-- 요약 목록(최대 3개) -->
    <div class="space-y-6">
      <article
        v-for="r in top3"
        :key="r.id"
        class="rounded-2xl border shadow-sm p-4"
      >
        <!-- 상단: 아바타/이름/룸타입/별점/날짜 -->
        <div class="flex items-start justify-between">
          <div class="flex items-center gap-3 min-w-0">
            <!-- 아바타 -->
            <div class="shrink-0">
              <img
                v-if="profileSrc(r)"
                :src="profileSrc(r)"
                alt="profile"
                class="w-12 h-12 rounded-full object-cover border"
              />
              <svg v-else viewBox="0 0 80 80" class="w-12 h-12 rounded-full border text-gray-400">
                <circle cx="40" cy="40" r="39" fill="#f3f4f6" />
                <circle cx="40" cy="30" r="14" fill="#e5e7eb" />
                <rect x="18" y="46" width="44" height="22" rx="11" fill="#e5e7eb" />
              </svg>
            </div>

            <div class="min-w-0">
              <div class="flex items-center gap-2">
                <span class="font-semibold truncate max-w-[12rem]">
                  {{ r.reviewerName || '익명' }}
                </span>

                <!-- 내 리뷰 뱃지 -->
                <span
                  v-if="isMine(r)"
                  class="shrink-0 text-[11px] px-2 py-0.5 rounded-full bg-blue-50 border border-blue-200 text-blue-700"
                >내 리뷰</span>

                <span
                  v-if="r.roomTypeName"
                  class="shrink-0 text-[11px] px-2 py-0.5 rounded-full bg-gray-100 border text-gray-700"
                >
                  {{ r.roomTypeName }}
                </span>
              </div>

              <div class="mt-1 flex items-center gap-1 text-amber-500">
                <span v-for="i in 5" :key="i">{{ i <= (r.rating || 0) ? '★' : '☆' }}</span>
              </div>
            </div>
          </div>

          <small class="text-gray-500 shrink-0">{{ dt(r.createdAt) }}</small>
        </div>

        <!-- 본문: 사진이 있을 때만 2열 -->
        <div class="mt-3" :class="firstPhoto(r) ? 'grid grid-cols-[120px,1fr] gap-4 items-start' : ''">
          <!-- 썸네일(있을 때만) -->
          <button
            v-if="firstPhoto(r)"
            class="block w-[120px] h-[120px] rounded-xl overflow-hidden border bg-neutral-50"
            @click="openImage(firstPhoto(r))"
            title="이미지 크게 보기"
          >
            <img :src="firstPhoto(r)" alt="review-photo" class="w-full h-full object-cover" loading="lazy" />
          </button>

          <!-- 텍스트 + 액션 -->
          <div class="min-w-0">
            <p class="whitespace-pre-wrap leading-7 text-[15px]">
              {{ r.comment || '' }}
            </p>

            <div class="mt-2 flex justify-end gap-3">
              <!-- 남의 리뷰만 신고 -->
              <button
                v-if="!isMine(r)"
                class="text-sm text-gray-500 underline"
                @click="openReport(r.id)"
              >
                신고하기
              </button>
              <!-- 내 리뷰만 삭제 -->
              <button
                v-if="isMine(r)"
                class="text-sm text-red-600 underline"
                @click="onDelete(r.id)"
              >
                삭제
              </button>
            </div>
          </div>
        </div>
      </article>

      <p v-if="!loading && top3.length===0" class="text-gray-500">아직 리뷰가 없습니다.</p>
      <p v-if="loading" class="text-gray-400">불러오는 중…</p>
    </div>

    <!-- 목록 아래 '리뷰 더보기' -->
    <div v-if="hasMore" class="mt-6">
      <button
        class="w-full py-2 text-sm rounded-lg border hover:bg-gray-50 active:bg-gray-100 transition"
        @click="openAll = true"
      >
        리뷰 더보기
      </button>
    </div>

    <!-- 작성 모달 -->
    <ReviewFormModal
      :open="openForm"
      :hotel-id="hotelId"
      :booking-id="eligibility?.bookingId"
      @close="openForm=false"
      @submitted="reloadAll"
    />

    <!-- 전체 리뷰 모달(무한스크롤) -->
    <ReviewListModal
      :open="openAll"
      :hotel-id="hotelId"
      :me-id="me?.id || null"
      @close="openAll=false"
      @changed="reloadAll"
    />

    <!-- 이미지 라이트박스 -->
    <div
      v-if="lightboxUrl"
      class="fixed inset-0 z-[100] bg-black/70 grid place-items-center p-4"
      @click="lightboxUrl = ''"
    >
      <img :src="lightboxUrl" alt="photo" class="max-h-[85vh] max-w-[92vw] rounded-2xl shadow-2xl object-contain" />
    </div>

    <!-- 신고 모달(드롭다운 + 기타입력) -->
    <ReportDialog
      :open="reportOpen"
      @close="reportOpen = false"
      @submit="submitReport"
    />
  </section>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
const emit = defineEmits(['rating-updated'])
import {
  fetchHotelReviews,
  fetchHotelRating,
  checkReviewEligibility,
  reportReview,
  deleteReview,
} from '@/api/reviews'
import { getMe } from '@/api/auth'
import ReviewFormModal from './ReviewFormModal.vue'
import ReviewListModal from './ReviewListModal.vue'
import ReportDialog from './ReportDialog.vue'   // ★ 신고 드롭다운 모달

const props = defineProps({
  hotelId: { type: Number, required: true }
})

const me = ref(null)
const rows = ref([])
const rating = ref(null)
const eligibility = ref(null)
const loading = ref(false)
const openForm = ref(false)
const openAll  = ref(false)

const lightboxUrl = ref('')

function isMine(r) {
  return (me.value?.id || null) === r.userId
}

const top3 = computed(() => rows.value.slice(0, 3))
const hasMore = computed(() => (rating.value?.count ?? 0) > 3)

function dt(s) {
  if (!s) return ''
  return String(s).slice(0,10).replace(/-/g,'.')
}
function firstPhoto(r) {
  return Array.isArray(r.photos) && r.photos.length ? r.photos[0] : r.photoUrl || null
}
function profileSrc(r) {
  const t = (r.profileImageType || '').toUpperCase()
  if (t === 'UPLOADED' && r.profileImageUrl) return r.profileImageUrl
  if (t === 'TEMPLATE') {
    const code = (r.profileImageTemplate || 'T1').toString().toLowerCase()
    return `/assets/profile-${code}.svg`
  }
  return null
}

async function loadTop3() {
  loading.value = true
  try {
    const res = await fetchHotelReviews(props.hotelId, 0, 3)
    rows.value = res?.content || []
  } finally {
    loading.value = false
  }
}
async function reloadAll() {
  await Promise.all([loadTop3(), getRating(), getEligibility()])
  const rounded = Math.round(((rating.value?.avg ?? 0) * 10)) / 10
  emit('rating-updated', rounded)
}

async function getRating() {
  try { rating.value = await fetchHotelRating(props.hotelId) }
  catch { rating.value = null }
}
async function getEligibility() {
  try { eligibility.value = await checkReviewEligibility(props.hotelId) }
  catch { eligibility.value = { eligible: false } }
}

// 삭제
async function onDelete(id) {
  if (!confirm('이 리뷰를 삭제할까요?')) return
  try {
    await deleteReview(id)
    await reloadAll()
  } catch (err) {
    let msg = '삭제에 실패했습니다.'
    try { const j = JSON.parse(err.message || '{}'); if (j?.error) msg = j.error } catch {}
    alert(msg)
    console.error('deleteReview error:', err)
  }
}

// 신고: 드롭다운 모달 사용
const reportOpen = ref(false)
const reportTargetId = ref(null)
function openReport(id) {
  reportTargetId.value = id
  reportOpen.value = true
}
async function submitReport({ reason }) {
  if (!reportTargetId.value) return
  try {
    await reportReview(reportTargetId.value, reason)
    alert('신고가 접수되었습니다.')
    reportOpen.value = false   // 안전 닫기 (중복 닫힘 무해)
  } catch (err) {
    let msg = '신고에 실패했습니다.'
    try { const j = JSON.parse(err?.message || '{}'); if (j?.error) msg = j.error } catch {}
    alert(msg)
    console.error('reportReview error:', err)
  } finally {
    reportTargetId.value = null
  }
}

function openImage(url) { lightboxUrl.value = url }

onMounted(async () => {
  try { me.value = await getMe() } catch {}
  await reloadAll()
})
</script>

<style scoped>
/* 필요 시 효과 추가 */
</style>

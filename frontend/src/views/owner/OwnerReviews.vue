<template>
  <div class="space-y-4">
    <header class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">리뷰 조회</h2>
      <div class="text-xs text-gray-500">
        호텔 ID: <strong>{{ hotelId }}</strong>
      </div>
    </header>

    <!-- 필터 -->
    <section class="p-3 bg-white rounded-lg border space-x-2 flex flex-wrap items-center">
      <label class="flex items-center space-x-2">
        <span class="text-sm text-gray-600">검색</span>
        <input v-model.trim="q" type="text" placeholder="내용 검색"
               class="border rounded px-2 py-1 w-64">
      </label>
      <label class="flex items-center space-x-2">
        <span class="text-sm text-gray-600">공개</span>
        <select v-model="visible" class="border rounded px-2 py-1">
          <option :value="null">전체</option>
          <option :value="true">공개</option>
          <option :value="false">숨김</option>
        </select>
      </label>
      <label class="flex items-center space-x-2">
        <input type="checkbox" v-model="reportedOnly">
        <span class="text-sm text-gray-600">신고만</span>
      </label>
      <button class="px-3 py-1.5 rounded bg-black text-white" @click="load()">적용</button>
    </section>

    <!-- 표 -->
    <section class="bg-white rounded-lg border overflow-hidden">
      <div class="grid items-center gap-3 font-semibold text-gray-600 px-4 py-3 table-head">
        <span>#</span>
        <span>작성자</span>
        <span>내용</span>
        <span class="text-center">공개</span>
        <span>작성일</span>
        <span class="text-center">액션</span>
      </div>

      <div v-if="loading" class="p-4 text-gray-400">불러오는 중…</div>

      <template v-else>
        <div v-for="r in rows" :key="r.id" class="grid items-center gap-3 px-4 py-3 border-t table-row">
          <span class="text-gray-500">#{{ r.id }}</span>
          <span class="truncate" :title="r.userName || '-'">{{ r.userName || '-' }}</span>

          <!-- 리뷰 내용 -->
          <span class="truncate" :title="r.comment || '-'">{{ r.comment || '-' }}</span>

          <span class="text-center">
            <span :class="['inline-block px-2 py-0.5 rounded-full text-xs font-bold',
                          r.visible ? 'bg-black text-white' : 'bg-gray-200 text-gray-600']">
              {{ r.visible ? '공개' : '숨김' }}
            </span>
          </span>
          <span class="text-gray-600">{{ formatDate(r.createdAt) }}</span>

          <span class="text-center">
            <button class="px-2 py-1 rounded border" @click="openReport(r)">신고</button>
          </span>
        </div>

        <div v-if="!rows.length" class="p-6 text-center text-gray-400">표시할 리뷰가 없습니다.</div>
      </template>
    </section>

    <!-- 신고 모달 -->
    <ReportDialog :open="reportOpen" @close="reportOpen=false" @submit="submitReport" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ReportDialog from '@/components/review/ReportDialog.vue'
import { api } from '@/router' // 기존 router/index.js 에서 export 한 api 도우미 사용
import { reportReview } from '@/api/reviews' // 기존 사용자/오너 공용 신고 API

const route = useRoute()
const hotelId = ref(Number(route.params.hotelId))

const state = reactive({
  page: 0,
  size: 20
})
const q = ref('')
const visible = ref(null)       // null | true | false
const reportedOnly = ref(false)
const loading = ref(false)
const rows = ref([])

function fmtBool(v){ return v === null ? null : !!v }
function formatDate(v) {
  if (!v) return '-'
  const d = new Date(v)
  if (!isNaN(d)) return d.toISOString().slice(0, 10)
  return String(v).slice(0, 10)
}

async function load() {
  if (!hotelId.value) return
  loading.value = true
  try {
    const p = new URLSearchParams()
    p.set('page', state.page)
    p.set('size', state.size)
    if (q.value) p.set('q', q.value)
    if (visible.value !== null) p.set('visible', String(fmtBool(visible.value)))
    if (reportedOnly.value) p.set('reportedOnly', 'true')

    const res = await api(`/api/owner/hotels/${hotelId.value}/reviews?` + p.toString(), { method: 'GET' })
    if (!res.ok) throw new Error('load failed')
    const data = await res.json()
    const list = Array.isArray(data?.content) ? data.content : []
    rows.value = list.map(normRow)
  } catch (e) {
    console.error(e)
    rows.value = []
  } finally {
    loading.value = false
  }
}

function normRow(r = {}) {
  return {
    id: r.id,
    userName: r.userName ?? r.reviewerName ?? '-', // 백에서 사용자명 내려오면 매핑
    comment: r.comment ?? null,
    visible: typeof r.visible === 'boolean' ? r.visible
            : String(r.visible ?? '').toLowerCase() !== 'false',
    createdAt: r.createdAt ?? r.created_at ?? null
  }
}

watch(() => route.params.hotelId, v => {
  const n = Number(v)
  if (!isNaN(n)) {
    hotelId.value = n
    load()
  }
})

onMounted(load)

/* ───── 신고 모달 ───── */
const reportOpen = ref(false)
const targetReviewId = ref(null)

function openReport(r){
  targetReviewId.value = r.id
  reportOpen.value = true
}

async function submitReport({ reasonCode, detail }) {
  if (!targetReviewId.value) return
  try {
    await reportReview(targetReviewId.value, { reason: reasonCode, detail, reporterType: 'OWNER' })
    alert('신고가 접수되었습니다.')
  } catch (e) {
    console.error(e)
    alert('신고에 실패했습니다.')
  } finally {
    reportOpen.value = false
  }
}
</script>

<style scoped>
.table-head,
.table-row {
  display: grid;
  grid-template-columns: 84px 1fr 2fr 90px 150px 110px; /* #, 작성자, 내용, 공개, 작성일, 액션 */
}
.table-row:hover { background: #fafafa; }
.truncate {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>

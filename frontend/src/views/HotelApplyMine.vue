<template>
  <div class="mx-auto max-w-5xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold">내 호텔 신청 목록</h1>
        <p class="mt-1 text-xs text-neutral-600">신청 후 상태가 변경되면 여기에서 확인할 수 있어요.</p>
      </div>
      <router-link
        to="/hotelapply"
        class="rounded-lg bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700"
      >
        + 새로 신청하기
      </router-link>
    </div>

    <div
      v-if="banner"
      class="mb-6 rounded-xl px-4 py-3 text-sm shadow-sm ring-1"
      :class="banner.type === 'error'
        ? 'bg-rose-50 text-rose-800 ring-rose-200'
        : 'bg-green-50 text-green-800 ring-green-200'"
    >
      {{ banner.text }}
    </div>

    <div class="card p-0 overflow-hidden">
      <table class="w-full border-collapse">
        <thead class="bg-neutral-50">
          <tr class="text-left text-sm text-neutral-600">
            <th class="px-4 py-3 w-24">신청 ID</th>
            <th class="px-4 py-3">호텔명</th>
            <th class="px-4 py-3 w-36">상태</th>
            <th class="px-4 py-3 w-44">신청일</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="it in rows" :key="it.id" class="border-t align-top">
            <td class="px-4 py-3 text-sm text-neutral-700">#{{ it.id }}</td>

            <td class="px-4 py-3">
              <div class="text-sm font-semibold text-neutral-900">{{ it.hotelName }}</div>
              <div class="text-xs text-neutral-500 truncate">
                {{ it.address1 }} <span v-if="it.address2">, {{ it.address2 }}</span>
              </div>

              <!-- REJECTED면 토글 가능 -->
              <div v-if="it.status==='REJECTED' && it.reviewMemo" class="mt-2">
                <button
                  class="toggle-btn"
                  type="button"
                  @click="toggleReject(it.id)"
                  :aria-expanded="isOpen(it.id) ? 'true' : 'false'"
                  :aria-controls="`rej-${it.id}`"
                >
                  {{ isOpen(it.id) ? '반려 사유 닫기' : '반려 사유 보기' }}
                </button>

                <!-- 토글 콘텐츠 -->
                <div
                  v-show="isOpen(it.id)"
                  :id="`rej-${it.id}`"
                  class="mt-2 rounded-lg bg-rose-50 px-3 py-2 text-xs leading-5 text-rose-800 ring-1 ring-rose-200"
                >
                  <div class="mb-1 font-semibold">반려 사유</div>
                  <div class="whitespace-pre-line break-words">
                    {{ it.reviewMemo }}
                  </div>
                  <div v-if="it.reviewedAt" class="mt-1 text-[11px] text-rose-700/80">
                    처리 일시: {{ fmt(it.reviewedAt) }}
                  </div>
                </div>
              </div>
            </td>

            <td class="px-4 py-3">
              <span :class="badgeCls(it.status)">{{ statusLabel(it.status) }}</span>
            </td>

            <td class="px-4 py-3 text-sm text-neutral-700">{{ fmt(it.createdAt) }}</td>
          </tr>

          <tr v-if="!loading && rows.length === 0">
            <td colspan="4" class="px-4 py-8 text-center text-neutral-400">신청 내역이 없습니다.</td>
          </tr>
        </tbody>
      </table>

      <div v-if="loading" class="px-4 py-3 text-sm text-neutral-500">불러오는 중…</div>
    </div>

    <!-- 페이징 -->
    <div class="mt-4 flex items-center justify-center gap-2">
      <button class="pg" :disabled="page===0 || loading" @click="go(page-1)">이전</button>
      <span class="text-sm text-neutral-600">Page {{ page+1 }} / {{ totalPages || 1 }}</span>
      <button class="pg" :disabled="page+1>=totalPages || loading" @click="go(page+1)">다음</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api/auth'

type Banner = { type: 'success' | 'error'; text: string }

type MyRow = {
  id: number
  hotelName: string
  address1?: string
  address2?: string
  status: 'PENDING'|'UNDER_REVIEW'|'NEEDS_INFO'|'APPROVED'|'REJECTED'
  createdAt?: string
  reviewedAt?: string
  reviewMemo?: string // DB review_memo -> reviewMemo
}

const banner = ref<Banner | null>(null)
const rows = ref<MyRow[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)

/** 열려 있는 REJECTED 사유의 행 id 모음 */
const openRejectIds = ref<Set<number>>(new Set())

function isOpen(id: number) {
  return openRejectIds.value.has(id)
}
function toggleReject(id: number) {
  const s = new Set(openRejectIds.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  openRejectIds.value = s
}

function fmt(iso?: string) {
  return iso ? iso.replace('T',' ').slice(0,19) : ''
}
function statusLabel(s: string) {
  return s === 'PENDING' ? '대기'
    : s === 'UNDER_REVIEW' ? '심사중'
    : s === 'NEEDS_INFO' ? '추가정보요청'
    : s === 'APPROVED' ? '승인'
    : s === 'REJECTED' ? '반려'
    : s
}
function badgeCls(s: string) {
  return [
    'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1',
    s === 'APPROVED'    ? 'bg-green-50 text-green-700 ring-green-200'
  : s === 'REJECTED'    ? 'bg-rose-50 text-rose-700 ring-rose-200'
  : s === 'UNDER_REVIEW'? 'bg-amber-50 text-amber-700 ring-amber-200'
  : s === 'NEEDS_INFO'  ? 'bg-indigo-50 text-indigo-700 ring-indigo-200'
                        : 'bg-neutral-50 text-neutral-700 ring-neutral-200'
  ].join(' ')
}

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/hotelapp/mine', {
      params: { page: page.value, size: size.value }
    })
    // Spring Page 응답 가정
    rows.value = data?.content ?? []
    totalPages.value = data?.totalPages ?? 0

    // 페이지 이동 시 열림 상태 초기화(선택)
    openRejectIds.value = new Set()
  } catch (e:any) {
    console.error('mine load error:', e)
    banner.value = { type: 'error', text: (e?.response?.data?.error || e.message || '목록 조회 실패') }
  } finally {
    loading.value = false
  }
}
function go(p:number) {
  page.value = Math.max(0, p)
  load()
}

onMounted(load)
</script>

<style scoped>
.card { @apply rounded-2xl bg-white shadow-sm ring-1 ring-neutral-200; }
.pg { @apply rounded-lg border border-neutral-300 px-3 py-1.5 text-sm text-neutral-700 disabled:opacity-40; }

/* 반려 사유 토글 버튼 */
.toggle-btn {
  @apply inline-flex items-center text-xs font-medium text-rose-700 hover:text-rose-800
         underline underline-offset-2 decoration-rose-300 hover:decoration-rose-500;
}
</style>

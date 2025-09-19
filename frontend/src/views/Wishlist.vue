<template>
  <!-- SearchView와 동일하게 전체폭 컨테이너 사용 -->
  <div class="search-root">
    <div class="page">
      <!-- 상단 정보/액션 + 가운데 제목 -->
      <div class="topbar">
        <!-- 좌측: 총 개수 (없으면 자리 유지용 placeholder) -->
        <span class="count" v-if="total !== null">
          총 {{ total.toLocaleString('ko-KR') }}개
        </span>
        <span class="count count--placeholder" v-else aria-hidden="true"></span>

        <!-- 가운데: 제목 -->
        <h2 class="title">내 위시리스트</h2>

        <!-- 우측: 새로고침 -->
        <button
          class="btn-refresh"
          :disabled="loading"
          @click="reload"
        >
          새로고침
        </button>
      </div>

      <!-- 리스트형 아이템 -->
      <div class="list">
        <article
          v-for="it in items"
          :key="it.wishlistId"
          class="item"
        >
          <!-- 썸네일 -->
          <div class="thumb" @click="goDetail(it.hotel.id)">
            <img :src="coverOf(it)" alt="" />
          </div>

          <!-- 본문 -->
          <div class="body">
            <div class="row">
              <h3 class="name" @click="goDetail(it.hotel.id)">
                {{ it.hotel.name }}
              </h3>
              <span v-if="it.hotel.rating != null" class="rating">
                ★ {{ fmtRating(it.hotel.rating) }}
              </span>
            </div>

            <div class="meta">
              <span>{{ it.hotel.region }}</span>
              <span class="dot">·</span>
              <span class="address">{{ it.hotel.address }}</span>
            </div>

            <div class="time">
              등록: {{ fmtDateTime(it.createdAt) }}
            </div>
          </div>

          <!-- 액션 -->
          <div class="actions">
            <button class="btn-outline" @click="goDetail(it.hotel.id)">View Place</button>
            <button class="btn-danger" @click="onRemove(it)">찜 삭제</button>
          </div>
        </article>
      </div>

      <!-- 더보기 (4개씩) -->
      <div class="more" v-if="hasMore">
        <button
          class="btn-more"
          :disabled="loading"
          @click="loadMore"
        >
          {{ loading ? '로딩중...' : '더보기 4개' }}
        </button>
      </div>

      <!-- 빈 결과 -->
      <div v-if="items.length === 0 && !loading" class="empty">
        찜한 호텔이 없습니다.
      </div>

      <!-- 에러 -->
      <div v-if="err" class="error">{{ err }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchWishlist, deleteWishlist } from '@/api/wishlistApi.js'

const router = useRouter()

// 오늘/내일 (상세보기 이동 시 쿼리로 사용)
const today = new Date()
const tomorrow = new Date(today); tomorrow.setDate(today.getDate() + 1)
const fmtDate = d => d.toISOString().slice(0, 10)
const todayStr = fmtDate(today)
const tomorrowStr = fmtDate(tomorrow)

const items = ref([])        // [{ wishlistId, createdAt, hotel:{ id, name, ... } }]
const total = ref(null)
const hasMore = ref(false)
const nextOffset = ref(0)
const limit = 4
const loading = ref(false)
const err = ref('')

async function loadPage(offset = 0, append = false) {
  loading.value = true
  err.value = ''
  try {
    const res = await fetchWishlist({ limit, offset })
    if (append) items.value.push(...(res.items || []))
    else items.value = res.items || []
    total.value = res.total ?? 0
    hasMore.value = !!res.hasMore
    nextOffset.value = res.nextOffset ?? (offset + (res.items?.length || 0))
  } catch (e) {
    err.value = e?.response?.data?.message || e?.message || '조회 실패'
  } finally {
    loading.value = false
  }
}
function reload(){ loadPage(0, false) }
function loadMore(){ loadPage(nextOffset.value, true) }

function coverOf(it) {
  return it?.hotel?.coverImageUrl || '/images/hotel/placeholder.jpg'
}

function fmtRating(r) {
  const n = Number(r)
  return Number.isFinite(n) ? n.toFixed(1) : r
}
function fmtDateTime(s) {
  const d = new Date(s)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}.${m}.${day} ${hh}:${mm}`
}

// View Place: 현재 날짜(오늘~내일), 인원 1명으로 상세페이지 이동
function goDetail(hotelId) {
  router.push({
    path: `/hotels/${hotelId}`,
    query: { checkIn: todayStr, checkOut: tomorrowStr, guests: 1 }
  })
}

// 찜 삭제
async function onRemove(it) {
  if (!confirm('해당 호텔을 찜 목록에서 삭제할까요?')) return
  try {
    await deleteWishlist(it.wishlistId)
    items.value = items.value.filter(x => x.wishlistId !== it.wishlistId)
    total.value = Math.max(0, (total.value || 0) - 1)
    if (items.value.length < limit && hasMore.value) {
      await loadMore()
    }
  } catch (e) {
    alert(e?.response?.data?.message || '삭제 실패')
  }
}

onMounted(() => loadPage(0, false))
</script>

<style scoped>
/* SearchView와 동일 레이아웃 */
.search-root{
  position: relative;
  left: 50%;
  right: 50%;
  margin-left: -50vw;
  margin-right: -50vw;
  width: 100vw;
}
.page{
  width: 100%;
  max-width: 1560px;
  padding-left: 24px;
  padding-right: 24px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 상단 (좌: 개수 / 중: 제목 / 우: 새로고침) */
.topbar{
  display:grid;
  grid-template-columns: 1fr auto 1fr;
  align-items:center;
  gap:8px;
  margin-bottom:16px;
}
.count{ color:#6b7280; font-size:14px; justify-self:start; }
.count--placeholder{ visibility:hidden; }
.title{
  justify-self:center;
  margin:0;
  font-size:22px;
  font-weight:800;
  letter-spacing:.2px;
  color:#0f172a;
}
.btn-refresh{
  justify-self:end;
  padding:8px 12px;
  border:1px solid #e5e7eb;
  border-radius:10px;
  background:#fff;
  cursor:pointer;
}
.btn-refresh:hover{ background:#f8fafc; }
.btn-refresh:disabled{ opacity:.6; cursor:not-allowed; }

/* 리스트 */
.list{ display:grid; gap:12px; }
.item{
  display:grid;
  grid-template-columns: 280px 1fr 160px;  /* 썸네일 / 본문 / 액션 */
  gap:16px;
  border:1px solid #e5e7eb; border-radius:14px; background:#fff; overflow:hidden;
  transition: box-shadow .15s ease, transform .05s ease;
  padding:12px;
}
.item:hover{ box-shadow: 0 6px 20px rgba(2,6,23,0.08) }

/* 썸네일 */
.thumb{ width:100%; aspect-ratio: 16/10; background:#f8fafc; border-radius:10px; overflow:hidden; cursor:pointer; }
.thumb img{ width:100%; height:100%; object-fit:cover; display:block; }

/* 본문 */
.body{ display:flex; flex-direction:column; justify-content:center; min-width:0; }
.row{ display:flex; align-items:flex-start; justify-content:space-between; gap:12px; }
.name{ font-weight:700; font-size:18px; cursor:pointer; line-height:1.35; }
.rating{ font-size:13px; color:#0a6; white-space:nowrap; }

.meta{ margin-top:6px; color:#374151; font-size:14px; }
.meta .dot{ margin:0 .35rem; opacity:.6 }
.address{ display:inline-block; max-width:100%; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.time{ margin-top:8px; color:#94a3b8; font-size:12px; }

/* 액션 */
.actions{
  display:flex; flex-direction:column; gap:8px; align-items:stretch; justify-content:center;
}
.btn-outline{
  padding:10px 12px; border:1px solid #e5e7eb; border-radius:10px; background:#fff; font-size:14px; cursor:pointer;
}
.btn-outline:hover{ background:#f8fafc; }
.btn-danger{
  padding:10px 12px; border:1px solid #fecaca; background:#fff1f2; color:#991b1b; border-radius:10px; font-size:14px; cursor:pointer;
}
.btn-danger:hover{ background:#ffe4e6; }

/* 더보기/빈상태/에러 */
.more{ margin-top:24px; text-align:center; }
.btn-more{ padding:12px 16px; border:1px solid #e5e7eb; border-radius:10px; background:#fff; cursor:pointer; }
.btn-more:hover{ background:#f8fafc; }

.empty{ text-align:center; color:#6b7280; padding: 40px 0; }
.error{
  color:#b91c1c; background:#fef2f2; border:1px solid #fecaca;
  border-radius: 10px; padding: 8px 10px; margin-top: 16px;
}

/* 반응형: 태블릿/모바일에서는 세로 스택 */
@media (max-width: 1024px){
  .item{ grid-template-columns: 240px 1fr; }
  .actions{ grid-column: 1 / -1; flex-direction:row; justify-content:flex-end; }
}
@media (max-width: 640px){
  .item{ grid-template-columns: 1fr; }
  .thumb{ aspect-ratio: 16/9; }
  .actions{ justify-content:stretch; }
  .title{ font-size:20px; }
}
</style>

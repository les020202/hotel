<template>
  <div class="wrap">
    <header class="topbar">
      <div class="titles">
        <h1>리뷰 / 신고 관리</h1>
        <p class="sub">실시간 검색·필터 / 관리자 전용</p>
      </div>

      <div class="tabs">
        <button
          class="tab"
          :class="{active: tab==='reviews'}"
          @click="tab='reviews'; fetchReviews()">
          리뷰 관리
        </button>
        <button
          class="tab"
          :class="{active: tab==='reports'}"
          @click="tab='reports'; fetchReports()">
          신고 관리
        </button>
      </div>
    </header>

    <!-- 필터/검색 -->
    <section class="card filters">
      <div class="row">
        <input class="search" v-model.trim="q" placeholder="호텔명/작성자/내용/사유 검색" />
        <div class="sp"></div>

        <label class="chip">
          <span>최소평점</span>
          <select v-model.number="minRating">
            <option :value="null">전체</option>
            <option v-for="n in [1,2,3,4,5]" :key="'min'+n" :value="n">{{n}}</option>
          </select>
        </label>
        <label class="chip">
          <span>최대평점</span>
          <select v-model.number="maxRating">
            <option :value="null">전체</option>
            <option v-for="n in [1,2,3,4,5]" :key="'max'+n" :value="n">{{n}}</option>
          </select>
        </label>

        <label v-if="tab==='reviews'" class="chip">
          <span>공개여부</span>
          <select v-model="visible">
            <option :value="null">전체</option>
            <option :value="true">공개</option>
            <option :value="false">숨김</option>
          </select>
        </label>

        <label v-if="tab==='reports'" class="chip">
          <input type="checkbox" v-model="ownerOnly" />
          <span>OWNER 신고만</span>
        </label>
      </div>
    </section>

    <!-- 표 -->
    <section class="card table">
      <div class="table-head reviews-grid" v-if="tab==='reviews'">
        <span>#</span>
        <span>호텔</span>
        <span>작성자</span>
        <span class="center">공개</span>
        <span>작성일</span>
        <span class="center">액션</span>
      </div>

      <div class="table-head reports-grid" v-else>
        <span>#신고</span>
        <span>호텔</span>
        <span>리뷰작성자</span>
        <span>신고자</span>
        <span>사유</span>
        <span>신고일</span>
        <span class="center">액션</span>
      </div>

      <div v-if="loading" class="skeleton-wrap">
        <div class="skeleton-row" v-for="i in 6" :key="i" />
      </div>

      <!-- 리뷰 목록 -->
      <template v-else-if="tab==='reviews'">
        <div
          v-for="r in filteredReviews"
          :key="r.id"
          class="table-row reviews-grid clickable"
          @click="openDetail(r)"
        >
          <span class="muted">#{{ r.id }}</span>
          <span class="ellipsis" :title="r.hotelName">{{ r.hotelName || '-' }}</span>
          <span class="ellipsis" :title="r.userName">{{ r.userName || '-' }}</span>
          <span class="center">
            <span :class="['badge', r.visible ? 'ok' : 'no']">
              {{ r.visible ? '공개' : '숨김' }}
            </span>
          </span>
          <span class="muted">{{ r.createdDate || formatDate(r.createdAt) || '-' }}</span>
          <span class="center actions-col">
            <button class="btn danger sm" @click.stop="remove(r.id)">삭제</button>
          </span>
        </div>
        <div v-if="!filteredReviews.length" class="empty">표시할 리뷰가 없습니다.</div>
      </template>

      <!-- 신고 목록 -->
      <template v-else>
        <div
          v-for="x in filteredReports"
          :key="`${x.reportId}-${x.id}`"
          class="table-row reports-grid clickable"
          @click="openDetailFromReport(x)"
        >
          <span class="muted">#{{ x.reportId }}</span>
          <span class="ellipsis" :title="x.hotelName">{{ x.hotelName || '-' }}</span>
          <span class="ellipsis" :title="x.userName">{{ x.userName || '-' }}</span>
          <span>
            <span :class="['badge', x.reporterRole==='ROLE_OWNER' ? 'owner' : '']" :title="x.reporterRole">
              {{ x.reporterName }}
            </span>
          </span>
          <span class="ellipsis" :title="x.reason || '-'">{{ x.reason || '-' }}</span>
          <span class="muted">{{ formatDate(x.reportedAt) || '-' }}</span>
          <span class="center actions-col">
            <button class="btn danger sm" @click.stop="remove(x.id)">리뷰 삭제</button>
          </span>
        </div>
        <div v-if="!filteredReports.length" class="empty">표시할 신고가 없습니다.</div>
      </template>
    </section>

    <!-- 상세 모달 -->
    <div v-if="detail.open" class="modal-backdrop" @click="closeDetail">
      <div class="modal" @click.stop>
        <div class="modal-head">
          <div class="title">
            <span class="muted">#{{ detail.data?.id }}</span>
            <strong class="ml8">{{ detail.data?.hotelName || '-' }}</strong>
          </div>
          <button class="btn close" @click="closeDetail">×</button>
        </div>

        <div class="meta">
          <div>
            <div class="label">작성자</div>
            <div class="val">{{ detail.data?.userName || '-' }}</div>
          </div>
          <div>
            <div class="label">공개</div>
            <div class="val">
              <span :class="['badge', detail.data?.visible ? 'ok' : 'no']">
                {{ detail.data?.visible ? '공개' : '숨김' }}
              </span>
            </div>
          </div>
          <div>
            <div class="label">작성일</div>
            <div class="val">{{ detail.data?.createdDate || formatDate(detail.data?.createdAt) || '-' }}</div>
          </div>
          <div>
            <div class="label">평점</div>
            <div class="val stars" :title="detail.data?.rating ?? '-'">
              <span v-for="i in 5" :key="i" class="star" :class="{on: i <= (detail.data?.rating || 0)}">★</span>
              <span class="num">{{ detail.data?.rating ?? '-' }}</span>
            </div>
          </div>
        </div>

        <div class="section">
          <div class="sec-title">내용</div>
          <div class="comment" v-if="detail.data?.comment">{{ detail.data.comment }}</div>
          <div class="muted" v-else>-</div>
        </div>

        <div class="section">
          <div class="sec-title">사진 ({{ detail.photos.length }})</div>
          <div v-if="detail.loadingPhotos" class="muted">사진 불러오는 중…</div>
          <div v-else-if="!detail.photos.length" class="muted">첨부 사진 없음</div>
          <div v-else class="photos">
            <a v-for="(p,idx) in detail.photos" :key="idx" :href="p" target="_blank" rel="noopener">
              <img :src="p" alt="review photo"/>
            </a>
          </div>
        </div>
      </div>
    </div>

    <!-- 토스트 -->
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminFetchReviews, adminFetchReports, adminDeleteReview, adminFetchReviewDetail } from '@/api/adminReviews'

const tab = ref('reviews')
const loading = ref(false)
const q = ref('')
const minRating = ref(null)
const maxRating = ref(null)
const visible = ref(null)
const ownerOnly = ref(false)

const rowsReviews = ref([])
const rowsReports = ref([])
const toast = ref('')

// 상세 상태
const detail = ref({
  open: false,
  data: null,          // { id, hotelName, userName, rating, comment, visible, createdAt/createdDate }
  photos: [],
  loadingPhotos: false
})

// ── Normalizers
function normReview(r = {}) {
  const createdAtRaw = r.createdAt ?? r.created_at ?? r.created ?? null
  const createdDate =
    r.createdDate ??
    r.created_date ??
    (createdAtRaw ? new Date(createdAtRaw).toISOString().slice(0, 10) : null)

  return {
    id:           r.id ?? r.reviewId ?? r.review_id ?? null,
    hotelName:    r.hotelName ?? r.hotel_name ?? r.hotel ?? '-',
    userName:     r.userName ?? r.user_name ?? r.author ?? r.reviewerName ?? '-',
    rating:       Number(r.rating ?? 0),
    comment:      r.comment ?? r.content ?? null,
    photoCount:   r.photoCount ?? r.photos?.length ?? r.photo_count ?? 0,
    visible:      typeof r.visible === 'boolean' ? r.visible
                  : String(r.visible ?? '').toLowerCase() !== 'false',
    createdAt:    createdAtRaw,
    createdDate,
  }
}

function normReport(x = {}) {
  return {
    reportId:     x.reportId ?? x.id ?? x.report_id ?? null,
    id:           x.reviewId ?? x.review_id ?? x.review?.id ?? null,
    hotelName:    x.hotelName ?? x.hotel_name ?? x.hotel ?? '-',
    userName:     x.userName ?? x.user_name ?? x.reviewUserName ?? '-',
    rating:       Number(x.rating ?? 0),
    comment:      x.comment ?? x.reviewComment ?? null,
    reporterName: x.reporterName ?? x.reporter_name ?? x.reporter ?? '-',
    reporterRole: x.reporterRole ?? x.reporter_role ?? 'ROLE_USER',
    reason:       x.reason ?? x.code ?? x.detail ?? null,
    reportedAt:   x.reportedAt ?? x.createdAt ?? x.reported_at ?? null,
  }
}

function formatDate(v) {
  if (!v) return ''
  try {
    const d = new Date(v)
    if (!isNaN(d)) return d.toISOString().slice(0, 10)
    return String(v).slice(0, 10)
  } catch { return '' }
}

// 필터
const filteredReviews = computed(() => {
  const k = q.value.toLowerCase()
  return rowsReviews.value.filter(r => {
    if (minRating.value && r.rating < minRating.value) return false
    if (maxRating.value && r.rating > maxRating.value) return false
    if (visible.value !== null && r.visible !== visible.value) return false
    if (!k) return true
    return [r.hotelName, r.userName, r.comment].filter(Boolean).some(v => String(v).toLowerCase().includes(k))
  })
})
const filteredReports = computed(() => {
  const k = q.value.toLowerCase()
  return rowsReports.value.filter(x => {
    if (minRating.value && x.rating < minRating.value) return false
    if (maxRating.value && x.rating > maxRating.value) return false
    if (ownerOnly.value && x.reporterRole !== 'ROLE_OWNER') return false
    if (!k) return true
    return [x.hotelName, x.userName, x.comment, x.reporterName, x.reason].filter(Boolean).some(v => String(v).toLowerCase().includes(k))
  })
})

// 서버 호출
async function fetchReviews() {
  loading.value = true
  try {
    const res = await adminFetchReviews({ minRating: minRating.value, maxRating: maxRating.value, visible: visible.value, q: q.value })
    const list = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : []
    rowsReviews.value = list.map(normReview)
  } catch (e) {
    console.error(e); rowsReviews.value = []
  } finally {
    loading.value = false
  }
}

async function fetchReports() {
  loading.value = true
  try {
    const res = await adminFetchReports({ minRating: minRating.value, maxRating: maxRating.value, ownerOnly: ownerOnly.value, q: q.value })
    const list = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : []
    rowsReports.value = list.map(normReport)
  } catch (e) {
    console.error(e); rowsReports.value = []
  } finally {
    loading.value = false
  }
}

async function remove(reviewId) {
  if (!confirm('해당 리뷰를 삭제할까요?\n신고/사진 등 연관 데이터도 함께 삭제됩니다.')) return
  try {
    await adminDeleteReview(reviewId)
    rowsReviews.value = rowsReviews.value.filter(r => r.id !== reviewId)
    rowsReports.value = rowsReports.value.filter(x => x.id !== reviewId)
    showToast('삭제되었습니다.')
    if (detail.value.open && detail.value.data?.id === reviewId) closeDetail()
  } catch (e) {
    console.error(e)
    showToast('삭제 실패: ' + (e?.message || '서버 오류'))
  }
}

// 상세 열기: 리뷰 탭에서
function openDetail(row) {
  detail.value.open = true
  detail.value.data = { ...row } // rating/comment 포함됨
  loadPhotos(row.id)
}
// 상세 열기: 신고 탭에서 (리뷰ID 기준으로)
function openDetailFromReport(rep) {
  detail.value.open = true
  // 신고행에도 rating/comment가 있으니 우선 채워두고 사진 fetch
  detail.value.data = normReview({
    id: rep.id,
    hotelName: rep.hotelName,
    userName: rep.userName,
    rating: rep.rating,
    comment: rep.comment,
    createdAt: rep.reportedAt // 없으면 포맷에서 처리됨
  })
  loadPhotos(rep.id)
}

async function loadPhotos(reviewId) {
  detail.value.loadingPhotos = true
  detail.value.photos = []
  try {
    // 백엔드: GET /api/admin/reviews/{id} → { id, rating, comment, photos: string[], ... }
    const res = await adminFetchReviewDetail(reviewId)
    const photos = res?.photos || res?.content || []
    // 상세 응답에 rating/comment가 더 최신이면 병합
    detail.value.data = { ...detail.value.data, ...res }
    detail.value.photos = photos
  } catch (e) {
    console.error(e)
  } finally {
    detail.value.loadingPhotos = false
  }
}

function closeDetail(){ detail.value.open = false; detail.value.photos = []; detail.value.data = null }

function showToast(msg){ toast.value = msg; setTimeout(()=> toast.value='', 1500) }

onMounted(() => {
  fetchReviews()
  fetchReports()
})
</script>

<style scoped>
.wrap{ padding:20px 22px 40px; color:#0f172a; font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial }
.topbar{
  display:flex; align-items:flex-end; justify-content:space-between; gap:16px;
  margin-bottom:16px; padding:18px 18px 16px; border:1px solid #e7edf7; border-radius:16px;
  background: radial-gradient(800px 260px at 8% 0%, #eef6ff 0%, transparent 60%), linear-gradient(180deg,#fff,#f9fbff);
  box-shadow: 0 10px 24px rgba(15,23,42,.05);
}
.titles h1{ margin:0; font-size:20px; font-weight:800 }
.titles .sub{ margin:4px 0 0; color:#6b7280; font-size:12px }

.tabs{ display:flex; gap:8px }
.tab{
  padding:8px 12px; border-radius:12px; border:1px solid #e1e8f5; background:#fff; cursor:pointer;
  font-weight:800; color:#0f172a;
}
.tab.active{ background:#111827; color:#fff; border-color:#111827 }

.card{ border:1px solid #e7edf7; border-radius:16px; background:#fff; box-shadow:0 12px 28px rgba(15,23,42,.05) }

.filters{ padding:12px 14px; margin-bottom:12px }
.filters .row{ display:flex; align-items:center; gap:10px; flex-wrap:wrap }
.search{
  min-width:260px; flex:1 1 280px;
  padding:10px 12px; border-radius:12px; border:1px solid #e1e8f5;
}
.sp{ flex:1 }

.chip{
  display:flex; align-items:center; gap:8px; padding:8px 10px;
  border:1px solid #e1e8f5; border-radius:12px; background:#fff;
}
.chip select{ border:0; outline:none; background:transparent }

.table{ padding:8px }
.table-head, .table-row{
  display:grid; align-items:center; gap:10px; padding:12px;
}
.table-head{
  position:sticky; top:0; background:#f9fbff; z-index:1; border-radius:12px; font-weight:700; color:#475569
}
.table-row{ border-top:1px solid #f0f4fb }
.table-row:hover{ background:#fcfdff }
.table-row.clickable{ cursor:pointer }

.reviews-grid{ grid-template-columns: 80px 1.4fr 1fr 90px 150px 130px }
.reports-grid{ grid-template-columns: 80px 1.4fr 1fr 1fr 1.8fr 150px 130px }

.center{ text-align:center }
.strong{ font-weight:800 }
.muted{ color:#6b7280 }
.ellipsis{ overflow:hidden; text-overflow:ellipsis; white-space:nowrap }

.badge{ display:inline-block; padding:3px 10px; border-radius:999px; font-weight:800; font-size:12px; background:#eef2ff; color:#475569 }
.badge.ok{ background:#0f172a; color:#fff }
.badge.no{ background:#f1f5f9; color:#64748b }
.badge.owner{ background:#fef3c7; color:#a16207; border:1px solid #fde68a }

.actions-col{ display:flex; align-items:center; justify-content:center; gap:6px }
.btn{ padding:6px 10px; border-radius:10px; border:0; cursor:pointer; font-weight:800; font-size:12px }
.btn.danger{ color:#fff; background:#ef4444 }
.btn.close{ background:#f1f5f9; color:#0f172a; border:1px solid #e5e7eb; border-radius:10px; padding:4px 10px; font-size:18px; line-height:1 }

.empty{ text-align:center; color:#94a3b8; padding:22px }
.skeleton-wrap{ padding:8px 12px }
.skeleton-row{ height:46px; border-radius:10px; margin:6px 0; background: linear-gradient(90deg,#f3f6fb 25%,#eaf0f9 37%,#f3f6fb 63%); background-size:400% 100%; animation: shimmer 1.2s infinite }
@keyframes shimmer{ 0%{ background-position: 100% 0 } 100%{ background-position: 0 0 } }

.toast{
  position:fixed; right:18px; bottom:18px; padding:10px 14px; border-radius:12px;
  color:#0f172a; background:#fff; border:1px solid #e7edf7; box-shadow:0 10px 26px rgba(15,23,42,.18);
  font-weight:700; z-index:60;
}

/* 상세 모달 */
.modal-backdrop{
  position:fixed; inset:0; background:rgba(15,23,42,.35); display:flex; align-items:flex-end; justify-content:center; padding:24px; z-index:70;
}
.modal{
  width:min(980px, 96vw); max-height:90vh; overflow:auto;
  background:#fff; border-radius:16px; border:1px solid #e7edf7; box-shadow:0 20px 60px rgba(15,23,42,.25); padding:16px 16px 24px;
}
.modal-head{ display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px }
.modal-head .title{ display:flex; align-items:center; gap:10px; font-size:16px }
.ml8{ margin-left:8px }

.meta{
  display:grid; grid-template-columns: repeat(4, 1fr); gap:10px; padding:12px; border:1px solid #eef2f7; border-radius:12px; background:#fbfdff; margin-bottom:12px
}
.meta .label{ color:#64748b; font-size:12px; margin-bottom:4px }
.meta .val{ font-weight:700 }
.stars .star{ font-size:16px; opacity:.3; margin-right:2px }
.stars .star.on{ opacity:1 }
.stars .num{ margin-left:6px; color:#475569; font-weight:700 }

.section{ margin-top:14px }
.sec-title{ font-weight:800; margin-bottom:8px }
.comment{
  white-space:pre-wrap; line-height:1.6; border:1px solid #eef2f7; border-radius:12px; padding:12px; background:#fff
}
.photos{ display:flex; flex-wrap:wrap; gap:10px }
.photos a{ display:block; width:120px; height:120px; border-radius:10px; overflow:hidden; border:1px solid #e5e7eb; background:#f9fafb }
.photos img{ width:100%; height:100%; object-fit:cover }

@media (max-width:1200px){
  .reviews-grid{ grid-template-columns: 60px 1.2fr 1fr 80px 130px 100px }
  .reports-grid{ grid-template-columns: 60px 1.2fr 1fr .9fr 1.6fr 130px 100px }
}
@media (max-width:900px){
  .reviews-grid{ grid-template-columns: 60px 1fr .9fr 70px 120px 90px }
  .reports-grid{ grid-template-columns: 60px 1fr .9fr .9fr 1.3fr 120px 90px }
  .meta{ grid-template-columns: 1fr 1fr }
}
</style>

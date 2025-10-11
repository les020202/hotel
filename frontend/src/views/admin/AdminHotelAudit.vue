<!-- src/views/admin/HotelAppReviewList.vue -->
<template>
  <section class="wrap">
    <!-- 헤더 -->
    <div class="hero">
      <div>
        <h2>호텔 심사</h2>
        <p>실시간 검색/필터 · 관리자 전용</p>
      </div>
      <AdminHotelTabs />
    </div>

    <!-- 알림 배너 -->
    <div
      v-if="banner"
      class="mb-3 p-3 rounded-lg text-sm"
      :class="banner.type==='error'
        ? 'bg-rose-50 text-rose-800 ring-1 ring-rose-200'
        : 'bg-emerald-50 text-emerald-800 ring-1 ring-emerald-200'"
    >
      {{ banner.text }}
    </div>

    <!-- 검색/필터 바 -->
    <div class="toolbar">
      <input
        v-model.trim="q"
        class="search"
        placeholder="숙소명/소유자/사업자번호 검색"
        @keyup.enter="load"
      />
      <div class="pills">
        <div class="pill">
          <span>상태</span>
          <select v-model="status">
            <option value="">전체</option>
            <option value="PENDING">대기</option>
            <option value="APPROVED">승인</option>
            <option value="REJECTED">반려</option>
          </select>
        </div>
        <button class="pill ghost" @click="load">검색</button>
      </div>
    </div>

    <!-- 표 -->
    <div class="card table-card">
      <table class="table">
        <thead>
          <tr>
            <th style="width:86px">#신청</th>
            <th style="width:120px">썸네일</th>
            <th>숙소명 / 주소</th>
            <th>소유자</th>
            <th>연락처</th>
            <th>신청일</th>
            <th style="width:240px">상태/액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in rows" :key="it.id">
            <td>#{{ it.id }}</td>

            <td>
              <img :src="thumb(it)" class="thumb" alt="" />
            </td>

            <td class="b">
              <button class="link" @click="openDetail(it.id)">{{ it.hotelName }}</button>
              <div class="sub">{{ it.address1 }} <template v-if="it.address2">, {{ it.address2 }}</template></div>
              <div v-if="it.approvedHotelId" class="ok">승인됨 · hotelId={{ it.approvedHotelId }}</div>
              <div v-if="it.status==='REJECTED' && it.reviewMemo" class="rej">
                반려 사유: {{ it.reviewMemo }}
              </div>
            </td>

            <td>{{ it.ownerName }}</td>
            <td>{{ it.phone }}</td>
            <td>{{ fmt(it.createdAt) }}</td>

            <td>
              <span class="badge" :data-variant="it.status">{{ label(it.status) }}</span>

              <button
                class="btn xs"
                @click="approve(it)"
                :disabled="it.status==='APPROVED' || loadingId===it.id"
                :aria-busy="loadingId===it.id"
              >{{ loadingId===it.id ? '승인 중…' : '승인' }}</button>

              <button
                class="btn xs danger"
                @click="openReject(it)"
                :disabled="it.status==='REJECTED'"
              >반려</button>
            </td>
          </tr>

          <tr v-if="!loading && rows.length===0">
            <td colspan="7" class="empty">표시할 신청이 없습니다.</td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="loading">불러오는 중…</div>
    </div>

    <!-- 상세 모달 -->
    <dialog ref="detailDlg" class="modal">
      <div class="modal-body" v-if="detail">
        <div class="flex gap-4 items-start">
          <img :src="thumb(detail)" class="h-28 w-40 object-cover rounded-lg ring-1 ring-black/5" />
          <div>
            <h3>#{{ detail.id }} - {{ detail.hotelName }}</h3>
            <div class="sub">{{ detail.address1 }} <template v-if="detail.address2">, {{ detail.address2 }}</template></div>
          </div>
        </div>

        <ul class="dl">
          <li><b>소유자</b><span>{{ detail.ownerName }}</span></li>
          <li><b>사업자번호</b><span>{{ detail.businessNo }}</span></li>
          <li><b>연락처</b><span>{{ detail.phone }}</span></li>
          <li><b>성급</b><span>{{ detail.gradeLevel }}성급</span></li>
          <li><b>어메니티</b><span>{{ detail.amenitiesCsv || '-' }}</span></li>
          <li><b>코멘트</b><span>{{ detail.comment || '-' }}</span></li>
          <li><b>상태</b><span>{{ label(detail.status) }}</span></li>
          <li><b>신청일</b><span>{{ fmt(detail.createdAt) }}</span></li>
          <li v-if="detail.reviewMemo"><b>반려 사유</b><span class="rej">{{ detail.reviewMemo }}</span></li>
        </ul>

        <!-- rooms_json 표시 -->
        <div v-if="rooms.length" class="rooms">
          <h4>객실 타입</h4>
          <table class="table">
            <thead>
              <tr>
                <th>타입</th><th>객실수</th><th>호실</th>
                <th>정원</th><th>면적(㎡)</th><th>1박(₩)</th>
                <th>환불</th><th>조식</th><th>정책</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(r, i) in rooms" :key="i">
                <td>{{ r.typeCode }}</td>
                <td class="num">{{ r.roomCount }}</td>
                <td class="ellipsis">{{ r.roomNos }}</td>
                <td class="num">{{ r.capacity }}</td>
                <td class="num">{{ r.sizeSqm }}</td>
                <td class="num">₩{{ (r.priceKrw||0).toLocaleString() }}</td>
                <td>{{ r.refundable ? 'Y' : 'N' }}</td>
                <td>{{ r.breakfast ? 'Y' : 'N' }}</td>
                <td class="ellipsis">{{ r.policyText }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="btns">
          <button class="btn" @click="detailDlg.close()">닫기</button>
        </div>
      </div>
    </dialog>

    <!-- 반려 모달 -->
    <dialog ref="rejectDlg" class="modal">
      <div class="modal-body">
        <h3>반려 사유 입력</h3>
        <textarea v-model.trim="rejectMemo" class="textarea" rows="4" placeholder="사유를 입력하세요"></textarea>
        <div class="btns">
          <button class="btn danger" @click="doReject">반려</button>
          <button class="btn" @click="rejectDlg.close()">취소</button>
        </div>
      </div>
    </dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import AdminHotelTabs from './AdminHotelTabs.vue'
import api from '@/api/auth'

type Banner = { type:'success'|'error'; text:string }
const banner = ref<Banner|null>(null)

const status = ref('PENDING')
const q = ref('')

const rows = ref<any[]>([])
const loading = ref(false)
const loadingId = ref<number|null>(null)

const detailDlg = ref<HTMLDialogElement|null>(null)
const rejectDlg = ref<HTMLDialogElement|null>(null)
const detail = ref<any|null>(null)
const current = ref<any|null>(null)
const rejectMemo = ref('')

// 로그인된 관리자 아이디(백엔드에서 헤더/세션으로 사용한다면 제거)
const adminId = 1

function fmt(iso?:string){ return iso?.replace('T',' ').slice(0,19) || '' }
function label(s:string){
  return s==='PENDING'?'대기'
    : s==='UNDER_REVIEW'?'검토중'
    : s==='NEEDS_INFO'?'보완요청'
    : s==='APPROVED'?'승인'
    : s==='REJECTED'?'반려' : s
}

function thumb(it:any){
  // 신청서 확장 컬럼을 사용 (없으면 placeholder)
  if (it.coverImageType==='UPLOADED' && it.coverImageUrl) return it.coverImageUrl
  if (it.coverImageType==='TEMPLATE' && it.coverImageTemplate) return `/assets/hotel-covers/${it.coverImageTemplate}.jpg`
  return '/assets/hotel-covers/placeholder.jpg'
}

const rooms = computed(() => {
  const raw = detail.value?.roomsJson
  if (!raw) return []
  try{
    const arr = typeof raw==='string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) ? arr : []
  }catch{ return [] }
})

async function load(){
  loading.value = true
  try{
    const params:any = { page:0, size:20 }
    if (status.value) params.status = status.value
    if (q.value) params.q = q.value
    // GET /admin/hotelapp?status=&q=
    const res = await api.get('/admin/hotelapp', { params })
    rows.value = res.data?.content || res.data?.items || res.data || []
  }catch(e:any){
    banner.value = { type:'error', text: e?.response?.data?.message || e.message || '조회 실패' }
  }finally{
    loading.value = false
  }
}

async function openDetail(id:number){
  try{
    const { data } = await api.get(`/admin/hotelapp/${id}`)
    detail.value = data
    detailDlg.value?.showModal()
  }catch(e:any){
    banner.value = { type:'error', text: e?.response?.data?.message || e.message || '상세 조회 실패' }
  }
}

async function approve(it:any){
  if (!confirm('승인하시겠어요?')) return
  try{
    loadingId.value = it.id
    // POST /admin/hotelapp/{id}/approve  → { hotelId:number }
    const { data } = await api.post(`/admin/hotelapp/${it.id}/approve`, null, {
      headers: { 'X-Admin-Id': String(adminId) }
    })
    // 테이블 반영
    it.status = 'APPROVED'
    it.approvedHotelId = data?.hotelId
    banner.value = { type:'success', text:`승인 완료 (hotelId=${data?.hotelId})` }
  }catch(e:any){
    banner.value = { type:'error', text: e?.response?.data?.message || e.message || '승인 실패' }
  }finally{
    loadingId.value = null
  }
}

function openReject(it:any){
  current.value = it
  rejectMemo.value = ''
  rejectDlg.value?.showModal()
}
async function doReject(){
  if (!rejectMemo.value) return alert('사유를 입력하세요.')
  try{
    // POST /admin/hotelapp/{id}/reject  body { memo }
    await api.post(`/admin/hotelapp/${current.value.id}/reject`, { memo: rejectMemo.value })
    current.value.status = 'REJECTED'
    current.value.reviewMemo = rejectMemo.value
    banner.value = { type:'success', text:'반려 처리했습니다.' }
  }catch(e:any){
    banner.value = { type:'error', text: e?.response?.data?.message || e.message || '반려 실패' }
  }finally{
    rejectDlg.value?.close()
  }
}

onMounted(load)
</script>

<style scoped>
:root{ --line:#e8ecf6; --card:#fff; --muted:#6b7280; --ink:#111827; }
.wrap{ padding:14px }

/* 헤더 */
.hero{
  display:flex; align-items:center; justify-content:space-between;
  padding:16px 18px; border-radius:16px;
  background:linear-gradient(135deg,#f7faff,#f0f6ff);
  border:1px solid #eaf0ff; margin-bottom:12px;
}
.hero h2{ margin:0; font-size:18px; font-weight:800; color:var(--ink) }
.hero p{ margin:4px 0 0; color:var(--muted); font-size:12px }

/* 툴바 */
.toolbar{ display:flex; gap:10px; align-items:center; margin-bottom:10px; flex-wrap:wrap }
.search{ flex:1 1 360px; height:40px; border-radius:12px; border:1px solid var(--line);
  padding:0 14px; outline:none }
.search:focus{ box-shadow:0 0 0 3px rgba(37,99,235,.1); border-color:#cfe0ff }
.pills{ display:flex; gap:8px; flex-wrap:wrap }
.pill{ display:flex; align-items:center; gap:8px; height:40px; padding:0 12px;
  border:1px solid var(--line); background:#fff; border-radius:20px; font-weight:700 }
.pill select{ border:0; background:transparent; outline:none; font-weight:700 }
.pill.ghost{ background:#f7faff } .pill.ghost:hover{ background:#eef5ff }

/* 테이블 */
.card{ background:#fff; border:1px solid var(--line); border-radius:12px }
.table-card{ overflow:auto }
.table{ width:100%; border-collapse:collapse }
th,td{ padding:12px 12px; border-bottom:1px solid #f1f4fb; text-align:left; font-size:14px }
th{ color:#475569; font-weight:800; background:#fbfdff }
.b{ font-weight:700 }
.sub{ color:#6b7280; font-size:12px; margin-top:2px }
.ok{ color:#059669; font-size:12px; margin-top:2px }
.rej{ color:#b91c1c; font-size:12px; margin-top:2px }
.ellipsis{ white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:520px }
.empty{ text-align:center; color:#94a3b8; padding:18px 0 }
.loading{ padding:14px; text-align:center }
.num{ text-align:right }
.thumb{ width:110px; height:70px; object-fit:cover; border-radius:8px; box-shadow:0 1px 0 rgba(0,0,0,.04) }

/* 상태 뱃지 */
.badge{
  display:inline-block; margin-right:8px; padding:2px 8px; border-radius:999px;
  font-size:12px; border:1px solid #e5e7eb; background:#f9fafb;
}
.badge[data-variant="PENDING"]{ background:#fff7ed; border-color:#fed7aa }
.badge[data-variant="UNDER_REVIEW"]{ background:#f5f3ff; border-color:#ddd6fe }
.badge[data-variant="NEEDS_INFO"]{ background:#eff6ff; border-color:#bfdbfe }
.badge[data-variant="APPROVED"]{ background:#ecfdf5; border-color:#a7f3d0 }
.badge[data-variant="REJECTED"]{ background:#fef2f2; border-color:#fecaca }

/* 버튼 & 모달 */
.btn{ height:30px; padding:0 10px; border-radius:10px; border:1px solid #cfe0ff;
  background:#f5f9ff; font-weight:700; cursor:pointer; margin-right:6px }
.btn[aria-busy="true"]{ opacity:.6; pointer-events:none }
.btn:hover{ background:#eaf3ff }
.btn.xs{ height:28px; font-size:12px }
.btn.danger{ background:#fff5f5; border-color:#fecaca }
.btn.danger:hover{ background:#ffe9e9 }
.link{ background:none; border:0; color:#2563eb; text-decoration:underline; cursor:pointer }

.modal{ border:0; border-radius:12px; padding:0; max-width:980px; width:92% }
.modal-body{ padding:18px }
.dl{ list-style:none; padding:0; margin:0 }
.dl li{ display:flex; gap:12px; margin:6px 0 }
.dl b{ min-width:110px }
.textarea{ width:100%; border:1px solid var(--line); border-radius:10px; padding:8px; resize:vertical }
.btns{ display:flex; gap:8px; justify-content:flex-end; margin-top:12px }

.rooms{ margin-top:16px }
.rooms h4{ margin:8px 0 10px; font-weight:800 }

/* 모달을 화면 정중앙에 고정 */
.modal{
  position: fixed;        /* ← 중요 */
  inset: 0;               /* top/right/bottom/left: 0 */
  margin: auto;           /* 가로/세로 중앙 정렬 */
  max-width: 980px;
  width: 92%;
  border: 0;
  border-radius: 12px;
  padding: 0;
  background: #fff;
  box-shadow: 0 20px 60px rgba(0,0,0,.18);
}

/* 백드롭(뒷배경) */
.modal::backdrop{
  background: rgba(0,0,0,.35);
  -webkit-backdrop-filter: saturate(120%) blur(2px);
  backdrop-filter: saturate(120%) blur(2px);
}

/* 내용 패딩은 기존대로 */
.modal-body{ padding:18px }

.modal[open]{ animation: pop .14s ease-out }
@keyframes pop{
  from{ transform: scale(.98); opacity: 0 }
  to  { transform: scale(1);    opacity: 1 }
}
</style>

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
            <option value="UNDER_REVIEW">검토중</option>
            <option value="NEEDS_INFO">보완요청</option>
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
            <th>숙소명</th>
            <th>소유자</th>
            <th>연락처</th>
            <th>신청일</th>
            <th style="width:220px">상태/액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in rows" :key="it.id">
            <td>#{{ it.id }}</td>
            <td class="b">
              <button class="link" @click="openDetail(it.id)">{{ it.hotelName }}</button>
            </td>
            <td>{{ it.ownerName }}</td>
            <td>{{ it.phone }}</td>
            <td>{{ fmt(it.createdAt) }}</td>
            <td>
              <span class="badge" :data-variant="it.status">{{ it.status }}</span>
              <button class="btn xs" @click="approve(it.id)" :disabled="it.status==='APPROVED'">승인</button>
              <button class="btn xs danger" @click="openReject(it.id)" :disabled="it.status==='REJECTED'">반려</button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length===0">
            <td colspan="6" class="empty">표시할 신청이 없습니다.</td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="loading">불러오는 중…</div>
    </div>

    <!-- 상세 모달 -->
    <dialog ref="detailDlg" class="modal">
      <div class="modal-body" v-if="detail">
        <h3>#{{ detail.id }} - {{ detail.hotelName }}</h3>
        <ul class="dl">
          <li><b>소유자</b><span>{{ detail.ownerName }}</span></li>
          <li><b>사업자번호</b><span>{{ detail.businessNo }}</span></li>
          <li><b>연락처</b><span>{{ detail.phone }}</span></li>
          <li><b>성급</b><span>{{ detail.gradeLevel }}성급</span></li>
          <li><b>주소</b>
            <span>
              {{ detail.address1 }}
              <template v-if="detail.address2"> {{ detail.address2 }}</template>
              <template v-if="detail.postcode"> ({{ detail.postcode }})</template>
            </span>
          </li>
          <li><b>어메니티</b><span>{{ detail.amenitiesCsv || '-' }}</span></li>
          <li><b>코멘트</b><span>{{ detail.comment || '-' }}</span></li>
          <li><b>상태</b><span>{{ detail.status }}</span></li>
          <li><b>신청일</b><span>{{ fmt(detail.createdAt) }}</span></li>
        </ul>

        <!-- rooms_json 가공 표시(있을 때) -->
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
        <textarea v-model.trim="rejectReason" class="textarea" rows="4" placeholder="사유를 입력하세요"></textarea>
        <div class="btns">
          <button class="btn danger" @click="doReject">반려</button>
          <button class="btn" @click="rejectDlg.close()">취소</button>
        </div>
      </div>
    </dialog>
  </section>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import AdminHotelTabs from './AdminHotelTabs.vue'
import api from '@/api/auth' // 토큰/리프레시/베이스URL 포함 axios 인스턴스

// 필터
const status = ref('PENDING')
const q = ref('')

// 목록 상태
const rows = ref([])
const loading = ref(false)

// 모달/상세
const detailDlg = ref(null)
const rejectDlg = ref(null)
const detail = ref(null)
const currentId = ref(null)
const rejectReason = ref('')

// rooms_json 파싱 (상세 뷰)
const rooms = computed(() => {
  const raw = detail.value?.roomsJson
  if (!raw) return []
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    // 백엔드에서 그대로 저장한 {typeCode,roomCount,...} 배열을 기대
    return Array.isArray(parsed) ? parsed : []
  } catch (_) {
    return []
  }
})

function fmt(iso){ return iso?.replace('T',' ').slice(0,19) || '' }

// 목록 조회
async function load(){
  loading.value = true
  try{
    const params = {
      page: 0, size: 20,
      ...(status.value ? { status: status.value } : {}),
      ...(q.value ? { q: q.value } : {})
    }
    // 기대: GET /api/admin/hotelapp?status=&q=&page=&size=
    const data = await api.get('/admin/hotelapp', { params }).then(r => r.data)
    rows.value = data?.content || data?.items || data || []
  } finally {
    loading.value = false
  }
}

// 상세 열기
async function openDetail(id){
  // 기대: GET /api/admin/hotelapp/{id}
  const data = await api.get(`/admin/hotelapp/${id}`).then(r => r.data)
  detail.value = data
  detailDlg.value?.showModal()
}

// 승인
async function approve(id){
  if (!confirm('승인하시겠어요?')) return
  // 기대: POST /api/admin/hotelapp/{id}/approve  body: { note?:string }
  const res = await api.post(`/admin/hotelapp/${id}/approve`, { note: '' })
  if (res.status >= 200 && res.status < 300){
    await load()
    alert('승인되었습니다.')
  } else {
    alert('승인 실패')
  }
}

// 반려
function openReject(id){
  currentId.value = id
  rejectReason.value = ''
  rejectDlg.value?.showModal()
}
async function doReject(){
  if (!rejectReason.value) return alert('사유를 입력하세요.')
  // 기대: POST /api/admin/hotelapp/{id}/reject  body: { reason:string, note?:string }
  const res = await api.post(`/admin/hotelapp/${currentId.value}/reject`, {
    reason: rejectReason.value, note: ''
  })
  if (res.status >= 200 && res.status < 300){
    rejectDlg.value?.close()
    await load()
    alert('반려되었습니다.')
  } else {
    alert('반려 실패')
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
.ellipsis{ white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:520px }
.empty{ text-align:center; color:#94a3b8; padding:18px 0 }
.loading{ padding:14px; text-align:center }
.num{ text-align:right }

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

/* rooms 표 */
.rooms{ margin-top:16px }
.rooms h4{ margin:8px 0 10px; font-weight:800 }
</style>

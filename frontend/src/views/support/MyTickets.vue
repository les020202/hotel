<template>
  <div class="page">
    <!-- 뒤로가기 버튼 (SupportInquiry.vue와 동일 스타일) -->
    <button class="back-btn" @click="goBack">← 뒤로가기</button>
    <div class="toolbar">
      <div class="tabs">
        <button :class="{on: q.status==='ALL'}" @click="setStatus('ALL')">전체</button>
        <button :class="{on: q.status==='UPLOADED'}" @click="setStatus('UPLOADED')">접수</button>
        <button :class="{on: q.status==='ANSWERED'}" @click="setStatus('ANSWERED')">답변완료</button>
      </div>
      <div class="search">
        <input v-model.trim="q.keyword" placeholder="제목 검색" @keydown.enter="load" />
        <button @click="load">검색</button>
      </div>
    </div>

    <div v-if="loading" class="loading">불러오는 중…</div>
    <div v-else-if="err" class="error">{{ err }}</div>
    <div v-else-if="items.length===0" class="empty">문의 내역이 없습니다.</div>

    <ul class="list" v-else>
      <li v-for="t in items" :key="t.id" @click="$router.push(`/support/contact/ticket/${t.id}`)">
        <div class="left">
          <div class="title">
            <span class="badge" :class="badgeClass(t.status)">{{ statusLabel(t.status) }}</span>
            {{ t.subject }}
          </div>
          <div class="meta">{{ fmt(t.createdAt) }}</div>
        </div>
        <div class="chev">›</div>
      </li>
    </ul>
  </div>
</template>

<script setup>
// 페이지 상단에 '뒤로가기' 버튼을 추가하고, 나머지 로직은 그대로 유지
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyTickets } from '@/api/support'

const router = useRouter()

const loading = ref(false)
const items = ref([])
const err = ref('')

const q = ref({ status:'ALL', keyword:'' })

function goBack () {
  router.back()
}

function badgeClass(s){ return { UPLOADED:'open', ANSWERED:'answered' }[s] || 'open' }
function statusLabel(s){ return { UPLOADED:'접수', ANSWERED:'답변완료' }[s] || '접수' }
function fmt(iso){
  if(!iso) return ''
  const d = new Date(iso); const z=n=>String(n).padStart(2,'0')
  return `${d.getFullYear()}-${z(d.getMonth()+1)}-${z(d.getDate())} ${z(d.getHours())}:${z(d.getMinutes())}`
}
function setStatus(s){ q.value.status = s; load() }

async function load(){
  loading.value = true
  err.value = ''
  try{
    const params = {}
    if (q.value.status !== 'ALL') params.status = q.value.status
    if (q.value.keyword) params.q = q.value.keyword
    const { data } = await getMyTickets(params)
    items.value = Array.isArray(data) ? data : (data.items || [])
  } catch (e) {
    err.value = e?.response?.data?.message || e?.response?.data?.error || e.message || '조회 실패'
    items.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page{ padding:16px }

/* SupportInquiry.vue의 뒤로가기 버튼 스타일과 동일 */
.back-btn {
  background: none;
  border: none;
  color: #0a6;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 16px;
  display: inline-block;
}
.back-btn:hover {
  text-decoration: underline;
}
.toolbar{ display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:12px; flex-wrap:wrap }
.tabs{ display:flex; gap:8px }
.tabs button{ padding:8px 12px; border:1px solid #e5e7eb; background:#fff; border-radius:8px; cursor:pointer }
.tabs button.on{ background:#0a6; color:#fff; border-color:#0a6 }
.search{ display:flex; gap:8px }
.search input{ padding:8px 10px; border:1px solid #e5e7eb; border-radius:8px; min-width:220px }
.search button{ padding:8px 12px; border:1px solid #e5e7eb; border-radius:8px; background:#fff; cursor:pointer }

.loading,.empty{ padding:20px; color:#6b7280 }
.error{ padding:12px; color:#ef4444 }
.list{ list-style:none; padding:0; margin:0 }
.list li{ display:flex; justify-content:space-between; align-items:center; padding:14px 12px; border-bottom:1px solid #f1f5f9; cursor:pointer }
.list li:hover{ background:#fafafa }
.left .title{ font-weight:600; display:flex; align-items:center; gap:8px }
.left .meta{ color:#6b7280; font-size:13px; margin-top:2px }
.badge{ font-size:12px; padding:4px 8px; border-radius:999px; color:#fff }
.badge.open{ background:#2563eb }
.badge.answered{ background:#10b981 }
.chev{ color:#9ca3af; font-size:22px }
</style>

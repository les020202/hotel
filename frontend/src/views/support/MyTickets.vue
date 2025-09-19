<template>
  <div class="page">
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
import { ref, onMounted } from 'vue'
import { getMyTickets } from '@/api/support'

const loading = ref(false)
const items = ref([])

const q = ref({ status:'ALL', keyword:'' })

function badgeClass(s){
  return { UPLOADED:'open', ANSWERED:'answered' }[s] || 'open'
}
function statusLabel(s){
  return { UPLOADED:'접수', ANSWERED:'답변완료' }[s] || '접수'
}
function fmt(iso){
  if(!iso) return ''
  const d = new Date(iso); const z=n=>String(n).padStart(2,'0')
  return `${d.getFullYear()}-${z(d.getMonth()+1)}-${z(d.getDate())} ${z(d.getHours())}:${z(d.getMinutes())}`
}
function setStatus(s){ q.value.status = s; load() }

async function load(){
  loading.value = true
  try{
    const params = {
      status: q.value.status==='ALL' ? undefined : q.value.status,
      q: q.value.keyword || undefined
    }
    const { data } = await getMyTickets(params)
    items.value = Array.isArray(data) ? data : (data.items || [])
  } finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.page{ padding:16px }
.toolbar{ display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:12px; flex-wrap:wrap }
.tabs{ display:flex; gap:8px }
.tabs button{ padding:8px 12px; border:1px solid #e5e7eb; background:#fff; border-radius:8px; cursor:pointer }
.tabs button.on{ background:#0a6; color:#fff; border-color:#0a6 }
.search{ display:flex; gap:8px }
.search input{ padding:8px 10px; border:1px solid #e5e7eb; border-radius:8px; min-width:220px }
.search button{ padding:8px 12px; border:1px solid #e5e7eb; border-radius:8px; background:#fff; cursor:pointer }

.loading,.empty{ padding:20px; color:#6b7280 }
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

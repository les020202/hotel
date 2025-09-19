<template>
  <div class="page">
    <div class="top">
      <button class="back" @click="$router.back()">‹ 뒤로</button>
      <div class="title">
        <span class="badge" :class="badgeClass(ticket?.status)">{{ statusLabel(ticket?.status) }}</span>
        {{ ticket?.subject || '문의 상세' }}
      </div>
      <div class="meta">{{ fmt(ticket?.createdAt) }}</div>
    </div>

    <div v-if="loading" class="loading">불러오는 중…</div>

    <div v-else>
      <div class="thread">
        <div class="msg" v-for="m in messages" :key="m.id" :class="{mine:isMine(m), admin:!isMine(m)}">
          <div class="who">{{ isMine(m) ? '나' : '운영자' }}</div>
          <div class="body" v-text="m.content" />
          <div class="when">{{ fmt(m.createdAt) }}</div>
        </div>
      </div>

      <div class="composer">
        <textarea v-model.trim="reply" placeholder="추가 문의 내용을 입력하세요" rows="3"></textarea>
        <div class="actions">
          <button class="send" :disabled="!reply" @click="sendReply">보내기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTicket, getTicketMessages, postTicketMessage } from '@/api/support'

// 로그인 사용자 ID를 프론트에서 갖고 있다면 비교용으로 주입
// 이번 예시는 메시지의 senderId가 내 ID와 같으면 '나'로 표기
const myId = window.__ME_ID__ || null

const route = useRoute()
const id = route.params.id

const loading = ref(false)
const ticket = ref(null)
const messages = ref([])
const reply = ref('')

function badgeClass(s){ return { UPLOADED:'open', ANSWERED:'answered' }[s] || 'open' }
function statusLabel(s){ return { UPLOADED:'접수', ANSWERED:'답변완료' }[s] || '접수' }
function fmt(iso){ if(!iso) return ''; const d=new Date(iso); const z=n=>String(n).padStart(2,'0'); return `${d.getFullYear()}-${z(d.getMonth()+1)}-${z(d.getDate())} ${z(d.getHours())}:${z(d.getMinutes())}` }
function isMine(m){ return myId && m.senderId === myId }

async function load(){
  loading.value = true
  try{
    const [{data: t}, {data: ms}] = await Promise.all([
      getTicket(id), getTicketMessages(id)
    ])
    ticket.value = t
    messages.value = Array.isArray(ms) ? ms : (ms.items || [])
  } finally { loading.value = false }
}

async function sendReply(){
  await postTicketMessage(id, reply.value)
  reply.value = ''
  await load()
}

onMounted(load)
</script>

<style scoped>
.page{ padding:16px }
.top .back{ border:0; background:transparent; color:#2563eb; cursor:pointer; margin-bottom:6px }
.top .title{ font-weight:800; font-size:18px; display:flex; gap:8px; align-items:center }
.top .meta{ color:#6b7280; font-size:13px; margin-top:2px }
.badge{ font-size:12px; padding:4px 8px; border-radius:999px; color:#fff }
.badge.open{ background:#2563eb }
.badge.answered{ background:#10b981 }

.thread{ margin-top:12px; display:grid; gap:10px }
.msg{ border:1px solid #e5e7eb; border-radius:10px; padding:10px 12px; background:#fff }
.msg.mine{ border-color:#c7f0e4; background:#f1fffa }
.msg.admin{ border-color:#c7d2fe; background:#eef2ff }
.msg .who{ font-weight:700; margin-bottom:4px }
.msg .when{ color:#6b7280; font-size:12px; margin-top:6px }

.composer{ margin-top:12px; display:grid; gap:8px }
.composer textarea{ resize:vertical; padding:10px; border:1px solid #e5e7eb; border-radius:10px }
.actions{ display:flex; justify-content:flex-end; gap:8px }
.actions .send{ border:0; background:#2563eb; color:#fff; padding:8px 14px; border-radius:8px; cursor:pointer }
.loading{ padding:12px; color:#6b7280 }
</style>

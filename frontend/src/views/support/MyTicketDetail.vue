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

    <div class="legend">
      <span class="pill mine">내 문의</span>는 <strong>운영자에게 전송된 내 메시지</strong>이며,
      <span class="pill admin">운영자 답변</span>은 <strong>운영자가 보낸 회신</strong>입니다.
    </div>

    <div v-if="loading" class="loading">불러오는 중…</div>

    <div v-else>
      <div class="thread">
        <div
          class="msg"
          v-for="m in messages"
          :key="m.id"
          :class="{ mine: !isAdmin(m), admin: isAdmin(m) }"
        >
          <div class="who" :title="isAdmin(m) ? '운영자가 보낸 답변입니다' : '운영자에게 전송된 내 문의입니다'">
            <span class="role-pill" :class="isAdmin(m) ? 'admin' : 'mine'">
              {{ isAdmin(m) ? '운영자 답변' : '내 문의' }}
            </span>
            <small v-if="!isAdmin(m)" class="sub">운영자에게 전송됨</small>
            <small v-else class="sub">운영자 회신</small>
          </div>
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
import { getMe } from '@/api/auth'  // ✅ 내 정보 로드

const route = useRoute()
const id = route.params.id

const loading = ref(false)
const ticket = ref(null)
const messages = ref([])
const reply = ref('')

// ✅ ref로 선언 (중요)
//   예전의 window.__ME_ID__ ?? ... 라인은 삭제하세요!
const myId = ref(null)

function badgeClass(s){ return { UPLOADED:'open', ANSWERED:'answered' }[s] || 'open' }
function statusLabel(s){ return { UPLOADED:'접수', ANSWERED:'답변완료' }[s] || '접수' }
function fmt(iso){
  if(!iso) return ''
  const d = new Date(iso); const z=n=>String(n).padStart(2,'0')
  return `${d.getFullYear()}-${z(d.getMonth()+1)}-${z(d.getDate())} ${z(d.getHours())}:${z(d.getMinutes())}`
}

/** 역할 문자열 추출 (있으면 사용) */
function extractRoles(m){
  let roles = []
  if (m?.senderRole) roles.push(m.senderRole)
  if (m?.role) roles.push(m.role)
  if (m?.userRole) roles.push(m.userRole)
  if (m?.sender?.role) roles.push(m.sender.role)
  if (Array.isArray(m?.roles)) roles = roles.concat(m.roles)
  if (Array.isArray(m?.sender?.roles)) roles = roles.concat(m.sender.roles)
  return roles.filter(Boolean).map(r => String(r).toUpperCase())
}

/** 운영자 메시지인지 판별 (방어 로직 포함) */
function isAdmin(m){
  // 1) 역할 문자열 우선
  const roles = extractRoles(m)
  if (roles.length > 0 && roles.some(r => r.includes('ADMIN'))) return true

  // 2) 역할이 없을 땐 ID 비교(안전 가드)
  //    - myId.value가 아직 null이면 운영자라고 단정하지 않음(에러도 방지)
  if (m?.senderId == null || myId.value == null) return false

  return Number(m.senderId) !== Number(myId.value)
}

async function load(){
  loading.value = true
  try{
    const mePromise = getMe()
      .then(me => { myId.value = me?.id ?? null })
      .catch(() => { myId.value = null })

    const [tRes, msRes] = await Promise.all([ getTicket(id), getTicketMessages(id), mePromise ])
    ticket.value = tRes.data
    const raw = msRes.data
    messages.value = Array.isArray(raw) ? raw : (raw?.items || [])
  } finally {
    loading.value = false
  }
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

.legend{ margin:10px 0 4px; font-size:13px; color:#4b5563; }
.pill{ display:inline-block; padding:2px 8px; border-radius:999px; font-size:12px; color:#fff; }
.pill.mine{ background:#059669 }
.pill.admin{ background:#3b82f6 }
.thread{ margin-top:12px; display:grid; gap:10px }
.msg{ border:1px solid #e5e7eb; border-radius:10px; padding:10px 12px; background:#fff }
.msg.mine{ border-color:#c7f0e4; background:#f1fffa }
.msg.admin{ border-color:#c7d2fe; background:#eef2ff }
.msg .who{ display:flex; align-items:center; gap:8px; font-weight:700; margin-bottom:4px }
.role-pill{ display:inline-block; padding:2px 8px; border-radius:999px; font-size:12px; color:#fff; }
.role-pill.mine{ background:#059669 }
.role-pill.admin{ background:#3b82f6 }
.sub{ color:#6b7280; font-weight:500; font-size:12px }
.msg .when{ color:#6b7280; font-size:12px; margin-top:6px }
.composer{ margin-top:12px; display:grid; gap:8px }
.composer textarea{ resize:vertical; padding:10px; border:1px solid #e5e7eb; border-radius:10px }
.actions{ display:flex; justify-content:flex-end; gap:8px }
.actions .send{ border:0; background:#2563eb; color:#fff; padding:8px 14px; border-radius:8px; cursor:pointer }
.loading{ padding:12px; color:#6b7280 }
</style>

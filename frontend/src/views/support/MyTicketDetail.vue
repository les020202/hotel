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
    <!-- 라벨 의미 안내 -->
    <div class="legend">
      <span class="pill mine">내 문의</span>는 <strong>운영자에게 전송된 내 메시지</strong>이며,
      <span class="pill admin">운영자 답변</span>은 <strong>운영자가 보낸 회신</strong>입니다.
    </div>
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
</template>

<script setup>
/*
  역할 판별 규칙
  - 메시지 객체(m)에서 역할 관련 가능한 모든 필드(senderRole, role, userRole, sender.role, sender.roles 등)를 모아서
    ADMIN 포함 여부를 검사
  - ADMIN(대소문자 무관, 'ROLE_ADMIN' 포함) 문자열이 하나라도 있으면 운영자 답변으로 처리
  - 그 외는 '내 문의'로 처리
*/

import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTicket, getTicketMessages, postTicketMessage } from '@/api/support'

// (선택) 내 사용자 ID가 필요한 경우 대비용 폴백
const myId =
  window.__ME_ID__ ??
  (localStorage.getItem('meId') ? Number(localStorage.getItem('meId')) : null) ??
  null

const route = useRoute()
const id = route.params.id

const loading = ref(false)
const ticket = ref(null)
const messages = ref([])
const reply = ref('')

function badgeClass(s){ return { UPLOADED:'open', ANSWERED:'answered' }[s] || 'open' }
function statusLabel(s){ return { UPLOADED:'접수', ANSWERED:'답변완료' }[s] || '접수' }
function fmt(iso){
  if(!iso) return ''
  const d = new Date(iso); const z=n=>String(n).padStart(2,'0')
  return `${d.getFullYear()}-${z(d.getMonth()+1)}-${z(d.getDate())} ${z(d.getHours())}:${z(d.getMinutes())}`
}

/** 메시지에서 역할 문자열들을 추출 (가능한 모든 경로 지원) */
function extractRoles(m){
  let roles = []
  // 단일 문자열 가능성
  if (m?.senderRole) roles.push(m.senderRole)
  if (m?.role) roles.push(m.role)
  if (m?.userRole) roles.push(m.userRole)
  if (m?.sender?.role) roles.push(m.sender.role)
  // 배열 가능성
  if (Array.isArray(m?.roles)) roles = roles.concat(m.roles)
  if (Array.isArray(m?.sender?.roles)) roles = roles.concat(m.sender.roles)
  // 대문자로 통일
  return roles.filter(Boolean).map(r => String(r).toUpperCase())
}

/** 운영자 메시지인지 판별: ADMIN 문자열 포함 여부 */
function isAdmin(m){
  const roles = extractRoles(m)
  if (roles.length > 0) {
    return roles.some(r => r.includes('ADMIN')) // 'ADMIN', 'ROLE_ADMIN' 모두 매칭
  }
  // 역할 정보가 전혀 없으면 보수적으로 '내 문의'로 처리
  // (원한다면 여기서 senderId 비교로 보완)
  return false
}

async function load(){
  loading.value = true
  try{
    const [{data: t}, {data: ms}] = await Promise.all([ getTicket(id), getTicketMessages(id) ])
    ticket.value = t
    messages.value = Array.isArray(ms) ? ms : (ms.items || [])
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

/* 라벨 의미 안내 */
.legend{
  margin:10px 0 4px;
  font-size:13px;
  color:#4b5563;
}
.pill{
  display:inline-block;
  padding:2px 8px;
  border-radius:999px;
  font-size:12px;
  color:#fff;
  vertical-align:baseline;
}
.pill.mine{ background:#059669 }   /* 내 문의(녹색) */
.pill.admin{ background:#3b82f6 }  /* 운영자 답변(파랑) */
.thread{ margin-top:12px; display:grid; gap:10px }
.msg{ border:1px solid #e5e7eb; border-radius:10px; padding:10px 12px; background:#fff }
.msg.mine{ border-color:#c7f0e4; background:#f1fffa }
.msg.admin{ border-color:#c7d2fe; background:#eef2ff }

.msg .who{ display:flex; align-items:center; gap:8px; font-weight:700; margin-bottom:4px }
.role-pill{
  display:inline-block;
  padding:2px 8px;
  border-radius:999px;
  font-size:12px;
  color:#fff;
}
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

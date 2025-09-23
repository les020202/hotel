<!-- frontend/src/views/admin/support/TicketDetail.vue -->
<template>
    <section class="wrap" v-if="ticket">
      <div class="head">
        <h3>#{{ ticket.id }} · {{ ticket.subject }}</h3>
        <div class="muted">작성자: {{ ticket.userName }} · 상태: {{ ticket.status }}</div>
      </div>
  
      <div class="thread card">
        <div v-for="m in ticket.messages" :key="m.id" class="msg" :class="{ admin: m.isAdmin }">
          <div class="meta">
            <b>{{ m.isAdmin ? '관리자' : (m.senderName||'사용자') }}</b>
            <span class="time">{{ new Date(m.createdAt).toLocaleString() }}</span>
            <span class="spacer" />
            <template v-if="m.isAdmin">
              <button class="link" @click="startEdit(m)">수정</button>
              <button class="link danger" @click="remove(m)">삭제</button>
            </template>
          </div>
  
          <div v-if="editId===m.id">
            <textarea v-model="editText" rows="4" class="ta"></textarea>
            <div class="actions">
              <button class="btn xs" @click="submitEdit(m)">저장</button>
              <button class="btn xs ghost" @click="cancelEdit">취소</button>
            </div>
          </div>
          <div v-else class="content">{{ m.content }}</div>
        </div>
      </div>
  
      <div class="reply card">
        <h4>답변 작성</h4>
        <textarea v-model="reply" rows="4" class="ta" placeholder="관리자 답변을 입력하세요"></textarea>
        <div class="actions">
          <button class="btn" @click="sendReply">등록</button>
        </div>
      </div>
    </section>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { adminGetThread, adminReplyTicket, adminEditMessage, adminDeleteMessage } from '@/api/adminSupport'
  
  const route = useRoute()
  const router = useRouter()
  const id = Number(route.params.id)
  
  const ticket = ref(null)
  const reply = ref('')
  
  const editId = ref(null)
  const editText = ref('')
  
  async function load(){
    const data = await adminGetThread(id)
    ticket.value = data
  }
  onMounted(load)
  
  async function sendReply(){
    if (!reply.value.trim()) return
    await adminReplyTicket(id, reply.value.trim())
    reply.value = ''
    await load()
  }
  
  function startEdit(m){
    editId.value = m.id
    editText.value = m.content
  }
  function cancelEdit(){ editId.value = null; editText.value='' }
  
  async function submitEdit(m){
    await adminEditMessage(m.id, editText.value.trim())
    cancelEdit()
    await load()
  }
  
  async function remove(m){
    if (!confirm('이 메시지를 삭제할까요?')) return
    await adminDeleteMessage(m.id)
    await load()
  }
  </script>
  
  <style scoped>
  .wrap{ padding:14px }
  .card{ background:#fff; border:1px solid #e8ecf6; border-radius:12px; padding:14px; margin:10px 0 }
  .head h3{ margin:0 0 4px }
  .muted{ color:#64748b; font-size:13px }
  .thread .msg{ border-bottom:1px solid #f1f5f9; padding:10px 0 }
  .thread .msg:last-child{ border-bottom:0 }
  .msg.admin .meta b{ color:#2563eb }
  .meta{ display:flex; align-items:center; gap:8px; font-size:13px; color:#475569 }
  .meta .spacer{ flex:1 }
  .link{ background:none; border:0; color:#2563eb; cursor:pointer }
  .link.danger{ color:#ef4444 }
  .content{ white-space:pre-wrap; margin-top:6px }
  .ta{ width:100%; border:1px solid #e2e8f0; border-radius:8px; padding:8px }
  .actions{ margin-top:8px; display:flex; gap:8px }
  .btn{ height:32px; padding:0 12px; border-radius:8px; border:1px solid #cfe0ff; background:#f5f9ff; font-weight:700; cursor:pointer }
  .btn.ghost{ background:#fff }
  .btn.xs{ height:28px; font-size:12px }
  .reply h4{ margin:0 0 8px }
  </style>
  
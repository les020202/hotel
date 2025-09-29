<template>
    <div class="split">
      <div class="left card">
        <div class="toolbar">
          <select v-model="status" class="select" @change="refresh">
            <option value="">전체</option>
            <option value="UPLOADED">접수</option>
            <option value="ANSWERED">답변완료</option>
          </select>
          <input v-model.trim="qExtra" @keyup.enter="refresh" placeholder="추가 키워드 (예: 로그인)" class="search"/>
          <button class="btn" @click="refresh">검색</button>
        </div>
  
        <ul class="list">
          <li v-for="t in rows" :key="t.id" :class="{on: t.id===selectedId}" @click="open(t.id)">
            <div class="tit">{{ t.subject }}</div>
            <div class="meta">#{{ t.id }} · {{ t.status }} · {{ fmt(t.createdAt) }}</div>
          </li>
        </ul>
      </div>
  
      <div class="right card" v-if="detail">
        <h3>#{{ detail.id }} · {{ detail.subject }}</h3>
        <div class="thread">
          <div v-for="m in (detail.messages||[])" :key="m.id" class="msg">
            <div class="who">from: {{ m.sender?.loginId || m.senderId }}</div>
            <div class="body">{{ m.content }}</div>
            <div class="time">{{ fmt(m.createdAt) }}</div>
          </div>
        </div>
  
        <div class="reply">
          <textarea v-model="reply" class="textarea" rows="5" placeholder="답변을 입력하세요"></textarea>
          <div class="actions">
            <button class="btn" @click="sendReply">답변 등록</button>
          </div>
        </div>
      </div>
  
      <div class="right card emptybox" v-else>
        좌측에서 버그 신고 티켓을 선택하세요
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import { listTickets, getTicket, replyTicket } from '@/api/adminSupport'
  
  // 상태/검색
  const status = ref('')
  // q는 항상 '버그'가 포함되도록 하되, 관리자가 넣는 추가 키워드는 qExtra로 받는다.
  const qExtra = ref('')
  
  // 데이터
  const rows = ref([])
  const selectedId = ref(null)
  const detail = ref(null)
  const reply = ref('')
  
  function fmt(iso){ if(!iso) return ''; return String(iso).replace('T',' ').substring(0,16) }
  
  async function refresh(){
    // 서버가 title LIKE 검색을 지원한다는 전제(기존 adminSupport와 동일)
    // '버그' 강제 + 추가 키워드(있으면)를 공백으로 연결
    const q = ['버그', qExtra.value?.trim()].filter(Boolean).join(' ')
    const params = { status: status.value || undefined, q: q || undefined, page:0, size:50 }
    const { list } = await listTickets(params)
    rows.value = list
    if (selectedId.value) await open(selectedId.value, true)
  }
  onMounted(refresh)
  
  async function open(id, keep=false){
    selectedId.value = id
    detail.value = await getTicket(id)
    if (!keep) reply.value = ''
  }
  async function sendReply(){
    if (!reply.value.trim()) return alert('내용을 입력하세요')
    await replyTicket(selectedId.value, reply.value.trim())
    reply.value = ''
    await open(selectedId.value, true)
  }
  </script>
  
  <style scoped>
  .split{ display:grid; grid-template-columns: 360px 1fr; gap:12px; }
  .card{ background:#fff; border:1px solid #e8ecf6; border-radius:12px; padding:10px; min-height:480px }
  .left .toolbar{ display:flex; gap:8px; margin-bottom:8px }
  .select{ height:34px; border:1px solid #e5e7eb; border-radius:8px; padding:0 10px }
  .search{ flex:1; height:34px; border:1px solid #e5e7eb; border-radius:8px; padding:0 10px }
  .btn{ height:34px; padding:0 12px; border-radius:10px; border:1px solid #cfe0ff; background:#f5f9ff; font-weight:700; cursor:pointer }
  .list{ list-style:none; margin:0; padding:0; }
  .list li{ padding:10px; border:1px solid #f1f4fb; border-radius:10px; margin-bottom:8px; cursor:pointer }
  .list li.on{ background:#f8fbff; border-color:#e0eaff }
  .tit{ font-weight:700 }
  .meta{ color:#6b7280; font-size:12px; margin-top:4px }
  
  .right h3{ margin:6px 0 12px; font-size:18px; font-weight:800 }
  .thread{ display:grid; gap:10px; max-height:420px; overflow:auto; padding-right:6px }
  .msg{ border:1px solid #f1f4fb; border-radius:10px; padding:10px }
  .who{ font-weight:700; font-size:12px; color:#374151 }
  .body{ margin:6px 0; white-space:pre-wrap }
  .time{ color:#6b7280; font-size:12px }
  .reply{ margin-top:10px }
  .textarea{ width:100%; border:1px solid #e5e7eb; border-radius:10px; padding:10px 12px; font-family:inherit }
  .actions{ display:flex; justify-content:flex-end; margin-top:8px }
  .emptybox{ display:flex; align-items:center; justify-content:center; color:#94a3b8; font-weight:700 }
  </style>
  
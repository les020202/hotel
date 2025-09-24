<template>
  <div class="card">
    <div class="toolbar">
      <button class="btn" @click="openCreate">+ 새 공지</button>
      <label class="pin">
        <input type="checkbox" v-model="onlyPinned" @change="refresh" />
        상단고정만
      </label>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th style="width:80px">#</th>
          <th>제목</th>
          <th style="width:120px">상단고정</th>
          <th style="width:160px">작성일</th>
          <th style="width:200px">액션</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="n in rows" :key="n.id">
          <td>{{ n.id }}</td>
          <td class="b">{{ n.title }}</td>
          <td>{{ n.pinned ? '예' : '아니오' }}</td>
          <td>{{ fmt(n.createdAt) }}</td>
          <td>
            <button class="btn xs" @click="openEdit(n)">수정</button>
            <button class="btn xs danger" @click="del(n)">삭제</button>
          </td>
        </tr>
        <tr v-if="!loading && rows.length===0">
          <td colspan="5" class="empty">공지 없음</td>
        </tr>
      </tbody>
    </table>

    <div v-if="loading" class="loading">불러오는 중…</div>
  </div>

  <!-- create/edit modal -->
  <div v-if="modalOpen" class="modal-backdrop" @click.self="close">
    <div class="modal">
      <h3>{{ editId ? '공지 수정' : '새 공지' }}</h3>
      <div class="form">
        <label>제목 <input v-model="form.title" class="input" /></label>
        <label>상단 고정
          <select v-model="form.pinned" class="input">
            <option :value="false">아니오</option>
            <option :value="true">예</option>
          </select>
        </label>
        <label>내용
          <textarea v-model="form.content" class="textarea" rows="8"></textarea>
        </label>
      </div>
      <div class="modal-actions">
        <button class="btn" @click="save">저장</button>
        <button class="btn ghost" @click="close">취소</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listNotices, createNotice, updateNotice, deleteNotice } from '@/api/adminSupport'

const rows = ref([])
const loading = ref(false)
const onlyPinned = ref(false)

const modalOpen = ref(false)
const editId = ref(null)
const form = reactive({ title:'', content:'', pinned:false })

function fmt(iso){ if(!iso) return ''; return String(iso).replace('T',' ').substring(0,16) }

async function refresh(){
  loading.value = true
  try{
    const { list } = await listNotices({ page: 0, size: 100, pinned: onlyPinned.value })
    rows.value = list
  }finally{
    loading.value = false
  }
}
onMounted(refresh)

function openCreate(){
  editId.value = null
  Object.assign(form,{title:'',content:'',pinned:false})
  modalOpen.value=true
}
function openEdit(n){
  editId.value = n.id
  Object.assign(form,{ title:n.title, content:n.content, pinned:!!n.pinned })
  modalOpen.value = true
}
function close(){ modalOpen.value=false }

async function save(){
  const payload = { title:form.title, content:form.content, pinned:!!form.pinned }
  if (editId.value) await updateNotice(editId.value, payload)
  else await createNotice(payload)
  modalOpen.value=false
  await refresh()
}
async function del(n){
  if(!confirm('삭제할까요?')) return
  await deleteNotice(n.id)
  await refresh()
}
</script>

<style scoped>
.card{ background:#fff; border:1px solid #e8ecf6; border-radius:12px; padding:10px }
.toolbar{ display:flex; gap:8px; align-items:center; margin-bottom:8px }
.pin{ font-size:12px; color:#555; display:flex; align-items:center; gap:6px }
.table{ width:100%; border-collapse:collapse }
th,td{ padding:10px; border-bottom:1px solid #f1f4fb; text-align:left; font-size:14px }
.b{ font-weight:700 }
.empty{ text-align:center; color:#94a3b8; padding:14px 0 }
.loading{ padding:14px; text-align:center }

.btn{ height:30px; padding:0 10px; border-radius:10px; border:1px solid #cfe0ff; background:#f5f9ff; font-weight:700; cursor:pointer; }
.btn.xs{ height:28px; font-size:12px }
.btn.ghost{ background:#fff }
.btn.danger{ background:#fff5f5; border-color:#fecaca }

.modal-backdrop{ position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:50 }
.modal{ width:min(680px,92vw); background:#fff; border-radius:12px; padding:18px; border:1px solid #e8ecf6 }
.modal h3{ margin:0 0 10px; font-size:18px; font-weight:800 }
.form{ display:grid; gap:10px; margin:10px 0 }
.input{ width:100%; height:40px; border:1px solid #e5e7eb; border-radius:10px; padding:0 12px }
.textarea{ width:100%; border:1px solid #e5e7eb; border-radius:10px; padding:10px 12px; font-family:inherit }
.modal-actions{ display:flex; justify-content:flex-end; gap:8px }
</style>

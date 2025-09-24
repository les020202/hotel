<template>
  <div class="card">
    <div class="toolbar">
      <select v-model="category" class="select" @change="refresh">
        <option value="">전체</option>
        <option v-for="c in cats" :key="c" :value="c">{{ c }}</option>
      </select>
      <button class="btn" @click="openCreate">+ 새 FAQ</button>
    </div>

    <table class="table">
      <thead>
        <tr><th style="width:80px">#</th><th>카테고리</th><th>질문</th><th style="width:200px">액션</th></tr>
      </thead>
      <tbody>
        <tr v-for="f in rows" :key="f.id">
          <td>{{ f.id }}</td>
          <td class="b">{{ f.category }}</td>
          <td class="ellipsis">{{ f.question }}</td>
          <td>
            <button class="btn xs" @click="openEdit(f)">수정</button>
            <button class="btn xs danger" @click="del(f)">삭제</button>
          </td>
        </tr>
        <tr v-if="!loading && rows.length===0"><td colspan="4" class="empty">FAQ 없음</td></tr>
      </tbody>
    </table>

    <div v-if="loading" class="loading">불러오는 중…</div>
  </div>

  <!-- modal -->
  <div v-if="modalOpen" class="modal-backdrop" @click.self="close">
    <div class="modal">
      <h3>{{ editId ? 'FAQ 수정' : '새 FAQ' }}</h3>
      <div class="form">
        <label>카테고리
          <select v-model="form.category" class="input">
            <option v-for="c in cats" :key="c" :value="c">{{ c }}</option>
          </select>
        </label>
        <label>질문 <input v-model="form.question" class="input" /></label>
        <label>답변
          <textarea v-model="form.answer" class="textarea" rows="8"></textarea>
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
import { listFaqs, createFaq, updateFaq, deleteFaq } from '@/api/adminSupport'

const cats = ['숙소','쿠폰/포인트/코인','결제/영수증','회원']
const category = ref('')
const rows = ref([])
const loading = ref(false)

const modalOpen = ref(false)
const editId = ref(null)
const form = reactive({ category: cats[0], question:'', answer:'' })

async function refresh(){
  loading.value = true
  try{
    const params = { category: category.value || undefined, page:0, size:200 }
    const { list } = await listFaqs(params)
    rows.value = list
  }finally{
    loading.value = false
  }
}
onMounted(refresh)

function openCreate(){
  editId.value = null
  Object.assign(form,{category:cats[0],question:'',answer:''})
  modalOpen.value=true
}
function openEdit(f){
  editId.value = f.id
  Object.assign(form,{ category:f.category, question:f.question, answer:f.answer })
  modalOpen.value = true
}
function close(){ modalOpen.value=false }

async function save(){
  const payload = { category: form.category, question: form.question, answer: form.answer }
  if (editId.value) await updateFaq(editId.value, payload)
  else await createFaq(payload)
  modalOpen.value=false
  await refresh()
}
async function del(f){
  if(!confirm('삭제할까요?')) return
  await deleteFaq(f.id)
  await refresh()
}
</script>

<style scoped>
.card{ background:#fff; border:1px solid #e8ecf6; border-radius:12px; padding:10px }
.toolbar{ display:flex; gap:8px; align-items:center; margin-bottom:8px }
.select{ height:34px; border:1px solid #e5e7eb; border-radius:8px; padding:0 10px }
.table{ width:100%; border-collapse:collapse }
th,td{ padding:10px; border-bottom:1px solid #f1f4fb; text-align:left; font-size:14px }
.b{ font-weight:700 }
.ellipsis{ white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:600px }
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

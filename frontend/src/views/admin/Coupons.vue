<!-- src/views/admin/CouponManage.vue -->
<template>
  <section class="wrap wrap--wide">
    <!-- 헤더 -->
    <div class="hero">
      <div>
        <h2>쿠폰 / 프로모션 관리</h2>
        <p>정액(원) 차감 쿠폰만 지원합니다.</p>
      </div>
      <button class="btn" @click="openCreate">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        쿠폰 등록
      </button>
    </div>

    <!-- 검색 -->
    <div class="toolbar">
      <input
        v-model.trim="q"
        class="search"
        placeholder="코드·제목 검색"
        @keyup.enter="load"
      />
      <div class="pills">
        <button class="pill ghost" @click="load">검색</button>
      </div>
    </div>

    <!-- 표 -->
    <div class="card table-card">
      <table class="table">
        <thead>
          <tr>
            <th style="width:60px">#</th>
            <th style="min-width:140px">코드</th>
            <th>제목</th>
            <th class="right" style="width:140px">차감 금액</th>
            <th style="width:120px">중복 사용</th>
            <th style="min-width:220px">유효기간</th>
            <th style="min-width:180px">생성일</th>
            <th style="width:180px" class="center">액션</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="c in filtered" :key="c.id">
            <td class="muted">#{{ c.id }}</td>

            <td>
              <button class="code-badge" @click="copyCode(c.code)" :title="`클릭하여 복사: ${c.code}`">
                <span class="code-text">{{ c.code }}</span>
                <svg class="copy-ico" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M16 3H8a2 2 0 0 0-2 2v0m0 0H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h8m-5-14h8a2 2 0 0 1 2 2v10M9 21h8a2 2 0 0 0 2-2V9" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                </svg>
              </button>
            </td>

            <td class="ellipsis">{{ c.title }}</td>
            <td class="right strong">₩{{ Number(c.amount||0).toLocaleString() }}</td>

            <td>
              <span class="badge" :data-variant="c.stackable ? 'ok' : 'no'">
                {{ c.stackable ? '가능' : '불가' }}
              </span>
            </td>

            <td class="muted">
              {{ c.validFrom && c.validTo ? (c.validFrom + ' ~ ' + c.validTo) : '상시' }}
            </td>

            <td class="muted">{{ c.createdAt || '-' }}</td>

            <td class="center actions-col">
              <button class="btn ghost xs" @click="openEdit(c)">수정</button>
              <button class="btn danger xs" @click="remove(c)">삭제</button>
            </td>
          </tr>

          <tr v-if="!loading && !filtered.length">
            <td colspan="8" class="empty">등록된 쿠폰이 없습니다.</td>
          </tr>
        </tbody>
      </table>

      <div v-if="loading" class="loading">불러오는 중…</div>
    </div>

    <!-- 등록/수정 모달 -->
    <dialog v-if="show" open class="modal" @click.self="closeModal">
      <div class="modal-body">
        <div class="modal-head">
          <strong>{{ editingId ? '쿠폰 수정' : '쿠폰 등록' }}</strong>
          <button class="icon" @click="closeModal" aria-label="닫기">×</button>
        </div>

        <form class="form" @submit.prevent="save">
          <label>
            <span>쿠폰 코드</span>
            <input v-model.trim="form.code" placeholder="예) NEW10K" required />
          </label>

          <label>
            <span>쿠폰 제목</span>
            <input v-model.trim="form.title" placeholder="예) 신규 가입 1만원 쿠폰" required />
          </label>

          <label>
            <span>차감 금액(원)</span>
            <div class="input-amount">
              <input type="number" min="1" step="1" v-model.number="form.amount" placeholder="예) 10000" required />
              <div class="suffix">원</div>
            </div>
            <small class="hint">정액 차감 쿠폰만 지원합니다.</small>
          </label>

          <label class="inline">
            <input type="checkbox" v-model="form.stackable">
            <span>다른 쿠폰과 중복 사용 허용</span>
          </label>

          <div class="grid-2">
            <label>
              <span>유효 시작일</span>
              <input type="date" v-model="form.validFrom" :min="today">
            </label>
            <label>
              <span>유효 종료일</span>
              <input type="date" v-model="form.validTo" :min="form.validFrom || today">
            </label>
          </div>

          <div class="form-row right">
            <button type="button" class="btn ghost" @click="closeModal">취소</button>
            <button type="submit" class="btn primary" :disabled="submitting">
              <span v-if="!submitting">저장</span>
              <span v-else class="spinner" aria-label="진행중"></span>
            </button>
          </div>

          <p v-if="err" class="err">{{ err }}</p>
        </form>
      </div>
    </dialog>

    <!-- 토스트 -->
    <div v-if="toast" class="toast">{{ toast }}</div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post, put, del } from '@/api/_http'

const today = new Date().toISOString().slice(0,10)
const loading = ref(true)
const rows = ref([])
const q = ref('')
const show = ref(false)
const submitting = ref(false)
const err = ref('')
const toast = ref('')

const editingId = ref(null)
const form = ref({
  code: '',
  title: '',
  amount: null,
  stackable: false,
  validFrom: '',
  validTo: ''
})

function mapCoupon(c) {
  return {
    id: c.id,
    code: c.code,
    title: c.title,
    amount: Number(c.amount || 0),
    stackable: !!(c.stackable ?? c.isStackable),
    validFrom: c.validFrom || c.valid_from || '',
    validTo: c.validTo || c.valid_to || '',
    createdAt: c.createdAt || c.created_at || ''
  }
}

function validate() {
  if (!form.value.code || !form.value.title) return '코드와 제목을 입력하세요.'
  if (!form.value.amount || form.value.amount < 1) return '차감 금액은 1원 이상이어야 합니다.'
  if (form.value.validTo && form.value.validTo < today) return '유효 종료일은 오늘(포함) 이후여야 합니다.'
  if (form.value.validFrom && form.value.validTo && form.value.validFrom > form.value.validTo) {
    return '유효 종료일은 시작일 이후여야 합니다.'
  }
  return ''
}

async function load() {
  loading.value = true
  try {
    const res = await get('/admin/coupons')
    rows.value = (Array.isArray(res) ? res : []).map(mapCoupon)
  } catch (e) {
    console.error(e)
    rows.value = []
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  if (!q.value) return rows.value
  const k = q.value.toLowerCase()
  return rows.value.filter(c =>
    String(c.code).toLowerCase().includes(k) ||
    String(c.title).toLowerCase().includes(k)
  )
})

function openCreate() {
  err.value = ''
  editingId.value = null
  form.value = { code:'', title:'', amount:null, stackable:false, validFrom:'', validTo:'' }
  show.value = true
}
function openEdit(c) {
  err.value = ''
  editingId.value = c.id
  form.value = {
    code: c.code,
    title: c.title,
    amount: c.amount,
    stackable: c.stackable,
    validFrom: c.validFrom || '',
    validTo: c.validTo || ''
  }
  show.value = true
}
function closeModal() { show.value = false }

async function save() {
  err.value = validate()
  if (err.value) return

  submitting.value = true
  try {
    const body = {
      code: form.value.code,
      title: form.value.title,
      amount: Number(form.value.amount),
      stackable: !!form.value.stackable,
      validFrom: form.value.validFrom || null,
      validTo: form.value.validTo || null
    }

    if (editingId.value) {
      const updated = await put(`/admin/coupons/${editingId.value}`, body)
      const mapped = mapCoupon(updated)
      const idx = rows.value.findIndex(r => r.id === editingId.value)
      if (idx >= 0) rows.value[idx] = mapped
      showToast('수정되었습니다.')
    } else {
      const created = await post('/admin/coupons', body)
      rows.value.unshift(mapCoupon(created))
      showToast('등록되었습니다.')
    }
    closeModal()
  } catch (e) {
    console.error(e)
    err.value = e?.message || '요청에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

async function remove(c) {
  if (!confirm(`쿠폰을 삭제할까요?\n[${c.code}] ${c.title}`)) return
  try {
    await del(`/admin/coupons/${c.id}`)
    rows.value = rows.value.filter(r => r.id !== c.id)
    showToast('삭제되었습니다.')
  } catch (e) {
    console.error(e)
    showToast('삭제 실패: ' + (e?.message || '서버 오류'))
  }
}

function copyCode(code) {
  navigator.clipboard?.writeText(String(code)).then(() => {
    showToast('코드가 복사되었습니다.')
  })
}

function showToast(msg) {
  toast.value = msg
  setTimeout(() => (toast.value = ''), 1800)
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
.hero p{ margin:4px 0 0; color:#6b7280; font-size:12px }

/* 툴바 */
.toolbar{ display:flex; gap:10px; align-items:center; margin-bottom:10px; flex-wrap:wrap }
.search{ flex:1 1 360px; height:40px; border-radius:12px; border:1px solid var(--line); padding:0 14px; outline:none }
.search:focus{ box-shadow:0 0 0 3px rgba(37,99,235,.1); border-color:#cfe0ff }
.pills{ display:flex; gap:8px; flex-wrap:wrap }
.pill{ display:flex; align-items:center; gap:8px; height:40px; padding:0 12px; border:1px solid var(--line); background:#fff; border-radius:20px; font-weight:700 }
.pill.ghost{ background:#f7faff } .pill.ghost:hover{ background:#eef5ff }

/* 표 카드 */
.card{ background:#fff; border:1px solid var(--line); border-radius:12px }
.table-card{ overflow:auto }
.table{ width:100%; border-collapse:collapse }
th,td{ padding:12px 12px; border-bottom:1px solid #f1f4fb; text-align:left; font-size:14px }
th{ color:#475569; font-weight:800; background:#fbfdff }
.right{ text-align:right } .center{ text-align:center }
.strong{ font-weight:800 }
.muted{ color:#6b7280 }
.ellipsis{ white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:520px }
.empty{ text-align:center; color:#94a3b8; padding:18px 0 }
.loading{ padding:14px; text-align:center }

/* 코드 뱃지 */
.code-badge{
  display:inline-flex; align-items:center; gap:8px;
  padding:6px 12px; border-radius:12px; border:1px solid #101826;
  background: linear-gradient(180deg, #0b1220, #0c1322); color:#e8f0ff;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.04), 0 6px 18px rgba(2,6,23,.22);
  cursor:pointer;
}
.code-text{
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-weight:800; letter-spacing:.2px; font-size:13px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;
}
.copy-ico{ width:16px; height:16px; opacity:.65; transition:opacity .15s ease }
.code-badge:hover .copy-ico{ opacity:1 }

/* 배지 */
.badge{ display:inline-block; padding:3px 10px; border-radius:999px; font-weight:800; font-size:12px }
.badge[data-variant="ok"]{ background:#0f172a; color:#fff }
.badge[data-variant="no"]{ background:#eef2ff; color:#475569 }

/* 모달 */
.modal{
  position: fixed; inset: 0; margin: auto;
  max-width: 560px; width: 92%;
  border: 0; border-radius: 18px; padding: 0;
  background:#fff; box-shadow:0 20px 50px rgba(15,23,42,.25);
}
.modal::backdrop{ background: rgba(0,0,0,.35); -webkit-backdrop-filter: saturate(120%) blur(2px); backdrop-filter: saturate(120%) blur(2px) }
.modal-body{ padding:16px }
.modal-head{ display:flex; align-items:center; justify-content:space-between; padding-bottom:8px; border-bottom:1px solid #eef3fb; margin-bottom:10px }
.modal .icon{ background:transparent; border:0; font-size:22px; cursor:pointer; color:#6b7280 }

/* 폼 */
.form{ display:grid; gap:12px }
.form label{ display:grid; gap:6px; font-size:13px; color:#334155 }
.form input[type="text"], .form input[type="number"], .form input[type="date"]{
  height:40px; padding:8px 12px; border-radius:12px; border:1px solid #dfe7f6; outline:none; font-size:14px; background:#fff; color:#0f172a; transition:border-color .18s ease, box-shadow .18s ease
}
.form input:focus{ border-color:#3b82f6; box-shadow:0 0 0 3px rgba(59,130,246,.18) }
.form .inline{ display:flex; align-items:center; gap:10px; margin-top:2px }
.grid-2{ display:grid; grid-template-columns:1fr 1fr; gap:10px }
@media (max-width:640px){ .grid-2{ grid-template-columns:1fr } }
.input-amount{ position:relative }
.input-amount .suffix{ position:absolute; top:50%; right:10px; transform:translateY(-50%); color:#6b7280; font-size:13px; pointer-events:none }
.form-row.right{ margin-top:4px; display:flex; justify-content:flex-end; gap:8px }
.spinner{ display:inline-block; width:18px; height:18px; border:2px solid rgba(255,255,255,.6); border-top-color:#fff; border-radius:50%; animation:spin 1s linear infinite }
.hint{ color:#64748b; font-size:12px }
.err{ color:#e11d48; font-size:13px; margin-top:4px }

/* 버튼 */
.btn{
  display:inline-flex; align-items:center; gap:8px;
  height:34px; padding:0 12px; border-radius:10px;
  border:1px solid #cfe0ff; background:#f5f9ff; font-weight:800; cursor:pointer
}
.btn svg{ width:18px; height:18px }
.btn.primary{ color:#fff; background:linear-gradient(135deg,#3b82f6,#2563eb); box-shadow:0 8px 20px rgba(37,99,235,.25) }
.btn.ghost{ background:#fff; color:#0f172a; border:1px solid #e1e8f5 }
.btn.danger{ color:#b91c1c; background:#fff5f5; border-color:#fecaca }
.btn.danger:hover{ background:#ffe9e9 }
.btn.xs{ height:28px; font-size:12px; border-radius:10px }

/* 액션/토스트 */
.actions-col{ display:flex; align-items:center; justify-content:center; gap:6px }
.toast{
  position:fixed; right:18px; bottom:18px; padding:10px 14px; border-radius:12px;
  color:#0f172a; background:#fff; border:1px solid #e7edf7; box-shadow:0 10px 26px rgba(15,23,42,.18);
  font-weight:700; z-index:60;
}

/* 가로 스크롤바 톤(한 번만 정의) */
.table-card{ scrollbar-color:#a4b9d8 #eef2ff; scrollbar-width:thin; }
:deep(.table-card)::-webkit-scrollbar{ height:10px; }
:deep(.table-card)::-webkit-scrollbar-track{ background:#eef2ff; border-radius:8px; }
:deep(.table-card)::-webkit-scrollbar-thumb{ background:#94a3b8; border-radius:8px; }
:deep(.table-card)::-webkit-scrollbar-thumb:hover{ background:#c9d4e2; }

/* 헤더 세로깨짐 방지(한 줄만) */
:deep(.table thead th),
:deep(.table thead th *){ white-space:nowrap; word-break:keep-all; line-height:1.25; }

/* 풀폭: 이 페이지 한정 */
.wrap.wrap--wide{ width:100% !important; max-width:none !important; margin:0 !important; }
.wrap.wrap--wide .hero,
.wrap.wrap--wide .card,
.wrap.wrap--wide .table-card{ width:100% !important; max-width:none !important; }
.wrap.wrap--wide .table{ width:100% !important; table-layout:auto; min-width:0 !important; }
.wrap.wrap--wide .actions-col{ flex-wrap:nowrap; }
</style>

<template>
  <div class="wrap">
    <header class="topbar">
      <div class="titles">
        <h1>쿠폰 / 프로모션 관리</h1>
        <p class="sub">정액(원) 차감 쿠폰만 지원합니다.</p>
      </div>
      <div class="actions">
        <div class="search">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M21 21l-3.8-3.8M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15Z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
          <input v-model.trim="q" placeholder="코드·제목 검색" />
        </div>
        <button class="btn primary" @click="openCreate">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          쿠폰 등록
        </button>
      </div>
    </header>

    <section class="card table">
      <div class="table-head">
        <span>#</span>
        <span>코드</span>
        <span>제목</span>
        <span class="right amount">차감 금액</span>
        <span>중복 사용</span>
        <span>유효기간</span>
        <span>생성일</span>
        <span class="center">액션</span>
      </div>

      <div v-if="loading" class="skeleton-wrap">
        <div class="skeleton-row" v-for="i in 5" :key="i"></div>
      </div>

      <template v-else>
        <div v-for="c in filtered" :key="c.id" class="table-row">
          <span class="muted">#{{ c.id }}</span>

          <!-- 코드 뱃지 (복사 가능) -->
          <button class="code-badge" @click="copyCode(c.code)" :title="`클릭하여 복사: ${c.code}`">
            <span class="code-text">{{ c.code }}</span>
            <svg class="copy-ico" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M16 3H8a2 2 0 0 0-2 2v0m0 0H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h8m-5-14h8a2 2 0 0 1 2 2v10M9 21h8a2 2 0 0 0 2-2V9" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
            </svg>
          </button>

          <span class="ellipsis">{{ c.title }}</span>
          <span class="right strong">{{ fmt(c.amount) }}</span>

          <span>
            <span :class="['badge', c.stackable ? 'ok' : 'no']">
              {{ c.stackable ? '가능' : '불가' }}
            </span>
          </span>

          <span class="muted">
            {{ c.validFrom && c.validTo ? (c.validFrom + ' ~ ' + c.validTo) : '상시' }}
          </span>

          <span class="muted">{{ c.createdAt || '-' }}</span>

          <span class="center actions-col">
            <button class="btn ghost sm" @click="openEdit(c)">수정</button>
            <button class="btn danger sm" @click="remove(c)">삭제</button>
          </span>
        </div>

        <div v-if="!filtered.length" class="empty">
          등록된 쿠폰이 없습니다.
        </div>
      </template>
    </section>

    <!-- 등록/수정 모달 -->
    <div v-if="show" class="backdrop" @click.self="closeModal">
      <div class="modal">
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
    <!-- 오늘 이전 선택 불가 -->
    <input type="date" v-model="form.validFrom" :min="today">
  </label>
  <label>
    <span>유효 종료일</span>
    <!-- 시작일이 있으면 그 날부터, 없으면 오늘부터 선택 -->
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
    </div>

    <!-- 토스트 -->
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post, put, del } from '@/api/_http'  // /admin/... 경로 사용

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
  // 종료일은 오늘 이전 금지
  if (form.value.validTo && form.value.validTo < today) {
    return '유효 종료일은 오늘(포함) 이후여야 합니다.'
  }
  // 시작일 > 종료일 금지
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

function fmt(n) { return `${Number(n || 0).toLocaleString()}원` }

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
/* ---------- 레이아웃 & 상단바 ---------- */
.wrap{
  padding:20px 22px 40px;
  color:#0f172a;
  font-family: ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial;
  min-width: 0;                 /* ★ 사이드바 밀림 방지 */
}
.topbar{
  display:flex; align-items:flex-end; justify-content:space-between; gap:16px;
  margin-bottom:16px; padding:18px 18px 16px; border:1px solid #e7edf7; border-radius:16px;
  background: radial-gradient(800px 260px at 8% 0%, #eef6ff 0%, transparent 60%), linear-gradient(180deg,#fff,#f9fbff);
  box-shadow: 0 10px 24px rgba(15,23,42,.05);
}
.titles h1{ margin:0; font-size:20px; font-weight:800; letter-spacing:.2px }
.titles .sub{ margin:4px 0 0; color:#6b7280; font-size:12px }
.actions{ display:flex; align-items:center; gap:10px }
.search{
  display:flex; align-items:center; gap:8px; padding:8px 10px; width:240px;
  border:1px solid #e1e8f5; border-radius:12px; background:#fff; color:#6b7280;
}
.search svg{ width:18px; height:18px }
.search input{ border:0; outline:none; flex:1; font-size:14px; background:transparent; color:#0f172a }

/* ---------- 버튼 ---------- */
.btn{
  display:inline-flex; align-items:center; gap:8px; padding:10px 14px; border-radius:12px; border:0; cursor:pointer;
  font-weight:800; font-size:14px; transition:transform .06s ease, box-shadow .12s ease;
}
.btn svg{ width:18px; height:18px }
.btn.primary{ color:#fff; background:linear-gradient(135deg,#3b82f6,#2563eb); box-shadow:0 8px 20px rgba(37,99,235,.25) }
.btn.primary:hover{ background:linear-gradient(135deg,#2563eb,#1d4ed8) }
.btn.ghost{ background:#fff; color:#0f172a; border:1px solid #e1e8f5 }
.btn.danger{ color:#fff; background:#ef4444 }
.btn.sm{ padding:6px 10px; font-size:12px; border-radius:10px }

/* ---------- 테이블 카드 ---------- */
.card{ border:1px solid #e7edf7; border-radius:16px; background:#fff; box-shadow: 0 12px 28px rgba(15,23,42,.05) }
.table{ padding:8px; overflow-x:auto; } /* ★ 좁아지면 내부 스크롤 */

/* 8열 그리드(유연) */
.table-head, .table-row{
  display:grid;
  grid-template-columns:
    70px                                   /* # */
    minmax(120px, max-content)             /* 코드(내용 길이만큼) */
    1fr                                     /* 제목 */
    minmax(110px, 140px)                   /* 차감 금액 */
    minmax(88px, 110px)                    /* 중복 사용 */
    minmax(220px, 1fr)                     /* 유효기간 */
    minmax(180px, 1fr)                     /* 생성일 */
    150px;                                 /* 액션 */
  align-items:center;
  column-gap: 22px;                        /* ★ 금액↔중복 사용 간격 확보 */
  padding:12px;
  min-width: 980px;                        /* ★ 더 좁아지면 가로 스크롤 */
}
@media (max-width: 960px){
  .table-head, .table-row{ min-width: 860px; }
}

.table-head{ position:sticky; top:0; background:#f9fbff; z-index:1; border-radius:12px; font-weight:700; color:#475569 }
.table-row{ border-top:1px solid #f0f4fb }
.table-row:hover{ background:#fcfdff }
.right{ text-align:right }
.center{ text-align:center }
.strong{ font-weight:800 }
.muted{ color:#6b7280 }
.ellipsis{ overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.actions-col{ display:flex; align-items:center; justify-content:center; gap:6px }

/* ---------- 코드 뱃지 ---------- */
.code-badge{
  display:inline-flex; align-items:center; gap:8px;
  padding:6px 12px; border-radius:12px; border:1px solid #101826;
  background: linear-gradient(180deg, #0b1220, #0c1322); color:#e8f0ff;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.04), 0 6px 18px rgba(2,6,23,.22);
  cursor:pointer;
  justify-self: start;                      /* 그리드 stretch 해제 */
  width: auto;                              /* 내용 길이만큼 */
  max-width: 100%;
}
.code-text{
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-weight:800; letter-spacing:.2px; font-size:13px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;
}
.copy-ico{ width:16px; height:16px; opacity:.65; transition:opacity .15s ease }
.code-badge:hover .copy-ico{ opacity:1 }

/* ---------- 배지/스켈/빈상태 ---------- */
.badge{ display:inline-block; padding:3px 10px; border-radius:999px; font-weight:800; font-size:12px }
.badge.ok{ background:#0f172a; color:#fff }
.badge.no{ background:#eef2ff; color:#475569 }

.empty{ text-align:center; color:#94a3b8; padding:26px }
.skeleton-wrap{ padding:8px 12px }
.skeleton-row{ height:46px; border-radius:10px; margin:6px 0; background: linear-gradient(90deg, #f3f6fb 25%, #eaf0f9 37%, #f3f6fb 63%); background-size: 400% 100%; animation: shimmer 1.2s infinite }
@keyframes shimmer{ 0%{ background-position: 100% 0 } 100%{ background-position: 0 0 } }

/* ---------- 모달 ---------- */
.backdrop{ position:fixed; inset:0; background:rgba(15,23,42,.35); display:grid; place-items:center; z-index:50 }
.modal{ width:min(560px, 92vw); border-radius:18px; background:#fff; border:1px solid #e7edf7; box-shadow:0 20px 50px rgba(15,23,42,.25) }
.modal-head{ display:flex; align-items:center; justify-content:space-between; padding:14px 16px; border-bottom:1px solid #eef3fb }
.modal .icon{ background:transparent; border:0; font-size:22px; cursor:pointer; color:#6b7280 }
.form{ padding:16px; display:grid; gap:12px }
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
@keyframes spin{ to{ transform:rotate(360deg) } }
.hint{ color:#64748b; font-size:12px }
.err{ color:#e11d48; font-size:13px; margin-top:6px }

/* ---------- 토스트 ---------- */
.toast{
  position:fixed; right:18px; bottom:18px; padding:10px 14px; border-radius:12px;
  color:#0f172a; background:#fff; border:1px solid #e7edf7; box-shadow:0 10px 26px rgba(15,23,42,.18);
  font-weight:700; z-index:60;
}
</style>

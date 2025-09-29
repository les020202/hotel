<!-- src/views/admin/UserManage.vue -->
<template>
  <section class="wrap">
    <!-- 헤더 -->
    <div class="hero">
      <div>
        <h2>유저 관리</h2>
        <p>아이디 / 이메일 / 이름 검색</p>
      </div>
    </div>

    <!-- 검색/필터 바 -->
    <div class="toolbar">
      <input
        v-model.trim="q"
        class="search"
        placeholder="아이디/이메일/이름 입력"
        @keyup.enter="load"
      />
      <div class="pills">
        <div class="pill">
          <span>역할</span>
          <select v-model="role">
            <option value="">전체</option>
            <option value="ROLE_USER">유저</option>
            <option value="ROLE_ADMIN">관리자</option>
            <option value="ROLE_OWNER">호텔업주</option>
          </select>
        </div>
        <div class="pill">
          <span>상태</span>
          <select v-model="status">
            <option value="">전체</option>
            <option value="ACTIVE">활성화</option>
            <option value="LOCKED">잠금</option>
            <option value="INACTIVE">비활성화</option>
          </select>
        </div>
        <button class="pill ghost" @click="load">검색</button>
      </div>
    </div>

    <!-- 표 -->
    <div class="card table-card">
      <table class="table">
        <thead>
          <tr>
            <th style="width:60px">#</th>
            <th>아이디</th>
            <th>이름</th>
            <th>이메일</th>
            <th>전화</th>
            <th>역할</th>
            <th>상태</th>
            <th>가입일</th>
            <th style="width:120px" class="center">액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in rows" :key="u.id">
            <td class="muted">#{{ u.id }}</td>
            <td>{{ u.loginId }}</td>
            <td>{{ u.name }}</td>
            <td>{{ u.email }}</td>
            <td>{{ u.phone || '-' }}</td>
            <td>
              <select
                :value="u.role"
                @change="onChangeRole(u, ($event.target as HTMLSelectElement).value)"
                class="pill-select"
              >
                <option value="ROLE_USER">유저</option>
                <option value="ROLE_ADMIN">관리자</option>
                <option value="ROLE_OWNER">호텔업주</option>
              </select>
            </td>
            <td>
              <span class="badge" :data-variant="u.status">{{ u.status }}</span>
              <select
                :value="u.status"
                @change="onChangeStatus(u, ($event.target as HTMLSelectElement).value)"
              >
                <option value="ACTIVE">활성화</option>
                <option value="LOCKED">잠금</option>
                <option value="INACTIVE">비활성화</option>
              </select>
            </td>
            <td class="muted">{{ u.createdAt }}</td>
            <td class="center">
              <button class="btn danger xs" @click="remove(u)" :disabled="u.status==='DELETED'">
                삭제
              </button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length===0">
            <td colspan="9" class="empty">검색 결과가 없습니다.</td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="loading">불러오는 중…</div>
    </div>

    <!-- 토스트 -->
    <div v-if="toast" class="toast">{{ toast }}</div>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { searchUsers, updateUserRole, updateUserStatus, deleteUser } from '@/api/adminUsers'

const q = ref('')
const role = ref('')
const status = ref('')
const loading = ref(true)
const rows = ref<any[]>([])
const toast = ref('')

let timer:any
watch([q, role, status], () => {
  clearTimeout(timer)
  timer = setTimeout(load, 250)
}, { immediate: true })

async function load() {
  loading.value = true
  try {
    rows.value = await searchUsers({ q: q.value, role: role.value, status: status.value }) || []
  } catch (e) {
    console.error(e)
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function onChangeRole(u:any, newRole:string){
  try{
    await updateUserRole(u.id, newRole)
    u.role = newRole
    toastOnce('권한을 변경했습니다.')
  }catch(e){ console.error(e); toastOnce('권한 변경 실패') }
}
async function onChangeStatus(u:any, newStatus:string){
  try{
    await updateUserStatus(u.id, newStatus)
    u.status = newStatus
    toastOnce('상태를 변경했습니다.')
  }catch(e){ console.error(e); toastOnce('상태 변경 실패') }
}
async function remove(u:any){
  if (!confirm(`정말 삭제하시겠어요?\n${u.loginId} (${u.email})`)) return
  try{
    await deleteUser(u.id)
    rows.value = rows.value.filter(r => r.id !== u.id)
    toastOnce('삭제되었습니다.')
  }catch(e){ console.error(e); toastOnce('삭제 실패') }
}
function toastOnce(msg:string){ toast.value=msg; setTimeout(()=>toast.value='',1500) }
</script>

<style scoped>
:root{ --line:#e8ecf6; --muted:#6b7280; --ink:#111827; }
.wrap{ padding:14px }

/* 헤더 */
.hero{
  display:flex; align-items:center; justify-content:space-between;
  padding:16px 18px; border-radius:16px;
  background:linear-gradient(135deg,#f7faff,#f0f6ff);
  border:1px solid #eaf0ff; margin-bottom:12px;
}
.hero h2{ margin:0; font-size:18px; font-weight:800; color:var(--ink) }
.hero p{ margin:4px 0 0; color:var(--muted); font-size:12px }

/* 툴바 */
.toolbar{ display:flex; gap:10px; align-items:center; margin-bottom:10px; flex-wrap:wrap }
.search{ flex:1 1 360px; height:40px; border-radius:12px; border:1px solid var(--line); padding:0 14px }
.search:focus{ box-shadow:0 0 0 3px rgba(37,99,235,.1); border-color:#cfe0ff }
.pills{ display:flex; gap:8px; flex-wrap:wrap }
.pill{ display:flex; align-items:center; gap:8px; height:40px; padding:0 12px; border:1px solid var(--line); background:#fff; border-radius:20px; font-weight:700 }
.pill select{ border:0; background:transparent; outline:none; font-weight:700 }
.pill.ghost{ background:#f7faff } .pill.ghost:hover{ background:#eef5ff }

/* 테이블 */
.card{ background:#fff; border:1px solid var(--line); border-radius:12px }
.table-card{ overflow:auto }
.table{ width:100%; border-collapse:collapse }
th,td{ padding:12px 12px; border-bottom:1px solid #f1f4fb; text-align:left; font-size:14px }
th{ color:#475569; font-weight:800; background:#fbfdff }
.center{text-align:center}
.muted{ color:var(--muted) }
.empty{ text-align:center; color:#94a3b8; padding:18px 0 }
.loading{ padding:14px; text-align:center }

/* 상태 뱃지 */
.badge{ display:inline-block; margin-right:8px; padding:2px 8px; border-radius:999px; font-size:12px; border:1px solid #e5e7eb; background:#f9fafb }
.badge[data-variant="ACTIVE"]{ background:#ecfdf5; border-color:#a7f3d0 }
.badge[data-variant="LOCKED"]{ background:#fff7ed; border-color:#fed7aa }
.badge[data-variant="INACTIVE"]{ background:#fef2f2; border-color:#fecaca }

/* 버튼 */
.btn{ height:30px; padding:0 10px; border-radius:10px; border:1px solid #cfe0ff; background:#f5f9ff; font-weight:700; cursor:pointer; }
.btn.xs{ height:28px; font-size:12px }
.btn.danger{ background:#fff5f5; border-color:#fecaca }
.btn.danger:hover{ background:#ffe9e9 }

/* 토스트 */
.toast{ position:fixed; right:18px; bottom:18px; padding:10px 14px; border-radius:12px; color:#0f172a; background:#fff; border:1px solid #e7edf7; box-shadow:0 10px 26px rgba(15,23,42,.18); font-weight:700; z-index:60 }
</style>

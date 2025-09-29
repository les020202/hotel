<script setup lang="js">
import { onMounted, reactive, ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/lib/api'

const route = useRoute()
const hotelId = ref(null)

/* ===== UTIL & TODAY ===== */
function toISO(d){ const dt=(d instanceof Date)?d:new Date(d); const m=dt.getMonth()+1; const day=dt.getDate(); return `${dt.getFullYear()}-${String(m).padStart(2,'0')}-${String(day).padStart(2,'0')}` }
function addDays(d,n){ const t=new Date(d); t.setDate(t.getDate()+n); return t }
function startOfWeek(date){ const d=new Date(date); const k=(d.getDay()+6)%7; d.setDate(d.getDate()-k); return d }
function endOfWeek(date){ const d=new Date(date); const k=(d.getDay()+6)%7; d.setDate(d.getDate()+(6-k)); return d }
function isPast(iso){ return new Date(iso) < new Date(todayISO) }
function dowShort(iso){ const d=new Date(iso); return ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'][d.getDay()] }
function isoWeekday(iso){ const d=new Date(iso); return ((d.getDay()+6)%7)+1 } // 1..7 (Mon..Sun)

const todayISO = toISO(new Date())
const maxISO   = toISO(addDays(new Date(), 30))

/* ===== VIEW MODE ===== */
const view = ref('type')
function switchView(v){ view.value = v; v==='overview' ? loadOverview() : load() }

/* ===== FILTERS ===== */
const roomTypeId = ref('')
const roomTypes  = ref([])
const range = reactive({ from: todayISO, to: maxISO })

/* ===== GRID & DATA (type month) ===== */
const grid = reactive({ startISO: range.from, endISO: range.to, weeks: [] })
const days = ref([])

function buildRangeGrid(fromIso,toIso){
  const from=new Date(fromIso), to=new Date(toIso)
  if (to<from){ to.setTime(from.getTime()); to.setDate(to.getDate()+1) }
  const gridStart=startOfWeek(from)
  const gridEnd  =endOfWeek(to)
  const dayCells=[]
  for(let d=new Date(gridStart); d<=gridEnd; d.setDate(d.getDate()+1)){
    const iso=toISO(d)
    dayCells.push({ date: iso, inRange: (new Date(iso)>=new Date(fromIso))&&(new Date(iso)<=new Date(toIso)) })
  }
  const weeks=[]; for(let i=0;i<dayCells.length;i+=7) weeks.push(dayCells.slice(i,i+7))
  const thisMonday = toISO(startOfWeek(new Date()))
  const filteredWeeks = weeks.filter(week => week.some(cell => cell.date >= thisMonday))
  const newStartISO = filteredWeeks.length ? filteredWeeks[0][0].date : thisMonday
  const newEndISO   = filteredWeeks.length ? filteredWeeks.at(-1).at(-1).date : newStartISO
  return { gridStartISO: newStartISO, gridEndISO: newEndISO, weeks: filteredWeeks }
}
function recomputeGrid(){
  const g=buildRangeGrid(range.from, range.to)
  grid.startISO=g.gridStartISO; grid.endISO=g.gridEndISO; grid.weeks=g.weeks
}

async function loadRoomTypes(){
  if (!hotelId.value) return
  const res=await api(`/api/owner/hotels/${hotelId.value}/room-types`)
  if (res.ok){
    roomTypes.value=await res.json()
    if (!roomTypeId.value && roomTypes.value.length) roomTypeId.value=String(roomTypes.value[0].id)
  } else roomTypes.value=[]
}
async function load(){
  if (!hotelId.value || !roomTypeId.value) return
  const q=new URLSearchParams({ roomTypeId: roomTypeId.value, from: grid.startISO, to: grid.endISO }).toString()
  const res=await api(`/api/owner/hotels/${hotelId.value}/inventory?`+q)
  days.value = res.ok ? await res.json() : []
}
function dayInfo(isoDate){ return days.value.find(d=>d.stayDate===isoDate) }

/* 룸타입/기간 변경 시 즉시 재조회(룸타입은 즉시 반영) */
watch(roomTypeId, async ()=>{ recomputeGrid(); await load() })
watch([()=>range.from,()=>range.to], ()=>{ recomputeGrid() })

/* ===== BULK UPDATE ===== */
const bulk=reactive({ weekdays:new Set(), allotment:'', price:'', status:'OPEN' })
function toggleWeekday(i){ bulk.weekdays.has(i)?bulk.weekdays.delete(i):bulk.weekdays.add(i) }
async function applyBulk(){
  if (!hotelId.value || !roomTypeId.value) return
  const effFrom = (new Date(range.from) < new Date(todayISO)) ? todayISO : range.from
  if (new Date(range.to) < new Date(todayISO)) { alert('선택한 기간이 모두 지난 날짜입니다.'); return }
  const body={ roomTypeId:Number(roomTypeId.value), from:effFrom, to:range.to,
    weekdays:Array.from(bulk.weekdays),
    allotment: bulk.allotment===''?null:Number(bulk.allotment),
    price:     bulk.price===''?null:Number(bulk.price),
    status:    bulk.status||null
  }
  const res=await api(`/api/owner/hotels/${hotelId.value}/inventory/bulk`,{method:'PUT', body:JSON.stringify(body)})
  if (res.ok){ await load(); alert('적용되었습니다. (지난 날짜 제외)') } else alert('실패: '+await res.text())
}

/* 요일 하이라이트: 선택 + inRange + 미래 날짜 */
function isMarkedDay(iso, inRange){
  if (!inRange) return false
  if (isPast(iso)) return false
  if (!bulk.weekdays || bulk.weekdays.size === 0) return false
  return bulk.weekdays.has(isoWeekday(iso))
}

/* ===== OVERVIEW (2w) ===== */
const overviewAnchor=ref(startOfWeek(new Date()))
const overviewRange=computed(()=>({ from: toISO(overviewAnchor.value), to: toISO(addDays(overviewAnchor.value,13)) }))
const overviewDays=ref([])
async function loadOverview(){
  if (!hotelId.value) return
  const url=`/api/owner/hotels/${hotelId.value}/inventory/overview?from=${overviewRange.value.from}&to=${overviewRange.value.to}`
  const res=await api(url); overviewDays.value = res.ok ? await res.json() : []
}
function prev2w(){ overviewAnchor.value=addDays(overviewAnchor.value,-14); loadOverview() }
function next2w(){ overviewAnchor.value=addDays(overviewAnchor.value,+14); loadOverview() }
const week1=computed(()=>overviewDays.value.slice(0,7))
const week2=computed(()=>overviewDays.value.slice(7,14))
function itemOf(day, code){ return day.items?.find(x=>x.typeCode===code) || null }
function shortType(code){ return ({STANDARD:'STD', DELUXE:'DLX', SUITE:'STE', PREMIUM:'PRM'})[code] || code }
function roomTypeIdByCode(code){ const rt = roomTypes.value.find(r => r.typeCode === code); return rt ? rt.id : null }

/* ===== REGISTER MODAL ===== */
const showRegister = ref(false)
const reg = reactive({
  roomTypeIds: new Set(),
  from: todayISO,
  to:   toISO(addDays(new Date(), 7)),
  weekdays: new Set([1,2,3,4,5,6,7]),
  allotment: '2',
  price: '88888',
  status: 'OPEN',
  overwrite: true
})
watch([roomTypes, roomTypeId], ()=>{
  if (roomTypeId.value) reg.roomTypeIds = new Set([ Number(roomTypeId.value) ])
})
function toggleRegType(id){ reg.roomTypeIds.has(id)?reg.roomTypeIds.delete(id):reg.roomTypeIds.add(id) }
function toggleRegWeekday(i){ reg.weekdays.has(i)?reg.weekdays.delete(i):reg.weekdays.add(i) }
const regMaxISO = maxISO
function clampRegDates(){
  if (new Date(reg.from) < new Date(todayISO)) reg.from = todayISO
  if (new Date(reg.to)   > new Date(regMaxISO)) reg.to   = regMaxISO
  if (new Date(reg.to)   < new Date(reg.from)) reg.to    = reg.from
}
watch(()=>reg.from, clampRegDates); watch(()=>reg.to, clampRegDates)
const regMatchedDays = computed(()=>{
  const f=new Date(reg.from), t=new Date(reg.to)
  let c=0
  for(let d=new Date(f); d<=t; d.setDate(d.getDate()+1)){
    const dow=(d.getDay()+6)%7+1
    if (reg.weekdays.size===0 || reg.weekdays.has(dow)) c++
  }
  return c
})
async function submitRegister(){
  if (!hotelId.value) return
  if (reg.roomTypeIds.size===0){ alert('등록할 객실타입을 선택하세요.'); return }
  if (new Date(reg.from) < new Date(todayISO) || new Date(reg.to) > new Date(regMaxISO)){
    alert('등록 가능 범위는 오늘부터 30일 이내입니다.'); return
  }
  const body = {
    roomTypeIds: Array.from(reg.roomTypeIds),
    from: reg.from,
    to: reg.to,
    weekdays: Array.from(reg.weekdays),
    allotment: reg.allotment===''?0:Number(reg.allotment),
    price: reg.price===''?0:Number(reg.price),
    status: reg.status || 'OPEN',
    overwrite: !!reg.overwrite
  }
  const res = await api(`/api/owner/hotels/${hotelId.value}/inventory/register`, {
    method: 'POST',
    body: JSON.stringify(body)
  })
  if (res.ok){
    const data = await res.json()
    alert(`등록 완료\n생성/업데이트: ${data.created}\n기존유지: ${data.skippedExisting}\n범위초과: ${data.skippedOutOfRange}`)
    showRegister.value = false
    await (view.value==='overview' ? loadOverview() : load())
  } else {
    alert('등록 실패: ' + await res.text())
  }
}

/* ===== DAY EDIT MODAL (type view & overview 공용) ===== */
const edit = reactive({
  show: false,
  date: '',
  price: '',
  allotment: '',
  status: 'OPEN',
  isNew: false,
  roomTypeId: null,
  roomTypeLabel: '' // "스탠다드(STANDARD)" 같은 표시
})
function openEdit(dateISO, forcedRoomTypeId=null, forcedRoomTypeLabel=''){
  if (isPast(dateISO)) return
  const targetRtId = forcedRoomTypeId ?? (roomTypeId.value ? Number(roomTypeId.value) : null)
  if (!targetRtId){ alert('룸타입을 먼저 선택하세요.'); return }

  edit.show = true
  edit.date = dateISO
  edit.roomTypeId = targetRtId
  edit.roomTypeLabel = forcedRoomTypeLabel || roomTypes.value.find(r=>r.id===targetRtId)?.nameWithCode || buildNameWithCode(targetRtId)

  // 타입별 월력에서 여는 경우: days 배열에서 찾음
  let info = null
  if (!forcedRoomTypeId) {
    info = dayInfo(dateISO)
  }
  // 기본값 세팅
  edit.isNew = !info
  edit.price = info?.price ?? ''
  edit.allotment = info?.allotment ?? (info ? '' : '')
  edit.status = (info?.status === 'CLOSED') ? 'CLOSED' : 'OPEN'
}
function buildNameWithCode(rtId){
  const rt = roomTypes.value.find(r=>r.id===rtId)
  return rt ? `${rt.name} (${rt.typeCode})` : ''
}
async function saveEdit(){
  if (!hotelId.value || !edit.roomTypeId) return
  if (new Date(edit.date) < new Date(todayISO)) { alert('지난 날짜는 수정할 수 없습니다.'); return }
  if (edit.isNew && (edit.allotment === '' || Number(edit.allotment) <= 0)) {
    alert('새 날짜를 만들 때는 총배정(Allotment)이 1 이상이어야 합니다.')
    return
  }
  const body={
    roomTypeId: Number(edit.roomTypeId),
    from: edit.date,
    to:   edit.date,
    weekdays: [],
    allotment: edit.allotment === '' ? null : Number(edit.allotment),
    price:     edit.price     === '' ? null : Number(edit.price),
    status:    edit.status || null
  }
  const res = await api(`/api/owner/hotels/${hotelId.value}/inventory/bulk`, {
    method:'PUT',
    body: JSON.stringify(body)
  })
  if (res.ok){
    edit.show = false
    await (view.value==='overview' ? loadOverview() : load())
  } else {
    alert('수정 실패: ' + await res.text())
  }
}

/* ===== INIT ===== */
onMounted(async ()=>{
  hotelId.value=Number(route.params.hotelId)
  recomputeGrid(); await loadRoomTypes(); await load()
})
watch(()=>route.params.hotelId, async v=>{
  hotelId.value=Number(v); recomputeGrid(); await loadRoomTypes()
  if (view.value==='overview') await loadOverview(); else await load()
})
</script>

<template>
  <div>
    <header class="topbar">
      <div class="title"><h1 class="text-xl font-bold">예약 캘린더</h1></div>
    </header>


    <!-- 탭 / 등록 -->
    <div class="mt-3 flex items-center gap-2">
      <button :class="['px-3 py-1 rounded-xl border', view==='overview' ? 'bg-black text-white border-black' : 'bg-white']" @click="switchView('overview')">개요(2주)</button>
      <button :class="['px-3 py-1 rounded-xl border', view==='type' ? 'bg-black text-white border-black' : 'bg-white']" @click="switchView('type')">타입별(월력)</button>
      <div class="flex-1"></div>
      <button class="px-3 py-1.5 rounded-xl bg-emerald-600 text-white hover:opacity-90" @click="showRegister=true">
        + 등록(booking_day)
      </button>
    </div>

    <!-- 타입별(월력) -->
    <template v-if="view==='type'">
      <!-- 필터 -->
      <div class="mt-4 grid grid-cols-1 md:grid-cols-4 gap-3">
        <div>
          <div class="text-xs text-gray-500">룸타입</div>
          <select class="w-full border rounded-lg p-2" v-model="roomTypeId">
            <option v-for="rt in roomTypes" :key="rt.id" :value="String(rt.id)">
              {{ rt.name }} ({{ rt.typeCode }})
            </option>
          </select>
        </div>
        <div>
          <div class="text-xs text-gray-500">기간 시작</div>
          <input type="date" class="w-full border rounded-lg p-2" v-model="range.from" />
        </div>
        <div>
          <div class="text-xs text-gray-500">기간 종료</div>
          <input type="date" class="w-full border rounded-lg p-2" v-model="range.to" />
        </div>
        <div class="flex items-end">
          <button class="w-full px-4 py-2 rounded-xl bg-black text-white hover:opacity-90" @click="load">조회</button>
        </div>
      </div>

      <div class="mt-2 text-xs text-gray-500">
        표시 범위: {{ grid.startISO }} ~ {{ grid.endISO }} (선택 범위: {{ range.from }} ~ {{ range.to }})
      </div>

      <!-- 캘린더 -->
      <div class="mt-4 bg-white border rounded-2xl overflow-hidden">
        <div class="grid grid-cols-7 text-center text-xs font-medium bg-gray-100 py-2">
          <div>Mon</div><div>Tue</div><div>Wed</div><div>Thu</div><div>Fri</div><div>Sat</div><div>Sun</div>
        </div>

        <div>
          <div v-for="(week, wi) in grid.weeks" :key="wi" class="grid grid-cols-7 border-t">
            <div
              v-for="d in week"
              :key="d.date"
              class="border-r p-2 min-h-[96px] relative cursor-pointer"
              :class="{
                'bg-gray-50 text-gray-400': !d.inRange,
                'opacity-60 pointer-events-none cursor-default': isPast(d.date),
                'ring-2 ring-amber-400 ring-offset-0 bg-amber-50': isMarkedDay(d.date, d.inRange)
              }"
              @click="openEdit(d.date)"
            >
              <div class="flex items-center justify-between">
                <div class="text-xs" :class="d.inRange ? 'text-gray-500' : 'text-gray-400'">{{ d.date.slice(8,10) }}</div>
                <div v-if="isPast(d.date)" class="text-[11px] text-gray-400">지난 날짜</div>
              </div>

              <div v-if="dayInfo(d.date)" class="mt-1 space-y-1">
                <div class="text-sm font-semibold tabular-nums whitespace-nowrap">
                  ₩{{ dayInfo(d.date).price?.toLocaleString?.() ?? dayInfo(d.date).price }}
                </div>
                <div class="text-xs tabular-nums whitespace-nowrap" :class="d.inRange ? 'text-gray-600' : 'text-gray-400'">
                  남은 {{ dayInfo(d.date).remainingQty }} / 총 {{ dayInfo(d.date).allotment }}
                </div>
                <div class="text-[11px] whitespace-nowrap"
                     :class="{
                       'text-green-600': d.inRange && dayInfo(d.date).sellable && !isPast(d.date),
                       'text-red-600':   d.inRange && !dayInfo(d.date).sellable && !isPast(d.date),
                       'text-gray-400': !d.inRange || isPast(d.date)
                     }">
                  {{ dayInfo(d.date).status }} {{ dayInfo(d.date).sellable ? '(판매중)' : '(판매불가)' }}
                </div>
              </div>

              <div v-else class="mt-1 text-xs italic" :class="d.inRange ? 'text-gray-400' : 'text-gray-300'">
                데이터 없음
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 일괄 수정 -->
      <div class="mt-6 p-4 bg-white border rounded-2xl">
        <div class="text-sm font-semibold mb-3">일괄 적용(수정)</div>
        <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
          <div class="col-span-2">
            <div class="text-xs text-gray-500">요일 선택</div>
            <div class="flex flex-wrap gap-2 mt-1">
              <button v-for="(lbl,idx) in ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']" :key="idx"
                      class="px-3 py-1 rounded-full border"
                      :class="{'bg-black text-white': bulk.weekdays.has(idx+1)}"
                      @click="toggleWeekday(idx+1)">{{ lbl }}</button>
              <button class="ml-2 px-3 py-1 rounded-full border" @click="bulk.weekdays.clear()">전체 해제</button>
            </div>
          </div>
          <div>
            <div class="text-xs text-gray-500">Allotment(총배정)</div>
            <input class="w-full border rounded-lg p-2" v-model="bulk.allotment" placeholder="비워두면 변경 없음" />
          </div>
          <div>
            <div class="text-xs text-gray-500">Price(₩)</div>
            <input class="w-full border rounded-lg p-2" v-model="bulk.price" placeholder="비워두면 변경 없음" />
          </div>
          <div>
            <div class="text-xs text-gray-500">Status</div>
            <select class="w-full border rounded-lg p-2" v-model="bulk.status">
              <option value="OPEN">OPEN</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </div>
        </div>
        <div class="mt-4">
          <button class="px-4 py-2 rounded-xl bg-black text-white hover:opacity-90" @click="applyBulk">적용</button>
        </div>
        <div class="text-xs text-gray-500 mt-2">* 지난 날짜/30일 초과는 적용되지 않습니다.</div>
      </div>
    </template>

    <!-- ===== 개요(2주) ===== -->
    <template v-else>
      <div class="mt-4 bg-white border rounded-2xl p-3">
        <div class="flex items-center justify-between mb-2">
          <button class="px-2 py-1 border rounded" @click="prev2w">‹ 이전 2주</button>
          <div class="text-sm font-medium">{{ overviewRange.from }} ~ {{ overviewRange.to }}</div>
          <button class="px-2 py-1 border rounded" @click="next2w">다음 2주 ›</button>
        </div>

        <div class="grid gap-3">
          <!-- Week 1 -->
          <div class="grid grid-cols-7 gap-3">
            <div v-for="d in week1" :key="d.date" class="rounded-2xl border p-3 text-[13px] min-h-[260px]">
              <div class="text-xs text-gray-500 mb-2"><b>{{ dowShort(d.date) }}</b> {{ d.date.slice(8,10) }}</div>

              <div
                v-for="code in ['STANDARD','DELUXE','SUITE','PREMIUM']"
                :key="code"
                class="py-2 px-2 mb-2 border rounded-lg hover:bg-gray-50 cursor-pointer space-y-1"
                @click="openEdit(d.date, roomTypeIdByCode(code), `${shortType(code)} (${code})`)"
              >
                <!-- 1) 타입, 가격 -->
                <div class="flex items-center justify-between leading-tight">
                  <span class="font-semibold">{{ shortType(code) }}</span>
                  <span class="font-semibold tabular-nums whitespace-nowrap">
                    <template v-if="itemOf(d,code)?.price !== undefined && itemOf(d,code)?.price !== null">
                      ₩{{ (itemOf(d,code)?.price).toLocaleString?.() }}
                    </template>
                    <template v-else>—</template>
                  </span>
                </div>

                <!-- 2) 재고 -->
                <div class="text-xs leading-tight tabular-nums text-gray-700">
                  <template v-if="itemOf(d,code)">
                    남은 {{ itemOf(d,code).remaining }} / 총 {{ itemOf(d,code).allotment }}
                  </template>
                  <template v-else>— / —</template>
                </div>

                <!-- 3) 상태 -->
                <div v-if="itemOf(d,code)"
                     class="text-xs leading-tight"
                     :class="itemOf(d,code).status==='OPEN'
                              ? 'text-green-600'
                              : itemOf(d,code).status==='SOLD_OUT'
                                ? 'text-red-600'
                                : 'text-gray-500'">
                  {{ itemOf(d,code).status }}
                </div>
                <div v-else class="text-xs leading-tight text-gray-400">—</div>
              </div>
            </div>
          </div>

          <!-- Week 2 -->
          <div class="grid grid-cols-7 gap-3">
            <div v-for="d in week2" :key="d.date" class="rounded-2xl border p-3 text-[13px] min-h-[260px]">
              <div class="text-xs text-gray-500 mb-2"><b>{{ dowShort(d.date) }}</b> {{ d.date.slice(8,10) }}</div>

              <div
                v-for="code in ['STANDARD','DELUXE','SUITE','PREMIUM']"
                :key="code"
                class="py-2 px-2 mb-2 border rounded-lg hover:bg-gray-50 cursor-pointer space-y-1"
                @click="openEdit(d.date, roomTypeIdByCode(code), `${shortType(code)} (${code})`)"
              >
                <!-- 1) 타입, 가격 -->
                <div class="flex items-center justify-between leading-tight">
                  <span class="font-semibold">{{ shortType(code) }}</span>
                  <span class="font-semibold tabular-nums whitespace-nowrap">
                    <template v-if="itemOf(d,code)?.price !== undefined && itemOf(d,code)?.price !== null">
                      ₩{{ (itemOf(d,code)?.price).toLocaleString?.() }}
                    </template>
                    <template v-else>—</template>
                  </span>
                </div>

                <!-- 2) 재고 -->
                <div class="text-xs leading-tight tabular-nums text-gray-700">
                  <template v-if="itemOf(d,code)">
                    남은 {{ itemOf(d,code).remaining }} / 총 {{ itemOf(d,code).allotment }}
                  </template>
                  <template v-else>— / —</template>
                </div>

                <!-- 3) 상태 -->
                <div v-if="itemOf(d,code)"
                     class="text-xs leading-tight"
                     :class="itemOf(d,code).status==='OPEN'
                              ? 'text-green-600'
                              : itemOf(d,code).status==='SOLD_OUT'
                                ? 'text-red-600'
                                : 'text-gray-500'">
                  {{ itemOf(d,code).status }}
                </div>
                <div v-else class="text-xs leading-tight text-gray-400">—</div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </template>

    <!-- 등록 모달 -->
    <div v-if="showRegister" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white w-full max-w-2xl rounded-2xl p-5">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-lg font-semibold">booking_day 등록</h3>
          <button class="text-gray-500" @click="showRegister=false">✕</button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <div class="text-xs text-gray-500 mb-1">기간</div>
            <div class="flex gap-2">
              <input type="date" class="border rounded-lg p-2 w-full" v-model="reg.from" :min="todayISO" :max="regMaxISO" />
              <input type="date" class="border rounded-lg p-2 w-full" v-model="reg.to" :min="todayISO" :max="regMaxISO" />
            </div>
            <div class="text-xs text-gray-500 mt-1">* 오늘 ~ {{ regMaxISO }} (최대 30일)</div>
          </div>

          <div>
            <div class="text-xs text-gray-500 mb-1">요일</div>
            <div class="flex flex-wrap gap-2">
              <button v-for="(lbl,idx) in ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']" :key="idx"
                      class="px-3 py-1 rounded-full border"
                      :class="{'bg-black text-white': reg.weekdays.has(idx+1)}"
                      @click="toggleRegWeekday(idx+1)">{{ lbl }}</button>
              <button class="px-3 py-1 rounded-full border" @click="reg.weekdays.clear()">전체</button>
            </div>
            <div class="text-xs text-gray-500 mt-1">선택 일수: {{ regMatchedDays }}일</div>
          </div>

          <div>
            <div class="text-xs text-gray-500 mb-1">객실타입</div>
            <div class="grid grid-cols-2 gap-2">
              <label v-for="rt in roomTypes" :key="rt.id" class="flex items-center gap-2 border rounded-lg p-2">
                <input type="checkbox" :checked="reg.roomTypeIds.has(rt.id)" @change="toggleRegType(rt.id)" />
                <span>{{ rt.name }} ({{ rt.typeCode }})</span>
              </label>
            </div>
            <div class="text-xs text-gray-500 mt-1">* 미선택 시 현재 선택된 타입으로 자동.</div>
          </div>

          <div class="grid grid-cols-1 gap-2">
            <div>
              <div class="text-xs text-gray-500 mb-1">Allotment</div>
              <input class="w-full border rounded-lg p-2" v-model="reg.allotment" placeholder="예: 2" />
            </div>
            <div>
              <div class="text-xs text-gray-500 mb-1">Price(₩)</div>
              <input class="w-full border rounded-lg p-2" v-model="reg.price" placeholder="예: 88888" />
            </div>
            <div>
              <div class="text-xs text-gray-500 mb-1">Status</div>
              <select class="w-full border rounded-lg p-2" v-model="reg.status">
                <option value="OPEN">OPEN</option>
                <option value="CLOSED">CLOSED</option>
              </select>
            </div>
            <label class="flex items-center gap-2">
              <input type="checkbox" v-model="reg.overwrite" />
              <span class="text-sm">기존 데이터가 있어도 덮어쓰기</span>
            </label>
          </div>
        </div>

        <div class="flex items-center justify-between mt-5">
          <div class="text-xs text-gray-500">
            * 이미 존재하는 날짜/타입은 <b>덮어쓰기</b> 옵션에 따라 업데이트됩니다.
          </div>
          <div class="flex gap-2">
            <button class="px-4 py-2 rounded-xl border" @click="showRegister=false">취소</button>
            <button class="px-4 py-2 rounded-xl bg-emerald-600 text-white hover:opacity-90" @click="submitRegister">등록</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 일별 편집 모달 -->
    <div v-if="edit.show" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white w-full max-w-md rounded-2xl p-5">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-lg font-semibold">일자 편집 — {{ edit.date }}</h3>
          <button class="text-gray-500" @click="edit.show=false">✕</button>
        </div>

        <div class="mb-2 text-sm text-gray-600">
          편집 대상: <span class="font-medium">{{ edit.roomTypeLabel }}</span>
        </div>

        <div class="grid grid-cols-1 gap-3">
          <div>
            <div class="text-xs text-gray-500 mb-1">Price(₩)</div>
            <input class="w-full border rounded-lg p-2" v-model="edit.price" placeholder="비워두면 변경 없음" />
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">Allotment(총배정)</div>
            <input class="w-full border rounded-lg p-2" v-model="edit.allotment" placeholder="비워두면 변경 없음" />
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">Status</div>
            <select class="w-full border rounded-lg p-2" v-model="edit.status">
              <option value="OPEN">OPEN</option>
              <option value="CLOSED">CLOSED</option>
            </select>
            <div class="text-xs text-gray-500 mt-1">* SOLD_OUT은 재고가 0일 때 자동 표기됩니다.</div>
          </div>
        </div>

        <div class="flex gap-2 justify-end mt-5">
          <button class="px-4 py-2 rounded-xl border" @click="edit.show=false">취소</button>
          <button class="px-4 py-2 rounded-xl bg-black text-white hover:opacity-90" @click="saveEdit">저장</button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.min-h-\[260px\]{ min-height:260px; }
.tabular-nums { font-variant-numeric: tabular-nums; }
</style>

<script setup>
import { ref, reactive, computed, nextTick, watch } from "vue";
import { useRouter } from "vue-router";
import DestinationInput from "@/components/DestinationInput.vue";
import RangeCalendar from "@/components/RangeCalendar.vue";
import GuestsPopover from "@/components/GuestsPopover.vue";

/* 기존 + rooms/autoNavigate 추가(rooms는 경고 방지/하위호환용) */
const props = defineProps({
  q:        { type: String, default: "" },
  region:   { type: String, default: null },
  checkIn:  { type: String, default: null },
  checkOut: { type: String, default: null },
  adults:   { type: Number, default: 2 },
  children: { type: Number, default: 0 },
  rooms:    { type: Number, default: 1 },         // 하위호환: 메인에서 v-model:rooms를 사용
  autoNavigate: { type: Boolean, default: true }, // 검색 버튼 클릭 시 /search로 자동 이동
});

const emit = defineEmits(["submit", "search", "changed"]);
const router = useRouter();

/* ✅ 들어온 텍스트의 단일 진실 */
const incomingQ = computed(() => props.q ?? props.region ?? "");

/* 내부 상태 (Number 캐스팅으로 NaN 방지) */
const q        = ref(incomingQ.value);
const checkIn  = ref(props.checkIn);
const checkOut = ref(props.checkOut);
const adults   = ref(Number(props.adults ?? 2));
const children = ref(Number(props.children ?? 0));
const rooms    = ref(Number(props.rooms ?? 1));

/* prop → state 동기화 (비동기 프리필 대응) */
watch(incomingQ, v => { if (v != null && v !== q.value) q.value = v; });
watch(() => props.checkIn,  v => { if (v !== checkIn.value)  checkIn.value  = v; });
watch(() => props.checkOut, v => { if (v !== checkOut.value) checkOut.value = v; });
watch(() => props.adults,   v => { if (v != null) adults.value   = Number(v); });
watch(() => props.children, v => { if (v != null) children.value = Number(v); });
watch(() => props.rooms,    v => { if (v != null) rooms.value    = Number(v); });

/* 열림 상태는 reactive + 전용 setter */
const open = reactive({ dest:false, dates:false, guests:false });
function setOpen(which /** 'dest'|'dates'|'guests'|null */) {
  open.dest = open.dates = open.guests = false;
  if (which) open[which] = true;
}

/* 진행 단계(옵션) */
const step = ref("dest"); // dest -> dates -> guests -> done

// 유아 2명까지 무료 → 과금 인원 계산
const billableGuests = (ad, ch) => {
  const A = Number(ad) || 0;   // 성인
  const C = Number(ch) || 0;   // 아동(유아)
  return A + Math.max(C - 2, 0);
};

function todayISO(){ return new Date().toISOString().slice(0,10); }
function addDays(dateISO, n) {
  const d = new Date(dateISO);
  d.setDate(d.getDate() + n);
  return d.toISOString().slice(0, 10);
}
function ensureDates(ci, co){
  const today = todayISO();
  const start = ci || today;
  const end   = (co && new Date(co) > new Date(start)) ? co : addDays(start, 1);
  return { start, end };
}

function payload() {
  return {
    q: q.value,
    checkIn: checkIn.value,
    checkOut: checkOut.value,
    adults: Number(adults.value || 0),
    children: Number(children.value || 0),
    rooms: Number(rooms.value || 1),
    // 백엔드가 실제로 사용하는 인원 수
    guests: billableGuests(adults.value, children.value),
  };
}

function focusDest(){
  setOpen('dest');
  step.value = "dest";
}

function onPickDestination(text){
  q.value = text?.trim() ?? "";
  emit("changed", payload());              // 상태만 알림
  step.value = "dates";
  nextTick(() => setOpen('dates'));       // 다음 단계로
}

// 날짜 완료 → 달력 닫고 인원 팝오버 자동 오픈 (검색 X)
function onRangeDone({ start, end }){
  checkIn.value = start;
  checkOut.value = end;
  emit("changed", payload());             // 상태만 알림
  step.value = "guests";
  setTimeout(() => setOpen('guests'), 220);
}

// 인원 팝오버 [적용] → 전부 닫기만 (자동 검색 X)
function onGuestsConfirm(){
  setOpen(null);
  step.value = "done";
  emit("changed", payload());             // 상태만 알림
}

/** 👉 /search 쿼리 구성 (백엔드/프론트 모두 호환) */
function buildQuery() {
  const { start, end } = ensureDates(checkIn.value, checkOut.value);

  // guests는 아동 2명 무료 규칙을 적용한 값
  const guests = billableGuests(adults.value, children.value);

  // region은 검색어 기반으로도 들어올 수 있으므로 최대한 유지
  const region = (props.region ?? q.value ?? '').trim() || null;

  const query = {
    // 프론트 검색 페이지에서도 사용하는 키 유지
    q: q.value || region || '',
    checkIn: start,
    checkOut: end,

    // 표시/유지용
    adults: Number(adults.value || 0),
    children: Number(children.value || 0),
    rooms: Number(rooms.value || 1),

    // 백엔드 시그니처
    region,
    guests, // ★ 핵심
  };

  // 빈 값 정리
  Object.keys(query).forEach(k => {
    if (query[k] === null || query[k] === undefined || query[k] === '') delete query[k];
  });
  return query;
}

// CTA 클릭할 때만 검색 실행
async function onClickSearch(){
  const query = buildQuery();

  // 1) 항상 외부에 알림 (하위호환 위해 두 이벤트 모두 발행)
  const data = { ...payload(), ...query };
  emit("submit", data);
  emit("search", data);

  // 2) autoNavigate가 true 면 컴포넌트 자체가 /search 로 라우팅
  if (props.autoNavigate) {
    await router.push({ name: 'search', query });
  }
}

/* Enter 키로도 검색 */
function onKeydown(e){
  if (e.key === 'Enter') {
    e.preventDefault();
    onClickSearch();
  }
}

const dateLabel = computed(() => {
  if (!checkIn.value || !checkOut.value) return "체크인 · 체크아웃";
  const f = (s) => {
    const d = new Date(s);
    return `${d.getFullYear()}년 ${d.getMonth()+1}월 ${d.getDate()}일`;
  };
  return `${f(checkIn.value)} – ${f(checkOut.value)}`;
});
const peopleLabel = computed(() => `성인 ${adults.value}명 · 아동 ${children.value}명`);
</script>

<template>
  <div class="bar" @keydown="onKeydown">
    <!-- 1) 목적지 / 호텔 -->
    <div class="cell cell--wide" @click="focusDest">
      <DestinationInput
        v-model="q"
        :open="open.dest"
        @close="setOpen(null)"
        @select="onPickDestination"
        placeholder="어디로 떠나시나요?"
      />
    </div>

    <!-- 2) 날짜 -->
    <div class="cell" @click="setOpen('dates')">
      <div class="cell-inner">
        <span class="label">날짜</span>
        <span class="value">{{ dateLabel }}</span>
      </div>
      <RangeCalendar
        :open="open.dates"
        :min-date="todayISO()"
        :months="2"
        :start="checkIn"
        :end="checkOut"
        @close="setOpen(null)"
        @complete="onRangeDone"
      />
    </div>

    <!-- 3) 인원 -->
    <div class="cell" @click="!open.dates && !open.guests && setOpen('guests')">
      <div class="cell-inner">
        <span class="label">인원</span>
        <span class="value">{{ peopleLabel }}</span>
      </div>
      <!-- 버블링 차단 래퍼 -->
      <div @click.stop @mousedown.stop>
        <GuestsPopover
          :open="open.guests"
          v-model:adults="adults"
          v-model:children="children"
          @close="setOpen(null)"
          @confirm="onGuestsConfirm"
          child-hint="어린이는 2명까지 무료입니다."
          child-age-label="0 - 7세"
        />
      </div>
    </div>

    <!-- CTA -->
    <button type="button" class="cta" @click="onClickSearch">검색하기</button>
  </div>
</template>

<style scoped>
.bar{
  display:grid; grid-template-columns: 1.6fr 1fr 1fr auto; gap:12px;
  background:#fff; border:1px solid #e5e7eb; border-radius:16px; padding:10px;
  box-shadow: 0 6px 18px rgba(0,0,0,.06);
}
.cell{ position:relative; }
.cell--wide{ grid-column: span 1; }
.cell-inner{ display:flex; flex-direction:column; gap:2px; padding:10px 12px; border-radius:12px; }
.cell:hover .cell-inner{ background:#f9fafb; }
.label{ font-size:12px; color:#6b7280; }
.value{ font-weight:600; color:#111827; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.cta{
  align-self:stretch; padding:0 20px; border-radius:12px; border:0;
  background:#5b74ff; color:#fff; font-weight:700; min-width:120px;
}
.cta:hover{ filter:brightness(.97); }
</style>

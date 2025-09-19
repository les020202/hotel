<script setup>
import { ref, reactive, computed, nextTick, watch } from "vue";
import DestinationInput from "@/components/DestinationInput.vue";
import RangeCalendar from "@/components/RangeCalendar.vue";
import GuestsPopover from "@/components/GuestsPopover.vue";

/* ✅ q 와 region 둘 다 받도록 (부모가 어느 키로 보내도 OK) */
const props = defineProps({
  q:        { type: String, default: "" },
  region:   { type: String, default: null },
  checkIn:  { type: String, default: null },
  checkOut: { type: String, default: null },
  adults:   { type: Number, default: 2 },
  children: { type: Number, default: 0 },
});

const emit = defineEmits(["submit", "changed"]);

/* ✅ 들어온 텍스트의 단일 진실 */
const incomingQ = computed(() => props.q ?? props.region ?? "");

/* 내부 상태 */
const q        = ref(incomingQ.value);
const checkIn  = ref(props.checkIn);
const checkOut = ref(props.checkOut);
const adults   = ref(props.adults);
const children = ref(props.children);

/* ✅ prop → state 동기화 (비동기 프리필 대응) */
watch(incomingQ, v => { if (v != null && v !== q.value) q.value = v; });
watch(() => props.checkIn,  v => { if (v !== checkIn.value)  checkIn.value  = v; });
watch(() => props.checkOut, v => { if (v !== checkOut.value) checkOut.value = v; });
watch(() => props.adults,   v => { if (v !== adults.value)   adults.value   = v; });
watch(() => props.children, v => { if (v !== children.value) children.value = v; });

/* ✅ 열림 상태는 reactive + 전용 setter로만 갱신 (객체 통째 대입 금지) */
const open = reactive({ dest:false, dates:false, guests:false });
function setOpen(which /** 'dest'|'dates'|'guests'|null */) {
  open.dest = open.dates = open.guests = false;
  if (which) open[which] = true;
}

/* 진행 단계(옵션) */
const step = ref("dest"); // dest -> dates -> guests -> done

function todayISO(){ return new Date().toISOString().slice(0,10); }

function payload(){
  return {
    q: q.value,
    checkIn: checkIn.value,
    checkOut: checkOut.value,
    adults: adults.value,
    children: children.value
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

// CTA 클릭할 때만 검색 실행
function onClickSearch(){
  emit("submit", payload());
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
  <div class="bar">
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
    <button class="cta" @click="onClickSearch">검색하기</button>
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

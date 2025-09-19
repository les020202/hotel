<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from "vue";

const props = defineProps({
  open: { type: Boolean, default: false },
  minDate: { type: String, default: null },  // "YYYY-MM-DD"
  months: { type: Number, default: 2 },      // 동시에 보여줄 달 개수
  start: { type: String, default: null },
  end:   { type: String, default: null },
});
const emit = defineEmits(["close","complete"]);

const start = ref(parse(props.start));
const end   = ref(parse(props.end));
const hover = ref(null);
const baseOffset = ref(0);                   // 기준 달 오프셋(0=today의 달)

/* --- date utils --- */
function parse(s){ if(!s) return null; const d=new Date(s); d.setHours(0,0,0,0); return d; }
function iso(d){ return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,10); }
function addMonths(d,m){ const x=new Date(d); x.setMonth(x.getMonth()+m); x.setDate(1); x.setHours(0,0,0,0); return x; }
function sameDay(a,b){ return !!a && !!b && a.getTime()===b.getTime(); }
function isAfter(a,b){ return !!a && !!b && a.getTime()>b.getTime(); }
function withinExclusive(x,a,b){
  if(!a||!b||!x) return false;
  const lo = a.getTime()<b.getTime()?a:b, hi = a.getTime()<b.getTime()?b:a;
  return x.getTime()>lo.getTime() && x.getTime()<hi.getTime();
}
function lastDayOfMonth(d){ return new Date(d.getFullYear(), d.getMonth()+1, 0); }

const today = (()=>{ const d=new Date(); d.setHours(0,0,0,0); return d; })();
const minDate = computed(()=> props.minDate ? parse(props.minDate) : today);

/* --- 월 단위 셀 생성: 다른 달 날짜는 null로 두어 빈칸 처리 --- */
function monthCells(baseMonthDate){
  const y = baseMonthDate.getFullYear();
  const m = baseMonthDate.getMonth();
  const first = new Date(y, m, 1); first.setHours(0,0,0,0);
  const daysInMonth = new Date(y, m+1, 0).getDate();
  const offset = (first.getDay()+6)%7; // 월=0 기준

  const total = 42; // 6x7
  const cells = new Array(total).fill(null);

  // day 1 위치부터 채우기
  for (let d=1; d<=daysInMonth; d++){
    const idx = offset + (d-1);
    const date = new Date(y, m, d); date.setHours(0,0,0,0);
    cells[idx] = date;
  }
  return cells; // 길이 42, 일부는 null(빈칸)
}

function isDisabled(d){ if (!d) return true;
   return d < minDate.value;}

/* --- 보여줄 달 목록 --- */
const baseMonth = computed(()=>{
  const base = start.value ? new Date(start.value) : new Date(today);
  base.setDate(1); base.setHours(0,0,0,0);
  return addMonths(base, baseOffset.value);
});
const months = computed(()=> Array.from({length: props.months}, (_,i)=> addMonths(baseMonth.value, i)));
const lastVisibleDate = computed(()=> lastDayOfMonth(months.value[months.value.length-1]));

/* --- pick / hover / nav --- */
function pick(date){
  if (!date || isDisabled(date)) return;

  // 새 범위 시작 또는 이전 범위 종료됨
  if (!start.value || (start.value && end.value)) {
    start.value = date; end.value = null; hover.value = null;

    // 마지막 달의 마지막날을 시작일로 찍으면 자동으로 다음 달로
    if (sameDay(date, lastVisibleDate.value)) goNext();
    return;
  }

  // 시작만 선택된 상태에서 더 이른 날짜 클릭 → 시작 재설정
  if (date <= start.value) { start.value = date; end.value = null; hover.value = null; return; }

  // 종료일 확정
  end.value = date;
  const payload = { start: iso(start.value), end: iso(end.value) };
  emit("complete", payload);

  // 0.2s 유지 후 닫기
  setTimeout(()=> emit("close"), 200);
}

function onEnter(date){
  if (!start.value || end.value) return;
  if (!date || isDisabled(date)) return;
  hover.value = isAfter(date, start.value) ? date : null;
}
function onLeave(){ hover.value = null; }

function goPrev(){ baseOffset.value -= props.months; }  // 보이는 월 수만큼 이동
function goNext(){ baseOffset.value += props.months; }

/* --- esc close --- */
function onKey(e){ if (e.key==="Escape") emit("close"); }
onMounted(()=> document.addEventListener("keydown", onKey));
onBeforeUnmount(()=> document.removeEventListener("keydown", onKey));
</script>

<template>
  <transition name="fade">
    <div
      v-if="open"
      class="cal-root"
      @click.self="emit('close')"
      @mousedown.stop
      @click.stop
      @mouseleave="onLeave"
    >
      <div class="cal-panel" @mousedown.stop @click.stop>
        <div class="months">
          <section v-for="(m,i) in months" :key="m.toISOString()">
            <header class="mh">
              <button v-if="i===0" class="nav nav-left"  @click="goPrev"  aria-label="이전">&#10094;</button>
              <span>{{ m.getFullYear() }}년 {{ m.getMonth()+1 }}월</span>
              <button v-if="i===months.length-1" class="nav nav-right" @click="goNext" aria-label="다음">&#10095;</button>
            </header>

            <div class="grid">
              <div class="wd" v-for="w in ['월','화','수','목','금','토','일']" :key="w">{{ w }}</div>

              <template v-for="(d,idx) in monthCells(m)" :key="idx">
                <button
                  v-if="d"
                  class="day"
                  :class="{
                    'day--disabled': isDisabled(d),
                    'day--today': d.getTime()===today.getTime(),

                    // 확정
                    'day--start': start && sameDay(d,start),
                    'day--end': end && sameDay(d,end),
                    'day--in-range': start && end && withinExclusive(d,start,end),

                    // 프리뷰(hover)
                    'day--hover-end': start && !end && hover && sameDay(d, hover),
                    'day--preview-in-range': start && !end && hover && withinExclusive(d, start, hover)
                  }"
                  :disabled="isDisabled(d)"
                  @click="pick(d)"
                  @mouseenter="onEnter(d)"
                >{{ d.getDate() }}</button>

                <!-- 다른 달의 칸은 비워서 '월 분리' 느낌을 줌 -->
                <div v-else class="day day--empty"></div>
              </template>
            </div>
          </section>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
/* fade */
.fade-enter-active,.fade-leave-active{ transition: opacity .12s ease }
.fade-enter-from,.fade-leave-to{ opacity:0 }

/* layout */
.cal-root{
  position:fixed; inset:0; background:rgba(17,24,39,.35);
  display:flex; justify-content:center; align-items:start; padding-top:6rem; z-index:50;
}
.cal-panel{ background:#fff; border-radius:16px; box-shadow:0 24px 60px rgba(0,0,0,.18); padding:16px 18px; }
.months{ display:flex; gap:24px; }
.mh{
  display:flex; align-items:center; justify-content:center; gap:8px;
  font-weight:800; font-size:18px; margin:8px 0 10px;
  position:relative; min-width: 320px;
}
.nav{
  position:absolute; top:50%; transform:translateY(-50%);
  width:34px; height:34px; border:1px solid #e5e7eb; border-radius:9999px;
  background:#fff; cursor:pointer;
}
.nav-left{ left:-6px; }
.nav-right{ right:-6px; }

/* grid */
.grid{
  display:grid; grid-template-columns: repeat(7, 40px); gap:6px; justify-content:center;
}
.wd{ text-align:center; font-size:12px; color:#6b7280; }

/* day cell */
.day{
  width:40px; height:40px; border-radius:8px; border:0; background:#fff; cursor:pointer;
  display:flex; align-items:center; justify-content:center;
}
.day--empty{ background:transparent; cursor:default; }

/* states */
.day--disabled{ color:#c7cad1; cursor:not-allowed }
.day--today{ background: rgba(70,90,255,.08); }

/* 확정된 시작/종료, 사이 구간 */
.day--start, .day--end{
  background: rgba(70,90,255,.22);
  outline:2px solid rgba(70,90,255,.35);
  border-radius:9999px;
}
.day--in-range{ background: rgba(70,90,255,.12); }

/* 프리뷰(hover) */
.day--hover-end{
  background: rgba(70,90,255,.22);
  outline:2px solid rgba(70,90,255,.30);
  border-radius:9999px;
}
.day--preview-in-range{ background: rgba(70,90,255,.10); }
</style>
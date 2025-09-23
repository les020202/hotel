<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from "vue";

// v-model:adults / v-model:children 과 호환되는 prop 이름 사용
const props = defineProps({
  open: { type: Boolean, default: false },

  // v-model 대상
  adults:   { type: Number, default: 2 },
  children: { type: Number, default: 0 },

  // (하위호환) 혹시 기존 이름으로 전달되면 우선 사용
  modelValueAdults:   { type: Number, default: undefined },
  modelValueChildren: { type: Number, default: undefined },

  minAdults:   { type: Number, default: 1 },
  maxAdults:   { type: Number, default: 8 },
  maxChildren: { type: Number, default: 8 },
  childHint:      { type: String, default: "어린이는 2명까지 무료입니다." },
  childAgeLabel:  { type: String, default: "0 - 7세" },
});

const emit = defineEmits(["update:adults","update:children","close","confirm"]);

// 초기값: 새 이름 우선, 없으면 구 이름 사용
const adults   = ref(
  Number.isFinite(props.adults) ? props.adults
  : Number.isFinite(props.modelValueAdults) ? props.modelValueAdults
  : 2
);
const children = ref(
  Number.isFinite(props.children) ? props.children
  : Number.isFinite(props.modelValueChildren) ? props.modelValueChildren
  : 0
);

// 부모에서 값이 바뀌면 동기화
watch(() => props.adults,   v => { if (Number.isFinite(v)) adults.value = v; });
watch(() => props.children, v => { if (Number.isFinite(v)) children.value = v; });

function incA(){ if (adults.value   < props.maxAdults)   { adults.value++;   emit("update:adults", adults.value);   } }
function decA(){ if (adults.value   > props.minAdults)   { adults.value--;   emit("update:adults", adults.value);   } }
function incC(){ if (children.value < props.maxChildren) { children.value++; emit("update:children", children.value); } }
function decC(){ if (children.value > 0)                  { children.value--; emit("update:children", children.value); } }

function onKey(e){ if (e.key === "Escape") emit("close"); }
onMounted(()=> document.addEventListener("keydown", onKey));
onBeforeUnmount(()=> document.removeEventListener("keydown", onKey));
</script>

<template>
  <transition name="fade">
    <div v-if="open" class="gp-root" @click.self="emit('close')">
      <div class="gp-panel" role="dialog" aria-label="인원 선택">
        <!-- 성인 -->
        <div class="row">
          <div class="col">
            <div class="label">성인</div>
            <div class="sub">18세 이상</div>
          </div>
          <div class="ctrl">
            <button class="btn" :disabled="adults<=minAdults" @click="decA">−</button>
            <span class="num">{{ adults }}</span>
            <button class="btn" :disabled="adults>=maxAdults" @click="incA">+</button>
          </div>
        </div>

        <!-- 아동 -->
        <div class="row">
          <div class="col">
            <div class="label">아동</div>
            <div class="sub">{{ childAgeLabel }}</div>
          </div>
          <div class="ctrl">
            <button class="btn" :disabled="children<=0" @click="decC">−</button>
            <span class="num">{{ children }}</span>
            <button class="btn" :disabled="children>=maxChildren" @click="incC">+</button>
          </div>
        </div>

        <p class="policy">{{ childHint }}</p>

        <div class="actions">
          <button class="cancel" @click="emit('close')">닫기</button>
          <button class="ok" @click="emit('confirm')">적용</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.fade-enter-active,.fade-leave-active{ transition: opacity .12s ease }
.fade-enter-from,.fade-leave-to{ opacity:0 }
.gp-root{ position:fixed; inset:0; background:rgba(17,24,39,.35); display:flex; align-items:start; justify-content:center; padding-top:8rem; z-index:50; }
.gp-panel{ width:320px; background:#fff; border-radius:14px; box-shadow:0 20px 50px rgba(0,0,0,.15); padding:16px; }
.row{ display:flex; align-items:center; justify-content:space-between; padding:10px 0; }
.col .label{ font-weight:700; }
.col .sub{ font-size:12px; color:#6b7280; margin-top:2px; }
.ctrl{ display:flex; align-items:center; gap:10px; }
.btn{ width:36px; height:36px; border-radius:9999px; border:1px solid #d1d5db; background:#fff; font-size:20px; line-height:1; }
.btn:disabled{ opacity:.4; cursor:not-allowed }
.num{ width:28px; text-align:center; font-weight:600; }
.policy{ margin:8px 0 0; font-size:12px; color:#dc2626; }
.actions{ display:flex; justify-content:flex-end; gap:8px; margin-top:14px; }
.cancel{ padding:8px 12px; border-radius:10px; border:1px solid #e5e7eb; background:#fff; }
.ok{ padding:8px 12px; border-radius:10px; background:#4f46e5; color:#fff; font-weight:600; }
</style>

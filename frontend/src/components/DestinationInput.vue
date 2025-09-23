<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed } from "vue";
import { fetchHotelSuggest } from "@/api/searchApi.js";

const props = defineProps({
  modelValue: { type: String, default: "" },
  open: { type: Boolean, default: false },
  placeholder: { type: String, default: "" }
});
const emit = defineEmits(["update:modelValue","select","close"]);

const q = ref(props.modelValue);
watch(() => props.modelValue, v => q.value = v);

// 국내 지역 고정 목록
const REGIONS = [
  "서울","인천","울산","경상북도","경상남도","부산","전라남도","전라북도",
  "강원","경기","충청북도","충청남도","광주","대구","대전"
];

const mode = computed(() => (q.value?.trim()?.length ? "hotel" : "region"));
const filteredRegions = computed(() => {
  const t = (q.value || "").trim();
  return t ? REGIONS.filter(r => r.includes(t)) : REGIONS;
});

// 실시간 호텔 검색
const suggestions = ref([]);
const loading = ref(false);
let timer = null;

watch(q, (v) => {
  emit("update:modelValue", v);
  if (!v?.trim()) { suggestions.value = []; return; }
  clearTimeout(timer);
  timer = setTimeout(async () => {
    loading.value = true;
    try {
      suggestions.value = await fetchHotelSuggest(v.trim(), 8);
    } finally {
      loading.value = false;
    }
  }, 200); // 디바운스 200ms
});

function choose(text){
  emit("update:modelValue", text);
  emit("select", text); // SearchBar.onPickDestination → 달력 자동 오픈
  emit("close");        // 패널 닫기
}

function onKey(e){
  if (e.key === "Escape") emit("close");
  if (e.key === "Enter") {
    if (mode.value === "hotel" && suggestions.value.length) choose(suggestions.value[0].name);
    else if (mode.value === "region" && filteredRegions.value.length) choose(filteredRegions.value[0]);
  }
}

onMounted(()=> document.addEventListener("keydown", onKey));
onBeforeUnmount(()=> document.removeEventListener("keydown", onKey));

const showPanel = computed(() => props.open);
</script>

<template>
  <div class="dest">
    <div class="input">
      <svg width="20" height="20" viewBox="0 0 24 24"><path fill="#6b7280" d="M21 20.3l-5.4-5.4a7.5 7.5 0 10-1.4 1.4L20.3 21 21 20.3zM4 10a6 6 0 1112 0A6 6 0 014 10z"/></svg>
      <input
        :placeholder="placeholder"
        v-model="q"
        class="ipt"
      />
    </div>

    <transition name="fade">
      <div v-if="showPanel" class="panel" role="dialog" @click.stop>
        <!-- 지역 모드 -->
        <template v-if="mode==='region'">
          <h4>대한민국 내 여행지</h4>
          <ul class="grid">
            <li v-for="name in filteredRegions" :key="name" @click.stop="choose(name)">
              <div class="thumb"></div>
              <div class="txt"><b>{{ name }}</b></div>
            </li>
            <li v-if="filteredRegions.length===0" class="empty">검색 결과가 없습니다</li>
          </ul>
        </template>

        <!-- 호텔 자동완성 모드 -->
        <template v-else>
          <h4>호텔 검색</h4>
          <ul class="list">
            <li v-if="loading" class="empty">검색 중…</li>
            <li v-else-if="!suggestions.length" class="empty">일치하는 호텔이 없습니다</li>

            <li v-for="it in suggestions" :key="it.hotelId" @click.stop="choose(it.name)">
              <div class="thumb"></div>
              <div class="txt">
                <b>{{ it.name }}</b>
                <div class="sub">{{ it.region }} <span v-if="it.address">· {{ it.address }}</span></div>
              </div>
            </li>
          </ul>
        </template>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.fade-enter-active,.fade-leave-active{ transition: opacity .12s ease }
.fade-enter-from,.fade-leave-to{ opacity:0 }

.dest{ position:relative; }
.input{ display:flex; align-items:center; gap:8px; padding:10px 12px; border-radius:12px; }
.ipt{ flex:1; border:0; outline:none; background:transparent; font-size:16px; }

.panel{
  position:absolute; left:0; right:0; top:52px; z-index:40;
  background:#fff; border:1px solid #e5e7eb; border-radius:12px;
  box-shadow:0 20px 40px rgba(0,0,0,.12); padding:14px 16px;
}
h4{ margin:6px 0 12px; font-size:13px; color:#6b7280; }

ul{ margin:0; padding:0; list-style:none; }
.grid{ display:grid; grid-template-columns: 1fr 1fr; gap:10px 14px; }
.list li, .grid li{ display:flex; gap:10px; padding:10px; border-radius:10px; cursor:pointer; }
.list li:hover, .grid li:hover{ background:#f9fafb; }
.thumb{ width:44px; height:44px; border-radius:12px; background:#e5e7eb; }
.txt b{ font-size:15px; color:#111827; }
.txt .sub{ color:#6b7280; font-size:12px; margin-top:2px; }
.empty{ text-align:center; color:#9ca3af; padding:12px 0; }
</style>
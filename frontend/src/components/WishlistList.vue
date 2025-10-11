<!-- src/components/WishlistList.vue -->
<template>
  <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-5">
    <article
      v-for="item in items"
      :key="item.wishlistId"
      class="card"
    >
      <div class="thumb" @click="goDetail(item.hotel.id)">
        <img :src="coverOf(item)" alt="" />
      </div>

      <div class="p-3">
        <div class="flex items-start justify-between gap-3">
          <h3 class="name line-clamp-2" @click="goDetail(item.hotel.id)">
            {{ item.hotel.name }}
          </h3>
          <span v-if="item.hotel.rating != null" class="rating">
            ★ {{ fmtRating(item.hotel.rating) }}
          </span>
        </div>

        <div class="meta text-sm text-gray-500 mt-1">
          <span>{{ item.hotel.region }}</span>
          <span class="dot">·</span>
          <span class="line-clamp-1">{{ item.hotel.address }}</span>
        </div>

        <div class="mt-3 flex items-center justify-between gap-2">
          <button class="btn-outline" @click="goDetail(item.hotel.id)">View Place</button>
          <button class="btn-danger" @click="$emit('remove', item)">찜 삭제</button>
        </div>

        <div class="time text-xs text-gray-400 mt-2">
          등록: {{ fmtDateTime(item.createdAt) }}
        </div>
      </div>
    </article>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  items: { type: Array, default: () => [] }
})
defineEmits(['remove'])

const router = useRouter()

function goDetail(hotelId) {
  router.push(`/hotel/${hotelId}`)
}

function coverOf(item) {
  // 응답에 coverImageUrl 만 제공됨(없으면 플레이스홀더)
  return item?.hotel?.coverImageUrl || '/images/hotel/placeholder.jpg'
}

function fmtRating(r) {
  // DB는 DECIMAL(2,1), 백엔드는 Double로 내려줌 → 한 자리 고정
  const n = Number(r)
  return Number.isFinite(n) ? n.toFixed(1) : r
}

function fmtDateTime(s) {
  const d = new Date(s)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}.${m}.${day} ${hh}:${mm}`
}
</script>

<style scoped>
.card{
  border: 1px solid #e5e7eb; border-radius: 14px; overflow: hidden; background: #fff;
  transition: box-shadow .15s ease, transform .05s ease;
}
.card:hover{ box-shadow: 0 6px 20px rgba(2,6,23,0.08) }
.thumb{ aspect-ratio: 4/3; background:#f8fafc; cursor: pointer; }
.thumb img{ width:100%; height:100%; object-fit:cover; display:block; }
.name{ font-weight: 700; cursor: pointer; }
.rating{ font-size: 12px; color:#0a6; white-space: nowrap; }
.meta .dot{ margin:0 .35rem; opacity:.6 }
.btn-outline{
  padding: 6px 10px; border:1px solid #e5e7eb; border-radius: 10px; background:#fff; font-size: 12px;
}
.btn-outline:hover{ border-color:#cbd5e1; }
.btn-danger{
  padding: 6px 10px; border:1px solid #fecaca; background:#fff1f2; color:#991b1b; border-radius:10px; font-size:12px;
}
.btn-danger:hover{ background:#ffe4e6; }
</style>

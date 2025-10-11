<!-- src/views/mypage/MyReviews.vue -->
<template>
  <div class="my-reviews">
    <h2>내 리뷰</h2>

    <div v-if="loading" class="loading">불러오는 중...</div>
    <div v-else>
      <div v-if="items.length === 0" class="empty">작성한 리뷰가 없습니다.</div>

      <ul class="list">
        <li v-for="r in items" :key="r.id" class="item">
          <div class="head">
            <div class="rating">★ {{ r.rating }}</div>
            <div class="meta">
              <span class="badge" :class="r.visible ? 'ok' : 'hidden'">
                {{ r.visible ? '노출 중' : '숨김됨' }}
              </span>
              <span class="date">{{ formatDate(r.createdAt) }}</span>
            </div>
          </div>

          <div class="comment">{{ r.comment }}</div>

          <div class="photos" v-if="r.photos?.length">
            <img v-for="p in r.photos" :key="p" :src="p" />
          </div>

          <div class="actions">
            <RouterLink :to="`/hotels/${r.hotelId}`">호텔로 이동</RouterLink>
            <button @click="onDelete(r.id)">삭제</button>
          </div>
        </li>
      </ul>

      <div class="pager" v-if="totalPages > 1">
        <button :disabled="page===0" @click="go(page-1)">이전</button>
        <span>{{ page+1 }} / {{ totalPages }}</span>
        <button :disabled="page===totalPages-1" @click="go(page+1)">다음</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { fetchMyReviews, deleteMyReview } from '@/api/reviews';

type ReviewItem = {
  id: number;
  hotelId: number;
  rating: number;
  comment: string;
  visible: boolean;
  createdAt: string;
  photos: string[];
};

const items = ref<ReviewItem[]>([]);
const page = ref(0);
const size = ref(10);
const totalPages = ref(0);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const res = await fetchMyReviews(page.value, size.value);
    items.value = res.content || [];
    totalPages.value = res.totalPages || 1;
  } finally {
    loading.value = false;
  }
}

function go(p: number) {
  page.value = p;
  load();
}

async function onDelete(id: number) {
  if (!confirm("이 리뷰를 삭제할까요?")) return;
  await deleteMyReview(id);
  // 현재 페이지 재조회
  // (삭제로 인해 페이지가 비면 한 페이지 당겨주고 다시 불러오기)
  if (items.value.length === 1 && page.value > 0) page.value -= 1;
  await load();
}

function formatDate(iso?: string) {
  if (!iso) return "";
  try {
    const d = new Date(iso);
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  } catch { return iso?.slice(0,10) || "" }
}

onMounted(load);
</script>

<style scoped>
.my-reviews { max-width: 820px; margin: 0 auto; }
.loading, .empty { padding: 16px; color: #666; }
.list { display: grid; gap: 12px; }
.item { border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; background: #fff; }
.head { display: flex; justify-content: space-between; align-items: center; }
.rating { font-weight: 700; }
.meta { display: flex; gap: 8px; align-items: center; }
.badge { font-size: 12px; padding: 3px 8px; border-radius: 999px; border: 1px solid #e5e7eb; }
.badge.ok { background: #ecfdf5; border-color: #a7f3d0; }
.badge.hidden { background: #f9fafb; color: #6b7280; }
.comment { margin: 8px 0; white-space: pre-wrap; }
.photos { display: flex; gap: 8px; flex-wrap: wrap; }
.photos img { width: 96px; height: 96px; object-fit: cover; border-radius: 8px; }
.actions { display: flex; gap: 12px; margin-top: 8px; }
.pager { display: flex; gap: 10px; align-items: center; justify-content: center; margin-top: 12px; }
</style>

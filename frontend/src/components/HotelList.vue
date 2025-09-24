<template>
  <!-- 카드 간격은 여기서 통제 -->
  <ul class="card-list">
    <li
      v-for="h in items"
      :key="h.hotelId"
      class="hotel-card"
      @click="goDetail(h)"
      role="button"
    >
      <div class="hotel-row">
        <!-- 좌측: 이미지 -->
        <figure class="img-wrap">
          <img :src="safeImg(h.coverImageUrl) || placeholder" alt="" class="img" loading="lazy" />
          <span class="badge">12 images</span>
        </figure>

        <!-- 우측: 정보 -->
        <div class="info">
          <div class="title-price">
            <h3 class="title">{{ h.name }}</h3>
            <div class="price-box">
              <div class="price-caption">starting from</div>
              <div class="price">₩{{ money(h.startingFrom) }}</div>
            </div>
          </div>

          <div class="addr">
            <span class="pin" aria-hidden="true">📍</span>
            <span>{{ h.address || h.region || '' }}</span>
          </div>

          <!-- 성급 -->
          <div class="stars-line">
            <div class="stars" :aria-label="`${h.gradeLevel || 0} star hotel`">
              <span v-for="i in (h.gradeLevel || 0)" :key="i">★</span>
            </div>
            <span class="gray">{{ (h.gradeLevel || 0) }} Star Hotel</span>
            <span class="dot">•</span>
            <span class="gray">Amenities</span>
          </div>

          <div class="rating-line">
            <div class="rating-box">
              {{ h.rating != null ? Number(h.rating).toFixed(1) : '—' }}
            </div>
            <div class="rating-label">{{ ratingLabel(h.rating) }}</div>
          </div>

          <div class="cta" @click.stop>
            <button
              class="wish"
              @click="onToggle(h.hotelId)"
              :aria-pressed="isWished(h.hotelId)"
              :title="isWished(h.hotelId) ? '위시리스트 제거' : '위시리스트 추가'"
            >
              <svg v-if="isWished(h.hotelId)" viewBox="0 0 24 24" class="w-6 h-6 fill-rose-500">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6.5 3.5 5 5.5 5c1.7 0 3.25 1.03 3.97 2.57h1.06C11.25 6.03 12.8 5 14.5 5 16.5 5 18 6.5 18 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" class="w-6 h-6 stroke-gray-700 fill-none">
                <path stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"
                      d="M12.1 20.3C7.14 15.78 4 12.94 4 9.5 4 7.5 5.5 6 7.5 6c1.54 0 3.04.99 3.57 2.36h1.87C13.46 6.99 14.96 6 16.5 6 18.5 6 20 7.5 20 9.5c0 3.44-3.14 6.28-8.1 10.8z"/>
              </svg>
            </button>

            <button class="view" @click="goDetail(h)">
              View Place
            </button>
          </div>
        </div>
      </div>
    </li>
  </ul>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

// ✅ 전역 위시리스트 상태/헬퍼 사용
//  - ensureWishlistLoaded: 로그인 시 서버에서 전체 위시 상태 1회 동기화
//  - isWished(hotelId): 전역 Set 기반으로 하트 색상 판정
//  - toggleWishlist(hotelId): 호텔ID 기준으로 POST/DELETE (by-hotel 우선)
import {
  ensureWishlistLoaded,
  isWished as isWishedHotel,
  toggleWishlist,
  syncHotels
} from '@/api/wishlistApi'

// 로그인 확인용 (미로그인일 때 로그인 페이지로 이동)
import { getMe } from '@/api/auth'

const props = defineProps({
  items: { type: Array, default: () => [] },
  checkIn: { type: String, default: null },
  checkOut: { type: String, default: null },
  guests: { type: Number, default: 1 }
})

const router = useRouter()
const placeholder = 'https://placehold.co/800x600?text=Hotel'

/* 금액 표시 */
const money = (v) => (v == null ? '-' : new Intl.NumberFormat('ko-KR').format(Number(v)))

/* 평점 라벨 */
const ratingLabel = (r) => {
  if (r == null) return ''
  const x = Number(r)
  if (x === 5.0) return 'amazing'
  if (x >= 4.5) return 'excellent'
  if (x >= 4.0) return 'very good'
  if (x >= 3.5) return 'good'
  if (x >= 3.0) return 'normal'
  if (x >= 2.5) return 'not bad'
  return 'bad'
}

/* 이미지 세이프 */
function safeImg(u){
  if (!u || typeof u !== 'string') return placeholder
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('/')) return u
  return placeholder
}

/* 전역 store 기반 상태 조회 */
function isWished(hotelId) {
  return isWishedHotel(hotelId)
}

/* 하트 토글(알림/토스트 없음) */
async function onToggle(hotelId) {
  try {
    await getMe() // 미로그인 시 401 → catch
  } catch {
    const redirect = encodeURIComponent(location.pathname + location.search)
    router.push(`/login?redirect=${redirect}`)
    return
  }
  try {
    await toggleWishlist(hotelId) // 내부에서 낙관적 UI + 실패 시 롤백
  } catch (e) {
    // 네트워크/서버 오류는 콘솔만
    console.error('wishlist error', e)
  }
}

/* 상세로 이동 (search에서 받은 파라미터 그대로 전달) */
function goDetail(h) {
  router.push({
    name: 'hotel-detail',
    params: { id: h.hotelId },
    query: {
      checkIn: props.checkIn,
      checkOut: props.checkOut,
      guests: props.guests ?? 1
    }
  })
}

/* 초기 진입 시: 로그인 상태면 위시 동기화 → 처음부터 빨간 하트 표시 */
onMounted(async () => {
  await ensureWishlistLoaded()
  // 현재 목록에 보이는 호텔들만 빠르게 동기화하고 싶다면 추가:
  const ids = (props.items || []).map(it => it.hotelId).filter(Boolean)
  if (ids.length) await syncHotels(ids)
})
</script>

<style scoped>
/* 리스트 컨테이너: 카드 간격 넓힘 */
.card-list{
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 3rem;  
  align-items: center;
}

/* 카드 기본 */
.hotel-card{
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  box-shadow: 0 1px 2px rgba(0,0,0,.05);
  max-width: 1100px; 
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  margin: 0 auto; 
}
.hotel-card:hover { box-shadow: 0 6px 20px rgba(0,0,0,.08); }

/* 가로 레이아웃 */
.hotel-row{
  display: flex;
  gap: 1.5rem;
  padding: 1rem;
  align-items: stretch;
}

/* 이미지 영역: 카드의 약 52% 차지 */
.img-wrap{
  position: relative;
  flex: 0 0 52%;
  max-width: 52%;
  height: 18rem;      /* md 기준 높이 */
  border-radius: 1rem;
  overflow: hidden;
}
@media (min-width: 1280px){
  .img-wrap{ height: 20rem; }
}
.img{
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}
.badge{
  position: absolute;
  top: .5rem; right: .5rem;
  background: rgba(0,0,0,.6);
  color: #fff; font-size: .75rem;
  padding: .25rem .5rem; border-radius: 9999px;
}

/* 정보 영역 */
.info{
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.title-price{
  display: flex; justify-content: space-between; gap: .75rem;
}
.title{
  font-weight: 800; font-size: 1.5rem; color: #111827;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.price-box{ text-align: right; }
.price-caption{ font-size: .875rem; color: #6b7280; }
.price{ color: #ef4444; font-weight: 800; font-size: 1.5rem; }

/* 주소 */
.addr{
  margin-top: .5rem; color: #374151; font-size: .9rem;
  display: flex; gap: .5rem;
}
.pin{ flex: 0 0 auto; }

/* 별/어메니티 */
.stars-line{
  margin-top: .5rem; display: flex; align-items: center;
  gap: .5rem; font-size: .9rem;
}
.stars{ color: #f59e0b; }
.gray{ color: #374151; }
.dot{ color: #d1d5db; }

/* 평점 */
.rating-line{
  margin-top: .5rem; display: flex; align-items: center; gap: .75rem;
}
.rating-box{
  border: 1px solid #e5e7eb; border-radius: .5rem;
  padding: .25rem .5rem; min-width: 2.5rem; text-align: center;
  color: #111827; font-size: .9rem;
}
.rating-label{ color: #374151; font-size: .9rem; }

/* CTA */
.cta{
  margin-top: auto; padding-top: 1rem;
  display: flex; align-items: center; gap: 1rem;
}
.wish{
  width: 3rem; height: 3rem; border: 1px solid #e5e7eb;
  border-radius: .75rem; background: #fff;
}
.wish:hover{ background: #f9fafb; }
.view{
  flex: 1 1 auto; background: #059669; color: #fff; font-weight: 600;
  padding: .75rem 1rem; border: 0; border-radius: .75rem;
}
.view:hover{ background: #047857; }
</style>

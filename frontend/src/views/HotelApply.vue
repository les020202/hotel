<template>
  <div class="min-h-screen bg-neutral-50">
    <!-- 상단 헤더바 -->
    <div class="sticky top-0 z-10 border-b border-neutral-200 bg-white/70 backdrop-blur-sm">
      <div class="mx-auto max-w-7xl px-6 py-4">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-xl font-bold">호텔 등록 — 사용자 뷰 (Vue)</h1>
            <p class="mt-0.5 text-xs text-neutral-600">
              필수: 소유자/사업자번호/연락처/등급/숙소이름/주소/지역 (+ 선택: 대표 이미지 URL)
            </p>
          </div>
          <div class="hidden sm:flex items-center gap-2">
            <button type="button" @click="onReset" class="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm text-neutral-700 shadow-sm hover:bg-neutral-50 active:scale-[.99] transition">초기화</button>
            <button type="button" @click="goMine" class="rounded-lg bg-neutral-900 px-3 py-1.5 text-sm font-semibold text-white shadow-sm hover:bg-black active:scale-[.99] transition">내 신청 목록</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 본문 -->
    <main class="mx-auto max-w-7xl px-6 py-8">
      <!-- 배너 -->
      <div
        v-if="banner"
        class="mb-6 rounded-xl px-4 py-3 text-sm shadow-sm ring-1"
        :class="banner.type === 'success'
          ? 'bg-green-50 text-green-800 ring-green-200'
          : 'bg-rose-50 text-rose-800 ring-rose-200'">
        {{ banner.text }}
      </div>

      <!-- 2열 레이아웃 -->
      <form @submit.prevent="onSubmit" class="grid grid-cols-1 gap-8 md:grid-cols-12">
        <!-- LEFT -->
        <section class="space-y-6 md:col-span-7 lg:col-span-8">
          <!-- 기본 정보 -->
          <div class="card">
            <div class="card-head">
              <h3>기본 정보</h3>
            </div>
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <label class="field">
                <span>소유자 이름</span>
                <input v-model.trim="ownerName" :class="inputCls" placeholder="홍길동" />
                <p v-if="showErr && !ownerName" class="err">필수 입력입니다.</p>
              </label>

              <label class="field">
                <span>사업자번호</span>
                <input v-model="bizNoModel" :class="inputCls" placeholder="123-45-67890" />
                <p v-if="showErr && !bizNo" class="err">필수 입력입니다.</p>
              </label>

              <label class="field">
                <span>전화번호</span>
                <input v-model="phoneModel" :class="inputCls" placeholder="010-1234-5678" />
                <p v-if="showErr && !phone" class="err">필수 입력입니다.</p>
              </label>

              <label class="field">
                <span>등급(성급)</span>
                <select v-model="gradeLevel" :class="inputCls">
                  <option value="">선택</option>
                  <option v-for="n in [1,2,3,4,5]" :key="n" :value="String(n)">{{ n }}성급</option>
                </select>
                <p v-if="showErr && !gradeLevel" class="err">필수 선택입니다.</p>
              </label>

              <label class="field md:col-span-2">
                <span>숙소이름</span>
                <input v-model.trim="hotelName" :class="inputCls" placeholder="예: 류경호텔" />
                <p v-if="showErr && !hotelName" class="err">필수 입력입니다.</p>
              </label>
            </div>
          </div>

          <!-- 어메니티 -->
          <div class="card">
            <div class="card-head">
              <h3>어메니티</h3>
            </div>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="a in AMENITY_OPTS"
                :key="a.code"
                type="button"
                @click="toggleAmenity(a.code)"
                class="chip"
                :class="amenities.includes(a.code) ? 'chip-on' : 'chip-off'">
                <span v-if="amenities.includes(a.code)" class="mr-1">✓</span>{{ a.name }}
              </button>
            </div>
          </div>

          <!-- 주소 + 지역 -->
          <div class="card">
            <div class="card-head"><h3>숙소 주소 / 지역</h3></div>
            <div class="grid grid-cols-1 gap-4 md:grid-cols-6">
              <label class="field md:col-span-2">
                <span>지역(시/도)</span>
                <select v-model="region" :class="inputCls">
                  <option value="">선택</option>
                  <option v-for="r in REGION_OPTS" :key="r" :value="r">{{ r }}</option>
                </select>
                <p v-if="showErr && !region" class="err">필수 선택입니다.</p>
              </label>

              <label class="field md:col-span-4">
                <span>우편번호 · 주소 검색</span>
                <div class="flex gap-2">
                  <input v-model="postcode" :class="inputCls" placeholder="우편번호" readonly />
                  <button type="button" class="btn-ghost" @click="showPost = true">주소 검색</button>
                </div>
              </label>

              <label class="field md:col-span-6">
                <span>주소</span>
                <input v-model="address1" :class="inputCls" placeholder="도로명 주소" readonly />
                <p v-if="showErr && !address1" class="err">필수 입력입니다.</p>
              </label>

              
            </div>
          </div>

          <!-- 대표 이미지 URL -->
          <div class="card">
            <div class="card-head">
              <h3>대표 이미지(URL)</h3>
              <p>이미지 파일 업로드 대신, 접근 가능한 이미지 URL을 입력하세요.</p>
            </div>
            <div class="grid grid-cols-1 gap-4 md:grid-cols-6">
              <label class="field md:col-span-6">
                <span>이미지 URL</span>
                <input v-model.trim="coverUrl" :class="inputCls" placeholder="https://example.com/hotel-cover.jpg" />
                <p v-if="coverUrl && !isValidUrl(coverUrl)" class="err">유효한 URL을 입력해주세요.</p>
              </label>
              <div v-if="coverUrl && isValidUrl(coverUrl)" class="md:col-span-6">
                <div class="rounded-xl ring-1 ring-neutral-200 overflow-hidden">
                  <img :src="coverUrl" alt="preview" class="h-48 w-full object-cover" />
                </div>
                <p class="mt-1 text-xs text-neutral-500">입력하신 URL 미리보기</p>
              </div>
            </div>
          </div>

          <!-- 객실 타입 -->
          <div class="card">
            <div class="mb-3 flex items-center justify-between">
              <div class="card-head mb-0">
                <h3>객실 타입</h3>
                <p>여러 개 등록할 수 있습니다.</p>
              </div>
              <div class="flex gap-2">
                <button type="button" @click="addRoom" class="btn-primary">+ 객실 타입 추가</button>
                <button v-if="rooms.length" type="button" @click="addRoomFromLast" class="btn-ghost">+ 최근 값으로 추가</button>
              </div>
            </div>

            <div class="space-y-5">
              <div v-for="(r, idx) in rooms" :key="r.id" class="rounded-xl border border-neutral-200 p-4 shadow-sm">
                <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <div class="flex items-center gap-2">
                    <span class="inline-flex h-7 items-center rounded-md bg-neutral-100 px-2 text-xs font-semibold text-neutral-700 ring-1 ring-neutral-200">
                      타입 #{{ idx + 1 }}
                    </span>
                    <span class="text-xs text-neutral-500">({{ labelOf(r.typeCode) }}, {{ r.capacity }}인 · {{ r.size }}m²)</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <button type="button" @click="duplicateRoom(idx)" class="btn-ghost">복제</button>
                    <button type="button" @click="removeRoom(r.id)" class="btn-danger" :disabled="rooms.length === 1">제거</button>
                  </div>
                </div>

                <div class="grid grid-cols-1 gap-3 md:grid-cols-3">
                  <label class="field">
                    <span>타입</span>
                    <select v-model="r.typeCode" :class="inputCls">
                      <option value="STANDARD">스탠다드</option>
                      <option value="DELUXE">디럭스</option>
                      <option value="SUITE">스위트</option>
                      <option value="PREMIUM">프리미엄</option>
                    </select>
                  </label>
                  <label class="field">
                    <span>객실 수</span>
                    <input v-model.number="r.roomCount" type="number" min="1" :class="inputCls" />
                  </label>
                  <label class="field">
                    <span>호실 (쉼표로 구분)</span>
                    <input v-model.trim="r.roomNos" :class="inputCls" placeholder="101,102,103" />
                  </label>
                  <label class="field">
                    <span>최대 투숙인원</span>
                    <input v-model.number="r.capacity" type="number" min="1" :class="inputCls" />
                  </label>
                  <label class="field">
                    <span>객실 크기(m²)</span>
                    <input v-model.number="r.size" type="number" min="1" :class="inputCls" />
                  </label>
                  <label class="field">
                    <span></span>
                  </label>

                  <label class="field">
                    <span>환불 가능</span>
                    <div class="flex h-10 items-center gap-3">
                      <input :id="'ref-' + r.id" type="checkbox" v-model="r.refundable" class="h-4 w-4" />
                      <label :for="'ref-' + r.id" class="text-sm">환불 가능 요금제</label>
                    </div>
                  </label>

                  <label class="field">
                    <span>조식 포함</span>
                    <div class="flex h-10 items-center gap-3">
                      <input :id="'bf-' + r.id" type="checkbox" v-model="r.breakfast" class="h-4 w-4" />
                      <label :for="'bf-' + r.id" class="text-sm">조식 포함</label>
                    </div>
                  </label>

                  <label class="field md:col-span-3">
                    <span>요금 정책</span>
                    <textarea v-model="r.policy" :class="[inputCls, 'min-h-24']" placeholder="환불/변경/성수기/주말가 등"></textarea>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <!-- 코멘트 -->
          <div class="card">
            <div class="card-head"><h3>내용(코멘트)</h3></div>
            <textarea v-model.trim="comment" :class="[inputCls, 'min-h-28']" placeholder="운영 정보나 참고 코멘트"></textarea>
          </div>
        </section>

        <!-- RIGHT : sticky 요약 -->
        <aside class="md:col-span-5 lg:col-span-4">
          <div class="sticky md:top-24 lg:top-28 space-y-6">
            <!-- 제출 -->
            <div id="submitCard" class="card">
              <div class="card-head mb-3"><h3>제출</h3></div>
              <div class="flex flex-col gap-3">
                <button type="submit" :disabled="submitting" class="h-11 rounded-xl bg-indigo-600 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700 active:scale-[.99] transition">
                  {{ submitting ? '전송 중...' : '신청하기' }}
                </button>
                <button type="button" @click="onReset" class="h-11 rounded-xl border border-neutral-300 text-sm font-semibold text-neutral-700 hover:bg-neutral-50 active:scale-[.99] transition">
                  취소
                </button>
                <p class="text-xs text-neutral-500">신청 후 검토에 2~3일 정도 소요될 수 있습니다.</p>
              </div>
            </div>

            <!-- 요약 -->
            <div class="card max-h-[calc(100vh-16rem)] overflow-auto">
              <div class="card-head mb-3"><h3>요약</h3></div>
              <div class="space-y-2 text-sm">
                <div class="row"><span>소유자</span><b>{{ ownerName || '-' }}</b></div>
                <div class="row"><span>사업자번호</span><b>{{ bizNo || '-' }}</b></div>
                <div class="row"><span>전화번호</span><b>{{ phone || '-' }}</b></div>
                <div class="row"><span>등급</span><b>{{ gradeLevel ? gradeLevel + '성급' : '-' }}</b></div>
                <div class="row"><span>숙소이름</span><b>{{ hotelName || '-' }}</b></div>
                <div class="row"><span>지역</span><b>{{ region || '-' }}</b></div>
                <div class="row"><span>주소</span><b>{{ address1 || '-' }}</b></div>
                <div class="row"><span>우편번호</span><b>{{ postcode || '-' }}</b></div>
                <div class="row">
                  <span>어메니티</span><b>{{ amenities.length ? amenities.join(', ') : '-' }}</b>
                </div>

                <div v-if="coverUrl" class="mt-3">
                  <div class="rounded-xl overflow-hidden ring-1 ring-neutral-200">
                    <img :src="coverUrl" alt="cover" class="h-28 w-full object-cover" />
                  </div>
                  <div class="mt-1 text-[11px] text-neutral-500 truncate">{{ coverUrl }}</div>
                </div>

                <div class="mt-3 rounded-xl bg-neutral-50 p-3 ring-1 ring-neutral-200">
                  <div class="mb-2 text-xs font-medium text-neutral-600">객실 타입</div>
                  <ul class="space-y-2 text-xs">
                    <li v-for="(r, i) in rooms" :key="r.id" class="flex items-center justify-between">
                      <span>#{{ i + 1 }} {{ labelOf(r.typeCode) }} · {{ r.capacity }}인 · {{ r.size }}m²</span>
                      <span class="tabular-nums"></span>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </form>

      <footer class="mt-12 pb-12 text-center text-xs text-neutral-500"></footer>
    </main>

    <!-- 주소 검색 모달 -->
    <PostcodeSearch v-if="showPost" @select="onSelectPostcode" @close="showPost = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import api from '@/api/auth'
import router from '@/router'
import PostcodeSearch from '@/views/mypage/PostcodeSearch.vue' // ✅ 여기 경로 사용

type Banner = { type: 'success' | 'error'; text: string }
type Room = {
  id: string
  typeCode: 'STANDARD' | 'DELUXE' | 'SUITE' | 'PREMIUM'
  roomCount: number
  roomNos: string
  capacity: number
  size: number
  price: number
  refundable: boolean
  breakfast: boolean
  policy: string
}

const banner = ref<Banner | null>(null)
const showErr = ref(false)

const ownerName = ref('')
const bizNo = ref('')
const phone = ref('')
const gradeLevel = ref('')
const hotelName = ref('')
const region = ref('')              // ✅ 지역
const postcode = ref('')
const address1 = ref('')
const address2 = ref('')
const coverUrl = ref('')            // ✅ 이미지 URL
const amenities = ref<string[]>([])
const comment = ref('')

const rooms = ref<Room[]>([blankRoom()])
const submitting = ref(false)
const showPost = ref(false)         // ✅ 모달 토글

/* 공통 UI */
const inputCls =
  'h-10 w-full rounded-lg border border-neutral-300 px-3 text-sm outline-none transition focus:border-indigo-300 focus:ring-2 focus:ring-indigo-200'

/* 옵션들 */
const AMENITY_OPTS = [
  { code: 'WIFI', name: '인터넷(Wi-Fi)' },
  { code: 'PARKING', name: '주차장' },
  { code: 'POOL', name: '수영장' },
  { code: 'SPA', name: '스파/사우나' },
  { code: 'GYM', name: '피트니스' },
  { code: 'RESTAURANT', name: '레스토랑' },
  { code: 'BAR', name: '바(Bar)' },
  { code: 'FRONTDESK_24H', name: '24시간 프론트' },
  { code: 'AIRPORT_SHUTTLE', name: '공항 셔틀' },
  { code: 'ROOM_SERVICE', name: '룸서비스' },
  { code: 'BREAKFAST_INCLUDED', name: '조식 포함' },
  { code: 'FREE_CANCELLATION', name: '무료 취소' },
  { code: 'PET_FRIENDLY', name: '반려동물 동반 가능' },
  { code: 'FAMILY_FRIENDLY', name: '가족/아동 친화 시설' },
  { code: 'NON_SMOKING', name: '금연' },
  { code: 'ELEVATOR', name: '엘리베이터' },
  { code: 'WHEELCHAIR', name: '장애인 편의시설' },
  { code: 'LAUNDRY', name: '세탁 서비스' },
  { code: 'BUSINESS_CENTER', name: '비즈니스 센터' },
  { code: 'BEACHFRONT', name: '비치프론트/해변 접근' }
]
const REGION_OPTS = [
  '서울특별시','부산광역시','대구광역시','인천광역시','광주광역시','대전광역시','울산광역시',
  '경기도','강원도','충청북도','충청남도','전라북도','전라남도','경상북도','경상남도'
]

/* 포맷터 모델 */
const bizNoModel = computed({
  get: () => bizNo.value,
  set: (v: string) => (bizNo.value = fmtBiz(v))
})
const phoneModel = computed({
  get: () => phone.value,
  set: (v: string) => (phone.value = fmtPhone(v))
})

/* 주소 검색 선택 결과 적용 */
function onSelectPostcode(pay: { zonecode:string; roadAddress?:string; jibunAddress?:string; address1?:string }) {
  postcode.value = pay.zonecode
  // data.address1 은 우리가 PostcodeSearch에서 조합해 보낸 값
  address1.value = pay.address1 || pay.roadAddress || pay.jibunAddress || ''
  // 시/도 자동 추출(사용자가 바꿀 수도 있음)
  const base = (pay.roadAddress || pay.jibunAddress || '').trim()
  const firstToken = base.split(/\s+/)[0]
  if (firstToken) region.value = firstToken
}

/* 유틸 */
function isValidUrl(u: string) {
  try { const x = new URL(u); return !!x.protocol && !!x.host } catch { return false }
}
function toggleAmenity(code: string) {
  const i = amenities.value.indexOf(code)
  if (i >= 0) amenities.value.splice(i, 1)
  else amenities.value.push(code)
}

function blankRoom(): Room {
  return {
    id: crypto.randomUUID(),
    typeCode: 'STANDARD',
    roomCount: 1,
    roomNos: '',
    capacity: 2,
    size: 20,
    price: 100000,
    refundable: true,
    breakfast: false,
    policy: ''
  }
}
function addRoom() { rooms.value.push(blankRoom()) }
function addRoomFromLast() {
  const last = rooms.value[rooms.value.length - 1]
  if (!last) return addRoom()
  const copy: Room = { ...JSON.parse(JSON.stringify(last)), id: crypto.randomUUID() }
  rooms.value.push(copy)
}
function duplicateRoom(idx: number) {
  const target = rooms.value[idx]; if (!target) return
  const copy: Room = { ...JSON.parse(JSON.stringify(target)), id: crypto.randomUUID() }
  rooms.value.splice(idx + 1, 0, copy)
}
function removeRoom(id: string) {
  if (rooms.value.length > 1) rooms.value = rooms.value.filter((r) => r.id !== id)
}
function goMine() { router.push('/hotelapply/mine') }

/* 제출/리셋 */
function onReset() {
  ownerName.value = bizNo.value = phone.value = gradeLevel.value = hotelName.value = ''
  region.value = ''
  postcode.value = address1.value = address2.value = ''
  coverUrl.value = ''
  amenities.value = []
  comment.value = ''
  rooms.value = [blankRoom()]
  banner.value = null
  showErr.value = false
}

async function onSubmit() {
  showErr.value = true
  const missing =
    !ownerName.value || !bizNo.value || !phone.value ||
    !gradeLevel.value || !hotelName.value || !address1.value || !region.value

  if (missing) { banner.value = { type: 'error', text: '필수 항목을 모두 입력해주세요.' }; return }
  if (coverUrl.value && !isValidUrl(coverUrl.value)) {
    banner.value = { type: 'error', text: '대표 이미지 URL 형식이 올바르지 않습니다.' }
    return
  }
  if (!window.confirm('신청을 제출하시겠어요? 제출 후 검토에 2~3일 정도 소요됩니다.')) return

  const payload = {
    ownerName: ownerName.value,
    businessNo: bizNo.value,
    phone: phone.value,
    gradeLevel: Number(gradeLevel.value),
    hotelName: hotelName.value,
    address1: address1.value,
    address2: address2.value,
    postcode: postcode.value,
    region: region.value,                               // ✅ 지역
    coverImageType: coverUrl.value ? 'UPLOADED' : 'NONE', // ✅ 이미지 필드
    coverImageUrl: coverUrl.value || null,
    coverImageTemplate: null,
    amenitiesCsv: amenities.value.join(','),
    comment: comment.value,
    roomsJson: JSON.stringify(
      rooms.value.map(r => ({
        typeCode: r.typeCode,
        roomCount: r.roomCount,
        roomNos: r.roomNos,
        capacity: r.capacity,
        sizeSqm: r.size,
        priceKrw: r.price,
        refundable: r.refundable ? 1 : 0,
        breakfast: r.breakfast ? 1 : 0,
        policyText: r.policy
      }))
    )
  }

  try {
    submitting.value = true
    await api.post('/hotelapp', payload)
    banner.value = { type: 'success', text: '신청이 접수되었습니다. 검토에 2~3일 정도 소요될 수 있습니다.' }
    onReset()
    requestAnimationFrame(() => document.getElementById('submitCard')?.scrollIntoView({ behavior: 'smooth' }))
    setTimeout(() => router.replace('/hotelapply/mine'), 1200)
  } catch (e: any) {
    const status = e?.response?.status
    const data = e?.response?.data
    const errText = data?.error || data?.message || data?.detail || e?.message || '신청 중 오류가 발생했습니다.'
    if (status === 409) {
      const code = (data?.code || data?.error || '').toString().toUpperCase()
      if (code.includes('PENDING') || errText.includes('already') || errText.includes('PENDING')) {
        banner.value = { type: 'error', text: '이미 접수된 신청이 있어요. 기존 신청으로 이동합니다.' }
      } else if (code.includes('CANONICAL') || errText.toLowerCase().includes('canonical')) {
        banner.value = { type: 'error', text: '같은 이름(슬러그)의 신청이 있어요. 호텔명을 조금 바꿔주세요.' }
      } else {
        banner.value = { type: 'error', text: `중복/충돌로 신청이 실패했어요: ${errText}` }
      }
      return
    }
    banner.value = { type: 'error', text: errText }
  } finally {
    submitting.value = false
  }
}

/* 라벨/포맷터 */
function labelOf(t: string) {
  return t === 'STANDARD' ? '스탠다드'
    : t === 'DELUXE' ? '디럭스'
    : t === 'SUITE' ? '스위트'
    : t === 'PREMIUM' ? '프리미엄'
    : t
}
function fmtBiz(raw: string) {
  const d = String(raw).replace(/[^0-9]/g, '').slice(0, 10)
  if (d.length <= 3) return d
  if (d.length <= 5) return `${d.slice(0, 3)}-${d.slice(3)}`
  return `${d.slice(0, 3)}-${d.slice(3, 5)}-${d.slice(5)}`
}
function fmtPhone(raw: string) {
  const d = String(raw).replace(/[^0-9]/g, '').slice(0, 11)
  if (d.startsWith('02')) {
    if (d.length <= 2) return d
    if (d.length <= 6) return `${d.slice(0, 2)}-${d.slice(2)}`
    if (d.length <= 10) return `${d.slice(0, 2)}-${d.slice(2, 6)}-${d.slice(6)}`
    return `${d.slice(0, 2)}-${d.slice(2, 6)}-${d.slice(6, 10)}`
  }
  if (d.length <= 3) return d
  if (d.length <= 7) return `${d.slice(0, 3)}-${d.slice(3)}`
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`
}
</script>

<style scoped>
.card { @apply rounded-2xl bg-white p-5 shadow-sm ring-1 ring-neutral-200; }
.card-head h3 { @apply text-base font-semibold; }
.card-head p { @apply mt-1 text-xs text-neutral-500; }

.field { @apply flex flex-col gap-1; }
.field > span { @apply text-xs font-medium text-neutral-600; }
.err { @apply mt-1 text-xs text-rose-600; }

.chip { @apply rounded-full border px-3 py-1 text-sm transition shadow-sm hover:shadow active:scale-[.99]; }
.chip-on { @apply border-indigo-600 bg-indigo-50 text-indigo-700; }
.chip-off { @apply border-neutral-300 bg-white text-neutral-700; }

.btn-primary { @apply rounded-xl bg-indigo-600 px-3 py-2 text-xs font-semibold text-white shadow-sm hover:bg-indigo-700 active:scale-[.99] transition; }
.btn-ghost { @apply rounded-xl border border-indigo-300 px-3 py-2 text-xs font-semibold text-indigo-700 hover:bg-indigo-50 active:scale-[.99] transition; }
.btn-danger { @apply rounded-xl border border-rose-300 px-3 py-1.5 text-sm font-medium text-rose-700 hover:bg-rose-50 active:scale-[.99] transition; }
.row { @apply flex items-center justify-between gap-4; }
.btn-ghost { @apply rounded-xl border border-indigo-300 px-3 py-2 text-xs font-semibold
             text-indigo-700 hover:bg-indigo-50 active:scale-[.99] transition
             whitespace-nowrap; }
</style>

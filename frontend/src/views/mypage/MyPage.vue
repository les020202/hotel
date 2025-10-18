<!-- src/views/mypage/MyPage.vue -->
<template>
  <div class="mypage">
    <div class="header-spacer" />

    <!-- 🔹 커버 영역 (업로드/템플릿 선택 가능) -->
    <div
      class="cover"
      :style="coverStyle"
      @click="openCoverModal"
      title="클릭해서 커버 이미지 변경"
    >
      <button class="cover-btn" type="button" @click.stop="openCoverModal">
        Upload new cover
      </button>
    </div>

    <!-- 🔹 프로필 영역 (아바타 이미지 + 이름) -->
    <div class="profile">
      <div
        class="avatar"
        :style="avatarStyle"
        @click="openAvatarModal"
        title="클릭해서 프로필 변경"
      >
        <span v-if="!avatarUrl" class="avatar-fallback" />
      </div>
      <div class="who">
        <div class="name">{{ me?.name || "Guest" }}</div>
      </div>
    </div>

    <!-- 🔹 마이페이지 탭 네비게이션 (결제수단 제거) -->
    <nav class="tabs">
      <RouterLink to="/mypage/account">계정</RouterLink>
      <RouterLink to="/mypage/history">내역</RouterLink>
      <RouterLink to="/mypage/my-reviews">내 리뷰</RouterLink>
      <RouterLink to="/mypage/support">고객지원</RouterLink>
      <RouterLink to="/mypage/coupon">쿠폰함</RouterLink>
    </nav>

    <!-- 🔹 탭별 내용 표시 영역 -->
    <div class="tab-content">
      <RouterView />
    </div>

    <div class="footer-spacer" />

    <!-- 🔹 커버 변경 모달 -->
    <div v-if="coverModal" class="modal" @click.self="coverModal = false">
      <div class="dialog">
        <h3>커버 선택</h3>
        <div class="choices">
          <!-- 기본 템플릿 3종 -->
          <button class="choice" @click="chooseCoverTemplate('C1')">
            <img :src="COVER_SRC.C1" alt="C1" />
            <div>C1</div>
          </button>
          <button class="choice" @click="chooseCoverTemplate('C2')">
            <img :src="COVER_SRC.C2" alt="C2" />
            <div>C2</div>
          </button>
          <button class="choice" @click="chooseCoverTemplate('C3')">
            <img :src="COVER_SRC.C3" alt="C3" />
            <div>C3</div>
          </button>
        </div>
        <!-- 직접 업로드 -->
        <div class="uploader">
          <label class="upload-btn">
            직접 업로드
            <input type="file" accept="image/*" hidden @change="uploadCover" />
          </label>
          <button class="close" @click="coverModal = false">닫기</button>
        </div>
      </div>
    </div>

    <!-- 🔹 아바타 변경 모달 -->
    <div v-if="avatarModal" class="modal" @click.self="avatarModal = false">
      <div class="dialog">
        <h3>프로필 선택</h3>
        <div class="choices">
          <!-- 기본 템플릿 3종 -->
          <button class="choice" @click="chooseAvatarTemplate('T1')">
            <img :src="AVATAR_SRC.T1" alt="T1" />
            <div>T1</div>
          </button>
          <button class="choice" @click="chooseAvatarTemplate('T2')">
            <img :src="AVATAR_SRC.T2" alt="T2" />
            <div>T2</div>
          </button>
          <button class="choice" @click="chooseAvatarTemplate('T3')">
            <img :src="AVATAR_SRC.T3" alt="T3" />
            <div>T3</div>
          </button>
        </div>
        <!-- 직접 업로드 -->
        <div class="uploader">
          <label class="upload-btn">
            직접 업로드
            <input type="file" accept="image/*" hidden @change="uploadAvatar" />
          </label>
          <button class="close" @click="avatarModal = false">닫기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import api, { getMe } from "@/api/auth";
import { ref, computed, onMounted } from "vue";

/* 🔹 이미지 템플릿 매핑 (src/assets) */
const COVER_SRC = {
  C1: new URL("@/assets/covers/C1.jpg", import.meta.url).href,
  C2: new URL("@/assets/covers/C2.jpg", import.meta.url).href,
  C3: new URL("@/assets/covers/C3.jpg", import.meta.url).href,
};
const AVATAR_SRC = {
  T1: new URL("@/assets/avatars/T1.png", import.meta.url).href,
  T2: new URL("@/assets/avatars/T2.jpg", import.meta.url).href,
  T3: new URL("@/assets/avatars/T3.jpg", import.meta.url).href,
};

/* ───────────────────────────────────────────────────────────────
   업로드 보안 유틸 (SVG 차단, 확장자/용량 체크, 이미지 유효성)
   ─────────────────────────────────────────────────────────────── */
const MAX_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_EXT = new Set(["png", "jpg", "jpeg", "webp"]);
const BLOCKED_MIME = new Set(["image/svg+xml"]);

function getExt(name = "") {
  const m = String(name).toLowerCase().match(/\.([a-z0-9]+)$/);
  return m ? m[1] : "";
}

async function validateImageFile(file) {
  if (!file) throw new Error("파일이 없습니다.");
  if (BLOCKED_MIME.has(file.type)) throw new Error("SVG는 허용되지 않습니다.");
  const ext = getExt(file.name);
  if (!ALLOWED_EXT.has(ext)) throw new Error("PNG/JPG/WebP만 업로드 가능합니다.");
  if (file.size > MAX_SIZE) throw new Error("파일 크기가 5MB를 초과했습니다.");

  // 브라우저 지원 시 실제 이미지 여부 간단 확인
  try {
    await createImageBitmap(file);
  } catch {
    throw new Error("이미지 파일이 손상되었거나 지원되지 않습니다.");
  }
  return true;
}

async function doUploadTo(url, file) {
  const fd = new FormData();
  fd.append("file", file);
  await api.post(url, fd, { headers: { "Content-Type": "multipart/form-data" } });
}

/* 상태 관리 */
const me = ref(null);
const coverModal = ref(false);
const avatarModal = ref(false);

/* 사용자 정보 로드 */
async function loadMe() {
  try {
    me.value = await getMe();
  } catch (e) {
    console.error(e);
  }
}
onMounted(loadMe);

/* 🔹 프로필 이미지 표시 */
const avatarUrl = computed(() => {
  if (!me.value) return null;
  if (me.value.profileImageType === "UPLOADED" && me.value.profileImageUrl)
    return me.value.profileImageUrl;
  if (me.value.profileImageType === "TEMPLATE" && me.value.profileImageTemplate)
    return AVATAR_SRC[me.value.profileImageTemplate] || null;
  return null;
});
const avatarStyle = computed(() =>
  avatarUrl.value
    ? {
        backgroundImage: `url(${avatarUrl.value})`,
        backgroundSize: "cover",
        backgroundPosition: "center",
      }
    : {}
);

/* 🔹 커버 이미지 표시 */
const coverStyle = computed(() => {
  if (!me.value) return {};
  if (me.value.coverImageType === "UPLOADED" && me.value.coverImageUrl) {
    return {
      backgroundImage: `url(${me.value.coverImageUrl})`,
      backgroundSize: "cover",
      backgroundPosition: "center",
    };
  }
  if (me.value.coverImageType === "TEMPLATE" && me.value.coverImageTemplate) {
    const src = COVER_SRC[me.value.coverImageTemplate];
    if (src)
      return {
        backgroundImage: `url(${src})`,
        backgroundSize: "cover",
        backgroundPosition: "center",
      };
  }
  // 기본 배경
  return { background: "linear-gradient(60deg,#0c7a66,#f9a43a 55%,#ffd86b)" };
});

/* 🔹 모달 열기 */
function openCoverModal() {
  coverModal.value = true;
}
function openAvatarModal() {
  avatarModal.value = true;
}

/* 🔹 템플릿 적용 */
async function chooseCoverTemplate(code) {
  try {
    await api.put("/users/me/cover/template", { template: code });
    coverModal.value = false;
    await loadMe();
  } catch (e) {
    console.error(e);
    alert("커버 템플릿 적용 실패");
  }
}
async function chooseAvatarTemplate(code) {
  try {
    await api.put("/users/me/profile/template", { template: code });
    avatarModal.value = false;
    await loadMe();
  } catch (e) {
    console.error(e);
    alert("프로필 템플릿 적용 실패");
  }
}

/* 🔹 파일 업로드 (강화된 검증 포함) */
async function uploadCover(e) {
  const file = e.target.files?.[0];
  try {
    await validateImageFile(file);
    await doUploadTo("/users/me/cover/upload", file);
    coverModal.value = false;
    await loadMe();
  } catch (err) {
    alert(err?.message || "커버 업로드 실패");
  } finally {
    e.target.value = "";
  }
}
async function uploadAvatar(e) {
  const file = e.target.files?.[0];
  try {
    await validateImageFile(file);
    await doUploadTo("/users/me/profile/upload", file);
    avatarModal.value = false;
    await loadMe();
  } catch (err) {
    alert(err?.message || "프로필 업로드 실패");
  } finally {
    e.target.value = "";
  }
}
</script>

<style scoped>
.mypage {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  box-sizing: border-box;
}
.header-spacer { height: 40px; }
.footer-spacer { height: 60px; }

/* 🔹 커버 영역 스타일 */
.cover {
  height: 240px;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  background: #f3f4f6;
  cursor: pointer;
  z-index: 2;
}
.cover-btn {
  position: absolute;
  right: 16px;
  bottom: 14px;
  padding: 10px 14px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  z-index: 5;
}

/* 🔹 프로필 영역 */
.profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: -48px;
  margin-bottom: 8px;
  position: relative;
  z-index: 3;
  pointer-events: none;
}
.avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 4px solid #fff;
  background: #e5e7eb;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  cursor: pointer;
  pointer-events: auto;
}
.avatar-fallback {
  display: block;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 30% 30%, #d1fae5, #60a5fa);
  border-radius: 50%;
}
.who { text-align: center; }
.name {
  font-size: 20px;
  font-weight: 700;
  color: #111;
}

/* 🔹 탭 네비게이션 */
.tabs {
  display: flex;
  gap: 16px;
  border-bottom: 1px solid #e5e7eb;
  margin-top: 16px;
  flex-wrap: wrap;
  justify-content: center;
}
.tabs a {
  padding: 10px 6px;
  text-decoration: none;
  color: #333;
  font-weight: 500;
}
.tabs a.router-link-active {
  color: #0a6;
  border-bottom: 2px solid #0a6;
}

/* 🔹 내용 영역 높이 */
.tab-content {
  min-height: 640px;
  padding: 18px 0;
}

/* 🔹 모달 공통 */
.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.38);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}
.dialog {
  width: 720px;
  max-width: 92vw;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}
.dialog h3 {
  margin: 0 0 12px;
  font-size: 18px;
}
.choices {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}
.choice {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
}
.choice img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}
.uploader {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.upload-btn {
  padding: 10px 14px;
  border-radius: 8px;
  background: #0a6;
  color: #fff;
  cursor: pointer;
  border: 0;
}
.close {
  padding: 10px 14px;
  border-radius: 8px;
  background: #fff;
  color: #111;
  border: 1px solid #d1d5db;
  cursor: pointer;
}
</style>

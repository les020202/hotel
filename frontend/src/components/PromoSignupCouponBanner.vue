<template>
    <section
      class="promo"
      role="region"
      aria-label="회원가입 즉시 10,000원 쿠폰 지급 프로모션"
    >
      <!-- 왼쪽 : 카피 & CTA -->
      <div class="promo-left">
        <h3 class="headline">
          <span class="only">회원가입 시</span>
          <br />
          즉시 <span class="price">₩{{ fmt }}</span> 쿠폰 지급
        </h3>
        <p class="sub">
          지금 가입하면 첫 예약에 바로 사용할 수 있어요.
        </p>
  
        <div class="ctas">
          <button class="btn primary" @click="goSignup">회원가입 / 로그인</button>
          <button class="btn ghost" @click="goMore">혜택 자세히 보기</button>
        </div>
      </div>
  
      <!-- 오른쪽 : 일러스트(쿠폰 + 리본/별) -->
      <div class="promo-art" aria-hidden="true">
        <div class="coupon">
          <span class="tag">COUPON</span>
          <div class="ticket">
            <span class="money">₩{{ fmt }}</span>
            <span class="cut"></span>
          </div>
        </div>
  
        <!-- 약간의 장식 -->
        <i class="star s1"></i>
        <i class="star s2"></i>
        <i class="star s3"></i>
        <i class="spark k1"></i>
        <i class="spark k2"></i>
      </div>
    </section>
  </template>
  
  <script setup>
  import { computed } from 'vue'
  import { useRouter } from 'vue-router'
  
  /** 금액은 필요 시 props로 뺄 수 있어요 */
  const amount = 10000
  const fmt = computed(() => amount.toLocaleString('ko-KR'))
  
  const router = useRouter()
  function goSignup(){ router.push('/signup') }
  function goMore(){ router.push('/event/coupon') } // 적절한 경로로 바꿔주세요
  </script>
  
  <style scoped>
  /* 래퍼 */
  .promo{
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: 24px;
    align-items: center;
  
    background: linear-gradient(135deg, #F1EEFF 0%, #EFEFFF 60%, #F6F7FF 100%);
    border: 1px solid #ecebff;
    border-radius: 18px;
    padding: 26px 28px;
    position: relative;
    overflow: hidden;
  }
  
  /* 왼쪽 카피 */
  .headline{
    margin: 0 0 6px;
    font-size: 26px;
    line-height: 1.25;
    color: #111827;
    letter-spacing: -0.2px;
  }
  .headline .only{ font-weight: 800; }
  .headline .price{
    display: inline-block;
    background: linear-gradient(90deg, #6b58ff 0%, #9b67ff 40%, #ff64c6 100%);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    font-weight: 900;
  }
  
  .sub{
    margin: 4px 0 14px;
    color: #4b5563;
    font-size: 14px;
  }
  
  .ctas{ display:flex; gap:10px; }
  .btn{
    height: 40px; padding: 0 14px;
    border-radius: 10px;
    font-size: 14px;
    cursor: pointer;
  }
  .btn.primary{
    background:#111; color:#fff; border:1px solid #111;
  }
  .btn.primary:hover{ opacity: .9; }
  .btn.ghost{
    background: #fff; color:#111; border:1px solid #e5e7eb;
  }
  
  /* 오른쪽 일러스트 */
  .promo-art{
    position: relative;
    height: 140px;
  }
  .coupon{
    position:absolute; right: 8px; bottom: 6px;
    transform: rotate(-8deg);
    filter: drop-shadow(0 10px 18px rgba(109, 89, 255, .28));
  }
  .tag{
    display:inline-block;
    padding: 6px 10px;
    background:#1f2a7a;
    color:#fff;
    font-weight: 700;
    font-size: 12px;
    border-radius: 999px;
    transform: translate(12px, 8px) rotate(7deg);
  }
  .ticket{
    position: relative;
    min-width: 220px;
    height: 110px;
    border-radius: 18px;
    background: linear-gradient(145deg, #7a65ff 0%, #6b58ff 40%, #5a49ff 100%);
    display:flex; align-items:center; justify-content:center;
  }
  .ticket::before,
  .ticket::after{
    content:"";
    position:absolute; top:50%; width:22px; height:22px; border-radius:50%;
    background: #F1EEFF; transform: translateY(-50%);
  }
  .ticket::before{ left:-11px; }
  .ticket::after { right:-11px; }
  
  .money{
    color:#fff; font-size:28px; font-weight:900; letter-spacing: .4px;
    text-shadow: 0 2px 0 rgba(0,0,0,.1);
  }
  .cut{
    position:absolute; inset:10px 0 auto;
    height: 1px; background: repeating-linear-gradient(
      to right, rgba(255,255,255,.8), rgba(255,255,255,.8) 8px, transparent 8px, transparent 16px
    );
    opacity:.7;
  }
  
  /* 장식 */
  .star, .spark{
    position:absolute; display:block;
    opacity:.85;
  }
  .star{
    width: 14px; height: 14px;
    background: radial-gradient(circle at 40% 40%, #ffd46b, #ff9f37);
    clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
    filter: drop-shadow(0 4px 8px rgba(255, 163, 60, .28));
  }
  .s1{ right: 160px; top: 12px; transform: scale(1.1) rotate(8deg); }
  .s2{ right: 36px; top: -4px;  transform: scale(0.9) rotate(-12deg); }
  .s3{ right: 96px; top: 60px; transform: scale(0.8) rotate(16deg); }
  
  .spark{
    width: 10px; height: 10px; border-radius: 2px;
    background:#ff7ab3; transform: rotate(35deg);
    filter: drop-shadow(0 4px 8px rgba(255, 122, 179, .25));
  }
  .k1{ right: 130px; bottom: 110px; }
  .k2{ right: 34px;  bottom: 90px; }
  
  /* 반응형 */
  @media (max-width: 900px){
    .promo{ grid-template-columns: 1fr; }
    .promo-art{ height: 120px; }
  }
  </style>
  
<!-- src/views/admin/support/SupportLayout.vue -->
<template>
    <section class="wrap">
      <div class="hero">
        <div>
          <h2>고객 지원</h2>
          <p>공지사항 · 자주 묻는 질문 · 고객 문의</p>
        </div>
  
        <div class="tabs">
          <RouterLink class="tab" :class="{on: $route.path.endsWith('/notices')}" to="/admin/support/notices">공지사항</RouterLink>
          <RouterLink class="tab" :class="{on: $route.path.endsWith('/faqs')}"    to="/admin/support/faqs">자주묻는 질문</RouterLink>
          <RouterLink class="tab" :class="{on: $route.path.endsWith('/tickets')}" to="/admin/support/tickets">문의</RouterLink>
        </div>
      </div>
  
      <router-view />
    </section>
  </template>
  
  <script setup>
  </script>
  
  <style scoped>
  .wrap{ padding:14px }
  .hero{
    display:flex; align-items:center; justify-content:space-between;
    padding:16px 18px; border-radius:16px;
    background:linear-gradient(135deg,#f7faff,#f0f6ff);
    border:1px solid #eaf0ff; margin-bottom:12px;
  }
  .hero h2{ margin:0; font-size:18px; font-weight:800 }
  .hero p{ margin:4px 0 0; color:#6b7280; font-size:12px }
  .tabs{ display:flex; gap:8px }
  .tab{
    display:inline-flex; align-items:center; justify-content:center;
    height:34px; padding:0 14px; border-radius:18px;
    border:1px solid #cfe0ff; background:#fff; font-weight:700;
  }
  .tab.on{ background:#2563eb10; border-color:#2563eb40 }
  </style>
  
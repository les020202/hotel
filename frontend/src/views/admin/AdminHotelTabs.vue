<template>
    <div class="seg-wrap">
      <router-link
        :to="{ name: 'AdminHotels' }"
        class="seg"
        :class="{ active: $route.name === 'AdminHotels' }"
      >호텔 관리</router-link>
  
      <router-link
        :to="{ name: 'AdminHotelAudit' }"
        class="seg"
        :class="{ active: $route.name === 'AdminHotelAudit' }"
      >호텔 심사</router-link>
    </div>
  </template>
  
  <script setup></script>
  
  <style scoped>
  .seg-wrap{
    display:flex; gap:8px; background:#f3f6ff; padding:6px; border-radius:999px;
    border:1px solid #e5ecff;
  }
  .seg{
    padding:8px 14px; border-radius:999px; color:#1f2a37; text-decoration:none;
    font-weight:700; background:#fff; border:1px solid #e5ecff; transition:.12s;
  }
  .seg:hover{ background:#f7faff }
  .seg.active{ background:#111827; border-color:#111827; color:#fff }
  </style>
  
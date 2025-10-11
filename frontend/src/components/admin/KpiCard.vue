<!-- src/components/admin/KpiCard.vue -->
<template>
  <div
    class="card kpi"
    :role="onClick ? 'button' : null"
    :tabindex="onClick ? 0 : null"
    @click="onClick && onClick()"
    @keyup.enter.space="onClick && onClick()"
  >
    <p class="label">{{ label }}</p>
    <h3 class="value">{{ String(value ?? '') }}</h3>
  </div>
</template>

<script setup>
const { label, value, onClick } = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '' },
  onClick: { type: Function } // optional
})
</script>

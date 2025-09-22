<template>
    <div class="bg-white border rounded-xl p-4">
    <p class="text-sm text-gray-500">{{ label }}</p>
    <div class="mt-1 text-2xl font-bold">{{ value }}<span v-if="suffix" class="text-base font-medium text-gray-500">{{ suffix }}</span></div>
    <p v-if="delta !== undefined" class="text-xs mt-1" :class="delta >= 0 ? 'text-green-600' : 'text-red-600'">
    {{ delta >= 0 ? '▲' : '▼' }} {{ Math.abs(delta) }}% 전주 대비
    </p>
    </div>
    </template>
    <script setup>
    defineProps(['label','value','suffix','delta'])
    </script>
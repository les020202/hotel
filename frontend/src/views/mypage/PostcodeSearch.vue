<template>
    <div class="pc-modal" @click.self="$emit('close')">
      <div class="pc-dialog">
        <div ref="wrap" class="pc-wrap"></div>
        <button class="pc-close" @click="$emit('close')">닫기</button>
      </div>
    </div>
  </template>
  
  <script setup>
  import { onMounted, ref } from 'vue'
  import { loadDaumPostcode } from '@/utils/loadDaumPostcode'
  
  const emit = defineEmits(['select','close'])
  const wrap = ref(null)
  
  onMounted(async () => {
    const daum = await loadDaumPostcode()
    new daum.Postcode({
      oncomplete: (data) => {
        const roadAddr = data.roadAddress
        const jibunAddr = data.jibunAddress
        // 도로명 보조정보(법정동/건물명) 조합
        const extraRoad = [
          (data.bname && /[동|로|가]$/.test(data.bname)) ? data.bname : '',
          data.buildingName || ''
        ].filter(Boolean).join(', ')
        const address1 = extraRoad ? `${roadAddr} (${extraRoad})` : (roadAddr || jibunAddr)
  
        emit('select', {
          zonecode: data.zonecode,       // 5자리
          roadAddress: roadAddr,
          jibunAddress: jibunAddr,
          address1
        })
        emit('close')
      },
      onresize: size => { if (wrap.value) wrap.value.style.height = size.height + 'px' },
      width: '100%',
      height: '100%'
    }).embed(wrap.value)
  })
  </script>
  
  <style scoped>
  .pc-modal{position:fixed; inset:0; background:rgba(0,0,0,.4); display:flex; align-items:center; justify-content:center; z-index:2000}
  .pc-dialog{ background:#fff; border-radius:10px; width:min(600px,95vw); height:480px; position:relative; overflow:hidden; display:flex; flex-direction:column }
  .pc-wrap{ flex:1; }
  .pc-close{ position:absolute; right:8px; top:8px; padding:6px 10px; border:1px solid #e5e7eb; background:#fff; border-radius:8px; cursor:pointer }
  </style>
  
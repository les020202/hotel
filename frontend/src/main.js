// main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import './assets/tailwind.css'

// ★ 관리자 경로이면 <body>에 .admin 클래스 토글
router.afterEach((to) => {
  const isAdmin = to.path.startsWith('/admin')  // 필요하면 meta.admin으로 바꿔도 됨
  document.body.classList.toggle('admin', isAdmin)
})

createApp(App).use(router).mount('#app')

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/main.css'   // 템플릿에 있을 경우만
import './assets/tailwind.css'

createApp(App).use(router).mount('#app')

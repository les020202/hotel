import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url' // ✅ ESM-safe (추천)

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)), // ESM-safe 방식
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // 로컬에서 테스트할 때는 localhost:8888
      '/api':     { target: 'http://localhost:8888', changeOrigin: true },
      '/oauth2':  { target: 'http://localhost:8888', changeOrigin: true },
      '/files':   { target: 'http://localhost:8888', changeOrigin: true },
      '/confirm': { target: 'http://localhost:8888', changeOrigin: true },
      '/logout':  { target: 'http://localhost:8888', changeOrigin: true },
      // 필요하다면 원격 서버용 주소도 추가 가능
      // '/api':     { target: 'http://172.16.15.59:8888', changeOrigin: true },
      // '/oauth2':  { target: 'http://172.16.15.59:8888', changeOrigin: true },
      // '/files':   { target: 'http://172.16.15.59:8888', changeOrigin: true },
      // '/confirm': { target: 'http://172.16.15.59:8888', changeOrigin: true },
      // '/logout':  { target: 'http://172.16.15.59:8888', changeOrigin: true },
    },
  },
})

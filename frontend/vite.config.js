import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url' // ESM-safe 방식 (추천)

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // ESM-safe 방식 사용
      '@': fileURLToPath(new URL('./src', import.meta.url)), 
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // 로컬 테스트용 proxy 설정
      '/api': { target: 'http://localhost:8888', changeOrigin: true },
      '/oauth2': { target: 'http://localhost:8888', changeOrigin: true },
      '/files': { target: 'http://localhost:8888', changeOrigin: true },
      '/uploads': { target: 'http://localhost:8888', changeOrigin: true }, 
      '/confirm': { target: 'http://localhost:8888', changeOrigin: true },
      '/logout': { target: 'http://localhost:8888', changeOrigin: true },
    },
  },
})

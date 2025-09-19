import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)), // ✅ ESM-safe
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api':     { target: 'http://172.16.15.59:8888', changeOrigin: true },
      '/oauth2':  { target: 'http://172.16.15.59:8888', changeOrigin: true },
      '/confirm': { target: 'http://172.16.15.59:8888', changeOrigin: true },
      // (선택) 로그아웃도 서버로 보낼 거면
      '/logout':  { target: 'http://172.16.15.59:8888', changeOrigin: true },
    },
  },
})

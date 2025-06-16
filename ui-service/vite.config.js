import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0', // 네트워크 접근 허용 추가
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // API Gateway
        changeOrigin: true,
        secure: false,
        // rewrite 제거 - 경로 그대로 유지
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('🔴 Proxy error:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('📤 Proxying request:', req.method, req.url, '→', proxyReq.path);
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('📥 Proxy response:', proxyRes.statusCode, req.url);
          });
        },
      },
      '/auth': {
        target: 'http://localhost:8080', // API Gateway
        changeOrigin: true,
        secure: false,
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('🔴 Auth proxy error:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('📤 Auth proxying request:', req.method, req.url, '→', proxyReq.path);
          });
        },
      }
    }
  }
})
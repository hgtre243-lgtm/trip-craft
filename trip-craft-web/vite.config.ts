import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
// export default defineConfig({
//   plugins: [vue()],
// })

export default defineConfig({
    plugins: [vue()],
    server: {
        host: '0.0.0.0',  // 允许局域网访问
        port: 5173,
        proxy: {
            // 只要前端请求以 /api 开头，Vite 会自动帮我们转发给 8080 端口的 Spring Boot
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    }
})

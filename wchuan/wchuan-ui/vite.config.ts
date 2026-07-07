import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // 必须引入 path 模块

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
    resolve: {
        // 核心配置：定义别名 @ 永远代表 src 的绝对路径。
        alias: {
            '@': path.resolve(__dirname, 'src')
        }
    },
    server: { // 开发服务器代理
        proxy: {
            '/api': {
                target: 'http://localhost:8080', // 开发环境连本地后端
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '') // 把 /api 替换掉再发给后端
            }
        }
    }
})


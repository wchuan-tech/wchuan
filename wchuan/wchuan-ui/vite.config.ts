import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // 必须引入 path 模块

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
    resolve: {
        // 核心配置：定义别名
        alias: {
            '@': path.resolve(__dirname, 'src')
        }
    }
})

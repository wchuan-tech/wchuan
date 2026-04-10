import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)

// 使用同一个实例进行配置
app.use(router)
app.use(ElementPlus)
// 挂载这个已经配置好路由的实例
app.mount('#app')
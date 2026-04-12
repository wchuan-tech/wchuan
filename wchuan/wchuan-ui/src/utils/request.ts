import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { type Result } from '../api/types'
import {ElMessage} from "element-plus";
import router from "../router";

const request = axios.create({
    baseURL:'http://localhost:8080', // 指向后端地址
    timeout: 5000
})

// 请求拦截器
request.interceptors.request.use(
    (config: InternalAxiosRequestConfig) =>{
    const token = localStorage.getItem('token') // 从浏览器缓存拿 Token
    if(token && config.headers){
        config.headers['token'] = token // Header 的键名 'token' 必须和后端 JwtFilter 里的获取名一致
    }
    // 处理完请求配置 返回给 axios
    return config
},error => {
    return Promise.reject(error)
})

request.interceptors.response.use(
    (response: AxiosResponse<Result>): any => {
        // 1. 提取后端返回的业务数据
        const res = response.data;

        // 2. 统一处理业务层级的 401
        if (res.code === 401) {
            ElMessage.error("登录已过期，请重新登录");
            handleUnauthorized();
            return Promise.reject(res); // 抛出异常，阻止页面继续执行业务逻辑
        }

        // 3. 其他非 200 的业务错误可以在这里统一提示
        if (res.code !== 200) {
            ElMessage.error(res.msg || '系统错误');
            return Promise.reject(res);
        }

        return res;
    },
    (error) => {
        // 4. 处理 HTTP 异常状态码 (4xx, 5xx)
        const { response } = error;

        if (response) {
            switch (response.status) {
                case 401:
                    handleUnauthorized();
                    break;
                case 403:
                    ElMessage.error("权限不足，拒绝访问");
                    break;
                case 500:
                    ElMessage.error("服务器内部错误");
                    break;
                default:
                    ElMessage.error(response.data?.msg || "网络请求异常");
            }
        } else {
            // 处理网络中断或超时
            ElMessage.error("网络连接超时，请检查您的网络");
        }

        return Promise.reject(error);
    }
);

/**
 * 抽离统一的“未授权/登录失效”处理逻辑
 */
function handleUnauthorized() {
    // 检查本地是否有 Token，防止多次并发请求导致弹出多个提示
    if (localStorage.getItem('token')) {
        ElMessage.error("登录已过期，请重新登录");
        localStorage.removeItem('token');
    }
    // 强制跳转  跳转到登录页，并且不留下历史记录（不能点返回键回来）
    router.replace('/login');
}


export default request
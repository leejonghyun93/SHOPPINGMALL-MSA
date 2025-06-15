import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'


import '@/assets/css/main.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import { setUserFromToken } from '@/stores/userStore'

// 페이지 로드 시 기존 토큰 확인
const token = localStorage.getItem('token')
if (token) {
    setUserFromToken(token)
}


const app = createApp(App)

// axios를 전역으로 사용할 수 있도록 설정
app.config.globalProperties.$axios = axios

app.use(router).mount('#app')
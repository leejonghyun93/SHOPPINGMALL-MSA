import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

import '@/assets/css/main.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import { initializeUser, user, resetUser, isTokenValid } from '@/stores/userStore'

// 페이지 로드 시 사용자 정보 초기화 (토큰 유효성 검사 포함)
initializeUser()

const app = createApp(App)

// axios를 전역으로 사용할 수 있도록 설정
app.config.globalProperties.$axios = axios

// axios 인터셉터 설정 - 요청 시 자동으로 Authorization 헤더 추가
axios.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token && user.id && isTokenValid(token)) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 중요한 인증 API들 (이런 API에서 401이 나면 실제 인증 문제)
const CRITICAL_AUTH_ENDPOINTS = [
    '/api/users/profile',
    '/api/orders/checkout',
    '/api/auth/verify',
    '/api/users/me'
]

// axios 인터셉터 설정 - 401 에러 시 선별적 로그아웃
axios.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            const requestUrl = error.config?.url || ''

            // 중요한 인증 API에서만 자동 로그아웃 처리
            const isCriticalEndpoint = CRITICAL_AUTH_ENDPOINTS.some(endpoint =>
                requestUrl.includes(endpoint)
            )

            if (isCriticalEndpoint) {
                console.log('중요한 인증 API에서 401 에러 발생, 로그아웃 처리:', requestUrl)
                resetUser()

                // 로그인 페이지로 리다이렉트 (선택사항)
                if (router.currentRoute.value.path !== '/login') {
                    router.push('/login')
                }
            } else {
                console.log('일반 API에서 401 에러 발생, 로그아웃하지 않음:', requestUrl)
                // 로그아웃하지 않고 에러만 전달
            }
        }
        return Promise.reject(error)
    }
)

app.use(router).mount('#app')
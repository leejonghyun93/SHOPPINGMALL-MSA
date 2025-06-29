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

// 🔥 에러 페이지로 리다이렉트하는 함수 추가
const redirectToErrorPage = (status, message, details) => {
    // 현재 페이지가 이미 에러 페이지라면 리다이렉트하지 않음
    if (router.currentRoute.value.path.startsWith('/error/')) {
        return
    }

    console.log(`🔄 에러 페이지로 리다이렉트: ${status}`)

    router.push({
        name: 'ErrorPage',
        params: { code: status },
        query: {
            message: message || '',
            details: details || ''
        }
    }).catch(err => {
        // 라우팅 실패 시 직접 URL 변경
        console.warn('라우터 푸시 실패, 직접 URL 변경:', err)
        window.location.href = `/error/${status}`
    })
}

// 🔥 에러 상태 코드별 처리 여부 결정
const shouldShowErrorPage = (status) => {
    // 300번대 - 리다이렉트는 브라우저가 자동 처리하므로 에러 페이지 표시 안 함
    if (status >= 300 && status < 400) {
        return false
    }

    // 401은 기존 로그인 처리를 우선함
    if (status === 401) {
        return false
    }

    // 400번대, 500번대는 에러 페이지 표시
    return status >= 400
}

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

// 🔥 axios 인터셉터 설정 - 401 에러 시 선별적 로그아웃 + 에러 페이지 기능 추가
axios.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status
        const requestUrl = error.config?.url || ''
        const message = error.response?.data?.message || error.message
        const serverData = error.response?.data

        console.log(`🚨 API 에러 발생: ${status} - ${requestUrl}`)

        // 🔥 기존 401 에러 처리 로직 유지
        if (status === 401) {
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
        // 🔥 새로 추가: 다른 에러들은 에러 페이지로 리다이렉트
        else if (status && shouldShowErrorPage(status)) {
            // 에러 상세 정보 수집
            const errorDetails = JSON.stringify({
                url: requestUrl,
                status: status,
                message: message,
                serverData: serverData,
                timestamp: new Date().toISOString(),
                userAgent: navigator.userAgent
            }, null, 2)

            // 에러 페이지로 리다이렉트
            redirectToErrorPage(status, message, errorDetails)
        }
        // 🔥 네트워크 에러 처리
        else if (!error.response) {
            console.log('🔄 네트워크 에러 - 502 에러 페이지로 리다이렉트')

            const networkErrorDetails = JSON.stringify({
                error: 'Network Error',
                url: requestUrl,
                message: 'Failed to connect to server',
                timestamp: new Date().toISOString()
            }, null, 2)

            redirectToErrorPage(502, '네트워크 연결에 문제가 발생했습니다', networkErrorDetails)
        }

        return Promise.reject(error)
    }
)

app.use(router).mount('#app')
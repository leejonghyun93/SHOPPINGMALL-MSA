import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

import '@/assets/css/main.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import { initializeUser, user, resetUser, isTokenValid } from '@/stores/userStore'

initializeUser()

const app = createApp(App)

app.config.globalProperties.$axios = axios

const redirectToErrorPage = (status, message, details) => {
    if (router.currentRoute.value.path.startsWith('/error/')) {
        return
    }

    router.push({
        name: 'ErrorPage',
        params: { code: status },
        query: {
            message: message || '',
            details: details || ''
        }
    }).catch(err => {
        window.location.href = `/error/${status}`
    })
}

const shouldShowErrorPage = (status) => {
    if (status >= 300 && status < 400) {
        return false
    }

    if (status === 401) {
        return false
    }

    return status >= 400
}

axios.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('jwt')
        if (token && user.id && isTokenValid(token)) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

const CRITICAL_AUTH_ENDPOINTS = [
    '/api/users/profile',
    '/api/orders/checkout',
    '/api/auth/verify',
    '/api/users/me'
]

axios.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status
        const requestUrl = error.config?.url || ''
        const message = error.response?.data?.message || error.message
        const serverData = error.response?.data

        if (status === 401) {
            const isCriticalEndpoint = CRITICAL_AUTH_ENDPOINTS.some(endpoint =>
                requestUrl.includes(endpoint)
            )

            if (isCriticalEndpoint) {
                resetUser()

                if (router.currentRoute.value.path !== '/login') {
                    router.push('/login')
                }
            }
        }
        else if (status && shouldShowErrorPage(status)) {
            const errorDetails = JSON.stringify({
                url: requestUrl,
                status: status,
                message: message,
                serverData: serverData,
                timestamp: new Date().toISOString(),
                userAgent: navigator.userAgent
            }, null, 2)

            redirectToErrorPage(status, message, errorDetails)
        }
        else if (!error.response) {
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
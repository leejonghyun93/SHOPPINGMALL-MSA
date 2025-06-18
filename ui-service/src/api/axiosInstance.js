// axiosInstance.js
import axios from 'axios'

// 🔥 프록시 사용하므로 baseURL 제거
const apiClient = axios.create({
    // baseURL 제거 - Vite 프록시가 '/api' 요청을 Gateway로 전달
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
})

// 🔥 요청 인터셉터
apiClient.interceptors.request.use(
    (config) => {
        console.log('🚀 Request Config:', {
            url: config.url,
            method: config.method,
            withAuth: config.withAuth,
            headers: config.headers
        })

        // withAuth가 false인 경우 Authorization 헤더 제거
        if (config.withAuth === false) {
            delete config.headers.Authorization
            console.log('🔓 Public API 호출 - Authorization 헤더 제거')
        } else {
            // 🔥 수정: 'authToken' → 'token'으로 변경
            const token = localStorage.getItem('token')
            if (token) {
                config.headers.Authorization = `Bearer ${token}`
                console.log('🔐 JWT 토큰 추가:', token.substring(0, 20) + '...')
            } else {
                console.log('🔓 JWT 토큰 없음')
            }
        }

        return config
    },
    (error) => {
        console.error('❌ Request Error:', error)
        return Promise.reject(error)
    }
)

// 🔥 응답 인터셉터
apiClient.interceptors.response.use(
    (response) => {
        console.log('✅ Response Success:', {
            status: response.status,
            url: response.config.url,
            dataType: typeof response.data,
            dataLength: Array.isArray(response.data) ? response.data.length : 'N/A'
        })

        return response
    },
    (error) => {
        console.error('❌ Response Error:', {
            status: error.response?.status,
            url: error.config?.url,
            message: error.message,
            data: error.response?.data
        })

        if (error.message === 'Network Error') {
            console.error('🌐 프록시 연결 오류: Gateway 서버 상태를 확인하세요')
        }

        if (error.response?.status === 401) {
            console.warn('🚫 인증 실패')
            // 🔥 수정: 'authToken' → 'token'으로 변경
            localStorage.removeItem('token')
        }

        return Promise.reject(error)
    }
)

export default apiClient
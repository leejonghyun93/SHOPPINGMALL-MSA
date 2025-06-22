// @/api/axiosInstance.js
import axios from 'axios'

// Axios 인스턴스 생성
const apiClient = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 요청 인터셉터 - 모든 요청에 토큰 자동 추가
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')

        // 🔥 중요: withAuth가 false가 아닌 경우 토큰 추가 (기본값은 true)
        if (config.withAuth !== false && token) {
            config.headers.Authorization = `Bearer ${token}`
            console.log('🔐 토큰 헤더 추가:', `Bearer ${token.substring(0, 20)}...`)
        }

        // withAuth 속성 제거 (서버로 전송되지 않도록)
        delete config.withAuth

        console.log('📡 API 요청:', {
            method: config.method?.toUpperCase(),
            url: config.url,
            hasAuth: !!config.headers.Authorization,
            data: config.data
        })

        return config
    },
    (error) => {
        console.error('❌ 요청 인터셉터 에러:', error)
        return Promise.reject(error)
    }
)

// 응답 인터셉터 - 401 에러 시 자동 로그아웃
apiClient.interceptors.response.use(
    (response) => {
        console.log('✅ API 응답:', {
            status: response.status,
            url: response.config.url,
            success: response.data?.success
        })
        return response
    },
    (error) => {
        console.error('❌ API 에러:', {
            status: error.response?.status,
            url: error.config?.url,
            message: error.response?.data?.message
        })

        // 401 Unauthorized - 토큰 만료 또는 무효
        if (error.response?.status === 401) {
            console.log('🔓 인증 만료 - 토큰 제거')
            localStorage.removeItem('token')

            // 로그인 페이지로 리다이렉트 (Vue Router 사용)
            if (window.location.pathname !== '/login') {
                window.location.href = '/login'
            }
        }

        return Promise.reject(error)
    }
)

export default apiClient
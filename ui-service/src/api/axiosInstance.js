// @/api/axiosInstance.js
import axios from 'axios'

// 기본 axios 인스턴스 생성
const apiClient = axios.create({
    baseURL: process.env.NODE_ENV === 'development' ? 'http://localhost:8080' : '',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: false
})

apiClient.interceptors.request.use(
    (config) => {
        if (process.env.NODE_ENV === 'development') {
            console.log('🚀 Request Config:', {
                url: config.url,
                method: config.method,
                withAuth: config.withAuth,
                headers: config.headers
            })
        }

        if (config.withAuth === false) {
            delete config.headers.Authorization
        } else {
            const token = localStorage.getItem('accessToken')
            if (token) {
                config.headers.Authorization = `Bearer ${token}`
                if (process.env.NODE_ENV === 'development') {
                    console.log('🔒 인증 토큰 추가됨')
                }
            } else if (process.env.NODE_ENV === 'development') {
                console.log('⚠️ 토큰이 없습니다')
            }
        }

        return config
    },
    (error) => Promise.reject(error)
)

apiClient.interceptors.response.use(
    (response) => {
        if (process.env.NODE_ENV === 'development') {
            console.log('✅ Response Success:', {
                status: response.status,
                url: response.config.url,
                data: response.data
            })
        }
        return response
    },
    (error) => {
        if (process.env.NODE_ENV === 'development') {
            console.error('❌ Response Error:', {
                status: error.response?.status,
                url: error.config?.url,
                message: error.message,
                data: error.response?.data
            })
        }

        if (error.code === 'ERR_NETWORK') {
            console.error('🌐 네트워크 연결 오류: 서버가 실행 중인지 확인하세요')
        }

        if (error.response?.status === 401) {
            console.error('🔒 인증 오류: 토큰이 유효하지 않거나 만료되었습니다')
            window.location.href = '/login'
        }

        if (error.response?.status === 403) {
            console.error('🚫 접근 권한이 없습니다')
        }

        if (error.response?.status === 404) {
            console.error('🔍 요청한 리소스를 찾을 수 없습니다')
        }

        return Promise.reject(error)
    }
)


export default apiClient
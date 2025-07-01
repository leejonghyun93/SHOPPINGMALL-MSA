// api/axiosInstance.js 또는 해당 파일 수정

import axios from 'axios'
import { user } from '@/stores/userStore'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 🔥 Request 인터셉터 - 토큰 자동 추가
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        const userId = localStorage.getItem('userId')

        if (token && token.trim() && token !== 'null' && token !== 'undefined') {
            const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
            config.headers.Authorization = authToken
        }

        if (userId && userId !== 'null' && userId !== 'undefined') {
            config.headers['X-User-Id'] = userId
        }

        // 🔥 디버깅을 위한 로그
        if (config.url.includes('/users/') || config.url.includes('/orders/')) {
            console.log(`🔗 API 요청: ${config.method?.toUpperCase()} ${config.url}`)
        }

        return config
    },
    (error) => {
        console.error('📡 Request 인터셉터 오류:', error)
        return Promise.reject(error)
    }
)

// 🔥 Response 인터셉터 - 에러 처리 개선
apiClient.interceptors.response.use(
    (response) => {
        // 성공 응답은 그대로 반환
        if (response.config.url?.includes('/users/') || response.config.url?.includes('/orders/')) {
            console.log(`✅ API 응답 성공: ${response.config.url}`, response.status)
        }
        return response
    },
    (error) => {
        const { config, response, message } = error

        // 🔥 특정 API들의 404 에러는 조용히 처리
        const silentFailurePaths = [
            '/api/users/points',
            '/api/users/coupons',
            '/api/users/profile',
            '/api/orders/count'
        ]

        const isSilentFailure = silentFailurePaths.some(path =>
            config?.url?.includes(path)
        )

        if (response) {
            const { status, data } = response

            console.warn(`❌ API 오류 [${status}]: ${config?.url}`, {
                status,
                message: data?.message || message,
                silent: isSilentFailure
            })

            // 🔥 401 Unauthorized - 토큰 만료
            if (status === 401) {
                console.warn('🔑 인증 만료 - 로그인 페이지로 이동')
                localStorage.removeItem('token')
                localStorage.removeItem('userId')

                // 현재 페이지가 로그인 페이지가 아닌 경우에만 리다이렉트
                if (!window.location.pathname.includes('/login')) {
                    window.location.href = '/login'
                }
                return Promise.reject(error)
            }

            // 🔥 404 Not Found - 조용한 실패 처리
            if (status === 404 && isSilentFailure) {
                console.log(`📭 API 엔드포인트 없음 (무시): ${config?.url}`)
                // 기본값을 가진 성공 응답으로 변환
                return Promise.resolve({
                    data: {
                        success: false,
                        data: null,
                        message: 'API not implemented'
                    },
                    status: 200,
                    config
                })
            }

            // 🔥 기타 에러 - 사용자에게 노출하지 않고 콘솔에만 로그
            if (status >= 500) {
                console.error(`🔥 서버 오류 [${status}]: ${config?.url}`)
            }

        } else if (message === 'Network Error') {
            console.error('🌐 네트워크 연결 오류 - 서버 연결 확인 필요')
        } else {
            console.error('❓ 알 수 없는 API 오류:', message)
        }

        return Promise.reject(error)
    }
)

export default apiClient
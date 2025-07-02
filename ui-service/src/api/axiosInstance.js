// api/axiosInstance.js - 선택적 인증 헤더 적용

import axios from 'axios'
import { jwtDecode } from 'jwt-decode'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 🔥 공개 API 엔드포인트 확인 함수
function isPublicEndpoint(url, method) {
    if (!url) return false

    // GET 요청이면서 공개 API들
    if (method?.toUpperCase() === 'GET') {
        const publicGetPaths = [
            '/api/categories/',
            '/api/categories/main',
            '/api/categories/hierarchy',
            '/api/products/',
            '/api/board/',
            '/api/qna/',
            '/api/images/',
            '/images/',
            '/api/notifications/broadcasts/',
            '/api/users/findId',
            '/api/users/checkUserId'
        ]

        if (publicGetPaths.some(path => url.includes(path))) {
            return true
        }
    }

    // 항상 공개인 경로들 (method 무관)
    const alwaysPublicPaths = [
        '/auth/',
        '/api/users/register',
        '/api/users/login',
        '/api/cart/guest/',
        '/api/payments/guest/',
        '/api/payments/webhook',
        '/api/products/guest-cart-details'
    ]

    return alwaysPublicPaths.some(path => url.includes(path))
}

// 🔥 Request 인터셉터 - 선택적 토큰 추가
apiClient.interceptors.request.use(
    (config) => {
        // 🔥 withAuth: false 옵션이 있으면 토큰 추가하지 않음 (최우선)
        if (config.withAuth === false) {
            console.log(`공개 API - withAuth: false: ${config.method?.toUpperCase()} ${config.url}`)
            return config
        }

        // 🔥 공개 API는 토큰을 보내지 않음
        const isPublicAPI = isPublicEndpoint(config.url, config.method)
        if (isPublicAPI) {
            console.log(`공개 API 자동 인식: ${config.method?.toUpperCase()} ${config.url}`)
            return config
        }

        const token = localStorage.getItem('token')
        const userId = localStorage.getItem('userId')

        if (token && token.trim() && token !== 'null' && token !== 'undefined') {
            try {
                const decoded = jwtDecode(token)
                const now = Date.now() / 1000

                if (decoded.exp < now) {
                    alert("토큰이 만료되었습니다. 다시 로그인해주세요.")
                    localStorage.clear()
                    window.location.href = "/login"
                    return Promise.reject(new Error("토큰 만료"))
                }

                const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
                config.headers.Authorization = authToken
                console.log(`인증 토큰 추가: ${config.method?.toUpperCase()} ${config.url}`)
            } catch (error) {
                console.error('토큰 디코딩 에러:', error)
                localStorage.removeItem("token")
            }
        }

        if (userId && userId !== 'null' && userId !== 'undefined') {
            config.headers['X-User-Id'] = userId
        }

        return config
    },
    (error) => {
        console.error('Request 인터셉터 오류:', error)
        return Promise.reject(error)
    }
)

// 🔥 Response 인터셉터 - 에러 처리 개선
apiClient.interceptors.response.use(
    (response) => {
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

            console.warn(`API 오류 [${status}]: ${config?.url}`, {
                status,
                message: data?.message || message,
                silent: isSilentFailure
            })

            // 🔥 401 Unauthorized - 공개 API에서 401이 나오면 토큰 없이 재시도
            if (status === 401) {
                if (isPublicEndpoint(config?.url, config?.method)) {
                    console.log('공개 API에서 401 발생 - 토큰 없이 재시도')
                    const retryConfig = { ...config }
                    delete retryConfig.headers.Authorization
                    return axios.request(retryConfig)
                }

                console.warn('인증 만료 - 로그인 페이지로 이동')
                localStorage.removeItem('token')
                localStorage.removeItem('userId')

                if (!window.location.pathname.includes('/login')) {
                    window.location.href = '/login'
                }
                return Promise.reject(error)
            }

            // 🔥 404 Not Found - 조용한 실패 처리
            if (status === 404 && isSilentFailure) {
                console.log(`API 엔드포인트 없음 (무시): ${config?.url}`)
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
                console.error(`서버 오류 [${status}]: ${config?.url}`)
            }

        } else if (message === 'Network Error') {
            console.error('네트워크 연결 오류 - 서버 연결 확인 필요')
        } else {
            console.error('알 수 없는 API 오류:', message)
        }

        return Promise.reject(error)
    }
)

export default apiClient
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

// 공개 API 엔드포인트 확인 함수
function isPublicEndpoint(url, method) {
    if (!url) return false

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

// 🔥 Request 인터셉터 - 순수 JWT Bearer 토큰만 사용 + 디버깅 강화
apiClient.interceptors.request.use(
    (config) => {
        // withAuth: false 옵션이 있으면 토큰 추가하지 않음
        if (config.withAuth === false) {
            console.log(`공개 API - withAuth: false: ${config.method?.toUpperCase()} ${config.url}`)
            return config
        }

        // 공개 API는 토큰을 보내지 않음
        const isPublicAPI = isPublicEndpoint(config.url, config.method)
        if (isPublicAPI) {
            console.log(`공개 API 자동 인식: ${config.method?.toUpperCase()} ${config.url}`)
            return config
        }

        const token = localStorage.getItem('token')

        if (token && token.trim() && token !== 'null' && token !== 'undefined') {
            try {
                const decoded = jwtDecode(token)
                const now = Date.now() / 1000

                // 🔥 상세한 토큰 디버깅
                console.log('=== 토큰 상세 분석 ===')
                console.log('토큰 원본:', token.substring(0, 50) + '...')
                console.log('디코딩 결과:', {
                    subject: decoded.sub,
                    username: decoded.username,
                    userId: decoded.userId,
                    exp: decoded.exp,
                    now: now,
                    expired: decoded.exp < now,
                    timeLeft: decoded.exp - now,
                    issuer: decoded.iss,
                    audience: decoded.aud
                })

                if (decoded.exp < now) {
                    console.error('❌ 토큰 만료!')
                    alert("토큰이 만료되었습니다. 다시 로그인해주세요.")
                    localStorage.clear()
                    window.location.href = "/login"
                    return Promise.reject(new Error("토큰 만료"))
                }

                // 🔥 순수 JWT Bearer 토큰만 추가
                const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
                config.headers.Authorization = authToken

                // 🔥 서버로 보내는 최종 헤더 확인
                console.log('최종 전송 헤더:', {
                    'Authorization': authToken.substring(0, 20) + '...',
                    'Content-Type': config.headers['Content-Type'],
                    'Accept': config.headers['Accept']
                })

                console.log(`✅ 인증 토큰 추가: ${config.method?.toUpperCase()} ${config.url}`)
            } catch (error) {
                console.error('💥 토큰 디코딩 에러:', error)
                console.error('문제 토큰:', token.substring(0, 100) + '...')
                localStorage.removeItem("token")
            }
        } else {
            console.warn('⚠️ 토큰이 없거나 유효하지 않음:', {
                hasToken: !!token,
                tokenLength: token?.length,
                tokenStart: token?.substring(0, 20)
            })
        }

        // 🔥 X-User-Id 헤더 완전 제거 - JWT 토큰에서만 사용자 정보 추출
        return config
    },
    (error) => {
        console.error('Request 인터셉터 오류:', error)
        return Promise.reject(error)
    }
)

// Response 인터셉터는 기존과 동일하지만 401 에러 디버깅 강화
apiClient.interceptors.response.use(
    (response) => {
        return response
    },
    (error) => {
        const { config, response, message } = error

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

            if (status === 401) {
                // 🔥 결제 완료 상황 체크
                const isPaymentComplete = sessionStorage.getItem('payment_completed') === 'true'
                const isOrderCompletePage = window.location.pathname.includes('/order-complete')
                const hasRecentPayment = sessionStorage.getItem('last_purchase_cleanup')
                const hasPaymentIdInUrl = new URLSearchParams(window.location.search).has('paymentId')

                console.error('=== 401 에러 상세 분석 ===')
                console.error('요청 URL:', config?.url)
                console.error('요청 메서드:', config?.method)
                console.error('요청 헤더:', config?.headers)
                console.error('응답 헤더:', response?.headers)
                console.error('응답 데이터:', response?.data)
                console.error('현재 토큰 상태:', {
                    subject: (() => {
                        try {
                            const token = localStorage.getItem('token')
                            if (token) {
                                const { jwtDecode } = require('jwt-decode')
                                const decoded = jwtDecode(token)
                                return {
                                    subject: decoded.sub,
                                    username: decoded.username,
                                    exp: decoded.exp,
                                    expired: decoded.exp < Date.now() / 1000
                                }
                            }
                        } catch (e) {
                            return { error: e.message }
                        }
                        return null
                    })()
                })
                console.error('결제 완료 상황 체크:', {
                    isPaymentComplete,
                    isOrderCompletePage,
                    hasRecentPayment,
                    hasPaymentIdInUrl
                })
                console.error('========================')

                // 🔥 결제 완료 직후라면 401 에러를 조용히 처리
                if (isPaymentComplete || isOrderCompletePage || hasRecentPayment || hasPaymentIdInUrl) {
                    console.log('🔇 결제 완료 상황에서 401 에러 - 자동 로그아웃 방지')

                    // 🔥 /api/users/profile 요청이라면 토큰에서 기본 사용자 정보로 응답
                    if (config?.url?.includes('/api/users/profile')) {
                        const token = localStorage.getItem('token')
                        if (token) {
                            try {
                                // 토큰 디코딩 (수동으로 구현)
                                const parts = token.replace('Bearer ', '').split('.')
                                if (parts.length === 3) {
                                    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
                                    while (base64.length % 4) {
                                        base64 += '='
                                    }
                                    const payload = JSON.parse(atob(base64))

                                    const fallbackUserData = {
                                        success: true,
                                        data: {
                                            id: payload.sub || payload.username,
                                            userId: payload.sub || payload.username,
                                            name: payload.name || payload.username || payload.sub,
                                            email: payload.email || '',
                                            phone: payload.phone || '',
                                            address: '',
                                            zipcode: '',
                                            gender: 'U',
                                            role: payload.role || 'USER'
                                        }
                                    }

                                    console.log('🔄 토큰에서 사용자 정보 추출하여 fallback 응답 생성')
                                    return Promise.resolve({
                                        data: fallbackUserData,
                                        status: 200,
                                        statusText: 'OK',
                                        headers: {},
                                        config
                                    })
                                }
                            } catch (e) {
                                console.error('토큰에서 사용자 정보 추출 실패:', e)
                            }
                        }
                    }

                    // 🔥 다른 API는 그냥 에러 반환 (로그아웃하지 않음)
                    return Promise.reject(error)
                }

                // 🔥 공개 API에서 401 발생 시 토큰 없이 재시도
                if (isPublicEndpoint(config?.url, config?.method)) {
                    console.log('공개 API에서 401 발생 - 토큰 없이 재시도')
                    const retryConfig = { ...config }
                    delete retryConfig.headers.Authorization
                    return axios.request(retryConfig)
                }

                // 🔥 일반적인 401 에러는 기존 로직 실행
                console.warn('🚪 인증 만료 - 로그인 페이지로 이동')
                localStorage.removeItem('token')
                localStorage.removeItem('userId')

                if (!window.location.pathname.includes('/login')) {
                    window.location.href = '/login'
                }
                return Promise.reject(error)
            }

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


// 🔥 디버깅 함수들
const testTokenStructure = async () => {
    const token = localStorage.getItem('token')
    if (!token) {
        console.log('토큰 없음')
        return
    }

    try {
        // 토큰 구조 분석
        const parts = token.replace('Bearer ', '').split('.')
        if (parts.length !== 3) {
            console.error('유효하지 않은 JWT 구조')
            return
        }

        // 헤더 디코딩
        const header = JSON.parse(atob(parts[0]))
        console.log('JWT 헤더:', header)

        // 페이로드 디코딩
        let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
        while (base64.length % 4) {
            base64 += '='
        }
        const payload = JSON.parse(atob(base64))
        console.log('JWT 페이로드:', payload)

        // 서버가 기대하는 필드 확인
        console.log('사용자 ID 후보들:', {
            sub: payload.sub,
            username: payload.username,
            userId: payload.userId,
            user_id: payload.user_id,
            id: payload.id
        })

    } catch (error) {
        console.error('토큰 구조 분석 실패:', error)
    }
}

const testDirectApiCall = async () => {
    const token = localStorage.getItem('token')

    // 순수 fetch로 테스트
    try {
        const response = await fetch('http://localhost:8080/api/users/profile', {
            method: 'GET',
            headers: {
                'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        })

        console.log('직접 API 호출 결과:', {
            status: response.status,
            statusText: response.statusText,
            headers: Object.fromEntries(response.headers.entries())
        })

        if (response.ok) {
            const data = await response.json()
            console.log('응답 데이터:', data)
        } else {
            const errorText = await response.text()
            console.log('에러 응답:', errorText)
        }
    } catch (error) {
        console.error('직접 API 호출 실패:', error)
    }
}

// 🔥 콘솔에서 디버깅 함수 실행 가능하도록 전역에 등록
window.debugToken = testTokenStructure
window.testDirectApi = testDirectApiCall

export default apiClient
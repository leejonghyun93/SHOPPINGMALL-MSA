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

// Request 인터셉터
apiClient.interceptors.request.use(
    (config) => {


        // withAuth: false 옵션이 있으면 토큰 추가하지 않음
        if (config.withAuth === false) {

            return config
        }

        // 공개 API는 토큰을 보내지 않음
        const isPublicAPI = isPublicEndpoint(config.url, config.method)
        if (isPublicAPI) {
            return config
        }

        const token = localStorage.getItem('jwt')

        if (token && token.trim() && token !== 'null' && token !== 'undefined') {
            try {
                //  만료 검증 강화
                const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
                const decoded = jwtDecode(cleanToken)
                const now = Date.now() / 1000



                if (decoded.exp < now) {
                    alert("토큰이 만료되었습니다. 다시 로그인해주세요.")

                    // 소셜 로그인 정보 보존하면서 토큰만 제거
                    const currentLoginType = localStorage.getItem('login_type')
                    const currentSocialProvider = localStorage.getItem('social_provider')
                    const currentSocialName = localStorage.getItem('social_name')
                    const currentSocialEmail = localStorage.getItem('social_email')

                    localStorage.removeItem('jwt')
                    localStorage.removeItem('userId')

                    // 소셜 로그인 정보 복원
                    if (currentLoginType === 'SOCIAL') {
                        localStorage.setItem('login_type', 'SOCIAL')
                        if (currentSocialProvider) localStorage.setItem('social_provider', currentSocialProvider)
                        if (currentSocialName) localStorage.setItem('social_name', currentSocialName)
                        if (currentSocialEmail) localStorage.setItem('social_email', currentSocialEmail)
                    }

                    window.location.href = "/login"
                    return Promise.reject(new Error("토큰 만료"))
                }

                // 🔥 토큰 형식 정규화
                const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
                config.headers.Authorization = authToken

                // 🔥 위시리스트 API일 때 상세 로깅
                if (config.url.includes('/api/wishlist')) {
                    console.log('🔍 위시리스트 API 요청:', {
                        url: config.url,
                        method: config.method,
                        hasToken: !!config.headers.Authorization,
                        tokenPrefix: config.headers.Authorization?.substring(0, 20) + '...',
                        userId: decoded.sub || decoded.username,
                        role: decoded.role
                    });
                }

            } catch (error) {
                console.error('🔍 토큰 처리 에러:', error);
                localStorage.removeItem("jwt")
                localStorage.removeItem("userId")
            }
        } else {
            console.log('🔍 토큰 없음 또는 유효하지 않음:', {
                url: config.url,
                hasToken: !!token,
                tokenValue: token?.substring(0, 20) + '...'
            });
        }

        return config
    },
    (error) => {
        console.error('🔍 Request Interceptor Error:', error);
        return Promise.reject(error)
    }
)

// Response 인터셉터
apiClient.interceptors.response.use(
    (response) => {
        // 성공적인 응답에 대해서도 로깅
        if (response.config.url.includes('/api/wishlist')) {
            console.log('🔍 위시리스트 API 응답 성공:', {
                url: response.config.url,
                status: response.status,
                data: response.data
            });
        }
        return response
    },
    (error) => {
        const { config, response, message } = error

        // 🔥 위시리스트 API 에러에 대한 상세 로깅
        if (config?.url?.includes('/api/wishlist')) {
            console.error('🔍 위시리스트 API 에러:', {
                url: config.url,
                status: response?.status,
                statusText: response?.statusText,
                data: response?.data,
                headers: response?.headers,
                message: message
            });
        }

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

            if (status === 401) {
                console.log('🔍 401 에러 처리 시작:', {
                    url: config?.url,
                    isWishlist: config?.url?.includes('/api/wishlist')
                });

                // 결제 완료 상황 체크
                const isPaymentComplete = sessionStorage.getItem('payment_completed') === 'true'
                const isOrderCompletePage = window.location.pathname.includes('/order-complete')
                const hasRecentPayment = sessionStorage.getItem('last_purchase_cleanup')
                const hasPaymentIdInUrl = new URLSearchParams(window.location.search).has('paymentId')

                // 결제 완료 직후라면 401 에러를 조용히 처리
                if (isPaymentComplete || isOrderCompletePage || hasRecentPayment || hasPaymentIdInUrl) {
                    // /api/users/profile 요청이라면 토큰에서 기본 사용자 정보로 응답
                    if (config?.url?.includes('/api/users/profile')) {
                        const token = localStorage.getItem('jwt')
                        if (token) {
                            try {
                                // 토큰 디코딩
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

                                    return Promise.resolve({
                                        data: fallbackUserData,
                                        status: 200,
                                        statusText: 'OK',
                                        headers: {},
                                        config
                                    })
                                }
                            } catch (e) {
                                // 토큰 파싱 실패 시 무시
                            }
                        }
                    }

                    // 다른 API는 그냥 에러 반환 (로그아웃하지 않음)
                    return Promise.reject(error)
                }

                // 🔥 위시리스트 API 401 에러 특별 처리
                if (config?.url?.includes('/api/wishlist')) {
                    console.error('🔍 위시리스트 401 에러 - 토큰 재확인 필요');

                    // 토큰 상태 디버깅
                    const token = localStorage.getItem('jwt');
                    if (token) {
                        try {
                            const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
                            const decoded = jwtDecode(cleanToken);
                            console.log('🔍 현재 토큰 정보:', {
                                sub: decoded.sub,
                                username: decoded.username,
                                role: decoded.role,
                                exp: decoded.exp,
                                isExpired: decoded.exp < Date.now() / 1000
                            });
                        } catch (e) {
                            console.error('🔍 토큰 파싱 실패:', e);
                        }
                    }
                }

                // 공개 API에서 401 발생 시 토큰 없이 재시도
                if (isPublicEndpoint(config?.url, config?.method)) {
                    console.log('🔍 공개 API 401 - 토큰 없이 재시도');
                    const retryConfig = { ...config }
                    delete retryConfig.headers.Authorization
                    return axios.request(retryConfig)
                }

                // 일반적인 401 에러 처리 - 소셜 로그인 정보 보존
                console.log('🔍 일반 401 에러 처리 - 로그아웃 진행');
                const currentLoginType = localStorage.getItem('login_type')
                const currentSocialProvider = localStorage.getItem('social_provider')
                const currentSocialName = localStorage.getItem('social_name')
                const currentSocialEmail = localStorage.getItem('social_email')

                localStorage.removeItem('jwt')
                localStorage.removeItem('userId')

                // 소셜 로그인 정보 복원
                if (currentLoginType === 'SOCIAL') {
                    localStorage.setItem('login_type', 'SOCIAL')
                    if (currentSocialProvider) localStorage.setItem('social_provider', currentSocialProvider)
                    if (currentSocialName) localStorage.setItem('social_name', currentSocialName)
                    if (currentSocialEmail) localStorage.setItem('social_email', currentSocialEmail)
                }

                if (!window.location.pathname.includes('/login')) {
                    window.location.href = '/login'
                }
                return Promise.reject(error)
            }

            if (status === 404 && isSilentFailure) {
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
        }

        return Promise.reject(error)
    }
)

export default apiClient
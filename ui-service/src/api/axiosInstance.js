// @/api/axiosInstance.js - 에러 페이지 기능 추가
import axios from 'axios'
import { user } from '@/stores/userStore'

// Axios 인스턴스 생성
const apiClient = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 스마트 로그아웃 처리 함수
const handleLogout = (reason = '인증 만료') => {
    localStorage.removeItem('token')

    // userStore 초기화
    user.id = null
    user.name = null
    user.role = null

    const currentPath = window.location.pathname

    // 인증이 필요한 페이지에서만 로그인 페이지로 리다이렉트
    const authRequiredPaths = [
        '/mypage',      // 마이페이지 전체
        '/checkout',    // 주문서 (로그인 필요)
        '/order-complete', // 주문 완료
        '/profile'      // 회원정보 관리
    ]

    const needsAuth = authRequiredPaths.some(path => currentPath.startsWith(path))

    if (needsAuth) {
        alert('로그인이 필요한 페이지입니다.')
        window.location.href = '/login'
    } else {
        // 헤더만 업데이트하여 로그아웃 상태 반영
        window.dispatchEvent(new Event('auth-changed'))
    }
}

// 🔥 에러 페이지로 리다이렉트하는 함수 추가
const redirectToErrorPage = (status, message, details) => {
    // 현재 페이지가 이미 에러 페이지라면 리다이렉트하지 않음
    if (window.location.pathname.startsWith('/error/')) {
        return
    }

    // SPA 라우터가 있는 경우
    if (window.router) {
        window.router.push({
            name: 'ErrorPage',
            params: { code: status },
            query: {
                message: message || '',
                details: details || ''
            }
        })
    } else {
        // 라우터가 없는 경우 직접 URL 변경
        const params = new URLSearchParams()
        if (message) params.set('message', message)
        if (details) params.set('details', details)

        const queryString = params.toString()
        const url = `/error/${status}${queryString ? `?${queryString}` : ''}`

        window.location.href = url
    }
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

// 토큰 유효성 검사 함수
const isTokenValid = (token) => {
    if (!token) return false

    try {
        const parts = token.split('.')
        if (parts.length !== 3) return false

        let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
        while (base64.length % 4) {
            base64 += '='
        }

        const payloadStr = atob(base64)
        const payload = JSON.parse(payloadStr)
        const currentTime = Math.floor(Date.now() / 1000)

        // 토큰 만료 체크 (30초 여유시간)
        if (payload.exp && payload.exp < (currentTime + 30)) {
            return false
        }

        return true
    } catch (error) {
        return false
    }
}

// 사용자 친화적 에러 메시지
const getAuthErrorMessage = (url) => {
    if (url?.includes('/api/users/profile')) {
        return '프로필 정보를 불러올 수 없습니다. 다시 로그인해주세요.'
    } else if (url?.includes('/api/orders/')) {
        return '주문 정보를 확인하려면 로그인이 필요합니다.'
    } else if (url?.includes('/api/payments/')) {
        return '결제를 진행하려면 로그인이 필요합니다.'
    } else if (url?.includes('/api/cart/')) {
        return '장바구니 기능을 사용하려면 로그인이 필요합니다.'
    } else {
        return '이 기능을 사용하려면 로그인이 필요합니다.'
    }
}

// 요청 인터셉터 - 토큰 자동 추가
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')

        // 요청 전 토큰 유효성 검사
        if (token && !isTokenValid(token)) {
            localStorage.removeItem('token')

            // 인증이 필요한 요청인 경우 에러 발생
            if (config.withAuth !== false) {
                return Promise.reject(new Error('토큰이 만료되었습니다'))
            }
        }

        // withAuth가 false가 아닌 경우 토큰 추가 (기본값은 true)
        if (config.withAuth !== false && token && isTokenValid(token)) {
            config.headers.Authorization = `Bearer ${token}`
        }

        // withAuth 속성 제거 (서버로 전송되지 않도록)
        delete config.withAuth

        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 🔥 응답 인터셉터 - 에러 페이지 기능 추가
apiClient.interceptors.response.use(
    (response) => {
        return response
    },
    (error) => {
        const status = error.response?.status
        const url = error.config?.url
        const message = error.response?.data?.message || error.message
        const serverData = error.response?.data

        console.log(`🚨 API 에러 발생: ${status} - ${url}`)

        // 401 Unauthorized 스마트 처리 (기존 로직 유지)
        if (status === 401) {
            const serverMessage = error.response?.data?.message
            const isTokenInvalid = serverMessage?.includes('JWT') ||
                serverMessage?.includes('token') ||
                serverMessage?.includes('Authorization header missing') ||
                serverMessage?.includes('Invalid JWT token')

            if (isTokenInvalid) {
                const friendlyMessage = '인증이 만료되어 다시 로그인이 필요합니다.'
                handleLogout(friendlyMessage)
                error.friendlyMessage = friendlyMessage
            } else {
                error.friendlyMessage = '접근 권한이 없습니다. 페이지를 새로고침해보세요.'
            }
        }
        // 🔥 새로 추가: 다른 에러들은 에러 페이지로 리다이렉트
        else if (status && shouldShowErrorPage(status)) {
            // 에러 상세 정보 수집
            const errorDetails = JSON.stringify({
                url: url,
                status: status,
                message: message,
                serverData: serverData,
                timestamp: new Date().toISOString()
            }, null, 2)

            console.log(`🔄 에러 페이지로 리다이렉트: ${status}`)

            // 에러 페이지로 리다이렉트
            redirectToErrorPage(status, message, errorDetails)
        }

        // 네트워크 에러 처리
        if (!error.response) {
            error.friendlyMessage = '네트워크 연결을 확인해주세요.'

            // 네트워크 에러도 에러 페이지로 (502로 처리)
            console.log('🔄 네트워크 에러 - 502 에러 페이지로 리다이렉트')
            redirectToErrorPage(502, '네트워크 연결에 문제가 발생했습니다',
                JSON.stringify({
                    error: 'Network Error',
                    url: url,
                    timestamp: new Date().toISOString()
                }, null, 2))
        }

        return Promise.reject(error)
    }
)

// 🔥 라우터 참조 설정 함수 (main.js에서 호출)
export const setRouter = (router) => {
    window.router = router
}

export default apiClient
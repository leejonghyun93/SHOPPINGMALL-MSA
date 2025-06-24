// @/api/axiosInstance.js - 보안 강화된 버전
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

// 🔥 스마트 로그아웃 처리 함수
const handleLogout = (reason = '인증 만료') => {
    console.log('🔓 자동 로그아웃 처리:', reason)
    localStorage.removeItem('token')

    // userStore 초기화
    user.id = null
    user.name = null
    user.role = null

    const currentPath = window.location.pathname

    // 🔥 인증이 필요한 페이지에서만 로그인 페이지로 리다이렉트
    const authRequiredPaths = [
        '/mypage',      // 마이페이지 전체
        '/checkout',    // 주문서 (로그인 필요)
        '/order-complete', // 주문 완료
        '/profile'      // 회원정보 관리
    ]

    const needsAuth = authRequiredPaths.some(path => currentPath.startsWith(path))

    if (needsAuth) {
        console.log('🔄 인증 필요 페이지 - 로그인 페이지로 리다이렉트')
        alert('로그인이 필요한 페이지입니다.')
        window.location.href = '/login'
    } else {
        console.log('ℹ️ 공개 페이지 - 리다이렉트 하지 않음:', currentPath)
        // 헤더만 업데이트하여 로그아웃 상태 반영
        window.dispatchEvent(new Event('auth-changed'))
    }
}

// 🔥 토큰 유효성 검사 함수
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
            console.log('🔓 토큰이 곧 만료되거나 이미 만료됨')
            return false
        }

        return true
    } catch (error) {
        console.error('❌ 토큰 검증 에러:', error)
        return false
    }
}

// 🔥 사용자 친화적 에러 메시지
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

        // 🔥 요청 전 토큰 유효성 검사
        if (token && !isTokenValid(token)) {
            console.log('🔓 요청 전 토큰 무효 감지 - 제거')
            localStorage.removeItem('token')

            // 인증이 필요한 요청인 경우 에러 발생
            if (config.withAuth !== false) {
                return Promise.reject(new Error('토큰이 만료되었습니다'))
            }
        }

        // 🔥 withAuth가 false가 아닌 경우 토큰 추가 (기본값은 true)
        if (config.withAuth !== false && token && isTokenValid(token)) {
            config.headers.Authorization = `Bearer ${token}`
            console.log('🔐 토큰 헤더 추가:', `Bearer ${token.substring(0, 20)}...`)

            // 🔥 토큰 내용 확인 (디버깅용)
            try {
                const parts = token.split('.')
                const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
                console.log('🎫 토큰 사용자:', payload.sub, '만료시간:', new Date(payload.exp * 1000).toLocaleString())
            } catch (e) {
                console.log('🎫 토큰 파싱 실패')
            }
        } else {
            console.log('🚫 토큰 없음 또는 무효:', {
                hasToken: !!token,
                isValid: token ? isTokenValid(token) : false,
                withAuth: config.withAuth
            })
        }

        // withAuth 속성 제거 (서버로 전송되지 않도록)
        delete config.withAuth

        console.log('📡 API 요청:', {
            method: config.method?.toUpperCase(),
            url: config.url,
            hasAuth: !!config.headers.Authorization,
            data: config.data ? '(데이터 있음)' : '(데이터 없음)'
        })

        return config
    },
    (error) => {
        console.error('❌ 요청 인터셉터 에러:', error)
        return Promise.reject(error)
    }
)

// 응답 인터셉터 - 401 에러 스마트 처리
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
        const status = error.response?.status
        const url = error.config?.url
        const message = error.response?.data?.message

        console.error('❌ API 에러:', {
            status,
            url,
            message
        })

        // 🔥 401 Unauthorized 스마트 처리
        if (status === 401) {
            console.log('🔒 401 인증 에러 발생:', url)

            // 🔥 401 응답의 메시지 확인
            const serverMessage = error.response?.data?.message
            const isTokenInvalid = serverMessage?.includes('JWT') ||
                serverMessage?.includes('token') ||
                serverMessage?.includes('Authorization header missing') ||
                serverMessage?.includes('Invalid JWT token')

            console.log('🔍 서버 응답 분석:', {
                message: serverMessage,
                isTokenInvalid: isTokenInvalid
            })

            // 🔥 실제 토큰 무효인 경우에만 로그아웃 처리
            if (isTokenInvalid) {
                console.log('🔓 유효하지 않은 토큰으로 인한 로그아웃')
                const friendlyMessage = '인증이 만료되어 다시 로그인이 필요합니다.'
                handleLogout(friendlyMessage)
                error.friendlyMessage = friendlyMessage
            } else {
                // 🔥 권한 부족 등 기타 401 에러는 로그아웃하지 않음
                console.log('ℹ️ 권한 부족 또는 기타 401 에러 - 로그아웃하지 않음')
                error.friendlyMessage = '접근 권한이 없습니다. 페이지를 새로고침해보세요.'
            }
        }

        // 🔥 네트워크 에러 처리
        if (!error.response) {
            console.error('🌐 네트워크 에러')
            error.friendlyMessage = '네트워크 연결을 확인해주세요.'
        }

        return Promise.reject(error)
    }
)

export default apiClient
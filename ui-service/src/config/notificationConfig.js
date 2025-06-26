// src/config/notificationConfig.js - CORS 오류 수정
export const NOTIFICATION_CONFIG = {
    // 🔥 임시로 알림 서비스에 직접 호출 (API Gateway 문제 해결 전까지)
    BASE_URL: 'http://localhost:8096/api/notifications',  // 직접 호출로 임시 변경
    ENDPOINTS: {
        HEALTH: '/health',
        BROADCASTS_SCHEDULE: '/broadcasts/schedule',
        SUBSCRIPTIONS_USER: '/subscriptions/user',
        SUBSCRIBE_BROADCAST: '/subscriptions',
        SUBSCRIPTIONS: '/subscriptions'
    },

    getNotificationBaseUrl() {
        return this.BASE_URL
    }
}

// 🔥 방송 알림 구독 함수 - 올바른 엔드포인트로 수정
export const subscribeBroadcastStart = async (userIdentifier, broadcastId) => {
    if (!userIdentifier || !broadcastId) {
        throw new Error('필수 파라미터가 누락되었습니다.')
    }

    // 🔥 컨트롤러의 실제 엔드포인트와 일치하도록 수정
    const endpoint = `/subscriptions/broadcast-start?userId=${userIdentifier}&broadcastId=${broadcastId}`

    const response = await notificationApiCall(endpoint, {
        method: 'POST'
    })

    if (!response.ok) {
        const errorText = await response.text()
        let errorData

        try {
            errorData = JSON.parse(errorText)
        } catch (e) {
            errorData = { message: errorText }
        }

        const error = new Error(errorData.message || `HTTP ${response.status}`)
        error.response = { status: response.status, data: errorData }
        throw error
    }

    return await response.json()
}

// 🔥 방송 알림 구독 취소 함수 - 올바른 엔드포인트로 수정
export const unsubscribeBroadcast = async (userIdentifier, broadcastId) => {
    if (!userIdentifier || !broadcastId) {
        throw new Error('필수 파라미터가 누락되었습니다.')
    }

    // 🔥 컨트롤러의 실제 엔드포인트와 일치하도록 수정
    const endpoint = `/subscriptions?userId=${userIdentifier}&broadcastId=${broadcastId}&type=BROADCAST_START`

    const response = await notificationApiCall(endpoint, {
        method: 'DELETE'
    })

    if (!response.ok) {
        const errorText = await response.text()
        let errorData

        try {
            errorData = JSON.parse(errorText)
        } catch (e) {
            errorData = { message: errorText }
        }

        const error = new Error(errorData.message || `HTTP ${response.status}`)
        error.response = { status: response.status, data: errorData }
        throw error
    }
}

// 🔥 JWT에서 사용자 정보 추출
const getCurrentUser = () => {
    const token = localStorage.getItem('token')

    if (!token || !isTokenValid(token)) {
        return { id: null, username: 'guest', identifier: null }
    }

    try {
        const parts = token.split('.')
        let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
        while (base64.length % 4) {
            base64 += '='
        }

        const payload = JSON.parse(atob(base64))
        console.log('🔍 JWT 페이로드:', payload)

        let userId = null
        let userIdentifier = null

        // 1. 숫자 ID 추출 시도
        const potentialIds = [payload.userId, payload.sub, payload.id]
        for (const id of potentialIds) {
            if (id && !isNaN(Number(id)) && Number(id) > 0) {
                userId = Number(id)
                userIdentifier = String(id)
                break
            }
        }

        // 2. 숫자 ID가 없으면 문자열 식별자 사용
        if (!userId) {
            userIdentifier = payload.sub || payload.username || payload.id
            console.log('📝 숫자 ID가 없어서 문자열 식별자 사용:', userIdentifier)
        }

        const username = payload.username || payload.name || userIdentifier || 'user'

        return {
            id: userId,
            identifier: userIdentifier,
            username: username
        }

    } catch (error) {
        console.error('JWT 파싱 오류:', error)
        return { id: null, username: 'guest', identifier: null }
    }
}

// 토큰 유효성 검사
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

        if (payload.exp && payload.exp < (currentTime + 30)) {
            return false
        }

        return true
    } catch (error) {
        return false
    }
}

// JWT 헤더 생성
const getAuthHeaders = () => {
    const token = localStorage.getItem('token')

    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    }

    if (token) {
        headers['Authorization'] = `Bearer ${token}`
    }

    return headers
}

// 🔥 알림 API 호출
export const notificationApiCall = async (endpoint, options = {}) => {
    let url

    if (endpoint.startsWith('http://') || endpoint.startsWith('https://')) {
        url = endpoint
    } else if (endpoint.startsWith('/')) {
        url = `${NOTIFICATION_CONFIG.BASE_URL}${endpoint}`
    } else {
        url = `${NOTIFICATION_CONFIG.BASE_URL}/${endpoint}`
    }

    try {
        console.log(`🔗 알림 API 호출: ${url}`)

        const response = await fetch(url, {
            method: options.method || 'GET',
            mode: 'cors',
            headers: {
                ...getAuthHeaders(),
                ...options.headers
            },
            ...options
        })

        console.log(`📡 응답: ${response.status} ${response.statusText}`)

        return response

    } catch (error) {
        console.error('❌ API 호출 실패:', error)
        return {
            ok: false,
            status: 0,
            json: async () => ({}),
            text: async () => ''
        }
    }
}

// export
export { getCurrentUser }
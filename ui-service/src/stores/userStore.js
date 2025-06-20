// src/stores/userStore.js
import { reactive } from "vue"
import { jwtDecode } from "jwt-decode"

export const user = reactive({
    id: null,
    name: null,
    role: null,
    email: null,
    phone: null,
})

/**
 * 앱 초기화 시 localStorage에서 토큰을 확인하고 사용자 정보 복원
 */
export function initializeUser() {
    const token = localStorage.getItem('token')
    if (token && isTokenValid(token)) {
        setUserFromToken(token)
        console.log('앱 시작 시 사용자 정보 복원됨:', { id: user.id, name: user.name })
    } else if (token) {
        // 토큰이 있지만 유효하지 않으면 제거
        console.log('유효하지 않은 토큰 제거')
        localStorage.removeItem('token')
    }
}

/**
 * 토큰 유효성 검사
 */
export function isTokenValid(token) {
    if (!token) return false

    try {
        const payload = jwtDecode(token)
        const currentTime = Math.floor(Date.now() / 1000)

        // 만료 시간 체크
        if (payload.exp && payload.exp < currentTime) {
            console.log('토큰이 만료되었습니다.')
            return false
        }

        return true
    } catch (error) {
        console.error('토큰 유효성 검사 실패:', error)
        return false
    }
}

/**
 * JWT 토큰으로부터 사용자 정보를 추출하여 user 상태 설정
 */
export function setUserFromToken(token) {
    try {
        const payload = jwtDecode(token)
        console.log('=== JWT 토큰 페이로드 전체 ===')
        console.log(payload)
        console.log('===========================')

        // sub가 'null'이면 username 사용
        let userId = payload.sub
        if (userId === 'null' || userId === null || userId === undefined) {
            userId = payload.username // 'asdasds' 사용
        }

        user.id = userId  // 이제 'asdasds'가 될 것
        user.name = payload.name || payload.username || payload.sub
        user.role = payload.role || payload.authorities?.[0] || 'USER'
        user.email = payload.email || payload.mail || payload.userEmail || null
        user.phone = payload.phone || payload.phoneNumber || payload.mobile || payload.userPhone || null

        // 🔧 추가: localStorage에 사용자 ID 저장
        if (userId) {
            localStorage.setItem('userId', userId)
            localStorage.setItem('user_id', userId)  // 백업용
            console.log('✅ localStorage에 사용자 ID 저장:', userId)
        }

        console.log('=== 토큰에서 추출된 사용자 정보 ===')
        console.log('ID:', user.id)
        console.log('Name:', user.name)
        console.log('Role:', user.role)
        console.log('Email:', user.email)
        console.log('Phone:', user.phone)
        console.log('=====================================')

    } catch (e) {
        console.error('토큰 디코딩 실패:', e)
        resetUser()
    }
}

/**
 * 로그인 성공 시 토큰 저장 및 사용자 정보 설정
 */
export function loginSuccess(token) {
    localStorage.setItem('token', token)
    setUserFromToken(token)
}

/**
 * user 상태 초기화 및 토큰 제거
 */
export function resetUser() {
    user.id = null
    user.name = null
    user.role = null
    user.email = null
    user.phone = null
    localStorage.removeItem("token")
    localStorage.removeItem("auth_token")
    localStorage.removeItem("access_token")
    // 🔧 추가: userId도 제거
    localStorage.removeItem("userId")
    localStorage.removeItem("user_id")
    sessionStorage.removeItem("auth_token")
}

/**
 * 사용자 로그인 여부 반환
 */
export function isLoggedIn() {
    return !!user.id
}

/**
 * 현재 사용자 정보 반환
 */
export function getCurrentUser() {
    return {
        id: user.id,
        name: user.name,
        role: user.role,
        email: user.email,
        phone: user.phone
    }
}

// 앱 시작 시 자동으로 사용자 정보 초기화
initializeUser()
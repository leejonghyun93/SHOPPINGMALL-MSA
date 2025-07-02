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
    } else if (token) {
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
        return !(payload.exp && payload.exp < currentTime)
    } catch (error) {
        return false
    }
}
export function getUserIdFromToken() {
    const token = localStorage.getItem('token')
    if (!token) return null

    try {
        const payload = jwtDecode(token)
        return payload.sub || payload.username || null
    } catch {
        return null
    }
}
/**
 * JWT 토큰으로부터 사용자 정보를 추출하여 user 상태 설정
 */
export function setUserFromToken(token) {
    try {
        const payload = jwtDecode(token)

        let userId = payload.sub
        if (userId === 'null' || userId === null || userId === undefined) {
            userId = payload.username
        }

        user.id = userId
        user.name = payload.name || payload.username || payload.sub
        user.role = payload.role || payload.authorities?.[0] || 'USER'
        user.email = payload.email || payload.mail || payload.userEmail || null
        user.phone = payload.phone || payload.phoneNumber || payload.mobile || payload.userPhone || null

        if (userId) {
            localStorage.setItem('userId', userId)
        }
    } catch (e) {
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
    localStorage.removeItem("userId")
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

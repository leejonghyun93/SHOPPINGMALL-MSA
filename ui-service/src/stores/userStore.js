// src/stores/userStore.js
import { reactive } from "vue"
import { jwtDecode } from "jwt-decode"

export const user = reactive({
    id: null,
    name: null,
    role: null,
})

/**
 * JWT 토큰으로부터 사용자 정보를 추출하여 user 상태 설정
 */
export function setUserFromToken(token) {
    try {
        const payload = jwtDecode(token)
        console.log('decoded payload:', payload)

        user.id = payload.sub || payload.userid || payload.id
        user.name = payload.name || payload.username || payload.sub
        user.role = payload.role || payload.authorities?.[0] || 'USER'

        console.log('User set:', { id: user.id, name: user.name, role: user.role })
    } catch (e) {
        console.error('토큰 디코딩 실패:', e)
        resetUser()
    }
}

/**
 * user 상태 초기화 및 토큰 제거
 */
export function resetUser() {
    user.id = null
    user.name = null
    user.role = null
    localStorage.removeItem("token")  // 'jwtToken'이 아닌 'token'으로 통일
}

/**
 * 사용자 로그인 여부 반환
 */
export function isLoggedIn() {
    return !!user.id
}
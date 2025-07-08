// userStore.js - 간소화 버전

import { reactive } from "vue"
import { jwtDecode } from "jwt-decode"

export const user = reactive({
    id: null,
    name: null,
    role: null,
    email: null,
    phone: null,
})

// 로그인 성공 처리 (일반 + 소셜 통합)
export function loginSuccess(token, loginType = 'NORMAL', additionalInfo = null) {
    localStorage.setItem('jwt', token)
    localStorage.setItem('login_type', loginType)

    // 소셜 로그인 추가 정보 저장
    if (loginType === 'SOCIAL' && additionalInfo) {
        if (additionalInfo.provider) {
            localStorage.setItem('social_provider', additionalInfo.provider)
        }
        if (additionalInfo.name) {
            localStorage.setItem('social_name', additionalInfo.name)
        }
        if (additionalInfo.email) {
            localStorage.setItem('social_email', additionalInfo.email)
        }
    }

    return setUserFromToken(token)
}

// 토큰에서 사용자 정보 설정
export function setUserFromToken(token) {
    try {
        const payload = jwtDecode(token)

        // 사용자 ID 설정
        user.id = payload.sub || payload.username || null

        // 이름 설정 (소셜 정보 우선)
        const socialName = localStorage.getItem('social_name')
        if (socialName) {
            user.name = socialName
        } else if (payload.name && payload.name.trim() && payload.name !== "사용자") {
            user.name = payload.name.trim()
        } else {
            user.name = user.id || "사용자"
        }

        // 이메일 설정 (소셜 정보 우선)
        const socialEmail = localStorage.getItem('social_email')
        if (socialEmail) {
            user.email = socialEmail
        } else {
            const emailFields = ['email', 'mail', 'userEmail', 'emailAddress']
            for (const field of emailFields) {
                if (payload[field]) {
                    user.email = payload[field]
                    break
                }
            }
        }

        // 기타 정보
        user.role = payload.role || payload.authorities?.[0] || 'USER'

        if (payload.phone) {
            user.phone = payload.phone
        }

        return true
    } catch (e) {
        resetUser()
        return false
    }
}

// 사용자 정보 초기화
export function initializeUser() {
    const token = localStorage.getItem('jwt')
    if (token && isTokenValid(token)) {
        setUserFromToken(token)
    } else {
        resetUser()
    }
}

// 토큰 유효성 검사
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

// 사용자 정보 완전 초기화
export function resetUser() {
    user.id = null
    user.name = null
    user.role = null
    user.email = null
    user.phone = null

    // localStorage 정리
    const keysToRemove = [
        'jwt', 'login_type', 'social_provider', 'social_name', 'social_email'
    ]
    keysToRemove.forEach(key => localStorage.removeItem(key))
}

// 로그인 여부 확인
export function isLoggedIn() {
    return !!user.id && !!localStorage.getItem('jwt')
}

// 현재 사용자 정보 반환
export function getCurrentUser() {
    return {
        id: user.id,
        name: user.name,
        role: user.role,
        email: user.email,
        phone: user.phone,
        loginType: localStorage.getItem('login_type') || 'NORMAL',
        isSocial: localStorage.getItem('login_type') === 'SOCIAL'
    }
}

// 소셜 로그인 여부 확인
export const isSocialLoginUser = () => {
    return localStorage.getItem('login_type') === 'SOCIAL'
}

// 일반 로그인 여부 확인
export const isNormalLoginUser = () => {
    return localStorage.getItem('login_type') === 'NORMAL'
}

// 소셜 로그인 제공업체 확인
export const getSocialLoginProvider = () => {
    return isSocialLoginUser() ? localStorage.getItem('social_provider') : null
}

// 사용자 정보 업데이트 (API에서 받은 데이터로)
export function updateUserFromApi(userData) {
    if (!userData) return false

    user.id = userData.userId || userData.id || user.id

    if (userData.name && userData.name.trim() && userData.name !== "사용자") {
        user.name = userData.name.trim()
    }

    if (userData.email && userData.email.trim()) {
        user.email = userData.email.trim()
    }

    if (userData.phone && userData.phone.trim()) {
        user.phone = userData.phone.trim()
    }

    user.role = userData.role || user.role || 'USER'

    return true
}

// 토큰에서 사용자 ID 추출
export function getUserIdFromToken() {
    const token = localStorage.getItem('jwt')
    if (!token) return null

    try {
        const payload = jwtDecode(token)
        return payload.sub || payload.username || null
    } catch {
        return null
    }
}

//  결제용 함수들 (호환성 유지)
export function backupNameForPayment() {
    if (user.name && user.name.trim() && user.name !== "사용자") {
        localStorage.setItem('payment_user_name', user.name)
    }
    if (user.email && user.email.trim()) {
        localStorage.setItem('payment_user_email', user.email)
    }
    if (user.phone && user.phone.trim()) {
        localStorage.setItem('payment_user_phone', user.phone)
    }
    return true
}

export function restoreNameAfterPayment() {
    const paymentName = localStorage.getItem('payment_user_name')
    const paymentEmail = localStorage.getItem('payment_user_email')
    const paymentPhone = localStorage.getItem('payment_user_phone')

    if (paymentName) {
        user.name = paymentName
        localStorage.removeItem('payment_user_name')
    }
    if (paymentEmail) {
        user.email = paymentEmail
        localStorage.removeItem('payment_user_email')
    }
    if (paymentPhone) {
        user.phone = paymentPhone
        localStorage.removeItem('payment_user_phone')
    }

    return true
}

// 호환성을 위한 추가 함수들
export function restoreUserInfoFromStorage() {
    // 간소화 버전에서는 별도 처리 불필요
    return true
}

export function setNormalLogin(token) {
    return loginSuccess(token, 'NORMAL')
}

export function setSocialLogin(token, provider, socialName = null, socialEmail = null, socialPhone = null) {
    return loginSuccess(token, 'SOCIAL', {
        provider,
        name: socialName,
        email: socialEmail,
        phone: socialPhone
    })
}

// 앱 시작 시 자동으로 사용자 정보 초기화
initializeUser()
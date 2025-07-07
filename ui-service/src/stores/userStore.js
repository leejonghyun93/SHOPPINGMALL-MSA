// userStore.js - 이메일/휴대폰 정보 포함 완전 수정 버전 (로그 제거)

import { reactive } from "vue"
import { jwtDecode } from "jwt-decode"

export const user = reactive({
    id: null,
    name: null,
    role: null,
    email: null,
    phone: null,
})

export function setUserFromToken(token) {
    try {
        const payload = jwtDecode(token)

        let userId = payload.sub
        if (userId === 'null' || userId === null || userId === undefined) {
            userId = payload.username
        }

        user.id = userId

        // 이름 처리
        if (payload.name && payload.name.trim() && payload.name !== "사용자" && payload.name !== "소셜사용자") {
            user.name = payload.name.trim()
            localStorage.setItem('user_display_name', user.name)
            sessionStorage.setItem('current_user_name', user.name)
        } else if (payload.username && payload.username !== userId) {
            user.name = payload.username
        } else {
            user.name = userId
        }

        // 이메일 처리
        const emailFields = ['email', 'mail', 'userEmail', 'emailAddress']
        for (const field of emailFields) {
            if (payload[field]) {
                user.email = payload[field]
                localStorage.setItem('user_email', payload[field])
                sessionStorage.setItem('user_email', payload[field])
                break
            }
        }

        // 휴대폰 처리
        const phoneFields = ['phone', 'phoneNumber', 'mobile', 'userPhone', 'tel', 'cellphone']
        for (const field of phoneFields) {
            if (payload[field]) {
                user.phone = payload[field]
                localStorage.setItem('user_phone', payload[field])
                sessionStorage.setItem('user_phone', payload[field])
                break
            }
        }

        user.role = payload.role || payload.authorities?.[0] || 'USER'

        if (userId) {
            localStorage.setItem('userId', userId)
        }

        return true
    } catch (e) {
        resetUser()
        return false
    }
}

export function updateUserFromApi(userData) {
    try {
        if (!userData) return false

        user.id = userData.userId || userData.id || user.id

        const token = localStorage.getItem('token')
        let tokenName = null
        if (token) {
            try {
                const payload = jwtDecode(token)
                tokenName = payload.name
            } catch (e) {}
        }

        // 이름 처리 (기존 로직 유지)
        if (tokenName && tokenName.trim() && tokenName !== "사용자" && tokenName !== "소셜사용자") {
            user.name = tokenName.trim()
        } else if (user.name && user.name.trim() && user.name !== "사용자" && user.name !== "소셜사용자") {
            // 기존 이름 유지
        } else if (userData.name && userData.name.trim() && userData.name !== "사용자" && userData.name !== "소셜사용자") {
            user.name = userData.name.trim()
        } else {
            user.name = user.id || "사용자"
        }

        // 이메일 처리
        if (userData.email && userData.email.trim()) {
            user.email = userData.email.trim()
            localStorage.setItem('user_email', user.email)
            sessionStorage.setItem('user_email', user.email)
        }

        // 휴대폰 처리
        if (userData.phone && userData.phone.trim()) {
            user.phone = userData.phone.trim()
            localStorage.setItem('user_phone', user.phone)
            sessionStorage.setItem('user_phone', user.phone)
        }

        user.role = userData.role || user.role || 'USER'

        if (user.name && user.name !== "사용자") {
            localStorage.setItem('user_display_name', user.name)
            sessionStorage.setItem('current_user_name', user.name)
        }

        return true
    } catch (error) {
        return false
    }
}

export function restoreUserInfoFromStorage() {
    // 이메일 복원
    if (!user.email) {
        const savedEmail = localStorage.getItem('user_email') ||
            sessionStorage.getItem('user_email')
        if (savedEmail) {
            user.email = savedEmail
        }
    }

    // 휴대폰 번호 복원
    if (!user.phone) {
        const savedPhone = localStorage.getItem('user_phone') ||
            sessionStorage.getItem('user_phone')
        if (savedPhone) {
            user.phone = savedPhone
        }
    }

    // 이름 복원
    if (!user.name || user.name === "사용자") {
        const savedName = sessionStorage.getItem('current_user_name') ||
            localStorage.getItem('user_display_name')
        if (savedName && savedName.trim() && savedName !== "사용자") {
            user.name = savedName
        }
    }
}

export function initializeUser() {
    const token = localStorage.getItem('token')
    if (token && isTokenValid(token)) {
        setUserFromToken(token)
        restoreUserInfoFromStorage()
    } else if (token) {
        localStorage.removeItem('token')
        localStorage.removeItem('user_display_name')
        localStorage.removeItem('user_email')
        localStorage.removeItem('user_phone')
        sessionStorage.removeItem('current_user_name')
        sessionStorage.removeItem('user_email')
        sessionStorage.removeItem('user_phone')
    }
}

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

export function loginSuccess(token) {
    localStorage.setItem('token', token)
    setUserFromToken(token)
}

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
    localStorage.removeItem("user_display_name")
    localStorage.removeItem("user_email")
    localStorage.removeItem("user_phone")
    sessionStorage.removeItem("auth_token")
    sessionStorage.removeItem("current_user_name")
    sessionStorage.removeItem("user_email")
    sessionStorage.removeItem("user_phone")
}

export function isLoggedIn() {
    return !!user.id
}

export function getCurrentUser() {
    return {
        id: user.id,
        name: user.name,
        role: user.role,
        email: user.email,
        phone: user.phone
    }
}

export function backupNameForPayment() {
    let backupSuccess = false

    if (user.name && user.name.trim() && user.name !== "사용자") {
        localStorage.setItem('payment_user_name', user.name)
        localStorage.setItem('user_display_name', user.name)
        sessionStorage.setItem('current_user_name', user.name)
        backupSuccess = true
    }

    if (user.email && user.email.trim()) {
        localStorage.setItem('payment_user_email', user.email)
        localStorage.setItem('user_email', user.email)
        sessionStorage.setItem('user_email', user.email)
        backupSuccess = true
    }

    if (user.phone && user.phone.trim()) {
        localStorage.setItem('payment_user_phone', user.phone)
        localStorage.setItem('user_phone', user.phone)
        sessionStorage.setItem('user_phone', user.phone)
        backupSuccess = true
    }

    sessionStorage.setItem('payment_backup_time', Date.now().toString())
    return backupSuccess
}

export function restoreNameAfterPayment() {
    let restored = false

    // 이름 복원
    const paymentName = localStorage.getItem('payment_user_name')
    if (paymentName && paymentName.trim()) {
        user.name = paymentName
        sessionStorage.setItem('current_user_name', paymentName)
        localStorage.removeItem('payment_user_name')
        restored = true
    } else {
        const savedName = sessionStorage.getItem('current_user_name') ||
            localStorage.getItem('user_display_name')
        if (savedName && savedName.trim() && savedName !== "사용자") {
            user.name = savedName
            restored = true
        }
    }

    // 이메일 복원
    const paymentEmail = localStorage.getItem('payment_user_email')
    if (paymentEmail && paymentEmail.trim()) {
        user.email = paymentEmail
        sessionStorage.setItem('user_email', paymentEmail)
        localStorage.setItem('user_email', paymentEmail)
        localStorage.removeItem('payment_user_email')
        restored = true
    } else {
        const savedEmail = localStorage.getItem('user_email') ||
            sessionStorage.getItem('user_email')
        if (savedEmail && !user.email) {
            user.email = savedEmail
            restored = true
        }
    }

    // 휴대폰 복원
    const paymentPhone = localStorage.getItem('payment_user_phone')
    if (paymentPhone && paymentPhone.trim()) {
        user.phone = paymentPhone
        sessionStorage.setItem('user_phone', paymentPhone)
        localStorage.setItem('user_phone', paymentPhone)
        localStorage.removeItem('payment_user_phone')
        restored = true
    } else {
        const savedPhone = localStorage.getItem('user_phone') ||
            sessionStorage.getItem('user_phone')
        if (savedPhone && !user.phone) {
            user.phone = savedPhone
            restored = true
        }
    }

    sessionStorage.removeItem('payment_backup_time')
    return restored
}

export function saveSocialLoginName(name) {
    if (name && name.trim() && name !== "사용자" && name.length > 1) {
        const cleanName = name.trim()
        localStorage.setItem('user_display_name', cleanName)
        sessionStorage.setItem('current_user_name', cleanName)
        user.name = cleanName
        return true
    }
    return false
}

export const isSocialLoginUser = () => {
    const socialName = localStorage.getItem('social_login_name') ||
        sessionStorage.getItem('current_user_name') ||
        localStorage.getItem('preserved_user_name');

    if (socialName && socialName.trim() && socialName !== "사용자") {
        return true;
    }

    const token = localStorage.getItem('token');
    if (token) {
        try {
            const parts = token.replace('Bearer ', '').split('.');
            if (parts.length === 3) {
                let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
                while (base64.length % 4) {
                    base64 += '=';
                }
                const payload = JSON.parse(atob(base64));

                if (payload.socialProvider || payload.provider || payload.loginType === 'SOCIAL') {
                    return true;
                }

                if (payload.name && payload.name !== payload.sub && payload.name !== payload.username) {
                    if (/[가-힣]/.test(payload.name) || (/^[a-zA-Z\s]+$/.test(payload.name) && payload.name.length > 1)) {
                        return true;
                    }
                }
            }
        } catch (e) {
            // 토큰 파싱 에러 무시
        }
    }

    return false;
};

export const getSocialLoginProvider = () => {
    const token = localStorage.getItem('token');
    if (token) {
        try {
            const parts = token.replace('Bearer ', '').split('.');
            if (parts.length === 3) {
                let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
                while (base64.length % 4) {
                    base64 += '=';
                }
                const payload = JSON.parse(atob(base64));
                return payload.socialProvider || payload.provider || 'UNKNOWN';
            }
        } catch (e) {
            // 토큰 파싱 에러
        }
    }
    return null;
};

// 앱 시작 시 자동으로 사용자 정보 초기화
initializeUser()
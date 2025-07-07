// userStore.js - 소셜/일반 로그인 완전 구별 버전

import { reactive } from "vue"
import { jwtDecode } from "jwt-decode"

export const user = reactive({
    id: null,
    name: null,
    role: null,
    email: null,
    phone: null,
})

// 일반 로그인 설정 (회원가입/로그인 시)
export function setNormalLogin(token) {
    clearSocialLoginMarkers();
    localStorage.setItem('login_type', 'NORMAL');
    sessionStorage.setItem('login_type', 'NORMAL');
    const success = setUserFromToken(token);
    return success;
}

// 소셜 로그인 설정 (소셜 콜백 시)
export function setSocialLogin(token, provider, socialName = null, socialEmail = null, socialPhone = null) {
    clearNormalLoginMarkers();
    localStorage.setItem('login_type', 'SOCIAL');
    sessionStorage.setItem('login_type', 'SOCIAL');

    if (provider) {
        localStorage.setItem('social_provider', provider);
        sessionStorage.setItem('social_provider', provider);
    }

    if (socialName && socialName.trim() !== '' && socialName !== '사용자') {
        if (isGarbledKorean(socialName)) {
            const repairedName = repairGarbledKorean(socialName);
            socialName = repairedName;
        }

        localStorage.setItem('social_name', socialName);
        sessionStorage.setItem('social_name', socialName);
        localStorage.setItem('user_display_name', socialName);
        sessionStorage.setItem('current_user_name', socialName);
    }

    if (socialEmail) {
        localStorage.setItem('social_email', socialEmail);
        sessionStorage.setItem('social_email', socialEmail);
    }

    if (socialPhone) {
        localStorage.setItem('social_phone', socialPhone);
        sessionStorage.setItem('social_phone', socialPhone);
    }

    const success = setUserFromToken(token);

    if (socialName) {
        user.name = socialName;
    }
    if (socialEmail) {
        user.email = socialEmail;
    }
    if (socialPhone) {
        user.phone = socialPhone;
    }

    return success;
}

// 소셜 로그인 여부 확인
export const isSocialLoginUser = () => {
    const loginType = localStorage.getItem('login_type') || sessionStorage.getItem('login_type')

    if (loginType === 'SOCIAL') {
        return true
    }

    if (loginType === 'NORMAL') {
        return false
    }

    const socialProvider = localStorage.getItem('social_provider')
    const socialName = localStorage.getItem('social_name')

    if (socialProvider || socialName) {
        localStorage.setItem('login_type', 'SOCIAL')
        sessionStorage.setItem('login_type', 'SOCIAL')
        return true
    }

    return false
}

// 일반 로그인 여부 확인
export const isNormalLoginUser = () => {
    const loginType = localStorage.getItem('login_type') || sessionStorage.getItem('login_type');

    if (loginType === 'NORMAL') {
        return true;
    }

    if (loginType === 'SOCIAL') {
        return false;
    }

    const isSocial = isSocialLoginUser();

    if (!isSocial) {
        localStorage.setItem('login_type', 'NORMAL');
        sessionStorage.setItem('login_type', 'NORMAL');
        return true;
    }

    return false;
};

// 소셜 로그인 제공업체 확인
export const getSocialLoginProvider = () => {
    if (!isSocialLoginUser()) {
        return null
    }

    const provider = localStorage.getItem('social_provider') || sessionStorage.getItem('social_provider')
    return provider || 'UNKNOWN'
}

// 마커 제거 함수들
function clearSocialLoginMarkers() {
    localStorage.removeItem('social_provider');
    localStorage.removeItem('social_name');
    localStorage.removeItem('social_email');
    localStorage.removeItem('social_phone');
    localStorage.removeItem('social_login_name');
    localStorage.removeItem('preserved_user_name');

    sessionStorage.removeItem('social_provider');
    sessionStorage.removeItem('social_name');
    sessionStorage.removeItem('social_email');
    sessionStorage.removeItem('social_phone');
    sessionStorage.removeItem('social_login_type');
}

function clearNormalLoginMarkers() {
    localStorage.removeItem('normal_login');
    sessionStorage.removeItem('normal_login');
}

// 토큰에서 사용자 정보 설정
export function setUserFromToken(token) {
    try {
        const payload = jwtDecode(token)

        let userId = payload.sub
        if (userId === 'null' || userId === null || userId === undefined) {
            userId = payload.username
        }

        user.id = userId

        // 이름 처리 - 소셜 로그인이 아닌 경우에만
        if (!isSocialLoginUser()) {
            if (payload.name && payload.name.trim() && payload.name !== "사용자") {
                user.name = payload.name.trim()
                localStorage.setItem('user_display_name', user.name)
                sessionStorage.setItem('current_user_name', user.name)
            } else if (payload.username && payload.username !== userId) {
                user.name = payload.username
            } else {
                user.name = userId
            }
        }

        // 이메일, 휴대폰 처리 - 소셜 정보가 없는 경우에만
        if (!localStorage.getItem('social_email')) {
            const emailFields = ['email', 'mail', 'userEmail', 'emailAddress']
            for (const field of emailFields) {
                if (payload[field]) {
                    user.email = payload[field]
                    localStorage.setItem('user_email', payload[field])
                    sessionStorage.setItem('user_email', payload[field])
                    break
                }
            }
        }

        if (!localStorage.getItem('social_phone')) {
            const phoneFields = ['phone', 'phoneNumber', 'mobile', 'userPhone', 'tel', 'cellphone']
            for (const field of phoneFields) {
                if (payload[field]) {
                    user.phone = payload[field]
                    localStorage.setItem('user_phone', payload[field])
                    sessionStorage.setItem('user_phone', payload[field])
                    break
                }
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

// API에서 받은 데이터로 사용자 정보 업데이트
export function updateUserFromApi(userData) {
    try {
        if (!userData) return false

        user.id = userData.userId || userData.id || user.id

        // 소셜 로그인인 경우 소셜 정보 우선 사용
        if (isSocialLoginUser()) {
            const socialName = localStorage.getItem('social_name')
            const socialEmail = localStorage.getItem('social_email')
            const socialPhone = localStorage.getItem('social_phone')

            user.name = socialName || userData.name || user.name || "사용자"
            user.email = socialEmail || userData.email || user.email
            user.phone = socialPhone || userData.phone || user.phone
        } else {
            // 일반 로그인인 경우 API 데이터 사용
            if (userData.name && userData.name.trim() && userData.name !== "사용자") {
                user.name = userData.name.trim()
                localStorage.setItem('user_display_name', user.name)
                sessionStorage.setItem('current_user_name', user.name)
            }

            if (userData.email && userData.email.trim()) {
                user.email = userData.email.trim()
                localStorage.setItem('user_email', user.email)
                sessionStorage.setItem('user_email', user.email)
            }

            if (userData.phone && userData.phone.trim()) {
                user.phone = userData.phone.trim()
                localStorage.setItem('user_phone', user.phone)
                sessionStorage.setItem('user_phone', user.phone)
            }
        }

        user.role = userData.role || user.role || 'USER'

        return true
    } catch (error) {
        return false
    }
}

// 스토리지에서 사용자 정보 복원
export function restoreUserInfoFromStorage() {
    // 소셜 로그인인 경우
    if (isSocialLoginUser()) {
        const socialName = localStorage.getItem('social_name')
        const socialEmail = localStorage.getItem('social_email')
        const socialPhone = localStorage.getItem('social_phone')

        if (socialName) user.name = socialName
        if (socialEmail) user.email = socialEmail
        if (socialPhone) user.phone = socialPhone

        return
    }

    // 일반 로그인인 경우
    if (!user.email) {
        const savedEmail = localStorage.getItem('user_email') || sessionStorage.getItem('user_email')
        if (savedEmail) user.email = savedEmail
    }

    if (!user.phone) {
        const savedPhone = localStorage.getItem('user_phone') || sessionStorage.getItem('user_phone')
        if (savedPhone) user.phone = savedPhone
    }

    if (!user.name || user.name === "사용자") {
        const savedName = sessionStorage.getItem('current_user_name') || localStorage.getItem('user_display_name')
        if (savedName && savedName.trim() && savedName !== "사용자") {
            user.name = savedName
        }
    }
}

// 사용자 정보 초기화
export function initializeUser() {
    const token = localStorage.getItem('token')
    if (token && isTokenValid(token)) {
        setUserFromToken(token)
        restoreUserInfoFromStorage()
    } else if (token) {
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

// 토큰에서 사용자 ID 추출
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

// 로그인 성공 처리
export function loginSuccess(token) {
    localStorage.setItem('token', token)
    setNormalLogin(token)
}

// 사용자 정보 완전 초기화
export function resetUser() {
    user.id = null;
    user.name = null;
    user.role = null;
    user.email = null;
    user.phone = null;

    const keysToRemove = [
        'token', 'auth_token', 'access_token', 'userId',
        'login_type', 'social_provider', 'social_name', 'social_email', 'social_phone',
        'user_display_name', 'user_email', 'user_phone',
        'social_login_name', 'preserved_user_name', 'normal_login'
    ];

    keysToRemove.forEach(key => localStorage.removeItem(key));

    const sessionKeysToRemove = [
        'login_type', 'social_provider', 'social_name', 'social_email', 'social_phone',
        'auth_token', 'current_user_name', 'user_email', 'user_phone',
        'social_login_type', 'normal_login'
    ];

    sessionKeysToRemove.forEach(key => sessionStorage.removeItem(key));
}

// 로그인 여부 확인
export function isLoggedIn() {
    return !!user.id
}

// 현재 사용자 정보 반환
export function getCurrentUser() {
    return {
        id: user.id,
        name: user.name,
        role: user.role,
        email: user.email,
        phone: user.phone,
        loginType: localStorage.getItem('login_type'),
        isSocial: isSocialLoginUser()
    }
}

// 결제용 이름 백업
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

// 결제 후 이름 복원
export function restoreNameAfterPayment() {
    let restored = false

    const paymentName = localStorage.getItem('payment_user_name')
    if (paymentName && paymentName.trim()) {
        user.name = paymentName
        sessionStorage.setItem('current_user_name', paymentName)
        localStorage.removeItem('payment_user_name')
        restored = true
    } else {
        const savedName = sessionStorage.getItem('current_user_name') || localStorage.getItem('user_display_name')
        if (savedName && savedName.trim() && savedName !== "사용자") {
            user.name = savedName
            restored = true
        }
    }

    const paymentEmail = localStorage.getItem('payment_user_email')
    if (paymentEmail && paymentEmail.trim()) {
        user.email = paymentEmail
        sessionStorage.setItem('user_email', paymentEmail)
        localStorage.setItem('user_email', paymentEmail)
        localStorage.removeItem('payment_user_email')
        restored = true
    } else {
        const savedEmail = localStorage.getItem('user_email') || sessionStorage.getItem('user_email')
        if (savedEmail && !user.email) {
            user.email = savedEmail
            restored = true
        }
    }

    const paymentPhone = localStorage.getItem('payment_user_phone')
    if (paymentPhone && paymentPhone.trim()) {
        user.phone = paymentPhone
        sessionStorage.setItem('user_phone', paymentPhone)
        localStorage.setItem('user_phone', paymentPhone)
        localStorage.removeItem('payment_user_phone')
        restored = true
    } else {
        const savedPhone = localStorage.getItem('user_phone') || sessionStorage.getItem('user_phone')
        if (savedPhone && !user.phone) {
            user.phone = savedPhone
            restored = true
        }
    }

    sessionStorage.removeItem('payment_backup_time')
    return restored
}

// 깨진 한글 확인
function isGarbledKorean(text) {
    if (!text) return false

    const garbledPatterns = [
        /[ì í î ë ê é è]/g,
        /â[^a-zA-Z]/g,
        /Ã[^a-zA-Z]/g,
        /[\uFFFD]/g,
        /[^\x00-\x7F\uAC00-\uD7AF\u3131-\u318E]/g
    ]

    return garbledPatterns.some(pattern => pattern.test(text))
}

// 깨진 한글 복구
function repairGarbledKorean(garbledText) {
    try {
        const bytes = new Uint8Array(garbledText.length)
        for (let i = 0; i < garbledText.length; i++) {
            bytes[i] = garbledText.charCodeAt(i) & 0xFF
        }
        const repaired = new TextDecoder('utf-8').decode(bytes)

        if (!isGarbledKorean(repaired) && /[가-힣]/.test(repaired)) {
            return repaired
        }
    } catch (error) {
        // 복구 실패
    }

    return garbledText
}

// 앱 시작 시 자동으로 사용자 정보 초기화
initializeUser()
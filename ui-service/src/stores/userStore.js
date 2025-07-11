// userState.js - 통합된 사용자 상태 관리 (기존 userStore.js 기능 포함)

import { reactive } from 'vue';
import { jwtDecode } from "jwt-decode";

// 통합된 사용자 상태
export const userState = reactive({
    // 기존 ChatCommon용 필드들
    userId: '',
    currentUser: '',

    // 기존 userStore 필드들
    id: null,
    name: null,
    role: null,
    email: null,
    phone: null,
});

// 🔄 두 상태 시스템 동기화 함수
const syncUserStates = () => {
    // userStore -> userState 동기화
    userState.userId = userState.id;
    userState.currentUser = userState.name;

    console.log('🔄 사용자 상태 동기화:', {
        id: userState.id,
        name: userState.name,
        userId: userState.userId,
        currentUser: userState.currentUser
    });
};

// 로그인 성공 처리 (일반 + 소셜 통합)
export function loginSuccess(token, loginType = 'NORMAL', additionalInfo = null) {
    localStorage.setItem('jwt', token);
    localStorage.setItem('login_type', loginType);

    // 소셜 로그인 추가 정보 저장
    if (loginType === 'SOCIAL' && additionalInfo) {
        if (additionalInfo.provider) {
            localStorage.setItem('social_provider', additionalInfo.provider);
        }
        if (additionalInfo.name) {
            localStorage.setItem('social_name', additionalInfo.name);
        }
        if (additionalInfo.email) {
            localStorage.setItem('social_email', additionalInfo.email);
        }
    }

    const success = setUserFromToken(token);
    syncUserStates(); // 동기화 실행
    return success;
}

// 토큰에서 사용자 정보 설정
export function setUserFromToken(token) {
    try {
        const payload = jwtDecode(token);

        // 사용자 ID 설정
        userState.id = payload.sub || payload.username || null;

        // 이름 설정 (소셜 정보 우선)
        const socialName = localStorage.getItem('social_name');
        if (socialName) {
            userState.name = socialName;
        } else if (payload.name && payload.name.trim() && payload.name !== "사용자") {
            userState.name = payload.name.trim();
        } else {
            userState.name = userState.id || "사용자";
        }

        // 이메일 설정 (소셜 정보 우선)
        const socialEmail = localStorage.getItem('social_email');
        if (socialEmail) {
            userState.email = socialEmail;
        } else {
            const emailFields = ['email', 'mail', 'userEmail', 'emailAddress'];
            for (const field of emailFields) {
                if (payload[field]) {
                    userState.email = payload[field];
                    break;
                }
            }
        }

        // 기타 정보
        userState.role = payload.role || payload.authorities?.[0] || 'USER';

        if (payload.phone) {
            userState.phone = payload.phone;
        }

        // 🔄 동기화 실행
        syncUserStates();
        return true;
    } catch (e) {
        console.error('❌ 토큰 파싱 실패:', e);
        resetUser();
        return false;
    }
}

// 사용자 정보 초기화
export function initializeUser() {
    const token = localStorage.getItem('jwt');
    if (token && isTokenValid(token)) {
        setUserFromToken(token);
    } else {
        resetUser();
    }
}

// 토큰 유효성 검사
export function isTokenValid(token) {
    if (!token) return false;

    try {
        const payload = jwtDecode(token);
        const currentTime = Math.floor(Date.now() / 1000);
        return !(payload.exp && payload.exp < currentTime);
    } catch (error) {
        return false;
    }
}

// 사용자 정보 완전 초기화
export function resetUser() {
    // 모든 상태 초기화
    userState.id = null;
    userState.name = null;
    userState.role = null;
    userState.email = null;
    userState.phone = null;

    // 채팅용 상태도 초기화
    userState.userId = '';
    userState.currentUser = '';

    // localStorage 정리
    const keysToRemove = [
        'jwt', 'login_type', 'social_provider', 'social_name', 'social_email'
    ];
    keysToRemove.forEach(key => localStorage.removeItem(key));

    console.log('🧹 사용자 상태 완전 초기화');
}

// 로그인 여부 확인
export function isLoggedIn() {
    return !!userState.id && !!localStorage.getItem('jwt');
}

// 현재 사용자 정보 반환
export function getCurrentUser() {
    return {
        id: userState.id,
        name: userState.name,
        role: userState.role,
        email: userState.email,
        phone: userState.phone,
        loginType: localStorage.getItem('login_type') || 'NORMAL',
        isSocial: localStorage.getItem('login_type') === 'SOCIAL'
    };
}

// 소셜 로그인 여부 확인
export const isSocialLoginUser = () => {
    return localStorage.getItem('login_type') === 'SOCIAL';
};

// 일반 로그인 여부 확인
export const isNormalLoginUser = () => {
    return localStorage.getItem('login_type') === 'NORMAL';
};

// 소셜 로그인 제공업체 확인
export const getSocialLoginProvider = () => {
    return isSocialLoginUser() ? localStorage.getItem('social_provider') : null;
};

// 사용자 정보 업데이트 (API에서 받은 데이터로)
export function updateUserFromApi(userData) {
    if (!userData) return false;

    userState.id = userData.userId || userData.id || userState.id;

    if (userData.name && userData.name.trim() && userData.name !== "사용자") {
        userState.name = userData.name.trim();
    }

    if (userData.email && userData.email.trim()) {
        userState.email = userData.email.trim();
    }

    if (userData.phone && userData.phone.trim()) {
        userState.phone = userData.phone.trim();
    }

    userState.role = userData.role || userState.role || 'USER';

    // 🔄 동기화 실행
    syncUserStates();
    return true;
}

// 토큰에서 사용자 ID 추출
export function getUserIdFromToken() {
    const token = localStorage.getItem('jwt');
    if (!token) return null;

    try {
        const payload = jwtDecode(token);
        return payload.sub || payload.username || null;
    } catch {
        return null;
    }
}

// 결제용 함수들 (호환성 유지)
export function backupNameForPayment() {
    if (userState.name && userState.name.trim() && userState.name !== "사용자") {
        localStorage.setItem('payment_user_name', userState.name);
    }
    if (userState.email && userState.email.trim()) {
        localStorage.setItem('payment_user_email', userState.email);
    }
    if (userState.phone && userState.phone.trim()) {
        localStorage.setItem('payment_user_phone', userState.phone);
    }
    return true;
}

export function restoreNameAfterPayment() {
    const paymentName = localStorage.getItem('payment_user_name');
    const paymentEmail = localStorage.getItem('payment_user_email');
    const paymentPhone = localStorage.getItem('payment_user_phone');

    if (paymentName) {
        userState.name = paymentName;
        localStorage.removeItem('payment_user_name');
    }
    if (paymentEmail) {
        userState.email = paymentEmail;
        localStorage.removeItem('payment_user_email');
    }
    if (paymentPhone) {
        userState.phone = paymentPhone;
        localStorage.removeItem('payment_user_phone');
    }

    // 🔄 동기화 실행
    syncUserStates();
    return true;
}

// 호환성을 위한 추가 함수들
export function restoreUserInfoFromStorage() {
    return true;
}

export function setNormalLogin(token) {
    return loginSuccess(token, 'NORMAL');
}

export function setSocialLogin(token, provider, socialName = null, socialEmail = null, socialPhone = null) {
    return loginSuccess(token, 'SOCIAL', {
        provider,
        name: socialName,
        email: socialEmail,
        phone: socialPhone
    });
}

// 🆕 채팅용 사용자 정보 업데이트 함수
export function updateChatUserInfo(nickname, userId) {
    userState.currentUser = nickname;
    userState.userId = userId;

    // 기존 상태도 업데이트
    if (nickname && nickname.trim() && nickname !== "사용자") {
        userState.name = nickname;
    }
    if (userId) {
        userState.id = userId;
    }

    console.log('💬 채팅용 사용자 정보 업데이트:', {
        currentUser: userState.currentUser,
        userId: userState.userId,
        name: userState.name,
        id: userState.id
    });
}

// 🆕 강제 동기화 함수 (디버깅용)
export function forceSyncUserStates() {
    syncUserStates();
}

// 🆕 사용자 상태 확인 함수 (디버깅용)
export function checkUserState() {
    console.log('🔍 현재 사용자 상태:', {
        // 새로운 통합 상태
        id: userState.id,
        name: userState.name,
        role: userState.role,
        email: userState.email,
        phone: userState.phone,

        // 채팅용 상태
        userId: userState.userId,
        currentUser: userState.currentUser,

        // 토큰 정보
        hasToken: !!localStorage.getItem('jwt'),
        loginType: localStorage.getItem('login_type'),
        socialProvider: localStorage.getItem('social_provider'),
        socialName: localStorage.getItem('social_name'),

        // 로그인 상태
        isLoggedIn: isLoggedIn(),
        isSocial: isSocialLoginUser()
    });
}

// 🆕 하위 호환성을 위한 user 객체 (Header.vue에서 사용)
export const user = userState;

// 앱 시작 시 자동으로 사용자 정보 초기화
initializeUser();

// 전역 디버깅 함수
if (typeof window !== 'undefined') {
    window.userDebug = {
        checkUserState,
        forceSyncUserStates,
        resetUser,
        initializeUser,
        userState,
        user: userState // 호환성
    };
}
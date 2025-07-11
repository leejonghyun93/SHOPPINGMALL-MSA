// userStateBridge.js - 새로운 파일 생성 (기존 파일들 건드리지 않음)

import { userState } from '@/stores/userState.js';  // 채팅용 상태
import { user } from '@/stores/userStore.js';  // 헤더용 상태
import { watch } from 'vue';

// 🔄 두 상태 시스템 자동 동기화
class UserStateBridge {
    constructor() {
        this.isInitialized = false;
        this.init();
    }

    init() {
        if (this.isInitialized) return;

        console.log('🌉 사용자 상태 브리지 초기화');

        // userStore -> userState 동기화 (헤더 -> 채팅)
        this.syncFromUserStoreToUserState();

        // userState -> userStore 동기화 (채팅 -> 헤더)
        this.syncFromUserStateToUserStore();

        // 초기 동기화 실행
        this.performInitialSync();

        this.isInitialized = true;
    }

    // 헤더 상태 변화 감지 -> 채팅 상태 업데이트
    syncFromUserStoreToUserState() {
        // user.id 변화 감지
        watch(() => user.id, (newId, oldId) => {
            if (newId !== oldId) {
                console.log('🔄 [헤더->채팅] ID 동기화:', newId);
                userState.userId = newId || '';
                userState.id = newId;
            }
        });

        // user.name 변화 감지
        watch(() => user.name, (newName, oldName) => {
            if (newName !== oldName) {
                console.log('🔄 [헤더->채팅] 이름 동기화:', newName);
                userState.currentUser = newName || '';
                userState.name = newName;
            }
        });

        // user.email 변화 감지
        watch(() => user.email, (newEmail, oldEmail) => {
            if (newEmail !== oldEmail) {
                console.log('🔄 [헤더->채팅] 이메일 동기화:', newEmail);
                userState.email = newEmail;
            }
        });

        // user.role 변화 감지
        watch(() => user.role, (newRole, oldRole) => {
            if (newRole !== oldRole) {
                console.log('🔄 [헤더->채팅] 역할 동기화:', newRole);
                userState.role = newRole;
            }
        });
    }

    // 채팅 상태 변화 감지 -> 헤더 상태 업데이트
    syncFromUserStateToUserStore() {
        // userState.id 변화 감지
        watch(() => userState.id, (newId, oldId) => {
            if (newId !== oldId && newId !== user.id) {
                console.log('🔄 [채팅->헤더] ID 동기화:', newId);
                user.id = newId;
                userState.userId = newId || '';
            }
        });

        // userState.name 변화 감지
        watch(() => userState.name, (newName, oldName) => {
            if (newName !== oldName && newName !== user.name) {
                console.log('🔄 [채팅->헤더] 이름 동기화:', newName);
                user.name = newName;
                userState.currentUser = newName || '';
            }
        });

        // userState.currentUser 변화 감지 (채팅 전용)
        watch(() => userState.currentUser, (newCurrentUser, oldCurrentUser) => {
            if (newCurrentUser !== oldCurrentUser && newCurrentUser !== user.name) {
                console.log('🔄 [채팅->헤더] 현재사용자 동기화:', newCurrentUser);
                user.name = newCurrentUser;
                userState.name = newCurrentUser;
            }
        });

        // userState.userId 변화 감지 (채팅 전용)
        watch(() => userState.userId, (newUserId, oldUserId) => {
            if (newUserId !== oldUserId && newUserId !== user.id) {
                console.log('🔄 [채팅->헤더] 사용자ID 동기화:', newUserId);
                user.id = newUserId;
                userState.id = newUserId;
            }
        });
    }

    // 초기 동기화 실행
    performInitialSync() {
        console.log('🔄 초기 동기화 실행');

        // 헤더 상태가 우선이라고 가정
        if (user.id) {
            userState.id = user.id;
            userState.userId = user.id;
        }

        if (user.name) {
            userState.name = user.name;
            userState.currentUser = user.name;
        }

        if (user.email) {
            userState.email = user.email;
        }

        if (user.role) {
            userState.role = user.role;
        }

        console.log('✅ 초기 동기화 완료:', {
            헤더: { id: user.id, name: user.name, email: user.email, role: user.role },
            채팅: { id: userState.id, name: userState.name, currentUser: userState.currentUser, userId: userState.userId }
        });
    }

    // 수동 동기화 (디버깅용)
    forceSync() {
        console.log('🔄 강제 동기화 실행');
        this.performInitialSync();
    }

    // 상태 확인 (디버깅용)
    checkSync() {
        console.log('🔍 동기화 상태 확인:', {
            헤더상태: {
                id: user.id,
                name: user.name,
                email: user.email,
                role: user.role,
                phone: user.phone
            },
            채팅상태: {
                id: userState.id,
                name: userState.name,
                currentUser: userState.currentUser,
                userId: userState.userId,
                email: userState.email,
                role: userState.role,
                phone: userState.phone
            },
            토큰정보: {
                hasToken: !!localStorage.getItem('jwt'),
                loginType: localStorage.getItem('login_type'),
                socialProvider: localStorage.getItem('social_provider'),
                socialName: localStorage.getItem('social_name')
            }
        });
    }
}

// 싱글톤 인스턴스 생성
const bridge = new UserStateBridge();

// 전역 디버깅 함수
if (typeof window !== 'undefined') {
    window.userStateBridge = {
        forceSync: () => bridge.forceSync(),
        checkSync: () => bridge.checkSync(),
        bridge: bridge
    };
}

export default bridge;
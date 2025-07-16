import { userState } from '@/stores/userState.js';  // 채팅용 상태
import { user } from '@/stores/userStore.js';      // 헤더용 상태
import { watch } from 'vue';

class UserStateBridge {
    constructor() {
        this.isInitialized = false;
        this.init();
    }

    init() {
        if (this.isInitialized) return;

        this.syncFromUserStoreToUserState();
        this.syncFromUserStateToUserStore();
        this.performInitialSync();

        this.isInitialized = true;
    }

    syncFromUserStoreToUserState() {
        watch(() => user.id, (newId, oldId) => {
            if (newId !== oldId) {
                userState.userId = newId || '';
                userState.id = newId;
            }
        });

        watch(() => user.name, (newName, oldName) => {
            if (newName !== oldName) {
                userState.currentUser = newName || '';
                userState.name = newName;
            }
        });

        watch(() => user.email, (newEmail, oldEmail) => {
            if (newEmail !== oldEmail) {
                userState.email = newEmail;
            }
        });

        watch(() => user.role, (newRole, oldRole) => {
            if (newRole !== oldRole) {
                userState.role = newRole;
            }
        });
    }

    syncFromUserStateToUserStore() {
        watch(() => userState.id, (newId, oldId) => {
            if (newId !== oldId && newId !== user.id) {
                user.id = newId;
                userState.userId = newId || '';
            }
        });

        watch(() => userState.name, (newName, oldName) => {
            if (newName !== oldName && newName !== user.name) {
                user.name = newName;
                userState.currentUser = newName || '';
            }
        });

        watch(() => userState.currentUser, (newCurrentUser, oldCurrentUser) => {
            if (newCurrentUser !== oldCurrentUser && newCurrentUser !== user.name) {
                user.name = newCurrentUser;
                userState.name = newCurrentUser;
            }
        });

        watch(() => userState.userId, (newUserId, oldUserId) => {
            if (newUserId !== oldUserId && newUserId !== user.id) {
                user.id = newUserId;
                userState.id = newUserId;
            }
        });
    }

    performInitialSync() {
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
    }

    forceSync() {
        this.performInitialSync();
    }

    checkSync() {
        // 로그 출력을 제거하였으므로 빈 함수로 둠 (필요 시 구현 가능)
    }
}

const bridge = new UserStateBridge();

if (typeof window !== 'undefined') {
    window.userStateBridge = {
        forceSync: () => bridge.forceSync(),
        checkSync: () => bridge.checkSync(),
        bridge: bridge
    };
}

export default bridge;

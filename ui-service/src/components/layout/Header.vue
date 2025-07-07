<template>
  <nav class="navbar navbar-dark bg-white custom-navbar d-flex justify-content-between align-items-center">

    <!-- 왼쪽: 홈 -->
    <div class="d-flex align-items-center gap-2">
      <router-link to="/" class="navbar-brand">트라이마켓</router-link>
      <router-link to="/" class="navbar-brand">홈</router-link>
      <router-link to="/broadcasts/category" class="navbar-brand">라이브 목록</router-link>
      <router-link to="/broadcasts/calendar" class="navbar-brand">예고</router-link>
      <router-link to="/category" class="navbar-brand">카테고리</router-link>
    </div>

    <!-- 오른쪽: 메뉴들 -->
    <div class="d-flex align-items-center">
      <!-- 검색창 -->
      <div class="input-group search-box me-2">
        <input
            type="text"
            class="form-control form-control-sm"
            placeholder="상품명을 입력하세요"
            v-model="searchKeyword"
            @keyup.enter="performSearch"
        />
        <button
            class="input-group-text search-btn"
            @click="performSearch"
            :disabled="!searchKeyword.trim()"
        >
          검색
        </button>
      </div>

      <!-- 로그인/회원가입 (로그인 안된 상태) -->
      <router-link v-if="!computedUser.id" to="/login" class="navbar-brand mx-2">로그인</router-link>
      <router-link v-if="!computedUser.id" to="/register" class="navbar-brand mx-2">회원가입</router-link>

      <!-- 사용자 메뉴 (로그인된 상태) -->
      <div v-if="computedUser.id" class="user-menu-container" @mouseenter="showDropdown" @mouseleave="hideDropdown">
        <span class="navbar-brand mx-2 user-name">
          {{ displayUserName }} 님 ▼
          <!-- 소셜 로그인 표시 -->
          <span v-if="isSocialUser" class="social-indicator" :title="`${getSocialProviderName()} 로그인`">
            {{ getSocialProviderIcon() }}
          </span>
        </span>

        <!-- 드롭다운 메뉴 -->
        <div class="dropdown-menu" :class="{ 'show': isDropdownVisible }">
          <router-link to="/mypage/orders" class="dropdown-item" @click="hideDropdown">
            마이페이지
          </router-link>
          <!-- 소셜 로그인 사용자 제한 적용 -->
          <div v-if="!isSocialUser" @click="navigateToProfile" class="dropdown-item">
            회원정보관리
          </div>
          <div v-else @click="showSocialAlert" class="dropdown-item social-restricted">
            <span>회원정보관리</span>
            <span class="restriction-badge"></span>
          </div>
          <div class="dropdown-divider"></div>
          <button @click="logout" class="dropdown-item logout-btn">
            로그아웃
          </button>
        </div>
      </div>

      <!-- 장바구니 아이콘 -->
      <router-link to="/cart" class="navbar-brand mx-2 icon-link" title="장바구니">
        <div class="icon-container">
          <svg class="svg-icon" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1"></circle>
            <circle cx="20" cy="21" r="1"></circle>
            <path d="m1 1 4 4 2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
          </svg>
          <span v-if="cartCount > 0" class="badge">{{ cartCount }}</span>
        </div>
      </router-link>

      <!-- 실시간 알림 아이콘 -->
      <div v-if="computedUser.id"
           class="notification-container mx-2"
           @mouseenter="showNotificationDropdown"
           @mouseleave="hideNotificationDropdown"
           title="알림">
        <div class="notification-icon icon-link">
          <svg class="svg-icon" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"></path>
            <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"></path>
          </svg>
          <span v-if="unreadNotificationCount > 0" class="notification-count-badge">
            {{ unreadNotificationCount > 99 ? '99+' : unreadNotificationCount }}
          </span>
        </div>

        <!-- 알림 드롭다운 메뉴 -->
        <div class="notification-dropdown" :class="{ 'show': isNotificationDropdownVisible }">
          <div class="notification-header">
            <h6 class="mb-0">알림</h6>
            <button v-if="notifications.length > 0 && unreadNotificationCount > 0"
                    @click="markAllAsRead"
                    class="btn btn-sm btn-link p-0">
              모두 읽음
            </button>
          </div>

          <div class="notification-list">
            <!-- 로딩 중 -->
            <div v-if="isLoadingNotifications" class="notification-loading">
              <div class="spinner-border spinner-border-sm" role="status">
                <span class="visually-hidden">Loading...</span>
              </div>
              <span class="ms-2">알림을 불러오는 중...</span>
            </div>

            <!-- 알림 목록 -->
            <div v-else-if="notifications.length > 0">
              <div v-for="notification in notifications"
                   :key="notification.notificationId"
                   class="notification-item"
                   :class="{
                     'unread': !notification.isRead,
                     'broadcast-start': notification.type === 'BROADCAST_START',
                     'high-priority': notification.priority === 'HIGH' || notification.priority === 'URGENT'
                   }"
                   @click="handleNotificationClick(notification)">

                <div class="notification-type-icon">
                  {{ notification.type === 'BROADCAST_START' ? '' :
                    notification.type === 'BROADCAST_END' ? '' :
                        notification.type === 'BROADCAST_REMINDER' ? '' : '' }}
                </div>

                <div class="notification-content">
                  <div class="notification-title">{{ notification.title }}</div>
                  <div class="notification-message">{{ notification.message }}</div>
                  <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
                </div>

                <div v-if="!notification.isRead" class="unread-indicator">
                  <div class="unread-dot"></div>
                </div>
              </div>
            </div>

            <!-- 알림 없음 -->
            <div v-else class="no-notifications">
              <div class="text-muted text-center py-3">
                <div class="mb-2"></div>
                <div class="mb-1">알림</div>
                <small>새로운 알림이 없습니다</small>
              </div>
            </div>
          </div>

          <div v-if="notifications.length > 0" class="notification-footer">
            <router-link to="/notifications"
                         class="btn btn-sm btn-outline-primary w-100"
                         @click="hideNotificationDropdown">
              모든 알림 보기
              <span v-if="unreadNotificationCount > 10" class="ms-1">
                ({{ unreadNotificationCount - 10 }}개 더)
              </span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { onMounted, computed, ref, onUnmounted, watch } from "vue";
import { useRouter } from "vue-router";
import { user, resetUser, updateUserFromApi, setUserFromToken, isSocialLoginUser, getSocialLoginProvider } from "@/stores/userStore";
import apiClient from '@/api/axiosInstance'
import { notificationApiCall, notificationHelpers } from '@/config/notificationConfig'

const router = useRouter();
const isDropdownVisible = ref(false);
const isNotificationDropdownVisible = ref(false);
const isLoadingNotifications = ref(false);
const cartCount = ref(0);
const unreadNotificationCount = ref(0);
const notifications = ref([]);
const searchKeyword = ref('');
const isUserInfoLoaded = ref(false);

// 소셜 로그인 관련 상태
const isSocialUser = ref(false);
const socialProvider = ref(null);

let notificationPollingInterval = null;

const computedUser = computed(() => user);

const displayUserName = computed(() => {
  console.log('🔍 Header.vue - displayUserName computed:', {
    isUserInfoLoaded: isUserInfoLoaded.value,
    userId: computedUser.value.id,
    userName: computedUser.value.name,
    userNameTrim: computedUser.value.name ? computedUser.value.name.trim() : null
  });

  if (!isUserInfoLoaded.value && computedUser.value.id) {
    return computedUser.value.name || "사용자";
  }

  if (computedUser.value.name &&
      computedUser.value.name.trim() &&
      computedUser.value.name !== computedUser.value.id) {
    return computedUser.value.name;
  }

  return "사용자";
});

// 소셜 로그인 여부 체크 함수
const checkSocialLoginStatus = () => {
  isSocialUser.value = isSocialLoginUser();
  socialProvider.value = getSocialLoginProvider();

  console.log('🔍 Header.vue - checkSocialLoginStatus:', {
    isSocialUser: isSocialUser.value,
    socialProvider: socialProvider.value
  });
};

// 소셜 로그인 제공업체 이름 반환
const getSocialProviderName = () => {
  switch (socialProvider.value) {
    case 'KAKAO':
    case 'kakao':
      return '카카오';
    case 'NAVER':
    case 'naver':
      return '네이버';
    case 'GOOGLE':
    case 'google':
      return '구글';
    default:
      return '소셜';
  }
};

// 소셜 로그인 제공업체 아이콘 반환
const getSocialProviderIcon = () => {
  switch (socialProvider.value) {
    case 'KAKAO':
    case 'kakao':
      return '';
    case 'NAVER':
    case 'naver':
      return '';
    case 'GOOGLE':
    case 'google':
      return '';
    default:
      return '';
  }
};

// 소셜 로그인 사용자 알림 표시
const showSocialAlert = () => {
  const providerName = getSocialProviderName();

  console.log('🔍 Header.vue - showSocialAlert:', {
    providerName,
    isSocialUser: isSocialUser.value
  });

  alert(`${providerName} 로그인으로 가입하신 회원은 보안상의 이유로 회원정보 수정이 제한됩니다.\n\n개인정보 변경이 필요한 경우 ${providerName} 계정에서 직접 수정해주세요.`);
  hideDropdown();
};

// 회원정보관리 네비게이션 함수
const navigateToProfile = () => {
  console.log('🔍 Header.vue - navigateToProfile called');
  hideDropdown();
  router.push('/mypage/profile');
};

watch(() => computedUser.value.id, async (newUserId, oldUserId) => {
  console.log('🔍 Header.vue - user.id watcher:', {
    newUserId,
    oldUserId,
    currentUser: computedUser.value
  });

  if (newUserId && newUserId !== oldUserId) {
    isUserInfoLoaded.value = false;

    try {
      await validateUserInfo();

      // 소셜 로그인 여부 체크
      checkSocialLoginStatus();

      await Promise.all([
        fetchCartCount(),
        fetchNotifications()
      ]);

      stopNotificationPolling();
      startNotificationPolling();
    } catch (error) {
      console.error('🔍 Header.vue - user.id watcher error:', error);
    } finally {
      isUserInfoLoaded.value = true;
    }
  } else if (!newUserId && oldUserId) {
    console.log('🔍 Header.vue - user logout detected');
    resetUserData();
    isUserInfoLoaded.value = false;
    // 소셜 로그인 상태도 초기화
    isSocialUser.value = false;
    socialProvider.value = null;
  }
}, { immediate: false });

const resetUserData = () => {
  console.log('🔍 Header.vue - resetUserData called');
  cartCount.value = 0;
  unreadNotificationCount.value = 0;
  notifications.value = [];
  stopNotificationPolling();
};

const performSearch = () => {
  const keyword = searchKeyword.value.trim();
  console.log('🔍 Header.vue - performSearch:', { keyword });

  if (!keyword) return;

  router.push({
    path: '/category',
    query: { search: keyword }
  });
};

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

    if (payload.exp && payload.exp < currentTime) {
      return false
    }

    return true
  } catch (error) {
    return false
  }
}

const validateUserInfo = async () => {
  console.log('🔍 Header.vue - validateUserInfo called');

  // 소셜 로그인 이름이 있으면 API 호출 건너뛰기
  const socialName = localStorage.getItem('social_login_name') ||
      sessionStorage.getItem('current_user_name') ||
      localStorage.getItem('preserved_user_name');

  console.log('🔍 Header.vue - validateUserInfo socialName check:', { socialName });

  if (socialName && socialName.trim() && socialName !== "사용자") {
    user.name = socialName;
    sessionStorage.setItem('current_user_name', socialName);
    isUserInfoLoaded.value = true;
    console.log('🔍 Header.vue - using social name:', socialName);
    return true;
  }

  // 이미 로딩된 상태이고 유효한 이름이 있으면 재검증 생략
  if (isUserInfoLoaded.value && user.name && user.name.trim() && user.name !== "사용자") {
    console.log('🔍 Header.vue - using cached user info');
    return true;
  }

  try {
    const response = await apiClient.get('/api/users/profile', {
      timeout: 3000,
      validateStatus: function (status) {
        return status < 500;
      }
    });

    console.log('🔍 Header.vue - API response:', response);

    if (response.status === 200 && response.data && response.data.success && response.data.data) {
      const userData = response.data.data;

      // 소셜 로그인 이름이 있으면 API 이름 완전 무시
      const preservedName = localStorage.getItem('social_login_name') ||
          sessionStorage.getItem('current_user_name') ||
          localStorage.getItem('preserved_user_name');

      // 기본 정보 업데이트
      user.id = userData.userId || userData.id;
      user.email = userData.email;
      user.role = userData.role || 'USER';
      user.phone = userData.phone;

      if (preservedName && preservedName.trim() && preservedName !== "사용자") {
        user.name = preservedName;
        sessionStorage.setItem('current_user_name', preservedName);
        console.log('🔍 Header.vue - using preserved name:', preservedName);
      } else if (userData.name && userData.name.trim()) {
        user.name = userData.name.trim();
        sessionStorage.setItem('current_user_name', user.name);
        console.log('🔍 Header.vue - using API name:', user.name);
      } else {
        user.name = "사용자";
        console.log('🔍 Header.vue - defaulting to "사용자"');
      }

      isUserInfoLoaded.value = true;
      return true;
    } else if (response.status === 401) {
      console.log('🔍 Header.vue - 401 response, falling back to token');
      return handleTokenFallback();
    } else {
      console.log('🔍 Header.vue - API error, falling back to token');
      return handleTokenFallback();
    }
  } catch (error) {
    console.error('🔍 Header.vue - validateUserInfo error:', error);
    return handleTokenFallback();
  }
};

const handleTokenFallback = () => {
  console.log('🔍 Header.vue - handleTokenFallback called');

  if (user.id) {
    isUserInfoLoaded.value = true;

    // API 실패 시에도 보존된 이름 복원
    const preservedName = localStorage.getItem('social_login_name') ||
        sessionStorage.getItem('current_user_name') ||
        localStorage.getItem('preserved_user_name');

    console.log('🔍 Header.vue - handleTokenFallback preservedName:', preservedName);

    if (preservedName && preservedName.trim() && preservedName !== "사용자") {
      user.name = preservedName;
      sessionStorage.setItem('current_user_name', preservedName);
      console.log('🔍 Header.vue - restored preserved name:', preservedName);
    } else if (!user.name || user.name === "사용자") {
      // 토큰에서 이름 추출 시도
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

            console.log('🔍 Header.vue - token payload:', payload);

            if (payload.name && payload.name.trim() && payload.name !== payload.sub) {
              user.name = payload.name;
              sessionStorage.setItem('current_user_name', payload.name);
              localStorage.setItem('social_login_name', payload.name);
              console.log('🔍 Header.vue - extracted name from token:', payload.name);
            }
          }
        } catch (e) {
          console.error('🔍 Header.vue - token parsing error:', e);
        }
      }
    }
    return true;
  }
  return false;
};

const refreshUserInfo = async () => {
  console.log('🔍 Header.vue - refreshUserInfo called');
  isUserInfoLoaded.value = false;
  const token = localStorage.getItem('token');

  if (token && isTokenValid(token)) {
    setUserFromToken(token);
    await validateUserInfo();
  }
};

window.refreshHeaderUserInfo = refreshUserInfo;

const fetchCartCount = async () => {
  if (!computedUser.value.id) return;

  try {
    const cartResponse = await apiClient.get('/api/cart/count');
    if (cartResponse.data.success) {
      cartCount.value = cartResponse.data.data.count || 0;
      console.log('🔍 Header.vue - cart count updated:', cartCount.value);
    }
  } catch (error) {
    console.error('🔍 Header.vue - fetchCartCount error:', error);
  }
}

const fetchNotifications = async () => {
  if (!computedUser.value.id) return;

  try {
    const unreadResponse = await notificationApiCall(`/unread-count?userId=${computedUser.value.id}`);
    if (unreadResponse && unreadResponse.ok) {
      const unreadData = await unreadResponse.json();
      unreadNotificationCount.value = unreadData.count || 0;
      console.log('🔍 Header.vue - unread notifications:', unreadNotificationCount.value);
    }

    const notificationsResponse = await notificationApiCall(`/recent?userId=${computedUser.value.id}&limit=10`);
    if (notificationsResponse && notificationsResponse.ok) {
      const notificationsData = await notificationsResponse.json();
      notifications.value = notificationsData || [];
      console.log('🔍 Header.vue - notifications loaded:', notifications.value.length);
    }
  } catch (error) {
    console.error('🔍 Header.vue - fetchNotifications error:', error);
  }
}

const startNotificationPolling = () => {
  if (!computedUser.value.id) return;
  console.log('🔍 Header.vue - starting notification polling');
  notificationPollingInterval = setInterval(() => {
    fetchNotifications();
  }, 10000);
}

const stopNotificationPolling = () => {
  if (notificationPollingInterval) {
    console.log('🔍 Header.vue - stopping notification polling');
    clearInterval(notificationPollingInterval);
    notificationPollingInterval = null;
  }
}

const formatTime = (timeString) => {
  return notificationHelpers.formatTime(timeString);
}

const handleNotificationClick = async (notification) => {
  try {
    if (!notification.isRead) {
      const success = await notificationHelpers.markAsRead(notification.notificationId, computedUser.value.id);
      if (success) {
        notification.isRead = true;
        notification.readAt = new Date().toISOString();
        unreadNotificationCount.value = Math.max(0, unreadNotificationCount.value - 1);
      }
    }

    hideNotificationDropdown();

    if (notification.type === 'BROADCAST_START') {
      if (notification.broadcastId) {
        router.push(`/live/${notification.broadcastId}`);
      } else {
        router.push('/broadcasts/category');
      }
    } else {
      router.push('/notifications');
    }
  } catch (error) {
    console.error('🔍 Header.vue - handleNotificationClick error:', error);
    hideNotificationDropdown();
  }
}

const markAllAsRead = async () => {
  try {
    const success = await notificationHelpers.markAllAsRead(computedUser.value.id);
    if (success) {
      notifications.value.forEach(notification => {
        notification.isRead = true;
        notification.readAt = new Date().toISOString();
      });
      unreadNotificationCount.value = 0;
      console.log('🔍 Header.vue - all notifications marked as read');
    }
  } catch (error) {
    console.error('🔍 Header.vue - markAllAsRead error:', error);
  }
}

function showDropdown() {
  isDropdownVisible.value = true;
}

function hideDropdown() {
  setTimeout(() => {
    isDropdownVisible.value = false;
  }, 150);
}

function showNotificationDropdown() {
  isNotificationDropdownVisible.value = true;
  if (notifications.value.length === 0) {
    isLoadingNotifications.value = true;
    fetchNotifications().finally(() => {
      isLoadingNotifications.value = false;
    });
  }
}

function hideNotificationDropdown() {
  setTimeout(() => {
    isNotificationDropdownVisible.value = false;
  }, 150);
}

function logout() {
  console.log('🔍 Header.vue - logout called');
  stopNotificationPolling();
  resetUser();
  resetUserData();
  searchKeyword.value = '';
  isDropdownVisible.value = false;
  isUserInfoLoaded.value = false;
  // 소셜 로그인 상태도 초기화
  isSocialUser.value = false;
  socialProvider.value = null;
  router.push("/login");
}

onMounted(async () => {
  console.log('🔍 Header.vue - onMounted called');
  const token = localStorage.getItem("token");

  if (token && isTokenValid(token)) {
    try {
      if (user.id && user.name) {
        isUserInfoLoaded.value = true;
        console.log('🔍 Header.vue - user already loaded');
      }

      const isValid = await validateUserInfo();

      if (isValid) {
        // 소셜 로그인 여부 체크
        checkSocialLoginStatus();

        await Promise.all([
          fetchCartCount(),
          fetchNotifications()
        ]);

        startNotificationPolling();
      } else {
        localStorage.removeItem("token");
        resetUserState();
      }

    } catch (error) {
      console.error('🔍 Header.vue - onMounted error:', error);
      localStorage.removeItem("token");
      resetUserState();
    }
  } else {
    if (token) {
      localStorage.removeItem("token");
    }
    resetUserState();
  }
});

const resetUserState = () => {
  console.log('🔍 Header.vue - resetUserState called');
  user.id = null;
  user.name = null;
  user.role = null;
  user.email = null;
  resetUserData();
  isUserInfoLoaded.value = false;
  // 소셜 로그인 상태도 초기화
  isSocialUser.value = false;
  socialProvider.value = null;
};

onUnmounted(() => {
  console.log('🔍 Header.vue - onUnmounted called');
  stopNotificationPolling();
});
</script>

<style scoped src="@/assets/css/header.css">

</style>
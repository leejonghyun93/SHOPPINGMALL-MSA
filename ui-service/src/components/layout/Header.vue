<template>
  <!-- 기존 template 그대로 유지 -->
  <nav class="navbar navbar-dark bg-white custom-navbar d-flex justify-content-between align-items-center">
    <!-- 왼쪽: 홈 -->
    <div class="d-flex align-items-center gap-2">
      <router-link to="/" class="navbar-brand"><img src="@/assets/logo-trimarket.png" alt="TriMarket Logo" class="logo" /></router-link>
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
          <span v-if="isSocialUser" class="social-indicator" :title="`${getSocialProviderName()} 로그인`">
            {{ getSocialProviderIcon() }}
          </span>
        </span>

        <!-- 드롭다운 메뉴 -->
        <div class="dropdown-menu" :class="{ 'show': isDropdownVisible }">
          <router-link to="/mypage/orders" class="dropdown-item" @click="hideDropdown">
            마이페이지
          </router-link>
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

      <!-- 안전한 알림 아이콘 (이메일 시스템에 영향 X) -->
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
                  {{ notification.type === 'BROADCAST_START' ? '🔴' :
                    notification.type === 'BROADCAST_END' ? '⚫' :
                        notification.type === 'BROADCAST_REMINDER' ? '⏰' : '📢' }}
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
                <div class="mb-2">🔔</div>
                <div class="mb-1">알림이 없습니다</div>
                <small>새로운 알림이 없습니다</small>
              </div>
            </div>
          </div>

          <div v-if="notifications.length > 0" class="notification-footer">
            <router-link to="/notifications"
                         class="btn btn-sm btn-outline-primary w-100"
                         @click="hideNotificationDropdown">
              모든 알림 보기
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
import { user, resetUser, setUserFromToken, isSocialLoginUser, getSocialLoginProvider } from "@/stores/userStore";
import apiClient from '@/api/axiosInstance'
import { notificationApiCall, notificationHelpers } from '@/config/notificationConfig'

const router = useRouter();

// 상태 변수들
const isDropdownVisible = ref(false);
const isNotificationDropdownVisible = ref(false);
const isLoadingNotifications = ref(false);
const cartCount = ref(0);
const unreadNotificationCount = ref(0);
const notifications = ref([]);
const searchKeyword = ref('');
const isUserInfoLoaded = ref(false);
const isSocialUser = ref(false);
const socialProvider = ref(null);

let notificationPollingInterval = null;

// 계산된 속성
const computedUser = computed(() => user);

const displayUserName = computed(() => {
  if (localStorage.getItem('login_type') === 'SOCIAL') {
    if (computedUser.value.name &&
        computedUser.value.name.trim() &&
        computedUser.value.name !== "사용자" &&
        computedUser.value.name !== "소셜사용자" &&
        computedUser.value.name !== computedUser.value.id &&
        computedUser.value.name.length >= 2) {
      return computedUser.value.name;
    }

    const sessionSocialName = sessionStorage.getItem('social_name');
    if (sessionSocialName &&
        sessionSocialName.trim() &&
        sessionSocialName !== "사용자" &&
        sessionSocialName !== "소셜사용자" &&
        sessionSocialName.length >= 2) {
      return sessionSocialName;
    }

    const localSocialName = localStorage.getItem('social_name');
    if (localSocialName &&
        localSocialName.trim() &&
        localSocialName !== "사용자" &&
        localSocialName !== "소셜사용자" &&
        localSocialName.length >= 2) {
      return localSocialName;
    }

    const provider = localStorage.getItem('social_provider');
    const providerNames = {
      'KAKAO': '카카오사용자',
      'NAVER': '네이버사용자',
      'GOOGLE': '구글사용자'
    };
    return providerNames[provider?.toUpperCase()] || '소셜사용자';
  }

  if (computedUser.value.name &&
      computedUser.value.name.trim() &&
      computedUser.value.name !== computedUser.value.id &&
      computedUser.value.name !== "사용자" &&
      computedUser.value.name.length >= 1) {
    return computedUser.value.name;
  }

  const savedName = sessionStorage.getItem('current_user_name') ||
      localStorage.getItem('user_display_name');
  if (savedName &&
      savedName.trim() &&
      savedName !== "사용자" &&
      savedName.length >= 1) {
    return savedName;
  }

  return "사용자";
});

// 소셜 로그인 관련 함수들
const checkSocialLoginStatus = () => {
  isSocialUser.value = isSocialLoginUser();
  socialProvider.value = getSocialLoginProvider();
};

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

const getSocialProviderIcon = () => {
  switch (socialProvider.value) {
    case 'KAKAO':
    case 'kakao':
      return '💬';
    case 'NAVER':
    case 'naver':
      return '🟢';
    case 'GOOGLE':
    case 'google':
      return '🔵';
    default:
      return '👤';
  }
};

const showSocialAlert = () => {
  const providerName = getSocialProviderName();
  alert(`${providerName} 로그인으로 가입하신 회원은 보안상의 이유로 회원정보 수정이 제한됩니다.\n\n개인정보 변경이 필요한 경우 ${providerName} 계정에서 직접 수정해주세요.`);
  hideDropdown();
};

const navigateToProfile = () => {
  hideDropdown();
  router.push('/mypage/profile');
};

// 실제 방송 상태 확인 함수
const checkBroadcastStatus = async (broadcastId) => {
  try {
    const response = await apiClient.get(`/api/broadcasts/${broadcastId}`, {
      timeout: 5000,
      validateStatus: function (status) {
        return status < 500;
      }
    });

    if (response.status === 200 && response.data) {
      const broadcast = response.data.data || response.data;
      const status = broadcast.broadcast_status || broadcast.broadcastStatus;
      return status === 'live';
    }

    return false;

  } catch (error) {
    return false;
  }
};

// 방송 시작 알림만 실제 상태로 필터링
const filterBroadcastNotifications = async (notifications) => {
  const filteredNotifications = [];

  for (const notification of notifications) {
    // 방송 시작 알림이 아니면 그대로 포함
    if (notification.type !== 'BROADCAST_START') {
      filteredNotifications.push(notification);
      continue;
    }

    // 방송 시작 알림인 경우 실제 라이브 중인지 확인
    if (notification.broadcastId) {
      const isLive = await checkBroadcastStatus(notification.broadcastId);

      if (isLive) {
        // 메시지를 현재 상황에 맞게 수정
        const updatedNotification = {
          ...notification,
          title: notification.title.replace('방송 시작 알림', '라이브 방송 중'),
          message: notification.message
              .replace('방송이 시작되면 알려드릴게요!', '지금 라이브 방송 중입니다!')
              .replace('방송이 시작되었습니다!', '지금 라이브 방송 중입니다!')
        };

        filteredNotifications.push(updatedNotification);
      }
    }
  }

  return filteredNotifications;
};

// 알림 조회 함수
const fetchNotifications = async () => {
  if (!computedUser.value.id) return;

  try {
    // 기본 알림 목록 조회
    const notificationsResponse = await notificationApiCall(`/recent?userId=${computedUser.value.id}&limit=20`);

    if (notificationsResponse && notificationsResponse.ok) {
      const allNotifications = await notificationsResponse.json();

      // 방송 시작 알림만 실제 상태 확인하여 필터링
      const filteredNotifications = await filterBroadcastNotifications(allNotifications);

      // UI 업데이트 (최대 10개만 표시)
      notifications.value = filteredNotifications.slice(0, 10);
      unreadNotificationCount.value = filteredNotifications.filter(n => !n.isRead).length;
    }

  } catch (error) {
    // 에러 시 기존 데이터 유지
  }
};

// 폴링 시작
const startNotificationPolling = () => {
  if (!computedUser.value.id) return;

  notificationPollingInterval = setInterval(() => {
    fetchNotifications();
  }, 30000);
};

const stopNotificationPolling = () => {
  if (notificationPollingInterval) {
    clearInterval(notificationPollingInterval);
    notificationPollingInterval = null;
  }
};

// 알림 드롭다운 표시
const showNotificationDropdown = async () => {
  isNotificationDropdownVisible.value = true;

  if (notifications.value.length === 0) {
    isLoadingNotifications.value = true;
    try {
      await fetchNotifications();
    } catch (error) {
      // 에러 처리
    } finally {
      isLoadingNotifications.value = false;
    }
  } else {
    // 이미 로드된 알림도 실시간으로 다시 확인
    await fetchNotifications();
  }
};

const hideNotificationDropdown = () => {
  setTimeout(() => {
    isNotificationDropdownVisible.value = false;
  }, 150);
};

// 알림 클릭 처리
const handleNotificationClick = async (notification) => {
  // 방송 시작 알림인 경우 클릭 시에도 다시 확인
  if (notification.type === 'BROADCAST_START' && notification.broadcastId) {
    const isLive = await checkBroadcastStatus(notification.broadcastId);
    if (!isLive) {
      alert('방송이 현재 진행되지 않고 있습니다. 잠시 후 다시 확인해주세요.');
      return;
    }
  }

  try {
    // 읽음 처리
    if (!notification.isRead) {
      const success = await notificationHelpers.markAsRead(notification.notificationId, computedUser.value.id);
      if (success) {
        notification.isRead = true;
        notification.readAt = new Date().toISOString();
        unreadNotificationCount.value = Math.max(0, unreadNotificationCount.value - 1);
      }
    }

    hideNotificationDropdown();

    // 페이지 이동
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
    hideNotificationDropdown();
  }
};

const markAllAsRead = async () => {
  try {
    const success = await notificationHelpers.markAllAsRead(computedUser.value.id);
    if (success) {
      // UI 즉시 업데이트
      notifications.value.forEach(notification => {
        notification.isRead = true;
        notification.readAt = new Date().toISOString();
      });
      unreadNotificationCount.value = 0;
    }
  } catch (error) {
    // 에러 처리
  }
};

// 기타 유틸리티 함수들
const formatTime = (timeString) => {
  return notificationHelpers.formatTime(timeString);
};

const performSearch = () => {
  const keyword = searchKeyword.value.trim();
  if (!keyword) return;

  router.push({
    path: '/category',
    query: { search: keyword }
  });
};

const fetchCartCount = async () => {
  if (!computedUser.value.id) return;

  try {
    const cartResponse = await apiClient.get('/api/cart/count');
    if (cartResponse.data.success) {
      cartCount.value = cartResponse.data.data.count || 0;
    }
  } catch (error) {
    // 에러 처리
  }
};

const isTokenValid = (token) => {
  if (!token) return false;

  try {
    const parts = token.split('.');
    if (parts.length !== 3) return false;

    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4) {
      base64 += '=';
    }

    const payloadStr = atob(base64);
    const payload = JSON.parse(payloadStr);
    const currentTime = Math.floor(Date.now() / 1000);

    return !(payload.exp && payload.exp < currentTime);
  } catch (error) {
    return false;
  }
};

const validateUserInfo = async () => {
  const socialName = localStorage.getItem('social_name') ||
      sessionStorage.getItem('social_name');

  if (socialName &&
      socialName.trim() &&
      socialName !== "사용자" &&
      socialName !== "소셜사용자" &&
      socialName.length >= 2) {
    user.name = socialName;
    sessionStorage.setItem('current_user_name', socialName);
    isUserInfoLoaded.value = true;
    return true;
  }

  try {
    const response = await apiClient.get('/api/users/profile', {
      timeout: 3000,
      validateStatus: function (status) {
        return status < 500;
      }
    });

    if (response.status === 200 && response.data && response.data.success && response.data.data) {
      const userData = response.data.data;

      user.id = userData.userId || userData.id;
      user.email = userData.email;
      user.role = userData.role || 'USER';
      user.phone = userData.phone;

      if (userData.name && userData.name.trim() && userData.name.length >= 1) {
        user.name = userData.name.trim();
        sessionStorage.setItem('current_user_name', user.name);
      } else {
        user.name = "사용자";
      }

      isUserInfoLoaded.value = true;
      return true;
    }
  } catch (error) {
    // 에러 처리
  }

  return false;
};

const resetUserData = () => {
  cartCount.value = 0;
  unreadNotificationCount.value = 0;
  notifications.value = [];
  stopNotificationPolling();
};

// 드롭다운 함수들
const showDropdown = () => {
  isDropdownVisible.value = true;
};

const hideDropdown = () => {
  setTimeout(() => {
    isDropdownVisible.value = false;
  }, 150);
};

const logout = () => {
  stopNotificationPolling();
  resetUser();
  resetUserData();
  searchKeyword.value = '';
  isDropdownVisible.value = false;
  isUserInfoLoaded.value = false;
  isSocialUser.value = false;
  socialProvider.value = null;
  router.push("/login");
};

// Watch 설정
watch(() => computedUser.value.id, async (newUserId, oldUserId) => {
  if (newUserId && newUserId !== oldUserId) {
    isUserInfoLoaded.value = false;
    checkSocialLoginStatus();

    try {
      await validateUserInfo();
      await Promise.all([
        fetchCartCount(),
        fetchNotifications()
      ]);
      stopNotificationPolling();
      startNotificationPolling();
    } catch (error) {
      // 에러 처리
    } finally {
      isUserInfoLoaded.value = true;
    }
  } else if (!newUserId && oldUserId) {
    resetUserData();
    isUserInfoLoaded.value = false;
    isSocialUser.value = false;
    socialProvider.value = null;
  }
}, { immediate: false });

// 컴포넌트 생명주기
onMounted(async () => {
  const token = localStorage.getItem("jwt");

  if (token && isTokenValid(token)) {
    try {
      setUserFromToken(token);
      const isValid = await validateUserInfo();

      if (isValid) {
        checkSocialLoginStatus();
        await Promise.all([
          fetchCartCount(),
          fetchNotifications()
        ]);
        startNotificationPolling();
      } else {
        localStorage.removeItem("jwt");
        resetUserData();
      }
    } catch (error) {
      localStorage.removeItem("jwt");
      resetUserData();
    }
  } else {
    if (token) {
      localStorage.removeItem("jwt");
    }
    resetUserData();
  }
});

onUnmounted(() => {
  stopNotificationPolling();
});
</script>
<style scoped src="@/assets/css/header.css">
</style>
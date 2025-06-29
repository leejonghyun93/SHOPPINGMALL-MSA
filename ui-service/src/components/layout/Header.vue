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
          {{ computedUser.name }} 님 ▼
        </span>

        <!-- 드롭다운 메뉴 -->
        <div class="dropdown-menu" :class="{ 'show': isDropdownVisible }">
          <router-link to="/mypage/orders" class="dropdown-item" @click="hideDropdown">
            마이페이지
          </router-link>
          <router-link to="/profile" class="dropdown-item" @click="hideDropdown">
            회원정보관리
          </router-link>
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

                <!-- 🔥 알림 타입 아이콘 추가 -->
                <div class="notification-type-icon">
                  {{ notification.type === 'BROADCAST_START' ? '🔴' :
                    notification.type === 'BROADCAST_END' ? '⏹️' :
                        notification.type === 'BROADCAST_REMINDER' ? '🔔' : '📢' }}
                </div>

                <div class="notification-content">
                  <div class="notification-title">{{ notification.title }}</div>
                  <div class="notification-message">{{ notification.message }}</div>
                  <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
                </div>

                <!-- 🔥 읽지 않은 알림 표시 개선 -->
                <div v-if="!notification.isRead" class="unread-indicator">
                  <div class="unread-dot"></div>
                </div>
              </div>
            </div>

            <!-- 알림 없음 -->
            <div v-else class="no-notifications">
              <div class="text-muted text-center py-3">
                <div class="mb-2">📭</div>
                <div class="mb-1">알림</div>
                <small>새로운 알림이 없습니다</small>
              </div>
            </div>
          </div>

          <!-- 🔥 푸터 개선 -->
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
import { onMounted, computed, ref, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { user, setUserFromToken } from "@/stores/userStore";
import apiClient from '@/api/axiosInstance'
import { notificationApiCall, notificationHelpers } from '@/config/notificationConfig'

const router = useRouter();
const isDropdownVisible = ref(false);
const isNotificationDropdownVisible = ref(false);
const isLoadingNotifications = ref(false);
const cartCount = ref(0);
const unreadNotificationCount = ref(0);
const notifications = ref([]);

// 🔥 검색 관련 변수
const searchKeyword = ref('');

// 폴링 인터벌 ID
let notificationPollingInterval = null;

const computedUser = computed(() => user);

// 🔥 검색 기능
const performSearch = () => {
  const keyword = searchKeyword.value.trim();

  if (!keyword) {
    return;
  }

  console.log('검색 실행:', keyword);

  // 카테고리 페이지로 이동하면서 검색어를 URL 파라미터로 전달
  router.push({
    path: '/category',
    query: { search: keyword }
  });

  // 검색 후 검색창 비우기 (선택사항)
  // searchKeyword.value = '';
};

// 토큰 유효성 검사 함수
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

// 사용자 정보 검증 함수
const validateUserInfo = async () => {
  const token = localStorage.getItem("token")
  if (!token || !isTokenValid(token)) {
    return false
  }

  try {
    const response = await apiClient.get('/api/users/profile')

    if (response.data.success && response.data.data) {
      const userData = response.data.data
      user.id = userData.id || userData.userId
      user.name = userData.name
      user.email = userData.email
      user.role = userData.role || 'USER'

      return true
    }
  } catch (error) {
    return false
  }

  return false
}

// 장바구니 수 가져오기
const fetchCartCount = async () => {
  if (!computedUser.value.id) return;

  try {
    const cartResponse = await apiClient.get('/api/cart/count');
    if (cartResponse.data.success) {
      cartCount.value = cartResponse.data.data.count || 0;
    }
  } catch (error) {
    // 에러 무시
  }
}

// 🔥 실시간 알림 데이터 가져오기 (새로운 API 사용)
const fetchNotifications = async () => {
  if (!computedUser.value.id) return;

  try {
    // 🔥 새로운 API 사용: 읽지 않은 알림 개수 조회
    const unreadResponse = await notificationApiCall(`/unread-count?userId=${computedUser.value.id}`);
    if (unreadResponse && unreadResponse.ok) {
      const unreadData = await unreadResponse.json();
      unreadNotificationCount.value = unreadData.count || 0;
    }

    // 🔥 새로운 API 사용: 최근 알림 목록 조회 (최대 10개)
    const notificationsResponse = await notificationApiCall(`/recent?userId=${computedUser.value.id}&limit=10`);
    if (notificationsResponse && notificationsResponse.ok) {
      const notificationsData = await notificationsResponse.json();
      notifications.value = notificationsData || [];
    }
  } catch (error) {
    console.error('알림 조회 실패:', error);
    // 에러 시 기존 값 유지
  }
}

// 알림 폴링 시작
const startNotificationPolling = () => {
  if (!computedUser.value.id) return;

  // 30초마다 알림 확인
  notificationPollingInterval = setInterval(() => {
    fetchNotifications();
  }, 10000);
}

// 알림 폴링 중지
const stopNotificationPolling = () => {
  if (notificationPollingInterval) {
    clearInterval(notificationPollingInterval);
    notificationPollingInterval = null;
  }
}

//  시간 포맷팅 (헬퍼 사용)
const formatTime = (timeString) => {
  return notificationHelpers.formatTime(timeString);
}

//  알림 클릭 처리 (개선된 버전)
const handleNotificationClick = async (notification) => {
  try {
    //  읽지 않은 알림이면 읽음 처리
    if (!notification.isRead) {
      const success = await notificationHelpers.markAsRead(notification.notificationId, computedUser.value.id);

      if (success) {
        // 로컬 상태 업데이트
        notification.isRead = true;
        notification.readAt = new Date().toISOString();
        unreadNotificationCount.value = Math.max(0, unreadNotificationCount.value - 1);
      }
    }

    hideNotificationDropdown();

    //  알림 타입에 따른 페이지 이동 (기존 로직 유지 + 개선)
    if (notification.type === 'BROADCAST_START') {
      // 방송 관련 알림이면 해당 방송으로 이동 (broadcastId가 있으면)
      if (notification.broadcastId) {
        router.push(`/live/${notification.broadcastId}`);
      } else {
        router.push('/broadcasts/category');
      }
    } else {
      // 기본적으로 알림 페이지로 이동
      router.push('/notifications');
    }
  } catch (error) {
    console.error('알림 클릭 처리 오류:', error);
    hideNotificationDropdown();
  }
}

//  모든 알림 읽음 처리 (새로운 API 사용)
const markAllAsRead = async () => {
  try {
    // 🔥 새로운 API 사용: 모든 알림 읽음 처리
    const success = await notificationHelpers.markAllAsRead(computedUser.value.id);

    if (success) {
      // 로컬 상태 업데이트
      notifications.value.forEach(notification => {
        notification.isRead = true;
        notification.readAt = new Date().toISOString();
      });

      unreadNotificationCount.value = 0;
      console.log('모든 알림 읽음 처리 완료');
    } else {
      console.warn('모든 알림 읽음 처리 실패');
    }
  } catch (error) {
    console.error('모든 알림 읽음 처리 오류:', error);
  }
}

// 드롭다운 관련 함수들
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

// 수동 새로고침 함수 추가
const refreshNotifications = async () => {
  console.log('알림 수동 새로고침...');
  isLoadingNotifications.value = true;

  try {
    await fetchNotifications();
    console.log('알림 새로고침 완료');
  } catch (error) {
    console.error('알림 새로고침 실패:', error);
  } finally {
    isLoadingNotifications.value = false;
  }
}

const handleNotificationIconClick = () => {
  // 알림 아이콘 클릭 시 즉시 새로고침
  if (!isNotificationDropdownVisible.value) {
    refreshNotifications();
  }
  showNotificationDropdown();
}

function logout() {
  stopNotificationPolling();
  localStorage.removeItem("token");
  user.id = null;
  user.name = null;
  user.role = null;
  isDropdownVisible.value = false;
  cartCount.value = 0;
  unreadNotificationCount.value = 0;
  notifications.value = [];
  router.push("/login");
}

onMounted(async () => {
  const token = localStorage.getItem("token");

  if (token && isTokenValid(token)) {
    try {
      setUserFromToken(token);

      validateUserInfo().catch(() => {
        // 검증 실패해도 기본 정보는 유지
      })

      // 데이터 가져오기
      await Promise.all([
        fetchCartCount(),
        fetchNotifications()
      ]);

      // 실시간 알림 폴링 시작
      startNotificationPolling();

    } catch (error) {
      localStorage.removeItem("token");
      user.id = null;
      user.name = null;
      user.role = null;
    }
  } else {
    if (token) {
      localStorage.removeItem("token");
    }
    user.id = null;
    user.name = null;
    user.role = null;
  }
});

onUnmounted(() => {
  stopNotificationPolling();
});
</script>

<style scoped src="@/assets/css/header.css"></style>

<!-- 🔥 검색 버튼 스타일 -->
<style scoped>
.search-btn {
  background-color: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.search-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
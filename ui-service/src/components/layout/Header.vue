<template>
  <nav class="navbar navbar-dark bg-white custom-navbar d-flex justify-content-between align-items-center">

    <!-- 왼쪽: 홈 -->
    <div class="d-flex align-items-center gap-2">
      <router-link to="/" class="navbar-brand">트라이마켓</router-link>
      <router-link to="/" class="navbar-brand">홈</router-link>
      <router-link to="/broadcasts/category" class="navbar-brand">라이브 목록</router-link>
      <router-link to="/broadcasts/schedule" class="navbar-brand">예고</router-link>
      <router-link to="/category" class="navbar-brand">카테고리</router-link>
    </div>

    <!-- 오른쪽: 메뉴들 -->
    <div class="d-flex align-items-center">
      <!-- 검색창 -->
      <div class="input-group search-box me-2">
        <input type="text" class="form-control form-control-sm" placeholder="상품명 또는 브랜드 입력" />
        <span class="input-group-text">
          🔍
        </span>
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
            <i class="fas fa-user"></i> 마이페이지
          </router-link>
          <router-link to="/profile" class="dropdown-item" @click="hideDropdown">
            <i class="fas fa-cog"></i> 회원정보관리
          </router-link>
          <div class="dropdown-divider"></div>
          <button @click="logout" class="dropdown-item logout-btn">
            <i class="fas fa-sign-out-alt"></i> 로그아웃
          </button>
        </div>
      </div>
      <!-- 🛒 장바구니 아이콘 -->
      <router-link to="/cart" class="navbar-brand mx-2" title="장바구니">🛒</router-link>
    </div>
  </nav>
</template>


<script setup>
import { onMounted, computed, ref } from "vue";
import { useRouter } from "vue-router";
import { user, setUserFromToken } from "@/stores/userStore";
import apiClient from '@/api/axiosInstance' // 🔥 공통 apiClient 추가

const router = useRouter();
const isDropdownVisible = ref(false);

const computedUser = computed(() => user);

// 🔥 토큰 유효성 검사 함수
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

    // 만료 시간 체크
    if (payload.exp && payload.exp < currentTime) {
      return false
    }

    return true
  } catch (error) {
    console.error('토큰 검증 에러:', error)
    return false
  }
}

// 🔥 사용자 정보 검증 함수 (선택적)
const validateUserInfo = async () => {
  const token = localStorage.getItem("token")
  if (!token || !isTokenValid(token)) {
    return false
  }

  try {
    // 🔥 공통 apiClient로 사용자 정보 검증
    const response = await apiClient.get('/api/users/profile')

    if (response.data.success && response.data.data) {
      // 서버에서 받은 최신 정보로 업데이트
      const userData = response.data.data
      user.id = userData.id || userData.userId
      user.name = userData.name
      user.email = userData.email
      user.role = userData.role || 'USER'

      console.log('✅ 헤더에서 사용자 정보 검증 완료:', user.name)
      return true
    }
  } catch (error) {
    // 401은 인터셉터에서 자동으로 처리
    console.log('사용자 정보 검증 실패:', error.message)
    return false
  }

  return false
}

onMounted(async () => {
  const token = localStorage.getItem("token");

  if (token && isTokenValid(token)) {
    try {
      // 토큰으로 기본 사용자 정보 설정
      setUserFromToken(token);
      console.log('✅ 헤더에서 기본 사용자 정보 설정 완료:', user.name);

      // 🔥 선택적으로 서버에서 최신 정보 검증 (백그라운드)
      validateUserInfo().catch(() => {
        // 검증 실패해도 기본 정보는 유지
        console.log('사용자 정보 백그라운드 검증 실패 - 기본 정보 유지')
      })

    } catch (error) {
      console.error('❌ 헤더에서 사용자 정보 설정 실패:', error);
      localStorage.removeItem("token");
      user.id = null;
      user.name = null;
      user.role = null;
    }
  } else {
    // 토큰이 없거나 무효한 경우
    if (token) {
      console.log('🔓 헤더에서 무효한 토큰 제거');
      localStorage.removeItem("token");
    }
    user.id = null;
    user.name = null;
    user.role = null;
  }
});

function showDropdown() {
  isDropdownVisible.value = true;
}

function hideDropdown() {
  setTimeout(() => {
    isDropdownVisible.value = false;
  }, 150);
}

function logout() {
  console.log('🔓 사용자 로그아웃');
  localStorage.removeItem("token");
  user.id = null;
  user.name = null;
  user.role = null;
  isDropdownVisible.value = false;
  router.push("/login");
}
</script>

<style scoped src="@/assets/css/header.css"></style>
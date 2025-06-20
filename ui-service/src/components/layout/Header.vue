<template>
  <nav class="navbar navbar-dark bg-white custom-navbar d-flex justify-content-between align-items-center">

    <!-- 왼쪽: 홈 -->
    <div class="d-flex align-items-center gap-2">
      <router-link to="/" class="navbar-brand">트라이마켓</router-link>
      <router-link to="/" class="navbar-brand">홈</router-link>
      <router-link to="/" class="navbar-brand">예고</router-link>
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

const router = useRouter();
const isDropdownVisible = ref(false);

const computedUser = computed(() => user);

onMounted(() => {
  const token = localStorage.getItem("token");
  if (token) {
    setUserFromToken(token);
  }
});

function showDropdown() {
  isDropdownVisible.value = true;
}

function hideDropdown() {
  // 약간의 지연을 주어 메뉴 클릭이 가능하도록 함
  setTimeout(() => {
    isDropdownVisible.value = false;
  }, 150);
}

function logout() {
  localStorage.removeItem("token");
  user.id = null;
  user.name = null;
  user.role = null;
  isDropdownVisible.value = false;
  router.push("/login");
}
</script>

<style scoped src="@/assets/css/header.css"></style>
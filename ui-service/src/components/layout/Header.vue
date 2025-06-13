<template>
  <nav class="navbar navbar-dark bg-white custom-navbar d-flex justify-content-between align-items-center">

    <!-- 왼쪽: 홈 -->
    <div class="d-flex align-items-center gap-2">
      <router-link to="/" class="navbar-brand">트라이마켓</router-link>
      <router-link to="/" class="navbar-brand">홈</router-link>
      <router-link to="/" class="navbar-brand">이벤트</router-link>
      <router-link to="/" class="navbar-brand">예고</router-link>
      <router-link to="/categorie" class="navbar-brand">카테고리</router-link>
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

      <router-link v-if="!computedUser.id" to="/login" class="navbar-brand mx-2">로그인</router-link>
      <router-link v-if="!computedUser.id" to="/register" class="navbar-brand mx-2">회원가입</router-link>

      <span v-if="computedUser.id" class="navbar-brand mx-2">{{ computedUser.name }}</span>
      <button v-if="computedUser.id" @click="logout" class="navbar-brand mx-2 btn p-0">로그아웃</button>
    </div>
  </nav>
</template>


<script setup>
import { onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { user, setUserFromToken } from "@/stores/userStore";

const router = useRouter();

const computedUser = computed(() => user);

onMounted(() => {
  const token = localStorage.getItem("token");
  if (token) {
    setUserFromToken(token);
  }
});

function logout() {
  localStorage.removeItem("token");
  user.id = null;
  user.name = null;
  user.role = null;
  router.push("/login");
}
</script>

<style scoped src="@/assets/css/header.css"></style>
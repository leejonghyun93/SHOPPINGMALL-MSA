<template>
  <nav class="navbar navbar-dark bg-dark custom-navbar d-flex justify-content-between align-items-center px-3">

    <!-- 왼쪽: 홈 -->
    <router-link to="/" class="navbar-brand">홈</router-link>

    <!-- 오른쪽: 메뉴들 (가로 정렬) -->
    <div class="d-flex align-items-center">
      <router-link v-if="!computedUser.id" to="/login" class="nav-link text-white mx-2">로그인</router-link>
      <router-link v-if="!computedUser.id" to="/register" class="nav-link text-white mx-2">회원가입</router-link>
      <router-link v-if="computedUser.id && computedUser.role === 'ADMIN'" to="/members" class="nav-link text-white mx-2">회원관리</router-link>
      <router-link to="/boardList" class="nav-link text-white mx-2">게시판</router-link>
      <span v-if="computedUser.id" class="nav-link text-white mx-2">{{ computedUser.name }}</span>
      <button v-if="computedUser.id" @click="logout" class="btn btn-link nav-link text-white mx-2 p-0">로그아웃</button>
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
<template>
  <div class="container mt-5" style="max-width: 400px;">
    <h3 class="mb-4">아이디 찾기</h3>
    <form @submit.prevent="handleFindId">
      <div class="mb-3">
        <label for="name" class="form-label">이름</label>
        <input v-model="name" type="text" class="form-control" id="name" required />
      </div>
      <div class="mb-3">
        <label for="email" class="form-label">가입 시 사용한 이메일</label>
        <input v-model="email" type="email" class="form-control" id="email" required />
      </div>
      <button type="submit" class="btn btn-primary w-100">아이디 찾기</button>
    </form>
    <div v-if="result" class="alert alert-info mt-3">{{ result }}</div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import axios from "axios";

const email = ref("");
const name = ref("");
const result = ref("");

const handleFindId = async () => {
  try {
    const res = await axios.post("/auth/find-id", {
      name: name.value,
      email: email.value
    });
    result.value = `회원님의 아이디는: ${res.data.userid} 입니다.`;
  } catch (error) {
    result.value = error.response?.data?.message || "아이디 찾기 실패";
  }
};
</script>

<style scoped>
.container {
  max-width: 960px;
  min-height: 100vh;
}
</style>
<template>
  <div class="container mt-5" style="max-width: 400px;">
    <h3 class="mb-4">비밀번호 찾기</h3>
    <form @submit.prevent="handleFindPassword">
      <div class="mb-3">
        <label for="userid" class="form-label">아이디</label>
        <input v-model="userid" type="text" class="form-control" id="userid" required />
      </div>
      <div class="mb-3">
        <label for="email" class="form-label">가입 시 사용한 이메일</label>
        <input v-model="email" type="email" class="form-control" id="email" required />
      </div>
      <button type="submit" class="btn btn-primary w-100">비밀번호 초기화 요청</button>
    </form>
    <div v-if="result" class="alert alert-info mt-3">{{ result }}</div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import axios from "axios";
import '@/assets/css/findIdPassword.css';

const userid = ref("");
const email = ref("");
const result = ref("");

const handleFindPassword = async () => {
  try {
    const res = await axios.post("/auth/find-password", {
      userid: userid.value,
      email: email.value,
    });
    result.value = res.data.message;
  } catch (error) {
    result.value = error.response?.data?.message || "비밀번호 찾기 실패";
  }
};
</script>

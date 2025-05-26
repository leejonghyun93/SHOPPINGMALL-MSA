<template>
  <div class="d-flex justify-content-center align-items-center vh-100 bg-light">
    <div class="card p-4 shadow-sm" style="width: 100%; max-width: 400px;">
      <h4 class="text-center mb-4">로그인</h4>

      <form @submit.prevent="handleLogin">
        <!-- 아이디 -->
        <div class="mb-3">
          <label for="userid" class="form-label">아이디</label>
          <input
              v-model="form.userid"
              type="text"
              class="form-control"
              id="userid"
              required
          />
        </div>

        <!-- 비밀번호 -->
        <div class="mb-3">
          <label for="password" class="form-label">비밀번호</label>
          <input
              v-model="form.password"
              type="password"
              class="form-control"
              id="password"
              required
          />
        </div>

        <!-- 체크박스 및 찾기 링크들 -->
        <div class="d-flex justify-content-between align-items-center mb-3">
          <div class="form-check">
            <input class="form-check-input" type="checkbox" id="rememberId" />
            <label class="form-check-label" for="rememberId">아이디 저장</label>
          </div>
          <div>
            <a href="#" class="small me-2">아이디 찾기</a>
            <a href="#" class="small">비밀번호 찾기</a>
          </div>
        </div>

        <!-- 로그인 버튼 -->
        <button type="submit" class="btn btn-primary w-100">로그인</button>

        <!-- 회원가입 링크 -->
        <div class="text-center mt-3">
          <router-link to="/register" class="text-decoration-none btn btn-dark w-100">
            회원가입
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import axios from "axios";
import { useRouter } from "vue-router";
import { setUserFromToken } from "@/stores/userStore"; // ✅ 추가

const router = useRouter();
const form = reactive({
  userid: "",
  password: "",
});

const errorMessage = ref("");

const handleLogin = async () => {
  try {
    const response = await axios.post("/auth/login", {
      userid: form.userid,
      passwd: form.password,
    });

    const token = response.data.token;

    if (token) {
      localStorage.setItem("token", token);
      setUserFromToken(token); // ✅ 상태 업데이트
      alert("로그인 성공!");
      router.push("/");
    } else {
      errorMessage.value = "토큰이 응답에 없습니다.";
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = "로그인 실패: 아이디 또는 비밀번호가 올바르지 않습니다.";
  }
};
</script>

<style scoped>
.error {
  color: red;
  margin-top: 10px;
}
</style>

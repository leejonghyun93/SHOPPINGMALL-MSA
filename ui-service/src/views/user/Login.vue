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

        <!-- 에러 메시지 -->
        <div v-if="errorMessage.length > 0" class="error mt-2">
          {{ errorMessage }}
        </div>

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
import { setUserFromToken } from "@/stores/userStore";

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
      setUserFromToken(token);
      alert("로그인 성공!");
      router.push("/");
    } else {
      errorMessage.value = "토큰이 응답에 없습니다.";
    }
  } catch (error) {
    if (error.response) {
      console.error("서버 응답 데이터:", error.response.data);
      console.error("서버 상태 코드:", error.response.status);
      errorMessage.value = error.response.data.message || "로그인 실패";
    } else {
      console.error("요청 실패:", error.message);
      errorMessage.value = "서버에 연결할 수 없습니다.";
    }
  }
};
</script>

<style scoped>
.error {
  color: red;
  font-size: 0.9rem;
  margin-top: 10px;
  text-align: center;
}
</style>

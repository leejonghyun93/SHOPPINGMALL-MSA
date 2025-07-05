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
            <input
                class="form-check-input"
                type="checkbox"
                id="rememberId"
                v-model="rememberId"
            />
            <label class="form-check-label" for="rememberId">아이디 저장</label>
          </div>
          <div>
            <router-link to="/findId" class="small me-2">아이디 찾기</router-link>
            <router-link to="/findPassword" class="small">비밀번호 찾기</router-link>
          </div>
        </div>

        <!-- 로그인 버튼 -->
        <button type="submit" class="btn btn-primary w-100" :disabled="isLoading">
          {{ isLoading ? '로그인 중...' : '로그인' }}
        </button>

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
import { reactive, ref, onMounted } from "vue";
import axios from "axios";
import { useRouter } from "vue-router";
import { setUserFromToken, user } from "@/stores/userStore";  // user도 import
import apiClient from '@/api/axiosInstance';  // API 클라이언트 추가
import '@/assets/css/memberList.css';

const router = useRouter();
const form = reactive({
  userid: "",
  password: "",
});
const rememberId = ref(false);
const errorMessage = ref("");
const isLoading = ref(false);

// 사용자 프로필 정보 가져오기 함수
const fetchUserProfile = async (token) => {
  try {
    console.log('로그인 후 프로필 정보 조회 시작');

    const response = await apiClient.get('/api/users/profile');

    if (response.data && response.data.success && response.data.data) {
      const userData = response.data.data;

      // 🔥 실제 사용자 정보로 업데이트
      user.id = userData.userId;
      user.name = userData.name;  // 실제 이름으로 설정
      user.email = userData.email;
      user.role = userData.role || 'USER';

      console.log('로그인 후 프로필 정보 설정 완료:', {
        id: user.id,
        name: user.name,
        email: user.email
      });

      return true;
    }
  } catch (error) {
    console.error('로그인 후 프로필 조회 실패:', error);
    // 프로필 조회 실패해도 로그인은 유지
  }
  return false;
};

// 페이지 로드 시 저장된 아이디 불러오기
onMounted(() => {
  const savedUserId = localStorage.getItem("savedUserId");
  if (savedUserId) {
    form.userid = savedUserId;
    rememberId.value = true;
  }
});

const handleLogin = async () => {
  // 입력값 검증
  if (!form.userid.trim()) {
    errorMessage.value = "아이디를 입력해주세요.";
    return;
  }

  if (!form.password.trim()) {
    errorMessage.value = "비밀번호를 입력해주세요.";
    return;
  }

  isLoading.value = true;
  errorMessage.value = "";

  try {
    // 백엔드 DTO 필드명에 맞게 수정
    const response = await axios.post("/auth/login", {
      userid: form.userid,      // username → userid
      passwd: form.password     // password → passwd
    });

    // AuthResponse 구조에 맞게 처리
    if (response.data.success && response.data.token) {
      localStorage.setItem("token", response.data.token);

      // 🔥 1. 토큰에서 기본 정보 설정
      setUserFromToken(response.data.token);

      // 🔥 2. API로 실제 사용자 정보 가져오기
      await fetchUserProfile(response.data.token);

      // 아이디 저장 처리
      if (rememberId.value) {
        localStorage.setItem("savedUserId", form.userid);
      } else {
        localStorage.removeItem("savedUserId");
      }

      console.log('로그인 완료 후 최종 사용자 정보:', {
        id: user.id,
        name: user.name,
        email: user.email
      });

      await router.push("/");
    } else {
      errorMessage.value = response.data.message || "로그인 실패";
    }
  } catch (error) {
    if (error.response) {
      const status = error.response.status;
      const data = error.response.data;
      if (data && data.message) {
        errorMessage.value = `${data.message}`;
      } else {
        switch (status) {
          case 400:
            errorMessage.value = "아이디 또는 비밀번호가 잘못되었습니다.";
            break;
          case 401:
            errorMessage.value = "인증에 실패했습니다.";
            break;
          case 403:
            errorMessage.value = "접근이 거부되었습니다.";
            break;
          case 404:
            errorMessage.value = "존재하지 않는 아이디입니다.";
            break;
          case 500:
            errorMessage.value = "서버 내부 오류입니다.";
            break;
          default:
            errorMessage.value = `로그인 실패 (오류 코드: ${status})`;
        }
      }
    } else if (error.request) {
      errorMessage.value = "서버에 연결할 수 없습니다.";
    } else {
      errorMessage.value = "예상치 못한 오류가 발생했습니다.";
    }
  } finally {
    isLoading.value = false;
  }
};
</script>
<style scoped src="@/assets/css/login.css"></style>



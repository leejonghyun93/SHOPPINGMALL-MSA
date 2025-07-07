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
        <div v-if="errorMessage.length > 0" class="alert alert-danger mt-2">
          {{ errorMessage }}
        </div>

        <!-- 구분선 -->
        <div class="divider my-4">
          <span>또는</span>
        </div>

        <!-- 소셜 로그인 버튼들 -->
        <div class="social-login-section">
          <!-- 카카오 로그인 -->
          <button
              type="button"
              class="btn w-100 mb-2"
              style="background-color: #FEE500; color: #000; border: none;"
              @click="handleKakaoLogin"
              :disabled="isLoading"
          >
            카카오로 로그인
          </button>

          <!-- 네이버 로그인 -->
          <button
              type="button"
              class="btn w-100 mb-3"
              style="background-color: #03C75A; color: white; border: none;"
              @click="handleNaverLogin"
              :disabled="isLoading"
          >
            네이버로 로그인
          </button>
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
import { useRouter, useRoute } from "vue-router";
import { setUserFromToken, user, updateUserFromApi } from "@/stores/userStore";
import apiClient from '@/api/axiosInstance';
import '@/assets/css/memberList.css';

const router = useRouter();
const route = useRoute();
const form = reactive({
  userid: "",
  password: "",
});
const rememberId = ref(false);
const errorMessage = ref("");
const isLoading = ref(false);

// 환경변수에서 소셜 로그인 설정 가져오기
const KAKAO_CLIENT_ID = import.meta.env.VITE_KAKAO_CLIENT_ID;
const NAVER_CLIENT_ID = import.meta.env.VITE_NAVER_CLIENT_ID;
const REDIRECT_URI = import.meta.env.VITE_REDIRECT_URI || `${window.location.origin}/auth/callback`;

// 소셜 로그인 토큰 처리 함수
const handleSocialLoginToken = async (token) => {
  try {
    console.log('📱 Login.vue에서 소셜 로그인 처리 시작');

    localStorage.setItem('token', token)

    // 🔥 개선된 토큰에서 소셜 정보 추출
    let socialProvider = 'KAKAO';
    let socialName = '소셜사용자';
    let socialEmail = null;

    try {
      const parts = token.split('.');
      if (parts.length === 3) {
        let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        while (base64.length % 4) {
          base64 += '=';
        }

        // 🔥 개선된 UTF-8 디코딩
        const binaryString = atob(base64);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
          bytes[i] = binaryString.charCodeAt(i);
        }

        const decoder = new TextDecoder('utf-8');
        const jsonStr = decoder.decode(bytes);
        console.log('📱 디코딩된 JSON 문자열:', jsonStr);

        const payload = JSON.parse(jsonStr);
        console.log('📱 토큰 페이로드:', payload);

        socialProvider = payload.socialProvider || payload.provider || 'KAKAO';

        // 🔥 한글 이름 처리
        if (payload.name && payload.name.trim()) {
          let rawName = payload.name;

          // URI 디코딩 시도
          if (rawName.includes('%')) {
            try {
              rawName = decodeURIComponent(rawName);
            } catch (e) {
              console.log('URI 디코딩 실패');
            }
          }

          // 깨진 한글 복구 시도
          if (isGarbledKorean(rawName)) {
            rawName = repairGarbledKorean(rawName);
          }

          // 최종 검증
          if (rawName && !isGarbledKorean(rawName) && rawName.length >= 2) {
            socialName = rawName;
          }
        } else if (payload.username && payload.username.trim()) {
          socialName = payload.username;
        }

        socialEmail = payload.email;
        console.log('📱 최종 추출된 이름:', socialName);
      }
    } catch (e) {
      console.error('📱 토큰 파싱 오류:', e);
    }

    // 소셜 로그인 설정
    localStorage.setItem('login_type', 'SOCIAL');
    localStorage.setItem('social_provider', socialProvider);
    localStorage.setItem('social_name', socialName);
    sessionStorage.setItem('social_name', socialName);

    if (socialEmail) {
      localStorage.setItem('social_email', socialEmail);
    }

    const tokenSuccess = setUserFromToken(token);

    if (tokenSuccess) {
      await router.push('/');
    } else {
      throw new Error('토큰 처리 실패');
    }

  } catch (error) {
    console.error('📱 에러 상세:', error);
    alert(`로그인 처리 중 오류: ${error.message}`);
  }
}

// 사용자 프로필 정보 가져오기 함수
const fetchUserProfile = async (token) => {
  try {
    const response = await apiClient.get('/api/users/profile');

    if (response.data && response.data.success && response.data.data) {
      const userData = response.data.data;
      const updated = updateUserFromApi(userData);

      if (updated) {
        return true;
      }
    }
  } catch (error) {
    // 에러 처리
  }
  return false;
};

// 소셜 로그인 콜백 처리
const checkSocialLoginCallback = async () => {
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');
  const error = urlParams.get('error');

  if (error) {
    errorMessage.value = decodeURIComponent(error);
    window.history.replaceState({}, document.title, window.location.pathname);
    return;
  }

  if (token) {
    await handleSocialLoginToken(token);
    return;
  }
};

// 페이지 로드 시 저장된 아이디 불러오기
onMounted(() => {
  const savedUserId = localStorage.getItem("savedUserId");
  if (savedUserId) {
    form.userid = savedUserId;
    rememberId.value = true;
  }

  checkSocialLoginCallback();
});

// 일반 로그인 처리
const handleLogin = async () => {
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
    const response = await axios.post("/auth/login", {
      userid: form.userid,
      passwd: form.password
    });

    if (response.data.success && response.data.token) {
      // 🔥 일반 로그인 처리
      localStorage.setItem("token", response.data.token);

      // 일반 로그인으로 설정
      localStorage.setItem('login_type', 'NORMAL');
      sessionStorage.setItem('login_type', 'NORMAL');

      // 소셜 로그인 정보 제거
      localStorage.removeItem('social_provider');
      localStorage.removeItem('social_name');
      localStorage.removeItem('social_email');
      sessionStorage.removeItem('social_provider');
      sessionStorage.removeItem('social_name');
      sessionStorage.removeItem('social_email');

      setUserFromToken(response.data.token);
      await fetchUserProfile(response.data.token);

      if (rememberId.value) {
        localStorage.setItem("savedUserId", form.userid);
      } else {
        localStorage.removeItem("savedUserId");
      }

      await router.push("/");
    }
  } catch (error) {
    console.error('📝 일반 로그인 오류:', error);

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

// 카카오 로그인
const handleKakaoLogin = () => {
  if (!KAKAO_CLIENT_ID) {
    errorMessage.value = "카카오 로그인 설정이 완료되지 않았습니다.";
    return;
  }

  try {
    const state = generateRandomState();
    localStorage.setItem('oauth_state', state);

    const kakaoAuthUrl = `https://kauth.kakao.com/oauth/authorize?` +
        `client_id=${KAKAO_CLIENT_ID}&` +
        `redirect_uri=${encodeURIComponent(REDIRECT_URI)}&` +
        `response_type=code&` +
        `state=${state}`;

    window.location.href = kakaoAuthUrl;
  } catch (error) {
    errorMessage.value = "카카오 로그인 처리 중 오류가 발생했습니다.";
  }
};

// 네이버 로그인
const handleNaverLogin = () => {
  if (!NAVER_CLIENT_ID) {
    errorMessage.value = "네이버 로그인 설정이 완료되지 않았습니다.";
    return;
  }

  try {
    const state = generateRandomState();
    localStorage.setItem('oauth_state', state);

    const naverAuthUrl = `https://nid.naver.com/oauth2.0/authorize?` +
        `client_id=${NAVER_CLIENT_ID}&` +
        `redirect_uri=${encodeURIComponent(REDIRECT_URI)}&` +
        `response_type=code&` +
        `state=${state}&` +
        `scope=profile`;

    window.location.href = naverAuthUrl;
  } catch (error) {
    errorMessage.value = "네이버 로그인 처리 중 오류가 발생했습니다.";
  }
};

// 랜덤 state 생성 (CSRF 보호)
const generateRandomState = () => {
  const state = Math.random().toString(36).substring(2, 15) +
      Math.random().toString(36).substring(2, 15);
  return state;
};
</script>

<style scoped src="@/assets/css/login.css"></style>
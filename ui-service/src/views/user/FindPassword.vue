<template>
  <div class="find-password-container">
    <div class="container mt-5" style="max-width: 500px;">
      <!-- 헤더 섹션 -->
      <div class="header-section text-center mb-4">
        <div class="icon-wrapper mb-3">
          <div class="key-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 10V8C6 5.79086 7.79086 4 10 4H14C16.2091 4 18 5.79086 18 8V10M3 10H21L20 20H4L3 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
        <h3 class="title">비밀번호 재설정</h3>
        <p class="subtitle">본인 확인 후 새로운 비밀번호로 변경할 수 있습니다.</p>
      </div>

      <!-- 1단계: 본인 확인 -->
      <div class="form-card" v-if="step === 1">
        <form @submit.prevent="handleVerifyUser">
          <h5 class="step-title mb-4">1단계: 본인 확인</h5>

          <div class="mb-3">
            <label for="userid" class="form-label">아이디 *</label>
            <input
                v-model="form.userid"
                type="text"
                class="form-control custom-input"
                id="userid"
                placeholder="아이디를 입력하세요"
                required
                :disabled="loading"
            />
          </div>

          <div class="mb-3">
            <label for="email" class="form-label">이메일 *</label>
            <input
                v-model="form.email"
                type="email"
                class="form-control custom-input"
                id="email"
                placeholder="example@email.com"
                required
                :disabled="loading"
            />
          </div>

          <div class="mb-3">
            <label for="name" class="form-label">이름 *</label>
            <input
                v-model="form.name"
                type="text"
                class="form-control custom-input"
                id="name"
                placeholder="가입시 등록한 이름"
                required
                :disabled="loading"
            />
          </div>

          <button
              type="submit"
              class="btn btn-primary custom-btn w-100"
              :disabled="loading"
          >
            <span v-if="loading" class="loading-spinner"></span>
            {{ loading ? '확인 중...' : '본인 확인' }}
          </button>
        </form>
      </div>

      <!-- 2단계: 새 비밀번호 설정 -->
      <div class="form-card" v-if="step === 2">
        <form @submit.prevent="handleResetPassword">
          <h5 class="step-title mb-4">2단계: 새 비밀번호 설정</h5>

          <div class="alert alert-success mb-4">
            <strong>{{ form.userid }}</strong>님의 본인 확인이 완료되었습니다.
          </div>

          <div class="mb-3">
            <label for="newPassword" class="form-label">새 비밀번호 *</label>
            <input
                v-model="form.newPassword"
                type="password"
                class="form-control custom-input"
                id="newPassword"
                placeholder="새 비밀번호를 입력하세요"
                required
                :disabled="loading"
                minlength="8"
            />
            <small class="form-text text-muted">8자 이상, 영문/숫자/특수문자 조합 권장</small>
          </div>

          <div class="mb-3">
            <label for="confirmPassword" class="form-label">비밀번호 확인 *</label>
            <input
                v-model="form.confirmPassword"
                type="password"
                class="form-control custom-input"
                id="confirmPassword"
                placeholder="새 비밀번호를 다시 입력하세요"
                required
                :disabled="loading"
            />
            <small v-if="passwordMismatch" class="text-danger">비밀번호가 일치하지 않습니다.</small>
          </div>

          <button
              type="submit"
              class="btn btn-success custom-btn w-100"
              :disabled="loading || passwordMismatch || !form.newPassword || !form.confirmPassword"
          >
            <span v-if="loading" class="loading-spinner"></span>
            {{ loading ? '변경 중...' : '비밀번호 변경' }}
          </button>

          <button
              type="button"
              class="btn btn-outline-secondary w-100 mt-2"
              @click="goBackToStep1"
              :disabled="loading"
          >
            이전 단계로
          </button>
        </form>
      </div>

      <!-- 3단계: 완료 -->
      <div v-if="step === 3" class="success-section">
        <div class="alert alert-success custom-alert">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="me-2">
            <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          비밀번호가 성공적으로 변경되었습니다!
        </div>

        <div class="completion-message">
          <h5>비밀번호 변경 완료</h5>
          <p>새로운 비밀번호로 로그인해주세요.</p>
        </div>

        <div class="text-center mt-4">
          <div class="d-grid gap-2">
            <router-link to="/login" class="btn btn-primary">
              로그인 페이지로 이동
            </router-link>
          </div>
        </div>
      </div>

      <!-- 에러 메시지 -->
      <div v-if="error" class="alert alert-danger custom-alert mt-3">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="me-2">
          <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
          <line x1="15" y1="9" x2="9" y2="15" stroke="currentColor" stroke-width="2"/>
          <line x1="9" y1="9" x2="15" y2="15" stroke="currentColor" stroke-width="2"/>
        </svg>
        {{ error }}
      </div>

      <!-- 하단 네비게이션 (완료 전에만 표시) -->
      <div v-if="step !== 3" class="bottom-navigation mt-4">
        <div class="nav-card">
          <div class="row text-center">
            <div class="col-4">
              <router-link to="/findId" class="nav-link-custom">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="nav-icon">
                  <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2"/>
                  <path d="M21 21L16.65 16.65" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>아이디 찾기</span>
              </router-link>
            </div>
            <div class="col-4">
              <router-link to="/login" class="nav-link-custom">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="nav-icon">
                  <path d="M15 3H19C20.1046 3 21 3.89543 21 5V19C21 20.1046 20.1046 21 19 21H15M10 17L15 12L10 7M15 12H3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>로그인</span>
              </router-link>
            </div>
            <div class="col-4">
              <router-link to="/register" class="nav-link-custom">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="nav-icon">
                  <path d="M16 21V19C16 16.7909 14.2091 15 12 15H5C2.79086 15 1 16.7909 1 19V21M20.5 11.5L22 13L20.5 14.5M22 13H18M12.5 7C12.5 9.20914 10.7091 11 8.5 11C6.29086 11 4.5 9.20914 4.5 7C4.5 4.79086 6.29086 3 8.5 3C10.7091 3 12.5 4.79086 12.5 7Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>회원가입</span>
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from 'vue-router';
import apiClient from '@/api/axiosInstance';
import '@/assets/css/findIdPassword.css';

const route = useRoute();
const router = useRouter();

const step = ref(1);
const loading = ref(false);
const error = ref("");

const form = ref({
  userid: "",
  email: "",
  name: "",
  newPassword: "",
  confirmPassword: ""
});

const passwordMismatch = computed(() => {
  return form.value.confirmPassword && form.value.newPassword !== form.value.confirmPassword;
});

onMounted(() => {
  if (route.query.userId) {
    form.value.userid = route.query.userId;
  }
  if (route.query.email) {
    form.value.email = route.query.email;
  }
});

const handleVerifyUser = async () => {
  if (!form.value.userid.trim() || !form.value.email.trim() || !form.value.name.trim()) {
    error.value = "모든 정보를 입력해주세요.";
    return;
  }

  loading.value = true;
  error.value = "";

  try {
    // 사용자 정보 확인
    const verifyResponse = await apiClient.get("/api/users/findId", {
      params: {
        name: form.value.name.trim(),
        email: form.value.email.trim()
      },
      withAuth: false
    });

    if (verifyResponse.data.success && verifyResponse.data.userId === form.value.userid.trim()) {
      step.value = 2; // 2단계로 이동
    } else {
      error.value = "입력하신 정보가 일치하지 않습니다.";
    }

  } catch (err) {
    if (err.response?.status === 404) {
      error.value = "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.";
    } else {
      error.value = "본인 확인 중 오류가 발생했습니다.";
    }
  } finally {
    loading.value = false;
  }
};

const handleResetPassword = async () => {
  if (!form.value.newPassword || !form.value.confirmPassword) {
    error.value = "새 비밀번호를 입력해주세요.";
    return;
  }

  if (form.value.newPassword !== form.value.confirmPassword) {
    error.value = "비밀번호가 일치하지 않습니다.";
    return;
  }

  if (form.value.newPassword.length < 8) {
    error.value = "비밀번호는 8자 이상이어야 합니다.";
    return;
  }

  loading.value = true;
  error.value = "";

  try {
    const resetResponse = await apiClient.post("/auth/resetPasswordImmediate", {
      userid: form.value.userid.trim(),
      email: form.value.email.trim(),
      name: form.value.name.trim(),
      newPassword: form.value.newPassword
    }, {
      withAuth: false,
      timeout: 15000
    });

    if (resetResponse.data.success) {
      step.value = 3; // 완료 단계로 이동

      // 폼 초기화
      form.value = {
        userid: "",
        email: "",
        name: "",
        newPassword: "",
        confirmPassword: ""
      };
    } else {
      error.value = resetResponse.data.message || "비밀번호 변경에 실패했습니다.";
    }

  } catch (err) {
    if (err.response) {
      error.value = err.response.data?.message || "비밀번호 변경에 실패했습니다.";
    } else {
      error.value = "서버에 연결할 수 없습니다.";
    }
  } finally {
    loading.value = false;
  }
};

const goBackToStep1 = () => {
  step.value = 1;
  form.value.newPassword = "";
  form.value.confirmPassword = "";
  error.value = "";
};

// 입력값 변경시 에러 메시지 클리어
watch(() => [form.value.userid, form.value.email, form.value.name, form.value.newPassword, form.value.confirmPassword], () => {
  if (error.value) {
    error.value = "";
  }
});
</script>

<style scoped src="@/assets/css/findPassword.css"></style>
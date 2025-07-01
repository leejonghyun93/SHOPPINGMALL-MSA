<template>
  <div class="find-password-container">
    <div class="container mt-5" style="max-width: 500px;">
      <!-- 헤더 섹션 -->
      <div class="header-section text-center mb-4">
        <div class="icon-wrapper mb-3">
          <div class="key-icon">🔑</div>
        </div>
        <h3 class="title">비밀번호 찾기</h3>
        <p class="subtitle">가입하신 정보를 입력하시면 비밀번호를 재설정할 수 있습니다.</p>
      </div>

      <!-- 폼 섹션 -->
      <div class="form-card">
        <form @submit.prevent="handleFindPassword">
          <div class="mb-3">
            <label for="userid" class="form-label">
              <span class="icon-emoji">👤</span>아이디
            </label>
            <input
                v-model="userid"
                type="text"
                class="form-control custom-input"
                id="userid"
                placeholder="아이디를 입력하세요"
                required
                :disabled="loading"
            />
          </div>

          <div class="mb-3">
            <label for="email" class="form-label">
              <span class="icon-emoji">📧</span>가입 시 사용한 이메일
            </label>
            <input
                v-model="email"
                type="email"
                class="form-control custom-input"
                id="email"
                placeholder="example@email.com"
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
            <span v-else class="icon-emoji">✉️</span>
            {{ loading ? '처리 중...' : '비밀번호 초기화 요청' }}
          </button>
        </form>
      </div>

      <!-- 결과 메시지 -->
      <div v-if="result && !error" class="alert alert-success custom-alert mt-3">
        <span class="icon-emoji">✅</span>
        {{ result }}
      </div>

      <div v-if="error" class="alert alert-danger custom-alert mt-3">
        <span class="icon-emoji">⚠️</span>
        {{ error }}
      </div>

      <!-- 성공 시 추가 액션 -->
      <div v-if="isSuccess" class="success-actions mt-4">
        <div class="text-center">
          <p class="success-text small mb-3">
            이메일을 확인하신 후 새 비밀번호로 로그인해주세요.
          </p>
          <div class="d-grid gap-2">
            <router-link to="/login" class="btn btn-outline-primary success-btn">
              <span class="icon-emoji">🚪</span>
              로그인 페이지로 이동
            </router-link>
          </div>
        </div>
      </div>

      <!-- 하단 네비게이션 -->
      <div class="bottom-navigation mt-4">
        <div class="nav-card">
          <div class="row text-center">
            <div class="col-4">
              <router-link to="/findId" class="nav-link-custom">
                <span class="nav-icon">🔍</span>
                <span>아이디 찾기</span>
              </router-link>
            </div>
            <div class="col-4">
              <router-link to="/login" class="nav-link-custom">
                <span class="nav-icon">🚪</span>
                <span>로그인</span>
              </router-link>
            </div>
            <div class="col-4">
              <router-link to="/register" class="nav-link-custom">
                <span class="nav-icon">👥</span>
                <span>회원가입</span>
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <!-- 도움말 섹션 -->
      <div class="help-section mt-4">
        <details class="help-details">
          <summary class="help-summary">
            <span class="icon-emoji">❓</span>
            비밀번호 찾기가 안 되시나요?
          </summary>
          <div class="help-content">
            <ul class="help-list">
              <li>아이디와 이메일 정보를 정확히 입력했는지 확인해주세요.</li>
              <li>스팸함을 포함하여 이메일을 확인해주세요.</li>
              <li>가입 시 사용한 이메일 주소가 맞는지 확인해주세요.</li>
              <li>문제가 지속되면 고객센터(1588-1234)로 문의해주세요.</li>
            </ul>
          </div>
        </details>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from "vue";
import { useRoute, useRouter } from 'vue-router';
import apiClient from '@/api/axiosInstance';
import '@/assets/css/findIdPassword.css';

const route = useRoute();
const router = useRouter();

const userid = ref("");
const email = ref("");
const result = ref("");
const error = ref("");
const loading = ref(false);
const isSuccess = ref(false);

// 아이디 찾기에서 전달받은 정보로 자동 입력
onMounted(() => {
  if (route.query.userId) {
    userid.value = route.query.userId;
  }
  if (route.query.email) {
    email.value = route.query.email;
  }

  // URL 파라미터가 있다면 안내 메시지 표시
  if (route.query.userId || route.query.email) {
    setTimeout(() => {
      showInfoMessage("아이디 찾기에서 전달받은 정보가 자동으로 입력되었습니다.");
    }, 500);
  }
});

const showInfoMessage = (message) => {
  // 임시 정보 메시지 (3초 후 사라짐)
  const tempDiv = document.createElement('div');
  tempDiv.className = 'alert alert-info custom-alert mt-2';
  tempDiv.innerHTML = `<span class="icon-emoji">ℹ️</span>${message}`;
  tempDiv.style.animation = 'fadeInOut 3s ease-in-out';

  const container = document.querySelector('.form-card');
  container.appendChild(tempDiv);

  setTimeout(() => {
    if (tempDiv.parentNode) {
      tempDiv.parentNode.removeChild(tempDiv);
    }
  }, 3000);
};

const handleFindPassword = async () => {
  // 폼 검증
  if (!userid.value.trim() || !email.value.trim()) {
    error.value = "아이디와 이메일을 모두 입력해주세요.";
    return;
  }

  loading.value = true;
  error.value = "";
  result.value = "";
  isSuccess.value = false;

  try {
    // 실제 findPassword API 호출
    const response = await apiClient.post("/auth/findPassword", {
      userid: userid.value.trim(),
      email: email.value.trim(),
    }, {
      withAuth: false,
      timeout: 15000
    });

    if (response.data.success !== false) {
      result.value = response.data.message || "비밀번호 재설정 이메일이 발송되었습니다.";
      isSuccess.value = true;

      // 성공 시 폼 비활성화
      setTimeout(() => {
        document.querySelector('#userid').disabled = true;
        document.querySelector('#email').disabled = true;
      }, 100);
    } else {
      error.value = response.data.message || "비밀번호 찾기에 실패했습니다.";
    }

  } catch (err) {
    if (err.response) {
      switch (err.response.status) {
        case 400:
          error.value = err.response.data?.message || "입력 정보를 확인해주세요.";
          break;
        case 404:
          error.value = "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.";
          break;
        case 429:
          error.value = "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.";
          break;
        case 500:
          error.value = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
          break;
        default:
          error.value = err.response.data?.message || "비밀번호 찾기에 실패했습니다.";
      }
    } else if (err.request) {
      error.value = "서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.";
    } else {
      error.value = "요청 처리 중 오류가 발생했습니다.";
    }
  } finally {
    loading.value = false;
  }
};

// 입력 필드 변경 시 메시지 초기화
const clearMessages = () => {
  error.value = "";
  result.value = "";
  isSuccess.value = false;
};

watch([userid, email], clearMessages);
</script>
<style scoped src="@/assets/css/findPassword.css"></style>



<template>
  <div class="find-password-container">
    <div class="container mt-5" style="max-width: 500px;">
      <!-- 헤더 섹션 -->
      <div class="header-section text-center mb-4">
        <div class="icon-wrapper mb-3">
          <!-- 🔥 FontAwesome 대신 CSS 아이콘 사용 -->
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
              <!-- 🔥 CSS 아이콘으로 변경 -->
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
              <!-- 🔥 CSS 아이콘으로 변경 -->
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
            <!-- 🔥 로딩 스피너도 CSS로 변경 -->
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
import { ref, onMounted } from "vue";
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

// 🔥 아이디 찾기에서 전달받은 정보로 자동 입력
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

// FindPassword.vue의 handleFindPassword에서 테스트 엔드포인트 먼저 호출

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
    console.log('🔍 실제 비밀번호 찾기 요청:', {
      userid: userid.value,
      email: email.value
    });

    // 🔥 실제 findPassword API 호출
    const response = await apiClient.post("/auth/findPassword", {
      userid: userid.value.trim(),
      email: email.value.trim(),
    }, {
      withAuth: false,
      timeout: 15000
    });

    console.log('✅ 비밀번호 찾기 성공:', response.data);

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
    console.error('❌ 비밀번호 찾기 실패:', err);

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
// 🔥 입력 필드 변경 시 메시지 초기화
const clearMessages = () => {
  error.value = "";
  result.value = "";
  isSuccess.value = false;
};

import { watch } from 'vue';
watch([userid, email], clearMessages);
</script>

<style scoped>
.find-password-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px 0;
}

.container {
  position: relative;
}

/* 🔥 이모지 아이콘 스타일 */
.icon-emoji {
  margin-right: 8px;
  font-size: 1.1em;
  display: inline-block;
  vertical-align: middle;
}

.key-icon {
  font-size: 2.5rem;
  line-height: 1;
}

.nav-icon {
  font-size: 1.5rem;
  margin-bottom: 4px;
  display: block;
}

/* 🔥 로딩 스피너 CSS로 구현 */
.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
  margin-right: 8px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 헤더 섹션 */
.header-section {
  color: white;
  margin-bottom: 2rem;
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.title {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.subtitle {
  font-size: 1rem;
  opacity: 0.9;
  margin-bottom: 0;
  line-height: 1.5;
}

/* 폼 카드 */
.form-card {
  background: white;
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  animation: slideInUp 0.6s ease-out;
}

.form-label {
  font-weight: 600;
  color: #495057;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
}

.custom-input {
  border: 2px solid #e9ecef;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 1rem;
  transition: all 0.3s ease;
  background: #f8f9fa;
  width: 100%;
}

.custom-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: white;
  transform: translateY(-1px);
  outline: none;
}

.custom-input:disabled {
  background-color: #e9ecef;
  opacity: 0.7;
}

/* 🔥 버튼 위치 및 스타일 수정 */
.custom-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  padding: 14px 24px;
  font-weight: 600;
  font-size: 1rem;
  transition: all 0.3s ease;
  text-transform: none;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 8px; /* 🔥 버튼과 입력 필드 간격 조정 */
}

.custom-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.3);
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

.custom-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

/* 알림 메시지 */
.custom-alert {
  border: none;
  border-radius: 12px;
  padding: 16px 20px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
}

.alert-success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.alert-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.alert-info {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
}

/* 성공 액션 */
.success-actions {
  animation: fadeInUp 0.5s ease;
}

.success-text {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.success-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.3);
  color: white;
  border-radius: 12px;
  padding: 12px 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  transition: all 0.3s ease;
}

.success-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.5);
  color: white;
  transform: translateY(-2px);
}

/* 하단 네비게이션 */
.bottom-navigation {
  margin-top: 2rem;
}

.nav-card {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 1rem;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.nav-link-custom {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: white;
  padding: 12px 8px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.nav-link-custom:hover {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  transform: translateY(-2px);
}

.nav-link-custom span:last-child {
  font-size: 0.8rem;
  font-weight: 500;
}

/* 도움말 섹션 */
.help-section {
  margin-top: 2rem;
}

.help-details {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.help-summary {
  padding: 16px 20px;
  color: white;
  cursor: pointer;
  font-weight: 500;
  border-radius: 12px;
  transition: all 0.3s ease;
  list-style: none;
  display: flex;
  align-items: center;
}

.help-summary:hover {
  background: rgba(255, 255, 255, 0.1);
}

.help-content {
  padding: 0 20px 20px;
  color: white;
}

.help-list {
  margin: 0;
  padding-left: 1.5rem;
  opacity: 0.9;
}

.help-list li {
  margin-bottom: 8px;
  line-height: 1.5;
  font-size: 0.9rem;
}

/* 애니메이션 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInOut {
  0%, 100% { opacity: 0; }
  20%, 80% { opacity: 1; }
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 반응형 디자인 */
@media (max-width: 576px) {
  .find-password-container {
    padding: 10px;
  }

  .container {
    max-width: 95% !important;
  }

  .form-card {
    padding: 1.5rem;
    margin: 0 10px;
  }

  .title {
    font-size: 1.5rem;
  }

  .subtitle {
    font-size: 0.9rem;
  }

  .icon-wrapper {
    width: 60px;
    height: 60px;
  }

  .key-icon {
    font-size: 2rem;
  }

  .nav-link-custom span:last-child {
    font-size: 0.7rem;
  }

  .custom-btn {
    padding: 12px 20px;
    font-size: 0.9rem;
  }
}
</style>
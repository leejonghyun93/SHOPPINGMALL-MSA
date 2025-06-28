<template>
  <div class="container mt-5" style="max-width: 400px;">
    <h3 class="mb-4">아이디 찾기</h3>
    <form @submit.prevent="handleFindId">
      <div class="mb-3">
        <label for="name" class="form-label">이름</label>
        <input
            v-model="name"
            type="text"
            class="form-control"
            id="name"
            required
            :disabled="loading"
        />
      </div>
      <div class="mb-3">
        <label for="email" class="form-label">가입 시 사용한 이메일</label>
        <input
            v-model="email"
            type="email"
            class="form-control"
            id="email"
            required
            :disabled="loading"
        />
      </div>
      <button
          type="submit"
          class="btn btn-primary w-100"
          :disabled="loading"
      >
        {{ loading ? '찾는 중...' : '아이디 찾기' }}
      </button>
    </form>

    <!-- 성공 결과 -->
    <div v-if="result && !error" class="alert alert-success mt-3">
      {{ result }}
    </div>

    <!-- 에러 메시지 -->
    <div v-if="error" class="alert alert-danger mt-3">
      {{ error }}
    </div>

    <!-- 🔥 성공 시 액션 버튼들 -->
    <div v-if="result && !error" class="action-buttons mt-3">
      <div class="d-grid gap-2">
        <!-- 로그인하러 가기 버튼 -->
        <router-link to="/login" class="btn btn-primary">
          <i class="fas fa-sign-in-alt me-2"></i>
          로그인하러 가기
        </router-link>

        <!-- 비밀번호 찾기 버튼 -->
        <button
            @click="goToPasswordReset"
            class="btn btn-outline-secondary"
        >
          <i class="fas fa-key me-2"></i>
          비밀번호 찾기
        </button>

        <!-- 다시 찾기 버튼 (선택사항) -->
        <button
            @click="resetForm"
            class="btn btn-outline-info"
        >
          <i class="fas fa-search me-2"></i>
          다른 아이디 찾기
        </button>
      </div>
    </div>

    <!-- 🔥 추가: 하단 링크들 -->
    <div v-if="!result" class="bottom-links mt-4 text-center">
      <div class="d-flex justify-content-between">
        <router-link to="/login" class="text-decoration-none small">
          로그인
        </router-link>
        <router-link to="/findPassword" class="text-decoration-none small">
          비밀번호 찾기
        </router-link>
        <router-link to="/register" class="text-decoration-none small">
          회원가입
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from 'vue-router';
import apiClient from '@/api/axiosInstance';
import '@/assets/css/findIdPassword.css';

const router = useRouter();

const email = ref("");
const name = ref("");
const result = ref("");
const error = ref("");
const loading = ref(false);
const foundUserId = ref(""); // 🔥 찾은 아이디 저장

const handleFindId = async () => {
  // 폼 검증
  if (!name.value.trim() || !email.value.trim()) {
    error.value = "이름과 이메일을 모두 입력해주세요.";
    return;
  }

  loading.value = true;
  error.value = "";
  result.value = "";

  try {
    console.log('🔍 아이디 찾기 요청:', {
      name: name.value,
      email: email.value
    });

    const response = await apiClient.get('/api/users/findId', {
      params: {
        name: name.value,
        email: email.value
      },
      withAuth: false,
      timeout: 10000
    });

    console.log('✅ 아이디 찾기 성공:', response.data);

    // 응답 데이터 구조에 맞게 수정
    if (response.data.success) {
      foundUserId.value = response.data.userId; // 🔥 찾은 아이디 저장
      result.value = `회원님의 아이디는: ${response.data.userId} 입니다.`;
    } else {
      error.value = response.data.message || "아이디를 찾을 수 없습니다.";
    }

  } catch (err) {
    console.error('❌ 아이디 찾기 실패:', err);

    if (err.response) {
      // 서버 응답이 있는 경우
      switch (err.response.status) {
        case 400:
          error.value = err.response.data?.message || "입력 정보를 확인해주세요.";
          break;
        case 404:
          error.value = "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.";
          break;
        case 500:
          error.value = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
          break;
        default:
          error.value = err.response.data?.message || "아이디 찾기에 실패했습니다.";
      }
    } else if (err.request) {
      // 요청은 보냈지만 응답이 없는 경우
      error.value = "서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.";
    } else {
      // 요청 설정 중 오류가 발생한 경우
      error.value = "요청 처리 중 오류가 발생했습니다.";
    }
  } finally {
    loading.value = false;
  }
};

// 🔥 비밀번호 찾기 페이지로 이동 (찾은 아이디 정보 전달)
const goToPasswordReset = () => {
  // 찾은 아이디와 입력한 정보를 비밀번호 찾기 페이지로 전달
  router.push({
    path: '/findPassword',
    query: {
      userId: foundUserId.value,
      name: name.value,
      email: email.value
    }
  });
};

// 🔥 폼 초기화 (다른 아이디 찾기)
const resetForm = () => {
  name.value = "";
  email.value = "";
  result.value = "";
  error.value = "";
  foundUserId.value = "";
};

// 🔥 입력 필드 변경 시 에러 메시지 초기화
const clearMessages = () => {
  error.value = "";
  result.value = "";
  foundUserId.value = "";
};

// watch를 사용하여 입력 변경 감지
import { watch } from 'vue';
watch([name, email], clearMessages);
</script>

<style scoped>
.container {
  padding: 20px;
}

.form-control:disabled {
  background-color: #f8f9fa;
}

.btn:disabled {
  cursor: not-allowed;
}

.alert {
  word-break: break-word;
}

/* 로딩 상태 시각적 피드백 */
.btn:disabled {
  opacity: 0.7;
}

/* 성공/에러 메시지 스타일 개선 */
.alert-success {
  border-color: #d4edda;
  background-color: #d1ecf1;
  color: #0c5460;
}

.alert-danger {
  border-color: #f5c6cb;
  background-color: #f8d7da;
  color: #721c24;
}

/* 🔥 액션 버튼들 스타일 */
.action-buttons {
  animation: fadeIn 0.5s ease-in;
}

.action-buttons .btn {
  transition: all 0.3s ease;
  border-radius: 8px;
  padding: 12px 20px;
  font-weight: 500;
}

.action-buttons .btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.action-buttons .btn-primary {
  background: linear-gradient(135deg, #007bff, #0056b3);
  border: none;
}

.action-buttons .btn-outline-secondary {
  border-color: #6c757d;
  color: #6c757d;
}

.action-buttons .btn-outline-secondary:hover {
  background-color: #6c757d;
  border-color: #6c757d;
  color: white;
}

.action-buttons .btn-outline-info {
  border-color: #17a2b8;
  color: #17a2b8;
}

.action-buttons .btn-outline-info:hover {
  background-color: #17a2b8;
  border-color: #17a2b8;
  color: white;
}

/* 하단 링크들 스타일 */
.bottom-links {
  border-top: 1px solid #e9ecef;
  padding-top: 15px;
}

.bottom-links a {
  color: #6c757d;
  transition: color 0.3s ease;
}

.bottom-links a:hover {
  color: #007bff;
}

/* 아이콘 스타일 */
.fas {
  width: 16px;
  text-align: center;
}

/* 애니메이션 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 반응형 디자인 */
@media (max-width: 576px) {
  .container {
    padding: 15px;
    margin-top: 20px !important;
  }

  .action-buttons .btn {
    padding: 10px 15px;
    font-size: 14px;
  }

  .bottom-links {
    font-size: 12px;
  }
}
</style>
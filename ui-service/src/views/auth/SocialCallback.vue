<template>
  <div class="d-flex justify-content-center align-items-center vh-100 bg-light">
    <div class="card p-4 shadow-sm text-center" style="width: 100%; max-width: 400px;">
      <div v-if="isProcessing">
        <div class="spinner-border text-primary mb-3" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <h5>로그인 처리 중...</h5>
        <p class="text-muted">잠시만 기다려주세요.</p>
      </div>

      <div v-else-if="hasError">
        <div class="text-danger mb-3">
          <i class="bi bi-exclamation-triangle" style="font-size: 2rem;"></i>
        </div>
        <h5 class="text-danger">로그인 실패</h5>
        <p class="text-muted">{{ errorMessage }}</p>
        <button
            class="btn btn-primary mt-3"
            @click="goToLogin"
        >
          로그인 페이지로 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { setUserFromToken, user, saveSocialLoginName } from '@/stores/userStore'
import apiClient from '@/api/axiosInstance'

const router = useRouter()
const isProcessing = ref(true)
const hasError = ref(false)
const errorMessage = ref('')

const processSocialCallback = async () => {
  try {
    const urlParams = new URLSearchParams(window.location.search)
    const token = urlParams.get('token')
    const refreshToken = urlParams.get('refresh')
    const error = urlParams.get('error')

    const cleanUrl = window.location.origin + window.location.pathname
    window.history.replaceState({}, document.title, cleanUrl)

    if (error) {
      showError(decodeURIComponent(error))
      return
    }

    if (!token) {
      showError('로그인 토큰을 받지 못했습니다.')
      return
    }

    localStorage.setItem('token', token)
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken)
    }

    // 🔥 토큰에서 소셜 로그인 사용자 이름 강제 추출 및 보존
    let extractedSocialName = null;
    try {
      const parts = token.split('.');
      if (parts.length === 3) {
        let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        while (base64.length % 4) {
          base64 += '=';
        }
        const payload = JSON.parse(atob(base64));

        if (payload.name && payload.name.trim() && payload.name !== payload.sub && payload.name !== "사용자") {
          if (/[가-힣]/.test(payload.name) || (/^[a-zA-Z\s]+$/.test(payload.name) && payload.name.length > 1)) {
            extractedSocialName = payload.name.trim();

            // 🔥 즉시 모든 저장소에 저장
            localStorage.setItem('social_login_name', extractedSocialName);
            localStorage.setItem('user_display_name', extractedSocialName);
            localStorage.setItem('preserved_user_name', extractedSocialName);
            sessionStorage.setItem('current_user_name', extractedSocialName);

            // userStore에 즉시 설정
            user.name = extractedSocialName;

            // 🔥 소셜 로그인 이름 잠금
            if (saveSocialLoginName) {
              saveSocialLoginName(extractedSocialName);
            }
          }
        }
      }
    } catch (e) {
      // 토큰 파싱 에러 무시
    }

    // 토큰으로 사용자 정보 설정
    setUserFromToken(token)

    // 🔥 setUserFromToken 후에도 이름이 덮어써지지 않도록 강제 복원
    if (extractedSocialName) {
      user.name = extractedSocialName;
      sessionStorage.setItem('current_user_name', extractedSocialName);
    }

    // 프로필 API는 호출하지만 이름은 덮어쓰지 않음
    const profileSuccess = await fetchUserProfile(token, extractedSocialName)

    // 🔥 최종 확인 및 강제 설정
    if (extractedSocialName && (!user.name || user.name === "사용자")) {
      user.name = extractedSocialName;
      sessionStorage.setItem('current_user_name', extractedSocialName);
    }

    setTimeout(async () => {
      await router.push('/')
    }, 1000)

  } catch (error) {
    showError('로그인 처리 중 오류가 발생했습니다.')
  }
}

const fetchUserProfile = async (token, protectedSocialName = null) => {
  try {
    // 🔥 소셜 로그인 이름이 보호되어 있으면 API 호출 건너뛰기
    if (protectedSocialName) {
      user.name = protectedSocialName;
      return true;
    }

    const response = await apiClient.get('/api/users/profile')

    if (response.data && response.data.success && response.data.data) {
      const userData = response.data.data

      user.id = userData.userId
      user.email = userData.email
      user.role = userData.role || 'USER'
      user.phone = userData.phone

      // 🔥 소셜 로그인 이름이 보호되어 있으면 API 이름 무시
      const currentSocialName = localStorage.getItem('social_login_name') ||
          sessionStorage.getItem('current_user_name');

      if (currentSocialName && currentSocialName.trim() && currentSocialName !== "사용자") {
        user.name = currentSocialName;
      } else if (userData.name && userData.name.trim()) {
        user.name = userData.name;
        sessionStorage.setItem('current_user_name', userData.name);
      } else {
        user.name = "사용자";
      }

      return true
    }
  } catch (error) {
    // 🔥 API 실패해도 소셜 로그인 이름은 절대 보호
    if (protectedSocialName) {
      user.name = protectedSocialName;
    }
  }
  return false
}

const showError = (message) => {
  isProcessing.value = false
  hasError.value = true
  errorMessage.value = message
}

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  processSocialCallback()
})
</script>

<style scoped>
.spinner-border {
  width: 3rem;
  height: 3rem;
}

.card {
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
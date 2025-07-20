<template>
  <div class="d-flex justify-content-center align-items-center vh-100 bg-light">
    <div class="card p-4 shadow-sm text-center" style="width: 100%; max-width: 400px;">
      <div v-if="isProcessing">
        <div class="spinner-border text-primary mb-3" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <h5>소셜 로그인 처리 중...</h5>
        <p class="text-muted">잠시만 기다려주세요.</p>
      </div>

      <div v-else-if="hasError">
        <div class="text-danger mb-3">
          <div style="font-size: 2rem;">⚠️</div>
        </div>
        <h5 class="text-danger">소셜 로그인 실패</h5>
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
import { setSocialLogin, user } from '@/stores/userStore'
import apiClient from '@/api/axiosInstance'

const router = useRouter()
const isProcessing = ref(true)
const hasError = ref(false)
const errorMessage = ref('')

const detectProvider = () => {
  const currentPath = window.location.pathname;

  if (currentPath.includes('/kakao/')) {
    return 'KAKAO';
  } else if (currentPath.includes('/naver/')) {
    return 'NAVER';
  } else {
    // localStorage에서 확인
    return localStorage.getItem('oauth_provider')?.toUpperCase() || 'SOCIAL';
  }
};

const processSocialCallback = async () => {
  try {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token') || urlParams.get('jwt');
    const error = urlParams.get('error');

    // 🔥 제공업체 자동 감지
    const detectedProvider = detectProvider();
    console.log('감지된 제공업체:', detectedProvider);

    // URL 정리
    const cleanUrl = window.location.origin + window.location.pathname;
    window.history.replaceState({}, document.title, cleanUrl);

    if (error) {
      showError(decodeURIComponent(error));
      return;
    }

    if (!token) {
      showError('소셜 로그인 토큰을 받지 못했습니다.');
      return;
    }

    // 토큰 파싱에서 provider 기본값으로 감지된 값 사용
    let socialProvider = detectedProvider;

    // ... 나머지 토큰 파싱 로직에서 socialProvider 기본값 사용 ...

  } catch (error) {
    showError('소셜 로그인 처리 중 오류가 발생했습니다.');
  }
};


// 이름 추출 함수
function extractName(payload, provider) {
  // 1. 직접적인 name 필드들 확인
  const nameFields = [
    'name', 'nickname', 'display_name', 'username', 'user_name',
    'full_name', 'realname', 'displayName', 'userName', 'fullName'
  ]

  for (const field of nameFields) {
    const value = payload[field]
    if (value && typeof value === 'string' && value.trim()) {
      const cleanName = cleanName(value.trim())
      if (isValidName(cleanName)) {
        return cleanName
      }
    }
  }

  // 2. 중첩된 객체에서 추출
  if (payload.kakao_account?.profile?.nickname) {
    const name = cleanName(payload.kakao_account.profile.nickname)
    if (isValidName(name)) return name
  }

  if (payload.response?.nickname) {
    const name = cleanName(payload.response.nickname)
    if (isValidName(name)) return name
  }

  if (payload.profile?.nickname) {
    const name = cleanName(payload.profile.nickname)
    if (isValidName(name)) return name
  }

  // 3. 제공업체별 특화 필드
  if (provider === 'KAKAO') {
    const kakaoFields = ['properties.nickname', 'kakao_account.profile.nickname']
    for (const fieldPath of kakaoFields) {
      const value = getNestedValue(payload, fieldPath)
      if (value) {
        const name = cleanName(value)
        if (isValidName(name)) return name
      }
    }
  }

  if (provider === 'NAVER') {
    const naverFields = ['response.nickname', 'response.name']
    for (const fieldPath of naverFields) {
      const value = getNestedValue(payload, fieldPath)
      if (value) {
        const name = cleanName(value)
        if (isValidName(name)) return name
      }
    }
  }

  return null
}

// 이메일 추출 함수
function extractEmail(payload) {
  const emailFields = ['email', 'mail', 'userEmail', 'emailAddress']
  for (const field of emailFields) {
    const value = getNestedValue(payload, field) || payload[field]
    if (value && typeof value === 'string' && value.includes('@')) {
      return value
    }
  }
  return null
}

// 휴대폰 추출 함수
function extractPhone(payload) {
  const phoneFields = ['phone', 'phoneNumber', 'mobile', 'tel']
  for (const field of phoneFields) {
    const value = getNestedValue(payload, field) || payload[field]
    if (value && typeof value === 'string') {
      return value
    }
  }
  return null
}

// 중첩된 객체 값 가져오기
function getNestedValue(obj, path) {
  return path.split('.').reduce((current, key) => current?.[key], obj)
}

// 이름 정제 함수
function cleanName(rawName) {
  if (!rawName || typeof rawName !== 'string') return null

  let cleanedName = rawName.trim()

  // URI 디코딩 시도
  try {
    if (cleanedName.includes('%')) {
      cleanedName = decodeURIComponent(cleanedName)
    }
  } catch (e) {
    // 디코딩 실패해도 계속
  }

  // 깨진 한글 복구 시도
  if (hasGarbledCharacters(cleanedName)) {
    try {
      cleanedName = repairGarbledKorean(cleanedName)
    } catch (e) {
      // 복구 실패해도 계속
    }
  }

  return cleanedName
}

// 유효한 이름인지 검증
function isValidName(name) {
  if (!name || typeof name !== 'string') return false

  const trimmed = name.trim()
  if (trimmed.length < 1 || trimmed.length > 50) return false

  // 기본값들 제외
  const invalidValues = [
    '사용자', '소셜사용자', 'user', 'null', 'undefined',
    '카카오사용자', '네이버사용자', '구글사용자',
    'kakao', 'naver', 'google', 'social'
  ]

  if (invalidValues.includes(trimmed.toLowerCase())) return false

  // 깨진 문자 확인
  if (hasGarbledCharacters(trimmed)) return false

  return true
}

// 제공업체 표시명 반환
function getProviderDisplayName(provider) {
  const names = {
    'KAKAO': '카카오',
    'NAVER': '네이버',
    'GOOGLE': '구글'
  }
  return names[provider] || '소셜'
}

// 깨진 문자 확인
function hasGarbledCharacters(text) {
  if (!text) return false

  const garbledPatterns = [
    /[\uFFFD]/g,
    /[ì í î ë ê é è ñ ò ó ô]/g,
    /â[^\s]/g,
    /Ã[^\s]/g,
  ]

  return garbledPatterns.some(pattern => pattern.test(text))
}

// 깨진 한글 복구
function repairGarbledKorean(garbledText) {
  try {
    const bytes = new Uint8Array(garbledText.length)
    for (let i = 0; i < garbledText.length; i++) {
      bytes[i] = garbledText.charCodeAt(i) & 0xFF
    }
    const repaired = new TextDecoder('utf-8').decode(bytes)

    if (!hasGarbledCharacters(repaired) && /[가-힣]/.test(repaired)) {
      return repaired
    }
  } catch (error) {
    // 복구 실패
  }

  return garbledText
}

// 프로필 API 호출
const fetchUserProfile = async () => {
  try {
    const response = await apiClient.get('/api/users/profile')

    if (response.data && response.data.success && response.data.data) {
      const userData = response.data.data

      user.id = userData.userId || user.id
      user.role = userData.role || user.role || 'USER'

      // 소셜 정보가 없는 경우에만 API 정보 사용
      if (!localStorage.getItem('social_name') && userData.name) {
        user.name = userData.name
        localStorage.setItem('social_name', userData.name)
        sessionStorage.setItem('social_name', userData.name)
      }
      if (!localStorage.getItem('social_email') && userData.email) {
        user.email = userData.email
        localStorage.setItem('social_email', userData.email)
        sessionStorage.setItem('social_email', userData.email)
      }
      if (!localStorage.getItem('social_phone') && userData.phone) {
        user.phone = userData.phone
        localStorage.setItem('social_phone', userData.phone)
        sessionStorage.setItem('social_phone', userData.phone)
      }

      return true
    }
  } catch (error) {
    // API 실패해도 소셜 정보는 유지
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
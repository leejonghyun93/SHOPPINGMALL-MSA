<template>
  <div class="profile-container">
    <!-- 비밀번호 확인 단계 -->
    <div v-if="currentStep === 'verify'" class="verification-step">
      <div class="step-header">
        <button @click="goBack" class="back-button">
          <i class="fas fa-arrow-left back-icon"></i>
        </button>
        <div class="header-info">
          <i class="fas fa-lock header-icon"></i>
          <div>
            <h2 class="step-title">비밀번호 재확인</h2>
            <p class="step-subtitle">
              회원님의 정보를 안전하게 보호하기 위해 비밀번호를 다시 한번 확인해주세요.
            </p>
          </div>
        </div>
      </div>

      <div class="verification-card">
        <div class="form-section">
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-user label-icon"></i>
              아이디
            </label>
            <input
                type="text"
                :value="computedUser.id || computedUser.username || ''"
                disabled
                class="form-input disabled"
            />
          </div>

          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-lock label-icon"></i>
              현재 비밀번호 <span class="required">*</span>
            </label>
            <div class="password-input-group">
              <input
                  :type="showCurrentPassword ? 'text' : 'password'"
                  v-model="currentPassword"
                  placeholder="현재 비밀번호를 입력해주세요"
                  class="form-input"
                  @keypress.enter="verifyPassword"
              />
              <button
                  type="button"
                  @click="togglePasswordVisibility('current')"
                  class="password-toggle"
              >
                <i :class="showCurrentPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
              </button>
            </div>
          </div>

          <div v-if="passwordError" class="error-alert">
            {{ passwordError }}
          </div>

          <div class="verification-actions">
            <button
                @click="verifyPassword"
                :disabled="verifying || !currentPassword"
                class="verify-button"
            >
              <div v-if="verifying" class="mini-spinner"></div>
              {{ verifying ? '확인 중...' : '확인' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 정보 수정 단계 -->
    <div v-else class="edit-step">
      <div class="step-header">
        <button @click="goBack" class="back-button">
          <i class="fas fa-arrow-left back-icon"></i>
        </button>
        <div class="header-info">
          <i class="fas fa-edit header-icon"></i>
          <div>
            <h2 class="step-title">개인정보 수정</h2>
            <p class="step-subtitle">변경하실 정보를 입력해주세요</p>
          </div>
        </div>
      </div>

      <div class="edit-card">
        <form @submit.prevent="updateUserInfo">
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">
                <i class="fas fa-user label-icon"></i>
                아이디
              </label>
              <input
                  type="text"
                  :value="userInfo.username"
                  disabled
                  class="form-input disabled"
              />
            </div>

            <div class="form-group">
              <label class="form-label">
                <i class="fas fa-user label-icon"></i>
                이름 <span class="required">*</span>
              </label>
              <input
                  type="text"
                  v-model="userInfo.name"
                  required
                  class="form-input"
              />
            </div>

            <div class="form-group">
              <label class="form-label">
                <i class="fas fa-envelope label-icon"></i>
                이메일 <span class="required">*</span>
              </label>
              <input
                  type="email"
                  v-model="userInfo.email"
                  required
                  class="form-input"
              />
            </div>

            <div class="form-group">
              <label class="form-label">
                <i class="fas fa-phone label-icon"></i>
                전화번호
              </label>
              <input
                  type="text"
                  v-model="userInfo.phone"
                  class="form-input"
                  placeholder="010-1234-5678"
              />
            </div>

            <div class="form-group">
              <label class="form-label">
                <i class="fas fa-calendar label-icon"></i>
                생년월일
              </label>
              <input
                  type="date"
                  v-model="userInfo.birthDate"
                  class="form-input"
              />
            </div>

            <div class="form-group">
              <label class="form-label">
                <i class="fas fa-user label-icon"></i>
                성별
              </label>
              <div class="gender-toggle">
                <label :class="{ active: userInfo.gender === 'M' }">
                  <input type="radio" value="M" v-model="userInfo.gender"> 남자
                </label>
                <label :class="{ active: userInfo.gender === 'F' }">
                  <input type="radio" value="F" v-model="userInfo.gender"> 여자
                </label>
                <label :class="{ active: userInfo.gender === 'U' }">
                  <input type="radio" value="U" v-model="userInfo.gender"> 선택 안 함
                </label>
              </div>
            </div>

            <div class="form-group full-width password-section">
              <h3 class="password-section-title">비밀번호 변경 (선택사항)</h3>

              <div class="password-grid">
                <div class="form-group">
                  <label class="form-label">
                    <i class="fas fa-lock label-icon"></i>
                    새 비밀번호
                  </label>
                  <div class="password-input-group">
                    <input
                        :type="showPasswords.new ? 'text' : 'password'"
                        v-model="userInfo.newPassword"
                        placeholder="새 비밀번호 (변경시에만 입력)"
                        class="form-input"
                    />
                    <button
                        type="button"
                        @click="togglePasswordVisibility('new')"
                        class="password-toggle"
                    >
                      <i :class="showPasswords.new ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                    </button>
                  </div>
                  <div class="form-help">8자 이상, 영문/숫자/특수문자 조합</div>
                </div>

                <div class="form-group">
                  <label class="form-label">
                    <i class="fas fa-lock label-icon"></i>
                    새 비밀번호 확인
                  </label>
                  <div class="password-input-group">
                    <input
                        :type="showPasswords.confirm ? 'text' : 'password'"
                        v-model="userInfo.confirmNewPassword"
                        placeholder="새 비밀번호 확인"
                        class="form-input"
                    />
                    <button
                        type="button"
                        @click="togglePasswordVisibility('confirm')"
                        class="password-toggle"
                    >
                      <i :class="showPasswords.confirm ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                    </button>
                  </div>
                  <div v-if="passwordMismatch" class="error-text">
                    새 비밀번호가 일치하지 않습니다.
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button
                type="submit"
                :disabled="saving"
                class="save-button"
            >
              <i class="fas fa-save button-icon"></i>
              <div v-if="saving" class="mini-spinner"></div>
              {{ saving ? '저장 중...' : '저장' }}
            </button>
            <button
                type="button"
                @click="cancelEdit"
                class="cancel-button"
            >
              <i class="fas fa-times button-icon"></i>
              취소
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="spinner"></div>
        <p class="loading-text">회원 정보를 불러오는 중...</p>
      </div>
    </div>

    <!-- 에러 상태 -->
    <div v-if="error" class="error-container">
      <p class="error-message">{{ error }}</p>
      <button @click="loadUserDetailInfo" class="retry-button">
        다시 시도
      </button>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, computed} from 'vue'
import {useRouter} from 'vue-router'
import {user, setUserFromToken} from '@/stores/userStore'

const router = useRouter()

// 개발 환경 체크
const isDevelopment = computed(() => import.meta.env.DEV)

// userStore에서 사용자 정보 가져오기
const computedUser = computed(() => user)

// 상태 관리
const currentStep = ref('verify')
const loading = ref(false)
const verifying = ref(false)
const saving = ref(false)
const error = ref('')

// 비밀번호 확인 관련
const currentPassword = ref('')
const passwordError = ref('')
const showCurrentPassword = ref(false)

// 정보 수정 관련
const userInfo = ref({
  username: '',
  name: '',
  email: '',
  phone: '',
  birthDate: '',
  gender: 'U',
  newPassword: '',
  confirmNewPassword: ''
})

const showPasswords = ref({
  new: false,
  confirm: false
})

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// 🔥 토큰 가져오기 함수
const getToken = () => {
  return localStorage.getItem('token')
}

// 🔥 JWT 토큰에서 userId 추출하는 함수
const getUserIdFromToken = (token) => {
  try {
    if (!token) return null

    // JWT 토큰은 header.payload.signature 형태
    const payload = token.split('.')[1]
    if (!payload) return null

    // Base64 디코딩
    const decodedPayload = JSON.parse(atob(payload))
    console.log('Decoded JWT payload:', decodedPayload)

    // 백엔드에서 사용하는 필드명에 맞춰 확인
    return decodedPayload.sub || decodedPayload.userId || decodedPayload.username || decodedPayload.user_id
  } catch (error) {
    console.error('JWT 토큰 파싱 오류:', error)
    return null
  }
}

// 🔥 현재 사용자 ID 가져오기 (여러 소스에서 시도)
const getCurrentUserId = () => {
  // 1. userStore에서 시도
  if (computedUser.value?.id) return computedUser.value.id
  if (computedUser.value?.userId) return computedUser.value.userId
  if (computedUser.value?.username) return computedUser.value.username

  // 2. localStorage에서 직접 시도
  const storedUserId = localStorage.getItem('userId')
  if (storedUserId) return storedUserId

  // 3. JWT 토큰에서 직접 추출 시도
  const token = getToken()
  if (token) {
    const userIdFromToken = getUserIdFromToken(token)
    if (userIdFromToken) {
      // localStorage에 저장해서 다음에 바로 사용할 수 있도록
      localStorage.setItem('userId', userIdFromToken)
      return userIdFromToken
    }
  }

  return null
}

// 인증 헤더 생성
const getAuthHeaders = () => {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

// 비밀번호 검증 computed
const passwordMismatch = computed(() => {
  if (!userInfo.value.newPassword && !userInfo.value.confirmNewPassword) {
    return false
  }
  return userInfo.value.newPassword !== userInfo.value.confirmNewPassword
})

// 사용자 정보 로드
const loadUserProfile = async () => {
  try {
    loading.value = true
    error.value = ''

    const token = getToken()
    if (!token) {
      error.value = '로그인이 필요합니다.'
      router.push('/login')
      return
    }

    console.log('프로필 로드 시도 - 토큰:', token.substring(0, 20) + '...')

    const response = await fetch(`${API_BASE_URL}/api/users/profile`, {
      method: 'GET',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      if (response.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        router.push('/login')
        return
      }
      throw new Error('사용자 정보를 불러올 수 없습니다.')
    }

    const userData = await response.json()
    console.log('받은 사용자 데이터:', userData)

    userInfo.value = {
      username: userData.userId,
      name: userData.name || '',
      email: userData.email || '',
      phone: userData.phone || '',
      birthDate: userData.birthDate || '',
      gender: userData.gender || 'U',
      newPassword: '',
      confirmNewPassword: ''
    }

  } catch (err) {
    console.error('프로필 로드 오류:', err)
    error.value = err.message || '사용자 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

// 비밀번호 확인
const verifyPassword = async () => {
  if (!currentPassword.value) {
    passwordError.value = '현재 비밀번호를 입력해주세요.'
    return
  }

  // 🔥 토큰과 userId 확인
  const token = getToken()
  const userId = getCurrentUserId()

  console.log('비밀번호 확인 시도:')
  console.log('- 토큰 존재:', !!token)
  console.log('- 현재 userId:', userId)
  console.log('- 토큰 앞부분:', token ? token.substring(0, 20) + '...' : 'null')

  if (!token) {
    passwordError.value = '인증 토큰이 없습니다. 다시 로그인해주세요.'
    router.push('/login')
    return
  }

  if (!userId) {
    passwordError.value = '사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.'
    router.push('/login')
    return
  }

  verifying.value = true
  passwordError.value = ''

  try {
    const requestBody = {
      password: currentPassword.value
    }

    console.log('비밀번호 확인 요청:', requestBody)

    const response = await fetch(`${API_BASE_URL}/api/users/verify-password`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(requestBody)
    })

    console.log('비밀번호 확인 응답 상태:', response.status)

    if (response.ok) {
      currentStep.value = 'edit'
      currentPassword.value = ''
      passwordError.value = ''
      await loadUserProfile()
    } else {
      const errorData = await response.json()
      console.error('비밀번호 확인 실패:', errorData)
      passwordError.value = errorData.message || '비밀번호가 일치하지 않습니다.'
    }
  } catch (err) {
    console.error('비밀번호 확인 오류:', err)
    passwordError.value = '비밀번호 확인 중 오류가 발생했습니다.'
  } finally {
    verifying.value = false
  }
}

// 사용자 정보 수정
const updateUserInfo = async () => {
  if (!userInfo.value.name || !userInfo.value.email) {
    alert('이름과 이메일은 필수 항목입니다.')
    return
  }

  if (userInfo.value.newPassword && passwordMismatch.value) {
    alert('새 비밀번호가 일치하지 않습니다.')
    return
  }

  if (userInfo.value.newPassword && userInfo.value.newPassword.length < 8) {
    alert('새 비밀번호는 8자 이상이어야 합니다.')
    return
  }

  try {
    saving.value = true

    const updateData = {
      name: userInfo.value.name,
      email: userInfo.value.email,
      phone: userInfo.value.phone,
      birthDate: userInfo.value.birthDate,
      gender: userInfo.value.gender
    }

    if (userInfo.value.newPassword) {
      updateData.password = userInfo.value.newPassword
    }

    const response = await fetch(`${API_BASE_URL}/api/users/profile`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(updateData)
    })

    if (!response.ok) {
      const result = await response.json()
      throw new Error(result.message || '회원 정보 수정에 실패했습니다.')
    }

    // userStore 업데이트
    if (user.name !== undefined) user.name = userInfo.value.name
    if (user.email !== undefined) user.email = userInfo.value.email

    // 비밀번호 필드 초기화
    userInfo.value.newPassword = ''
    userInfo.value.confirmNewPassword = ''

    alert('회원 정보가 수정되었습니다.')
    router.push('/mypage')

  } catch (err) {
    alert(err.message || '회원 정보 수정에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

// 뒤로 가기
const goBack = () => {
  if (currentStep.value === 'edit') {
    currentStep.value = 'verify'
    userInfo.value.newPassword = ''
    userInfo.value.confirmNewPassword = ''
  } else {
    router.push('/mypage')
  }
}

// 취소
const cancelEdit = () => {
  userInfo.value.newPassword = ''
  userInfo.value.confirmNewPassword = ''
  currentStep.value = 'verify'
  currentPassword.value = ''
  passwordError.value = ''
}

// 비밀번호 표시/숨김 토글
const togglePasswordVisibility = (field) => {
  if (field === 'current') {
    showCurrentPassword.value = !showCurrentPassword.value
  } else {
    showPasswords.value[field] = !showPasswords.value[field]
  }
}

// 🔥 컴포넌트 마운트 시 실행 - 토큰 검증 및 userId 설정
onMounted(() => {
  const token = getToken()
  console.log('컴포넌트 마운트 - 토큰 존재:', !!token)

  if (token) {
    // userStore 업데이트
    setUserFromToken(token)

    // userId 확인 및 설정
    const userId = getUserIdFromToken(token)
    if (userId) {
      localStorage.setItem('userId', userId)
      console.log('userId 설정됨:', userId)
    } else {
      console.warn('JWT 토큰에서 userId를 추출할 수 없습니다.')
    }
  } else {
    console.warn('토큰이 없습니다. 로그인 페이지로 이동합니다.')
    router.push('/login')
  }
})
</script>

<style scoped>
.profile-container {
  background: transparent;
  padding: 0;
  min-height: 600px;
}

.step-header {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: #f3f4f6;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  color: #6b7280;
}

.back-button:hover {
  background: #e5e7eb;
  color: #374151;
  transform: translateX(-2px);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.header-icon {
  width: 40px;
  height: 40px;
  color: #3b82f6;
  background: #dbeafe;
  padding: 8px;
  border-radius: 10px;
}

.step-title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.step-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 4px 0 0 0;
}

.verification-card,
.edit-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #f3f4f6;
  max-width: 600px;
  margin: 0 auto;
}

.edit-card {
  max-width: 900px;
}

.form-section {
  max-width: 400px;
  margin: 0 auto;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.password-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 16px;
}

.form-group.full-width {
  grid-column: span 2;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.label-icon {
  width: 16px;
  height: 16px;
  color: #6b7280;
}

.required {
  color: #dc2626;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  background: white;
  transition: all 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input.disabled {
  background: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.password-input-group {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input-group .form-input {
  padding-right: 44px;
}

.password-toggle {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  z-index: 1;
}

.password-toggle:hover {
  color: #374151;
  background: #f3f4f6;
}

.gender-toggle {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.gender-toggle label {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border: 2px solid #e9ecef;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
  background-color: white;
  font-size: 14px;
}

.gender-toggle label:hover {
  border-color: #007bff;
  background-color: #f8f9fa;
}

.gender-toggle label.active {
  border-color: #007bff;
  background-color: #007bff;
  color: white;
}

.gender-toggle input[type="radio"] {
  margin-right: 8px;
}

.form-help {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.error-text {
  font-size: 12px;
  color: #dc2626;
  margin-top: 4px;
}

.password-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  margin-top: 8px;
}

.password-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 16px 0;
}

.verification-actions {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;
}

.verify-button,
.save-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 16px;
}

.verify-button:hover,
.save-button:hover {
  background: #2563eb;
  transform: translateY(-1px);
}

.verify-button:disabled,
.save-button:disabled {
  background: #9ca3af;
  cursor: not-allowed;
  transform: none;
}

.cancel-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 24px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-button:hover {
  background: #e5e7eb;
  color: #374151;
}

.button-icon {
  width: 16px;
  height: 16px;
}

.mini-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f3f4f6;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

.error-alert {
  padding: 12px 16px;
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 8px;
  margin: 16px 0;
  font-size: 14px;
  font-weight: 500;
}

.loading-container,
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.loading-content {
  text-align: center;
}

.loading-text {
  color: #6b7280;
  font-size: 16px;
  font-weight: 500;
  margin: 0;
}

.error-container {
  text-align: center;
  padding: 48px 24px;
  background-color: #fef2f2;
  border-color: #fecaca;
}

.error-message {
  color: #dc2626;
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 20px 0;
}

.retry-button {
  padding: 12px 24px;
  background: #dc2626;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-button:hover {
  background: #b91c1c;
  transform: translateY(-1px);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .step-header {
    padding: 20px;
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .step-title {
    font-size: 24px;
  }

  .verification-card,
  .edit-card {
    padding: 24px;
    margin: 0 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .password-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .form-group.full-width {
    grid-column: span 1;
  }

  .form-actions {
    flex-direction: column;
    gap: 12px;
  }

  .verify-button,
  .save-button,
  .cancel-button {
    width: 100%;
    justify-content: center;
  }

  .password-section {
    padding: 20px;
  }
}
</style>
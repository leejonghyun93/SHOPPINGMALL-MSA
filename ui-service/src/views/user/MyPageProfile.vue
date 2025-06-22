<template>
  <div class="profile-container">
    <!-- 비밀번호 확인 단계 -->
    <div v-if="currentStep === 'verify'" class="verification-step">
      <div class="step-header">
        <button @click="goBack" class="back-button">
          <svg class="back-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M19 12H5M12 19L5 12L12 5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="header-info">
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
              <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
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
              <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
                <circle cx="12" cy="16" r="1" fill="currentColor"/>
                <path d="M7 11V7C7 5.67392 7.52678 4.40215 8.46447 3.46447C9.40215 2.52678 10.6739 2 12 2C13.3261 2 14.5979 2.52678 15.5355 3.46447C16.4732 4.40215 17 5.67392 17 7V11" stroke="currentColor" stroke-width="2"/>
              </svg>
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
                <svg v-if="showCurrentPassword" class="eye-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M14.12 14.12C13.8454 14.4148 13.5141 14.6512 13.1462 14.8151C12.7782 14.9791 12.3809 15.0673 11.9781 15.0744C11.5753 15.0815 11.1752 15.0074 10.8016 14.8565C10.4281 14.7056 10.0887 14.4811 9.80385 14.1962C9.51897 13.9113 9.29439 13.5719 9.14351 13.1984C8.99262 12.8248 8.91853 12.4247 8.92563 12.0219C8.93274 11.6191 9.02091 11.2218 9.18488 10.8538C9.34884 10.4859 9.58525 10.1546 9.88 9.88" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M9.9 4.24C10.5883 4.0789 11.2931 3.99836 12 4C19 4 23 12 23 12C22.393 13.1356 21.6691 14.2048 20.84 15.19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M3.16 3.16L20.84 20.84" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M12.9 12.9C12.8181 13.7611 12.3944 14.5519 11.7272 15.1027C11.0600 15.6535 10.2008 15.9181 9.33 15.84" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else class="eye-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M1 12S5 4 12 4S23 12 23 12S19 20 12 20S1 12 1 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                </svg>
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
          <svg class="back-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M19 12H5M12 19L5 12L12 5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="header-info">
          <svg class="header-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V18C2 18.5304 2.21071 19.0391 2.58579 19.4142C2.96086 19.7893 3.46957 20 4 20H16C16.5304 20 17.0391 19.7893 17.4142 19.4142C17.7893 19.0391 18 18.5304 18 18V11" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M18.5 2.50023C18.8978 2.10243 19.4374 1.87891 20 1.87891C20.5626 1.87891 21.1022 2.10243 21.5 2.50023C21.8978 2.89804 22.1213 3.43762 22.1213 4.00023C22.1213 4.56284 21.8978 5.10243 21.5 5.50023L12 15.0002L8 16.0002L9 12.0002L18.5 2.50023Z" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
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
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
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
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
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
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M4 4H20C21.1 4 22 4.9 22 6V18C22 19.1 21.1 20 20 20H4C2.9 20 2 19.1 2 18V6C2 4.9 2.9 4 4 4Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <polyline points="22,6 12,13 2,6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
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
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M22 16.92V19.92C22.0011 20.1985 21.9441 20.4742 21.8325 20.7293C21.7209 20.9845 21.5573 21.2136 21.3521 21.4019C21.1468 21.5901 20.9046 21.7335 20.6407 21.8227C20.3769 21.9119 20.0974 21.9451 19.82 21.92C16.7428 21.5856 13.787 20.5341 11.19 18.85C8.77382 17.3147 6.72533 15.2662 5.18999 12.85C3.49997 10.2412 2.44824 7.27099 2.11999 4.18C2.095 3.90347 2.12787 3.62476 2.21649 3.36162C2.30512 3.09849 2.44756 2.85669 2.63476 2.65162C2.82196 2.44655 3.0498 2.28271 3.30379 2.17052C3.55777 2.05833 3.83233 2.00026 4.10999 2H7.10999C7.59531 1.99522 8.06606 2.16708 8.43376 2.48353C8.80145 2.79999 9.04229 3.23945 9.10999 3.72C9.23662 4.68007 9.47144 5.62273 9.80999 6.53C9.94454 6.88792 9.97366 7.27691 9.8939 7.65088C9.81415 8.02485 9.62886 8.36811 9.35999 8.64L8.08999 9.91C9.51355 12.4135 11.5865 14.4864 14.09 15.91L15.36 14.64C15.6319 14.3711 15.9751 14.1858 16.3491 14.1061C16.7231 14.0263 17.1121 14.0555 17.47 14.19C18.3773 14.5286 19.3199 14.7634 20.28 14.89C20.7658 14.9585 21.2094 15.2032 21.5265 15.5775C21.8437 15.9518 22.0122 16.4296 22 16.92Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
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
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
                  <line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" stroke-width="2"/>
                  <line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" stroke-width="2"/>
                  <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/>
                </svg>
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
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                성별
              </label>
              <div class="gender-toggle">
                <label :class="{ active: userInfo.gender === 'M' }">
                  <input type="radio" value="M" v-model="userInfo.gender">
                  <span>남자</span>
                </label>
                <label :class="{ active: userInfo.gender === 'F' }">
                  <input type="radio" value="F" v-model="userInfo.gender">
                  <span>여자</span>
                </label>
                <label :class="{ active: userInfo.gender === 'U' }">
                  <input type="radio" value="U" v-model="userInfo.gender">
                  <span>선택 안 함</span>
                </label>
              </div>
            </div>

            <div class="form-group full-width password-section">
              <h3 class="password-section-title">비밀번호 변경 (선택사항)</h3>

              <div class="password-grid">
                <div class="form-group">
                  <label class="form-label">
                    <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
                      <circle cx="12" cy="16" r="1" fill="currentColor"/>
                      <path d="M7 11V7C7 5.67392 7.52678 4.40215 8.46447 3.46447C9.40215 2.52678 10.6739 2 12 2C13.3261 2 14.5979 2.52678 15.5355 3.46447C16.4732 4.40215 17 5.67392 17 7V11" stroke="currentColor" stroke-width="2"/>
                    </svg>
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
                      <svg v-if="showPasswords.new" class="eye-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M14.12 14.12C13.8454 14.4148 13.5141 14.6512 13.1462 14.8151C12.7782 14.9791 12.3809 15.0673 11.9781 15.0744C11.5753 15.0815 11.1752 15.0074 10.8016 14.8565C10.4281 14.7056 10.0887 14.4811 9.80385 14.1962C9.51897 13.9113 9.29439 13.5719 9.14351 13.1984C8.99262 12.8248 8.91853 12.4247 8.92563 12.0219C8.93274 11.6191 9.02091 11.2218 9.18488 10.8538C9.34884 10.4859 9.58525 10.1546 9.88 9.88" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M9.9 4.24C10.5883 4.0789 11.2931 3.99836 12 4C19 4 23 12 23 12C22.393 13.1356 21.6691 14.2048 20.84 15.19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M3.16 3.16L20.84 20.84" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M12.9 12.9C12.8181 13.7611 12.3944 14.5519 11.7272 15.1027C11.0600 15.6535 10.2008 15.9181 9.33 15.84" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                      <svg v-else class="eye-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M1 12S5 4 12 4S23 12 23 12S19 20 12 20S1 12 1 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                      </svg>
                    </button>
                  </div>
                  <div class="form-help">8자 이상, 영문/숫자/특수문자 조합</div>
                </div>

                <div class="form-group">
                  <label class="form-label">
                    <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
                      <circle cx="12" cy="16" r="1" fill="currentColor"/>
                      <path d="M7 11V7C7 5.67392 7.52678 4.40215 8.46447 3.46447C9.40215 2.52678 10.6739 2 12 2C13.3261 2 14.5979 2.52678 15.5355 3.46447C16.4732 4.40215 17 5.67392 17 7V11" stroke="currentColor" stroke-width="2"/>
                    </svg>
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
                      <svg v-if="showPasswords.confirm" class="eye-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M14.12 14.12C13.8454 14.4148 13.5141 14.6512 13.1462 14.8151C12.7782 14.9791 12.3809 15.0673 11.9781 15.0744C11.5753 15.0815 11.1752 15.0074 10.8016 14.8565C10.4281 14.7056 10.0887 14.4811 9.80385 14.1962C9.51897 13.9113 9.29439 13.5719 9.14351 13.1984C8.99262 12.8248 8.91853 12.4247 8.92563 12.0219C8.93274 11.6191 9.02091 11.2218 9.18488 10.8538C9.34884 10.4859 9.58525 10.1546 9.88 9.88" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M9.9 4.24C10.5883 4.0789 11.2931 3.99836 12 4C19 4 23 12 23 12C22.393 13.1356 21.6691 14.2048 20.84 15.19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M3.16 3.16L20.84 20.84" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M12.9 12.9C12.8181 13.7611 12.3944 14.5519 11.7272 15.1027C11.0600 15.6535 10.2008 15.9181 9.33 15.84" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                      <svg v-else class="eye-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M1 12S5 4 12 4S23 12 23 12S19 20 12 20S1 12 1 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                      </svg>
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
            <div class="withdraw-group">
              <button
                  type="button"
                  @click="showWithdrawModal"
                  class="withdraw-button action-button"
              >
                <svg class="button-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M3 6H5H21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M8 6V4C8 3.46957 8.21071 2.96086 8.58579 2.58579C8.96086 2.21071 9.46957 2 10 2H14C14.5304 2 15.0391 2.21071 15.4142 2.58579C15.7893 2.96086 16 3.46957 16 4V6M19 6V20C19 20.5304 18.7893 21.0391 18.4142 21.4142C18.0391 21.7893 17.5304 22 17 22H7C6.46957 22 5.96086 21.7893 5.58579 21.4142C5.21071 21.0391 5 20.5304 5 20V6H19Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <line x1="10" y1="11" x2="10" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  <line x1="14" y1="11" x2="14" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                회원탈퇴
              </button>
            </div>
            <div class="button-group">
              <button
                  type="submit"
                  :disabled="saving"
                  class="save-button action-button"
              >
                <svg class="button-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H16L21 8V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <polyline points="17,21 17,13 7,13 7,21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <polyline points="7,3 7,8 15,8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <div v-if="saving" class="mini-spinner"></div>
                {{ saving ? '저장 중...' : '저장' }}
              </button>
              <button
                  type="button"
                  @click="cancelEdit"
                  class="cancel-button action-button"
              >
                <svg class="button-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/>
                  <line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/>
                </svg>
                취소
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
    <div v-if="showWithdrawConfirm" class="modal-overlay" @click="closeWithdrawModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">회원탈퇴</h3>
          <button @click="closeWithdrawModal" class="modal-close">
            <svg class="close-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/>
              <line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/>
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div class="warning-section">
            <div class="warning-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 9V13M12 17H12.01M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="warning-content">
              <h4 class="warning-title">정말로 탈퇴하시겠습니까?</h4>
              <p class="warning-text">
                탈퇴 시 모든 개인정보와 이용 기록이 삭제되며, 복구할 수 없습니다.<br>
                신중하게 결정해 주세요.
              </p>
            </div>
          </div>

          <form @submit.prevent="executeWithdrawal" class="withdrawal-form">
            <div class="form-group">
              <label class="form-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="16" r="1" fill="currentColor"/>
                  <path d="M7 11V7C7 5.67392 7.52678 4.40215 8.46447 3.46447C9.40215 2.52678 10.6739 2 12 2C13.3261 2 14.5979 2.52678 15.5355 3.46447C16.4732 4.40215 17 5.67392 17 7V11" stroke="currentColor" stroke-width="2"/>
                </svg>
                비밀번호 확인 <span class="required">*</span>
              </label>
              <input
                  type="password"
                  v-model="withdrawalForm.password"
                  placeholder="현재 비밀번호를 입력해주세요"
                  class="form-input"
                  required
              />
            </div>

            <div class="form-group">
              <label class="form-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M8.5 14.5L11 16.5L15.5 12M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                탈퇴 사유 <span class="required">*</span>
              </label>
              <select v-model="withdrawalForm.reason" class="form-select" required>
                <option value="">탈퇴 사유를 선택해주세요</option>
                <option v-for="reason in withdrawalReasons" :key="reason" :value="reason">
                  {{ reason }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                확인 문구 입력 <span class="required">*</span>
              </label>
              <input
                  type="text"
                  v-model="withdrawalForm.confirmText"
                  placeholder="'회원탈퇴'를 정확히 입력해주세요"
                  class="form-input"
                  :class="{ 'error': withdrawalForm.confirmText && !isWithdrawConfirmValid }"
                  required
              />
              <div class="form-help">
                탈퇴를 확인하시려면 <strong>'회원탈퇴'</strong>를 정확히 입력해주세요.
              </div>
            </div>

            <div v-if="withdrawalError" class="error-alert">
              {{ withdrawalError }}
            </div>

            <div class="modal-actions">
              <button
                  type="button"
                  @click="closeWithdrawModal"
                  class="cancel-button"
                  :disabled="withdrawing"
              >
                취소
              </button>
              <button
                  type="submit"
                  class="danger-button"
                  :disabled="withdrawing || !isWithdrawConfirmValid"
              >
                <div v-if="withdrawing" class="mini-spinner"></div>
                {{ withdrawing ? '처리 중...' : '탈퇴하기' }}
              </button>
            </div>
          </form>
        </div>
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

// 토큰 가져오기 함수
const getToken = () => {
  return localStorage.getItem('token')
}

// JWT 토큰에서 userId 추출하는 함수
const getUserIdFromToken = (token) => {
  try {
    if (!token) return null

    const payload = token.split('.')[1]
    if (!payload) return null

    const decodedPayload = JSON.parse(atob(payload))

    // username을 우선적으로 사용
    return decodedPayload.username || decodedPayload.sub || decodedPayload.userId || decodedPayload.user_id
  } catch (error) {
    return null
  }
}

// 현재 사용자 ID 가져오기 (여러 소스에서 시도)
const getCurrentUserId = () => {
  // 1. userStore에서 시도
  if (computedUser.value?.id) return computedUser.value.id
  if (computedUser.value?.userId) return computedUser.value.userId
  if (computedUser.value?.username) return computedUser.value.username

  // 2. localStorage에서 직접 시도
  const storedUserId = localStorage.getItem('userId')
  if (storedUserId) return storedUserId

  // 3. JWT 토큰에서 username 추출 (sub 대신)
  const token = getToken()
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      const username = payload.username  // ← sub 대신 username 사용
      if (username) {
        localStorage.setItem('userId', username)
        return username
      }
    } catch (error) {
      console.error('토큰 파싱 오류:', error)
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

  // 토큰과 userId 확인
  const token = getToken()
  const userId = getCurrentUserId()

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

    const url = `${API_BASE_URL}/api/users/verify-password`

    const response = await fetch(url, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(requestBody)
    })

    if (response.ok) {
      currentStep.value = 'edit'
      currentPassword.value = ''
      passwordError.value = ''
      await loadUserProfile()
    } else {
      const errorData = await response.json()
      passwordError.value = errorData.message || '비밀번호가 일치하지 않습니다.'
    }
  } catch (err) {
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

// 컴포넌트 마운트 시 실행 - 토큰 검증 및 userId 설정
onMounted(() => {
  const token = getToken()

  if (token) {
    // userStore 업데이트
    setUserFromToken(token)

    // userId 확인 및 설정
    const userId = getUserIdFromToken(token)
    if (userId) {
      localStorage.setItem('userId', userId)
    }
  } else {
    router.push('/login')
  }
})

// 회원탈퇴 관련 상태 추가
const showWithdrawConfirm = ref(false)
const withdrawalForm = ref({
  password: '',
  reason: '',
  confirmText: '',
  withdrawalDate: new Date().toISOString().split('T')[0] // 오늘 날짜
})
const withdrawing = ref(false)
const withdrawalError = ref('')

// 회원탈퇴 사유 옵션
const withdrawalReasons = [
  '서비스 불만족',
  '이용 빈도 낮음',
  '개인정보 보호',
  '타 서비스 이용',
  '기타'
]

// 회원탈퇴 모달 표시
const showWithdrawModal = () => {
  // 폼 초기화
  withdrawalForm.value = {
    password: '',
    reason: '',
    confirmText: '',
    withdrawalDate: new Date().toISOString().split('T')[0]
  }
  withdrawalError.value = ''
  showWithdrawConfirm.value = true
}

// 회원탈퇴 모달 닫기
const closeWithdrawModal = () => {
  showWithdrawConfirm.value = false
  withdrawalForm.value = {
    password: '',
    reason: '',
    confirmText: '',
    withdrawalDate: new Date().toISOString().split('T')[0]
  }
  withdrawalError.value = ''
}

// 회원탈퇴 확인 텍스트 검증
const isWithdrawConfirmValid = computed(() => {
  return withdrawalForm.value.confirmText === '회원탈퇴'
})

// 회원탈퇴 실행
// 회원탈퇴 실행 함수 - JSON 오류 수정 버전
const executeWithdrawal = async () => {
  // 유효성 검사
  if (!withdrawalForm.value.password) {
    withdrawalError.value = '비밀번호를 입력해주세요.'
    return
  }

  if (!withdrawalForm.value.reason) {
    withdrawalError.value = '탈퇴 사유를 선택해주세요.'
    return
  }

  if (!isWithdrawConfirmValid.value) {
    withdrawalError.value = '확인 텍스트를 정확히 입력해주세요.'
    return
  }

  // 최종 확인
  if (!confirm('정말로 회원탈퇴를 진행하시겠습니까?\n탈퇴 후에는 계정 복구가 불가능합니다.')) {
    return
  }

  withdrawing.value = true
  withdrawalError.value = ''

  try {
    // 토큰 재확인
    const token = getToken()
    if (!token) {
      throw new Error('인증 토큰이 없습니다. 다시 로그인해주세요.')
    }

    const userId = getCurrentUserId()
    if (!userId) {
      throw new Error('사용자 정보를 찾을 수 없습니다.')
    }

    const requestData = {
      userId: userId,
      password: withdrawalForm.value.password,
      withdrawalReason: withdrawalForm.value.reason,
      withdrawalDate: withdrawalForm.value.withdrawalDate
    }
    const headers = getAuthHeaders()
    console.log('실제 전송할 헤더:', headers)

// 특히 Authorization 헤더 확인
    console.log('Authorization 헤더:', headers.Authorization)
    console.log('회원탈퇴 요청 데이터:', requestData)
    console.log('사용할 토큰:', token ? 'token exists' : 'no token')

    const response = await fetch(`${API_BASE_URL}/api/users/withdraw`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(requestData)
    })

    console.log('응답 상태:', response.status)
    console.log('응답 헤더:', response.headers)

    // 🔥 응답 처리 개선: JSON 파싱 전에 응답 내용 확인
    const contentType = response.headers.get('content-type')
    console.log('Content-Type:', contentType)

    let result = null

    // Content-Type이 JSON인지 확인하고, 응답 본문이 있는지 확인
    if (contentType && contentType.includes('application/json')) {
      const responseText = await response.text()
      console.log('응답 본문:', responseText)

      if (responseText.trim()) {
        try {
          result = JSON.parse(responseText)
        } catch (parseError) {
          console.error('JSON 파싱 오류:', parseError)
          throw new Error('서버 응답을 처리할 수 없습니다.')
        }
      } else {
        console.log('빈 응답 본문')
        result = { success: response.ok }
      }
    } else {
      // JSON이 아닌 응답인 경우
      const responseText = await response.text()
      console.log('비-JSON 응답:', responseText)
      result = {
        success: response.ok,
        message: responseText || '응답 없음'
      }
    }

    if (response.ok) {
      // 성공 처리 - result가 null이어도 OK
      alert('회원탈퇴가 정상적으로 처리되었습니다.\n그동안 이용해 주셔서 감사합니다.')

      // 로컬 스토리지 정리
      localStorage.removeItem('token')
      localStorage.removeItem('userId')

      // 로그인 페이지로 이동
      router.push('/login')
    } else {
      // 오류 처리
      const errorMessage = result?.message || `서버 오류 (${response.status})`
      throw new Error(errorMessage)
    }

  } catch (error) {
    console.error('회원탈퇴 오류:', error)

    // 네트워크 오류나 기타 오류 처리
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      withdrawalError.value = '네트워크 연결에 문제가 있습니다. 다시 시도해주세요.'
    } else {
      withdrawalError.value = error.message || '회원탈퇴 처리 중 오류가 발생했습니다.'
    }
  } finally {
    withdrawing.value = false
  }
}
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

.back-icon {
  width: 20px;
  height: 20px;
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
  padding: 8px;
  border-radius: 10px;
  background: #dbeafe;
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

.eye-icon {
  width: 20px;
  height: 20px;
}

.gender-toggle {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.gender-toggle label {
  display: flex;
  align-items: center;
  flex-direction: row;
  padding: 8px 16px;
  border: 2px solid #e9ecef;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
  background-color: white;
  font-size: 14px;
  white-space: nowrap;
}

.gender-toggle label:hover {
  border-color: #007bff;
  background-color: #f8f9fa;
  width: 30%;
}

.gender-toggle label.active {
  border-color: #007bff;
  background-color: #007bff;
  color: white;
  width: 20%;
}

.gender-toggle input[type="radio"] {
  width: 20%;
  margin-right: 8px;
  margin-bottom: 0;
  flex-shrink: 0;
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
  justify-content: space-between;
  align-items: center !important;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;
  min-height: 48px;
}

.button-group,
.withdraw-group {
  display: flex;
  align-items: center !important;
  gap: 16px;
  height: 48px;
  margin-top: 24px;
}

/* 모든 버튼 공통 기본 스타일 - 완전히 동일한 크기 */
.action-button,
.verify-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px !important;
  padding: 0 24px !important;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid;
  box-sizing: border-box;
  line-height: 1;
  margin: 0 !important;
  min-height: unset;
  max-height: unset;
  vertical-align: middle;
  position: relative;
  top: 0;
  bottom: 0;
}

/* 회원탈퇴 버튼 */
.withdraw-button {
  background: #fff5f5;
  color: #dc2626;
  border-color: #dc2626;
}

.withdraw-button:hover {
  background: #dc2626;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
}

/* 저장 버튼 */
.save-button {
  background: #dbeafe;
  color: #3b82f6;
  border-color: #3b82f6;
}

.save-button:hover {
  background: #3b82f6;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.save-button:disabled {
  background: #f3f4f6 !important;
  color: #9ca3af !important;
  border-color: #d1d5db !important;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 취소 버튼 */
.cancel-button {
  background: #f9fafb;
  color: #6b7280;
  border-color: #d1d5db;
}

.cancel-button:hover {
  background: #f3f4f6;
  color: #374151;
  border-color: #9ca3af;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 확인 버튼 */
.verify-button {
  background: #dbeafe;
  color: #3b82f6;
  border-color: #3b82f6;
}

.verify-button:hover {
  background: #3b82f6;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.verify-button:disabled {
  background: #f3f4f6 !important;
  color: #9ca3af !important;
  border-color: #d1d5db !important;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.button-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.mini-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  flex-shrink: 0;
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
    align-items: stretch;
  }

  .button-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }

  .action-button {
    width: 100%;
    justify-content: center;
  }

  .withdraw-button {
    order: -1; /* 모바일에서 맨 위로 */
  }

  .password-section {
    padding: 20px;
  }
}
/* 모달 기본 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0 24px;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 24px;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.modal-close:hover {
  background: #f3f4f6;
  color: #374151;
}

.close-icon {
  width: 20px;
  height: 20px;
}

.modal-body {
  padding: 0 24px 24px 24px;
}

/* 경고 섹션 */
.warning-section {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
  margin-bottom: 24px;
}

.warning-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  background: #fee2e2;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #dc2626;
}

.warning-icon svg {
  width: 20px;
  height: 20px;
}

.warning-content {
  flex: 1;
}

.warning-title {
  font-size: 16px;
  font-weight: 600;
  color: #dc2626;
  margin: 0 0 8px 0;
}

.warning-text {
  font-size: 14px;
  color: #7f1d1d;
  margin: 0;
  line-height: 1.5;
}

/* 폼 스타일 */
.withdrawal-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
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

.form-input,
.form-select {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  background: white;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input.error {
  border-color: #dc2626;
}

.form-input.error:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.form-help {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.error-alert {
  padding: 12px 16px;
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}

/* 모달 액션 버튼 */
.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

.modal-actions button {
  flex: 1;
  height: 48px;
  padding: 0 24px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 2px solid;
}

.cancel-button {
  background: #f9fafb;
  color: #6b7280;
  border-color: #d1d5db;
}

.cancel-button:hover:not(:disabled) {
  background: #f3f4f6;
  color: #374151;
  border-color: #9ca3af;
}

.danger-button {
  background: #dc2626;
  color: white;
  border-color: #dc2626;
}

.danger-button:hover:not(:disabled) {
  background: #b91c1c;
  border-color: #b91c1c;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
}

.danger-button:disabled {
  background: #f3f4f6 !important;
  color: #9ca3af !important;
  border-color: #d1d5db !important;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.mini-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 모바일 대응 */
@media (max-width: 768px) {
  .modal-overlay {
    padding: 10px;
  }

  .modal-content {
    max-height: 95vh;
  }

  .modal-header,
  .modal-body {
    padding-left: 20px;
    padding-right: 20px;
  }

  .warning-section {
    flex-direction: column;
    text-align: center;
  }

  .modal-actions {
    flex-direction: column;
  }
}
</style>
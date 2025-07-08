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
<!--                <label :class="{ active: userInfo.gender === 'U' }">-->
<!--                  <input type="radio" value="U" v-model="userInfo.gender">-->
<!--                  <span>선택 안 함</span>-->
<!--                </label>-->
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
import { user, setUserFromToken, resetUser } from '@/stores/userStore'

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
  return localStorage.getItem('jwt')
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
      // 토큰 파싱 오류 발생시 처리
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
        localStorage.removeItem('jwt')
        localStorage.removeItem('userId')
        router.push('/login')
        return
      }
      throw new Error('사용자 정보를 불러올 수 없습니다.')
    }

    const responseData = await response.json()
    const userData = responseData.data

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
  withdrawalDate: new Date().toISOString().split('T')[0]
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
// 회원탈퇴 실행 함수 - 완전한 버전
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

    // 서버가 기대하는 정확한 요청 구조
    const requestData = {
      userId: userId,
      password: withdrawalForm.value.password,
      withdrawalReason: withdrawalForm.value.reason,
      withdrawalDate: withdrawalForm.value.withdrawalDate
    }

    const apiUrl = `${API_BASE_URL}/api/users/withdrawal/process`

    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(requestData)
    })

    // 응답 처리
    const contentType = response.headers.get('content-type')
    let result = null

    if (contentType && contentType.includes('application/json')) {
      const responseText = await response.text()

      if (responseText.trim()) {
        try {
          result = JSON.parse(responseText)
        } catch (parseError) {
          throw new Error('서버 응답을 처리할 수 없습니다.')
        }
      } else {
        result = { success: response.ok }
      }
    } else {
      const responseText = await response.text()
      result = {
        success: response.ok,
        message: responseText || '응답 없음'
      }
    }

    if (response.ok) {

      // 1. userStore의 완전 초기화 함수 호출
      resetUser()

      // 2. 현재 컴포넌트 상태 초기화
      userInfo.value = {
        username: '',
        name: '',
        email: '',
        phone: '',
        birthDate: '',
        gender: 'U',
        newPassword: '',
        confirmNewPassword: ''
      }

      // 3. 폼 상태 초기화
      withdrawalForm.value = {
        password: '',
        reason: '',
        confirmText: '',
        withdrawalDate: new Date().toISOString().split('T')[0]
      }

      // 4. 모든 단계 초기화
      currentStep.value = 'verify'
      currentPassword.value = ''
      passwordError.value = ''

      // 5. 모달 닫기
      showWithdrawConfirm.value = false

      // 6. 성공 알림
      alert('회원탈퇴가 정상적으로 처리되었습니다.\n그동안 이용해 주셔서 감사합니다.')

      // 7. 로그인 페이지로 강제 이동 (완전 초기화)
      window.location.href = '/login'

    } else {

      if (response.status === 404) {
        throw new Error('회원탈퇴 API를 찾을 수 없습니다. 서버 설정을 확인해주세요.')
      } else if (response.status === 401) {
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.')
      } else if (response.status === 403) {
        throw new Error('권한이 없습니다.')
      } else {
        const errorMessage = result?.message || `서버 오류 (${response.status})`
        throw new Error(errorMessage)
      }
    }

  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      withdrawalError.value = '네트워크 연결에 문제가 있습니다. 서버가 실행 중인지 확인해주세요.'
    } else {
      withdrawalError.value = error.message || '회원탈퇴 처리 중 오류가 발생했습니다.'
    }
  } finally {
    withdrawing.value = false
  }
}
</script>

<style scoped src="@/assets/css/myPageFile.css"></style>

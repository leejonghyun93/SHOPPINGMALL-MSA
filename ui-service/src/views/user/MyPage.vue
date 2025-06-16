<template>
  <div class="mypage-container">
    <div class="container-fluid">
      <div class="row">
        <!-- 왼쪽 사이드바 -->
        <div class="col-md-3 sidebar">
          <!-- 사용자 정보 섹션 -->
          <div class="user-info-section">
            <div class="welcome-text">반가워요! <span class="username">{{ userName }}</span></div>

            <div class="user-stats">
              <div class="stat-item">
                <div class="stat-label">적립금</div>
                <div class="stat-value">{{ (points || 0).toLocaleString() }}원</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">쿠폰수</div>
                <div class="stat-value">{{ coupons || 0 }}개</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">상품권</div>
                <div class="stat-value">{{ giftCards || 0 }}개</div>
              </div>
            </div>
          </div>

          <!-- 메뉴 섹션 -->
          <div class="menu-section">
            <div class="menu-category" @click="setActiveTab('orders')" :class="{ active: activeTab === 'orders' }">
              <div class="menu-header">
                <i class="fas fa-box text-primary"></i>
                <span>주문 내역</span>
                <span class="menu-count">1</span>
              </div>
            </div>

            <div class="menu-category" @click="setActiveTab('coupons')" :class="{ active: activeTab === 'coupons' }">
              <div class="menu-header">
                <i class="fas fa-ticket-alt text-success"></i>
                <span>쿠폰</span>
                <span class="menu-count">0</span>
              </div>
            </div>

            <div class="menu-category" @click="setActiveTab('wishlist')" :class="{ active: activeTab === 'wishlist' }">
              <div class="menu-header">
                <i class="fas fa-heart text-danger"></i>
                <span>찜한 상품</span>
                <span class="menu-count">0</span>
              </div>
            </div>

            <div class="menu-category" @click="setActiveTab('frequent')" :class="{ active: activeTab === 'frequent' }">
              <div class="menu-header">
                <i class="fas fa-shopping-cart text-warning"></i>
                <span>자주 구매</span>
              </div>
            </div>

            <!-- 소셜 섹션 -->
            <div class="social-section">
              <h6>소셜</h6>
              <ul class="social-menu">
                <li><a href="#" @click="setActiveTab('returns')">취소 · 반품 내역</a></li>
                <li><a href="#" @click="setActiveTab('reviews')">상품 후기</a></li>
                <li><a href="#" @click="setActiveTab('inquiries')">상품 문의</a></li>
              </ul>
            </div>

            <!-- 내 정보관리 섹션 -->
            <div class="profile-section">
              <h6>내 정보관리</h6>
              <ul class="profile-menu">
                <li><a href="#" @click="setActiveTab('shipping')" :class="{ active: activeTab === 'shipping' }">배송지 관리</a></li>
                <li><a href="#" @click="setActiveTab('colorstyle')" :class="{ active: activeTab === 'colorstyle' }">나의 컬러스타일</a></li>
                <li><a href="#" @click="setActiveTab('profile')" :class="{ active: activeTab === 'profile' }">회원 정보 관리</a></li>
                <li><a href="#" @click="setActiveTab('vip')" :class="{ active: activeTab === 'vip' }">VIP 예상 등급</a></li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 오른쪽 메인 컨텐츠 -->
        <div class="col-md-9 main-content">
          <!-- 주문 내역 탭 -->
          <div v-if="activeTab === 'orders'" class="tab-content">
            <div class="content-header">
              <h4>주문 내역</h4>
              <div class="filter-section">
                <select class="form-select filter-select">
                  <option>3개월</option>
                  <option>6개월</option>
                  <option>1년</option>
                </select>
                <div class="search-box">
                  <input type="text" class="form-control" placeholder="상품명으로 검색해보세요">
                  <i class="fas fa-search search-icon"></i>
                </div>
              </div>
            </div>

            <!-- 주문 내역 카드 -->
            <div class="order-card">
              <div class="order-header">
                <div class="order-date">2025.06.08</div>
                <div class="order-number">주문번호 23B1204902043</div>
                <i class="fas fa-chevron-right"></i>
              </div>

              <div class="delivery-info">
                <span class="delivery-status">배송완료</span>
                <span class="delivery-date">6월(일) 03:13</span>
              </div>

              <div class="product-info">
                <div class="product-image">
                  <img src="https://via.placeholder.com/80x80" alt="상품 이미지">
                </div>
                <div class="product-details">
                  <div class="product-category">생활내장</div>
                  <div class="product-name">[로그리더슬] 비키스 (250mL X 6개)</div>
                  <div class="product-price">
                    <span class="price">4,480원</span>
                    <span class="quantity">1개</span>
                  </div>
                </div>
                <div class="product-actions">
                  <i class="fas fa-shopping-cart"></i>
                </div>
              </div>

              <div class="order-actions">
                <button class="btn btn-outline-secondary">반품접수</button>
                <button class="btn btn-primary">후기작성</button>
              </div>
            </div>
          </div>

          <!-- 회원 정보 관리 탭 -->
          <div v-if="activeTab === 'profile'" class="tab-content">
            <div class="content-header">
              <h4>회원 정보 관리</h4>
            </div>
            <div class="profile-management">
              <div class="info-section">
                <h5>개인정보</h5>
                <div class="info-item" @click="setActiveTab('basic-info')">
                  <span>개인 정보 수정</span>
                  <i class="fas fa-chevron-right arrow-icon"></i>
                </div>
              </div>

              <div class="info-section">
                <h5>보안관리</h5>
                <div class="info-item" @click="handleLoginManagement">
                  <span>로그인 관리</span>
                  <i class="fas fa-chevron-right arrow-icon"></i>
                </div>

                <div class="info-item">
                  <span>해외 지역 로그인 차단</span>
                  <div class="form-check form-switch">
                    <input class="form-check-input" type="checkbox" checked>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 개인정보 수정 탭 -->
          <div v-if="activeTab === 'basic-info'" class="tab-content">
            <div class="content-header">
              <h4>개인정보 수정</h4>
            </div>

            <!-- 비밀번호 확인 단계 -->
            <div v-if="!passwordVerified" class="personal-info-form">
              <div class="form-section">
                <h5>비밀번호 재확인</h5>
                <p class="text-muted">회원님의 정보를 안전하게 보호하기 위해 비밀번호를 다시 한번 확인해주세요.</p>

                <div class="mb-3">
                  <label class="form-label">아이디</label>
                  <input type="text" class="form-control" :value="computedUser.username || computedUser.id || ''" readonly>
                </div>

                <div class="mb-3">
                  <label class="form-label">비밀번호 <span class="text-danger">*</span></label>
                  <input type="password" class="form-control" placeholder="현재 비밀번호를 입력해주세요" v-model="currentPassword">
                </div>

                <div v-if="passwordError" class="alert alert-danger">
                  {{ passwordError }}
                </div>

                <div class="text-center">
                  <button class="btn btn-primary px-5" @click="verifyPassword" :disabled="verifying">
                    {{ verifying ? '확인 중...' : '확인' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- 개인정보 수정 폼 -->
            <div v-else class="personal-info-form">
              <div class="form-section">
                <h5>개인정보 수정</h5>

                <div class="mb-3">
                  <label class="form-label">아이디</label>
                  <input type="text" class="form-control" :value="userInfo.username" readonly>
                </div>

                <div class="mb-3">
                  <label class="form-label">이름 <span class="text-danger">*</span></label>
                  <input type="text" class="form-control" v-model="userInfo.name" placeholder="이름을 입력하세요">
                </div>

                <div class="mb-3">
                  <label class="form-label">이메일 <span class="text-danger">*</span></label>
                  <input type="email" class="form-control" v-model="userInfo.email" placeholder="이메일을 입력하세요">
                </div>

                <div class="mb-3">
                  <label class="form-label">휴대폰 번호</label>
                  <input type="tel" class="form-control" v-model="userInfo.phone" placeholder="010-1234-5678">
                </div>

                <!-- 🔥 새 비밀번호 섹션 추가 -->
                <div class="password-section">
                  <h6 class="password-section-title">비밀번호 변경 (선택사항)</h6>

                  <div class="mb-3">
                    <label class="form-label">새 비밀번호</label>
                    <input type="password" class="form-control" v-model="userInfo.newPassword" placeholder="새 비밀번호 (변경시에만 입력)">
                    <div class="form-text">8자 이상, 영문/숫자/특수문자 조합</div>
                  </div>

                  <div class="mb-3">
                    <label class="form-label">새 비밀번호 확인</label>
                    <input type="password" class="form-control" v-model="userInfo.confirmNewPassword" placeholder="새 비밀번호 확인">
                    <div v-if="passwordMismatch" class="text-danger mt-1">새 비밀번호가 일치하지 않습니다.</div>
                  </div>
                </div>

                <!-- 🔥 생년월일 추가 -->
                <div class="mb-3">
                  <label class="form-label">생년월일</label>
                  <input type="date" class="form-control" v-model="userInfo.birthDate">
                </div>

                <!-- 🔥 성별 추가 -->
                <div class="mb-3">
                  <label class="form-label">성별</label>
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

                <div class="text-center">
                  <button class="btn btn-secondary me-2" @click="cancelEdit">취소</button>
                  <button class="btn btn-primary px-4" @click="updateUserInfo" :disabled="updating">
                    {{ updating ? '저장 중...' : '저장' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 기타 탭들 플레이스홀더 -->
          <div v-if="!['orders', 'profile', 'basic-info'].includes(activeTab)" class="tab-content">
            <div class="content-header">
              <h4>{{ getTabTitle() }}</h4>
            </div>
            <div class="placeholder-content">
              <div class="text-center text-muted py-5">
                <i class="fas fa-tools fa-3x mb-3"></i>
                <p>{{ getTabTitle() }} 기능은 준비 중입니다.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { user, setUserFromToken } from '@/stores/userStore'

const route = useRoute()

// 사용자 데이터 - userStore에서 가져오기
const computedUser = computed(() => user)
const userName = computed(() => {
  return computedUser.value.name ? computedUser.value.name + '님' : '사용자님'
})

// 활성 탭
const activeTab = ref('orders')

// 상태 관리
const passwordVerified = ref(false)
const verifying = ref(false)
const updating = ref(false)
const passwordError = ref('')

// 사이드바에 표시할 정보
const points = ref(12500)
const coupons = ref(3)
const giftCards = ref(1)

// 🔥 userInfo 객체에 새 필드들 추가
const userInfo = ref({
  username: '',
  name: '',
  email: '',
  phone: '',
  birthDate: '',
  gender: 'U',           // 🔥 성별 추가
  newPassword: '',       // 🔥 새 비밀번호 추가
  confirmNewPassword: '' // 🔥 새 비밀번호 확인 추가
})

// 폼 데이터
const currentPassword = ref('')

// 🔥 새 비밀번호 검증 computed 추가
const passwordMismatch = computed(() => {
  if (!userInfo.value.newPassword && !userInfo.value.confirmNewPassword) {
    return false // 둘 다 비어있으면 오류 없음
  }
  return userInfo.value.newPassword !== userInfo.value.confirmNewPassword
})

// 적립금, 쿠폰 등 추가 정보만 필요한 경우 별도 API 호출 (선택사항)
async function fetchUserExtraInfo() {
  try {
    const token = localStorage.getItem('token')
    if (!token) return
  } catch (error) {
    // 에러 발생 시 기본값 유지
  }
}

// 🔥 사용자 상세 정보 로드 함수 업데이트
async function loadUserDetailInfo() {
  try {
    const token = localStorage.getItem('token')
    if (!token) return

    const response = await fetch('/api/users/profile', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })

    if (response.ok) {
      const userData = await response.json()
      userInfo.value = {
        username: userData.userId,
        name: userData.name || '',
        email: userData.email || '',
        phone: userData.phone || '',
        birthDate: userData.birthDate || '',
        gender: userData.gender || 'U',  // 🔥 성별 추가
        newPassword: '',                 // 🔥 항상 빈 값으로 시작
        confirmNewPassword: ''           // 🔥 항상 빈 값으로 시작
      }
    } else {
      console.error('사용자 정보 로드 실패')
    }
  } catch (error) {
    console.error('사용자 정보 로드 중 오류:', error)
  }
}

// URL 파라미터에 따라 초기 탭 설정
onMounted(() => {
  // userStore에서 사용자 정보 설정 (헤더와 동일한 방식)
  const token = localStorage.getItem('token')
  if (token) {
    setUserFromToken(token)
  }

  // 추가 정보 가져오기 (적립금, 쿠폰 등)
  fetchUserExtraInfo()

  if (route.query.tab) {
    activeTab.value = route.query.tab
  }
  if (route.query.section === 'profile') {
    activeTab.value = 'profile'
  }
})

function setActiveTab(tab) {
  activeTab.value = tab
}

function getTabTitle() {
  const titles = {
    'orders': '주문 내역',
    'coupons': '쿠폰',
    'wishlist': '찜한 상품',
    'frequent': '자주 구매',
    'returns': '취소 · 반품 내역',
    'reviews': '상품 후기',
    'inquiries': '상품 문의',
    'shipping': '배송지 관리',
    'colorstyle': '나의 컬러스타일',
    'profile': '회원 정보 관리',
    'vip': 'VIP 예상 등급',
    'basic-info': '개인정보 수정'
  }
  return titles[activeTab.value] || '마이페이지'
}

// 폼 액션 함수들
async function verifyPassword() {
  if (!currentPassword.value) {
    passwordError.value = '비밀번호를 입력해주세요.'
    return
  }

  verifying.value = true
  passwordError.value = ''

  try {
    const token = localStorage.getItem('token')
    if (!token) {
      passwordError.value = '로그인이 필요합니다.'
      return
    }

    // 실제 API 호출로 비밀번호 검증
    const response = await fetch('/api/users/verify-password', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        password: currentPassword.value
      })
    })

    const result = await response.json()

    if (response.ok) {
      // 비밀번호 검증 성공
      passwordVerified.value = true
      await loadUserDetailInfo()
    } else {
      // 비밀번호 검증 실패
      passwordError.value = result.message || '비밀번호가 일치하지 않습니다.'
    }

  } catch (error) {
    console.error('비밀번호 검증 중 오류:', error)
    passwordError.value = '비밀번호 확인 중 오류가 발생했습니다.'
  } finally {
    verifying.value = false
  }
}

// 🔥 개인정보 수정 함수 업데이트
async function updateUserInfo() {
  // 🔥 필수 필드 검증
  if (!userInfo.value.name || !userInfo.value.email) {
    alert('이름과 이메일은 필수 항목입니다.')
    return
  }

  // 🔥 새 비밀번호 검증
  if (userInfo.value.newPassword && passwordMismatch.value) {
    alert('새 비밀번호가 일치하지 않습니다.')
    return
  }

  // 🔥 새 비밀번호 강도 검증
  if (userInfo.value.newPassword && userInfo.value.newPassword.length < 8) {
    alert('새 비밀번호는 8자 이상이어야 합니다.')
    return
  }

  updating.value = true

  try {
    const token = localStorage.getItem('token')
    if (!token) {
      alert('로그인이 필요합니다.')
      return
    }

    // 🔥 업데이트 데이터 구성 (새 비밀번호가 있을 때만 포함)
    const updateData = {
      name: userInfo.value.name,
      email: userInfo.value.email,
      phone: userInfo.value.phone,
      birthDate: userInfo.value.birthDate,
      gender: userInfo.value.gender
    }

    // 새 비밀번호가 입력된 경우에만 추가
    if (userInfo.value.newPassword) {
      updateData.password = userInfo.value.newPassword
    }

    const response = await fetch('/api/users/profile', {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updateData)
    })

    const result = await response.json()

    if (response.ok) {
      // userStore 업데이트
      user.name = userInfo.value.name
      user.email = userInfo.value.email

      // 🔥 비밀번호 필드 초기화
      userInfo.value.newPassword = ''
      userInfo.value.confirmNewPassword = ''

      alert('개인정보가 수정되었습니다.')
      activeTab.value = 'profile'
    } else {
      alert(result.message || '개인정보 수정에 실패했습니다.')
    }

  } catch (error) {
    console.error('개인정보 수정 중 오류:', error)
    alert('개인정보 수정 중 오류가 발생했습니다.')
  } finally {
    updating.value = false
  }
}

// 🔥 취소 함수 업데이트
function cancelEdit() {
  passwordVerified.value = false
  currentPassword.value = ''
  passwordError.value = ''

  // 🔥 새 비밀번호 필드들도 초기화
  userInfo.value.newPassword = ''
  userInfo.value.confirmNewPassword = ''

  activeTab.value = 'profile'
}

function handleLoginManagement() {
  // 로그인 관리 기능 처리
  alert('로그인 관리 기능은 준비 중입니다.')
}
</script>

<style scoped>
.mypage-container {
  background-color: #afafaf;
  padding: 20px 0;
}

/* 사이드바 */
.sidebar {
  background-color: transparent;
  padding: 0;
  padding-right: 15px;
  height: fit-content;
}

/* 사용자 정보 섹션 - 독립적인 카드 */
.user-info-section {
  background-color: white;
  padding: 20px;
  border: 1px solid #b3b3b3;
  margin-bottom: 15px;
  border-radius: 10px;
}

/* 메뉴 섹션 - 독립적인 카드 */
.menu-section {
  background-color: white;
  border: 1px solid #b3b3b3;
  border-radius: 10px;
  padding: 0;
  height: 100%;
  overflow: hidden;
}

.welcome-text {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

.username {
  color: #007bff;
  font-weight: bold;
}

.user-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.menu-category {
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.menu-category:last-child {
  border-bottom: none;
}

.menu-category.active {
  background-color: #e3f2fd;
}

.menu-header {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  transition: background-color 0.2s;
}

.menu-header:hover {
  background-color: #f8f9fa;
}

.menu-header i {
  margin-right: 10px;
  width: 20px;
}

.menu-header span:nth-child(2) {
  flex: 1;
  font-weight: 500;
}

.menu-count {
  background-color: #dc3545;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: auto;
}

/* 하위 메뉴들 */
.social-section, .profile-section, .personal-info-section {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.social-section:last-child, .profile-section:last-child, .personal-info-section:last-child {
  border-bottom: none;
}

.social-section h6, .profile-section h6 {
  color: #666;
  font-size: 12px;
  margin-bottom: 10px;
  text-transform: uppercase;
}

.social-menu, .profile-menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.social-menu li, .profile-menu li {
  margin-bottom: 8px;
}

.social-menu a, .profile-menu a {
  color: #333;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
  cursor: pointer;
}

.social-menu a:hover, .profile-menu a:hover,
.social-menu a.active, .profile-menu a.active {
  color: #007bff;
  font-weight: bold;
}

/* 메인 컨텐츠 */
.main-content {
  background-color: transparent;
  padding: 0;
  overflow: visible;
}

.tab-content {
  min-height: 400px;
}

/* 헤더 영역 - 독립적인 카드 */
.content-header {
  background-color: white;
  border: 1px solid #b3b3b3;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-header h4 {
  margin: 0;
  color: #333;
  border-bottom: 2px solid #000;
  padding-bottom: 5px;
  display: inline-block;
}

.filter-section {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-select {
  width: 100px;
  height: 35px;
}

.search-box {
  position: relative;
  width: 250px;
}

.search-box input {
  height: 35px;
  padding-right: 35px;
}

.search-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #666;
}

/* 주문 카드 */
.order-card {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  background-color: white;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #eee;
}

.order-date {
  font-weight: bold;
  color: #333;
}

.order-number {
  color: #666;
  font-size: 12px;
  margin-left: 15px;
  flex: 1;
}

.delivery-info {
  padding: 10px 20px;
  background-color: #fff5f5;
}

.delivery-status {
  color: #dc3545;
  font-weight: bold;
  margin-right: 10px;
}

.delivery-date {
  color: #666;
  font-size: 14px;
}

.product-info {
  display: flex;
  align-items: center;
  padding: 20px;
}

.product-image {
  margin-right: 15px;
}

.product-image img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.product-details {
  flex: 1;
}

.product-category {
  color: #666;
  font-size: 12px;
  margin-bottom: 5px;
}

.product-name {
  font-weight: 500;
  margin-bottom: 8px;
  color: #333;
}

.product-price {
  display: flex;
  gap: 10px;
}

.price {
  font-weight: bold;
  color: #333;
}

.quantity {
  color: #666;
}

.product-actions {
  margin-left: 15px;
}

.product-actions i {
  cursor: pointer;
  color: #666;
  font-size: 18px;
}

.order-actions {
  display: flex;
  gap: 10px;
  padding: 15px 20px;
  border-top: 1px solid #eee;
  background-color: #f8f9fa;
}

.order-actions .btn {
  padding: 8px 16px;
  font-size: 14px;
}

.personal-info-form {
  background-color: white;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 20px;
}

.form-section {
  max-width: 500px;
}

.form-section h5 {
  color: #333;
  margin-bottom: 10px;
  font-weight: 600;
}

.form-section p {
  margin-bottom: 20px;
  font-size: 14px;
}

.placeholder-content {
  padding: 40px 20px;
  background-color: white;
  border: 1px solid #eee;
  border-radius: 10px;
}

.profile-management {
  padding: 20px;
  background-color: white;
  border-radius: 10px;
  border: 1px solid #eee;
}

.info-section {
  margin-bottom: 30px;
}

.info-section h5 {
  color: #333;
  margin-bottom: 15px;
  font-weight: 600;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.info-item:hover {
  background-color: #f8f9fa;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item span {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.arrow-icon {
  color: #666;
  font-size: 16px;
  margin-left: auto;
}

/* 🔥 새로 추가된 스타일들 */

/* 비밀번호 섹션 스타일 */
.password-section {
  background-color: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin: 20px 0;

}

.password-section-title {
  color: #495057;
  margin-bottom: 15px;
  font-weight: 600;
  font-size: 14px;
}

/* 성별 토글 스타일 (회원가입과 동일) */
.gender-toggle {
  display: flex;
  gap: 15px;
  padding: 10px 0;
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

.gender-toggle label.active input[type="radio"] {
  accent-color: white;
}

/* 폼 텍스트 스타일 */
.form-text {
  font-size: 12px;
  color: #6c757d;
  margin-top: 5px;
}

/* 반응형 */
@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
    margin-top: 15px;
  }

  .content-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .filter-section {
    width: 100%;
    justify-content: space-between;
  }

  .search-box {
    width: 200px;
  }

  .product-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .product-details {
    width: 100%;
  }

  .gender-toggle {
    flex-direction: column;
    gap: 10px;
  }

  .gender-toggle label {
    justify-content: center;
  }
}
</style>
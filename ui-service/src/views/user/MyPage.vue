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
                <div class="stat-value">{{ points.toLocaleString() }}원</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">쿠폰수</div>
                <div class="stat-value">{{ coupons }}개</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">상품권</div>
                <div class="stat-value">{{ giftCards }}개</div>
              </div>
            </div>

            <div class="quick-action">
              <button class="btn btn-outline-primary w-100">
                혈 압약으로 매일 은혜 배송 받기 →
              </button>
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
                <span class="menu-badge">쿠폰대전</span>
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
                <li><a href="#" @click="setActiveTab('colorever')">캘러베에서 · 컬러버해</a></li>
                <li><a href="#" @click="setActiveTab('returns')">취소 · 반품 내역</a></li>
                <li><a href="#" @click="setActiveTab('reviews')">상품 후기</a></li>
                <li><a href="#" @click="setActiveTab('inquiries')">상품 문의</a></li>
              </ul>
            </div>

            <!-- 혜택 섹션 -->
            <div class="benefit-section">
              <h6>혜택</h6>
              <ul class="benefit-menu">
                <li><a href="#" @click="setActiveTab('colorbertimes')">컬러버때스</a></li>
              </ul>
            </div>

            <!-- 내 정보관리 섹션 -->
            <div class="profile-section">
              <h6>내 정보관리</h6>
              <ul class="profile-menu">
                <li><a href="#" @click="setActiveTab('shipping')" :class="{ active: activeTab === 'shipping' }">배송지 관리</a></li>
                <li><a href="#" @click="setActiveTab('colorstyle')" :class="{ active: activeTab === 'colorstyle' }">나의 컬러스타일</a></li>
                <li><a href="#" @click="setActiveTab('profile')" :class="{ active: activeTab === 'profile' }">회원 정보 관리</a></li>
                <li><a href="#" @click="setActiveTab('vip')" :class="{ active: activeTab === 'vip' }">VIP 혜상 등급</a></li>
              </ul>
            </div>

            <!-- 개인정보 수정 섹션 -->
            <div class="personal-info-section">
              <div class="section-header" @click="togglePersonalInfo" :class="{ expanded: showPersonalInfo }">
                <span>개인정보 수정</span>
                <i class="fas fa-chevron-right" :class="{ rotated: showPersonalInfo }"></i>
              </div>
              <div class="submenu" v-show="showPersonalInfo">
                <ul class="submenu-list">
                  <li><a href="#" @click="setActiveTab('basic-info')" :class="{ active: activeTab === 'basic-info' }">아이디</a></li>
                  <li><a href="#" @click="setActiveTab('current-password')" :class="{ active: activeTab === 'current-password' }">현재 비밀번호</a></li>
                  <li><a href="#" @click="setActiveTab('new-password')" :class="{ active: activeTab === 'new-password' }">새 비밀번호</a></li>
                  <li><a href="#" @click="setActiveTab('confirm-password')" :class="{ active: activeTab === 'confirm-password' }">새 비밀번호 확인</a></li>
                  <li><a href="#" @click="setActiveTab('name')" :class="{ active: activeTab === 'name' }">이름</a></li>
                  <li><a href="#" @click="setActiveTab('email')" :class="{ active: activeTab === 'email' }">이메일</a></li>
                  <li><a href="#" @click="setActiveTab('phone')" :class="{ active: activeTab === 'phone' }">휴대폰</a></li>
                  <li><a href="#" @click="setActiveTab('gender')" :class="{ active: activeTab === 'gender' }">성별</a></li>
                  <li><a href="#" @click="setActiveTab('birth')" :class="{ active: activeTab === 'birth' }">생년월일</a></li>
                </ul>
                <div class="submenu-footer">
                  <a href="#" @click="setActiveTab('use-agreement')" :class="{ active: activeTab === 'use-agreement' }">이용약관동의</a>
                </div>
              </div>
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
                <div class="row mb-3">
                  <div class="col-md-3">
                    <button class="btn btn-primary w-100" @click="setActiveTab('basic-info')">개인 정보 수정</button>
                  </div>
                  <div class="col-md-9">
                    <i class="fas fa-chevron-right"></i>
                  </div>
                </div>
              </div>

              <div class="info-section">
                <h5>보안관리</h5>
                <div class="row mb-3">
                  <div class="col-md-3">
                    <span>로그인 관리</span>
                  </div>
                  <div class="col-md-9">
                    <i class="fas fa-chevron-right"></i>
                  </div>
                </div>

                <div class="row mb-3">
                  <div class="col-md-3">
                    <span>해외 지역 로그인 차단</span>
                  </div>
                  <div class="col-md-6"></div>
                  <div class="col-md-3">
                    <div class="form-check form-switch">
                      <input class="form-check-input" type="checkbox" checked>
                    </div>
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
            <div class="personal-info-form">
              <div class="form-section">
                <h5>비밀번호 재확인</h5>
                <p class="text-muted">회원님의 정보를 안전하게 보호하기 위해 비밀번호를 다시 한번 확인해주세요.</p>

                <div class="mb-3">
                  <label class="form-label">아이디</label>
                  <input type="text" class="form-control" value="fhohfodf" readonly>
                </div>

                <div class="mb-3">
                  <label class="form-label">비밀번호 <span class="text-danger">*</span></label>
                  <input type="password" class="form-control" placeholder="현재 비밀번호를 입력해주세요" v-model="currentPassword">
                </div>

                <div class="text-center">
                  <button class="btn btn-primary px-5" @click="verifyPassword">확인</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 비밀번호 변경 탭 -->
          <div v-if="activeTab === 'new-password'" class="tab-content">
            <div class="content-header">
              <h4>비밀번호 변경</h4>
            </div>
            <div class="personal-info-form">
              <div class="form-section">
                <div class="mb-3">
                  <label class="form-label">현재 비밀번호 <span class="text-danger">*</span></label>
                  <input type="password" class="form-control" v-model="passwordForm.current">
                </div>

                <div class="mb-3">
                  <label class="form-label">새 비밀번호 <span class="text-danger">*</span></label>
                  <input type="password" class="form-control" v-model="passwordForm.new">
                  <div class="form-text">8자 이상, 영문/숫자/특수문자 조합</div>
                </div>

                <div class="mb-3">
                  <label class="form-label">새 비밀번호 확인 <span class="text-danger">*</span></label>
                  <input type="password" class="form-control" v-model="passwordForm.confirm">
                </div>

                <div class="text-center">
                  <button class="btn btn-secondary me-2" @click="resetPasswordForm">취소</button>
                  <button class="btn btn-primary px-4" @click="updatePassword">변경</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 이메일 변경 탭 -->
          <div v-if="activeTab === 'email'" class="tab-content">
            <div class="content-header">
              <h4>이메일 변경</h4>
            </div>
            <div class="personal-info-form">
              <div class="form-section">
                <div class="mb-3">
                  <label class="form-label">현재 이메일</label>
                  <input type="email" class="form-control" value="user@example.com" readonly>
                </div>

                <div class="mb-3">
                  <label class="form-label">새 이메일 <span class="text-danger">*</span></label>
                  <input type="email" class="form-control" v-model="emailForm.new" placeholder="새 이메일을 입력하세요">
                </div>

                <div class="mb-3">
                  <label class="form-label">인증번호</label>
                  <div class="input-group">
                    <input type="text" class="form-control" v-model="emailForm.verificationCode" placeholder="인증번호 입력">
                    <button class="btn btn-outline-secondary" @click="sendVerificationCode">인증번호 발송</button>
                  </div>
                </div>

                <div class="text-center">
                  <button class="btn btn-secondary me-2" @click="resetEmailForm">취소</button>
                  <button class="btn btn-primary px-4" @click="updateEmail">변경</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 기타 탭들 플레이스홀더 -->
          <div v-if="!['orders', 'profile', 'basic-info', 'new-password', 'email'].includes(activeTab)" class="tab-content">
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 사용자 데이터
const userName = ref('이종현님')
const points = ref(0)
const coupons = ref(0)
const giftCards = ref(0)

// 활성 탭
const activeTab = ref('orders')
const showPersonalInfo = ref(false)

// 폼 데이터
const currentPassword = ref('')
const passwordForm = ref({
  current: '',
  new: '',
  confirm: ''
})
const emailForm = ref({
  new: '',
  verificationCode: ''
})

// URL 파라미터에 따라 초기 탭 설정
onMounted(() => {
  if (route.query.tab) {
    activeTab.value = route.query.tab
    if (['basic-info', 'current-password', 'new-password', 'confirm-password', 'name', 'email', 'phone', 'gender', 'birth', 'use-agreement'].includes(route.query.tab)) {
      showPersonalInfo.value = true
    }
  }
  if (route.query.section === 'profile') {
    activeTab.value = 'profile'
  }
})

function setActiveTab(tab) {
  activeTab.value = tab
  if (['basic-info', 'current-password', 'new-password', 'confirm-password', 'name', 'email', 'phone', 'gender', 'birth', 'use-agreement'].includes(tab)) {
    showPersonalInfo.value = true
  }
}

function togglePersonalInfo() {
  showPersonalInfo.value = !showPersonalInfo.value
}

function getTabTitle() {
  const titles = {
    'orders': '주문 내역',
    'coupons': '쿠폰',
    'wishlist': '찜한 상품',
    'frequent': '자주 구매',
    'colorever': '캘러베에서 · 컬러버해',
    'returns': '취소 · 반품 내역',
    'reviews': '상품 후기',
    'inquiries': '상품 문의',
    'colorbertimes': '컬러버때스',
    'shipping': '배송지 관리',
    'colorstyle': '나의 컬러스타일',
    'profile': '회원 정보 관리',
    'vip': 'VIP 혜상 등급',
    'basic-info': '개인정보 수정',
    'current-password': '현재 비밀번호',
    'new-password': '새 비밀번호',
    'confirm-password': '새 비밀번호 확인',
    'name': '이름',
    'email': '이메일',
    'phone': '휴대폰',
    'gender': '성별',
    'birth': '생년월일',
    'use-agreement': '이용약관동의'
  }
  return titles[activeTab.value] || '마이페이지'
}

// 폼 액션 함수들
function verifyPassword() {
  if (!currentPassword.value) {
    alert('비밀번호를 입력해주세요.')
    return
  }
  // TODO: API 호출로 비밀번호 검증
  console.log('비밀번호 검증:', currentPassword.value)
  // 검증 성공 시 실제 개인정보 수정 화면으로 이동하거나 다른 처리
}

function updatePassword() {
  if (!passwordForm.value.current || !passwordForm.value.new || !passwordForm.value.confirm) {
    alert('모든 필드를 입력해주세요.')
    return
  }
  if (passwordForm.value.new !== passwordForm.value.confirm) {
    alert('새 비밀번호가 일치하지 않습니다.')
    return
  }
  // TODO: API 호출로 비밀번호 변경
  console.log('비밀번호 변경:', passwordForm.value)
  alert('비밀번호가 변경되었습니다.')
  resetPasswordForm()
}

function resetPasswordForm() {
  passwordForm.value = {
    current: '',
    new: '',
    confirm: ''
  }
}

function sendVerificationCode() {
  if (!emailForm.value.new) {
    alert('새 이메일을 입력해주세요.')
    return
  }
  // TODO: API 호출로 인증번호 발송
  console.log('인증번호 발송:', emailForm.value.new)
  alert('인증번호가 발송되었습니다.')
}

function updateEmail() {
  if (!emailForm.value.new || !emailForm.value.verificationCode) {
    alert('모든 필드를 입력해주세요.')
    return
  }
  // TODO: API 호출로 이메일 변경
  console.log('이메일 변경:', emailForm.value)
  alert('이메일이 변경되었습니다.')
  resetEmailForm()
}

function resetEmailForm() {
  emailForm.value = {
    new: '',
    verificationCode: ''
  }
}
</script>

<style scoped>
.mypage-container {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding: 20px 0;
}

/* 사이드바 */
.sidebar {
  background-color: white;
  border-radius: 8px;
  padding: 0;
  height: fit-content;
}

.user-info-section {
  padding: 20px;
  border-bottom: 1px solid #eee;
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

.quick-action {
  margin-top: 15px;
}

.quick-action .btn {
  font-size: 12px;
  padding: 8px 12px;
}

/* 메뉴 섹션 */
.menu-section {
  padding: 0;
}

.menu-category {
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
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

.menu-badge {
  background-color: #28a745;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  margin-left: auto;
}

/* 하위 메뉴들 */
.social-section, .benefit-section, .profile-section, .personal-info-section {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.social-section h6, .benefit-section h6, .profile-section h6 {
  color: #666;
  font-size: 12px;
  margin-bottom: 10px;
  text-transform: uppercase;
}

.social-menu, .benefit-menu, .profile-menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.social-menu li, .benefit-menu li, .profile-menu li {
  margin-bottom: 8px;
}

.social-menu a, .benefit-menu a, .profile-menu a {
  color: #333;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}

.social-menu a:hover, .benefit-menu a:hover, .profile-menu a:hover,
.social-menu a.active, .benefit-menu a.active, .profile-menu a.active {
  color: #007bff;
  font-weight: bold;
}

/* 개인정보 수정 섹션 */
.personal-info-section {
  padding: 0;
}

.section-header {
  display: flex;
  justify-content: between;
  align-items: center;
  padding: 20px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.section-header:hover {
  background-color: #f8f9fa;
}

.section-header span {
  flex: 1;
  font-weight: 500;
  color: #333;
}

.section-header i {
  transition: transform 0.3s ease;
}

.section-header.expanded i.rotated {
  transform: rotate(90deg);
}

.submenu {
  background-color: #f8f9fa;
  border-bottom: 1px solid #f0f0f0;
}

.submenu-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.submenu-list li {
  border-bottom: 1px solid #e9ecef;
}

.submenu-list li:last-child {
  border-bottom: none;
}

.submenu-list a {
  display: block;
  padding: 12px 20px;
  color: #666;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}

.submenu-list a:hover,
.submenu-list a.active {
  background-color: #007bff;
  color: white;
  padding-left: 30px;
}

.submenu-footer {
  padding: 15px 20px;
  border-top: 1px solid #e9ecef;
}

.submenu-footer a {
  color: #666;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}

.submenu-footer a:hover,
.submenu-footer a.active {
  color: #007bff;
  font-weight: bold;
}

/* 메인 컨텐츠 */
.main-content {
  background-color: white;
  border-radius: 8px;
  padding: 0;
}

.tab-content {
  min-height: 400px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.content-header h4 {
  margin: 0;
  color: #333;
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
  margin: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: between;
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

/* 회원정보 관리 */
.profile-management {
  padding: 20px;
}

.info-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.info-section h5 {
  color: #333;
  margin-bottom: 15px;
  font-weight: 600;
}

.info-section .row {
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-section .row:last-child {
  border-bottom: none;
}

/* 개인정보 수정 폼 */
.personal-info-form {
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
}
</style>
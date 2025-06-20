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
                <div class="stat-label">상품권</div>
                <div class="stat-value">{{ giftCards || 0 }}개</div>
              </div>
            </div>
          </div>

          <!-- 메뉴 섹션 -->
          <div class="menu-section">
            <div class="menu-category" @click="navigateToTab('orders')" :class="{ active: activeTab === 'orders' }">
              <div class="menu-header">
                <i class="fas fa-box text-primary"></i>
                <span>주문 내역</span>
                <span class="menu-count">{{ orderCount }}</span>
              </div>
            </div>

            <div class="menu-category" @click="navigateToTab('coupons')" :class="{ active: activeTab === 'coupons' }">
              <div class="menu-header">
                <i class="fas fa-ticket-alt text-success"></i>
                <span>쿠폰</span>
                <span class="menu-count">{{ coupons }}</span>
              </div>
            </div>

            <div class="menu-category" @click="navigateToTab('wishlist')" :class="{ active: activeTab === 'wishlist' }">
              <div class="menu-header">
                <i class="fas fa-heart text-danger"></i>
                <span>찜한 상품</span>
                <span class="menu-count">0</span>
              </div>
            </div>



            <!-- 소셜 섹션 -->
            <div class="social-section">
              <h6>소셜</h6>
              <ul class="social-menu">
                <li><a href="#" @click="navigateToTab('returns')" :class="{ active: activeTab === 'returns' }">취소 · 반품 내역</a></li>
                <li><a href="#" @click="navigateToTab('reviews')" :class="{ active: activeTab === 'reviews' }">상품 후기</a></li>
                <li><a href="#" @click="navigateToTab('inquiries')" :class="{ active: activeTab === 'inquiries' }">상품 문의</a></li>
              </ul>
            </div>

            <!-- 내 정보관리 섹션 -->
            <div class="profile-section">
              <h6>내 정보관리</h6>
              <ul class="profile-menu">

                <li><a href="#" @click="navigateToProfile()">회원 정보 관리</a></li>
                <li><a href="#" @click="navigateToTab('vip')" :class="{ active: activeTab === 'vip' }">VIP 예상 등급</a></li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 오른쪽 메인 컨텐츠 - 라우터 뷰 -->
        <div class="col-md-9 main-content">
          <router-view @update-counts="updateCounts" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { user, setUserFromToken } from '@/stores/userStore'

const route = useRoute()
const router = useRouter()

// 사용자 데이터 - userStore에서 가져오기
const computedUser = computed(() => user)
const userName = computed(() => {
  return computedUser.value.name ? computedUser.value.name + '님' : '사용자님'
})

// 현재 활성 탭 (라우트 기반)
const activeTab = computed(() => route.name)

// 사이드바에 표시할 정보
const points = ref(0)
const coupons = ref(0)
const giftCards = ref(0)
const orderCount = ref(0)

// 탭 네비게이션
const navigateToTab = (tabName) => {
  router.push({ name: `mypage-${tabName}` })
}

// 자식 컴포넌트에서 카운트 업데이트
const updateCounts = (data) => {
  if (data.orderCount !== undefined) orderCount.value = data.orderCount
  if (data.coupons !== undefined) coupons.value = data.coupons
  if (data.giftCards !== undefined) giftCards.value = data.giftCards
}

// 적립금, 쿠폰 등 추가 정보 가져오기
async function fetchUserExtraInfo() {
  try {
    const token = localStorage.getItem('token')
    if (!token) return

    // API 호출해서 적립금, 쿠폰 수 등 가져오기
    // const response = await fetch('/api/users/stats', { headers: { Authorization: `Bearer ${token}` } })
    // const data = await response.json()
    // points.value = data.points
    // coupons.value = data.coupons
    // giftCards.value = data.giftCards
  } catch (error) {
    console.error('사용자 추가 정보 로드 실패:', error)
  }
}
function navigateToProfile() {
  // 이 컴포넌트를 라우터로 이동
  router.push('/mypage/profile');
}
onMounted(() => {
  // userStore에서 사용자 정보 설정
  const token = localStorage.getItem('token')
  if (token) {
    setUserFromToken(token)
  }

  // 추가 정보 가져오기
  fetchUserExtraInfo()

  // 기본 라우트가 없으면 주문 내역으로 리다이렉트
  if (route.name === 'mypage') {
    router.replace({ name: 'mypage-orders' })
  }
})
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
  margin-top: -70%;
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
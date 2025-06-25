<template>
  <div class="mypage-container">
    <!-- Font Awesome 6.7.2 CDN 추가 -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css">

    <div class="container-fluid">
      <div class="row">
        <!-- 왼쪽 사이드바 -->
        <div class="col-md-3 sidebar">
          <!-- 사용자 정보 섹션 -->
          <div class="user-info-section">
            <div class="welcome-text">반가워요! <span class="username">{{ userName }}</span></div>

            <div class="benefit-cards">
              <div class="benefit-card">
                <div class="benefit-label">적립금</div>
                <div class="benefit-value">{{ (points || 0).toLocaleString() }}<span class="unit">원</span></div>
              </div>
              <div class="benefit-card">
                <div class="benefit-label">할인쿠폰</div>
                <div class="benefit-value">{{ giftCards || 0 }}<span class="unit">원</span></div>
              </div>
              <div class="benefit-card">
                <div class="benefit-label">상품권</div>
                <div class="benefit-value">{{ coupons || 0 }}<span class="unit">원</span></div>
              </div>
            </div>

            <button class="benefit-button">
              쇼핑할 때 쓸 수 있는 혜택이 많이
              <i class="fas fa-chevron-right"></i>
            </button>
          </div>

          <!-- 메뉴 섹션 -->
          <div class="menu-section">
            <div class="menu-title">자주 찾는 메뉴</div>

            <!-- 메뉴 아이템들 -->
            <div class="menu-items">
              <div class="menu-item" @click="navigateToTab('orders')" :class="{ active: activeTab === 'orders' }">
                <div class="menu-icon orders-icon">
                  <svg class="svg-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M20 7H4V5C4 4.44772 4.44772 4 5 4H19C19.5523 4 20 4.44772 20 5V7Z" fill="#1976d2"/>
                    <path d="M3 7H21V19C21 19.5523 20.5523 20 20 20H4C3.44772 20 3 19.5523 3 19V7Z" stroke="#1976d2"
                          stroke-width="2" fill="none"/>
                    <path d="M9 11H15" stroke="#1976d2" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </div>
                <div class="menu-info">
                  <div class="menu-name">주문 내역</div>
                  <div class="menu-count">{{ orderCount || 0 }}</div>
                </div>
              </div>

              <div class="menu-item" @click="navigateToTab('coupons')" :class="{ active: activeTab === 'coupons' }">
                <div class="menu-icon coupons-icon">
                  <svg class="svg-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path
                        d="M21 8V16C21 17.1046 20.1046 18 19 18H5C3.89543 18 3 17.1046 3 16V8C3 6.89543 3.89543 6 5 6H19C20.1046 6 21 6.89543 21 8Z"
                        stroke="#7b1fa2" stroke-width="2" fill="none"/>
                    <circle cx="8" cy="12" r="1" fill="#7b1fa2"/>
                    <circle cx="16" cy="12" r="1" fill="#7b1fa2"/>
                    <path d="M12 8V16" stroke="#7b1fa2" stroke-width="1" stroke-dasharray="2 2"/>
                  </svg>
                </div>
                <div class="menu-info">
                  <div class="menu-name">쿠폰</div>
                  <div class="menu-count">{{ coupons || 0 }}</div>
                </div>
              </div>

              <div class="menu-item" @click="navigateToTab('wishlist')" :class="{ active: activeTab === 'wishlist' }">
                <div class="menu-icon wishlist-icon">
                  <svg class="svg-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path
                        d="M12 21.35L10.55 20.03C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3C9.24 3 10.91 3.81 12 5.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5C22 12.28 18.6 15.36 13.45 20.04L12 21.35Z"
                        fill="#d32f2f"/>
                  </svg>
                </div>
                <div class="menu-info">
                  <div class="menu-name">찜한 상품</div>
                  <div class="menu-count">0</div>
                </div>
              </div>

              <div class="menu-item" @click="navigateToTab('frequent')" :class="{ active: activeTab === 'frequent' }">
                <div class="menu-icon frequent-icon">
                  <svg class="svg-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path
                        d="M7 4V2C7 1.44772 7.44772 1 8 1H16C16.5523 1 17 1.44772 17 2V4H20C20.5523 4 21 4.44772 21 5C21 5.55228 20.5523 6 20 6H19V19C19 20.1046 18.1046 21 17 21H7C5.89543 21 5 20.1046 5 19V6H4C3.44772 6 3 5.55228 3 5C3 4.44772 3.44772 4 4 4H7Z"
                        fill="#388e3c"/>
                    <path d="M9 3H15V4H9V3Z" fill="white"/>
                  </svg>
                </div>
                <div class="menu-info">
                  <div class="menu-name">자주 구매</div>
                </div>
              </div>
            </div>

            <!-- 배너 섹션 -->
            <div class="banner-section">
              <div class="banner-content">
                <div class="banner-text">
                  <div class="banner-main">베네핏 회원 혜택받고 즐거워지고</div>
                  <div class="banner-sub">2주년 기념 혜택 + 최대 30% 쿠폰 +</div>
                </div>
                <div class="banner-badge">0원</div>
                <div class="banner-discount">-30%</div>
              </div>
            </div>

            <!-- 하단 링크 섹션 -->
            <div class="bottom-links">
              <div class="link-section">
                <div class="section-title">소셜</div>
                <div class="link-item" @click="navigateToTab('returns')" :class="{ active: activeTab === 'returns' }">
                  취소 · 반품 내역
                </div>
                <div class="link-item" @click="navigateToTab('reviews')" :class="{ active: activeTab === 'reviews' }">
                  상품 후기
                </div>
                <div class="link-item" @click="navigateToTab('inquiries')"
                     :class="{ active: activeTab === 'inquiries' }">
                  상품 문의
                </div>
              </div>

              <div class="link-section">
                <div class="section-title">내 정보관리</div>
                <div class="link-item" @click="navigateToProfile()">
                  회원 정보 관리
                </div>
                <div class="link-item" @click="navigateToTab('vip')" :class="{ active: activeTab === 'vip' }">
                  VIP 예상 등급
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 오른쪽 메인 컨텐츠 - 라우터 뷰 -->
        <div class="col-md-9 main-content">
          <router-view @update-counts="updateCounts"/>
        </div>
      </div>
    </div>
  </div>
</template>

// 수정된 마이페이지 스크립트 - 공통 apiClient 사용
<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { user, setUserFromToken } from '@/stores/userStore'
import apiClient from '@/api/axiosInstance' //  공통 apiClient 임포트

const route = useRoute()
const router = useRouter()

// 사용자 데이터 - userStore에서 가져오기
const computedUser = computed(() => user)
const userName = computed(() => {
  return computedUser.value.name ? computedUser.value.name + '님' : '사용자님'
})

// 현재 활성 탭 (라우트 기반)
const activeTab = computed(() => {
  const tabNameMap = {
    'MyPageOrders': 'orders',
    'MyPageProfile': 'profile',
    'MyPageCoupons': 'coupons',
    'MyPageWishlist': 'wishlist',
    'MyPageFrequent': 'frequent',
    'MyPageReturns': 'returns',
    'MyPageReviews': 'reviews',
    'MyPageInquiries': 'inquiries',
    'MyPageVip': 'vip'
  }
  return tabNameMap[route.name] || 'orders'
})

// 사이드바에 표시할 정보
const points = ref(0)
const coupons = ref(0)
const giftCards = ref(0)
const orderCount = ref(0)

// 탭 네비게이션
const navigateToTab = (tabName) => {
  const routeNameMap = {
    'orders': 'MyPageOrders',
    'profile': 'MyPageProfile',
    'coupons': 'MyPageCoupons',
    'wishlist': 'MyPageWishlist',
    'frequent': 'MyPageFrequent',
    'returns': 'MyPageReturns',
    'reviews': 'MyPageReviews',
    'inquiries': 'MyPageInquiries',
    'vip': 'MyPageVip'
  }

  const routeName = routeNameMap[tabName]
  if (routeName) {
    router.push({ name: routeName })
  }
}

// 자식 컴포넌트에서 카운트 업데이트
const updateCounts = (data) => {
  if (data.orderCount !== undefined) orderCount.value = data.orderCount
  if (data.coupons !== undefined) coupons.value = data.coupons
  if (data.giftCards !== undefined) giftCards.value = data.giftCards
}

//  공통 apiClient를 사용한 사용자 추가 정보 가져오기
//  사용자 추가 정보 로딩 (포인트, 쿠폰 등)
const fetchUserExtraInfo = async () => {

  // 각 API를 개별적으로 호출하여 일부 실패해도 다른 정보는 로드되도록
  const apiCalls = [
    {
      name: '포인트 정보',
      call: () => apiClient.get('/api/users/points'),
      onSuccess: (response) => {
        if (response.data.success) {
          availablePoints.value = response.data.data || 0
        }
      }
    },
    {
      name: '쿠폰 정보',
      call: () => apiClient.get('/api/users/coupons'),
      onSuccess: (response) => {
        if (response.data.success) {
          availableCoupons.value = response.data.data?.length || 0

        }
      }
    },
    {
      name: '주문 개수',
      call: () => apiClient.get('/api/orders/count'),
      onSuccess: (response) => {
        if (response.data.success) {
          totalOrders.value = response.data.data || 0
        }
      }
    }
  ]

  // 각 API를 병렬로 호출하되 실패해도 다른 API는 계속 실행
  const results = await Promise.allSettled(
      apiCalls.map(async (api) => {
        try {
          const response = await api.call()
          api.onSuccess(response)
          return { name: api.name, success: true }
        } catch (error) {
          return { name: api.name, success: false, error: error.message }
        }
      })
  )

  // 로드 결과 요약
  const successCount = results.filter(r => r.value?.success).length
  const totalCount = results.length

  // 일부 실패한 경우 사용자에게 알림 (선택적)
  if (successCount < totalCount) {
    const failedApis = results
        .filter(r => !r.value?.success)
        .map(r => r.value?.name)
        .join(', ')

  }
}

function navigateToProfile() {
  router.push({ name: 'MyPageProfile' })
}

// 토큰 유효성 검사
const isTokenValid = (token) => {
  if (!token) return false

  try {
    const parts = token.split('.')
    if (parts.length !== 3) return false

    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    while (base64.length % 4) {
      base64 += '='
    }

    const payloadStr = atob(base64)
    const payload = JSON.parse(payloadStr)
    const currentTime = Math.floor(Date.now() / 1000)

    if (payload.exp && payload.exp < currentTime) {
      return false
    }

    return true
  } catch (error) {
    return false
  }
}

// 마운트 시 처리
onMounted(async () => {

  const token = localStorage.getItem('token')

  if (!token) {
    router.push('/login')
    return
  }

  if (!isTokenValid(token)) {
    localStorage.removeItem('token')
    router.push('/login')
    return
  }

  // userStore에서 사용자 정보 설정
  try {
    setUserFromToken(token)
  } catch (error) {
    localStorage.removeItem('token')
    router.push('/login')
    return
  }

  await fetchUserExtraInfo()

  // 기본 라우트가 없으면 주문 내역으로 리다이렉트
  if (route.name === 'mypage') {
    router.replace({ name: 'MyPageOrders' })
  }

})
</script>

<style scoped>
/* 기본 컨테이너 */
.mypage-container {
  background-color: #f8f9fa;
  padding: 20px 0;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Malgun Gothic', sans-serif;
}

.container-fluid {
  height: 100%;
}

.row {
  min-height: calc(100vh - 40px);
}

/* 사이드바 */
.sidebar {
  padding: 0;
  padding-right: 15px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 사용자 정보 섹션 */
.user-info-section {
  background-color: white;
  padding: 24px 20px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.welcome-text {
  font-size: 18px;
  margin-bottom: 20px;
  color: #333;
  font-weight: 600;
  line-height: 1.4;
}

.username {
  color: #5d5fef;
  font-weight: 700;
}

/* 혜택 카드들 */
.benefit-cards {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.benefit-card {
  flex: 1;
  background-color: #f8f9fc;
  padding: 16px 12px;
  border-radius: 12px;
  text-align: center;
  border: 1px solid #e9ecef;
}

.benefit-label {
  font-size: 11px;
  color: #6c757d;
  margin-bottom: 8px;
  font-weight: 500;
}

.benefit-value {
  font-size: 18px;
  font-weight: 700;
  color: #212529;
  line-height: 1;
}

.benefit-value .unit {
  font-size: 12px;
  font-weight: 500;
  color: #6c757d;
  margin-left: 2px;
}

/* 혜택 버튼 */
.benefit-button {
  width: 100%;
  background: linear-gradient(135deg, #e3f2fd, #f3e5f5);
  border: none;
  border-radius: 12px;
  padding: 16px;
  font-size: 13px;
  font-weight: 500;
  color: #5d5fef;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.benefit-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(93, 95, 239, 0.15);
}

/* 메뉴 섹션 */
.menu-section {
  background-color: white;
  border-radius: 16px;
  padding: 24px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  flex: 1;
}

.menu-title {
  font-size: 16px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 20px;
}

/* 메뉴 아이템들 */
.menu-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 24px;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 16px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item:hover {
  background-color: #f8f9fa;
}

.menu-item.active {
  background-color: #e3f2fd;
}

/* 메뉴 아이콘 스타일 */
.menu-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.svg-icon {
  width: 20px;
  height: 20px;
}

/* 각 메뉴별 아이콘 배경색 */
.orders-icon {
  background-color: #e3f2fd;
}

.coupons-icon {
  background-color: #f3e5f5;
}

.wishlist-icon {
  background-color: #ffebee;
}

.frequent-icon {
  background-color: #e8f5e8;
}

.menu-info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.menu-name {
  font-size: 14px;
  font-weight: 500;
  color: #212529;
}

.menu-count {
  font-size: 16px;
  font-weight: 700;
  color: #212529;
}

/* 배너 섹션 */
.banner-section {
  margin-bottom: 24px;
}

.banner-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.banner-text {
  color: white;
  margin-bottom: 12px;
}

.banner-main {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
  line-height: 1.3;
}

.banner-sub {
  font-size: 11px;
  opacity: 0.9;
  line-height: 1.3;
}

.banner-badge {
  position: absolute;
  top: 16px;
  right: 60px;
  background-color: white;
  color: #667eea;
  font-size: 14px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 6px;
}

.banner-discount {
  position: absolute;
  top: 16px;
  right: 16px;
  background-color: #ff4757;
  color: white;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 6px;
  border-radius: 4px;
}

/* 하단 링크 섹션 */
.bottom-links {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.link-section {
  border-bottom: 1px solid #f1f3f4;
  padding-bottom: 16px;
}

.link-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #6c757d;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.link-item {
  padding: 8px 0;
  font-size: 13px;
  color: #495057;
  cursor: pointer;
  transition: color 0.2s;
  line-height: 1.4;
}

.link-item:hover,
.link-item.active {
  color: #5d5fef;
  font-weight: 500;
}

/* 메인 컨텐츠 */
.main-content {
  background-color: transparent;
  padding: 0;
  overflow: visible;
  height: 100%;
}

/* 반응형 */
@media (max-width: 768px) {
  .sidebar {
    margin-bottom: 20px;
  }

  .benefit-cards {
    flex-direction: column;
    gap: 8px;
  }

  .main-content {
    margin-left: 0;
  }
}
</style>
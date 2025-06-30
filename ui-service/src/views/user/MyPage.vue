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
                <div class="benefit-value">{{ (availablePoints || 0).toLocaleString() }}<span class="unit">원</span></div>
              </div>
              <div class="benefit-card">
                <div class="benefit-label">할인쿠폰</div>
                <div class="benefit-value">{{ availableCoupons || 0 }}<span class="unit">개</span></div>
              </div>
              <div class="benefit-card">
                <div class="benefit-label">상품권</div>
                <div class="benefit-value">{{ giftCards || 0 }}<span class="unit">원</span></div>
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
                  <div class="menu-count">{{ totalOrders || 0 }}</div>
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
                  <div class="menu-count">{{ availableCoupons || 0 }}</div>
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

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { user, setUserFromToken } from '@/stores/userStore'
import apiClient from '@/api/axiosInstance'

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

// 🔥 사이드바에 표시할 정보 - 변수명 수정
const availablePoints = ref(0)
const availableCoupons = ref(0)
const giftCards = ref(0)
const totalOrders = ref(0)

// 탭 네비게이션
const navigateToTab = (tabName) => {
  const routeNameMap = {
    'orders': 'MyPageOrders',
    'profile': 'MyPageProfile',
    'coupons': 'MyPageCoupons',
    'wishlist': 'MyPageWishlist',
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
  if (data.orderCount !== undefined) totalOrders.value = data.orderCount
  if (data.coupons !== undefined) availableCoupons.value = data.coupons
  if (data.giftCards !== undefined) giftCards.value = data.giftCards
}

// 🔥 사용자 추가 정보 로딩 함수 수정
const fetchUserExtraInfo = async () => {
  const apiCalls = [
    {
      name: '주문 개수',
      call: () => apiClient.get('/api/orders/count'),
      onSuccess: (response) => {
        if (response.data.success) {
          totalOrders.value = response.data.data?.count || 0
        }
      }
    }
  ]

  const results = await Promise.allSettled(
      apiCalls.map(async (api) => {
        try {
          const response = await api.call()
          api.onSuccess(response)
          return { name: api.name, success: true }
        } catch (error) {
          console.warn(`${api.name} 로드 실패:`, error.message)
          return { name: api.name, success: false, error: error.message }
        }
      })
  )

  const successCount = results.filter(r => r.value?.success).length
  const totalCount = results.length

  if (successCount < totalCount) {
    const failedApis = results
        .filter(r => !r.value?.success)
        .map(r => r.value?.name)
        .join(', ')

    console.warn(`일부 데이터 로드 실패: ${failedApis}`)
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
  console.log('🔍 MyPage 마운트 시작')
  console.log('현재 라우트:', route.name, route.path)

  const token = localStorage.getItem('token')

  if (!token) {
    console.warn('토큰이 없음 - 로그인 페이지로 이동')
    router.push('/login')
    return
  }

  if (!isTokenValid(token)) {
    console.warn('토큰이 유효하지 않음 - 로그인 페이지로 이동')
    localStorage.removeItem('token')
    router.push('/login')
    return
  }

  // userStore에서 사용자 정보 설정
  try {
    setUserFromToken(token)
    console.log('✅ 사용자 정보 설정 완료')
  } catch (error) {
    console.error('사용자 정보 설정 실패:', error)
    localStorage.removeItem('token')
    router.push('/login')
    return
  }

  // 추가 사용자 정보 로드
  await fetchUserExtraInfo()

  // // 🔥 기본 라우트가 /mypage 인 경우 주문 내역으로 리다이렉트
  // if (route.path === '/mypage') {
  //   console.log('기본 경로 접근 - 주문 내역으로 리다이렉트')
  //   router.replace('/mypage/orders')
  // }

  console.log('✅ MyPage 초기화 완료')
})
</script>

<style scoped src="@/assets/css/myPage.css"></style>
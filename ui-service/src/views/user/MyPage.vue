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
            <div class="welcome-text">
              반가워요! <span class="username">{{ userName }}</span>
              <!--  소셜 로그인 표시 -->
<!--              <div v-if="isSocialUser" class="social-login-badge">-->
<!--                <span class="social-badge">{{ socialProviderName }}</span>-->
<!--              </div>-->
            </div>

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
                <!--  소셜 로그인 사용자 제한 적용 -->
                <div v-if="!isSocialUser" class="link-item" @click="navigateToProfile()">
                  회원 정보 관리
                </div>
                <div v-else class="link-item disabled" @click="showSocialUserAlert">
                  <span class="disabled-text">회원 정보 관리</span>
                  <span class="social-restriction-icon">🔒</span>
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

    <!-- 소셜 로그인 사용자 알림 모달 -->
    <div v-if="showSocialAlert" class="modal-overlay" @click="closeSocialAlert">
      <div class="modal-content social-alert-modal" @click.stop>
        <div class="modal-header">
          <div class="social-icon">
            {{ socialProviderName === '카카오' ? '💬' : socialProviderName === '네이버' ? '🟢' : '👤' }}
          </div>
          <h3 class="modal-title">{{ socialProviderName }} 로그인 사용자</h3>
          <button @click="closeSocialAlert" class="modal-close">
            <svg class="close-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/>
              <line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/>
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div class="alert-content">
            <div class="alert-message">
              <p class="main-message">
                <strong>{{ socialProviderName }} 로그인</strong>으로 가입하신 회원은<br>
                보안상의 이유로 회원정보 수정이 제한됩니다.
              </p>

              <div class="restriction-details">
                <h4>이용 가능한 서비스:</h4>
                <ul class="available-services">
                  <li>주문 내역 조회</li>
                  <li>상품 주문 및 결제</li>
                  <li>상품 후기 작성</li>
                  <li>고객센터 문의</li>
                </ul>

                <h4 class="mt-3">제한되는 서비스:</h4>
                <ul class="restricted-services">
                  <li>개인정보 수정 (이름, 이메일 등)</li>
                  <li>비밀번호 변경</li>
                  <li>회원탈퇴 ({{ socialProviderName }}에서 직접 처리)</li>
                </ul>
              </div>

              <div class="help-section">
                <p class="help-text">
                  <strong>개인정보 변경이 필요하신 경우:</strong><br>
                  {{ socialProviderName }} 계정에서 직접 정보를 수정해주세요.
                </p>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button @click="closeSocialAlert" class="confirm-button">
            확인
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { user, setUserFromToken, isSocialLoginUser, getSocialLoginProvider } from '@/stores/userStore'
import apiClient from '@/api/axiosInstance'

const route = useRoute()
const router = useRouter()

//  소셜 로그인 관련 상태 추가
const isSocialUser = ref(false)
const socialProvider = ref(null)
const showSocialAlert = ref(false)

// 사용자 데이터 - userStore에서 가져오기
const computedUser = computed(() => user)
const userName = computed(() => {
  return computedUser.value.name ? computedUser.value.name + '님' : '사용자님'
})

//  소셜 로그인 제공업체 표시명
const socialProviderName = computed(() => {
  switch (socialProvider.value) {
    case 'KAKAO':
    case 'kakao':
      return '카카오'
    case 'NAVER':
    case 'naver':
      return '네이버'
    case 'GOOGLE':
    case 'google':
      return '구글'
    default:
      return '소셜'
  }
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

// 소셜 로그인 사용자 알림 표시
const showSocialUserAlert = () => {
  showSocialAlert.value = true
}

const closeSocialAlert = () => {
  showSocialAlert.value = false
}

// 주문 개수만 별도로 다시 로드하는 함수
const reloadOrderCount = async () => {
  try {
    const response = await apiClient.get('/api/orders/count')

    if (response.data.success) {
      const orderCount = response.data.data || 0
      totalOrders.value = orderCount
    }
  } catch (error) {
    // 에러 처리만 유지
  }
}

// 자식 컴포넌트에서 카운트 업데이트
const updateCounts = (data) => {
  if (data.orderCount !== undefined) totalOrders.value = data.orderCount
  if (data.coupons !== undefined) availableCoupons.value = data.coupons
  if (data.giftCards !== undefined) giftCards.value = data.giftCards
}

// 사용자 추가 정보 로딩 함수
const fetchUserExtraInfo = async () => {
  const apiCalls = [
    {
      name: '주문 개수',
      call: () => apiClient.get('/api/orders/count'),
      onSuccess: (response) => {
        if (response.data.success) {
          const orderCount = response.data.data || 0
          totalOrders.value = orderCount
        } else {
          totalOrders.value = 0
        }
      }
    },
    {
      name: '장바구니 개수',
      call: () => apiClient.get('/api/cart/count'),
      onSuccess: (response) => {
        if (response.data.success) {
          const cartCount = response.data.data || 0
          // 장바구니 개수를 사용할 곳이 있다면 설정
        }
      }
    }
  ]

  await Promise.allSettled(
      apiCalls.map(async (api) => {
        try {
          const response = await api.call()
          api.onSuccess(response)
          return { name: api.name, success: true }
        } catch (error) {
          // 주문 개수 API 실패시 기본값 설정
          if (api.name === '주문 개수') {
            totalOrders.value = 0
          }
          return { name: api.name, success: false, error: error.message }
        }
      })
  )
}

//  회원정보관리 네비게이션 (소셜 로그인 체크 추가)
function navigateToProfile() {
  // 소셜 로그인 사용자는 접근 차단
  if (isSocialUser.value) {
    showSocialUserAlert()
    return
  }

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

//  소셜 로그인 여부 체크 함수
const checkSocialLoginStatus = () => {
  isSocialUser.value = isSocialLoginUser()
  socialProvider.value = getSocialLoginProvider()

  console.log('🔍 소셜 로그인 체크:', {
    isSocialUser: isSocialUser.value,
    provider: socialProvider.value,
    providerName: socialProviderName.value
  })
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

    //  소셜 로그인 여부 체크
    checkSocialLoginStatus()

  } catch (error) {
    localStorage.removeItem('token')
    router.push('/login')
    return
  }

  // 추가 사용자 정보 로드
  await fetchUserExtraInfo()

  // 1초 후 주문 개수 재확인
  setTimeout(async () => {
    await reloadOrderCount()
  }, 1000)
})

// 자식 컴포넌트에서 호출할 수 있도록 expose
defineExpose({
  reloadOrderCount
})
</script>

<style scoped src="@/assets/css/myPage.css"></style>

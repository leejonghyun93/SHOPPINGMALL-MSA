<template>
  <div class="checkout-container">
    <!-- 헤더 -->
    <div class="checkout-header">
      <button class="back-button" @click="goBack">
        <ChevronLeft :size="24" />
      </button>
      <h1 class="checkout-title">주문서</h1>
      <div class="header-spacer"></div>
    </div>

    <div class="checkout-content">
      <!-- 왼쪽: 주문 정보 -->
      <div class="order-form">
        <!-- 주문 상품 -->
        <div class="form-section">
          <h2 class="section-title">주문 상품</h2>
          <div class="order-items">
            <div v-for="item in orderItems" :key="item.id" class="order-item">
              <div class="item-image">
                <img :src="item.image" :alt="item.name" />
              </div>
              <div class="item-details">
                <h3 class="item-name">{{ item.name }}</h3>
                <div class="item-price">
                  <span v-if="item.discountRate > 0" class="discount-rate">{{ item.discountRate }}%</span>
                  <span class="final-price">{{ formatPrice(item.salePrice) }}원</span>
                </div>
                <div class="item-quantity">수량: {{ item.quantity }}개</div>
              </div>
            </div>
          </div>
          <div class="delivery-notice">
            <span class="delivery-text">[롯데칠성] 칼피스 (250mL X 6개) 상품을 주문합니다.</span>
          </div>
        </div>

        <!-- 주문자 정보 -->
        <div class="form-section">
          <h2 class="section-title">주문자 정보</h2>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">보내는 분</label>
              <span class="form-value">{{ userInfo.name || '이종현' }}</span>
            </div>
            <div class="form-group">
              <label class="form-label">휴대폰</label>
              <span class="form-value">{{ userInfo.phone || '010-2019-3286' }}</span>
            </div>
            <div class="form-group">
              <label class="form-label">이메일</label>
              <div class="email-info">
                <span class="form-value">{{ userInfo.email || 'fightfool@naver.com' }}</span>
                <div class="email-notice">
                  <p>이메일로 주문에 대한 정보를 보내드립니다.</p>
                  <p>잘못 입력된 이메일이나 메일차단 시 확인이 불가합니다.</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 배송 정보 -->
        <div class="form-section">
          <h2 class="section-title">배송 정보
            <button class="modify-btn">배송지 변경 안내</button>
          </h2>

          <div class="delivery-address">
            <div class="address-label">배송지</div>
            <div class="address-content">
              <p>{{ deliveryInfo.address || '서울특별시 송파구 정현로 135 (어마덜랩터원) 7층 16층 한국스프트에이전시협의회' }}</p>
              <button class="change-btn">변경</button>
            </div>
          </div>

          <div class="delivery-info-section">
            <div class="delivery-info-label">배송 요청사항</div>
            <div class="delivery-options">
              <span>{{ deliveryInfo.request || '빠짐 · 공동현관 비밀번호 (권장)' }}</span>
              <p>{{ deliveryInfo.recipientName || userInfo.name || '이종현' }}, {{ deliveryInfo.recipientPhone || userInfo.phone || '010-2019-3286' }}</p>
              <button class="edit-btn">수정</button>
            </div>
          </div>
        </div>

        <!-- 결제 수단 -->
        <div class="form-section">
          <h2 class="section-title">결제 수단</h2>

          <div class="payment-methods">
            <div class="payment-option">
              <label class="radio-container">
                <input type="radio" name="payment" value="card" v-model="selectedPayment">
                <span class="radio-mark"></span>
                <span class="payment-label">컬리페이 충전결제</span>
                <span class="recommended-badge">추천</span>
              </label>
              <div class="payment-notice">
                충전결제 사용량은 컬리페이 7배치 적립받고
              </div>
            </div>

            <div class="payment-option">
              <label class="radio-container">
                <input type="radio" name="payment" value="kurly" v-model="selectedPayment">
                <span class="radio-mark"></span>
                <span class="payment-label">Kurly Pay</span>
                <span class="new-badge">새로운</span>
                <span class="hot-badge">HOT</span>
              </label>
            </div>

            <div class="payment-option">
              <label class="radio-container">
                <input type="radio" name="payment" value="npay" v-model="selectedPayment">
                <span class="radio-mark"></span>
                <span class="payment-label">네이버페이</span>
              </label>
            </div>

            <div class="payment-option">
              <label class="radio-container">
                <input type="radio" name="payment" value="general" v-model="selectedPayment" checked>
                <span class="radio-mark"></span>
                <span class="payment-label">다른 결제수단</span>
              </label>
            </div>

            <!-- 다른 결제수단 상세 -->
            <div v-if="selectedPayment === 'general'" class="sub-payment-methods">
              <div class="sub-payment-group">
                <label class="radio-container">
                  <input type="radio" name="subPayment" value="credit" v-model="selectedSubPayment" checked>
                  <span class="radio-mark"></span>
                  <span class="payment-label">신용/체크카드</span>
                </label>
                <div class="card-options">
                  <button class="card-option-btn active">신용카드</button>
                  <button class="card-option-btn">간편결제</button>
                  <button class="card-option-btn">휴대폰</button>
                </div>
              </div>

              <div class="sub-payment-group">
                <label class="radio-container">
                  <input type="radio" name="subPayment" value="kakao" v-model="selectedSubPayment">
                  <span class="radio-mark"></span>
                  <span class="payment-label">카카오페이</span>
                </label>
              </div>

              <div class="sub-payment-group">
                <label class="radio-container">
                  <input type="radio" name="subPayment" value="toss" v-model="selectedSubPayment">
                  <span class="radio-mark"></span>
                  <span class="payment-label">토스</span>
                </label>
              </div>

              <div class="sub-payment-group">
                <label class="radio-container">
                  <input type="radio" name="subPayment" value="payco" v-model="selectedSubPayment">
                  <span class="radio-mark"></span>
                  <span class="payment-label">페이코</span>
                  <span class="event-badge">이벤트</span>
                </label>
              </div>
            </div>
          </div>

          <div class="payment-notice-section">
            <p class="notice-title">주의사항 안내</p>
            <ul class="notice-list">
              <li>무이자 할부가 적용되지 않은 카드, PG사에서는 취급 없음</li>
              <li>무이자카드: 광주 씨, 씨티 씨, 롯데 씨, 삼성 씨를 확인 안됨</li>
              <li>삼성카드: 온라인 50만 원 이상 결제 시, 2024 컵 3월 정책 안됨</li>
              <li>하나카드: 온라인 결제 시, 1.2만원 참여 학습 온라인 시개 안됨</li>
            </ul>
            <p class="details-link">자세히보기</p>
          </div>
        </div>

        <!-- 쿠폰 -->
        <div class="form-section">
          <h2 class="section-title">쿠폰</h2>
          <div class="coupon-section">
            <div class="coupon-input">
              <input type="text" placeholder="쿠폰 번호를 입력 해주세요 / 쿠폰명 입력" class="coupon-input-field">
            </div>
            <p class="coupon-notice">쿠폰 적용 시 샛별배송 지역이 안됩니다.</p>
            <p class="coupon-terms">쿠폰 이용 문의 (#칼리컬러) *</p>
            <a href="#" class="coupon-link">더민컬러츠 할인정보로 해결 무료배송 ></a>
          </div>
        </div>

        <!-- 적립금/컬리패스 -->
        <div class="form-section">
          <h2 class="section-title">적립금·컬리패스</h2>
          <div class="points-section">
            <div class="points-row">
              <span class="points-label">적립금 · 컬리패스</span>
              <span class="points-value">사용</span>
              <span class="points-amount">0 원</span>
            </div>
            <div class="points-row">
              <span class="points-label">혜택금</span>
              <span class="points-amount">0 원</span>
            </div>
            <div class="points-row">
              <span class="points-label">컬리패스</span>
              <span class="points-amount">0 원</span>
            </div>

            <div class="points-input-section">
              <input type="number" placeholder="0" class="points-input" v-model="pointsToUse">
              <button class="use-all-btn">전액사용</button>
            </div>

            <div class="points-info">
              <p>적립금만 입력해주십시오 또는 사용이 안되시면.</p>
              <p>컬리패스는 컬리패스 가격 시 사용할 수 있습니다.</p>
            </div>
          </div>
        </div>

        <!-- 컬리카드 혜택 -->
        <div class="form-section">
          <h2 class="section-title">컬리카드 혜택</h2>
          <div class="kurlycard-section">
            <div class="kurlycard-info">
              <span class="card-status">즉시 할인</span>
              <span class="card-discount">컬리카드 첫 결제 -10,000원</span>
            </div>
            <button class="card-apply-btn">혜택 받기</button>
          </div>
        </div>
      </div>

      <!-- 오른쪽: 결제 정보 -->
      <div class="payment-summary">
        <div class="summary-card">
          <h3 class="summary-title">결제금액</h3>

          <div class="summary-details">
            <div class="summary-row">
              <span class="summary-label">주문금액</span>
              <span class="summary-value">{{ formatPrice(orderAmount) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">상품금액</span>
              <span class="summary-value">{{ formatPrice(productAmount) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">상품할인금액</span>
              <span class="summary-value">{{ formatPrice(discountAmount) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">배송비</span>
              <span class="summary-value">+ {{ formatPrice(deliveryFee) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">쿠폰할인</span>
              <span class="summary-value">{{ formatPrice(couponDiscount) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">카드즉시할인</span>
              <span class="summary-value">{{ formatPrice(cardDiscount) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">적립금 컬리패스</span>
              <span class="summary-value">{{ formatPrice(pointsUsed) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">혜택금</span>
              <span class="summary-value">{{ formatPrice(benefitAmount) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">컬리패스</span>
              <span class="summary-value">{{ formatPrice(kurlypassAmount) }}원</span>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-row total">
              <span class="summary-label">최종 결제금액</span>
              <span class="summary-value">{{ formatPrice(finalAmount) }}원</span>
            </div>
          </div>

          <div class="benefits-notice">
            <p>🎁 최종 결제 시 여시기 2,000원 더 받기 ></p>
          </div>

          <button class="checkout-btn" @click="processPayment" :disabled="!canProceed">
            {{ formatPrice(finalAmount) }}원 결제하기
          </button>

          <div class="agreement-text">
            결제 시 이용약관 및 개인정보 수집에 이용에 동의하게 됩니다
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 체크아웃 페이지 <script setup> 부분 수정

import { ref, computed, onMounted } from 'vue'
import { ChevronLeft } from 'lucide-vue-next'
import axios from 'axios'

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// 상태 관리
const selectedPayment = ref('general')
const selectedSubPayment = ref('credit')
const pointsToUse = ref(0)

// 사용자 정보
const userInfo = ref({
  name: '',
  phone: '',
  email: ''
})

// 배송 정보
const deliveryInfo = ref({
  address: '',
  detailAddress: '',
  zipCode: '',
  request: '',
  recipientName: '',
  recipientPhone: ''
})

// 주문 상품
const orderItems = ref([])

// 가격 정보
const orderAmount = ref(0)
const productAmount = ref(0)
const discountAmount = ref(0)
const deliveryFee = ref(3000)
const couponDiscount = ref(0)
const cardDiscount = ref(0)
const pointsUsed = ref(0)
const benefitAmount = ref(0)
const kurlypassAmount = ref(0)

// 인증 관련 (수정된 부분)
const isLoggedIn = ref(false)
const authError = ref('')
const loading = ref(false)

// 계산된 값들
const finalAmount = computed(() => {
  return Math.max(0, orderAmount.value + deliveryFee.value - couponDiscount.value - cardDiscount.value - pointsUsed.value - benefitAmount.value - kurlypassAmount.value)
})

const canProceed = computed(() => {
  return selectedPayment.value && orderItems.value.length > 0
})

// 인증 헤더 생성 (개선된 버전)
const getAuthHeaders = () => {
  const token = localStorage.getItem('auth_token') ||
      localStorage.getItem('token') ||
      localStorage.getItem('access_token') ||
      sessionStorage.getItem('auth_token')

  const userId = localStorage.getItem('user_id') ||
      localStorage.getItem('userId') ||
      sessionStorage.getItem('user_id') ||
      'guest_' + Date.now()

  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': userId
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
    console.log('JWT 토큰 사용:', token.substring(0, 20) + '...')
  } else {
    console.log('게스트 사용자 헤더 생성:', userId)
  }

  return headers
}

// 로그인 상태 확인 (관대한 버전)
const checkLoginStatus = () => {
  const token = localStorage.getItem('auth_token') ||
      localStorage.getItem('token') ||
      localStorage.getItem('access_token') ||
      sessionStorage.getItem('auth_token')

  isLoggedIn.value = !!token
  console.log('체크아웃 페이지 로그인 상태:', isLoggedIn.value)

  // 로그인되지 않았어도 게스트로 진행 허용
  return true
}

// 사용자 정보 로드 (옵셔널)
const loadUserInfo = async () => {
  if (!isLoggedIn.value) {
    // 게스트 사용자 기본 정보 설정
    userInfo.value = {
      name: '게스트 사용자',
      phone: '',
      email: ''
    }
    return
  }

  try {
    const response = await axios.get(`${API_BASE_URL}/api/users/profile`, {
      headers: getAuthHeaders()
    })

    if (response.data.success) {
      const userData = response.data.data
      userInfo.value = {
        name: userData.name || userData.username || '사용자',
        phone: userData.phone || '',
        email: userData.email || ''
      }
    }
  } catch (error) {
    console.error('사용자 정보 로드 실패:', error)
    // 에러가 발생해도 게스트로 진행
    userInfo.value = {
      name: '게스트 사용자',
      phone: '',
      email: ''
    }
  }
}

// 배송지 정보 로드 (옵셔널)
const loadDeliveryInfo = async () => {
  if (!isLoggedIn.value) {
    // 게스트 사용자 기본 배송지 설정
    deliveryInfo.value = {
      address: '서울특별시 강남구',
      detailAddress: '상세주소를 입력해주세요',
      zipCode: '12345',
      request: '문 앞에 놓아주세요',
      recipientName: userInfo.value.name,
      recipientPhone: ''
    }
    return
  }

  try {
    const response = await axios.get(`${API_BASE_URL}/api/users/addresses`, {
      headers: getAuthHeaders()
    })

    if (response.data.success && response.data.data?.length > 0) {
      const address = response.data.data[0]
      deliveryInfo.value = {
        address: address.address || '',
        detailAddress: address.detailAddress || '',
        zipCode: address.zipCode || '',
        request: address.request || '',
        recipientName: address.recipientName || userInfo.value.name,
        recipientPhone: address.recipientPhone || userInfo.value.phone
      }
    }
  } catch (error) {
    console.error('배송지 정보 로드 실패:', error)
    // 에러가 발생해도 기본값으로 진행
  }
}

// 주문 데이터 로드 (세션에서)
const loadOrderData = () => {
  try {
    const checkoutData = sessionStorage.getItem('checkout_data')
    if (checkoutData) {
      const data = JSON.parse(checkoutData)
      console.log('세션에서 체크아웃 데이터 로드:', data)

      if (data.items && data.items.length > 0) {
        orderItems.value = data.items
        orderAmount.value = data.totalPrice || data.totalAmount || 0
        productAmount.value = data.productAmount || orderAmount.value
        deliveryFee.value = data.deliveryFee || 3000
      } else {
        console.warn('체크아웃 데이터에 상품이 없습니다.')
        // 장바구니로 리다이렉트
        alert('주문할 상품이 없습니다. 장바구니로 이동합니다.')
        window.location.href = '/cart'
      }
    } else {
      console.warn('세션에 체크아웃 데이터가 없습니다.')
      alert('주문 정보를 찾을 수 없습니다. 장바구니로 이동합니다.')
      window.location.href = '/cart'
    }
  } catch (error) {
    console.error('주문 데이터 로드 실패:', error)
    alert('주문 정보 로드 중 오류가 발생했습니다.')
    window.location.href = '/cart'
  }
}

// 인증 에러 처리 (관대한 버전)
const handleAuthError = () => {
  console.log('인증 에러 발생, 게스트로 계속 진행')
  isLoggedIn.value = false

  // 토큰 정리 (선택적)
  localStorage.removeItem('auth_token')
  localStorage.removeItem('token')
  localStorage.removeItem('access_token')

  // 게스트로 계속 진행하도록 허용
}

// 메서드들
const goBack = () => {
  window.history.back()
}

const formatPrice = (price) => {
  return price?.toLocaleString() || '0'
}

// 결제 처리 (개선된 버전)
const processPayment = async () => {
  if (!canProceed.value) {
    alert('결제 정보를 확인해주세요.')
    return
  }

  try {
    loading.value = true
    console.log('결제 처리 시작...')

    // 주문 생성 API 호출
    const orderData = {
      userId: localStorage.getItem('user_id') || 'guest_' + Date.now(),
      items: orderItems.value.map(item => ({
        productId: item.productId || item.id,
        productName: item.name,
        quantity: item.quantity,
        unitPrice: item.salePrice,
        totalPrice: item.salePrice * item.quantity,
        imageUrl: item.image
      })),
      phone: userInfo.value.phone,
      email: userInfo.value.email,
      recipientName: deliveryInfo.value.recipientName,
      recipientPhone: deliveryInfo.value.recipientPhone,
      orderZipcode: deliveryInfo.value.zipCode,
      orderAddressDetail: deliveryInfo.value.address + ' ' + deliveryInfo.value.detailAddress,
      deliveryMemo: deliveryInfo.value.request,
      paymentMethod: selectedPayment.value,
      paymentMethodName: getPaymentMethodName(selectedPayment.value),
      usedPoint: pointsToUse.value
    }

    console.log('주문 데이터:', orderData)

    const response = await fetch(`${API_BASE_URL}/api/orders/checkout`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(orderData)
    })

    if (!response.ok) {
      let errorMessage = '주문 처리 중 오류가 발생했습니다.'

      try {
        const errorData = await response.json()
        errorMessage = errorData.message || errorMessage
      } catch (e) {
        errorMessage = `서버 오류 (${response.status}): ${response.statusText}`
      }

      throw new Error(errorMessage)
    }

    const result = await response.json()
    console.log('주문 결과:', result)

    if (result.success) {
      alert('주문이 완료되었습니다!')

      // 세션 데이터 정리
      sessionStorage.removeItem('checkout_data')

      // 주문 완료 페이지로 이동
      window.location.href = `/order-complete?orderId=${result.data.orderId}`
    } else {
      throw new Error(result.message || '주문 처리에 실패했습니다.')
    }
  } catch (error) {
    console.error('결제 처리 실패:', error)
    alert(`결제 처리 중 오류가 발생했습니다:\n${error.message}`)
  } finally {
    loading.value = false
  }
}

// 결제 수단명 변환
const getPaymentMethodName = (method) => {
  const methodNames = {
    'card': '컬리페이',
    'kurly': 'Kurly Pay',
    'npay': '네이버페이',
    'general': '일반결제',
    'credit': '신용카드',
    'kakao': '카카오페이',
    'toss': '토스',
    'payco': '페이코'
  }
  return methodNames[method] || '기타'
}

// 컴포넌트 마운트 시 실행 (수정된 버전)
onMounted(async () => {
  console.log('체크아웃 페이지 로드 시작')

  // 로그인 상태 확인 (게스트도 허용)
  checkLoginStatus()

  // 주문 데이터 로드 (필수)
  loadOrderData()

  // 사용자 정보 로드 (옵셔널)
  await loadUserInfo()

  // 배송지 정보 로드 (옵셔널)
  await loadDeliveryInfo()

  console.log('체크아웃 페이지 초기화 완료')
})
</script>

<style scoped>
.checkout-container {
  max-width: 1200px;
  margin: 0 auto;
  background: #f8f9fa;
  min-height: 100vh;
  padding: 0 20px;
  position: relative;
}

.checkout-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: white;
  padding: 16px 0;
  display: flex;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 1px solid #e5e7eb;
}

.back-button {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.2s;
  margin-right: 16px;
}

.back-button:hover {
  background: #f3f4f6;
}

.checkout-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.header-spacer {
  flex: 1;
}

.checkout-content {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 40px;
  margin-bottom: 40px;
  align-items: start;
  min-height: 100vh;
}

.order-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.form-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #e5e7eb;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modify-btn {
  background: none;
  border: 1px solid #d1d5db;
  color: #6b7280;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

/* 주문 상품 */
.order-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
}

.item-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  background: #f3f4f6;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-details {
  flex: 1;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.item-price {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.discount-rate {
  color: #ef4444;
  font-size: 12px;
  font-weight: 700;
}

.final-price {
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.item-quantity {
  font-size: 12px;
  color: #6b7280;
}

.delivery-notice {
  margin-top: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.delivery-text {
  font-size: 14px;
  color: #6b7280;
}

/* 주문자 정보 */
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.form-label {
  min-width: 80px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.form-value {
  font-size: 14px;
  color: #1f2937;
}

.email-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.email-notice {
  font-size: 12px;
  color: #6b7280;
}

.email-notice p {
  margin: 0;
  line-height: 1.4;
}

/* 배송 정보 */
.delivery-address {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.address-label {
  min-width: 80px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.address-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.address-content p {
  margin: 0;
  font-size: 14px;
  color: #1f2937;
  line-height: 1.4;
}

.change-btn, .edit-btn {
  background: none;
  border: 1px solid #d1d5db;
  color: #6b7280;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.delivery-info-section {
  display: flex;
  gap: 16px;
}

.delivery-info-label {
  min-width: 80px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.delivery-options {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.delivery-options span {
  font-size: 14px;
  color: #1f2937;
}

.delivery-options p {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
}

/* 결제 수단 */
.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.payment-option {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.radio-container {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.radio-container input[type="radio"] {
  display: none;
}

.radio-mark {
  width: 18px;
  height: 18px;
  border: 2px solid #d1d5db;
  border-radius: 50%;
  position: relative;
  transition: all 0.2s;
}

.radio-container input[type="radio"]:checked + .radio-mark {
  border-color: #8b5cf6;
  background: white;
}

.radio-container input[type="radio"]:checked + .radio-mark::after {
  content: '';
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8b5cf6;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.payment-label {
  flex: 1;
  color: #1f2937;
}

.recommended-badge {
  background: #8b5cf6;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.new-badge {
  background: #10b981;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.hot-badge {
  background: #ef4444;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  margin-left: 4px;
}

.event-badge {
  background: #f59e0b;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.payment-notice {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}

.sub-payment-methods {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sub-payment-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-options {
  display: flex;
  gap: 8px;
  margin-left: 26px;
}

.card-option-btn {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.card-option-btn.active {
  background: #8b5cf6;
  color: white;
  border-color: #8b5cf6;
}

.payment-notice-section {
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.notice-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.notice-list {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

.notice-list li {
  margin-bottom: 4px;
}

.details-link {
  margin-top: 8px;
  font-size: 12px;
  color: #8b5cf6;
  cursor: pointer;
}

/* 쿠폰 */
.coupon-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coupon-input-field {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.coupon-notice, .coupon-terms {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.coupon-link {
  font-size: 12px;
  color: #3b82f6;
  text-decoration: none;
}

/* 적립금 */
.points-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.points-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.points-label {
  color: #374151;
}

.points-value {
  color: #6b7280;
}

.points-amount {
  color: #1f2937;
  font-weight: 500;
}

.points-input-section {
  display: flex;
  gap: 8px;
  align-items: center;
}

.points-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  text-align: right;
}

.use-all-btn {
  background: none;
  border: 1px solid #d1d5db;
  color: #6b7280;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.points-info {
  font-size: 12px;
  color: #6b7280;
}

.points-info p {
  margin: 0;
  line-height: 1.4;
}

/* 컬리카드 혜택 */
.kurlycard-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fef7cd;
  border-radius: 8px;
  border: 1px solid #f59e0b;
}

.kurlycard-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-status {
  font-size: 12px;
  color: #92400e;
  font-weight: 500;
}

.card-discount {
  font-size: 14px;
  color: #1f2937;
  font-weight: 600;
}

.card-apply-btn {
  background: #f59e0b;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

/* 결제 정보 (오른쪽) - CSS Sticky 방법 */
.payment-summary {
  position: -webkit-sticky;
  position: sticky;
  top: 20px;
  height: fit-content;
  align-self: flex-start;
}

.summary-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
}

.summary-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  padding: 24px 24px 0;
}

.summary-details {
  padding: 20px 24px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.summary-row:last-of-type {
  margin-bottom: 0;
}

.summary-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.summary-value {
  font-size: 14px;
  color: #1f2937;
  font-weight: 600;
}

.summary-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 16px 0;
}

.summary-row.total {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.summary-row.total .summary-label {
  font-weight: 700;
  font-size: 16px;
  color: #1f2937;
}

.summary-row.total .summary-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.benefits-notice {
  padding: 12px 24px;
  background: #f0f9ff;
  border-top: 1px solid #e5e7eb;
  text-align: center;
}

.benefits-notice p {
  margin: 0;
  font-size: 12px;
  color: #0369a1;
}

.checkout-btn {
  width: 100%;
  height: 56px;
  background: #8b5cf6;
  color: white;
  border: none;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 0;
}

.checkout-btn:hover:not(:disabled) {
  background: #7c3aed;
}

.checkout-btn:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.agreement-text {
  padding: 16px 24px;
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
  line-height: 1.4;
}

/* 반응형 디자인 */
@media (max-width: 1024px) {
  .checkout-content {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .payment-summary {
    position: static;
    order: -1;
    top: auto;
  }
}

@media (max-width: 768px) {
  .checkout-container {
    padding: 0 16px;
  }

  .checkout-header {
    padding: 12px 0;
    margin-bottom: 20px;
  }

  .checkout-title {
    font-size: 20px;
  }

  .checkout-content {
    gap: 16px;
  }

  .form-section {
    padding: 20px;
  }

  .section-title {
    font-size: 16px;
  }

  .form-group {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .form-label {
    min-width: auto;
  }

  .delivery-address,
  .delivery-info-section {
    flex-direction: column;
    gap: 8px;
  }

  .address-label,
  .delivery-info-label {
    min-width: auto;
  }

  .address-content {
    flex-direction: column;
    gap: 12px;
  }

  .card-options {
    margin-left: 0;
    flex-wrap: wrap;
  }

  .points-input-section {
    flex-direction: column;
    align-items: stretch;
  }

  .kurlycard-section {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .checkout-container {
    padding: 0 12px;
  }

  .form-section {
    padding: 16px;
  }

  .order-item {
    padding: 12px;
  }

  .item-image {
    width: 50px;
    height: 50px;
  }

  .payment-option {
    padding: 12px;
  }

  .sub-payment-methods {
    margin-top: 12px;
    padding-top: 12px;
  }

  .card-options {
    gap: 4px;
  }

  .card-option-btn {
    padding: 4px 8px;
    font-size: 11px;
  }

  .summary-details {
    padding: 16px 20px;
  }

  .summary-title {
    padding: 20px 20px 0;
    font-size: 16px;
  }
}
</style>
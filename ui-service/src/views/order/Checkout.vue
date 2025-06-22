<template>
  <div class="checkout-container">
    <!-- 헤더 -->
    <div class="checkout-header">
      <button class="back-button" @click="goBack">
        <ChevronLeft :size="24"/>
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
                <img :src="item.image" :alt="item.name"/>
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
<!--          <div class="delivery-notice">-->
<!--            <span class="delivery-text"></span>-->
<!--          </div>-->
        </div>

        <!-- 주문자 정보 -->
        <div class="form-section">
          <h2 class="section-title">주문자 정보</h2>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">보내는 분</label>
              <span class="form-value">{{ userInfo.name }}</span>
            </div>
            <div class="form-group">
              <label class="form-label">휴대폰</label>
              <span class="form-value">{{ userInfo.phone }}</span>
            </div>
            <div class="form-group">
              <label class="form-label">이메일</label>
              <div class="email-info">
                <span class="form-value">{{ userInfo.email }}</span>
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
              <p>
                <template v-if="deliveryInfo.zipCode">
                  ({{ deliveryInfo.zipCode }}) {{ deliveryInfo.address }}<br>
                  {{ deliveryInfo.detailAddress }}
                </template>
                <template v-else>
                  {{ deliveryInfo.address || '서울특별시 송파구 정현로 135 (어마덜랩터원) 7층 16층 한국스프트에이전시협의회' }}
                </template>
              </p>
              <button class="change-btn" @click="openAddressModal">변경</button>
            </div>
          </div>

          <div class="delivery-info-section">
            <div class="delivery-info-label">배송 요청사항</div>
            <div class="delivery-options">
              <span>{{ deliveryInfo.request || '빠짐 · 공동현관 비밀번호 (권장)' }}</span>
              <p>{{ deliveryInfo.recipientName || userInfo.name || '-' }},
                {{ deliveryInfo.recipientPhone || userInfo.phone || '-' }}</p>
              <button class="edit-btn" @click="editDeliveryRequest">수정</button>
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

        <!--        &lt;!&ndash; 쿠폰 &ndash;&gt;-->
        <!--        <div class="form-section">-->
        <!--          <h2 class="section-title">쿠폰</h2>-->
        <!--          <div class="coupon-section">-->
        <!--            <div class="coupon-input">-->
        <!--              <input type="text" placeholder="쿠폰 번호를 입력 해주세요 / 쿠폰명 입력" class="coupon-input-field">-->
        <!--            </div>-->
        <!--            <p class="coupon-notice">쿠폰 적용 시 샛별배송 지역이 안됩니다.</p>-->
        <!--            <p class="coupon-terms">쿠폰 이용 문의 (#칼리컬러) *</p>-->
        <!--            <a href="#" class="coupon-link">더민컬러츠 할인정보로 해결 무료배송 ></a>-->
        <!--          </div>-->
        <!--        </div>-->

        <!-- 적립금/컬리패스 -->
        <div class="form-section">
          <h2 class="section-title">적립금</h2>
          <div class="points-section">
            <div class="points-row">
              <span class="points-label">적립금 </span>
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

    <!-- 주소 변경 모달 -->
    <div v-if="showAddressModal" class="address-modal-overlay" @click="closeAddressModal">
      <div class="address-modal" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">배송지 변경</h3>
          <button class="close-btn" @click="closeAddressModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m18 6-12 12"/>
              <path d="m6 6 12 12"/>
            </svg>
          </button>
        </div>

        <div class="address-search-section">
          <div class="address-info">
            <p>우편번호를 검색하고 상세주소를 입력해주세요.</p>
          </div>

          <div class="search-input-group">
            <input
                type="text"
                v-model="newAddress.zipCode"
                placeholder="우편번호"
                class="search-input"
                readonly
            >
            <button class="search-btn" @click="searchAddress">우편번호 검색</button>
          </div>

          <input
              type="text"
              v-model="newAddress.address"
              placeholder="기본주소"
              class="search-input"
              readonly
              style="margin-bottom: 8px;"
          >

          <div class="detail-address-group">
            <input
                type="text"
                v-model="newAddress.detailAddress"
                placeholder="상세주소를 입력하세요"
                class="detail-address-input"
                @keyup.enter="saveAddress"
            >
          </div>
        </div>

        <div class="modal-actions">
          <button class="cancel-btn" @click="closeAddressModal">취소</button>
          <button
              class="save-btn"
              @click="saveAddress"
              :disabled="!canSaveAddress"
          >
            적용
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue'
import {ChevronLeft} from 'lucide-vue-next'
import axios from 'axios'
import {user, setUserFromToken} from "@/stores/userStore"

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// 상태 관리
const selectedPayment = ref('general')
const selectedSubPayment = ref('credit')
const pointsToUse = ref(0)

// 주소 모달 관련
const showAddressModal = ref(false)
const newAddress = ref({
  zipCode: '',
  address: '',
  detailAddress: ''
})

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
const deliveryFee = ref(0)
const couponDiscount = ref(0)
const cardDiscount = ref(0)
const pointsUsed = ref(0)
const benefitAmount = ref(0)
const kurlypassAmount = ref(0)

// 인증 관련
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

const canSaveAddress = computed(() => {
  return newAddress.value.zipCode &&
      newAddress.value.address &&
      newAddress.value.detailAddress.trim()
})

// 주소 모달 관련 함수들
const openAddressModal = () => {
  showAddressModal.value = true
  newAddress.value = {
    zipCode: deliveryInfo.value.zipCode || '',
    address: deliveryInfo.value.address || '',
    detailAddress: deliveryInfo.value.detailAddress || ''
  }
}

const closeAddressModal = () => {
  showAddressModal.value = false
  newAddress.value = {
    zipCode: '',
    address: '',
    detailAddress: ''
  }
}

// 카카오 API 동적 로드
const loadKakaoScript = () => {
  return new Promise((resolve, reject) => {
    if (typeof daum !== 'undefined' && daum.Postcode) {
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('카카오 API 로드 실패'))

    document.head.appendChild(script)
  })
}

// 주소 검색
const searchAddress = async () => {
  try {
    if (typeof daum === 'undefined' || !daum.Postcode) {
      await loadKakaoScript()
    }

    new daum.Postcode({
      oncomplete: function (data) {
        newAddress.value.zipCode = data.zonecode
        newAddress.value.address = data.address

        setTimeout(() => {
          const detailInput = document.querySelector('.detail-address-input')
          if (detailInput) {
            detailInput.focus()
          }
        }, 100)
      }
    }).open()
  } catch (error) {
    alert('우편번호 검색 서비스를 로드할 수 없습니다.')
  }
}

const saveAddress = () => {
  if (!canSaveAddress.value) {
    alert('우편번호, 기본주소, 상세주소를 모두 입력해주세요.')
    return
  }

  deliveryInfo.value = {
    ...deliveryInfo.value,
    zipCode: newAddress.value.zipCode,
    address: newAddress.value.address,
    detailAddress: newAddress.value.detailAddress
  }

  closeAddressModal()
}

const editDeliveryRequest = () => {
  const newRequest = prompt('배송 요청사항을 입력하세요:', deliveryInfo.value.request)
  if (newRequest !== null) {
    deliveryInfo.value.request = newRequest.trim() || '문 앞에 놓아주세요'
  }
}

// 안전한 Base64 디코딩 함수
const safeBase64Decode = (str) => {
  try {
    let base64 = str.replace(/-/g, '+').replace(/_/g, '/')
    while (base64.length % 4) {
      base64 += '='
    }
    return atob(base64)
  } catch (error) {
    throw error
  }
}

// 토큰 유효성 검사 함수
const isTokenValid = (token) => {
  if (!token) return false

  try {
    const parts = token.split('.')
    if (parts.length !== 3) return false

    const payloadStr = safeBase64Decode(parts[1])
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

// 인증 헤더 생성
const getAuthHeaders = () => {
  const token = localStorage.getItem('token')

  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': user.id || 'guest_' + Date.now()
  }

  if (token && isTokenValid(token) && user.id) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

// 로그인 상태 확인
const checkLoginStatus = () => {
  const token = localStorage.getItem('token')

  console.log('=== 로그인 상태 확인 ===')
  console.log('Token exists:', !!token)

  if (token && isTokenValid(token)) {
    setUserFromToken(token)
    console.log('After setUserFromToken - user.id:', user.id)
    console.log('After setUserFromToken - user:', user)
    isLoggedIn.value = !!user.id
  } else {
    isLoggedIn.value = false
    if (token && !isTokenValid(token)) {
      localStorage.removeItem('token')
    }
  }

  console.log('Final isLoggedIn:', isLoggedIn.value)
  console.log('========================')

  return isLoggedIn.value
}

// 사용자 정보 로드
const loadUserInfo = async () => {
  if (!isLoggedIn.value) {
    userInfo.value = {
      name: '게스트 사용자',
      phone: '',
      email: ''
    }

    deliveryInfo.value = {
      address: '서울특별시 송파구 정현로 135',
      detailAddress: '(어마덜랩터원) 7층 16층 한국스프트에이전시협의회',
      zipCode: '05506',
      request: '문 앞에 놓아주세요',
      recipientName: '게스트 사용자',
      recipientPhone: ''
    }
    return
  }

  userInfo.value = {
    name: user.name || '사용자',
    phone: user.phone || '',
    email: user.email || ''
  }

  try {
    const response = await axios.get(`${API_BASE_URL}/api/users/profile`, {
      headers: getAuthHeaders()
    })

    if (response.data.success) {
      const userData = response.data.data
      userInfo.value = {
        name: userData.name || user.name || '사용자',
        phone: userData.phone || user.phone || '',
        email: userData.email || user.email || ''
      }

      if (userData.zipcode || userData.address) {
        deliveryInfo.value = {
          address: userData.address || '',
          detailAddress: userData.myaddress || '',
          zipCode: userData.zipcode || '',
          request: '문 앞에 놓아주세요',
          recipientName: userData.name || user.name,
          recipientPhone: userData.phone || user.phone || ''
        }
      }
    }
  } catch (error) {
    // API 실패해도 로그인 상태 유지
  }
}

// 배송지 정보 로드
const loadDeliveryInfo = async () => {
  if (!isLoggedIn.value) return
  if (deliveryInfo.value.address) return

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
    // 로그인 상태 유지
  }
}

// 주문 데이터 로드
const loadOrderData = () => {
  try {
    const checkoutData = sessionStorage.getItem('checkout_data')
    if (checkoutData) {
      const data = JSON.parse(checkoutData)

      if (data.items && data.items.length > 0) {
        orderItems.value = data.items
        orderAmount.value = data.totalPrice || data.totalAmount || 0
        productAmount.value = data.productAmount || orderAmount.value
        deliveryFee.value = data.deliveryFee || 0
      } else {
        alert('주문할 상품이 없습니다. 장바구니로 이동합니다.')
        window.location.href = '/cart'
      }
    } else {
      alert('주문 정보를 찾을 수 없습니다. 장바구니로 이동합니다.')
      window.location.href = '/cart'
    }
  } catch (error) {
    alert('주문 정보 로드 중 오류가 발생했습니다.')
    window.location.href = '/cart'
  }
}

// 메서드들
const goBack = () => {
  window.history.back()
}

const formatPrice = (price) => {
  return price?.toLocaleString() || '0'
}

// 결제 처리
const processPayment = async () => {
  console.log('=== processPayment 함수 시작 ===')

  if (!canProceed.value) {
    alert('결제 정보를 확인해주세요.')
    return
  }

  try {
    loading.value = true

    // 1. 주문 생성 (기존 로직)
    const orderData = {
      userId: user.id && user.id !== 'null' ? user.id : undefined,
      items: orderItems.value.map(item => ({
        productId: item.productId || item.id,
        productName: item.name,
        quantity: item.quantity,
        unitPrice: item.salePrice,
        totalPrice: item.salePrice * item.quantity,
        imageUrl: item.image || ''
      })),
      phone: userInfo.value.phone || '',
      email: userInfo.value.email || '',
      recipientName: deliveryInfo.value.recipientName || userInfo.value.name || '수령인',
      recipientPhone: deliveryInfo.value.recipientPhone || userInfo.value.phone || '',
      orderZipcode: deliveryInfo.value.zipCode || '',
      orderAddressDetail: deliveryInfo.value.address ?
          (deliveryInfo.value.address + ' ' + deliveryInfo.value.detailAddress).trim() : '',
      deliveryMemo: deliveryInfo.value.request || '',
      paymentMethod: selectedPayment.value,
      paymentMethodName: getPaymentMethodName(selectedPayment.value),
      usedPoint: pointsToUse.value || 0,
      totalAmount: finalAmount.value
    }

    console.log('📤 주문 생성 요청:', orderData)

    const orderResponse = await fetch(`${API_BASE_URL}/api/orders/checkout`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(orderData)
    })

    if (!orderResponse.ok) {
      const errorData = await orderResponse.json()
      throw new Error(errorData.message || '주문 생성 실패')
    }

    const orderResult = await orderResponse.json()
    console.log('✅ 주문 생성 성공:', orderResult)

    if (orderResult.success) {
      // 2. PG사 결제 호출
      console.log('💳 PG 결제 시작')
      await initiatePayment({
        orderId: orderResult.data.orderId,
        amount: finalAmount.value,
        orderName: `주문 ${orderResult.data.orderId}`,
        userEmail: userInfo.value.email,
        userName: userInfo.value.name,
        userPhone: userInfo.value.phone
      })
    }
  } catch (error) {
    console.log('❌ 에러 발생:', error)
    alert(`주문 처리 중 오류가 발생했습니다:\n${error.message}`)
  } finally {
    loading.value = false
  }
}

// PG 결제 함수 (아임포트 예시)
// Checkout.vue에서 아임포트 동적 로드
const loadIamportScript = () => {
  return new Promise((resolve, reject) => {
    // 이미 로드되었는지 확인
    if (typeof window.IMP !== 'undefined') {
      resolve(window.IMP)
      return
    }

    // 스크립트 태그 생성
    const script = document.createElement('script')
    script.src = 'https://cdn.iamport.kr/v1/iamport.js'
    script.onload = () => {
      if (typeof window.IMP !== 'undefined') {
        console.log('✅ 아임포트 로드 완료')
        resolve(window.IMP)
      } else {
        reject(new Error('아임포트 로드 실패'))
      }
    }
    script.onerror = () => reject(new Error('아임포트 스크립트 로드 에러'))

    document.head.appendChild(script)
  })
}

// initiatePayment 함수 수정
const initiatePayment = async (paymentData) => {
  try {
    console.log('📦 아임포트 스크립트 로드 중...')
    const IMP = await loadIamportScript()

    return new Promise((resolve, reject) => {
      console.log('✅ 아임포트 모듈 확인:', typeof IMP)

      // 공식 테스트 가맹점 코드 사용
      IMP.init('imp19424728')

      console.log('🚀 결제창 호출 시작')

      IMP.request_pay({
        pg: 'kakaopay.TC0ONETIME', // 카카오페이 테스트
        pay_method: 'card',
        merchant_uid: paymentData.orderId,
        name: paymentData.orderName,
        amount: 100, // 테스트용 100원
        buyer_email: paymentData.userEmail,
        buyer_name: paymentData.userName,
        buyer_tel: paymentData.userPhone,
      }, async (response) => {
        try {
          console.log('💳 결제 응답:', response)

          if (response.success) {
            console.log('✅ 결제 성공!')
            console.log('결제 고유ID:', response.imp_uid)
            console.log('주문번호:', response.merchant_uid)

            // 🎉 결제 성공 처리
            alert(`결제가 완료되었습니다!\n결제 금액: ${response.paid_amount}원`)

            // 체크아웃 데이터 정리
            sessionStorage.removeItem('checkout_data')

            // 주문 완료 페이지로 이동
            window.location.href = `/order-complete?orderId=${response.merchant_uid}&paymentId=${response.imp_uid}`

            resolve(response)

          } else {
            // ❌ 결제 실패 처리
            console.log('❌ 결제 실패:', response)

            let failureReason = '알 수 없는 오류'
            switch(response.error_code) {
              case 'STOP_PAYMENT':
                failureReason = '사용자가 결제를 취소했습니다'
                break
              case 'CARD_DECLINED':
                failureReason = '카드 결제가 거절되었습니다'
                break
              case 'INSUFFICIENT_FUNDS':
                failureReason = '잔액이 부족합니다'
                break
              default:
                failureReason = response.error_msg || '결제 처리 중 오류가 발생했습니다'
            }

            alert(`결제 실패: ${failureReason}`)
            reject(new Error(failureReason))
          }

        } catch (error) {
          console.error('결제 응답 처리 중 오류:', error)
          alert(`결제 처리 중 시스템 오류가 발생했습니다: ${error.message}`)
          reject(error)
        }
      })
    })

  } catch (error) {
    console.error('아임포트 초기화 실패:', error)
    alert('결제 시스템을 초기화할 수 없습니다. 잠시 후 다시 시도해주세요.')
    throw error
  }
}

// 결제 검증 함수
const verifyPayment = async (impUid, merchantUid) => {
  try {
    console.log('🔍 결제 검증 시작:', {impUid, merchantUid})

    const response = await fetch(`${API_BASE_URL}/api/payments/verify`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        impUid: impUid,
        merchantUid: merchantUid
      })
    })

    if (!response.ok) {
      throw new Error('결제 검증 실패')
    }

    const result = await response.json()
    console.log('✅ 결제 검증 완료:', result)

    if (result.success) {
      alert('결제가 완료되었습니다!')
      sessionStorage.removeItem('checkout_data')
      window.location.href = `/order-complete?orderId=${merchantUid}`
    } else {
      throw new Error(result.message || '결제 검증 실패')
    }
  } catch (error) {
    console.error('결제 검증 오류:', error)
    alert(`결제 검증 중 오류가 발생했습니다: ${error.message}`)
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

// 컴포넌트 마운트
onMounted(async () => {
  checkLoginStatus()
  loadOrderData()
  await loadUserInfo()
  await loadDeliveryInfo()
})
</script>


<style scoped src="@/assets/css/checkout.css"></style>


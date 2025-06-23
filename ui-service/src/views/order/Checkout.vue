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

                <!-- 🔥 개선된 카드 옵션 버튼들 -->
                <div v-if="selectedSubPayment === 'credit'" class="card-options">
                  <button
                      class="card-option-btn"
                      :class="{ active: cardPaymentType === 'card' }"
                      @click="setCardPaymentType('card')"
                      type="button"
                  >
                    신용카드
                  </button>
                  <button
                      class="card-option-btn"
                      :class="{ active: cardPaymentType === 'simple' }"
                      @click="setCardPaymentType('simple')"
                      type="button"
                  >
                    간편결제
                  </button>
                  <button
                      class="card-option-btn"
                      :class="{ active: cardPaymentType === 'phone' }"
                      @click="setCardPaymentType('phone')"
                      type="button"
                  >
                    휴대폰
                  </button>
                </div>

                <!-- 🔥 선택된 결제 방식에 따른 안내 문구 -->
                <div v-if="selectedSubPayment === 'credit'" class="payment-guide">
                  <div v-if="cardPaymentType === 'card'" class="guide-text">
                    💳 신용카드 또는 체크카드로 결제합니다
                  </div>
                  <div v-else-if="cardPaymentType === 'simple'" class="guide-text">
                    ⚡ 삼성페이, 애플페이 등 간편결제로 결제합니다
                  </div>
                  <div v-else-if="cardPaymentType === 'phone'" class="guide-text">
                    📱 휴대폰 소액결제로 결제합니다 (통신사 요금에 합산)
                  </div>
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
              <li v-if="cardPaymentType === 'phone'" class="phone-notice">📱 휴대폰 결제는 월 30만원 한도가 있습니다</li>
            </ul>
            <p class="details-link">자세히보기</p>
          </div>
        </div>

        <!-- 적립금 -->
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

            <div class="points-input-section">
              <input type="number" placeholder="0" class="points-input" v-model="pointsToUse">
              <button class="use-all-btn">전액사용</button>
            </div>

            <div class="points-info">
              <p>적립금만 입력해주십시오 또는 사용이 안되시면.</p>
            </div>
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
              <span class="summary-label">혜택금</span>
              <span class="summary-value">{{ formatPrice(benefitAmount) }}원</span>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-row total">
              <span class="summary-label">최종 결제금액</span>
              <span class="summary-value">{{ formatPrice(finalAmount) }}원</span>
            </div>
          </div>

          <div class="benefits-notice">
            <p> 최종 결제 시 여시기 2,000원 더 받기 ></p>
          </div>

          <button class="checkout-btn" @click="processPayment" :disabled="!canProceed || loading">
            <span v-if="loading">결제 처리 중...</span>
            <span v-else>{{ formatPrice(finalAmount) }}원 결제하기</span>
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

// 공통 유틸 import
import {
  getFailureReason,
  getSuccessMessage,
  getPgDisplayName,
  getMessageType
} from '@/utils/paymentMessages.js'

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// 🔥 결제 타입 상태 추가
const cardPaymentType = ref('card') // 'card', 'simple', 'phone'

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

// 🔥 중복 메시지 방지
let lastMessage = null
let lastMessageTime = 0

const showFriendlyMessage = (message, type = 'info') => {
  const now = Date.now()

  // 같은 메시지가 1초 이내에 호출되면 무시
  if (lastMessage === message && (now - lastMessageTime) < 1000) {
    console.log('🔄 중복 메시지 방지:', message)
    return
  }

  lastMessage = message
  lastMessageTime = now

  const icons = {
    success: '🎉',
    info: '💡',
    warning: '⚠️',
    error: '❌'
  }

  const icon = icons[type] || '💡'
  alert(`${icon} ${message}`)

  console.log(`💬 ${type.toUpperCase()}: ${message}`)
}

// 🔥 카드 결제 타입 설정 함수
const setCardPaymentType = (type) => {
  cardPaymentType.value = type

  // 신용/체크카드가 선택되지 않았다면 자동 선택
  if (selectedSubPayment.value !== 'credit') {
    selectedSubPayment.value = 'credit'
  }

  console.log(`카드 결제 타입 변경: ${type}`)

  // 사용자에게 선택 확인 메시지
  const typeNames = {
    'card': '신용카드',
    'simple': '간편결제',
    'phone': '휴대폰 결제'
  }

  showFriendlyMessage(`${typeNames[type]}가 선택되었습니다! 💳`, 'info')
}

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
    showFriendlyMessage('우편번호 검색 서비스를 로드할 수 없습니다.', 'error')
  }
}

const saveAddress = () => {
  if (!canSaveAddress.value) {
    showFriendlyMessage('우편번호, 기본주소, 상세주소를 모두 입력해주세요.', 'warning')
    return
  }

  deliveryInfo.value = {
    ...deliveryInfo.value,
    zipCode: newAddress.value.zipCode,
    address: newAddress.value.address,
    detailAddress: newAddress.value.detailAddress
  }

  closeAddressModal()
  showFriendlyMessage('배송지가 변경되었습니다! ', 'success')
}

const editDeliveryRequest = () => {
  const newRequest = prompt('배송 요청사항을 입력하세요:', deliveryInfo.value.request)
  if (newRequest !== null) {
    deliveryInfo.value.request = newRequest.trim() || '문 앞에 놓아주세요'
    showFriendlyMessage('배송 요청사항이 변경되었습니다! 📝', 'info')
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
        showFriendlyMessage('주문할 상품이 없습니다. 장바구니로 이동합니다.', 'warning')
        window.location.href = '/cart'
      }
    } else {
      showFriendlyMessage('주문 정보를 찾을 수 없습니다. 장바구니로 이동합니다.', 'warning')
      window.location.href = '/cart'
    }
  } catch (error) {
    showFriendlyMessage('주문 정보 로드 중 오류가 발생했습니다.', 'error')
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

// 아임포트 동적 로드
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
        console.log('아임포트 로드 완료')
        resolve(window.IMP)
      } else {
        reject(new Error('아임포트 로드 실패'))
      }
    }
    script.onerror = () => reject(new Error('아임포트 스크립트 로드 에러'))

    document.head.appendChild(script)
  })
}

// 🔥 결제 유효성 검사 개선
const validatePaymentMethod = () => {
  if (!selectedPayment.value) {
    showFriendlyMessage('결제 수단을 선택해주세요.', 'warning')
    return false
  }

  if (selectedPayment.value === 'general' && !selectedSubPayment.value) {
    showFriendlyMessage('세부 결제 방식을 선택해주세요.', 'warning')
    return false
  }

  // 휴대폰 결제 시 추가 검증
  if (selectedSubPayment.value === 'credit' && cardPaymentType.value === 'phone') {
    if (!userInfo.value.phone) {
      showFriendlyMessage('휴대폰 결제를 위해 휴대폰 번호가 필요합니다.', 'warning')
      return false
    }
  }

  return true
}

// 🔥 개선된 PG 결제 함수 - 카드 타입별 처리
const initiatePayment = async (paymentData) => {
  try {
    console.log('아임포트 스크립트 로드 중...')
    const IMP = await loadIamportScript()

    return new Promise((resolve, reject) => {
      IMP.init('imp19424728')

      // PG사 선택 로직 개선
      let pgProvider = 'kakaopay.TC0ONETIME'
      let payMethod = 'card'

      if (selectedPayment.value === 'general') {
        switch(selectedSubPayment.value) {
          case 'credit':
            // 🔥 카드 타입별 처리
            if (cardPaymentType.value === 'phone') {
              pgProvider = 'html5_inicis'
              payMethod = 'phone' // 휴대폰 결제
            } else if (cardPaymentType.value === 'simple') {
              pgProvider = 'html5_inicis'
              payMethod = 'samsung' // 삼성페이 등 간편결제
            } else {
              pgProvider = 'html5_inicis'
              payMethod = 'card' // 일반 신용카드
            }
            break
          case 'kakao':
            pgProvider = 'kakaopay.TC0ONETIME'
            payMethod = 'card'
            break
          case 'toss':
            pgProvider = 'tosspay.tosstest'
            payMethod = 'card'
            break
          case 'payco':
            pgProvider = 'payco.PARTNERTEST'
            payMethod = 'card'
            break
          default:
            pgProvider = 'html5_inicis'
            payMethod = 'card'
        }
      } else if (selectedPayment.value === 'kurly') {
        pgProvider = 'kcp.T0000'
        payMethod = 'card'
      }

      const actualAmount = paymentData.amount || finalAmount.value

      console.log(`선택된 PG: ${pgProvider}`)
      console.log(`결제 방식: ${payMethod}`)
      console.log(`카드 타입: ${cardPaymentType.value}`)
      console.log(`결제 금액: ${actualAmount.toLocaleString()}원`)

      // 🔥 결제 요청 데이터 구성
      const paymentRequest = {
        pg: pgProvider,
        pay_method: payMethod,
        merchant_uid: paymentData.orderId,
        name: paymentData.orderName,
        amount: actualAmount,
        buyer_email: paymentData.userEmail,
        buyer_name: paymentData.userName,
        buyer_tel: paymentData.userPhone,
        custom_data: {
          finalAmount: actualAmount,
          originalAmount: orderAmount.value,
          deliveryFee: deliveryFee.value,
          cardType: cardPaymentType.value
        }
      }

      // 🔥 휴대폰 결제 시 추가 설정
      if (payMethod === 'phone') {
        paymentRequest.digital = false // 실물 상품
        paymentRequest.buyer_postcode = deliveryInfo.value.zipCode || ''
        paymentRequest.buyer_addr = deliveryInfo.value.address || ''
      }

      IMP.request_pay(paymentRequest, async (response) => {
        try {
          console.log(`${pgProvider} 결제 응답:`, response)

          if (response.success) {
            console.log('✅ 결제 성공!')

            // 공통 유틸 사용 - 성공 메시지
            const successMsg = getSuccessMessage(pgProvider, response.paid_amount)
            showFriendlyMessage(successMsg, 'success')

            sessionStorage.removeItem('checkout_data')
            window.location.href = `/order-complete?orderId=${response.merchant_uid}&paymentId=${response.imp_uid}&amount=${response.paid_amount}`
            resolve(response)

          } else {
            console.log('❌ 결제 실패:', response)

            // 🔥 공통 유틸 사용 - 실패 메시지 (한 번만 호출)
            const friendlyReason = getFailureReason(response.error_code, response.error_msg)
            const messageType = getMessageType(response.error_code, response.error_msg)

            showFriendlyMessage(friendlyReason, messageType)

            // reject로 에러 전달하되, 메시지는 이미 표시했으므로 별도 처리 안함
            const error = new Error(friendlyReason)
            error.alreadyHandled = true
            reject(error)
          }

        } catch (error) {
          console.error('결제 응답 처리 중 오류:', error)

          if (!error.alreadyHandled) {
            const errorMsg = getFailureReason('SYSTEM_ERROR', '결제 처리 중 오류가 발생했습니다')
            showFriendlyMessage(errorMsg, 'error')
          }
          reject(error)
        }
      })
    })

  } catch (error) {
    console.error('아임포트 초기화 실패:', error)
    const errorMsg = getFailureReason('SYSTEM_ERROR', '결제 시스템 초기화 실패')
    showFriendlyMessage(errorMsg, 'error')
    throw error
  }
}

// 🔥 개선된 결제 수단명 변환
const getPaymentMethodName = (method) => {
  if (method === 'general' && selectedSubPayment.value === 'credit') {
    const typeNames = {
      'card': '신용카드',
      'simple': '간편결제',
      'phone': '휴대폰결제'
    }
    return typeNames[cardPaymentType.value] || '신용카드'
  }

  const methodNames = {
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

// 🔥 개선된 결제 처리 메인 함수
const processPayment = async () => {
  console.log('=== processPayment 함수 시작 ===')
  console.log(`최종 결제 금액: ${finalAmount.value.toLocaleString()}원`)
  console.log(`선택된 결제방식: ${selectedPayment.value} > ${selectedSubPayment.value} > ${cardPaymentType.value}`)

  // 결제 방식 유효성 검사
  if (!validatePaymentMethod()) {
    return
  }

  if (!canProceed.value) {
    showFriendlyMessage('결제 정보를 확인해주세요.', 'warning')
    return
  }

  try {
    loading.value = true

    // 주문 생성 데이터 준비
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
      usedPoint: pointsUsed.value || 0,
      totalAmount: finalAmount.value,
      // 🔥 추가 결제 정보
      cardType: cardPaymentType.value
    }

    console.log('주문 생성 요청:', orderData)

    // Payment Service의 주문 API 호출
    const orderResponse = await fetch(`${API_BASE_URL}/api/payments/orders/checkout`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(orderData)
    })

    if (!orderResponse.ok) {
      const errorData = await orderResponse.json()
      throw new Error(errorData.message || '주문 생성 실패')
    }

    const orderResult = await orderResponse.json()
    console.log('주문 생성 성공:', orderResult)

    if (orderResult.success) {
      // PG사 결제 호출
      console.log('PG 결제 시작')
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

    if (!error.alreadyHandled) {
      const friendlyError = getFailureReason(null, error.message)
      showFriendlyMessage(friendlyError, 'error')
    }
  } finally {
    loading.value = false
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
      showFriendlyMessage('결제가 완료되었습니다! 🎉', 'success')
      sessionStorage.removeItem('checkout_data')
      window.location.href = `/order-complete?orderId=${merchantUid}`
    } else {
      throw new Error(result.message || '결제 검증 실패')
    }
  } catch (error) {
    console.error('결제 검증 오류:', error)
    const errorMsg = getFailureReason('SYSTEM_ERROR', `결제 검증 중 오류가 발생했습니다: ${error.message}`)
    showFriendlyMessage(errorMsg, 'error')
  }
}

// 컴포넌트 마운트
onMounted(async () => {
  try {
    checkLoginStatus()
    loadOrderData()
    await loadUserInfo()
    await loadDeliveryInfo()

    console.log('✅ 체크아웃 페이지 초기화 완료')
  } catch (error) {
    console.error('초기화 중 오류:', error)
    showFriendlyMessage('페이지 로드 중 문제가 발생했습니다.', 'error')
  }
})
</script>

<style scoped src="@/assets/css/checkout.css"></style>
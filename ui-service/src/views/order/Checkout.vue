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
            <!-- 다른 결제수단 상세 -->
            <div v-if="selectedPayment === 'general'" class="sub-payment-methods">
              <div class="sub-payment-group">
                <label class="radio-container">
                  <input type="radio" name="subPayment" value="credit" v-model="selectedSubPayment" checked>
                  <span class="radio-mark"></span>
                  <span class="payment-label">신용/체크카드</span>
                </label>

                <!-- 개선된 카드 옵션 버튼들 -->
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

                <!-- 선택된 결제 방식에 따른 안내 문구 -->
                <div v-if="selectedSubPayment === 'credit'" class="payment-guide">
                  <div v-if="cardPaymentType === 'card'" class="guide-text">
                    신용카드 또는 체크카드로 결제합니다
                  </div>
                  <div v-else-if="cardPaymentType === 'simple'" class="guide-text">
                    삼성페이, 애플페이 등 간편결제로 결제합니다
                  </div>
                  <div v-else-if="cardPaymentType === 'phone'" class="guide-text">
                    휴대폰 소액결제로 결제합니다 (통신사 요금에 합산)
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
                  <input type="radio" name="subPayment" value="payco" v-model="selectedSubPayment">
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
              <li v-if="cardPaymentType === 'phone'" class="phone-notice">휴대폰 결제는 월 30만원 한도가 있습니다</li>
            </ul>
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
              <span class="summary-label">배송비</span>
              <span class="summary-value">{{ formatPrice(deliveryFee) }}원</span>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-row total">
              <span class="summary-label">최종 결제금액</span>
              <span class="summary-value">{{ formatPrice(finalAmount) }}원</span>
            </div>
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
import apiClient from '@/api/axiosInstance'
import { user, setUserFromToken, backupNameForPayment, restoreNameAfterPayment } from "@/stores/userStore"

import {
  getFailureReason,
  getSuccessMessage,
  getPgDisplayName,
  getMessageType
} from '@/utils/paymentMessages.js'

const cardPaymentType = ref('card')
const selectedPayment = ref('general')
const selectedSubPayment = ref('credit')
const pointsToUse = ref(0)
const showAddressModal = ref(false)
const newAddress = ref({
  zipCode: '',
  address: '',
  detailAddress: ''
})

const userInfo = ref({
  name: '',
  phone: '',
  email: ''
})

const deliveryInfo = ref({
  address: '',
  detailAddress: '',
  zipCode: '',
  request: '',
  recipientName: '',
  recipientPhone: ''
})

const orderItems = ref([])
const orderAmount = ref(0)
const productAmount = ref(0)
const discountAmount = ref(0)
const deliveryFee = ref(0)
const couponDiscount = ref(0)
const cardDiscount = ref(0)
const pointsUsed = ref(0)
const benefitAmount = ref(0)
const kurlypassAmount = ref(0)
const isLoggedIn = ref(false)
const authError = ref('')
const loading = ref(false)

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

// 중복 메시지 방지
let lastMessage = null
let lastMessageTime = 0

const showFriendlyMessage = (message, type = 'info') => {
  const now = Date.now()
  if (lastMessage === message && (now - lastMessageTime) < 1000) {
    return
  }
  lastMessage = message
  lastMessageTime = now
  alert(message)
}

const setCardPaymentType = (type) => {
  cardPaymentType.value = type
  if (selectedSubPayment.value !== 'credit') {
    selectedSubPayment.value = 'credit'
  }
  const typeNames = {
    'card': '신용카드',
    'simple': '간편결제',
    'phone': '휴대폰 결제'
  }
  showFriendlyMessage(`${typeNames[type]}가 선택되었습니다!`, 'info')
}

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
  showFriendlyMessage('배송지가 변경되었습니다!', 'success')
}

const editDeliveryRequest = () => {
  const newRequest = prompt('배송 요청사항을 입력하세요:', deliveryInfo.value.request)
  if (newRequest !== null) {
    deliveryInfo.value.request = newRequest.trim() || '문 앞에 놓아주세요'
    showFriendlyMessage('배송 요청사항이 변경되었습니다!', 'info')
  }
}

const isTokenValid = (token) => {
  if (!token || typeof token !== 'string') return false

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

    if (!payload.sub && !payload.username) {
      return false
    }

    return true
  } catch (error) {
    return false
  }
}

const checkLoginStatus = () => {
  const token = localStorage.getItem('token')

  if (!token) {
    isLoggedIn.value = false
    return false
  }

  if (!isTokenValid(token)) {
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    isLoggedIn.value = false
    return false
  }

  // 토큰에서 사용자 정보 설정
  try {
    setUserFromToken(token)
    isLoggedIn.value = !!user.id
    return isLoggedIn.value
  } catch (error) {
    isLoggedIn.value = false
    return false
  }
}

const loadUserInfo = async () => {
  try {
    if (!isLoggedIn.value) {
      userInfo.value = {
        name: '게스트 사용자',
        phone: '',
        email: ''
      }
      setDefaultDeliveryInfo('게스트 사용자', '')
      return
    }

    // 1단계: userStore와 저장소에서 기본 정보 수집
    let finalUserName = user.name || '사용자'
    let finalUserEmail = user.email || localStorage.getItem('user_email') || sessionStorage.getItem('user_email') || ''
    let finalUserPhone = user.phone || localStorage.getItem('user_phone') || sessionStorage.getItem('user_phone') || ''

    // 2단계: 토큰에서 추가 정보 추출
    const token = localStorage.getItem('token')
    if (token) {
      try {
        const parts = token.replace('Bearer ', '').split('.')
        if (parts.length === 3) {
          let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
          while (base64.length % 4) {
            base64 += '='
          }
          const payload = JSON.parse(atob(base64))

          // 이름 보완
          if (!finalUserName || finalUserName === '사용자') {
            if (payload.name && payload.name.trim() && payload.name !== payload.sub) {
              finalUserName = payload.name
            }
          }

          // 이메일 보완
          if (!finalUserEmail) {
            const emailFields = ['email', 'mail', 'userEmail', 'emailAddress']
            for (const field of emailFields) {
              if (payload[field]) {
                finalUserEmail = payload[field]
                break
              }
            }
          }

          // 휴대폰 보완
          if (!finalUserPhone) {
            const phoneFields = ['phone', 'phoneNumber', 'mobile', 'userPhone', 'tel', 'cellphone']
            for (const field of phoneFields) {
              if (payload[field]) {
                finalUserPhone = payload[field]
                break
              }
            }
          }
        }
      } catch (e) {
        // 토큰 파싱 실패 무시
      }
    }

    // 3단계: API 호출로 정보 보완 (필요한 경우에만)
    const hasCompleteInfo = finalUserEmail && finalUserPhone
    if (!hasCompleteInfo) {
      try {
        const response = await apiClient.get('/api/users/profile', {
          timeout: 3000,
          validateStatus: function (status) {
            return status < 500
          }
        })

        if (response.status === 200 && response.data?.success && response.data?.data) {
          const userData = response.data.data

          // 이메일과 휴대폰이 빈 문자열이 아닌 경우에만 사용
          if (!finalUserEmail && userData.email && userData.email.trim()) {
            finalUserEmail = userData.email.trim()
          }

          if (!finalUserPhone && userData.phone && userData.phone.trim()) {
            finalUserPhone = userData.phone.trim()
          }

          // 소셜 로그인이 아닌 경우에만 이름 업데이트
          const isSocialLogin = localStorage.getItem('social_login_name') ||
              sessionStorage.getItem('current_user_name')

          if (!isSocialLogin && userData.name && userData.name.trim() && userData.name !== "사용자") {
            finalUserName = userData.name.trim()
          }

          // userStore 업데이트
          user.name = finalUserName
          if (finalUserEmail) {
            user.email = finalUserEmail
          }
          if (finalUserPhone) {
            user.phone = finalUserPhone
          }

          // 정보 저장
          if (finalUserEmail) {
            localStorage.setItem('user_email', finalUserEmail)
            sessionStorage.setItem('user_email', finalUserEmail)
          }
          if (finalUserPhone) {
            localStorage.setItem('user_phone', finalUserPhone)
            sessionStorage.setItem('user_phone', finalUserPhone)
          }
        }
      } catch (error) {
        // API 실패해도 기존 정보 사용
      }
    }

    // 4단계: 최종 사용자 정보 설정
    userInfo.value = {
      name: finalUserName,
      phone: finalUserPhone,
      email: finalUserEmail
    }

    setDefaultDeliveryInfo(userInfo.value.name, userInfo.value.phone)

  } catch (error) {
    // 실패해도 최소한의 정보는 설정
    userInfo.value = {
      name: user.name || user.id || '사용자',
      phone: user.phone || '',
      email: user.email || ''
    }

    setDefaultDeliveryInfo(userInfo.value.name, userInfo.value.phone)
  }
}

const setDefaultDeliveryInfo = (name, phone) => {
  deliveryInfo.value = {
    address: '서울특별시 송파구 정현로 135',
    detailAddress: '(어마덜랩터원) 7층 16층 한국스프트에이전시협의회',
    zipCode: '05506',
    request: '문 앞에 놓아주세요',
    recipientName: name || '수령인',
    recipientPhone: phone || ''
  }
}

const loadDeliveryInfo = async () => {
  if (!isLoggedIn.value) {
    return
  }

  if (deliveryInfo.value.address) {
    return
  }

  try {
    const response = await apiClient.get('/api/users/addresses')

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
    deliveryInfo.value = {
      address: '서울특별시 송파구 정현로 135',
      detailAddress: '(어마덜랩터원) 7층 16층 한국스프트에이전시협의회',
      zipCode: '05506',
      request: '문 앞에 놓아주세요',
      recipientName: '게스트 사용자',
      recipientPhone: ''
    }
  }
}

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

const goBack = () => {
  window.history.back()
}

const formatPrice = (price) => {
  return price?.toLocaleString() || '0'
}

// 주문명 생성 함수 추가
const generateOrderName = () => {
  if (!orderItems.value || orderItems.value.length === 0) {
    return '주문상품'
  }

  const firstItem = orderItems.value[0]
  const itemName = firstItem.name || firstItem.productName || '상품'

  if (orderItems.value.length === 1) {
    // 단일 상품인 경우: "상품명"
    return itemName
  } else {
    // 여러 상품인 경우: "상품명 외 2건"
    return `${itemName} 외 ${orderItems.value.length - 1}건`
  }
}

const loadIamportScript = () => {
  return new Promise((resolve, reject) => {
    if (typeof window.IMP !== 'undefined') {
      resolve(window.IMP)
      return
    }
    const script = document.createElement('script')
    script.src = 'https://cdn.iamport.kr/v1/iamport.js'
    script.onload = () => {
      if (typeof window.IMP !== 'undefined') {
        resolve(window.IMP)
      } else {
        reject(new Error('아임포트 로드 실패'))
      }
    }
    script.onerror = () => reject(new Error('아임포트 스크립트 로드 에러'))
    document.head.appendChild(script)
  })
}

const validatePaymentMethod = () => {
  if (!selectedPayment.value) {
    showFriendlyMessage('결제 수단을 선택해주세요.', 'warning')
    return false
  }
  if (selectedPayment.value === 'general' && !selectedSubPayment.value) {
    showFriendlyMessage('세부 결제 방식을 선택해주세요.', 'warning')
    return false
  }
  if (selectedSubPayment.value === 'credit' && cardPaymentType.value === 'phone') {
    if (!userInfo.value.phone) {
      showFriendlyMessage('휴대폰 결제를 위해 휴대폰 번호가 필요합니다.', 'warning')
      return false
    }
  }
  return true
}

const initiatePayment = async (paymentData) => {
  try {
    const IMP = await loadIamportScript()

    return new Promise((resolve, reject) => {
      IMP.init('imp19424728')

      // PG 설정 로직 (기존과 동일)
      let pgProvider = 'kakaopay.TC0ONETIME'
      let payMethod = 'card'

      if (selectedPayment.value === 'general') {
        switch(selectedSubPayment.value) {
          case 'credit':
            if (cardPaymentType.value === 'phone') {
              pgProvider = 'html5_inicis'
              payMethod = 'phone'
            } else if (cardPaymentType.value === 'simple') {
              pgProvider = 'html5_inicis'
              payMethod = 'samsung'
            } else {
              pgProvider = 'html5_inicis'
              payMethod = 'card'
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
      }

      const actualAmount = paymentData.amount || finalAmount.value

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

      IMP.request_pay(paymentRequest, async (response) => {
        try {
          if (response.success) {
            const pendingOrderData = sessionStorage.getItem('pending_order_data')
            if (!pendingOrderData) {
              throw new Error('임시 주문 데이터를 찾을 수 없습니다')
            }

            const orderData = JSON.parse(pendingOrderData)

            try {
              const orderResponse = await apiClient.post('/api/payments/orders/checkout', {
                ...orderData,
                paymentId: response.imp_uid,
                paidAmount: response.paid_amount,
                pgProvider: pgProvider
              })

              if (orderResponse.data.success) {
                // 장바구니 정리 시 에러 무시
                try {
                  await clearPurchasedItemsFromCart(orderData.items)
                } catch (cartError) {
                  // 무시하고 진행
                }

                // 세션 정리
                sessionStorage.removeItem('pending_order_data')
                sessionStorage.removeItem('checkout_data')
                sessionStorage.setItem('payment_completed', 'true')

                const successMsg = getSuccessMessage(pgProvider, response.paid_amount)
                showFriendlyMessage(successMsg, 'success')

                // 토큰이 만료된 경우에도 주문 완료 페이지로 이동
                const currentToken = localStorage.getItem('token')
                if (currentToken && !isTokenValid(currentToken)) {
                  // 토큰을 삭제하지 말고 주문 완료 페이지에서 처리하도록 함
                }

                window.location.href = `/order-complete?orderId=${orderResponse.data.data.orderId}&paymentId=${response.imp_uid}&amount=${response.paid_amount}&from=checkout`
                resolve(response)
              } else {
                throw new Error(orderResponse.data.message || '주문 생성 실패')
              }

            } catch (orderError) {
              // 결제는 성공했으나 주문 생성 실패 시 처리
              if (orderError.response?.status === 401) {
                sessionStorage.setItem('payment_completed', 'true')
                sessionStorage.setItem('pending_payment_verification', JSON.stringify({
                  paymentId: response.imp_uid,
                  orderId: paymentData.orderId,
                  amount: response.paid_amount
                }))

                window.location.href = `/order-complete?orderId=${paymentData.orderId}&paymentId=${response.imp_uid}&amount=${response.paid_amount}&from=checkout&verify=true`
                resolve(response)
                return
              }

              throw orderError
            }

          } else {
            sessionStorage.removeItem('pending_order_data')
            const friendlyReason = getFailureReason(response.error_code, response.error_msg)
            const messageType = getMessageType(response.error_code, response.error_msg)
            showFriendlyMessage(friendlyReason, messageType)
            const error = new Error(friendlyReason)
            error.alreadyHandled = true
            reject(error)
          }

        } catch (error) {
          sessionStorage.removeItem('pending_order_data')

          if (!error.alreadyHandled) {
            if (response.success && response.imp_uid) {
              try {
                await apiClient.post(`/api/payments/${response.imp_uid}/cancel`, {
                  reason: '주문 생성 실패로 인한 자동 취소',
                  refund_amount: response.paid_amount
                })
              } catch (cancelError) {
                // 취소 실패 무시
              }
            }
            const errorMsg = getFailureReason('SYSTEM_ERROR', '결제 처리 중 오류가 발생했습니다')
            showFriendlyMessage(errorMsg, 'error')
          }
          reject(error)
        }
      })
    })

  } catch (error) {
    sessionStorage.removeItem('pending_order_data')
    const errorMsg = getFailureReason('SYSTEM_ERROR', '결제 시스템 초기화 실패')
    showFriendlyMessage(errorMsg, 'error')
    throw error
  }
}

const clearPurchasedItemsFromCart = async (purchasedItems) => {
  try {
    const currentLoginStatus = checkLoginStatus()

    if (currentLoginStatus) {
      const productIds = purchasedItems
          .map(item => {
            let productId = item.productId || item.id || item.product_id
            if (typeof productId === 'string') {
              const numericId = parseInt(productId, 10)
              if (!isNaN(numericId)) {
                productId = numericId
              }
            }
            return productId
          })
          .filter(id => id !== null && id !== undefined)

      if (productIds.length > 0) {
        try {
          const response = await apiClient.post('/api/cart/remove-purchased-items', {
            productIds: productIds
          }, {
            timeout: 3000,
            validateStatus: function (status) {
              return status < 500;
            }
          })

          if (response.status === 401) {
            // 401 에러 무시
          } else if (response.data?.success) {
            // 성공 처리
          } else {
            // 실패하지만 무시
          }
        } catch (error) {
          // 에러를 던지지 않고 조용히 처리
        }
      }
    } else {
      const productIds = purchasedItems
          .map(item => item.productId || item.id || item.product_id)
          .filter(Boolean)

      if (productIds.length > 0) {
        try {
          const guestCart = JSON.parse(localStorage.getItem('guestCart') || '[]')
          const updatedCart = guestCart.filter(cartItem => {
            return !productIds.includes(String(cartItem.productId))
          })
          localStorage.setItem('guestCart', JSON.stringify(updatedCart))
        } catch (error) {
          // 에러 무시
        }
      }
    }

    sessionStorage.setItem('cart_cleaned_after_payment', 'true')
    sessionStorage.setItem('last_purchase_cleanup', Date.now().toString())

  } catch (error) {
    // 결제 성공 후이므로 에러를 던지지 않음
  }
}

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

// 결제 시작 시 이름 백업
const processPayment = async () => {
  if (!validatePaymentMethod()) {
    return
  }

  if (!canProceed.value) {
    showFriendlyMessage('결제 정보를 확인해주세요.', 'warning')
    return
  }

  try {
    loading.value = true

    backupNameForPayment()
    sessionStorage.setItem('payment_in_progress', 'true')

    const currentLoginStatus = checkLoginStatus()

    if (!currentLoginStatus) {
      showFriendlyMessage('결제를 위해 로그인이 필요합니다.', 'info')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1500)
      return
    }

    if (!userInfo.value.name || userInfo.value.name.trim() === '') {
      showFriendlyMessage('주문자 이름이 필요합니다.', 'warning')
      return
    }

    if (!deliveryInfo.value.address || deliveryInfo.value.address.trim() === '') {
      showFriendlyMessage('배송 주소를 입력해주세요.', 'warning')
      return
    }

    const tempOrderId = `ORDER${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

    const orderData = {
      userId: user.id && user.id !== 'null' ? user.id : undefined,
      items: orderItems.value.map(item => ({
        productId: item.productId || item.id,
        productName: item.name || item.productName,
        quantity: item.quantity,
        unitPrice: item.salePrice || item.price,
        totalPrice: (item.salePrice || item.price) * item.quantity,
        imageUrl: item.image || item.imageUrl || ''
      })),
      phone: userInfo.value.phone || '',
      email: userInfo.value.email || '',
      recipientName: deliveryInfo.value.recipientName || userInfo.value.name || '수령인',
      recipientPhone: deliveryInfo.value.recipientPhone || userInfo.value.phone || '',
      orderZipcode: deliveryInfo.value.zipCode || '',
      orderAddressDetail: deliveryInfo.value.address ?
          (deliveryInfo.value.address + ' ' + (deliveryInfo.value.detailAddress || '')).trim() : '',
      deliveryMemo: deliveryInfo.value.request || '문 앞에 놓아주세요',
      paymentMethod: selectedPayment.value,
      paymentMethodName: getPaymentMethodName(selectedPayment.value),
      usedPoint: pointsUsed.value || 0,
      totalAmount: finalAmount.value,
      cardType: cardPaymentType.value,
      tempOrderId: tempOrderId
    }

    if (!orderData.items || orderData.items.length === 0) {
      showFriendlyMessage('주문할 상품이 없습니다.', 'warning')
      return
    }

    if (orderData.totalAmount <= 0) {
      showFriendlyMessage('결제 금액이 올바르지 않습니다.', 'warning')
      return
    }

    sessionStorage.setItem('pending_order_data', JSON.stringify(orderData))

    const orderName = generateOrderName()

    await initiatePayment({
      orderId: tempOrderId,
      amount: finalAmount.value,
      orderName: orderName,
      userEmail: userInfo.value.email,
      userName: userInfo.value.name,
      userPhone: userInfo.value.phone,
      orderData: orderData
    })

  } catch (error) {
    restoreNameAfterPayment()
    sessionStorage.removeItem('payment_in_progress')

    if (!error.alreadyHandled) {
      const friendlyError = error.friendlyMessage ||
          getFailureReason(null, error.message) ||
          '결제 처리 중 오류가 발생했습니다.'
      showFriendlyMessage(friendlyError, 'error')
    }
  } finally {
    loading.value = false
  }
}

const verifyPayment = async (impUid, merchantUid) => {
  try {
    const response = await apiClient.post('/api/payments/verify', {
      impUid: impUid,
      merchantUid: merchantUid
    })

    if (response.data.success) {
      showFriendlyMessage('결제가 완료되었습니다!', 'success')
      sessionStorage.removeItem('checkout_data')
      window.location.href = `/order-complete?orderId=${merchantUid}`
    } else {
      throw new Error(response.data.message || '결제 검증 실패')
    }
  } catch (error) {
    const errorMsg = getFailureReason('SYSTEM_ERROR', `결제 검증 중 오류가 발생했습니다: ${error.message}`)
    showFriendlyMessage(errorMsg, 'error')
  }
}

onMounted(async () => {
  try {
    // 1. 로그인 상태 확인
    const loginValid = checkLoginStatus();

    // 2. 주문 데이터 로드
    loadOrderData();

    // 3. 사용자 정보 로드 (로그인 여부와 관계없이)
    await loadUserInfo();

    // 4. 배송 정보 로드
    await loadDeliveryInfo();

  } catch (error) {
    showFriendlyMessage('페이지 로드 중 문제가 발생했습니다.', 'error');
  }
});
</script>
<style scoped src="@/assets/css/checkout.css"></style>
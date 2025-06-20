<template>
  <div class="order-complete-page">
    <div class="container">
      <!-- 로딩 상태 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-content">
          <div class="spinner"></div>
          <p class="loading-text">주문 정보를 불러오는 중...</p>
        </div>
      </div>

      <!-- 에러 상태 -->
      <div v-else-if="error" class="error-container">
        <div class="error-content">
          <p class="error-message">{{ error }}</p>
          <button @click="goHome" class="error-button">홈으로 돌아가기</button>
        </div>
      </div>

      <!-- 주문 완료 내용 -->
      <div v-else-if="orderData" class="order-content">
        <!-- 주문완료 헤더 -->
        <div class="order-header">
          <div class="success-icon">
            <CheckCircle class="icon" />
          </div>
          <h1 class="title">주문이 완료되었습니다</h1>
          <p class="subtitle">주문해 주셔서 감사합니다.</p>

          <div class="order-info-box">
            <div class="info-row">
              <span class="label">주문번호</span>
              <span class="value order-id">{{ orderData.orderId }}</span>
            </div>
            <div class="info-row">
              <span class="label">주문일시</span>
              <span class="value">{{ formatDate(orderData.orderDate) }}</span>
            </div>
          </div>
        </div>

        <!-- 주문 상품 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">주문상품 ({{ orderData.items?.length || 0 }}개)</h2>
          </div>
          <div class="section-content">
            <!-- 디버깅 정보 -->
            <div v-if="!orderData.items || orderData.items.length === 0" class="no-items">
              <p class="no-items-text">주문 상품 정보가 없습니다.</p>
              <div class="debug-info">
                <p>디버깅 정보:</p>
                <p>orderData.items: {{ orderData.items }}</p>
                <p>전체 데이터: {{ JSON.stringify(orderData, null, 2) }}</p>
              </div>
            </div>

            <div v-else class="items-list">
              <div
                  v-for="(item, index) in orderData.items"
                  :key="item.productId || item.orderItemId || index"
                  class="item-row"
              >
                <img
                    :src="item.imageUrl || '/api/placeholder/80/80'"
                    :alt="item.productName || item.name"
                    class="item-image"
                />
                <div class="item-info">
                  <h3 class="item-name">{{ item.productName || item.name }}</h3>
                  <p class="item-quantity">{{ item.quantity }}개</p>
                </div>
                <div class="item-price">
                  <p class="price">{{ formatPrice(item.totalPrice) }}원</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 주문자 정보 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">주문자 정보</h2>
          </div>
          <div class="section-content">
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">주문자</span>
                <span class="info-value">{{ orderData.userId || '주문자' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">휴대폰</span>
                <span class="info-value">{{ orderData.phone || '-' }}</span>
              </div>
              <div class="info-item last">
                <span class="info-label">이메일</span>
                <span class="info-value">{{ orderData.email || '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 결제 정보 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">결제 정보</h2>
          </div>
          <div class="section-content">
            <div class="payment-list">
              <div class="payment-item">
                <span class="payment-label">주문금액</span>
                <span class="payment-value">{{ formatPrice(calculateItemTotal()) }}원</span>
              </div>
              <div class="payment-item">
                <span class="payment-label">배송비</span>
                <span class="payment-value">{{ formatPrice(orderData.deliveryFee || 0) }}원</span>
              </div>
              <div v-if="orderData.discountAmount" class="payment-item">
                <span class="payment-label">할인금액</span>
                <span class="payment-value discount">-{{ formatPrice(orderData.discountAmount) }}원</span>
              </div>
              <div v-if="orderData.usedPoint" class="payment-item">
                <span class="payment-label">사용 적립금</span>
                <span class="payment-value discount">-{{ formatPrice(orderData.usedPoint) }}원</span>
              </div>
              <div class="payment-total">
                <div class="total-item">
                  <span class="total-label">총 결제금액</span>
                  <span class="total-value">{{ formatPrice(orderData.totalPrice) }}원</span>
                </div>
              </div>
              <div class="payment-method">
                <span class="payment-label">결제수단</span>
                <span class="payment-value">{{ orderData.paymentMethodName || '카드결제' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 배송지 정보 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">배송지 정보</h2>
          </div>
          <div class="section-content">
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">받는 분</span>
                <span class="info-value">{{ orderData.recipientName || '수령인' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">휴대폰</span>
                <span class="info-value">{{ orderData.recipientPhone || '-' }}</span>
              </div>
              <div v-if="orderData.orderZipcode || orderData.orderAddressDetail" class="info-item">
                <span class="info-label">주소</span>
                <div class="address-info">
                  <div v-if="orderData.orderZipcode" class="zipcode">({{ orderData.orderZipcode }})</div>
                  <div class="address">{{ orderData.orderAddressDetail || '배송지 정보 없음' }}</div>
                </div>
              </div>
              <div v-if="orderData.deliveryMemo" class="info-item last">
                <span class="info-label">배송 요청사항</span>
                <span class="delivery-memo">{{ orderData.deliveryMemo }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 액션 버튼 -->
        <div class="action-buttons">
          <button @click="goToOrderList" class="btn btn-primary">주문 내역 보기</button>
          <button @click="goHome" class="btn btn-secondary">쇼핑 계속하기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { CheckCircle, Package, CreditCard, MapPin, Phone, Calendar } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const route = useRoute()
const router = useRouter()

const orderData = ref(null)
const loading = ref(true)
const error = ref('')
const paymentId = ref('')

// 인증 헤더 생성
const getAuthHeaders = () => {
  const token = localStorage.getItem('token')
  const headers = {
    'Content-Type': 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

// 주문 정보 로드
const loadOrderData = async (orderId) => {
  try {
    loading.value = true

    const userId = localStorage.getItem('userId') || 'guest'

    console.log('🔍 사용자 ID 확인:', {
      localStorage_userId: localStorage.getItem('userId'),
      finalUserId: userId
    })

    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
    const url = `${API_BASE_URL}/api/orders/${orderId}?userId=${userId}`

    console.log('🔍 주문 정보 요청:', {
      orderId: orderId,
      userId: userId,
      url: url,
      headers: getAuthHeaders()
    })

    const response = await fetch(url, {
      method: 'GET',
      headers: getAuthHeaders()
    })

    console.log('📡 응답 상태:', response.status, response.statusText)

    if (!response.ok) {
      const errorText = await response.text()
      console.error('❌ 에러 응답 본문:', errorText)
      throw new Error(`주문 정보를 찾을 수 없습니다. (${response.status}: ${errorText})`)
    }

    const result = await response.json()
    console.log('✅ 응답 데이터:', result)
    console.log('✅ 주문 상품 데이터:', result.data?.items)
    console.log('✅ 주문 데이터 전체 구조:', JSON.stringify(result.data, null, 2))

    if (result.success) {
      orderData.value = result.data
    } else {
      throw new Error(result.message || '주문 정보를 불러오는데 실패했습니다.')
    }
  } catch (err) {
    console.error('주문 정보 로드 실패:', err)
    error.value = err.message || '주문 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

// 가격 포맷팅
const formatPrice = (price) => {
  if (price === null || price === undefined) return '0'
  return price.toLocaleString()
}

// 날짜 포맷팅
const formatDate = (dateString) => {
  if (!dateString) return '-'

  try {
    const date = new Date(dateString)
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    return dateString
  }
}

// 상품 총액 계산
const calculateItemTotal = () => {
  if (!orderData.value?.items) return 0

  return orderData.value.items.reduce((total, item) => {
    return total + (item.totalPrice || 0)
  }, 0)
}

// 네비게이션 함수들
const goHome = () => {
  router.push('/')
}

const goToOrderList = () => {
  router.push('/orders')
}

// 컴포넌트 마운트
onMounted(async () => {
  const orderId = route.query.orderId
  paymentId.value = route.query.paymentId || ''

  if (!orderId) {
    error.value = '주문번호가 제공되지 않았습니다.'
    loading.value = false
    return
  }

  await loadOrderData(orderId)
})
</script>

<style scoped>
/* 페이지 전체 */
.order-complete-page {
  min-height: 100vh;
  background-color: white;
  padding: 2rem 0;
}

.container {
  margin: 0 auto;
  padding: 0 1rem;
}

/* 로딩 상태 */
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 25rem;
}

.loading-content {
  text-align: center;
}

.spinner {
  width: 3rem;
  height: 3rem;
  border: 2px solid transparent;
  border-bottom: 2px solid #16a34a;
  border-radius: 50%;
  margin: 0 auto 1rem;
  animation: spin 1s linear infinite;
}

.loading-text {
  color: #6b7280;
}

/* 에러 상태 */
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 25rem;
}

.error-content {
  text-align: center;
}

.error-message {
  color: #dc2626;
  font-size: 1.125rem;
  margin-bottom: 1rem;
}

.error-button {
  padding: 0.5rem 1.5rem;
  background-color: #16a34a;
  color: white;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.error-button:hover {
  background-color: #15803d;
}

/* 주문 내용 */
.order-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* 주문 헤더 */
.order-header {
  background-color: white;
  border: 1px solid #e5e7eb;
  padding: 2rem;
  text-align: center;
}

.success-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
}

.success-icon .icon {
  width: 4rem;
  height: 4rem;
  color: #22c55e;
}

.title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #111827;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 1.5rem;
}

.order-info-box {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  padding: 1rem;
  margin: 0 auto;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.info-row:last-child {
  margin-bottom: 0;
}

.label {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

.value {
  font-size: 0.875rem;
  color: #111827;
}

.order-id {
  font-family: monospace;
  font-weight: bold;
}

/* 섹션 */
.section {
  background-color: white;
  border: 1px solid #e5e7eb;
}

.section-header {
  border-bottom: 1px solid #e5e7eb;
  padding: 1rem 1.5rem;
  background-color: #f9fafb;
}

.section-title {
  font-size: 1.125rem;
  font-weight: bold;
  color: #111827;
}

.section-content {
  padding: 1.5rem;
}

/* 상품 없음 상태 */
.no-items {
  text-align: center;
  padding: 2rem 0;
}

.no-items-text {
  color: #6b7280;
  margin-bottom: 1rem;
}

.debug-info {
  font-size: 0.75rem;
  color: #9ca3af;
  background-color: #f3f4f6;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  text-align: left;
}

/* 상품 목록 */
.items-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.item-row {
  display: flex;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid #f3f4f6;
}

.item-row:last-child {
  border-bottom: none;
}

.item-image {
  width: 5rem;
  height: 5rem;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  margin-right: 1rem;
}

.item-info {
  flex: 1;
}

.item-name {
  font-weight: 500;
  color: #111827;
  margin-bottom: 0.25rem;
}

.item-quantity {
  font-size: 0.875rem;
  color: #6b7280;
}

.item-price {
  text-align: right;
}

.price {
  font-weight: bold;
  font-size: 1.125rem;
  color: #111827;
}

/* 정보 목록 */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f3f4f6;
}

.info-item.last {
  border-bottom: none;
}

.info-label {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

.info-value {
  font-size: 0.875rem;
  color: #111827;
}

/* 주소 정보 */
.address-info {
  text-align: right;
  max-width: 18rem;
}

.zipcode {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}

.address {
  font-size: 0.875rem;
  color: #111827;
}

.delivery-memo {
  font-size: 0.875rem;
  color: #111827;
  text-align: right;
  max-width: 18rem;
}

/* 결제 정보 */
.payment-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.payment-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
}

.payment-label {
  color: #374151;
}

.payment-value {
  color: #111827;
}

.payment-value.discount {
  color: #dc2626;
}

.payment-total {
  border-top: 1px solid #e5e7eb;
  padding-top: 0.75rem;
  margin-top: 0.75rem;
}

.total-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
}

.total-label {
  font-size: 1.125rem;
  font-weight: bold;
  color: #111827;
}

.total-value {
  font-size: 1.125rem;
  font-weight: bold;
  color: #16a34a;
}

.payment-method {
  border-top: 1px solid #f3f4f6;
  padding-top: 0.75rem;
  margin-top: 1rem;
  display: flex;
  justify-content: space-between;
}

/* 액션 버튼 */
.action-buttons {
  display: flex;
  justify-content: center; /* 좌우 중앙 */
  align-items: center;     /* 위아래 중앙 */
  gap: 0.75rem;
  padding-top: 1rem;
}

.btn {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  /* 추가 */
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-primary {
  background-color: #16a34a;
  color: white;
  border-color: #16a34a;
}

.btn-primary:hover {
  background-color: #15803d;
}

.btn-secondary {
  background-color: white;
  color: #374151;
  border-color: #d1d5db;
}

.btn-secondary:hover {
  background-color: #f9fafb;
}

/* 애니메이션 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
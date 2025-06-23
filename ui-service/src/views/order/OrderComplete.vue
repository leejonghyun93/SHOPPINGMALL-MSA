<template>
  <div class="order-complete-page">
    <div class="container">
      <!-- 네비게이션 브레드크럼 -->
      <div class="breadcrumb">
        <button @click="goBack" class="breadcrumb-item">
          <ArrowLeft class="breadcrumb-icon" />
          주문 내역
        </button>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-current">주문 상세</span>
      </div>

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
          <AlertCircle class="error-icon" />
          <p class="error-message">{{ error }}</p>
          <div class="error-actions">
            <button @click="loadOrderData(route.query.orderId)" class="error-button retry">다시 시도</button>
            <button @click="goBack" class="error-button secondary">목록으로 돌아가기</button>
          </div>
        </div>
      </div>

      <!-- 주문 완료 내용 -->
      <div v-else-if="orderData" class="order-content">
        <!-- 주문완료 헤더 -->
        <div class="order-header">
          <div class="success-section">
            <div class="success-icon">
              <CheckCircle class="icon" />
            </div>
            <div class="success-content">
              <h1 class="title">주문 상세 정보</h1>
              <p class="subtitle">주문번호 {{ orderData.orderId }}의 상세 내역입니다.</p>
            </div>
          </div>

          <div class="order-info-box">
            <div class="info-row">
              <span class="label">주문번호</span>
              <span class="value order-id">{{ orderData.orderId }}</span>
            </div>
            <div class="info-row">
              <span class="label">주문일시</span>
              <span class="value">{{ formatDate(orderData.orderDate) }}</span>
            </div>
            <div class="info-row">
              <span class="label">주문상태</span>
              <span class="value">
                <span class="status-badge" :class="getStatusClass(orderData.orderStatus)">
                  {{ orderData.orderStatus || '배송완료' }}
                </span>
              </span>
            </div>
          </div>
        </div>

        <!-- 🔥 주문 취소 가능 여부 알림 -->
        <div v-if="canCancelOrder(orderData.orderStatus)" class="cancel-notice">
          <Info class="notice-icon" />
          <div class="notice-content">
            <h4 class="notice-title">주문 취소 가능</h4>
            <p class="notice-text">이 주문은 아직 취소할 수 있습니다. 취소 시 결제금액이 환불됩니다.</p>
          </div>
        </div>

        <!-- 주문 상품 -->
        <div class="section">
          <div class="section-header">
            <Package class="section-icon" />
            <h2 class="section-title">주문상품 ({{ orderData.items?.length || 0 }}개)</h2>
          </div>
          <div class="section-content">
            <!-- 상품이 없을 때 -->
            <div v-if="!orderData.items || orderData.items.length === 0" class="no-items">
              <Package class="no-items-icon" />
              <p class="no-items-text">주문 상품 정보가 없습니다.</p>
            </div>

            <!-- 상품 목록 -->
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
                  <div class="item-details">
                    <span class="item-quantity">수량: {{ item.quantity }}개</span>
                    <span class="item-unit-price">단가: {{ formatPrice(item.unitPrice || (item.totalPrice / item.quantity)) }}원</span>
                  </div>
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
            <User class="section-icon" />
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
            <CreditCard class="section-icon" />
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
              <!-- 🔥 결제 ID 정보 (취소 시 필요) -->
              <div v-if="orderData.paymentId" class="payment-method">
                <span class="payment-label">결제번호</span>
                <span class="payment-value payment-id">{{ orderData.paymentId }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 배송지 정보 -->
        <div class="section">
          <div class="section-header">
            <MapPin class="section-icon" />
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
          <button @click="goBack" class="btn btn-secondary">
            <ArrowLeft class="btn-icon" />
            주문 내역으로
          </button>

          <!-- 🔥 주문 취소 버튼 (조건부 표시) -->
          <button
              v-if="canCancelOrder(orderData.orderStatus)"
              @click="showCancelModal = true"
              class="btn btn-danger"
          >
            <X class="btn-icon" />
            주문취소
          </button>

          <button
              v-else
              @click="reorder"
              class="btn btn-primary"
          >
            <RefreshCw class="btn-icon" />
            재주문하기
          </button>

          <button @click="goHome" class="btn btn-outline">
            <Home class="btn-icon" />
            홈으로
          </button>
        </div>
      </div>
    </div>

    <!-- 🔥 주문 취소 모달 -->
    <div v-if="showCancelModal" class="modal-overlay" @click="showCancelModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">주문 취소</h3>
          <button @click="showCancelModal = false" class="modal-close">
            <X class="close-icon" />
          </button>
        </div>

        <div class="modal-body">
          <div class="cancel-warning">
            <AlertTriangle class="warning-icon" />
            <div class="warning-content">
              <h4 class="warning-title">주문을 취소하시겠습니까?</h4>
              <p class="warning-text">
                취소된 주문은 되돌릴 수 없으며, 결제금액은 환불 처리됩니다.
              </p>
            </div>
          </div>

          <div class="cancel-reason">
            <label class="reason-label">취소 사유 (선택)</label>
            <select v-model="cancelReason" class="reason-select">
              <option value="">취소 사유를 선택해주세요</option>
              <option value="단순변심">단순변심</option>
              <option value="상품정보상이">상품정보 상이</option>
              <option value="배송지연">배송 지연</option>
              <option value="판매자요청">판매자 요청</option>
              <option value="기타">기타</option>
            </select>
          </div>

          <div v-if="cancelReason === '기타'" class="cancel-detail">
            <label class="detail-label">상세 사유</label>
            <textarea
                v-model="cancelDetail"
                class="detail-textarea"
                placeholder="취소 사유를 자세히 입력해주세요"
                rows="3"
            ></textarea>
          </div>

          <div class="refund-info">
            <h4 class="refund-title">환불 정보</h4>
            <div class="refund-details">
              <div class="refund-item">
                <span class="refund-label">환불 금액</span>
                <span class="refund-value">{{ formatPrice(orderData.totalPrice) }}원</span>
              </div>
              <div class="refund-item">
                <span class="refund-label">환불 방법</span>
                <span class="refund-value">{{ orderData.paymentMethodName || '카드결제' }} 취소</span>
              </div>
              <div class="refund-item">
                <span class="refund-label">환불 예상일</span>
                <span class="refund-value">영업일 기준 3-5일</span>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="showCancelModal = false" class="btn btn-outline">
            취소
          </button>
          <button
              @click="cancelOrder"
              class="btn btn-danger"
              :disabled="cancelLoading"
          >
            <div v-if="cancelLoading" class="btn-spinner"></div>
            <X v-else class="btn-icon" />
            {{ cancelLoading ? '처리중...' : '주문취소' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  CheckCircle,
  Package,
  CreditCard,
  MapPin,
  Phone,
  Calendar,
  User,
  ArrowLeft,
  AlertCircle,
  RefreshCw,
  Home,
  X,
  Info,
  AlertTriangle
} from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const route = useRoute()
const router = useRouter()

const orderData = ref(null)
const loading = ref(true)
const error = ref('')
const paymentId = ref('')

// 🔥 주문 취소 관련 상태
const showCancelModal = ref(false)
const cancelLoading = ref(false)
const cancelReason = ref('')
const cancelDetail = ref('')

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
    error.value = ''

    const userId = localStorage.getItem('userId') || 'guest'

    console.log('🔍 주문 정보 요청:', {
      orderId: orderId,
      userId: userId
    })

    const url = `${API_BASE_URL}/api/orders/${orderId}?userId=${userId}`

    const response = await fetch(url, {
      method: 'GET',
      headers: getAuthHeaders()
    })

    console.log('📡 응답 상태:', response.status, response.statusText)

    if (!response.ok) {
      const errorText = await response.text()
      console.error('❌ 에러 응답:', errorText)
      throw new Error(`주문 정보를 찾을 수 없습니다. (${response.status})`)
    }

    const result = await response.json()
    console.log('✅ 주문 데이터:', result)

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

// 🔥 주문 취소 가능 여부 확인
const canCancelOrder = (status) => {
  const cancellableStatuses = ['주문접수', '결제완료', '배송준비']
  return cancellableStatuses.includes(status)
}

// 🔥 주문 취소 실행
const cancelOrder = async () => {
  if (!orderData.value) return

  try {
    cancelLoading.value = true

    const userId = localStorage.getItem('userId') || 'guest'

    const cancelData = {
      orderId: orderData.value.orderId,
      userId: userId,
      reason: cancelReason.value || '사용자 요청',
      detail: cancelDetail.value || '',
      refundAmount: orderData.value.totalPrice,
      paymentId: orderData.value.paymentId
    }

    console.log('🔥 주문 취소 요청:', cancelData)

    const response = await fetch(`${API_BASE_URL}/api/orders/${orderData.value.orderId}/cancel`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(cancelData)
    })

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.message || '주문 취소에 실패했습니다.')
    }

    const result = await response.json()
    console.log('✅ 주문 취소 성공:', result)

    if (result.success) {
      // 성공 알림
      alert('주문이 성공적으로 취소되었습니다.\n환불은 영업일 기준 3-5일 소요됩니다.')

      // 주문 데이터 갱신
      await loadOrderData(orderData.value.orderId)

      // 모달 닫기
      showCancelModal.value = false
      cancelReason.value = ''
      cancelDetail.value = ''
    } else {
      throw new Error(result.message || '주문 취소 처리 중 오류가 발생했습니다.')
    }

  } catch (err) {
    console.error('주문 취소 실패:', err)
    alert(`주문 취소 실패: ${err.message}`)
  } finally {
    cancelLoading.value = false
  }
}

// 주문 상태에 따른 CSS 클래스
const getStatusClass = (status) => {
  const statusMap = {
    '주문접수': 'status-pending',
    '결제완료': 'status-paid',
    '배송준비': 'status-preparing',
    '배송중': 'status-shipping',
    '배송완료': 'status-delivered',
    '주문취소': 'status-cancelled',
    '반품': 'status-returned'
  }
  return statusMap[status] || 'status-default'
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
const goBack = () => {
  if (window.history.length > 1) {
    router.go(-1)
  } else {
    router.push({ name: 'MyPageOrders' })
  }
}

const goHome = () => {
  router.push('/')
}

const goToOrderList = () => {
  router.push({ name: 'MyPageOrders' })
}

// 재주문 기능
const reorder = () => {
  if (!orderData.value?.items) {
    alert('재주문할 상품이 없습니다.')
    return
  }

  const productIds = orderData.value.items.map(item => item.productId).filter(Boolean)

  if (productIds.length === 0) {
    alert('재주문할 수 있는 상품이 없습니다.')
    return
  }

  router.push({
    path: '/cart',
    query: { reorder: productIds.join(',') }
  })
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
/* 기존 스타일 + 추가 스타일 */

/* 🔥 취소 알림 박스 */
.cancel-notice {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 1.5rem;
}

.notice-icon {
  width: 24px;
  height: 24px;
  color: #856404;
  flex-shrink: 0;
  margin-top: 2px;
}

.notice-content {
  flex: 1;
}

.notice-title {
  font-size: 14px;
  font-weight: 600;
  color: #856404;
  margin-bottom: 4px;
}

.notice-text {
  font-size: 13px;
  color: #856404;
  margin: 0;
  line-height: 1.4;
}

/* 🔥 결제 ID 스타일 */
.payment-id {
  font-family: monospace;
  font-size: 12px;
  background-color: #f8f9fa;
  padding: 2px 6px;
  border-radius: 4px;
}

/* 🔥 취소 버튼 스타일 */
.btn-danger {
  background-color: #dc3545;
  color: white;
  border: 1px solid #dc3545;
}

.btn-danger:hover:not(:disabled) {
  background-color: #c82333;
  border-color: #bd2130;
}

.btn-danger:disabled {
  background-color: #dc3545;
  opacity: 0.6;
  cursor: not-allowed;
}

/* 🔥 모달 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e9ecef;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.modal-close:hover {
  background-color: #f8f9fa;
}

.close-icon {
  width: 20px;
  height: 20px;
  color: #666;
}

.modal-body {
  padding: 1.5rem;
}

/* 🔥 취소 경고 */
.cancel-warning {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 1.5rem;
}

.warning-icon {
  width: 24px;
  height: 24px;
  color: #856404;
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
}

.warning-title {
  font-size: 16px;
  font-weight: 600;
  color: #856404;
  margin-bottom: 4px;
}

.warning-text {
  font-size: 14px;
  color: #856404;
  margin: 0;
  line-height: 1.4;
}

/* 🔥 취소 사유 */
.cancel-reason {
  margin-bottom: 1.5rem;
}

.reason-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.reason-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  background-color: white;
}

.cancel-detail {
  margin-bottom: 1.5rem;
}

.detail-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.detail-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
}

/* 🔥 환불 정보 */
.refund-info {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 1rem;
}

.refund-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.refund-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.refund-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.refund-label {
  font-size: 14px;
  color: #666;
}

.refund-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 1.5rem;
  border-top: 1px solid #e9ecef;
  justify-content: flex-end;
}

/* 🔥 버튼 스피너 */
.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-bottom: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 기존 모든 CSS는 그대로 유지... */
.order-complete-page {
  min-height: 100vh;
  background-color: #f8f9fa;
  padding: 2rem 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 1rem;
}

/* 브레드크럼 네비게이션 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  padding: 12px 0;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: #5f0080;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.2s;
}

.breadcrumb-item:hover {
  color: #4a0066;
}

.breadcrumb-icon {
  width: 16px;
  height: 16px;
}

.breadcrumb-separator {
  color: #ccc;
  font-size: 14px;
}

.breadcrumb-current {
  color: #666;
  font-size: 14px;
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
  border-bottom: 2px solid #5f0080;
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

.error-icon {
  width: 3rem;
  height: 3rem;
  color: #dc2626;
  margin: 0 auto 1rem;
}

.error-message {
  color: #dc2626;
  font-size: 1.125rem;
  margin-bottom: 1.5rem;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.error-button {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.error-button.retry {
  background-color: #5f0080;
  color: white;
}

.error-button.retry:hover {
  background-color: #4a0066;
}

.error-button.secondary {
  background-color: #e9ecef;
  color: #495057;
}

.error-button.secondary:hover {
  background-color: #dee2e6;
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
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 2rem;
}

.success-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.success-icon .icon {
  width: 3rem;
  height: 3rem;
  color: #22c55e;
}

.success-content {
  flex: 1;
}

.title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #111827;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #6b7280;
}

.order-info-box {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
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

/* 상태 배지 */
.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background-color: #fff3cd;
  color: #856404;
}

.status-paid {
  background-color: #d1ecf1;
  color: #0c5460;
}

.status-preparing {
  background-color: #cce5ff;
  color: #004085;
}

.status-shipping {
  background-color: #d4edda;
  color: #155724;
}

.status-delivered {
  background-color: #d1ecf1;
  color: #0c5460;
}

.status-cancelled {
  background-color: #f8d7da;
  color: #721c24;
}

.status-returned {
  background-color: #ffeaa7;
  color: #6c5500;
}

.status-default {
  background-color: #e9ecef;
  color: #495057;
}

/* 섹션 */
.section {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 1rem 1.5rem;
  background-color: #f9fafb;
}

.section-icon {
  width: 20px;
  height: 20px;
  color: #5f0080;
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

.no-items-icon {
  width: 3rem;
  height: 3rem;
  color: #d1d5db;
  margin: 0 auto 1rem;
}

.no-items-text {
  color: #6b7280;
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
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  margin-right: 1rem;
}

.item-info {
  flex: 1;
}

.item-name {
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.5rem;
  font-size: 1rem;
}

.item-details {
  display: flex;
  gap: 1rem;
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
  border-top: 2px solid #e5e7eb;
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
  font-size: 1.25rem;
  font-weight: bold;
  color: #5f0080;
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
  justify-content: center;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 0;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  font-size: 14px;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

.btn-primary {
  background-color: #5f0080;
  color: white;
  border: 1px solid #5f0080;
}

.btn-primary:hover {
  background-color: #4a0066;
  border-color: #4a0066;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
  border: 1px solid #6c757d;
}

.btn-secondary:hover {
  background-color: #545b62;
  border-color: #545b62;
}

.btn-outline {
  background-color: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-outline:hover {
  background-color: #f9fafb;
  border-color: #9ca3af;
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

/* 반응형 디자인 */
@media (max-width: 768px) {
  .container {
    padding: 0 1rem;
  }

  .order-header {
    padding: 1.5rem;
  }

  .success-section {
    flex-direction: column;
    text-align: center;
    gap: 1rem;
  }

  .title {
    font-size: 1.25rem;
  }

  .section-content {
    padding: 1rem;
  }

  .item-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
    padding: 1rem;
    border: 1px solid #f3f4f6;
    border-radius: 8px;
    margin-bottom: 0.75rem;
  }

  .item-row:last-child {
    border-bottom: 1px solid #f3f4f6;
    margin-bottom: 0;
  }

  .item-image {
    margin-right: 0;
    margin-bottom: 0.5rem;
  }

  .item-details {
    flex-direction: column;
    gap: 0.25rem;
  }

  .item-price {
    text-align: left;
    align-self: flex-start;
  }

  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
  }

  .address-info {
    text-align: left;
    max-width: none;
  }

  .delivery-memo {
    text-align: left;
    max-width: none;
  }

  .action-buttons {
    flex-direction: column;
    gap: 0.5rem;
  }

  .btn {
    width: 100%;
    padding: 1rem;
  }

  .breadcrumb {
    margin-bottom: 16px;
  }

  .breadcrumb-item {
    font-size: 13px;
  }

  .order-info-box {
    padding: 0.75rem;
  }

  .section-header {
    padding: 1rem;
  }

  .section-title {
    font-size: 1rem;
  }

  .modal-content {
    margin: 1rem;
    max-width: calc(100vw - 2rem);
  }

  .modal-footer {
    flex-direction: column;
  }

  .modal-footer .btn {
    width: 100%;
  }
}
</style>
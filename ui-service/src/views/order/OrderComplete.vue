<template>
  <div class="order-complete-page">
    <div class="container">
      <!-- 네비게이션 브레드크럼 -->
      <div class="breadcrumb">
        <button @click="goBack" class="breadcrumb-item">
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
                  {{ getStatusDisplayName(orderData.orderStatus) }}
                </span>
              </span>
            </div>
          </div>
        </div>

        <!-- 주문 취소 가능 여부 알림 -->
        <div v-if="canCancelOrder(orderData.orderStatus)" class="cancel-notice">
          <div class="notice-content">
            <h4 class="notice-title">주문 취소 가능</h4>
            <p class="notice-text">이 주문은 아직 취소할 수 있습니다. 취소 시 결제금액이 환불됩니다.</p>
          </div>
        </div>

        <!-- 주문 상품 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">주문상품 ({{ orderData.items?.length || 0 }}개)</h2>
          </div>
          <div class="section-content">
            <!-- 상품이 없을 때 -->
            <div v-if="!orderData.items || orderData.items.length === 0" class="no-items">
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
                    :src="getProductImage({
                      image: item.imageUrl,
                      mainImage: item.imageUrl,
                      productName: item.productName || item.name
                    })"
                    :alt="item.productName || item.name"
                    class="item-image"
                    @error="handleImageError"
                    @load="handleImageLoad"
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
            <h2 class="section-title">주문자 정보</h2>
          </div>
          <div class="section-content">
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">주문자</span>
                <span class="info-value">{{ orderData.userName || orderData.recipientName || '주문자' }}</span>
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
              <!-- 결제 ID 정보 (취소 시 필요) -->
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
            주문 내역으로
          </button>

          <!-- 주문 취소 버튼 (조건부 표시) -->
          <button
              v-if="canCancelOrder(orderData.orderStatus)"
              @click="showCancelModal = true"
              class="btn btn-danger"
          >
            주문취소
          </button>

          <button
              v-else
              @click="reorder"
              class="btn btn-primary"
          >
            재주문하기
          </button>

          <button @click="goHome" class="btn btn-outline">
            홈으로
          </button>
        </div>
      </div>
    </div>

    <!-- 주문 취소 모달 -->
    <div v-if="showCancelModal" class="modal-overlay" @click="showCancelModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">주문 취소</h3>
          <button @click="showCancelModal = false" class="modal-close">
            ✕
          </button>
        </div>

        <div class="modal-body">
          <div class="cancel-warning">
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
              @click="cancelOrderAction"
              class="btn btn-danger"
              :disabled="cancelLoading"
          >
            <div v-if="cancelLoading" class="btn-spinner"></div>
            {{ cancelLoading ? '처리중...' : '주문취소' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { user } from '@/stores/userStore'
import { useSmartImages } from '@/composables/useSmartImages'

// 상태 유틸리티 import
import {
  getStatusDisplayName,
  getStatusClass,
  canCancelOrder,
  getStatusIcon
} from '@/utils/orderStatusUtils'

const { getProductImage, handleImageError, handleImageLoad } = useSmartImages()

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const route = useRoute()
const router = useRouter()

const orderData = ref(null)
const loading = ref(true)
const error = ref('')
const paymentId = ref('')

// 주문 취소 관련 상태
const showCancelModal = ref(false)
const cancelLoading = ref(false)
const cancelReason = ref('')
const cancelDetail = ref('')

// 인증 헤더 생성
const getAuthHeaders = () => {
  const token = localStorage.getItem('jwt')

  const headers = {
    'Content-Type': 'application/json'
  }

  if (token && token.trim() && token !== 'null' && token !== 'undefined') {
    const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    headers.Authorization = authToken
  }

  return headers
}

// 토큰 자동 갱신 함수
const refreshTokenIfNeeded = async () => {
  const token = localStorage.getItem('jwt')
  if (!token) return false

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    })

    if (response.ok) {
      const result = await response.json()
      if (result.success && result.token) {
        localStorage.setItem('jwt', result.token)
        return true
      }
    }
    return false
  } catch (error) {
    return false
  }
}

// 주문 정보 로드
const loadOrderData = async (orderId) => {
  try {
    loading.value = true
    error.value = ''

    const userId = localStorage.getItem('userId') || 'guest'
    const url = `${API_BASE_URL}/api/orders/${orderId}?userId=${userId}`

    const response = await fetch(url, {
      method: 'GET',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error(`주문 정보를 찾을 수 없습니다. (${response.status})`)
    }

    const result = await response.json()

    if (result.success) {
      orderData.value = result.data

      // 사용자 정보 보완 처리

      // 이름 정보 보완
      if (!orderData.value.userName || orderData.value.userName === '사용자') {
        if (orderData.value.recipientName && orderData.value.recipientName !== '수령인') {
          orderData.value.userName = orderData.value.recipientName
        } else if (user.name && user.name !== '사용자') {
          orderData.value.userName = user.name
        } else {
          // 토큰에서 이름 추출
          const token = localStorage.getItem('jwt')
          if (token) {
            try {
              const parts = token.replace('Bearer ', '').split('.')
              if (parts.length === 3) {
                let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
                while (base64.length % 4) {
                  base64 += '='
                }
                const payload = JSON.parse(atob(base64))

                if (payload.name && payload.name !== payload.sub) {
                  orderData.value.userName = payload.name
                }
              }
            } catch (e) {
              // 토큰 파싱 실패 무시
            }
          }
        }
      }

      // 이메일 정보 보완
      if (!orderData.value.email) {
        if (user.email) {
          orderData.value.email = user.email
        } else {
          const savedEmail = localStorage.getItem('user_email') ||
              sessionStorage.getItem('user_email')
          if (savedEmail) {
            orderData.value.email = savedEmail
          } else {
            // 토큰에서 이메일 추출
            const token = localStorage.getItem('jwt')
            if (token) {
              try {
                const parts = token.replace('Bearer ', '').split('.')
                if (parts.length === 3) {
                  let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
                  while (base64.length % 4) {
                    base64 += '='
                  }
                  const payload = JSON.parse(atob(base64))

                  const emailFields = ['email', 'mail', 'userEmail', 'emailAddress']
                  for (const field of emailFields) {
                    if (payload[field]) {
                      orderData.value.email = payload[field]
                      break
                    }
                  }
                }
              } catch (e) {
                // 토큰 파싱 실패 무시
              }
            }
          }
        }
      }

      // 휴대폰 번호 보완
      if (!orderData.value.phone) {
        if (orderData.value.recipientPhone) {
          orderData.value.phone = orderData.value.recipientPhone
        } else if (user.phone) {
          orderData.value.phone = user.phone
        } else {
          const savedPhone = localStorage.getItem('user_phone') ||
              sessionStorage.getItem('user_phone')
          if (savedPhone) {
            orderData.value.phone = savedPhone
          } else {
            // 토큰에서 휴대폰 추출
            const token = localStorage.getItem('jwt')
            if (token) {
              try {
                const parts = token.replace('Bearer ', '').split('.')
                if (parts.length === 3) {
                  let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
                  while (base64.length % 4) {
                    base64 += '='
                  }
                  const payload = JSON.parse(atob(base64))

                  const phoneFields = ['phone', 'phoneNumber', 'mobile', 'userPhone', 'tel', 'cellphone']
                  for (const field of phoneFields) {
                    if (payload[field]) {
                      orderData.value.phone = payload[field]
                      break
                    }
                  }
                }
              } catch (e) {
                // 토큰 파싱 실패 무시
              }
            }
          }
        }
      }

    } else {
      throw new Error(result.message || '주문 정보를 불러오는데 실패했습니다.')
    }
  } catch (err) {
    error.value = err.message || '주문 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

// 주문 취소 실행 (토큰 자동 갱신 포함)
const cancelOrderAction = async () => {
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

    // 첫 번째 시도
    let response = await fetch(`${API_BASE_URL}/api/orders/${orderData.value.orderId}/cancel`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(cancelData)
    })

    // 401 오류시 토큰 갱신 후 재시도
    if (response.status === 401) {
      const refreshed = await refreshTokenIfNeeded()
      if (refreshed) {
        // 토큰 갱신 성공, 다시 요청
        response = await fetch(`${API_BASE_URL}/api/orders/${orderData.value.orderId}/cancel`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(cancelData)
        })
      } else {
        // 토큰 갱신 실패
        alert('로그인이 만료되었습니다. 다시 로그인해주세요.')
        localStorage.removeItem('jwt')
        localStorage.removeItem('userId')
        router.push('/login')
        return
      }
    }

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.message || '주문 취소에 실패했습니다.')
    }

    const result = await response.json()

    if (result.success) {
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
    alert(`주문 취소 실패: ${err.message}`)
  } finally {
    cancelLoading.value = false
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
const goBack = () => {
  const fromPage = route.query.from
  const wasPaymentComplete = sessionStorage.getItem('payment_completed') === 'true'

  //  수정: checkout에서 온 경우 (결제 완료 후)와 mypage에서 온 경우 모두 마이페이지로
  if (fromPage === 'checkout' || fromPage === 'payment' || fromPage === 'mypage' || wasPaymentComplete) {
    sessionStorage.removeItem('payment_completed') // 정리
    router.push({
      name: 'MyPageOrders',
      query: { from: 'order-complete' }
    })
  } else {
    router.push({ name: 'MyPageOrders' })
  }
}

const goHome = () => {
  router.push('/')
}

const goToOrderList = () => {
  // 명확하게 MyPageOrders로 이동
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
  const paymentId = route.query.paymentId || ''
  const amount = route.query.amount || ''

  // 결제 완료 후 직접 온 경우 처리
  if (paymentId && amount) {
    sessionStorage.setItem('payment_completed', 'true')
  }

  // 결제 중 정보 손실 복구 시도
  const backupTime = sessionStorage.getItem('payment_backup_time')
  if (backupTime) {
    const backupAge = Date.now() - parseInt(backupTime)
    if (backupAge < 10 * 60 * 1000) { // 10분 이내
      // 결제 백업 정보에서 복구
      const paymentName = localStorage.getItem('payment_user_name')
      const paymentEmail = localStorage.getItem('payment_user_email')
      const paymentPhone = localStorage.getItem('payment_user_phone')

      if (paymentName && (!user.name || user.name === "사용자")) {
        user.name = paymentName
        sessionStorage.setItem('current_user_name', paymentName)
      }

      if (paymentEmail && !user.email) {
        user.email = paymentEmail
        sessionStorage.setItem('user_email', paymentEmail)
        localStorage.setItem('user_email', paymentEmail)
      }

      if (paymentPhone && !user.phone) {
        user.phone = paymentPhone
        sessionStorage.setItem('user_phone', paymentPhone)
        localStorage.setItem('user_phone', paymentPhone)
      }
    }
  }

  if (!orderId) {
    error.value = '주문번호가 제공되지 않았습니다.'
    loading.value = false
    return
  }

  await loadOrderData(orderId)
})
</script>

<style scoped src="@/assets/css/orderComplete.css"></style>
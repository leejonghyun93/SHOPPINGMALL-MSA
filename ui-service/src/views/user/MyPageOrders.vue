<template>
  <div class="orders-container">
    <!-- 상단 헤더 영역 (제목과 필터) - 고정 -->
    <div class="orders-header">
      <h1 class="orders-title">주문 내역</h1>
      <div class="orders-controls">
        <div class="control-group">
          <!-- 기간 선택 -->
          <select
              v-model="selectedPeriod"
              @change="onPeriodChange"
              class="period-select"
          >
            <option value="">전체 기간</option>
            <option value="1">1개월</option>
            <option value="3">3개월</option>
            <option value="6">6개월</option>
            <option value="12">1년</option>
          </select>

          <!-- 검색 -->
          <div class="search-container">
            <input
                v-model="searchQuery"
                @input="onSearch"
                type="text"
                placeholder="상품명으로 검색해보세요"
                class="search-input"
            />
            <Search class="search-icon" />
          </div>
        </div>
        <!-- 필터 상태 표시 -->
        <div v-if="selectedPeriod || searchQuery" class="filter-status">
          <div class="filter-tags">
            <span v-if="selectedPeriod" class="filter-tag">
              {{ selectedPeriod }}개월
              <button @click="clearPeriodFilter" class="filter-clear">×</button>
            </span>
            <span v-if="searchQuery" class="filter-tag">
              "{{ searchQuery }}"
              <button @click="clearSearchFilter" class="filter-clear">×</button>
            </span>
          </div>
          <button @click="clearAllFilters" class="clear-all-button">
            전체 초기화
          </button>
        </div>
      </div>
    </div>

    <!-- 주문 목록 영역 - 스크롤 가능 -->
    <div class="orders-wrapper">
      <!-- 로딩 상태 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-content">
          <div class="spinner"></div>
          <p class="loading-text">주문 내역을 불러오는 중...</p>
        </div>
      </div>

      <!-- 에러 상태 -->
      <div v-else-if="error" class="error-container">
        <p class="error-message">{{ error }}</p>
        <button @click="loadOrders" class="retry-button">
          다시 시도
        </button>
      </div>

      <!-- 주문 목록이 없을 때 -->
      <div v-else-if="!filteredOrders.length" class="empty-container">
        <Package class="empty-icon" />
        <h3 class="empty-title">주문 내역이 없습니다</h3>
        <p class="empty-description">아직 주문하신 상품이 없어요.</p>
        <button @click="goShopping" class="shopping-button">
          쇼핑하러 가기
        </button>
      </div>

      <!-- 주문 목록과 페이징을 분리 -->
      <div v-else class="orders-content">
        <!-- 스크롤 가능한 주문 목록 영역 -->
        <div class="orders-list-container">
          <div class="orders-list">
            <div
                v-for="order in paginatedOrders"
                :key="order.orderId"
                class="order-card"
            >
              <!-- 주문 헤더 - 날짜, 주문번호, 상태 -->
              <div class="order-header">
                <div class="order-info">
                  <div class="order-date">{{ formatDate(order.orderDate) }}</div>
                  <div class="order-number">주문번호 {{ order.orderId }}</div>
                  <div class="order-status">
                    <!-- 상태 유틸리티 적용 -->
                    <span class="status-badge" :class="getStatusClass(order.orderStatus)">
                      {{ getStatusIcon(order.orderStatus) }} {{ getStatusDisplayName(order.orderStatus) }}
                    </span>
                    <span class="order-time">{{ formatDateTime(order.orderDate) }}</span>
                  </div>
                </div>
                <button @click="viewOrderDetail(order.orderId)" class="detail-button" title="주문 상세보기">
                  <Eye class="detail-icon" />
                </button>
              </div>

              <!-- 주문 상품들 -->
              <div class="order-content">
                <!-- 안전한 상품 아이템 렌더링 -->
                <div
                    v-for="(item, index) in (order.items || [])"
                    :key="`${item.productId || index}-${index}`"
                    class="product-item"
                    @click="viewOrderDetail(order.orderId)"
                >
                  <img
                      :src="getProductImage(item)"
                      :alt="getProductName(item)"
                      class="product-image"
                  />
                  <div class="product-details">
                    <div class="product-name">{{ getProductName(item) }}</div>
                    <div class="product-info">
                      <span class="product-price">{{ formatPrice(getProductPrice(item)) }}원</span>
                      <span class="product-quantity">{{ getProductQuantity(item) }}개</span>
                    </div>
                  </div>
                  <div class="product-actions">
                    <span class="cart-icon">🛒</span>
                  </div>
                </div>

                <!-- 주문 상품이 없는 경우 -->
                <div v-if="!order.items || order.items.length === 0" class="no-items">
                  <p>주문 상품 정보가 없습니다.</p>
                </div>

                <!-- 주문 총액 표시 -->
                <div class="order-total">
                  <span class="total-label">주문 총액</span>
                  <span class="total-amount">{{ formatPrice(order.totalPrice) }}원</span>
                </div>

                <!-- 액션 버튼들 -->
                <div class="order-actions">
                  <button
                      @click="viewOrderDetail(order.orderId)"
                      class="action-button detail-btn"
                  >
                    <FileText class="btn-icon" />
                    주문상세
                  </button>
                  <button
                      @click="reorder(order.items)"
                      class="action-button reorder-btn"
                      :disabled="!order.items || order.items.length === 0"
                  >
                    <RefreshCw class="btn-icon" />
                    재주문
                  </button>
                  <!-- 상태 유틸리티로 취소 버튼 조건 확인 -->
                  <button
                      v-if="canCancelOrder(order.orderStatus)"
                      @click="cancelOrder(order.orderId)"
                      class="action-button cancel-btn"
                  >
                    <X class="btn-icon" />
                    주문취소
                  </button>
                  <button
                      @click="writeReview(order)"
                      class="action-button review-btn"
                  >
                    <Star class="btn-icon" />
                    후기작성
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 페이지네이션 (고정) -->
        <div v-if="totalPages > 1" class="pagination-container">
          <div class="pagination">
            <!-- 이전 페이지 버튼 -->
            <button
                v-if="currentPage > 1"
                @click="goToPage(currentPage - 1)"
                class="page-button nav-button"
            >
              이전
            </button>

            <!-- 페이지 번호들 -->
            <button
                v-for="page in displayPages"
                :key="page"
                @click="goToPage(page)"
                class="page-button"
                :class="{ active: page === currentPage }"
            >
              {{ page }}
            </button>

            <!-- 다음 페이지 버튼 -->
            <button
                v-if="currentPage < totalPages"
                @click="goToPage(currentPage + 1)"
                class="page-button nav-button"
            >
              다음
            </button>
          </div>

          <!-- 페이지 정보 -->
          <div class="page-info">
            <span class="page-text">
              {{ (currentPage - 1) * ordersPerPage + 1 }} -
              {{ Math.min(currentPage * ordersPerPage, filteredOrders.length) }}
              / {{ filteredOrders.length }}개 주문
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Package,
  Search,
  Calendar,
  Truck,
  Eye,
  FileText,
  RefreshCw,
  X,
  Star
} from 'lucide-vue-next'

// 상태 유틸리티 import
import {
  getStatusDisplayName,
  getStatusClass,
  canCancelOrder,
  getStatusIcon
} from '@/utils/orderStatusUtils'

const router = useRouter()

// 상태 관리
const orders = ref([])
const loading = ref(true)
const error = ref('')
const selectedPeriod = ref('')
const searchQuery = ref('')
const currentPage = ref(1)
const ordersPerPage = 5

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// 인증 헤더 생성
const getAuthHeaders = () => {
  const token = localStorage.getItem('token')

  const headers = {
    'Content-Type': 'application/json'
  }

  if (token && token.trim() && token !== 'null' && token !== 'undefined') {
    const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    headers.Authorization = authToken
  }

  return headers
}

// loadOrders 함수
const loadOrders = async () => {
  try {
    loading.value = true
    error.value = ''

    const userId = localStorage.getItem('userId') || 'guest'

    const url = `${API_BASE_URL}/api/orders/list?userId=${userId}`

    const response = await fetch(url, {
      method: 'GET',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error(`주문 목록을 불러올 수 없습니다. (${response.status})`)
    }

    const result = await response.json()

    if (result.success) {
      // 백엔드 데이터 구조에 맞게 정확한 매핑
      orders.value = (result.data || []).map(order => {
        return {
          orderId: order.orderId,
          orderDate: order.orderDate,
          orderStatus: order.orderStatus,
          totalPrice: order.totalPrice,
          paymentId: order.paymentId, // 취소 시 필요
          // OrderDTO의 items 필드 매핑
          items: (order.items || []).map(item => ({
            productId: item.productId,
            // 백엔드에서 name 필드를 productName으로 매핑
            productName: item.name || item.productName || '상품명 없음',
            quantity: item.quantity || 1,
            totalPrice: item.totalPrice || 0,
            imageUrl: item.imageUrl || '/api/placeholder/60/60'
          }))
        }
      })

    } else {
      throw new Error(result.message || '주문 목록을 불러오는데 실패했습니다.')
    }
  } catch (err) {
    error.value = err.message || '주문 목록을 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

// 기간 및 검색 필터링된 주문 목록
const filteredOrders = computed(() => {
  let filtered = orders.value

  // 기간 필터링
  if (selectedPeriod.value) {
    const monthsAgo = parseInt(selectedPeriod.value)
    const cutoffDate = new Date()
    cutoffDate.setMonth(cutoffDate.getMonth() - monthsAgo)

    filtered = filtered.filter(order => {
      if (!order.orderDate) return true
      const orderDate = new Date(order.orderDate)
      return orderDate >= cutoffDate
    })
  }

  // 검색 필터링 수정 (안전한 접근)
  if (searchQuery.value) {
    const searchTerm = searchQuery.value.toLowerCase()
    filtered = filtered.filter(order => {
      // items 배열이 존재하고 비어있지 않은지 확인
      if (!order.items || !Array.isArray(order.items)) {
        return false
      }

      return order.items.some(item => {
        const productName = item.productName || item.name || ''
        return productName.toLowerCase().includes(searchTerm)
      })
    })
  }

  return filtered
})

// 현재 페이지의 주문들
const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * ordersPerPage
  const end = start + ordersPerPage
  return filteredOrders.value.slice(start, end)
})

// 페이지네이션 계산
const totalPages = computed(() => Math.ceil(filteredOrders.value.length / ordersPerPage))

const displayPages = computed(() => {
  const pages = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, start + 4)

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// 안전한 접근자 함수들
const getProductName = (item) => {
  return item.productName || item.name || '상품명 없음'
}

const getProductPrice = (item) => {
  return item.totalPrice || item.price || 0
}

const getProductQuantity = (item) => {
  return item.quantity || 1
}

const getProductImage = (item) => {
  return item.imageUrl || item.image || '/api/placeholder/60/60'
}

// 페이지 이동 - 스크롤을 주문 목록 컨테이너 맨 위로
const goToPage = (page) => {
  currentPage.value = page
  // 주문 목록 컨테이너의 스크롤을 맨 위로 이동
  const ordersContainer = document.querySelector('.orders-list-container')
  if (ordersContainer) {
    ordersContainer.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 검색 처리
const onSearch = () => {
  currentPage.value = 1
}

// 기간 변경 처리
const onPeriodChange = () => {
  currentPage.value = 1
}

// 필터 초기화 함수들
const clearPeriodFilter = () => {
  selectedPeriod.value = ''
  currentPage.value = 1
}

const clearSearchFilter = () => {
  searchQuery.value = ''
  currentPage.value = 1
}

const clearAllFilters = () => {
  selectedPeriod.value = ''
  searchQuery.value = ''
  currentPage.value = 1
}

// 주문 상세보기 - OrderComplete 페이지로 이동
const viewOrderDetail = (orderId) => {
  router.push(`/order-complete?orderId=${orderId}`)
}

// 후기 작성
const writeReview = (order) => {
  // 후기 작성 페이지로 이동 (향후 구현)
  alert('후기 작성 기능은 준비 중입니다.')
}

// 날짜 포맷팅 (2025.06.08 형태)
const formatDate = (dateString) => {
  if (!dateString) return '-'

  try {
    const date = new Date(dateString)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}.${month}.${day}`
  } catch (error) {
    return dateString
  }
}

// 날짜 시간 포맷팅 (6.9(월) 03:13 형태)
const formatDateTime = (dateString) => {
  if (!dateString) return ''

  try {
    const date = new Date(dateString)
    const month = date.getMonth() + 1
    const day = date.getDate()
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')

    const weekdays = ['일', '월', '화', '수', '목', '금', '토']
    const weekday = weekdays[date.getDay()]

    return `${month}.${day}(${weekday}) ${hours}:${minutes}`
  } catch (error) {
    return ''
  }
}

// 가격 포맷팅
const formatPrice = (price) => {
  if (price === null || price === undefined) return '0'
  return price.toLocaleString()
}

// 주문 취소
const cancelOrder = async (orderId) => {
  try {
    // 1. 기본 인증 확인
    const token = localStorage.getItem('token')
    const userId = localStorage.getItem('userId')

    if (!token || token === 'null' || token === 'undefined') {
      alert('로그인이 필요합니다. 다시 로그인해주세요.')
      router.push('/login')
      return
    }

    if (!userId || userId === 'null' || userId === 'undefined') {
      alert('사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.')
      router.push('/login')
      return
    }

    // 2. 사용자 확인
    const confirmed = confirm(`주문을 취소하시겠습니까?\n\n주문번호: ${orderId}\n취소된 주문은 되돌릴 수 없으며, 결제금액이 환불됩니다.`)
    if (!confirmed) return

    // 3. 취소 사유 입력
    const reason = prompt('취소 사유를 입력해주세요 (선택사항):') || '사용자 요청'

    // 4. 주문 정보 조회
    const order = orders.value.find(o => o.orderId === orderId)
    if (!order) {
      alert('주문 정보를 찾을 수 없습니다.')
      return
    }

    // 5. 취소 요청 데이터
    const cancelData = {
      orderId: orderId,
      userId: userId,
      reason: reason,
      detail: '',
      refundAmount: order.totalPrice,
      paymentId: order.paymentId || null
    }

    // 6. API 호출
    const response = await fetch(`${API_BASE_URL}/api/orders/${orderId}/cancel`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(cancelData)
    })

    // 7. 응답 처리
    const responseText = await response.text()
    let result

    if (responseText) {
      try {
        result = JSON.parse(responseText)
      } catch (parseError) {
        throw new Error(`서버 응답 파싱 실패: ${responseText}`)
      }
    } else {
      throw new Error(`서버에서 빈 응답: ${response.status}`)
    }

    if (response.ok && result.success) {
      alert('주문이 성공적으로 취소되었습니다.\n환불은 영업일 기준 3-5일 소요됩니다.')
      await loadOrders() // 주문 목록 새로고침
    } else {
      throw new Error(result?.message || `취소 실패: ${response.status}`)
    }

  } catch (err) {
    alert(`주문 취소 실패: ${err.message}`)
  }
}

// 재주문
const reorder = (items) => {
  if (!items || items.length === 0) {
    alert('재주문할 상품이 없습니다.')
    return
  }

  const productIds = items.map(item => item.productId).filter(id => id)
  if (productIds.length === 0) {
    alert('유효한 상품이 없습니다.')
    return
  }

  router.push({
    path: '/cart',
    query: { reorder: productIds.join(',') }
  })
}

// 쇼핑하러 가기
const goShopping = () => {
  router.push('/')
}

// 컴포넌트 마운트
onMounted(() => {
  loadOrders()
})
</script>

<style scoped src="@/assets/css/myPageOrder.css"></style>
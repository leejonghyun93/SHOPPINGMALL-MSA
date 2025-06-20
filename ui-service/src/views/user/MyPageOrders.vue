<template>
  <div class="orders-container">
    <!-- 상단 헤더 영역 (제목과 필터) -->
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
              📅 {{ selectedPeriod }}개월
              <button @click="clearPeriodFilter" class="filter-clear">×</button>
            </span>
            <span v-if="searchQuery" class="filter-tag">
              🔍 "{{ searchQuery }}"
              <button @click="clearSearchFilter" class="filter-clear">×</button>
            </span>
          </div>
          <button @click="clearAllFilters" class="clear-all-button">
            전체 초기화
          </button>
        </div>
      </div>
    </div>

    <!-- 주문 목록 영역 (검은 테두리로 둘러싸임) -->
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

      <!-- 주문 목록 -->
      <div v-else class="orders-list">
        <div
            v-for="order in paginatedOrders"
            :key="order.orderId"
            class="order-card"
        >
          <!-- 주문 헤더 - 날짜, 주문번호, 상태를 세로로 배치 -->
          <div class="order-header">
            <div class="order-info">
              <div class="order-date">{{ formatDate(order.orderDate) }}</div>
              <div class="order-number">주문번호 {{ order.orderId }} 📋</div>
              <div class="order-status">
                {{ order.orderStatus || '배송완료' }} {{ formatDateTime(order.orderDate) }} 📦
              </div>
            </div>
            <button @click="viewOrderDetail(order.orderId)" class="detail-button">
              ›
            </button>
          </div>

          <!-- 주문 상품들 -->
          <div class="order-content">
            <div
                v-for="(item, index) in order.items"
                :key="item.productId"
                class="product-item"
            >
              <img
                  :src="item.imageUrl || '/api/placeholder/60/60'"
                  :alt="item.productName"
                  class="product-image"
              />
              <div class="product-details">
                <div class="product-name">{{ item.productName }}</div>
                <div class="product-price">{{ formatPrice(item.totalPrice) }}원 {{ item.quantity }}개</div>
              </div>
              <div class="cart-icon">🛒</div>
            </div>

            <!-- 후기작성 버튼 -->
            <div class="order-actions">
              <button class="review-button">
                후기작성
              </button>
            </div>
          </div>
        </div>

        <!-- 페이지네이션 -->
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
import { Package, Search, Calendar, Truck } from 'lucide-vue-next'

const router = useRouter()

// 상태 관리
const orders = ref([])
const loading = ref(true)
const error = ref('')
const selectedPeriod = ref('')
const searchQuery = ref('')
const currentPage = ref(1)
const ordersPerPage = 10

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

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

// 주문 목록 로드
const loadOrders = async () => {
  try {
    loading.value = true
    error.value = ''

    const userId = localStorage.getItem('userId') || 'guest'

    // 🔧 수정: 올바른 엔드포인트로 변경
    const url = `${API_BASE_URL}/api/orders/list?userId=${userId}`

    console.log('주문 목록 요청:', url)

    const response = await fetch(url, {
      method: 'GET',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error(`주문 목록을 불러올 수 없습니다. (${response.status})`)
    }

    const result = await response.json()
    console.log('주문 목록 응답:', result)

    if (result.success) {
      // 백엔드에서 받은 데이터를 프론트엔드 형식에 맞게 변환
      orders.value = (result.data || []).map(order => ({
        orderId: order.orderId,
        orderDate: order.orderDate,
        orderStatus: order.orderStatus,
        totalPrice: order.totalPrice,
        // OrderDTO의 items 구조에 맞게 매핑
        items: order.orderItems || order.items || []
      }))
    } else {
      throw new Error(result.message || '주문 목록을 불러오는데 실패했습니다.')
    }
  } catch (err) {
    console.error('주문 목록 로드 실패:', err)
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

  // 검색 필터링
  if (searchQuery.value) {
    filtered = filtered.filter(order => {
      return order.items.some(item =>
          item.productName.toLowerCase().includes(searchQuery.value.toLowerCase())
      )
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

// 페이지 이동
const goToPage = (page) => {
  currentPage.value = page
  // 페이지 변경 시 스크롤을 맨 위로 이동
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 검색 처리
const onSearch = () => {
  currentPage.value = 1
}

// 기간 변경 처리
const onPeriodChange = () => {
  currentPage.value = 1
  console.log('기간 변경:', selectedPeriod.value + '개월')
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

// 주문 상세 보기
const viewOrderDetail = (orderId) => {
  router.push(`/order-complete?orderId=${orderId}`)
}

// 주문 취소
const cancelOrder = async (orderId) => {
  if (!confirm('정말로 주문을 취소하시겠습니까?')) return

  try {
    const userId = localStorage.getItem('userId')

    const response = await fetch(`${API_BASE_URL}/api/orders/${orderId}/cancel?userId=${userId}`, {
      method: 'PUT',
      headers: getAuthHeaders()
    })

    if (response.ok) {
      alert('주문이 취소되었습니다.')
      loadOrders()
    } else {
      const errorData = await response.json()
      throw new Error(errorData.message || '주문 취소에 실패했습니다.')
    }
  } catch (err) {
    alert(err.message)
  }
}

// 재주문
const reorder = (items) => {
  const productIds = items.map(item => item.productId)
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
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
                  <div class="order-number">주문번호 {{ order.orderId }} 📋</div>
                  <div class="order-status">
                    <!-- 🔥 상태 유틸리티 적용 -->
                    <span class="status-badge" :class="getStatusClass(order.orderStatus)">
                      {{ getStatusIcon(order.orderStatus) }} {{ getStatusDisplayName(order.orderStatus) }}
                    </span>
                    <span class="order-time">{{ formatDateTime(order.orderDate) }} 📦</span>
                  </div>
                </div>
                <button @click="viewOrderDetail(order.orderId)" class="detail-button" title="주문 상세보기">
                  <Eye class="detail-icon" />
                </button>
              </div>

              <!-- 주문 상품들 -->
              <div class="order-content">
                <div
                    v-for="(item, index) in order.items"
                    :key="item.productId"
                    class="product-item"
                    @click="viewOrderDetail(order.orderId)"
                >
                  <img
                      :src="item.imageUrl || '/api/placeholder/60/60'"
                      :alt="item.productName"
                      class="product-image"
                  />
                  <div class="product-details">
                    <div class="product-name">{{ item.productName }}</div>
                    <div class="product-info">
                      <span class="product-price">{{ formatPrice(item.totalPrice) }}원</span>
                      <span class="product-quantity">{{ item.quantity }}개</span>
                    </div>
                  </div>
                  <div class="product-actions">
                    <span class="cart-icon">🛒</span>
                  </div>
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
                  >
                    <RefreshCw class="btn-icon" />
                    재주문
                  </button>
                  <!-- 🔥 상태 유틸리티로 취소 버튼 조건 확인 -->
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

// 🔥 상태 유틸리티 import
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
  const userId = localStorage.getItem('userId')

  console.log('🔍 인증 정보 확인:', {
    tokenExists: !!token,
    tokenLength: token ? token.length : 0,
    userId: userId,
    tokenStart: token ? token.substring(0, 20) + '...' : 'none'
  })

  const headers = {
    'Content-Type': 'application/json'
  }

  // 🔥 토큰이 있고 유효할 때만 Authorization 헤더 추가
  if (token && token.trim() && token !== 'null' && token !== 'undefined') {
    // Bearer 접두사가 없다면 추가
    const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    headers.Authorization = authToken
    console.log('✅ Authorization 헤더 추가됨:', authToken.substring(0, 30) + '...')
  } else {
    console.log('⚠️ 토큰이 없거나 유효하지 않음')
  }

  // userId 헤더도 추가 (백엔드에서 요구할 수 있음)
  if (userId && userId !== 'null' && userId !== 'undefined') {
    headers['X-User-Id'] = userId
    console.log('✅ X-User-Id 헤더 추가됨:', userId)
  }

  console.log('📤 최종 헤더:', {
    'Content-Type': headers['Content-Type'],
    'Authorization': headers.Authorization ? headers.Authorization.substring(0, 30) + '...' : '없음',
    'X-User-Id': headers['X-User-Id'] || '없음'
  })

  return headers
}

// 🔥 토큰 자동 갱신 함수
const refreshTokenIfNeeded = async () => {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')

  console.log('🔄 토큰 갱신 시작:', {
    tokenExists: !!token,
    tokenLength: token ? token.length : 0,
    userId: userId,
    tokenStart: token ? token.substring(0, 30) + '...' : 'none'
  })

  if (!token || token === 'null' || token === 'undefined') {
    console.error('❌ 갱신할 토큰이 없음')
    return false
  }

  try {
    // Bearer 접두사 확인 및 정리
    const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token

    console.log('📤 토큰 갱신 요청:', {
      url: `${API_BASE_URL}/auth/refresh`,
      tokenLength: cleanToken.length,
      tokenPreview: cleanToken.substring(0, 50) + '...'
    })

    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${cleanToken}`
      }
    })

    console.log('📡 토큰 갱신 응답:', {
      status: response.status,
      statusText: response.statusText,
      ok: response.ok,
      headers: Object.fromEntries(response.headers.entries())
    })

    // 응답 본문 읽기
    const responseText = await response.text()
    console.log('📄 토큰 갱신 응답 본문:', responseText)

    if (response.ok) {
      let result
      try {
        result = JSON.parse(responseText)
      } catch (parseError) {
        console.error('❌ 토큰 갱신 응답 JSON 파싱 실패:', parseError)
        return false
      }

      if (result.success && result.token) {
        // 새 토큰 저장
        localStorage.setItem('token', result.token)

        // 사용자 정보도 업데이트
        if (result.userId) localStorage.setItem('userId', result.userId)
        if (result.username) localStorage.setItem('username', result.username)

        console.log('✅ 토큰 자동 갱신 성공:', {
          newTokenLength: result.token.length,
          userId: result.userId,
          username: result.username
        })
        return true
      } else {
        console.error('❌ 토큰 갱신 실패 - 응답 형식 오류:', result)
        return false
      }
    } else {
      console.error('❌ 토큰 갱신 HTTP 오류:', {
        status: response.status,
        statusText: response.statusText,
        body: responseText
      })
      return false
    }

  } catch (error) {
    console.error('❌ 토큰 갱신 네트워크 오류:', {
      name: error.name,
      message: error.message,
      stack: error.stack
    })
    return false
  }
}

// 주문 목록 로드
const loadOrders = async () => {
  try {
    loading.value = true
    error.value = ''

    const userId = localStorage.getItem('userId') || 'guest'

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

      // 🔥 디버깅: 로드된 주문들의 상태 확인
      console.log('=== 주문 상태 디버깅 ===')
      orders.value.forEach(order => {
        console.log(`주문 ${order.orderId}:`)
        console.log(`  - 원본 상태: "${order.orderStatus}"`)
        console.log(`  - 표시명: "${getStatusDisplayName(order.orderStatus)}"`)
        console.log(`  - CSS 클래스: "${getStatusClass(order.orderStatus)}"`)
        console.log(`  - 취소 가능: ${canCancelOrder(order.orderStatus)}`)
        console.log(`  - 아이콘: ${getStatusIcon(order.orderStatus)}`)
      })
      console.log('========================')

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

// 🔥 주문 상세보기 - OrderComplete 페이지로 이동
const viewOrderDetail = (orderId) => {
  console.log('주문 상세보기:', orderId)
  router.push(`/order-complete?orderId=${orderId}`)
}

// 🔥 후기 작성
const writeReview = (order) => {
  console.log('후기 작성:', order.orderId)
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

// 🔥 주문 취소 (토큰 자동 갱신 포함)
const cancelOrder = async (orderId) => {
  try {
    console.log('🚀 주문 취소 시작:', orderId)

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

    console.log('🔥 주문 취소 요청 데이터:', cancelData)

    // 6. 첫 번째 API 호출
    let response = await fetch(`${API_BASE_URL}/api/orders/${orderId}/cancel`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(cancelData)
    })

    console.log('📡 첫 번째 응답:', {
      status: response.status,
      statusText: response.statusText,
      ok: response.ok
    })

    // 7. 401 오류시 토큰 갱신 후 재시도
    if (response.status === 401) {
      console.log('🔄 토큰 만료 감지, 갱신 시도...')

      const refreshed = await refreshTokenIfNeeded()
      if (refreshed) {
        console.log('✅ 토큰 갱신 완료, 주문 취소 재시도...')

        // 토큰 갱신 성공, 다시 요청
        response = await fetch(`${API_BASE_URL}/api/orders/${orderId}/cancel`, {
          method: 'POST',
          headers: getAuthHeaders(), // 새로운 토큰으로 헤더 재생성
          body: JSON.stringify(cancelData)
        })

        console.log('📡 재시도 응답:', {
          status: response.status,
          statusText: response.statusText,
          ok: response.ok
        })
      } else {
        // 토큰 갱신 실패
        console.error('❌ 토큰 갱신 실패')
        alert('로그인이 만료되었습니다. 다시 로그인해주세요.')
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        router.push('/login')
        return
      }
    }

    // 8. 다른 오류 처리
    if (response.status === 403) {
      console.error('❌ 403 Forbidden - 권한 없음')
      alert('이 작업을 수행할 권한이 없습니다.')
      return
    }

    // 9. 응답 본문 처리
    const responseText = await response.text()
    console.log('📄 최종 응답 본문:', responseText)

    let result
    if (responseText) {
      try {
        result = JSON.parse(responseText)
      } catch (parseError) {
        console.error('❌ JSON 파싱 실패:', parseError)
        throw new Error(`서버 응답 파싱 실패: ${responseText}`)
      }
    } else {
      throw new Error(`서버에서 빈 응답: ${response.status}`)
    }

    // 10. 성공 처리
    if (response.ok && result.success) {
      alert('주문이 성공적으로 취소되었습니다.\n환불은 영업일 기준 3-5일 소요됩니다.')
      await loadOrders() // 주문 목록 새로고침
    } else {
      throw new Error(result?.message || `취소 실패: ${response.status}`)
    }

  } catch (err) {
    console.error('🚨 주문 취소 최종 실패:', {
      name: err.name,
      message: err.message,
      stack: err.stack
    })
    alert(`주문 취소 실패: ${err.message}`)
  }
}

// 재주문
const reorder = (items) => {
  console.log('재주문:', items)
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

<style scoped>
/* 기본 컨테이너 */
.orders-container {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: 100vh;
}

/* 상단 헤더 */
.orders-header {
  background-color: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.orders-title {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 20px;
}

.orders-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.control-group {
  display: flex;
  gap: 16px;
  align-items: center;
}

.period-select {
  padding: 10px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background-color: white;
  cursor: pointer;
}

.search-container {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.search-input {
  width: 100%;
  padding: 10px 16px 10px 40px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: #666;
}

/* 필터 상태 */
.filter-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.filter-tags {
  display: flex;
  gap: 8px;
}

.filter-tag {
  background-color: #5f0080;
  color: white;
  padding: 4px 8px;
  border-radius: 16px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-clear {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
  padding: 0;
  margin-left: 4px;
}

.clear-all-button {
  background-color: #dc3545;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

/* 주문 목록 래퍼 */
.orders-wrapper {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* 로딩, 에러, 빈 상태 */
.loading-container, .error-container, .empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px;
}

.loading-content, .error-container, .empty-container {
  text-align: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #5f0080;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: #666;
  font-size: 16px;
}

.error-message {
  color: #dc3545;
  font-size: 16px;
  margin-bottom: 16px;
}

.retry-button {
  background-color: #5f0080;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
}

.empty-icon {
  width: 60px;
  height: 60px;
  color: #ccc;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 20px;
  color: #333;
  margin-bottom: 8px;
}

.empty-description {
  color: #666;
  margin-bottom: 24px;
}

.shopping-button {
  background-color: #5f0080;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

/* 주문 목록 */
.orders-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.orders-list-container {
  flex: 1;
  overflow-y: auto;
  max-height: 600px;
}

.orders-list {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 주문 카드 */
.order-card {
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 20px;
  background-color: white;
  transition: box-shadow 0.2s;
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 주문 헤더 */
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f3f4;
}

.order-info {
  flex: 1;
}

.order-date {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.order-number {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
  font-family: monospace;
}

.order-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending { background-color: #fff3cd; color: #856404; }
.status-paid { background-color: #d1ecf1; color: #0c5460; }
.status-preparing { background-color: #cce5ff; color: #004085; }
.status-shipping { background-color: #d4edda; color: #155724; }
.status-delivered { background-color: #d1ecf1; color: #0c5460; }
.status-cancelled { background-color: #f8d7da; color: #721c24; }
.status-returned { background-color: #ffeaa7; color: #6c5500; }
.status-default { background-color: #e9ecef; color: #495057; }

.order-time {
  font-size: 12px;
  color: #999;
}

.detail-button {
  background: none;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-button:hover {
  background-color: #5f0080;
  border-color: #5f0080;
  color: white;
}

.detail-icon {
  width: 18px;
  height: 18px;
}

/* 주문 내용 */
.order-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #f1f3f4;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.product-item:hover {
  background-color: #f8f9fa;
}

.product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  margin-right: 12px;
  border: 1px solid #e9ecef;
}

.product-details {
  flex: 1;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.product-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.product-price {
  font-size: 14px;
  font-weight: 600;
  color: #5f0080;
}

.product-quantity {
  font-size: 13px;
  color: #666;
}

.product-actions {
  display: flex;
  align-items: center;
}

.cart-icon {
  font-size: 20px;
}

/* 주문 총액 */
.order-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  margin-top: 8px;
}

.total-label {
  font-size: 14px;
  color: #666;
}

.total-amount {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

/* 액션 버튼들 */
.order-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f3f4;
}

.action-button {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: white;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-icon {
  width: 14px;
  height: 14px;
}

.detail-btn:hover {
  background-color: #5f0080;
  border-color: #5f0080;
  color: white;
}

.reorder-btn:hover {
  background-color: #28a745;
  border-color: #28a745;
  color: white;
}

.cancel-btn:hover {
  background-color: #dc3545;
  border-color: #dc3545;
  color: white;
}

.review-btn:hover {
  background-color: #ffc107;
  border-color: #ffc107;
  color: #212529;
}

/* 페이지네이션 */
.pagination-container {
  padding: 24px;
  border-top: 1px solid #e9ecef;
  background-color: #f8f9fa;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}

.page-button {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  background-color: white;
  color: #666;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.page-button:hover {
  background-color: #f8f9fa;
  border-color: #5f0080;
}

.page-button.active {
  background-color: #5f0080;
  border-color: #5f0080;
  color: white;
}

.nav-button {
  font-weight: 500;
}

.page-info {
  text-align: center;
}

.page-text {
  font-size: 14px;
  color: #666;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .orders-container {
    padding: 12px;
  }

  .orders-header {
    padding: 16px;
  }

  .orders-title {
    font-size: 24px;
  }

  .control-group {
    flex-direction: column;
    align-items: stretch;
  }

  .search-container {
    max-width: none;
  }

  .filter-status {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .order-card {
    padding: 16px;
  }

  .order-header {
    flex-direction: column;
    gap: 12px;
  }

  .detail-button {
    align-self: flex-end;
  }

  .product-item {
    padding: 8px;
  }

  .product-image {
    width: 50px;
    height: 50px;
  }

  .order-actions {
    flex-direction: column;
    gap: 6px;
  }

  .action-button {
    padding: 12px 16px;
    font-size: 14px;
  }

  .pagination {
    flex-wrap: wrap;
    gap: 4px;
  }

  .page-button {
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>
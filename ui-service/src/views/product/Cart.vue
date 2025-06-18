<template>
  <div class="cart-container">
    <!-- 헤더 -->
    <div class="cart-header">
      <button class="back-button" @click="goBack">
        <ChevronLeft :size="24" />
      </button>
      <h1 class="cart-title">장바구니</h1>
      <div class="header-spacer"></div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>장바구니를 불러오는 중...</p>
    </div>

    <!-- 메인 컨텐츠 -->
    <div v-else class="main-content">
      <!-- 왼쪽: 장바구니 내용 -->
      <div class="cart-content">
        <!-- 장바구니가 비어있을 때 -->
        <div v-if="cartItems.length === 0" class="empty-cart">
          <div class="empty-icon">!</div>
          <h3>장바구니에 담긴 상품이 없습니다</h3>
        </div>

        <!-- 장바구니 내용이 있을 때 -->
        <div v-else>
          <!-- 전체 선택 -->
          <div class="select-all-section">
            <label class="checkbox-container">
              <input
                  type="checkbox"
                  v-model="selectAll"
                  @change="toggleSelectAll"
              >
              <span class="checkmark"></span>
              전체선택 {{ selectedItems.length }}/{{ cartItems.length }}
            </label>
            <button
                class="delete-selected-btn"
                @click="deleteSelectedItems"
                :disabled="selectedItems.length === 0"
            >
              선택삭제
            </button>
          </div>

          <!-- 배송비 안내 -->
          <div class="delivery-info">
            <div class="delivery-badge">🚚 샛별배송</div>
            <div class="delivery-text">
              <span class="delivery-time">23시 전 주문 시 내일 아침 7시 전 도착</span>
              <span class="delivery-condition">(우선배송 상품 포함 {{ freeDeliveryThreshold.toLocaleString() }}원 이상 구매 시)</span>
            </div>
          </div>

          <!-- 냉동 상품 섹션 -->
          <div v-if="frozenItems.length > 0" class="cart-section">
            <div class="section-header">
              <div class="section-title">
                <span class="temp-icon">❄️</span>
                냉동 상품
              </div>
            </div>

            <div class="cart-items">
              <div
                  v-for="item in frozenItems"
                  :key="item.id"
                  class="cart-item"
              >
                <label class="checkbox-container">
                  <input
                      type="checkbox"
                      v-model="selectedItems"
                      :value="item.id"
                  >
                  <span class="checkmark"></span>
                </label>

                <div class="item-image">
                  <img :src="item.image" :alt="item.name" @error="handleImageError" />
                </div>

                <div class="item-details">
                  <h3 class="item-name">{{ item.name }}</h3>
                  <div class="item-price-section">
                    <div class="price-info">
                      <span v-if="item.discountRate > 0" class="discount-rate">{{ item.discountRate }}%</span>
                      <span class="final-price">{{ formatPrice(item.salePrice) }}원</span>
                    </div>
                    <div v-if="item.discountRate > 0" class="original-price">{{ formatPrice(item.price) }}원</div>
                  </div>

                  <div class="quantity-controls">
                    <button
                        class="quantity-btn"
                        @click="decreaseQuantity(item)"
                        :disabled="item.quantity <= 1"
                    >
                      <Minus :size="16" />
                    </button>
                    <span class="quantity">{{ item.quantity }}</span>
                    <button
                        class="quantity-btn"
                        @click="increaseQuantity(item)"
                    >
                      <Plus :size="16" />
                    </button>
                  </div>
                </div>

                <button class="delete-item-btn" @click="deleteItem(item.id)">
                  <X :size="20" />
                </button>
              </div>
            </div>
          </div>

          <!-- 일반 상품 섹션 -->
          <div v-if="normalItems.length > 0" class="cart-section">
            <div class="section-header">
              <div class="section-title">
                <span class="temp-icon">🛍️</span>
                일반 상품
              </div>
            </div>

            <div class="cart-items">
              <div
                  v-for="item in normalItems"
                  :key="item.id"
                  class="cart-item"
              >
                <label class="checkbox-container">
                  <input
                      type="checkbox"
                      v-model="selectedItems"
                      :value="item.id"
                  >
                  <span class="checkmark"></span>
                </label>

                <div class="item-image">
                  <img :src="item.image" :alt="item.name" @error="handleImageError" />
                </div>

                <div class="item-details">
                  <h3 class="item-name">{{ item.name }}</h3>
                  <div class="item-price-section">
                    <div class="price-info">
                      <span v-if="item.discountRate > 0" class="discount-rate">{{ item.discountRate }}%</span>
                      <span class="final-price">{{ formatPrice(item.salePrice) }}원</span>
                    </div>
                    <div v-if="item.discountRate > 0" class="original-price">{{ formatPrice(item.price) }}원</div>
                  </div>

                  <div class="quantity-controls">
                    <button
                        class="quantity-btn"
                        @click="decreaseQuantity(item)"
                        :disabled="item.quantity <= 1"
                    >
                      <Minus :size="16" />
                    </button>
                    <span class="quantity">{{ item.quantity }}</span>
                    <button
                        class="quantity-btn"
                        @click="increaseQuantity(item)"
                    >
                      <Plus :size="16" />
                    </button>
                  </div>
                </div>

                <button class="delete-item-btn" @click="deleteItem(item.id)">
                  <X :size="20" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 오른쪽: 주문 요약 (사이드바) -->
      <div class="order-summary-sidebar">
        <div class="summary-card">
          <h3 class="summary-title">결제금액</h3>

          <div class="summary-details">
            <div class="summary-row">
              <span class="summary-label">상품금액</span>
              <span class="summary-value">{{ formatPrice(totalProductPrice) }}원</span>
            </div>
            <div class="summary-row discount">
              <span class="summary-label">상품할인금액</span>
              <span class="summary-value discount-text">{{ formatPrice(totalDiscount) }}원</span>
            </div>
            <div class="summary-note-text">로그인 후 할인 금액 적용</div>
            <div class="summary-row">
              <span class="summary-label">배송비</span>
              <span class="summary-value">{{ formatPrice(deliveryFee) }}원</span>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-row total">
              <span class="summary-label">결제예정금액</span>
              <span class="summary-value total-price">{{ formatPrice(finalTotal) }}원</span>
            </div>
            <div class="summary-note">
              쿠폰/적립금은 주문서에서 사용 가능합니다
            </div>
          </div>

          <button
              class="checkout-btn"
              @click="goToCheckout"
              :disabled="selectedItems.length === 0 || cartItems.length === 0"
          >
            {{ cartItems.length === 0 ? '로그인' : '주문하기' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ChevronLeft, Minus, Plus, X } from 'lucide-vue-next'
import axios from 'axios'

// 상수 설정
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const FREE_DELIVERY_THRESHOLD = 40000
const DELIVERY_FEE = 3000

// 반응형 상태
const loading = ref(false)
const cartItems = ref([])
const selectedItems = ref([])
const selectAll = ref(false)
const freeDeliveryThreshold = ref(FREE_DELIVERY_THRESHOLD)
const isLoggedIn = ref(false)

// JWT 토큰에서 사용자 ID 추출
const extractUserIdFromJWT = () => {
  try {
    const token = localStorage.getItem('auth_token') ||
        localStorage.getItem('token') ||
        localStorage.getItem('access_token') ||
        sessionStorage.getItem('auth_token')

    if (!token) return null

    const parts = token.split('.')
    if (parts.length !== 3) return null

    let payload = parts[1]
    while (payload.length % 4) payload += '='
    payload = payload.replace(/-/g, '+').replace(/_/g, '/')

    const decoded = JSON.parse(atob(payload))
    return decoded.sub || decoded.userId || decoded.username || null
  } catch (error) {
    return null
  }
}

// 로그인 상태 및 사용자 ID 확인
const checkLoginStatus = () => {
  const token = localStorage.getItem('auth_token') ||
      localStorage.getItem('token') ||
      localStorage.getItem('access_token') ||
      sessionStorage.getItem('auth_token')

  isLoggedIn.value = !!token

  if (token) {
    const userId = extractUserIdFromJWT()
    if (userId) {
      localStorage.setItem('user_id', userId)
    }
  }

  return isLoggedIn.value
}

// 사용자 ID 가져오기
const getUserId = () => {
  if (isLoggedIn.value) {
    const jwtUserId = extractUserIdFromJWT()
    if (jwtUserId) return jwtUserId

    const storedUserId = localStorage.getItem('user_id') || localStorage.getItem('userId')
    if (storedUserId) return storedUserId
  }

  return generateGuestId() // guestId 없으면 여기서 생성해서 반환
}

// 게스트 ID 생성
const generateGuestId = () => {
  let guestId = localStorage.getItem('guestId')
  if (!guestId) {
    guestId = crypto.randomUUID()
    localStorage.setItem('guestId', guestId)
  }
  return guestId // 이게 없으면 undefined 반환됨!
}

// 인증 헤더 생성
const getAuthHeaders = () => {
  const token = localStorage.getItem('token') // 또는 다른 키들

  const userId = getUserId()
  console.log('🚀 X-User-Id:', userId)

  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': userId
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}


// 로컬 스토리지 관리
const loadLocalCart = () => {
  try {
    const localCart = localStorage.getItem('temp_cart')
    if (localCart) {
      const parsedCart = JSON.parse(localCart)
      cartItems.value = parsedCart
      selectedItems.value = parsedCart.map(item => item.id)
      selectAll.value = parsedCart.length > 0
    }
  } catch (error) {
    cartItems.value = []
  }
}

const saveLocalCart = () => {
  try {
    localStorage.setItem('temp_cart', JSON.stringify(cartItems.value))
  } catch (error) {
    // 저장 실패 시 무시
  }
}

// 서버 장바구니 로드
const loadServerCart = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/cart`, {
      headers: getAuthHeaders()
    })
    console.log('장바구니 응답 데이터:', response.data)
    if (response.data.success && response.data.data?.cartItems?.length > 0) {
      const serverItems = response.data.data.cartItems.map(mapCartItemToProduct)
      cartItems.value = serverItems
      selectedItems.value = serverItems.map(item => item.id)
      selectAll.value = serverItems.length > 0
      saveLocalCart()
    }
  } catch (error) {
    // 서버 로드 실패 시 로컬 데이터 사용
    loadLocalCart()
  }
}

// 장바구니 데이터 로드
const loadCartItems = async () => {
  try {
    loading.value = true

    if (checkLoginStatus()) {
      await loadServerCart()
    } else {
      loadLocalCart()
    }
  } catch (error) {
    loadLocalCart()
  } finally {
    loading.value = false
  }
}

// 상품 매핑
const mapCartItemToProduct = (cartItem) => {
  return {
    id: cartItem.cartItemId || cartItem.id || Date.now(),
    productId: cartItem.productId,
    name: cartItem.productName || cartItem.name || '상품명 없음',
    price: cartItem.productPrice || cartItem.price || 0,
    salePrice: cartItem.salePrice || cartItem.productPrice || cartItem.price || 0,
    discountRate: cartItem.discountRate || 0,
    quantity: cartItem.quantity || 1,
    image: cartItem.productImage || cartItem.image || generatePlaceholderImage(),
    category: cartItem.category || 'normal',
    deliveryType: cartItem.deliveryType || 'normal'
  }
}

// 컴퓨티드 속성들
const frozenItems = computed(() =>
    cartItems.value.filter(item => item.category === 'frozen')
)

const normalItems = computed(() =>
    cartItems.value.filter(item => item.category === 'normal')
)

const selectedCartItems = computed(() =>
    cartItems.value.filter(item => selectedItems.value.includes(item.id))
)

const totalProductPrice = computed(() =>
    selectedCartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
)

const totalDiscount = computed(() =>
    selectedCartItems.value.reduce((sum, item) =>
        sum + ((item.price - item.salePrice) * item.quantity), 0
    )
)

const deliveryFee = computed(() => {
  const subtotal = selectedCartItems.value.reduce((sum, item) =>
      sum + (item.salePrice * item.quantity), 0
  )
  return subtotal >= freeDeliveryThreshold.value ? 0 : DELIVERY_FEE
})

const finalTotal = computed(() =>
    selectedCartItems.value.reduce((sum, item) =>
        sum + (item.salePrice * item.quantity), 0
    ) + deliveryFee.value
)

// 수량 변경
const updateCartItemQuantity = async (cartItemId, newQuantity) => {
  try {
    if (isLoggedIn.value) {
      const response = await axios.put(`${API_BASE_URL}/api/cart/items`, {
        cartItemId,
        quantity: newQuantity
      }, {
        headers: getAuthHeaders()
      })

      if (!response.data.success) {
        throw new Error(response.data.message)
      }
    }
    saveLocalCart()
    return true
  } catch (error) {
    if (isLoggedIn.value) {
      alert('수량 변경에 실패했습니다.')
      return false
    } else {
      saveLocalCart()
      return true
    }
  }
}

// 상품 삭제
const deleteCartItem = async (cartItemId) => {
  try {
    if (isLoggedIn.value) {
      const response = await axios.delete(`${API_BASE_URL}/api/cart/items/${cartItemId}`, {
        headers: getAuthHeaders()
      })

      if (!response.data.success) {
        throw new Error(response.data.message)
      }
    }
    saveLocalCart()
    return true
  } catch (error) {
    if (isLoggedIn.value) {
      alert('상품 삭제에 실패했습니다.')
      return false
    } else {
      saveLocalCart()
      return true
    }
  }
}

// 다중 상품 삭제
const deleteMultipleCartItems = async (cartItemIds) => {
  try {
    if (isLoggedIn.value) {
      const response = await axios.delete(`${API_BASE_URL}/api/cart/items`, {
        data: { cartItemIds },
        headers: getAuthHeaders()
      })

      if (!response.data.success) {
        throw new Error(response.data.message)
      }
    }
    saveLocalCart()
    return true
  } catch (error) {
    if (isLoggedIn.value) {
      alert('상품 삭제에 실패했습니다.')
      return false
    } else {
      saveLocalCart()
      return true
    }
  }
}

// 이벤트 핸들러들
const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedItems.value = cartItems.value.map(item => item.id)
  } else {
    selectedItems.value = []
  }
}

const increaseQuantity = async (item) => {
  const success = await updateCartItemQuantity(item.id, item.quantity + 1)
  if (success) {
    item.quantity++
    saveLocalCart()
  }
}

const decreaseQuantity = async (item) => {
  if (item.quantity <= 1) return

  const success = await updateCartItemQuantity(item.id, item.quantity - 1)
  if (success) {
    item.quantity--
    saveLocalCart()
  }
}

const deleteItem = async (itemId) => {
  if (confirm('상품을 장바구니에서 제거하시겠습니까?')) {
    const success = await deleteCartItem(itemId)
    if (success) {
      cartItems.value = cartItems.value.filter(item => item.id !== itemId)
      selectedItems.value = selectedItems.value.filter(id => id !== itemId)
      saveLocalCart()
    }
  }
}

const deleteSelectedItems = async () => {
  if (selectedItems.value.length === 0) return

  if (confirm(`선택한 ${selectedItems.value.length}개 상품을 삭제하시겠습니까?`)) {
    const success = await deleteMultipleCartItems(selectedItems.value)
    if (success) {
      cartItems.value = cartItems.value.filter(item => !selectedItems.value.includes(item.id))
      selectedItems.value = []
      selectAll.value = false
      saveLocalCart()
    }
  }
}

// 체크아웃
const goToCheckout = () => {
  if (selectedItems.value.length === 0) {
    alert('주문할 상품을 선택해주세요.')
    return
  }

  try {
    const selectedProducts = cartItems.value.filter(item =>
        selectedItems.value.includes(item.id)
    )

    const checkoutData = {
      items: selectedProducts,
      totalPrice: finalTotal.value,
      productAmount: totalProductPrice.value,
      discountAmount: totalDiscount.value,
      deliveryFee: deliveryFee.value,
      userId: getUserId()
    }

    sessionStorage.setItem('checkout_data', JSON.stringify(checkoutData))
    window.location.href = '/checkout'
  } catch (error) {
    alert('주문 페이지로 이동 중 오류가 발생했습니다.')
  }
}

// 네비게이션
const goBack = () => {
  window.history.back()
}

// 로그인 후 처리
const handleAfterLogin = async () => {
  isLoggedIn.value = true

  if (cartItems.value.length > 0) {
    try {
      for (const item of cartItems.value) {
        await axios.post(`${API_BASE_URL}/api/cart/items`, {
          productId: productId,
          quantity: quantity
        }, {
          headers: getAuthHeaders()
        })
      }
    } catch (error) {
      // 저장 실패는 무시
    }
  }

  const shouldRedirectToCheckout = sessionStorage.getItem('checkout_redirect')
  const savedSelectedItems = sessionStorage.getItem('selected_items')

  if (shouldRedirectToCheckout && savedSelectedItems) {
    sessionStorage.removeItem('checkout_redirect')
    sessionStorage.removeItem('selected_items')

    const selectedItemIds = JSON.parse(savedSelectedItems)
    selectedItems.value = selectedItemIds

    const selectedProducts = cartItems.value.filter(item => selectedItemIds.includes(item.id))

    sessionStorage.setItem('checkout_data', JSON.stringify({
      selectedItems: selectedProducts,
      totalAmount: finalTotal.value
    }))

    window.location.href = '/checkout'
  }
}

// 전역 함수 노출
window.handleCartAfterLogin = handleAfterLogin

// 유틸리티 함수들
const formatPrice = (price) => {
  return price?.toLocaleString() || '0'
}

const generatePlaceholderImage = () => {
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjEyMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjNmNGY2Ii8+CiAgPHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSIxMiIgZmlsbD0iIzZiNzI4MCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuydtOuvuOyngDwvdGV4dD4KPC9zdmc+Cg=='
}

const handleImageError = (event) => {
  if (event.target.dataset.errorHandled) return
  event.target.dataset.errorHandled = 'true'
  event.target.src = generatePlaceholderImage()
}

// 워처
watch(selectedItems, () => {
  selectAll.value = selectedItems.value.length === cartItems.value.length && cartItems.value.length > 0
}, { deep: true })

// 마운트
onMounted(() => {
  // 비회원일 경우 guestId 생성
  if (!checkLoginStatus()) {
    let guestId = localStorage.getItem('guestId');
    if (!guestId) {
      guestId = crypto.randomUUID(); // 또는 uuidv4()
      localStorage.setItem('guestId', guestId);
    }
  }

  loadCartItems()
})
</script>

<style scoped src="@/assets/css/cart.css"></style>
<template>
  <div class="cart-container">
    <!-- 헤더 -->
    <div class="cart-header">
      <button class="back-button" @click="goBack">
        <ChevronLeft :size="24"/>
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
        <!-- 안전한 조건 체크: cartItems가 존재하고 배열인지 확인 -->
        <div v-if="!cartItems || !Array.isArray(cartItems) || cartItems.length === 0" class="empty-cart">
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
              전체선택 {{ selectedItems?.length || 0 }}/{{ cartItems?.length || 0 }}
            </label>
            <button
                class="delete-selected-btn"
                @click="deleteSelectedItems"
                :disabled="!selectedItems || selectedItems.length === 0"
            >
              선택삭제
            </button>
          </div>

          <!-- 냉동 상품 섹션 -->
          <div v-if="frozenItems && frozenItems.length > 0" class="cart-section">
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
                  <img
                      :src="getProductImage(item)"
                      :alt="item.name"
                      @error="handleImageError"
                      @load="handleImageLoad"
                  />
                </div>

                <div class="item-details">
                  <h3 class="item-name">{{ item.name || '상품명 없음' }}</h3>

                  <div class="item-price-section">
                    <div class="price-info">
                      <span v-if="hasItemDiscount(item)" class="discount-rate">{{ item.discountRate || 0 }}%</span>
                      <span class="final-price">{{ formatPrice(item.salePrice || 0) }}원</span>
                    </div>
                    <div v-if="hasItemDiscount(item)" class="original-price">{{ formatPrice(item.price || 0) }}원</div>
                  </div>

                  <div class="item-total-price">
                    <span class="total-label">소계: </span>
                    <span class="total-amount">{{ formatPrice(getItemTotalPrice(item)) }}원</span>
                    <span v-if="hasItemDiscount(item)" class="total-discount">
                      ({{ formatPrice(getItemTotalDiscount(item)) }}원 할인)
                    </span>
                  </div>

                  <div class="quantity-controls">
                    <button
                        class="quantity-btn"
                        @click="decreaseQuantity(item)"
                        :disabled="(item.quantity || 1) <= 1"
                    >
                      <Minus :size="16"/>
                    </button>
                    <span class="quantity">{{ item.quantity || 1 }}</span>
                    <button
                        class="quantity-btn"
                        @click="increaseQuantity(item)"
                    >
                      <Plus :size="16"/>
                    </button>
                  </div>
                </div>

                <button class="delete-item-btn" @click="deleteItem(item.id)">
                  <X :size="20"/>
                </button>
              </div>
            </div>
          </div>

          <!-- 일반 상품 섹션 -->
          <div v-if="normalItems && normalItems.length > 0" class="cart-section">
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
                  <img
                      :src="getProductImage(item)"
                      :alt="item.name"
                      @error="handleImageError"
                      @load="handleImageLoad"
                  />
                </div>

                <div class="item-details">
                  <h3 class="item-name">{{ item.name || '상품명 없음' }}</h3>

                  <div class="item-price-section">
                    <div class="price-info">
                      <span v-if="hasItemDiscount(item)" class="discount-rate">{{ item.discountRate || 0 }}%</span>
                      <span class="final-price">{{ formatPrice(item.salePrice || 0) }}원</span>
                    </div>
                    <div v-if="hasItemDiscount(item)" class="original-price">{{ formatPrice(item.price || 0) }}원</div>
                  </div>

                  <div class="item-total-price">
                    <span class="total-label">소계: </span>
                    <span class="total-amount">{{ formatPrice(getItemTotalPrice(item)) }}원</span>
                    <span v-if="hasItemDiscount(item)" class="total-discount">
                      ({{ formatPrice(getItemTotalDiscount(item)) }}원 할인)
                    </span>
                  </div>

                  <div class="quantity-controls">
                    <button
                        class="quantity-btn"
                        @click="decreaseQuantity(item)"
                        :disabled="(item.quantity || 1) <= 1"
                    >
                      <Minus :size="16"/>
                    </button>
                    <span class="quantity">{{ item.quantity || 1 }}</span>
                    <button
                        class="quantity-btn"
                        @click="increaseQuantity(item)"
                    >
                      <Plus :size="16"/>
                    </button>
                  </div>
                </div>

                <button class="delete-item-btn" @click="deleteItem(item.id)">
                  <X :size="20"/>
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
            <div v-if="totalDiscount > 0" class="summary-row discount">
              <span class="summary-label">상품할인금액</span>
              <span class="summary-value discount-text">-{{ formatPrice(totalDiscount) }}원</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">배송비</span>
              <span class="summary-value">{{ formatPrice(deliveryFee) }}원</span>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-row total">
              <span class="summary-label">결제예정금액</span>
              <span class="summary-value total-price">{{ formatPrice(finalTotal) }}원</span>
            </div>

          </div>

          <button
              class="checkout-btn"
              @click="goToCheckout"
              :disabled="!selectedItems || selectedItems.length === 0 || !cartItems || cartItems.length === 0 || checkoutLoading"
          >
            <span v-if="checkoutLoading">주문 준비 중...</span>
            <span v-else-if="!cartItems || cartItems.length === 0">장바구니 비어있음</span>
            <span v-else-if="!isLoggedIn">로그인</span>
            <span v-else>주문하기</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ChevronLeft, Minus, Plus, X } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import apiClient from '@/api/axiosInstance.js'
import { useSmartImages } from '@/composables/useSmartImages'

const router = useRouter()
const { getProductImage, handleImageError, handleImageLoad } = useSmartImages()

// 상수 설정
const FREE_DELIVERY_THRESHOLD = 40000
const DELIVERY_FEE = 0

// 안전한 초기화 - 모든 ref를 적절한 기본값으로 초기화
const loading = ref(false)
const checkoutLoading = ref(false)
const cartItems = ref([])
const selectedItems = ref([])
const selectAll = ref(false)
const freeDeliveryThreshold = ref(FREE_DELIVERY_THRESHOLD)
const isLoggedIn = ref(false)

// 토큰 유효성 검사 함수
const isTokenValid = (token) => {
  if (!token) {
    return false
  }

  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return false
    }

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

    return true
  } catch (error) {
    return false
  }
}

// 로그인 상태 확인 함수
const checkLoginStatus = () => {
  const token = localStorage.getItem('jwt')
  const valid = token && isTokenValid(token)
  isLoggedIn.value = valid

  if (!valid && token) {
    localStorage.removeItem('jwt')
  }

  return isLoggedIn.value
}

// 개별 상품 할인 여부 확인 함수
const hasItemDiscount = (item) => {
  if (!item) return false
  return item.discountRate > 0 && item.salePrice < item.price;
}

// 안전한 컴퓨티드 속성들
const frozenItems = computed(() => {
  if (!Array.isArray(cartItems.value)) return []
  return cartItems.value.filter(item => item && item.category === 'frozen')
})

const normalItems = computed(() => {
  if (!Array.isArray(cartItems.value)) return []
  return cartItems.value.filter(item => item && item.category === 'normal')
})

const selectedCartItems = computed(() => {
  if (!Array.isArray(cartItems.value) || !Array.isArray(selectedItems.value)) return []
  return cartItems.value.filter(item => item && selectedItems.value.includes(item.id))
})

const totalProductPrice = computed(() => {
  if (!Array.isArray(selectedCartItems.value)) return 0
  return selectedCartItems.value.reduce((sum, item) => {
    if (!item) return sum
    return sum + ((item.price || 0) * (item.quantity || 1));
  }, 0)
})

const totalSalePrice = computed(() => {
  if (!Array.isArray(selectedCartItems.value)) return 0
  return selectedCartItems.value.reduce((sum, item) => {
    if (!item) return sum
    return sum + ((item.salePrice || 0) * (item.quantity || 1));
  }, 0)
})

const totalDiscount = computed(() => {
  if (!Array.isArray(selectedCartItems.value)) return 0
  return selectedCartItems.value.reduce((sum, item) => {
    if (!item || !hasItemDiscount(item)) return sum
    return sum + (((item.price || 0) - (item.salePrice || 0)) * (item.quantity || 1));
  }, 0)
})

const deliveryFee = computed(() => {
  return totalSalePrice.value >= freeDeliveryThreshold.value ? 0 : DELIVERY_FEE;
})

const finalTotal = computed(() => {
  return totalSalePrice.value + deliveryFee.value;
})

// 안전한 이벤트 핸들러들
const toggleSelectAll = () => {
  if (!Array.isArray(cartItems.value)) return

  if (selectAll.value) {
    selectedItems.value = cartItems.value.map(item => item?.id).filter(Boolean)
  } else {
    selectedItems.value = []
  }
}

// 상품 삭제 함수
const deleteItem = async (itemId) => {
  if (!itemId || !confirm('상품을 장바구니에서 제거하시겠습니까?')) return;

  const item = cartItems.value.find(item => item?.id === itemId);

  if (isLoggedIn.value) {
    try {
      await apiClient.delete(`/api/cart/items/${itemId}`);
    } catch (error) {
      if (error.response?.status === 404) {
        // 서버에 없어도 UI에서는 제거
      } else if (error.response?.status === 401) {
        return // 401은 인터셉터에서 처리
      } else {
        alert('상품 삭제에 실패했습니다.');
        return;
      }
    }
  } else {
    if (item) {
      const guestCart = JSON.parse(localStorage.getItem('guestCart') || '[]');
      const updatedCart = guestCart.filter(cartItem => cartItem.productId !== item.productId);
      localStorage.setItem('guestCart', JSON.stringify(updatedCart));
    }
  }

  cartItems.value = cartItems.value.filter(item => item?.id !== itemId);
  selectedItems.value = selectedItems.value.filter(id => id !== itemId);
}

// 선택 상품 삭제 함수
const deleteSelectedItems = async () => {
  if (!Array.isArray(selectedItems.value) || selectedItems.value.length === 0) return;

  if (!confirm(`선택한 ${selectedItems.value.length}개 상품을 삭제하시겠습니까?`)) return;

  if (isLoggedIn.value) {
    const selectedCartItemIds = cartItems.value
        .filter(item => item && selectedItems.value.includes(item.id))
        .map(item => item.id);

    try {
      await apiClient.delete('/api/cart/items', {
        data: {
          cartItemIds: selectedCartItemIds
        }
      });
    } catch (error) {
      if (error.response?.status === 401) {
        return // 인터셉터에서 처리
      }

      // 일괄 삭제 실패 시 개별 삭제 시도
      for (const cartItemId of selectedCartItemIds) {
        try {
          await apiClient.delete(`/api/cart/items/${cartItemId}`);
        } catch (individualError) {
          // 개별 삭제 실패 처리
        }
      }
    }
  } else {
    const selectedProductIds = cartItems.value
        .filter(item => item && selectedItems.value.includes(item.id))
        .map(item => item.productId);

    const guestCart = JSON.parse(localStorage.getItem('guestCart') || '[]');
    const updatedCart = guestCart.filter(cartItem =>
        !selectedProductIds.includes(cartItem.productId)
    );
    localStorage.setItem('guestCart', JSON.stringify(updatedCart));
  }

  cartItems.value = cartItems.value.filter(item => item && !selectedItems.value.includes(item.id));
  selectedItems.value = [];
  selectAll.value = false;
}

const getItemTotalPrice = (item) => {
  if (!item) return 0
  return (item.salePrice || 0) * (item.quantity || 1);
}

const getItemTotalDiscount = (item) => {
  if (!item || !hasItemDiscount(item)) return 0
  return ((item.price || 0) - (item.salePrice || 0)) * (item.quantity || 1);
}

const updateGuestCartQuantity = (productId, newQuantity) => {
  try {
    const guestCart = JSON.parse(localStorage.getItem('guestCart') || '[]');
    const itemIndex = guestCart.findIndex(item => item.productId === productId);

    if (itemIndex >= 0) {
      guestCart[itemIndex].quantity = newQuantity;
      localStorage.setItem('guestCart', JSON.stringify(guestCart));
    }
  } catch (error) {
    // 에러 처리
  }
}

const goToCheckout = async () => {
  try {
    checkoutLoading.value = true;

    // 1. 로그인 상태 확인
    const currentLoginStatus = checkLoginStatus()

    if (!currentLoginStatus) {
      alert('주문하려면 로그인이 필요합니다.')
      router.push('/login')
      return
    }

    // 2. 선택된 상품 확인
    const selectedProducts = cartItems.value.filter(item =>
        item && selectedItems.value.includes(item.id)
    )

    if (selectedProducts.length === 0) {
      alert('주문할 상품을 선택해주세요.')
      return
    }

    // 3. 주문 데이터 생성
    const checkoutData = {
      items: selectedProducts,
      totalPrice: finalTotal.value,
      productAmount: totalProductPrice.value,
      discountAmount: totalDiscount.value,
      deliveryFee: deliveryFee.value
    }

    // 4. 세션 저장
    sessionStorage.setItem('checkout_data', JSON.stringify(checkoutData))

    // 5. 주문서로 이동
    router.push('/checkout')

  } catch (error) {
    alert('주문 페이지로 이동 중 오류가 발생했습니다.')
  } finally {
    checkoutLoading.value = false;
  }
}

const goBack = () => {
  router.go(-1)
}

const formatPrice = (price) => {
  return (price || 0).toLocaleString()
}

// 안전한 선택 상품 감시
watch(selectedItems, () => {
  if (!Array.isArray(cartItems.value) || !Array.isArray(selectedItems.value)) return
  selectAll.value = selectedItems.value.length === cartItems.value.length && cartItems.value.length > 0
}, { deep: true })

// 컴포넌트 마운트
onMounted(async () => {
  loading.value = true

  try {
    const loginStatus = checkLoginStatus()

    // 결제 완료 후 돌아온 경우 체크
    const urlParams = new URLSearchParams(window.location.search)
    const isFromPayment = urlParams.get('from') === 'payment' ||
        urlParams.get('payment') === 'complete' ||
        sessionStorage.getItem('payment_completed') === 'true'

    // 장바구니 정리 완료 체크
    const cartCleaned = sessionStorage.getItem('cart_cleaned_after_payment') === 'true'
    const lastCleanup = sessionStorage.getItem('last_purchase_cleanup')
    const cleanupRecent = lastCleanup && (Date.now() - parseInt(lastCleanup)) < 30000 // 30초 이내

    if (isFromPayment) {
      // 결제 완료 후 세션 정리
      sessionStorage.removeItem('checkout_data')
      sessionStorage.removeItem('pending_order_data')
      sessionStorage.removeItem('payment_completed')

      // URL 정리
      if (urlParams.has('from') || urlParams.has('payment')) {
        const cleanUrl = window.location.pathname
        window.history.replaceState({}, '', cleanUrl)
      }

      // 정리 마킹도 제거 (한 번만 알림)
      if (cartCleaned) {
        sessionStorage.removeItem('cart_cleaned_after_payment')
        sessionStorage.removeItem('last_purchase_cleanup')
      }
    }

    // 서버/로컬 장바구니 로드 로직
    if (loginStatus) {
      // 로그인 사용자 - 서버에서 장바구니 로드
      try {
        // 결제 완료 후라면 캐시 무시하고 새로 로드
        const cacheParam = isFromPayment || cartCleaned ? `?_t=${Date.now()}` : ''
        const response = await apiClient.get(`/api/cart${cacheParam}`)

        if (response.data.success && Array.isArray(response.data.data?.cartItems)) {
          const serverItems = response.data.data.cartItems
              .map(mapCartItemToProduct)
              .filter(Boolean)

          cartItems.value = serverItems
          selectedItems.value = serverItems.map(item => item.id)
          selectAll.value = serverItems.length > 0

        } else {
          cartItems.value = []
        }

      } catch (error) {
        cartItems.value = []
      }
    } else {
      // 게스트 사용자 - 로컬 스토리지에서 장바구니 로드
      try {
        const localCart = JSON.parse(localStorage.getItem('guestCart') || '[]')

        if (Array.isArray(localCart) && localCart.length > 0) {
          // 게스트 장바구니 처리 로직
          cartItems.value = localCart.map(mapCartItemToProduct).filter(Boolean)
          selectedItems.value = cartItems.value.map(item => item.id)
          selectAll.value = cartItems.value.length > 0
        } else {
          cartItems.value = []
        }
      } catch (error) {
        cartItems.value = []
      }
    }

    // 결제 완료 알림 (한 번만)
    if (isFromPayment && cartCleaned && cleanupRecent) {
      setTimeout(() => {
        // 중복 알림 방지
        if (!sessionStorage.getItem('payment_success_notified')) {
          sessionStorage.setItem('payment_success_notified', 'true')

          // 5초 후 알림 제거
          setTimeout(() => {
            sessionStorage.removeItem('payment_success_notified')
          }, 5000)

          alert('결제가 완료되었습니다! 구매하신 상품이 장바구니에서 제거되었습니다.')
        }
      }, 1000)
    }

  } catch (error) {
    cartItems.value = []
  } finally {
    loading.value = false
  }
})

// 수량 증가 함수
const increaseQuantity = async (item) => {
  if (!item) return

  const originalQuantity = item.quantity || 1;
  item.quantity = originalQuantity + 1;

  if (isLoggedIn.value) {
    try {
      const requestPayload = {
        cartItemId: item.cartItemId || item.id,
        quantity: item.quantity
      }

      const response = await apiClient.put('/api/cart/items', requestPayload);

    } catch (error) {
      // 수량 롤백
      item.quantity = originalQuantity;

      if (error.response?.status === 404) {
        alert('장바구니 상품을 찾을 수 없습니다. 장바구니를 다시 불러오겠습니다.');
        await loadCartItems();
        return;
      } else if (error.response?.status === 401) {
        return // 인터셉터에서 처리
      } else {
        alert('수량 변경에 실패했습니다: ' + (error.response?.data?.message || error.message));
      }
    }
  } else {
    updateGuestCartQuantity(item.productId, item.quantity);
  }
}

const loadCartItems = async () => {
  loading.value = true;
  try {
    if (isLoggedIn.value) {
      const response = await apiClient.get('/api/cart');
      if (response.data.success && Array.isArray(response.data.data?.cartItems)) {
        const serverItems = response.data.data.cartItems
            .map(mapCartItemToProduct)
            .filter(Boolean);

        cartItems.value = serverItems;
        selectedItems.value = serverItems.map(item => item.id);
        selectAll.value = serverItems.length > 0;
      }
    }
  } catch (error) {
    // 장바구니 재로드 실패 처리
  } finally {
    loading.value = false;
  }
}

// 수량 감소 함수
const decreaseQuantity = async (item) => {
  if (!item || (item.quantity || 1) <= 1) return;

  const originalQuantity = item.quantity || 1;
  item.quantity = originalQuantity - 1;

  if (isLoggedIn.value) {
    try {
      const requestPayload = {
        cartItemId: item.cartItemId || item.id,
        quantity: item.quantity
      }

      const response = await apiClient.put('/api/cart/items', requestPayload);

    } catch (error) {
      // 수량 롤백
      item.quantity = originalQuantity;

      if (error.response?.status === 404) {
        alert('장바구니 상품을 찾을 수 없습니다. 페이지를 새로고침합니다.');
        window.location.reload();
      } else if (error.response?.status === 401) {
        // 401은 인터셉터에서 처리됨
        return
      } else {
        alert('수량 변경에 실패했습니다: ' + (error.response?.data?.message || error.message));
      }
    }
  } else {
    updateGuestCartQuantity(item.productId, item.quantity);
  }
}

// 상품 매핑 함수
const mapCartItemToProduct = (cartItem) => {
  if (!cartItem) return null

  const originalPrice = cartItem.productPrice || cartItem.price || 0;
  const salePrice = cartItem.salePrice || originalPrice;

  let discountRate = 0;
  if (originalPrice > 0 && salePrice < originalPrice) {
    discountRate = Math.floor(((originalPrice - salePrice) / originalPrice) * 100);
    if (discountRate <= 0 || discountRate >= 100) {
      discountRate = 0;
    }
  }

  return {
    // id와 cartItemId를 모두 설정
    id: cartItem.cartItemId || cartItem.id || `item_${cartItem.productId}_${Date.now()}`,
    cartItemId: cartItem.cartItemId, // 실제 cartItemId 추가
    productId: cartItem.productId,
    name: cartItem.productName || cartItem.name || '상품명 없음',
    price: originalPrice,
    salePrice: salePrice > 0 && salePrice < originalPrice ? salePrice : originalPrice,
    discountRate: discountRate,
    quantity: cartItem.quantity || 1,
    image: cartItem.productImage || cartItem.image,
    mainImage: cartItem.mainImage,
    category: cartItem.category || 'normal',
    deliveryType: cartItem.deliveryType || 'normal'
  }
}

</script>
<style scoped src="@/assets/css/cart.css"></style>
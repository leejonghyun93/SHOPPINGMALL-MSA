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
                  <img :src="item.image" :alt="item.name" @error="handleImageError"/>
                </div>

                <div class="item-details">
                  <h3 class="item-name">{{ item.name }}</h3>

                  <div class="item-price-section">
                    <div class="price-info">
                      <span v-if="hasItemDiscount(item)" class="discount-rate">{{ item.discountRate }}%</span>
                      <span class="final-price">{{ formatPrice(item.salePrice) }}원</span>
                    </div>
                    <div v-if="hasItemDiscount(item)" class="original-price">{{ formatPrice(item.price) }}원</div>
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
                        :disabled="item.quantity <= 1"
                    >
                      <Minus :size="16"/>
                    </button>
                    <span class="quantity">{{ item.quantity }}</span>
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
                  <img :src="item.image" :alt="item.name" @error="handleImageError"/>
                </div>

                <div class="item-details">
                  <h3 class="item-name">{{ item.name }}</h3>

                  <div class="item-price-section">
                    <div class="price-info">
                      <span v-if="hasItemDiscount(item)" class="discount-rate">{{ item.discountRate }}%</span>
                      <span class="final-price">{{ formatPrice(item.salePrice) }}원</span>
                    </div>
                    <div v-if="hasItemDiscount(item)" class="original-price">{{ formatPrice(item.price) }}원</div>
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
                        :disabled="item.quantity <= 1"
                    >
                      <Minus :size="16"/>
                    </button>
                    <span class="quantity">{{ item.quantity }}</span>
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
              <span class="summary-label">할인적용가격</span>
              <span class="summary-value">{{ formatPrice(totalSalePrice) }}원</span>
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
import { useRouter } from 'vue-router'
import apiClient from '@/api/axiosInstance'

const router = useRouter()

// 상수 설정
const FREE_DELIVERY_THRESHOLD = 40000
const DELIVERY_FEE = 0

// 반응형 상태
const loading = ref(false)
const cartItems = ref([])
const selectedItems = ref([])
const selectAll = ref(false)
const freeDeliveryThreshold = ref(FREE_DELIVERY_THRESHOLD)

// 상품 매핑 함수
const mapCartItemToProduct = (cartItem) => {
  const originalPrice = cartItem.productPrice || cartItem.price || 0;
  const salePrice = cartItem.salePrice || originalPrice;

  // 할인율 계산 개선
  let discountRate = 0;
  if (originalPrice > 0 && salePrice < originalPrice) {
    discountRate = Math.floor(((originalPrice - salePrice) / originalPrice) * 100);
    // 할인율 제한: 0% ~ 99%
    if (discountRate <= 0 || discountRate >= 100) {
      discountRate = 0;
    }
  }

  return {
    id: cartItem.cartItemId || cartItem.id || `item_${cartItem.productId}_${Date.now()}`,
    productId: cartItem.productId,
    name: cartItem.productName || cartItem.name || '상품명 없음',
    price: originalPrice,
    salePrice: salePrice > 0 && salePrice < originalPrice ? salePrice : originalPrice,
    discountRate: discountRate,
    quantity: cartItem.quantity || 1,
    image: cartItem.productImage || cartItem.image || generatePlaceholderImage(),
    category: cartItem.category || 'normal',
    deliveryType: cartItem.deliveryType || 'normal'
  }
}

// 개별 상품 할인 여부 확인 함수
const hasItemDiscount = (item) => {
  return item.discountRate > 0 && item.salePrice < item.price;
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
    selectedCartItems.value.reduce((sum, item) => {
      return sum + (item.price * item.quantity);
    }, 0)
)

const totalSalePrice = computed(() =>
    selectedCartItems.value.reduce((sum, item) => {
      return sum + (item.salePrice * item.quantity);
    }, 0)
)

const totalDiscount = computed(() => {
  return selectedCartItems.value.reduce((sum, item) => {
    if (hasItemDiscount(item)) {
      return sum + ((item.price - item.salePrice) * item.quantity);
    }
    return sum;
  }, 0)
})

const deliveryFee = computed(() => {
  return totalSalePrice.value >= freeDeliveryThreshold.value ? 0 : DELIVERY_FEE;
})

const finalTotal = computed(() => {
  return totalSalePrice.value + deliveryFee.value;
})

// 이벤트 핸들러들
const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedItems.value = cartItems.value.map(item => item.id)
  } else {
    selectedItems.value = []
  }
}

const increaseQuantity = async (item) => {
  const token = localStorage.getItem('token');
  const originalQuantity = item.quantity;
  item.quantity++;

  if (token) {
    try {
      await apiClient.put('/api/cart/items', {
        cartItemId: item.id,
        quantity: item.quantity
      }, {
        withAuth: true
      });
    } catch (error) {
      item.quantity = originalQuantity;
      if (error.response?.status === 404) {
        alert('장바구니 상품을 찾을 수 없습니다. 페이지를 새로고침합니다.');
        window.location.reload();
      } else {
        alert('수량 변경에 실패했습니다.');
      }
    }
  } else {
    updateGuestCartQuantity(item.productId, item.quantity);
  }
}

const decreaseQuantity = async (item) => {
  if (item.quantity <= 1) return;

  const token = localStorage.getItem('token');
  const originalQuantity = item.quantity;
  item.quantity--;

  if (token) {
    try {
      await apiClient.put('/api/cart/items', {
        cartItemId: item.id,
        quantity: item.quantity
      }, {
        withAuth: true
      });
    } catch (error) {
      item.quantity = originalQuantity;
      if (error.response?.status === 404) {
        alert('장바구니 상품을 찾을 수 없습니다. 페이지를 새로고침합니다.');
        window.location.reload();
      } else {
        alert('수량 변경에 실패했습니다.');
      }
    }
  } else {
    updateGuestCartQuantity(item.productId, item.quantity);
  }
}

const deleteItem = async (itemId) => {
  if (!confirm('상품을 장바구니에서 제거하시겠습니까?')) return;

  const token = localStorage.getItem('token');
  const item = cartItems.value.find(item => item.id === itemId);

  if (token) {
    try {
      await apiClient.delete(`/api/cart/items/${itemId}`, {
        withAuth: true
      });
    } catch (error) {
      if (error.response?.status === 404) {
        // 서버에 없어도 UI에서는 제거
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

  cartItems.value = cartItems.value.filter(item => item.id !== itemId);
  selectedItems.value = selectedItems.value.filter(id => id !== itemId);
}

const deleteSelectedItems = async () => {
  if (selectedItems.value.length === 0) return;

  if (!confirm(`선택한 ${selectedItems.value.length}개 상품을 삭제하시겠습니까?`)) return;

  const token = localStorage.getItem('token');

  if (token) {
    const selectedCartItemIds = cartItems.value
        .filter(item => selectedItems.value.includes(item.id))
        .map(item => item.id);

    try {
      await apiClient.delete('/api/cart/items', {
        data: {
          cartItemIds: selectedCartItemIds
        },
        withAuth: true
      });
    } catch (error) {
      for (const cartItemId of selectedCartItemIds) {
        try {
          await apiClient.delete(`/api/cart/items/${cartItemId}`, {
            withAuth: true
          });
        } catch (individualError) {
          console.error(`상품 ${cartItemId} 삭제 실패:`, individualError);
        }
      }
    }
  } else {
    const selectedProductIds = cartItems.value
        .filter(item => selectedItems.value.includes(item.id))
        .map(item => item.productId);

    const guestCart = JSON.parse(localStorage.getItem('guestCart') || '[]');
    const updatedCart = guestCart.filter(cartItem =>
        !selectedProductIds.includes(cartItem.productId)
    );
    localStorage.setItem('guestCart', JSON.stringify(updatedCart));
  }

  cartItems.value = cartItems.value.filter(item => !selectedItems.value.includes(item.id));
  selectedItems.value = [];
  selectAll.value = false;
}

const getItemTotalPrice = (item) => {
  return item.salePrice * item.quantity;
}

const getItemTotalDiscount = (item) => {
  if (hasItemDiscount(item)) {
    return (item.price - item.salePrice) * item.quantity;
  }
  return 0;
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
    console.error('게스트 장바구니 업데이트 실패:', error);
  }
}

const goToCheckout = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    alert('주문하려면 로그인이 필요합니다.')
    router.push('/login')
    return
  }

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
      deliveryFee: deliveryFee.value
    }

    sessionStorage.setItem('checkout_data', JSON.stringify(checkoutData))
    router.push('/checkout')
  } catch (error) {
    alert('주문 페이지로 이동 중 오류가 발생했습니다.')
  }
}

const goBack = () => {
  router.go(-1)
}

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

watch(selectedItems, () => {
  selectAll.value = selectedItems.value.length === cartItems.value.length && cartItems.value.length > 0
}, { deep: true })

onMounted(async () => {
  loading.value = true

  const token = localStorage.getItem('token')
  const isLoggedIn = !!token

  if (isLoggedIn) {
    try {
      const response = await apiClient.get('/api/cart', {
        withAuth: true
      })

      if (response.data.success && response.data.data?.cartItems?.length > 0) {
        const serverItems = response.data.data.cartItems.map(mapCartItemToProduct)
        cartItems.value = serverItems
        selectedItems.value = serverItems.map(item => item.id)
        selectAll.value = serverItems.length > 0
      } else {
        cartItems.value = []
      }

    } catch (error) {
      if (error.response?.status === 401) {
        localStorage.removeItem('token')
      }
      cartItems.value = []
    }
  } else {
    try {
      const localCart = JSON.parse(localStorage.getItem('guestCart') || '[]')

      if (localCart.length > 0) {
        const productIds = localCart.map(item => item.productId)

        const requestData = productIds.map(productId => ({
          productId: productId,
          quantity: localCart.find(item => item.productId === productId)?.quantity || 1
        }))

        const response = await apiClient.post('/api/products/guest-cart-details', requestData, {
          withAuth: false
        })

        const enrichedItems = response.data.map(product => {
          const localItem = localCart.find(i => i.productId === product.productId)
          return mapCartItemToProduct({
            ...product,
            cartItemId: `local_${product.productId}`,
            quantity: localItem?.quantity || 1,
            productName: product.name || product.title,
            productImage: product.mainImage || product.image,
            productPrice: product.price,
            salePrice: product.salePrice || product.price,
            discountRate: product.discountRate || 0
          })
        })

        cartItems.value = enrichedItems
        selectedItems.value = enrichedItems.map(item => item.id)
        selectAll.value = enrichedItems.length > 0
      } else {
        cartItems.value = []
      }

    } catch (error) {
      cartItems.value = []
    }
  }

  loading.value = false
})
</script>

<style scoped src="@/assets/css/cart.css"></style>
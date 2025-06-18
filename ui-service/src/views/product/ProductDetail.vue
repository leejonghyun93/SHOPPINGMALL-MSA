<template>
  <div class="product-detail-container">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>상품 정보를 불러오는 중...</p>
    </div>

    <!-- 에러 상태 -->
    <div v-else-if="error" class="error-container">
      <h3>상품을 불러올 수 없습니다</h3>
      <p>{{ error }}</p>
      <button @click="loadProduct" class="retry-button">다시 시도</button>
    </div>

    <!-- 상품 상세 내용 -->
    <div v-else-if="product">
      <!-- 헤더 -->
      <div class="product-header">
        <button class="back-button" @click="goBack">
          <ChevronLeft :size="24" />
        </button>
        <div class="header-actions">
          <button class="share-button" @click="handleShare">
            <Share2 :size="20" />
          </button>
        </div>
      </div>

      <!-- 상품 이미지 섹션 -->
      <div class="product-images">
        <div class="main-image">
          <img :src="getCurrentImage()" :alt="product.name || product.title" @error="handleImageError" />
          <div v-if="product.isLive" class="live-badge">
            <span class="live-dot"></span>
            LIVE
          </div>
          <div v-if="getDiscountRate() > 0" class="discount-badge">
            {{ getDiscountRate() }}% 할인
          </div>
        </div>
        <div v-if="product.images && product.images.length > 1" class="image-indicators">
          <div
              v-for="(image, index) in product.images"
              :key="index"
              :class="['indicator', { active: currentImageIndex === index }]"
              @click="currentImageIndex = index"
          ></div>
        </div>
      </div>

      <!-- 상품 정보 섹션 -->
      <div class="product-info-section">
        <div class="brand-info" v-if="product.brand">
          <span class="brand-label">브랜드관</span>
          <span class="brand-name">{{ product.brand }}</span>
        </div>

        <h1 class="product-title">{{ product.name || product.title }}</h1>
        <p v-if="product.subtitle || product.productShortDescription" class="product-subtitle">
          {{ product.subtitle || product.productShortDescription }}
        </p>

        <div class="price-section">
          <div class="discount-info">
            <span v-if="getDiscountRate() > 0" class="discount-rate">{{ getDiscountRate() }}%</span>
            <span class="final-price">{{ formatPrice(getFinalPrice()) }}원</span>
          </div>
          <div v-if="getDiscountRate() > 0" class="original-price">{{ formatPrice(product.price) }}원</div>
        </div>

        <div class="delivery-notice">
          <span class="delivery-text">첫 구매라면 10,000원! 즉시 할인!</span>
          <ChevronLeft class="chevron-right" :size="16" />
        </div>

        <!-- 상품 상세 정보 테이블 -->
        <div class="product-details-table">
          <div class="detail-row">
            <span class="detail-label">배송</span>
            <span class="detail-value">샛별배송</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">판매자</span>
            <span class="detail-value">컬리</span>
          </div>
          <div v-if="product.deliveryInfo" class="detail-row">
            <span class="detail-label">포장타입</span>
            <span class="detail-value">{{ product.deliveryInfo }}</span>
          </div>
          <div v-if="product.packaging" class="detail-row">
            <span class="detail-label">판매단위</span>
            <span class="detail-value">{{ product.packaging }}</span>
          </div>
          <div v-if="product.weight" class="detail-row">
            <span class="detail-label">중량/용량</span>
            <span class="detail-value">{{ product.weight }}</span>
          </div>
          <div v-if="product.origin" class="detail-row">
            <span class="detail-label">원산지</span>
            <span class="detail-value">{{ product.origin }}</span>
          </div>
          <div v-if="product.allergyInfo" class="detail-row">
            <span class="detail-label">알레르기정보</span>
            <span class="detail-value">{{ product.allergyInfo }}</span>
          </div>
        </div>
      </div>

      <!-- 구매 액션 섹션 -->
      <div class="purchase-section">
        <div class="total-price">
          <span class="total-label">총 상품금액:</span>
          <span class="total-amount">{{ formatPrice(getFinalPrice() * quantity) }} 원</span>
        </div>

        <div class="action-buttons">
          <div class="quantity-and-wishlist">
            <button
                :class="['wishlist-button', { active: isWishlisted }]"
                @click="toggleWishlist"
            >
              <Heart :size="20" :fill="isWishlisted ? '#ff4444' : 'none'" />
            </button>
            <button
                class="notification-button"
                @click="toggleNotification"
            >
              <Bell :size="20" />
            </button>
          </div>
          <button class="buy-now-button" @click="handleAddToCart">
            장바구니 담기
          </button>
        </div>
      </div>

      <!-- 탭 섹션 -->
      <div class="tabs-section">
        <div class="tab-headers">
          <button
              v-for="tab in tabs"
              :key="tab.id"
              :class="['tab-header', { active: selectedTab === tab.id }]"
              @click="selectedTab = tab.id"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="tab-content">
          <div v-if="selectedTab === 'details'" class="details-content">
            <div v-if="product.detailImages && product.detailImages.length > 0">
              <img
                  v-for="(image, index) in product.detailImages"
                  :key="index"
                  :src="image"
                  :alt="`상품 상세 이미지 ${index + 1}`"
                  @error="handleImageError"
              />
            </div>
            <div v-else class="no-detail-images">
              <p>{{ product.productDescription || '상품 상세 이미지가 준비 중입니다.' }}</p>
            </div>
          </div>

          <div v-if="selectedTab === 'info'" class="info-content">
            <h3>상품 정보</h3>
            <p v-if="product.origin">원산지: {{ product.origin }}</p>
            <p v-if="product.deliveryInfo">포장타입: {{ product.deliveryInfo }}</p>
            <p v-if="product.packaging">판매단위: {{ product.packaging }}</p>
            <p v-if="product.weight">중량/용량: {{ product.weight }}</p>
            <p v-if="product.ingredients">원재료명: {{ product.ingredients }}</p>
            <p v-if="product.productDescription">상품설명: {{ product.productDescription }}</p>
          </div>

          <div v-if="selectedTab === 'reviews'" class="reviews-content">
            <div class="review-summary">
              <div class="rating-overview">
                <div class="average-rating">
                  <span class="rating-score">{{ getAverageRating() }}</span>
                  <div class="stars">
                    <Star v-for="i in 5" :key="i" :size="16" :fill="i <= Math.floor(getAverageRating()) ? '#ffc107' : 'none'" />
                  </div>
                </div>
                <span class="review-count">({{ getReviewCount() }}개 후기)</span>
              </div>
            </div>

            <div class="review-list">
              <div v-if="reviews.length === 0" class="no-reviews">
                <p>아직 작성된 후기가 없습니다.</p>
              </div>
              <div v-else class="review-item" v-for="review in reviews" :key="review.id">
                <div class="reviewer-info">
                  <span class="reviewer-name">{{ maskUserName(review.userName) }}</span>
                  <div class="review-rating">
                    <Star v-for="i in 5" :key="i" :size="12" :fill="i <= review.rating ? '#ffc107' : 'none'" />
                  </div>
                </div>
                <p class="review-text">{{ review.content }}</p>
                <span class="review-date">{{ formatDate(review.createdAt) }}</span>
              </div>
            </div>
          </div>

          <div v-if="selectedTab === 'inquiry'" class="inquiry-content">
            <p>상품 문의는 고객센터로 연락주세요.</p>
            <div class="contact-info">
              <p>📞 고객센터: 1588-1234</p>
              <p>⏰ 운영시간: 09:00 ~ 18:00 (주말, 공휴일 휴무)</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 연관 상품 섹션 -->
      <div v-if="relatedProducts.length > 0" class="related-products-section">
        <h3>이런 상품도 있어요</h3>
        <div class="related-products-grid">
          <div
              v-for="relatedProduct in relatedProducts"
              :key="relatedProduct.productId"
              class="related-product-item"
              @click="goToProduct(relatedProduct.productId)"
          >
            <img :src="getProductImage(relatedProduct)" :alt="relatedProduct.name || relatedProduct.title" @error="handleImageError" />
            <div class="related-product-info">
              <p class="related-product-title">{{ relatedProduct.name || relatedProduct.title }}</p>
              <p class="related-product-price">{{ formatPrice(relatedProduct.salePrice || relatedProduct.price) }}원</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 선택 상세 버튼 (하단 고정) -->
      <div class="detail-selection-button">
        <button class="selection-detail-btn" @click="showSelectionModal">
          상품 선택
          <Plus :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ChevronLeft, Share2, Heart, Bell, Star, Plus } from 'lucide-vue-next'
// 🔥 수정: axios 대신 apiClient 사용
import apiClient from '@/api/axiosInstance' // 실제 파일 위치에 맞게 수정 필요

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const error = ref(null)
const product = ref(null)
const relatedProducts = ref([])
const reviews = ref([])
const selectedTab = ref('details')
const quantity = ref(1)
const isWishlisted = ref(false)
const showNotification = ref(false)
const currentImageIndex = ref(0)

const tabs = computed(() => [
  { id: 'details', label: '상품설명' },
  { id: 'info', label: '상세정보' },
  { id: 'reviews', label: `후기 (${getReviewCount()})` },
  { id: 'inquiry', label: '문의' }
])

// 🔥 수정: 토큰 키 이름 통일
const getAuthToken = () => {
  return localStorage.getItem('token') // 실제 저장된 키 이름과 일치
}

// 🔥 수정: JWT 디코딩 함수 개선
function base64UrlDecode(str) {
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  while (base64.length % 4) {
    base64 += '='
  }
  return atob(base64)
}

const isAuthenticated = () => {
  const token = getAuthToken()
  if (!token) {
    console.log('🔓 토큰이 없음')
    return false
  }
  try {
    const payloadJson = base64UrlDecode(token.split('.')[1])
    const payload = JSON.parse(payloadJson)
    const isValid = payload.exp > Date.now() / 1000
    console.log('🔐 토큰 검증:', {
      valid: isValid,
      exp: new Date(payload.exp * 1000),
      now: new Date()
    })
    return isValid
  } catch (e) {
    console.error('❌ JWT 디코딩 실패:', e)
    return false
  }
}

// 🔥 제거: setupAxiosInterceptors 함수 삭제 (apiClient에서 처리)

const getDiscountRate = () => product.value?.discountRate || product.value?.discount || 0
const getAverageRating = () => product.value?.averageRating || product.value?.productRating || 4.5
const getReviewCount = () => product.value?.reviewCount || product.value?.productReviewCount || 0

const getProductImage = (prod) => {
  if (prod.images?.length > 0) return prod.images[0]
  return prod.mainImage || prod.image || 'https://via.placeholder.com/300x200?text=상품+이미지'
}

// 🔥 수정: loadProduct 함수 - apiClient 사용
const loadProduct = async () => {
  try {
    loading.value = true
    error.value = null
    const productId = route.params.id

    console.log('🔍 상품 조회 시작:', productId)

    // 🔥 프록시 사용으로 /api 경로 사용
    const response = await apiClient.get(`/api/products/${productId}`, {
      withAuth: false // 상품 조회는 인증 불필요
    })

    product.value = response.data
    console.log('✅ 상품 조회 성공:', product.value)

    await loadRelatedProducts(productId)
  } catch (err) {
    console.error('❌ 상품 조회 실패:', err)
    error.value = err.response?.data?.message || '상품 정보를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

// 🔥 수정: loadRelatedProducts 함수 - apiClient 사용
const loadRelatedProducts = async (productId) => {
  try {
    console.log('🔍 연관 상품 조회 시작:', productId)

    const res = await apiClient.get(`/api/products/${productId}/related?limit=4`, {
      withAuth: false // 연관 상품 조회도 인증 불필요
    })

    relatedProducts.value = res.data || []
    console.log('✅ 연관 상품 조회 성공:', relatedProducts.value.length)
  } catch (err) {
    console.error('❌ 연관 상품 조회 실패:', err)
    relatedProducts.value = []
  }
}

const goBack = () => router.go(-1)
const goToProduct = (id) => router.push(`/product/${id}`)

const handleShare = () => {
  if (navigator.share) {
    navigator.share({
      title: product.value?.name,
      text: product.value?.subtitle || product.value?.productShortDescription,
      url: window.location.href
    }).catch(console.error)
  } else {
    navigator.clipboard.writeText(window.location.href)
    alert('상품 링크가 복사되었습니다!')
  }
}

const toggleWishlist = () => {
  isWishlisted.value = !isWishlisted.value
}

const toggleNotification = () => {
  showNotification.value = !showNotification.value
}

// 🔥 수정: handleAddToCart 함수 완전 개선
const handleAddToCart = async () => {
  console.log('🛒 장바구니 담기 시작');

  // 상품 정보 검증
  if (!product.value?.productId) {
    alert('상품 정보를 찾을 수 없습니다.');
    return;
  }

  const cartItem = {
    productId: product.value.productId,
    quantity: quantity.value,
    productOptionId: null
  };

  // 🔍 토큰 상태 상세 확인
  const token = localStorage.getItem('token');
  console.log('🔐 토큰 확인:', {
    exists: !!token,
    length: token?.length,
    preview: token ? token.substring(0, 50) + '...' : 'No token'
  });

  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      console.log('🔍 JWT 페이로드:', {
        sub: payload.sub,
        name: payload.name,
        iss: payload.iss,
        exp: new Date(payload.exp * 1000),
        valid: payload.exp > Date.now() / 1000
      });
    } catch (e) {
      console.error('❌ JWT 디코딩 실패:', e);
    }
  }

  // 비인증 사용자 처리
  if (!isAuthenticated()) {
    console.log('🔓 비인증 사용자 - 로컬 스토리지 사용');
    // ... 기존 로컬 스토리지 로직
    return;
  }

  // 🔍 Manual 요청 테스트 (디버깅용)
  try {
    console.log('📡 수동 요청 테스트 시작');

    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    console.log('📤 요청 헤더:', headers);
    console.log('📦 요청 데이터:', cartItem);

    const response = await fetch('/api/cart', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(cartItem)
    });

    console.log('📨 응답 상태:', response.status);
    console.log('📨 응답 헤더:', Object.fromEntries(response.headers.entries()));

    if (response.ok) {
      const data = await response.json();
      console.log('✅ 응답 데이터:', data);

      const goToCart = confirm('장바구니에 추가되었습니다! 장바구니로 이동하시겠습니까?');
      if (goToCart) {
        router.push('/cart');
      }
    } else {
      const errorText = await response.text();
      console.error('❌ 에러 응답:', {
        status: response.status,
        statusText: response.statusText,
        body: errorText
      });

      if (response.status === 401) {
        alert('인증이 필요합니다. 다시 로그인해주세요.');
        localStorage.removeItem('token');
        router.push('/login');
      } else {
        alert(`요청 실패: ${response.status} ${response.statusText}`);
      }
    }

  } catch (error) {
    console.error('❌ 네트워크 오류:', error);
    alert('네트워크 오류가 발생했습니다.');
  }
};

const showSelectionModal = () => {
  // 상품 옵션 선택 모달 표시 로직
}

const getCurrentImage = () => {
  if (product.value?.images?.length > 0)
    return product.value.images[currentImageIndex.value] || product.value.images[0]
  return product.value?.mainImage || product.value?.image || 'https://via.placeholder.com/600x600?text=상품+이미지'
}

const getFinalPrice = () => {
  if (!product.value) return 0
  if (product.value.salePrice > 0) return product.value.salePrice
  if (product.value.discountPrice > 0) return product.value.discountPrice
  const discountRate = getDiscountRate()
  if (discountRate > 0 && product.value.price) {
    return Math.floor(product.value.price * (100 - discountRate) / 100)
  }
  return product.value.price || 0
}

const formatPrice = (price) => price?.toLocaleString() || '0'
const formatDate = (date) => new Date(date).toLocaleDateString('ko-KR')
const maskUserName = (name) => name?.charAt(0) + '*'.repeat(name.length - 1)
const handleImageError = (e) => {
  if (e.target.dataset.errorHandled) return
  e.target.dataset.errorHandled = 'true'
  e.target.style.display = 'none'
  const placeholder = document.createElement('div')
  placeholder.className = 'image-placeholder'
  placeholder.innerHTML = '이미지 없음'
  e.target.parentNode.appendChild(placeholder)
}

onMounted(() => {
  // 🔥 제거: setupAxiosInterceptors() 호출 삭제
  loadProduct()
})

watch(() => route.params.id, (newId) => {
  if (newId) loadProduct()
})
</script>

<style scoped src="@/assets/css/productDetail.css"></style>
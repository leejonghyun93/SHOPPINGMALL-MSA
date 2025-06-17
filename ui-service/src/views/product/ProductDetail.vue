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
import axios from 'axios'

// 라우터 설정
const router = useRouter()
const route = useRoute()

// 반응형 상태
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

// API 기본 URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// 탭 데이터 (computed로 만들어서 동적 업데이트)
const tabs = computed(() => [
  { id: 'details', label: '상품설명' },
  { id: 'info', label: '상세정보' },
  { id: 'reviews', label: `후기 (${getReviewCount()})` },
  { id: 'inquiry', label: '문의' }
])

// 유틸리티 메소드들
const getDiscountRate = () => {
  if (!product.value) return 0
  return product.value.discountRate || product.value.discount || 0
}

const getAverageRating = () => {
  if (!product.value) return 4.5
  return product.value.averageRating ||
      (product.value.productRating ? product.value.productRating : 4.5)
}

const getReviewCount = () => {
  if (!product.value) return 0
  return product.value.reviewCount || product.value.productReviewCount || 0
}

const getProductImage = (prod) => {
  if (prod.images && prod.images.length > 0) {
    return prod.images[0]
  }
  return prod.mainImage || prod.image || 'https://via.placeholder.com/300x200?text=상품+이미지'
}

// 메소드들
const loadProduct = async () => {
  try {
    loading.value = true
    error.value = null

    const productId = route.params.id
    console.log('상품 ID:', productId)

    // 상품 상세 정보 조회
    const response = await axios.get(`${API_BASE_URL}/api/products/${productId}`)

    if (response.data) {
      product.value = response.data
      console.log('상품 정보 로드 완료:', product.value)

      // 연관 상품 로드
      loadRelatedProducts(productId)
    } else {
      throw new Error('상품 정보를 찾을 수 없습니다.')
    }
  } catch (err) {
    console.error('상품 로드 실패:', err)
    error.value = err.response?.data?.message || '상품 정보를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

const loadRelatedProducts = async (productId) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/products/${productId}/related?limit=4`)
    relatedProducts.value = response.data || []
    console.log('연관 상품 로드 완료:', relatedProducts.value.length, '개')
  } catch (err) {
    console.error('연관 상품 로드 실패:', err)
    relatedProducts.value = []
  }
}

const goBack = () => {
  router.go(-1)
}

const goToProduct = (productId) => {
  router.push(`/product/${productId}`)
}

const handleShare = () => {
  if (navigator.share) {
    navigator.share({
      title: product.value.name || product.value.title,
      text: product.value.subtitle || product.value.productShortDescription || product.value.name,
      url: window.location.href
    }).catch(console.error)
  } else {
    // 폴백: URL 복사
    navigator.clipboard.writeText(window.location.href)
    alert('상품 링크가 복사되었습니다!')
  }
}

const toggleWishlist = () => {
  isWishlisted.value = !isWishlisted.value
  console.log('찜하기 토글:', isWishlisted.value)
  // TODO: 찜하기 API 호출
}

const toggleNotification = () => {
  showNotification.value = !showNotification.value
  console.log('알림 설정 토글:', showNotification.value)
  // TODO: 알림 설정 API 호출
}

const handleAddToCart = () => {
  console.log('장바구니 담기:', {
    productId: product.value.productId,
    quantity: quantity.value
  })
  // TODO: 장바구니 추가 API 호출
  alert('장바구니에 상품이 추가되었습니다!')
}

const showSelectionModal = () => {
  console.log('상품 선택 모달 열기')
  // TODO: 상품 선택 모달 구현
}

const getCurrentImage = () => {
  if (!product.value) return 'https://via.placeholder.com/600x600?text=상품+이미지'

  if (product.value.images && product.value.images.length > 0) {
    return product.value.images[currentImageIndex.value] || product.value.images[0]
  }

  return product.value.mainImage || product.value.image || 'https://via.placeholder.com/600x600?text=상품+이미지'
}

const getFinalPrice = () => {
  if (!product.value) return 0

  // salePrice가 있으면 우선 사용
  if (product.value.salePrice && product.value.salePrice > 0) {
    return product.value.salePrice
  }

  // discountPrice가 있으면 사용
  if (product.value.discountPrice && product.value.discountPrice > 0) {
    return product.value.discountPrice
  }

  // 할인율이 있으면 계산
  const discountRate = getDiscountRate()
  if (discountRate > 0 && product.value.price) {
    return Math.floor(product.value.price * (100 - discountRate) / 100)
  }

  return product.value.price || 0
}

const formatPrice = (price) => {
  return price?.toLocaleString() || '0'
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('ko-KR')
}

const maskUserName = (userName) => {
  if (!userName || userName.length < 2) return userName
  return userName.charAt(0) + '*'.repeat(userName.length - 1)
}

const handleImageError = (event) => {
  // 한 번만 처리하도록 플래그 확인
  if (event.target.dataset.errorHandled) {
    return
  }

  event.target.dataset.errorHandled = 'true'
  event.target.style.display = 'none' // 이미지 숨기기

  // 또는 CSS로 스타일링된 div로 대체
  const placeholder = document.createElement('div')
  placeholder.className = 'image-placeholder'
  placeholder.innerHTML = '이미지 없음'
  event.target.parentNode.appendChild(placeholder)
}
// 컴포넌트 마운트 시 상품 정보 로드
onMounted(() => {
  loadProduct()
})

// 라우트 변경 감지
watch(() => route.params.id, (newId) => {
  if (newId) {
    loadProduct()
  }
})
</script>

<style scoped>
.product-detail-container {
  margin: 0 auto;
  background: white;
  min-height: 100vh;
  position: relative;
}

/* 로딩 및 에러 상태 */
.loading-container, .error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  padding: 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #8b5cf6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.retry-button {
  background: #8b5cf6;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 16px;
}

/* 헤더 */
.product-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: white;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.back-button, .share-button {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.back-button:hover, .share-button:hover {
  background: #f5f5f5;
}

/* 상품 이미지 */
.product-images {
  position: relative;
  width: 100%;
  height: 400px;
  background: #f8f9fa;
}

.main-image {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.live-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  background: #ff4444;
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 4px;
}

.live-dot {
  width: 6px;
  height: 6px;
  background: white;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

.discount-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  background: #8b5cf6;
  color: white;
  padding: 8px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.image-indicators {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
}

.indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  transition: background-color 0.2s;
  cursor: pointer;
}

.indicator.active {
  background: white;
}

/* 상품 정보 섹션 */
.product-info-section {
  padding: 20px 16px;
}

.brand-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.brand-label {
  background: #f3f4f6;
  color: #6b7280;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.brand-name {
  color: #8b5cf6;
  font-weight: 500;
  font-size: 14px;
}

.product-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 8px 0 4px 0;
  line-height: 1.4;
}

.product-subtitle {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 16px;
}

.price-section {
  margin-bottom: 16px;
}

.discount-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.discount-rate {
  color: #ef4444;
  font-size: 20px;
  font-weight: 700;
}

.final-price {
  color: #1f2937;
  font-size: 24px;
  font-weight: 700;
}

.original-price {
  color: #9ca3af;
  text-decoration: line-through;
  font-size: 14px;
}

.delivery-notice {
  background: #fef3c7;
  border: 1px solid #fbbf24;
  border-radius: 6px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.delivery-text {
  color: #92400e;
  font-size: 14px;
  font-weight: 500;
}

.chevron-right {
  transform: rotate(180deg);
  color: #92400e;
}

/* 상품 상세 정보 테이블 */
.product-details-table {
  border-top: 1px solid #e5e7eb;
  margin-top: 20px;
}

.detail-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  flex: 0 0 80px;
  color: #6b7280;
  font-size: 14px;
}

.detail-value {
  flex: 1;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.4;
}

/* 구매 액션 섹션 */
.purchase-section {
  position: sticky;
  bottom: 0;
  background: white;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
}

.total-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.total-label {
  color: #6b7280;
  font-size: 14px;
}

.total-amount {
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.quantity-and-wishlist {
  display: flex;
  gap: 8px;
}

.wishlist-button, .notification-button {
  width: 48px;
  height: 48px;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.wishlist-button:hover, .notification-button:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

.wishlist-button.active {
  border-color: #ef4444;
  background: #fef2f2;
}

.buy-now-button {
  flex: 1;
  height: 48px;
  background: #8b5cf6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.buy-now-button:hover {
  background: #7c3aed;
}

/* 탭 섹션 */
.tabs-section {
  margin-top: 20px;
}

.tab-headers {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
}

.tab-header {
  flex: 1;
  padding: 16px 8px;
  background: none;
  border: none;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.tab-header.active {
  color: #1f2937;
  background: white;
  border-bottom-color: #8b5cf6;
}

.tab-content {
  min-height: 400px;
  padding: 20px 16px;
}

.details-content img {
  width: 100%;
  height: auto;
  border-radius: 8px;
  margin-bottom: 16px;
}

.no-detail-images {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.info-content h3 {
  margin-bottom: 16px;
  color: #1f2937;
}

.info-content p {
  margin-bottom: 8px;
  color: #4b5563;
  line-height: 1.5;
}

.review-summary {
  margin-bottom: 24px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.rating-overview {
  display: flex;
  align-items: center;
  gap: 12px;
}

.average-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-score {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.stars {
  display: flex;
  gap: 2px;
}

.review-count {
  color: #6b7280;
  font-size: 14px;
}

.no-reviews {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px solid #f3f4f6;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.reviewer-name {
  font-weight: 500;
  color: #1f2937;
}

.review-rating {
  display: flex;
  gap: 1px;
}

.review-text {
  color: #4b5563;
  line-height: 1.5;
  margin-bottom: 8px;
}

.review-date {
  color: #9ca3af;
  font-size: 12px;
}

.contact-info {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  margin-top: 16px;
}

.contact-info p {
  margin-bottom: 8px;
  color: #4b5563;
}

/* 연관 상품 섹션 */
.related-products-section {
  padding: 20px 16px;
  border-top: 8px solid #f9fafb;
}

.related-products-section h3 {
  margin-bottom: 16px;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
}

.related-products-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.related-product-item {
  background: white;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.related-product-item:hover {
  border-color: #d1d5db;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.related-product-item img {
  width: 100%;
  height: 120px;
  object-fit: cover;
}

.related-product-info {
  padding: 12px;
}

.related-product-title {
  font-size: 14px;
  color: #1f2937;
  margin-bottom: 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-product-price {
  font-size: 16px;
  font-weight: 600;
  color: #8b5cf6;
  margin: 0;
}

/* 하단 선택 상세 버튼 */
.detail-selection-button {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 100;
}

.selection-detail-btn {
  background: #1f2937;
  color: white;
  border: none;
  padding: 12px 16px;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s;
}

.selection-detail-btn:hover {
  transform: translateY(-1px);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (max-width: 480px) {
  .product-detail-container {
    max-width: 100%;
  }

  .product-title {
    font-size: 18px;
  }

  .final-price {
    font-size: 20px;
  }

  .tab-header {
    font-size: 13px;
    padding: 14px 4px;
  }

  .related-products-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .related-product-item img {
    height: 100px;
  }

  .related-product-info {
    padding: 8px;
  }

  .related-product-title {
    font-size: 12px;
  }

  .related-product-price {
    font-size: 14px;
  }
}
</style>
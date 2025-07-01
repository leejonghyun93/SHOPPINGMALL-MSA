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

        <!-- 상품 상세 정보 테이블 -->
        <div class="product-details-table">
          <div class="detail-row">
            <span class="detail-label">배송</span>
            <span class="detail-value">무료배송</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">판매자</span>
            <span class="detail-value">트라이마켓</span>
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

          <!-- 리뷰 탭 -->
          <div v-if="selectedTab === 'reviews'" class="reviews-content">
            <div class="review-summary">
              <div class="rating-overview">
                <div class="average-rating">
                  <span class="rating-score">{{ getAverageRating() }}</span>
                </div>
                <span class="review-count">({{ getReviewCount() }}개 후기)</span>
              </div>

              <div class="review-actions">
                <button
                    v-if="!showReviewForm"
                    @click="showReviewForm = true"
                    class="write-review-button"
                >
                  <Plus :size="16" />
                  리뷰 작성하기
                </button>
              </div>
            </div>

            <!-- 리뷰 작성/수정 폼 -->
            <div v-if="showReviewForm" class="review-form">
              <h4>{{ isReviewEditMode ? '리뷰 수정' : '리뷰 작성' }}</h4>

              <div class="form-group">
                <label>평점</label>
                <div class="rating-input">
                  <Star
                      v-for="i in 5"
                      :key="i"
                      :size="24"
                      :fill="i <= reviewForm.rating ? '#ffc107' : 'none'"
                      :stroke="i <= reviewForm.rating ? '#ffc107' : '#ccc'"
                      @click="reviewForm.rating = i"
                      style="cursor: pointer;"
                  />
                  <span class="rating-text">{{ reviewForm.rating }}점</span>
                </div>
              </div>

              <div class="form-group">
                <label>제목</label>
                <input
                    v-model="reviewForm.title"
                    type="text"
                    placeholder="리뷰 제목을 입력하세요"
                    class="form-input"
                />
              </div>

              <div class="form-group">
                <label>내용</label>
                <textarea
                    v-model="reviewForm.content"
                    placeholder="리뷰 내용을 입력하세요"
                    rows="4"
                    class="form-textarea"
                ></textarea>
              </div>

              <div class="form-buttons">
                <button @click="cancelReviewForm" class="cancel-button">취소</button>
                <button
                    @click="isReviewEditMode ? updateReview() : submitReview()"
                    class="submit-button"
                >
                  {{ isReviewEditMode ? '수정' : '등록' }}
                </button>
              </div>
            </div>

            <!-- 리뷰 목록 -->
            <div class="review-list">
              <div v-if="reviews.length === 0" class="no-reviews">
                <p>아직 작성된 후기가 없습니다.</p>
                <p>첫 번째 후기를 작성해보세요!</p>
              </div>

              <div class="review-item" v-for="review in reviews" :key="review.reviewId">
                <div class="review-header">
                  <div class="reviewer-info">
                    <span class="reviewer-name">{{ maskUserName(review.authorName) }}</span>
                    <div class="review-rating">
                      <Star v-for="i in 5" :key="i" :size="12" :fill="i <= review.rating ? '#ffc107' : 'none'" />
                    </div>
                  </div>

                  <div v-if="(getCurrentUser()?.sub || getCurrentUser()?.userId) === review.userId" class="review-actions">
                    <button @click="editReview(review)" class="edit-button">수정</button>
                    <button @click="deleteReview(review.reviewId)" class="delete-button">삭제</button>
                  </div>
                </div>

                <h5 class="review-title">{{ review.title }}</h5>
                <p class="review-text">{{ review.content }}</p>
                <span class="review-date">{{ formatDate(review.createdDate) }}</span>
              </div>
            </div>
          </div>

          <!-- Q&A 문의 탭 -->
          <div v-if="selectedTab === 'inquiry'" class="inquiry-content">
            <div class="qna-summary">
              <div class="qna-overview">
                <span class="qna-count">{{ getQnaCount() }}개의 문의</span>
              </div>

              <!-- Q&A 작성 버튼 -->
              <div class="qna-actions">
                <button
                    v-if="!showQnaForm"
                    @click="showQnaForm = true"
                    class="write-qna-button"
                >
                  <Plus :size="16" />
                  문의 작성하기
                </button>
              </div>
            </div>

            <!-- Q&A 작성/수정 폼 -->
            <div v-if="showQnaForm" class="qna-form">
              <h4>{{ isQnaEditMode ? '문의 수정' : '문의 작성' }}</h4>

              <div class="form-group">
                <label>문의 유형</label>
                <select v-model="qnaForm.qnaType" class="form-select">
                  <option value="배송">배송</option>
                  <option value="상품">상품</option>
                  <option value="교환/반품">교환/반품</option>
                  <option value="기타">기타</option>
                </select>
              </div>

              <div class="form-group">
                <label>제목</label>
                <input
                    v-model="qnaForm.title"
                    type="text"
                    placeholder="문의 제목을 입력하세요"
                    class="form-input"
                />
              </div>

              <div class="form-group">
                <label>내용</label>
                <textarea
                    v-model="qnaForm.content"
                    placeholder="문의 내용을 입력하세요"
                    rows="4"
                    class="form-textarea"
                ></textarea>
              </div>

              <div class="form-group">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="qnaForm.isSecret">
                  비밀글로 작성하기
                </label>
              </div>

              <div class="form-buttons">
                <button @click="cancelQnaForm" class="cancel-button">취소</button>
                <button
                    @click="isQnaEditMode ? updateQna() : submitQna()"
                    class="submit-button"
                >
                  {{ isQnaEditMode ? '수정' : '등록' }}
                </button>
              </div>
            </div>

            <!-- Q&A 목록 -->
            <div class="qna-list">
              <div v-if="qnas.length === 0" class="no-qnas">
                <p>아직 작성된 문의가 없습니다.</p>
                <p>궁금한 점이 있다면 문의를 남겨보세요!</p>
              </div>

              <div class="qna-item" v-for="qna in qnas" :key="qna.qnaId">
                <div class="qna-header">
                  <div class="qna-info">
                    <span class="qna-type">{{ qna.qnaType }}</span>
                    <span v-if="qna.isSecret === 'Y'" class="secret-badge">🔒 비밀글</span>
                    <span :class="['qna-status', qna.qnaStatus.toLowerCase()]">
                      {{ getQnaStatusText(qna.qnaStatus) }}
                    </span>
                  </div>

                  <!-- 수정/삭제 버튼 (본인 문의인 경우만) -->
                  <div v-if="(getCurrentUser()?.sub || getCurrentUser()?.userId) === qna.userId" class="qna-actions">
                    <button @click="editQna(qna)" class="edit-button">수정</button>
                    <button @click="deleteQna(qna.qnaId)" class="delete-button">삭제</button>
                  </div>
                </div>

                <!-- Q&A 제목 -->
                <h5 class="qna-title" @click="toggleQnaDetail(qna.qnaId)">
                  {{ qna.title }}
                  <ChevronDown :size="16" :class="['expand-icon', { expanded: expandedQna === qna.qnaId }]" />
                </h5>

                <!-- Q&A 메타 정보 -->
                <div class="qna-meta">
                  <span class="qna-author">{{ maskUserName(qna.authorName) }}</span>
                  <span class="qna-date">{{ formatDate(qna.createdDate) }}</span>
                  <span class="qna-views">조회 {{ qna.viewCount || 0 }}</span>
                </div>

                <!-- Q&A 상세 내용 (펼쳐진 경우만 표시) -->
                <div v-if="expandedQna === qna.qnaId" class="qna-detail">
                  <div class="qna-question">
                    <h6>문의 내용</h6>
                    <p>{{ qna.content }}</p>
                  </div>

                  <!-- 답변이 있는 경우 -->
                  <div v-if="qna.answerContent" class="qna-answer">
                    <h6>답변</h6>
                    <p>{{ qna.answerContent }}</p>
                    <div class="answer-meta">
                      <span class="answer-author">{{ qna.answerAuthorName || '관리자' }}</span>
                      <span class="answer-date">{{ formatDate(qna.answerDate) }}</span>
                    </div>
                  </div>

                  <!-- 답변이 없는 경우 -->
                  <div v-else class="no-answer">
                    <p>아직 답변이 등록되지 않았습니다.</p>
                  </div>
                </div>
              </div>
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

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ChevronLeft, Share2, Heart, Bell, Star, Plus, ChevronDown } from 'lucide-vue-next'
import apiClient from '@/api/axiosInstance'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const error = ref(null)
const product = ref(null)
const relatedProducts = ref([])
const reviews = ref([])
const qnas = ref([])
const selectedTab = ref('details')
const quantity = ref(1)
const isWishlisted = ref(false)
const showNotification = ref(false)
const currentImageIndex = ref(0)

// 리뷰 CRUD 관련 상태
const showReviewForm = ref(false)
const isReviewEditMode = ref(false)
const reviewForm = ref({
  rating: 5,
  title: '',
  content: ''
})
const editingReviewId = ref(null)

// Q&A CRUD 관련 상태
const showQnaForm = ref(false)
const isQnaEditMode = ref(false)
const qnaForm = ref({
  qnaType: '기타',
  title: '',
  content: '',
  isSecret: false
})
const editingQnaId = ref(null)
const expandedQna = ref(null)

const tabs = computed(() => [
  { id: 'details', label: '상품설명' },
  { id: 'info', label: '상세정보' },
  { id: 'reviews', label: `후기 (${getReviewCount()})` },
  { id: 'inquiry', label: `문의 (${getQnaCount()})` }
])

const getAuthToken = () => {
  return localStorage.getItem('token')
}

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
    return false
  }
  try {
    const payloadJson = base64UrlDecode(token.split('.')[1])
    const payload = JSON.parse(payloadJson)
    const isValid = payload.exp > Date.now() / 1000
    return isValid
  } catch (e) {
    return false
  }
}

const getCurrentUser = () => {
  const token = getAuthToken()
  if (!token) return null

  try {
    const payloadJson = base64UrlDecode(token.split('.')[1])
    const payload = JSON.parse(payloadJson)
    return payload
  } catch (e) {
    return null
  }
}

const isMyReview = (review) => {
  const currentUser = getCurrentUser()
  if (!currentUser) return false

  return review.authorName === currentUser.sub ||
      review.authorId === currentUser.userId ||
      review.userId === currentUser.userId
}

// Q&A 관련 유틸리티 함수들
const getQnaCount = () => qnas.value.length

const getQnaStatusText = (status) => {
  const statusMap = {
    'WAITING': '답변대기',
    'ANSWERED': '답변완료',
    'CLOSED': '문의종료'
  }
  return statusMap[status] || status
}

const toggleQnaDetail = (qnaId) => {
  expandedQna.value = expandedQna.value === qnaId ? null : qnaId
}

// Q&A CRUD 함수들
const submitQna = async () => {
  if (!isAuthenticated()) {
    alert('로그인이 필요합니다.')
    router.push('/login')
    return
  }

  if (!qnaForm.value.title.trim() || !qnaForm.value.content.trim()) {
    alert('제목과 내용을 모두 입력해주세요.')
    return
  }

  try {
    const qnaData = {
      productId: route.params.id.toString(),
      qnaType: qnaForm.value.qnaType,
      title: qnaForm.value.title.trim(),
      content: qnaForm.value.content.trim(),
      isSecret: qnaForm.value.isSecret ? 'Y' : 'N'
    }

    const response = await apiClient.post('/api/qna', qnaData, {
      withAuth: true,
      timeout: 10000
    })

    if (response.data.success) {
      alert('문의가 등록되었습니다.')
      cancelQnaForm()
      await loadProductQnas(route.params.id)
    } else {
      alert(response.data.message || 'Q&A 등록에 실패했습니다.')
    }

  } catch (error) {
    if (error.response?.status === 401) {
      alert('로그인이 필요합니다.')
      localStorage.removeItem('token')
      router.push('/login')
    } else if (error.response?.status === 403) {
      alert('해당 상품을 구매하고 배송완료된 고객만 문의를 작성할 수 있습니다.')
    } else if (error.response?.status === 400) {
      const errorMsg = error.response?.data?.message || '입력 정보를 확인해주세요.'
      alert(`Q&A 등록 실패: ${errorMsg}`)
    } else {
      alert(`문의 등록에 실패했습니다: ${error.response?.data?.message || error.message}`)
    }
  }
}

const editQna = (qna) => {
  isQnaEditMode.value = true
  showQnaForm.value = true
  editingQnaId.value = qna.qnaId

  qnaForm.value = {
    qnaType: qna.qnaType,
    title: qna.title,
    content: qna.content,
    isSecret: qna.isSecret === 'Y'
  }
}

const updateQna = async () => {
  if (!qnaForm.value.title.trim() || !qnaForm.value.content.trim()) {
    alert('제목과 내용을 모두 입력해주세요.')
    return
  }

  try {
    const qnaData = {
      qnaType: qnaForm.value.qnaType,
      title: qnaForm.value.title.trim(),
      content: qnaForm.value.content.trim(),
      isSecret: qnaForm.value.isSecret ? 'Y' : 'N'
    }

    await apiClient.put(`/api/qna/${editingQnaId.value}`, qnaData, { withAuth: true })

    alert('문의가 수정되었습니다.')
    cancelQnaForm()
    await loadProductQnas(route.params.id)
  } catch (error) {
    if (error.response?.status === 403) {
      alert('본인의 문의만 수정할 수 있습니다.')
    } else {
      alert('문의 수정에 실패했습니다.')
    }
  }
}

const deleteQna = async (qnaId) => {
  if (!confirm('정말로 이 문의를 삭제하시겠습니까?')) {
    return
  }

  try {
    await apiClient.delete(`/api/qna/${qnaId}`, { withAuth: true })

    alert('문의가 삭제되었습니다.')
    await loadProductQnas(route.params.id)
  } catch (error) {
    if (error.response?.status === 403) {
      alert('본인의 문의만 삭제할 수 있습니다.')
    } else {
      alert('문의 삭제에 실패했습니다.')
    }
  }
}

const cancelQnaForm = () => {
  showQnaForm.value = false
  isQnaEditMode.value = false
  editingQnaId.value = null
  qnaForm.value = {
    qnaType: '기타',
    title: '',
    content: '',
    isSecret: false
  }
}

// Q&A 로딩 함수
const loadProductQnas = async (productId) => {
  try {
    const response = await apiClient.get(`/api/qna/product/${productId}`, {
      params: {
        page: 1,
        size: 10,
        sortBy: 'createdAt'
      },
      withAuth: false,
      skipErrorRedirect: true
    })

    if (response.data && Array.isArray(response.data)) {
      qnas.value = response.data.map(qna => ({
        ...qna,
        id: qna.qnaId,
        userName: qna.authorName,
        createdAt: qna.createdDate,
        userId: qna.userId || qna.authorId || qna.user_id,
        authorId: qna.authorId || qna.userId || qna.user_id
      }))
    } else {
      qnas.value = []
    }

  } catch (error) {
    qnas.value = []
  }
}

// 리뷰 CRUD 함수들
const submitReview = async () => {
  if (!isAuthenticated()) {
    alert('로그인이 필요합니다.')
    router.push('/login')
    return
  }

  if (!reviewForm.value.title.trim() || !reviewForm.value.content.trim()) {
    alert('제목과 내용을 모두 입력해주세요.')
    return
  }

  try {
    const reviewData = {
      productId: route.params.id.toString(),
      title: reviewForm.value.title.trim(),
      content: reviewForm.value.content.trim(),
      rating: reviewForm.value.rating
    }

    const response = await apiClient.post('/api/board/reviews', reviewData, {
      withAuth: true,
      timeout: 10000
    })

    if (response.data.success) {
      alert('리뷰가 등록되었습니다.')
      cancelReviewForm()
      await loadProductReviews(route.params.id)
    } else {
      alert(response.data.message || '리뷰 등록에 실패했습니다.')
    }

  } catch (error) {
    if (error.response?.status === 401) {
      alert('로그인이 필요합니다.')
      localStorage.removeItem('token')
      router.push('/login')
    } else if (error.response?.status === 403) {
      alert('해당 상품을 구매하고 배송완료된 고객만 리뷰를 작성할 수 있습니다.')
    } else if (error.response?.status === 400) {
      const errorMsg = error.response?.data?.message || '입력 정보를 확인해주세요.'
      alert(`리뷰 등록 실패: ${errorMsg}`)
    } else {
      alert(`리뷰 등록에 실패했습니다: ${error.response?.data?.message || error.message}`)
    }
  }
}

const editReview = (review) => {
  isReviewEditMode.value = true
  showReviewForm.value = true
  editingReviewId.value = review.reviewId

  reviewForm.value = {
    rating: review.rating,
    title: review.title,
    content: review.content
  }
}

const updateReview = async () => {
  if (!reviewForm.value.title.trim() || !reviewForm.value.content.trim()) {
    alert('제목과 내용을 모두 입력해주세요.')
    return
  }

  try {
    const reviewData = {
      title: reviewForm.value.title.trim(),
      content: reviewForm.value.content.trim(),
      rating: reviewForm.value.rating
    }

    await apiClient.put(`/api/board/reviews/${editingReviewId.value}`, reviewData, { withAuth: true })

    alert('리뷰가 수정되었습니다.')
    cancelReviewForm()
    await loadProductReviews(route.params.id)
  } catch (error) {
    if (error.response?.status === 403) {
      alert('본인의 리뷰만 수정할 수 있습니다.')
    } else {
      alert('리뷰 수정에 실패했습니다.')
    }
  }
}

const deleteReview = async (reviewId) => {
  if (!confirm('정말로 이 리뷰를 삭제하시겠습니까?')) {
    return
  }

  try {
    await apiClient.delete(`/api/board/reviews/${reviewId}`, { withAuth: true })

    alert('리뷰가 삭제되었습니다.')
    await loadProductReviews(route.params.id)
  } catch (error) {
    if (error.response?.status === 403) {
      alert('본인의 리뷰만 삭제할 수 있습니다.')
    } else {
      alert('리뷰 삭제에 실패했습니다.')
    }
  }
}

const cancelReviewForm = () => {
  showReviewForm.value = false
  isReviewEditMode.value = false
  editingReviewId.value = null
  reviewForm.value = {
    rating: 5,
    title: '',
    content: ''
  }
}

// 기타 유틸리티 함수들
const getDiscountRate = () => {
  if (!product.value) return 0;

  if (product.value.salePrice > 0 && product.value.price > 0) {
    const discountAmount = product.value.price - product.value.salePrice;
    const discountRate = Math.floor((discountAmount / product.value.price) * 100);

    if (discountRate <= 0 || discountRate >= 100) {
      return 0;
    }

    return discountRate;
  }

  return 0;
};

const getAverageRating = () => {
  if (reviews.value.length === 0) return 0;

  const totalRating = reviews.value.reduce((sum, review) => sum + (review.rating || 0), 0);
  return (totalRating / reviews.value.length).toFixed(1);
};

const getReviewCount = () => reviews.value.length;

const getProductImage = (prod) => {
  if (prod.images?.length > 0) return prod.images[0]
  return prod.mainImage || prod.image || 'https://via.placeholder.com/300x200?text=상품+이미지'
}

const loadProductReviews = async (productId) => {
  try {
    // 먼저 상품별 리뷰 API 시도
    try {
      const productReviewResponse = await apiClient.get(`/api/board/product/${productId}`, {
        params: {
          page: 1,
          size: 10,
          sortBy: 'createdAt'
        },
        withAuth: false,
        skipErrorRedirect: true
      });

      if (productReviewResponse.data && Array.isArray(productReviewResponse.data) && productReviewResponse.data.length > 0) {
        reviews.value = productReviewResponse.data.map(review => ({
          ...review,
          id: review.reviewId,
          userName: review.authorName,
          createdAt: review.createdDate,
          userId: review.userId || review.authorId || review.user_id,
          authorId: review.authorId || review.userId || review.user_id
        }));
        return;
      }
    } catch (err) {
      // 상품별 리뷰 API 실패시 조용히 넘어감
    }

    // 백엔드 컨트롤러 확인을 위해 헬스체크 시도
    try {
      await apiClient.get('/api/board/health', {
        withAuth: false,
        skipErrorRedirect: true
      });
    } catch (healthErr) {
      // 백엔드 서비스가 동작하지 않는 경우
      reviews.value = [];
      return;
    }

    // 전체 리뷰 목록에서 필터링 시도 (조심스럽게)
    try {
      const allReviewsResponse = await apiClient.get('/api/board/list', {
        params: {
          page: 1,
          size: 50,
          sortBy: 'createdAt'
        },
        withAuth: false,
        timeout: 5000,
        skipErrorRedirect: true
      });

      if (allReviewsResponse.data) {
        let reviewData = [];

        if (Array.isArray(allReviewsResponse.data)) {
          reviewData = allReviewsResponse.data;
        } else if (allReviewsResponse.data.content && Array.isArray(allReviewsResponse.data.content)) {
          reviewData = allReviewsResponse.data.content;
        } else if (allReviewsResponse.data.data && Array.isArray(allReviewsResponse.data.data)) {
          reviewData = allReviewsResponse.data.data;
        }

        const filteredReviews = reviewData.filter(review => {
          return review.productId === productId ||
              review.productId === parseInt(productId) ||
              review.productId?.toString() === productId.toString();
        });

        reviews.value = filteredReviews.map(review => ({
          ...review,
          id: review.reviewId,
          userName: review.authorName,
          createdAt: review.createdDate,
          userId: review.userId || review.authorId || review.user_id,
          authorId: review.authorId || review.userId || review.user_id
        }));

      } else {
        reviews.value = [];
      }
    } catch (err) {
      // 전체 리뷰 API도 실패하면 빈 배열로 설정
      reviews.value = [];
    }

  } catch (error) {
    // 모든 시도가 실패하면 빈 배열로 설정
    reviews.value = [];
  }
};

const loadProduct = async () => {
  try {
    loading.value = true
    error.value = null
    const productId = route.params.id

    const response = await apiClient.get(`/api/products/${productId}`, {
      withAuth: false
    })

    product.value = response.data

    await loadRelatedProducts(productId)
  } catch (err) {
    error.value = err.response?.data?.message || '상품 정보를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

const loadRelatedProducts = async (productId) => {
  try {
    const res = await apiClient.get(`/api/products/${productId}/related?limit=4`, {
      withAuth: false
    })

    relatedProducts.value = res.data || []
  } catch (err) {
    relatedProducts.value = []
  }
}

const goBack = () => router.go(-1)
const goToProduct = (id) => router.push(`/product/${id}`)

const toggleWishlist = () => {
  isWishlisted.value = !isWishlisted.value
}

const handleAddToCart = async () => {
  if (!product.value?.productId) {
    alert('상품 정보를 찾을 수 없습니다.');
    return;
  }

  // 로그인 상태 먼저 확인
  const token = localStorage.getItem('token');
  if (!token) {
    alert('로그인이 필요합니다.');
    router.push('/login');
    return;
  }

  // 토큰 유효성 검증
  if (!isAuthenticated()) {
    alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
    localStorage.removeItem('token');
    router.push('/login');
    return;
  }

  const cartItem = {
    productId: product.value.productId,
    quantity: quantity.value,
    productOptionId: 'defaultOptionId'
  };

  try {
    // 사용자 프로필 확인으로 인증 상태 재검증
    await apiClient.get('/api/users/profile');

    const response = await apiClient.post('/api/cart', cartItem, {
      withAuth: true,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (response.data.success) {
      const goToCart = confirm('장바구니에 추가되었습니다! 장바구니로 이동하시겠습니까?');
      if (goToCart) {
        router.push('/cart');
      }
    } else {
      alert(response.data.message || '장바구니 추가에 실패했습니다.');
    }

  } catch (error) {
    if (error.response) {
      const status = error.response.status;
      const message = error.response.data?.message || error.message;

      if (status === 401) {
        alert('인증이 만료되었습니다. 다시 로그인해주세요.');
        localStorage.removeItem('token');
        router.push('/login');
        return;
      } else if (status === 403) {
        alert('권한이 없습니다.');
        return;
      } else {
        alert(`장바구니 추가 실패: ${message}`);
      }
    } else if (error.request) {
      alert('네트워크 오류가 발생했습니다.');
    } else {
      alert('장바구니 추가 중 오류가 발생했습니다.');
    }
  }
};

const getCurrentImage = () => {
  if (product.value?.images?.length > 0)
    return product.value.images[currentImageIndex.value] || product.value.images[0]
  return product.value?.mainImage || product.value?.image || 'https://via.placeholder.com/600x600?text=상품+이미지'
}

const getFinalPrice = () => {
  if (!product.value) return 0;

  if (product.value.salePrice > 0 && product.value.salePrice < product.value.price) {
    return product.value.salePrice;
  }

  return product.value.price || 0;
};

const formatPrice = (price) => price?.toLocaleString() || '0'

const formatDate = (date) => {
  if (!date) return '';
  try {
    return new Date(date).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  } catch (e) {
    return date;
  }
};

const maskUserName = (name) => {
  if (!name || name.length === 0) return '익명';
  if (name.length === 1) return name;
  if (name.length === 2) return name.charAt(0) + '*';
  return name.charAt(0) + '*'.repeat(name.length - 2) + name.charAt(name.length - 1);
};

const handleImageError = (e) => {
  if (e.target.dataset.errorHandled) return
  e.target.dataset.errorHandled = 'true'
  e.target.style.display = 'none'
  const placeholder = document.createElement('div')
  placeholder.className = 'image-placeholder'
  placeholder.innerHTML = '이미지 없음'
  e.target.parentNode.appendChild(placeholder)
}

// 라이프사이클
onMounted(async () => {
  await loadProduct();

  const productId = route.params.id;
  if (productId) {
    await loadProductReviews(productId);
    await loadProductQnas(productId);
  }
});

watch(() => route.params.id, async (newId) => {
  if (newId) {
    await loadProduct();
    await loadProductReviews(newId);
    await loadProductQnas(newId);
  }
});
</script>

<style scoped src="@/assets/css/productDetail.css"></style>
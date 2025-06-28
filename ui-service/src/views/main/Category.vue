<template>
  <div class="main-container">
    <!-- 메인 카테고리 섹션 (동그란 아이콘들) -->
    <div class="category-container">
      <div class="category-list">
        <div
            v-for="category in categories"
            :key="category.categoryId"
            class="category-item"
            :class="{ active: selectedCategory === category.categoryId }"
            @click="selectCategory(category.categoryId)"
        >
          <div class="category-icon">
            <span class="icon-content" v-if="category.icon">{{ category.icon }}</span>
            <span v-else class="all-icon">전체</span>
          </div>
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- 하위 카테고리 섹션 -->
    <div v-if="subCategories.length > 0" class="sub-category-container">
      <div class="sub-category-list">
        <!-- 전체 버튼을 맨 앞에 -->
        <div
            class="sub-category-item"
            :class="{ active: selectedSubCategory === '' }"
            @click="selectSubCategory('')"
        >
          <span class="sub-category-name">전체</span>
        </div>

        <!-- 하위 카테고리들 -->
        <div
            v-for="subCategory in subCategories"
            :key="subCategory.categoryId"
            class="sub-category-item"
            :class="{ active: selectedSubCategory === subCategory.categoryId }"
            @click="selectSubCategory(subCategory.categoryId)"
        >
          <span class="sub-category-name">{{ subCategory.name }}</span>
        </div>
      </div>
    </div>

    <!-- 인기 BEST 섹션 -->
    <div class="best-section">
      <div class="best-header">
        <h2 class="best-title">
          인기 BEST
          <span class="info-icon">ⓘ</span>
        </h2>
        <div class="filter-tabs">
          <button
              v-for="tab in filterTabs"
              :key="tab.id"
              class="filter-tab"
              :class="{ active: selectedFilter === tab.id }"
              @click="selectFilter(tab.id)"
          >
            {{ tab.name }}
          </button>
        </div>
      </div>

      <!-- 로딩 상태 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>상품을 불러오는 중...</p>
        <p class="loading-details">카테고리: {{ selectedCategory }} / 서브카테고리: {{ selectedSubCategory || '없음' }}</p>
      </div>

      <!-- 상품이 없을 때 -->
      <div v-else-if="!products || products.length === 0" class="no-products">
        <div class="no-products-icon">📦</div>
        <h3>선택한 카테고리에 상품이 없습니다</h3>
        <p class="no-products-details">
          현재 선택된 카테고리: <strong>{{ selectedCategory }}</strong>
          <span v-if="selectedSubCategory"> > <strong>{{ selectedSubCategory }}</strong></span>
        </p>
        <div class="no-products-actions">
          <button @click="fetchProducts" class="action-btn primary">다시 시도</button>
          <button @click="selectCategory('ALL')" class="action-btn secondary">전체 카테고리 보기</button>
          <button @click="debugProductData" class="action-btn debug">디버깅 실행</button>
        </div>
      </div>

      <!-- 상품이 있을 때 -->
      <div v-else>
        <!-- 인기 BEST 가로 슬라이더 -->
        <div class="products-slider-container">
          <div class="products-info">
            <span class="products-count">인기 상품 TOP {{ Math.min(products.length, 10) }}</span>

          </div>

          <div
              class="products-horizontal-slider"
              ref="slider"
              @mousedown="startDrag"
              @mousemove="drag"
              @mouseup="endDrag"
              @mouseleave="endDrag"
              @wheel="handleWheel"
              @touchstart.prevent="startTouch"
              @touchmove.prevent="touchMove"
              @touchend="endTouch"
          >
            <div
                v-for="(product, index) in products.slice(0, 10)"
                :key="product.id || index"
                class="product-card horizontal"
                @click="goToProductDetail(product)"
            >
              <!-- 순위 배지 -->
              <div class="rank-badge">{{ index + 1 }}</div>

              <!-- 라이브 배지 (라이브 상품인 경우) -->
              <div v-if="product.isLive" class="live-badge">
                <span class="live-dot"></span>
                LIVE
              </div>

              <!-- 시청자 수 (라이브 상품인 경우) -->
              <div v-if="product.viewers" class="viewers-count">
                👥 {{ product.viewers }}
              </div>

              <!-- 상품 이미지 -->
              <div class="product-image">
                <img
                    :src="product.image || defaultImage"
                    :alt="product.title || '상품명 없음'"
                    @error="handleImageError"
                    @load="handleImageLoad"
                    loading="lazy"
                />
                <!-- 이미지 로딩 실패 시 오버레이 -->
                <div v-if="product.imageError" class="image-error-overlay">
                  <span>이미지 로드 실패</span>
                </div>
              </div>

              <!-- 상품 정보 -->
              <div class="product-info">
                <h3 class="product-title">{{ product.title || '상품명 없음' }}</h3>
                <div class="product-pricing">
                  <span class="price">{{ formatPrice(product.price) }}원</span>
                  <span v-if="product.originalPrice && product.originalPrice !== product.price" class="original-price">
                    {{ formatPrice(product.originalPrice) }}원
                  </span>
                </div>
                <div class="product-meta">
                  <span class="shipping-info">🚚 무료배송</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 전체 상품 그리드 섹션 -->
    <div v-if="products && products.length > 0" class="products-grid-section">
      <div class="grid-header">
        <h2 class="grid-title">전체 상품</h2>
        <div class="grid-controls">
          <div class="sort-controls">
            <label for="sortSelect">정렬:</label>
            <select id="sortSelect" v-model="selectedSort" @change="handleSortChange" class="sort-select">
              <option value="default">기본순</option>
              <option value="price-low">가격 낮은순</option>
              <option value="price-high">가격 높은순</option>
              <option value="discount">할인율순</option>
              <option value="name">상품명순</option>
            </select>
          </div>
          <div class="products-total">
            총 {{ sortedProducts.length }}개 상품
          </div>
        </div>
      </div>

      <!-- 상품 4열 그리드 -->
      <div class="products-grid">
        <div
            v-for="(product, index) in paginatedProducts"
            :key="product.id || index"
            class="product-card grid"
            @click="goToProductDetail(product)"
        >
          <!-- 라이브 배지 (라이브 상품인 경우) -->
          <div v-if="product.isLive" class="live-badge">
            <span class="live-dot"></span>
            LIVE
          </div>

<!--          &lt;!&ndash; 시청자 수 (라이브 상품인 경우) &ndash;&gt;-->
<!--          <div v-if="product.viewers" class="viewers-count">-->
<!--            👥 {{ product.viewers }}-->
<!--          </div>-->

          <!-- 상품 이미지 -->
          <div class="product-image">
            <img
                :src="product.image || defaultImage"
                :alt="product.title || '상품명 없음'"
                @error="handleImageError"
                @load="handleImageLoad"
                loading="lazy"
            />
            <!-- 이미지 로딩 실패 시 오버레이 -->
            <div v-if="product.imageError" class="image-error-overlay">
              <span>이미지 로드 실패</span>
            </div>
          </div>

          <!-- 상품 정보 -->
          <div class="product-info">
            <h3 class="product-title">{{ product.title || '상품명 없음' }}</h3>
            <div class="product-pricing">
              <span class="price">{{ formatPrice(product.price) }}원</span>
              <span v-if="product.originalPrice && product.originalPrice !== product.price" class="original-price">
                {{ formatPrice(product.originalPrice) }}원
              </span>
            </div>
            <div class="product-meta">
              <span class="shipping-info">🚚 무료배송</span>
              <span v-if="product.categoryId" class="category-tag">{{ product.categoryId }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 더보기 버튼 -->
      <div v-if="sortedProducts.length > currentPage * itemsPerPage" class="load-more-container">
        <button @click="loadMore" class="load-more-btn">
          더보기 ({{ sortedProducts.length - currentPage * itemsPerPage }}개 상품 더 있음)
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, watch, onMounted, computed} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import apiClient from '@/api/axiosInstance.js'

// 반응형 상태 변수들
const selectedCategory = ref('ALL')
const selectedSubCategory = ref('')
const selectedFilter = ref('sale')
const selectedSort = ref('default')
const currentPage = ref(1)
const itemsPerPage = ref(12)
const slider = ref(null)
const isDragging = ref(false)
const startX = ref(0)
const scrollLeft = ref(0)
const router = useRouter()
const route = useRoute()
const subCategories = ref([])
const loading = ref(false)
const showDebug = ref(false)

// 초기 데이터
const categories = ref([
  {categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0}
])

const filterTabs = ref([
  {id: 'sale', name: '상품'}
])

const products = ref([])

// 상수 및 유틸리티
const defaultImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZGRkIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPk5vIEltYWdlPC90ZXh0Pjwvc3ZnPg=='

const getIconForCategory = (categoryId) => {
  const iconMap = {
    '1': '🥬', '2': '🥫', '3': '🍱',
    '4': '🍞', '5': '🥛', '6': '💊',
    '7': '🍳', '8': '🧻', '9': '🍼'
  }
  return iconMap[categoryId] || '📦'
}

// 계산된 속성
const currentCategoryName = computed(() => {
  const category = categories.value.find(cat => cat.categoryId === selectedCategory.value)
  return category ? category.name : selectedCategory.value
})

// 정렬된 상품 목록
const sortedProducts = computed(() => {
  if (!products.value || products.value.length === 0) return []

  const sorted = [...products.value]

  switch (selectedSort.value) {
    case 'price-low':
      return sorted.sort((a, b) => a.price - b.price)
    case 'price-high':
      return sorted.sort((a, b) => b.price - a.price)
    case 'discount':
      return sorted.sort((a, b) => (b.discount || 0) - (a.discount || 0))
    case 'name':
      return sorted.sort((a, b) => a.title.localeCompare(b.title))
    default:
      return sorted
  }
})

// 페이지네이션된 상품 목록
const paginatedProducts = computed(() => {
  const startIndex = 0
  const endIndex = currentPage.value * itemsPerPage.value
  return sortedProducts.value.slice(startIndex, endIndex)
})

/**
 * fetchProducts 함수 - fetch 방식
 */
const fetchProducts = async () => {
  try {
    loading.value = true


    let requestCategoryId
    if (selectedSubCategory.value && selectedSubCategory.value !== '') {
      requestCategoryId = selectedSubCategory.value
    } else if (selectedCategory.value === 'ALL') {
      requestCategoryId = 'ALL'
    } else {
      requestCategoryId = selectedCategory.value
    }


    const params = {categoryId: requestCategoryId, limit: 20}

    // axiosInstance 사용하여 요청

    const response = await apiClient.get('/api/products/filter', {
      params: params,
      withAuth: false
    })

    const productData = response.data

    // 응답이 배열이 아니면 처리
    if (!Array.isArray(productData)) {
      products.value = []
      return
    }

    if (productData.length === 0) {
      products.value = []
      return
    }

    // 상품 데이터 변환
    products.value = productData.map((product, index) => {


      const convertedProduct = {
        id: product.productId || `product_${index}`,
        title: product.name || product.title || '상품명 없음',
        price: product.price || product.salePrice || product.finalPrice || 0,
        originalPrice: product.originalPrice || product.price || 0,
        discount: product.discount || product.discountRate || null,
        image: getImageUrl(product), // 이미지 URL 생성
        isLive: product.isLive || false,
        viewers: product.viewers || null,
        categoryId: product.categoryId || 'unknown',
        imageError: false,
        // 디버깅용 원본 이미지 정보 보존
        _originalImageData: {
          mainImage: product.mainImage,
          image: product.image,
          images: product.images,
          mainImageUrl: product.mainImageUrl
        }
      }


      return convertedProduct
    })


  } catch (error) {
    products.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 수정된 getImageUrl 함수
 * 백엔드에서 이미 완전한 API 경로를 제공하므로 기본 URL만 붙임
 */
const getImageUrl = (product) => {

  // 디버깅을 위한 원본 데이터 로깅
  const availableImageData = {
    mainImageUrl: product.mainImageUrl,
    mainImage: product.mainImage,
    image: product.image,
    images: product.images
  };


  // 우선순위에 따라 이미지 경로 선택
  let selectedImagePath = null;
  let source = '';

  if (product.mainImageUrl && product.mainImageUrl.trim() !== '') {
    selectedImagePath = product.mainImageUrl.trim();
    source = 'mainImageUrl';
  } else if (product.mainImage && product.mainImage.trim() !== '') {
    selectedImagePath = product.mainImage.trim();
    source = 'mainImage';
  } else if (product.image && product.image.trim() !== '') {
    selectedImagePath = product.image.trim();
    source = 'image';
  } else if (product.images && Array.isArray(product.images) && product.images.length > 0) {
    selectedImagePath = product.images[0].trim();
    source = 'images[0]';
  }

  if (selectedImagePath) {
    let finalUrl;

    // 절대 URL로 변환
    if (selectedImagePath.startsWith('http://') || selectedImagePath.startsWith('https://')) {
      // 이미 완전한 URL인 경우
      finalUrl = selectedImagePath;
    } else if (selectedImagePath.startsWith('/api/')) {
      // API 경로인 경우 (/api/images/products/IMG002.jpg)
      finalUrl = `http://localhost:8080${selectedImagePath}`;
    } else if (selectedImagePath.startsWith('/')) {
      // 루트 상대 경로인 경우 (/images/IMG002.jpg)
      finalUrl = `http://localhost:8080${selectedImagePath}`;
    } else {
      // 파일명만 있는 경우 (IMG002.jpg)
      finalUrl = `http://localhost:8080/api/images/products/${selectedImagePath}`;
    }

    return finalUrl;
  }

  return defaultImage;
};


/**
 * 더욱 간단한 버전 (추천)
 */
const getImageUrlSimple = (product) => {
  // 우선순위: mainImage > image > images[0] > mainImageUrl
  const imagePath = product.mainImage ||
      product.image ||
      (product.images && product.images[0]) ||
      product.mainImageUrl

  if (imagePath && imagePath.trim() !== '') {
    // 이미 완전한 경로인 경우 (http로 시작)
    if (imagePath.startsWith('http')) {
      return imagePath
    }

    // API 경로인 경우 기본 URL만 붙임
    if (imagePath.startsWith('/api/')) {
      return `http://localhost:8080${imagePath}`
    }

    // 상대 경로인 경우 전체 경로 구성
    return `http://localhost:8080/api/images/products/${imagePath}`
  }

  return defaultImage
}

/**
 * 환경변수를 사용한 버전 (프로덕션 환경 고려)
 */
const getImageUrlWithEnv = (product) => {
  const BASE_URL = process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080'

  const imagePath = product.mainImage ||
      product.image ||
      (product.images && product.images[0]) ||
      product.mainImageUrl

  if (imagePath && imagePath.trim() !== '') {
    if (imagePath.startsWith('http')) {
      return imagePath
    }

    if (imagePath.startsWith('/api/')) {
      return `${BASE_URL}${imagePath}`
    }

    return `${BASE_URL}/api/images/products/${imagePath}`
  }

  return defaultImage
}

/**
 * 디버깅용 함수 - 이미지 URL 생성 과정 로깅
 */
const getImageUrlWithDebug = (product) => {
  const imagePath = product.mainImage ||
      product.image ||
      (product.images && product.images[0]) ||
      product.mainImageUrl

  if (imagePath && imagePath.trim() !== '') {
    let finalUrl

    if (imagePath.startsWith('http')) {
      finalUrl = imagePath

    } else if (imagePath.startsWith('/api/')) {
      finalUrl = `http://localhost:8080${imagePath}`

    } else {
      finalUrl = `http://localhost:8080/api/images/products/${imagePath}`
    }

    return finalUrl
  }

  return defaultImage
}
/**
 * 메인 카테고리 조회
 */
const fetchMainCategories = async () => {
  try {
    const res = await apiClient.get('/api/categories/main', {withAuth: false})

    if (res.data && res.data.length > 0) {
      const allCategory = {categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0}

      const serverCategories = res.data
          .filter(cat => cat.categoryUseYn === 'Y' && cat.categoryLevel === 1)
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .map(cat => ({
            categoryId: cat.categoryId,
            name: cat.name,
            icon: getIconForCategory(cat.categoryId),
            categoryDisplayOrder: cat.categoryDisplayOrder
          }))

      categories.value = [allCategory, ...serverCategories]
    }
  } catch (error) {
    categories.value = [{categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0}]
  }
}

/**
 * 하위 카테고리 조회
 */
const fetchSubCategories = async (parentCategoryId) => {
  try {
    if (parentCategoryId === 'ALL') {
      subCategories.value = []
      return
    }

    const res = await apiClient.get(`/api/categories/${parentCategoryId}/sub`, {withAuth: false})

    subCategories.value = res.data?.length > 0
        ? res.data
            .filter(cat => cat.categoryUseYn === 'Y')
            .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
            .map(cat => ({
              categoryId: cat.categoryId,
              name: cat.name,
              categoryDisplayOrder: cat.categoryDisplayOrder
            }))
        : []
  } catch (error) {
    subCategories.value = []
  }
}

/**
 * 정렬 변경 처리
 */
const handleSortChange = () => {
  currentPage.value = 1
}

/**
 * 더보기 버튼 처리
 */
const loadMore = () => {
  currentPage.value += 1
}

/**
 * 메인 카테고리 선택
 */
const selectCategory = async (categoryId) => {
  if (selectedCategory.value === categoryId) return

  selectedCategory.value = categoryId
  selectedSubCategory.value = ''
  currentPage.value = 1
  selectedSort.value = 'default'

  await fetchSubCategories(categoryId)
  await fetchProducts()

  if (categoryId === 'ALL') {
    router.push('/category/')
  } else {
    router.push(`/category/${categoryId}`)
  }
}

/**
 * 서브 카테고리 선택
 */
const selectSubCategory = async (subCategoryId) => {
  if (selectedSubCategory.value === subCategoryId) {
    return
  }

  selectedSubCategory.value = subCategoryId
  currentPage.value = 1

  await fetchProducts()
}

/**
 * 필터 선택
 */
const selectFilter = async (filterId) => {
  selectedFilter.value = filterId
  currentPage.value = 1
  await fetchProducts()
}

/**
 * 상품 상세 페이지로 이동
 */
const goToProductDetail = (product) => {
  router.push(`/product/${product.id}`)
}

// 유틸리티 함수들

/**
 * 가격 포맷팅
 */
const formatPrice = (price) => {
  return price?.toLocaleString() || '0'
}

/**
 * 이미지 에러 처리
 */
const handleImageError = (event) => {
  const img = event.target;

  // 이미 처리된 경우 무시
  if (img.dataset.errorHandled === 'true' || img.src === defaultImage) {
    return;
  }

  // 에러 처리 완료 표시
  img.dataset.errorHandled = 'true';

  // 이벤트 핸들러 제거 (무한 루프 방지)
  img.onerror = null;

  // 기본 이미지로 변경
  img.src = defaultImage;

};

// 상품 데이터 디버깅 함수 개선
const debugProductImages = () => {

  if (products.value && products.value.length > 0) {
    products.value.slice(0, 3).forEach((product, index) => {


      const generatedUrl = getImageUrl(product);


      // 실제 이미지 접근 테스트
      testImageAccess(generatedUrl, `상품${index + 1}`);
    });
  } else {

  }
};

// 🔥 이미지 URL 테스트 함수
const testImageUrls = async () => {
  const testFileName = 'IMG002.jpg'; // 실제 파일명으로 변경
  const testUrls = [
    `http://localhost:8080/api/images/products/${testFileName}`,
    `http://localhost:8088/api/images/products/${testFileName}`,
    `http://localhost:8080/images/${testFileName}`,
    `http://localhost:8088/images/${testFileName}`,
    `http://localhost:8080/files/images/${testFileName}`
  ];

  for (const url of testUrls) {
    try {
      const response = await fetch(url, {
        method: 'HEAD',
        mode: 'no-cors' // CORS 문제 회피
      });

    } catch (error) {

    }
  }

  // 실제 이미지 태그로 테스트
  testUrls.forEach((url, index) => {
    const testImg = new Image();

    testImg.src = url;
  });
};
/**
 * 이미지 로드 성공 처리
 */
const handleImageLoad = (event) => {
  const img = event.target;

  // 에러 처리 플래그 제거
  img.removeAttribute('data-error-handled');
};

// 드래그 스크롤 기능

const startDrag = (e) => {
  e.preventDefault()
  isDragging.value = true
  startX.value = e.pageX
  scrollLeft.value = slider.value.scrollLeft
  slider.value.style.cursor = 'grabbing'
  slider.value.style.userSelect = 'none'
}

const drag = (e) => {
  if (!isDragging.value) return
  e.preventDefault()
  const x = e.pageX
  const walk = (x - startX.value) * 1.5
  slider.value.scrollLeft = scrollLeft.value - walk
}

const endDrag = () => {
  if (!isDragging.value) return
  isDragging.value = false
  slider.value.style.cursor = 'grab'
  slider.value.style.userSelect = 'auto'
}

const handleWheel = (e) => {
  e.preventDefault()
  const scrollAmount = e.deltaY * 0.5
  slider.value.scrollLeft += scrollAmount
}

const startTouch = (e) => {
  isDragging.value = true
  startX.value = e.touches[0].pageX
  scrollLeft.value = slider.value.scrollLeft
}

const touchMove = (e) => {
  if (!isDragging.value) return
  const x = e.touches[0].pageX
  const walk = (x - startX.value) * 1.5
  slider.value.scrollLeft = scrollLeft.value - walk
}

const endTouch = () => {
  isDragging.value = false
}

// 디버깅 함수들

/**
 * 상품 데이터 디버깅
 */
const debugProductData = async () => {
  try {
    const testResponse = await apiClient.get('/api/products/filter', {
      params: {categoryId: 'ALL', limit: 5},
      withAuth: false
    })

    const categories = ['ALL', '1', '2', '3', '4']
    for (const catId of categories) {
      try {
        const catResponse = await apiClient.get('/api/products/filter', {
          params: {categoryId: catId, limit: 3},
          withAuth: false
        })

      } catch (catError) {

      }
    }

    alert('디버깅 정보가 콘솔에 출력되었습니다. 개발자 도구를 확인하세요.')

  } catch (error) {

    alert('디버깅 실행 중 오류가 발생했습니다: ' + error.message)
  }
}

// 라이프사이클 훅

/**
 * URL 파라미터 변화 감지
 */
watch(() => route.params, async (newParams, oldParams) => {
  let needsProductRefresh = false
  let needsSubCategoryRefresh = false

  if (newParams.categoryId && newParams.categoryId !== selectedCategory.value) {
    selectedCategory.value = newParams.categoryId
    selectedSubCategory.value = ''
    needsSubCategoryRefresh = true
    needsProductRefresh = true
  }

  if (needsSubCategoryRefresh) {
    await fetchSubCategories(selectedCategory.value)
  }

  if (needsProductRefresh) {
    await fetchProducts()
  }
}, {immediate: false})

/**
 * 컴포넌트 마운트 시 초기화
 */
onMounted(async () => {
  try {
    await fetchMainCategories()

    if (route.params.categoryId) {
      selectedCategory.value = route.params.categoryId
      await fetchSubCategories(route.params.categoryId)

      if (route.params.subCategoryId) {
        selectedSubCategory.value = route.params.subCategoryId
      }
    }

    await fetchProducts()

  } catch (error) {

  }
})
</script>

<style scoped src="@/assets/css/category.css"></style>